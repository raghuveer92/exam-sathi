package com.examsaathi.service;

import com.examsaathi.dto.request.SubmitTestAnswerRequest;
import com.examsaathi.dto.request.SubmitTopicTestRequest;
import com.examsaathi.dto.response.*;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.entity.*;
import com.examsaathi.exception.GoogleSheetsException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import com.examsaathi.util.MockTestMasteryUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Topic mock tests powered by Google Sheets (no DB question bank).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TopicMockTestService {

    private static final double MARKS_PER_QUESTION = 1.0;

    private final TestAttemptRepository attemptRepository;
    private final TestAttemptAnswerRepository answerRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserExamRepository userExamRepository;
    private final StudyProgressRepository progressRepository;
    private final TopicTestConfigService topicTestConfigService;
    private final ExamSubjectGroupService examSubjectGroupService;
    private final GoogleSheetsService googleSheetsService;
    private final CacheEvictionService cacheEvictionService;
    private final ObjectMapper objectMapper;

    public TestAttemptResponse startTopicTest(Long userId, Long topicId) {
        UserExam userExam = resolveUserExamForTopic(userId, topicId);

        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        Exam exam = userExam.getExam();
        String sheetId = googleSheetsService.requireSheetId(exam);

        List<SheetQuestion> pool = new ArrayList<>(filterPoolForTopic(sheetId, exam, topic));
        TopicTestConfig config = topicTestConfigService.resolveConfig(topicId);
        int configuredNum = config != null ? config.getNumQuestions() : 10;
        int durationMinutes = config != null ? config.getDurationMinutes() : 15;
        pool = new ArrayList<>(googleSheetsService.applyDifficultyFilter(
            pool, config != null ? config.getDifficultyFilter() : "ALL"));

        int numQuestions = googleSheetsService.resolveEffectiveQuestionCount(
            configuredNum, pool.size(), true);
        String topicContext = topic.getChapter().getSubject().getName() + " / " + topic.getTitle();
        googleSheetsService.validatePoolSize(pool, numQuestions, topicContext);

        Collections.shuffle(pool);
        List<SheetQuestion> selected = new ArrayList<>(pool.subList(0, numQuestions));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        TestAttempt attempt = TestAttempt.builder()
            .user(user)
            .topic(topic)
            .topicTestConfig(config)
            .durationMinutes(durationMinutes)
            .totalQuestions(selected.size())
            .questionCount(selected.size())
            .maxScore((double) selected.size() * MARKS_PER_QUESTION)
            .build();

        for (SheetQuestion sheetQuestion : selected) {
            attempt.getAnswers().add(TestAttemptAnswer.builder()
                .attempt(attempt)
                .sheetQuestionId(sheetQuestion.getId())
                .questionSnapshot(toSnapshot(sheetQuestion))
                .build());
        }

        return toInProgressResponse(attemptRepository.save(attempt));
    }

    public TestAttemptResponse submitTopicTest(Long userId, SubmitTopicTestRequest request) {
        if (request.getAttemptId() == null) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "attemptId is required");
        }

        TestAttempt attempt = attemptRepository.findByIdAndUserId(request.getAttemptId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException("Test attempt", request.getAttemptId()));

        if (!isSheetBasedAttempt(attempt)) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Attempt is not a Google Sheets topic mock test");
        }
        if (attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Test already submitted");
        }

        Map<String, SubmitTestAnswerRequest> answerMap = Optional.ofNullable(request.getAnswers())
            .orElse(Collections.emptyList())
            .stream()
            .filter(a -> a.getSheetQuestionId() != null && !a.getSheetQuestionId().isBlank())
            .collect(Collectors.toMap(
                a -> a.getSheetQuestionId().trim(),
                a -> a,
                (a, b) -> b));

        int correct = 0;
        int incorrect = 0;
        int skipped = 0;
        double score = 0;

        List<TestAttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        for (TestAttemptAnswer answer : answers) {
            SheetQuestion sheetQuestion = fromSnapshot(answer.getQuestionSnapshot());
            SubmitTestAnswerRequest submitted = answerMap.get(answer.getSheetQuestionId());

            if (submitted != null) {
                answer.setMarkedForReview(Boolean.TRUE.equals(submitted.getMarkedForReview()));
                if (submitted.getSelectedOptionKeys() != null && !submitted.getSelectedOptionKeys().isEmpty()) {
                    List<String> keys = submitted.getSelectedOptionKeys().stream()
                        .map(k -> k.trim().toUpperCase())
                        .distinct()
                        .sorted()
                        .toList();
                    answer.setSelectedOptionKeys(String.join(",", keys));
                    answer.setAnsweredAt(LocalDateTime.now());
                }
            }

            boolean answered = answer.getSelectedOptionKeys() != null && !answer.getSelectedOptionKeys().isBlank();
            if (!answered) {
                skipped++;
                answer.setIsCorrect(false);
                answer.setMarksAwarded(0.0);
                continue;
            }

            boolean isCorrect = gradeSheetAnswer(sheetQuestion, answer.getSelectedOptionKeys());
            answer.setIsCorrect(isCorrect);
            if (isCorrect) {
                correct++;
                answer.setMarksAwarded(MARKS_PER_QUESTION);
                score += MARKS_PER_QUESTION;
            } else {
                incorrect++;
                answer.setMarksAwarded(0.0);
            }
        }

        answerRepository.saveAll(answers);

        int total = attempt.getTotalQuestions();
        double percentage = total > 0
            ? MockTestMasteryUtil.roundOneDecimal((score / attempt.getMaxScore()) * 100.0)
            : 0.0;
        double accuracy = total > 0
            ? MockTestMasteryUtil.roundOneDecimal((correct * 100.0) / total)
            : 0.0;
        TestAttempt.MasteryLevel masteryLevel = MockTestMasteryUtil.resolveMasteryLevel(percentage);

        attempt.setCorrectCount(correct);
        attempt.setIncorrectCount(incorrect);
        attempt.setSkippedCount(skipped);
        attempt.setQuestionCount(total);
        attempt.setWrongCount(incorrect);
        attempt.setUnansweredCount(skipped);
        attempt.setScore(score);
        attempt.setPercentage(percentage);
        attempt.setAccuracy(accuracy);
        attempt.setMasteryLevel(masteryLevel);

        applyImprovementTracking(attempt, userId, percentage);

        attempt.setTimeSpentSeconds(request.getTimeSpentSeconds());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(Boolean.TRUE.equals(request.getTimedOut())
            ? TestAttempt.AttemptStatus.TIMED_OUT
            : TestAttempt.AttemptStatus.SUBMITTED);

        TestAttempt saved = attemptRepository.save(attempt);
        updateStudyProgressAfterSubmit(userId, attempt.getTopic().getId(), percentage, masteryLevel);

        cacheEvictionService.evictDashboard(userId);
        cacheEvictionService.evictSyncBundle(userId);
        cacheEvictionService.evictLeaderboardAndAnalytics();

        return toCompletedResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public TestAttemptResponse getTopicTestResult(Long userId, Long attemptId) {
        TestAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Test attempt", attemptId));

        if (!isSheetBasedAttempt(attempt)) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Attempt is not a Google Sheets topic mock test");
        }
        if (attempt.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS) {
            return toInProgressResponse(attempt);
        }
        return toCompletedResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public TopicMasteryResponse getTopicMastery(Long userId, Long topicId) {
        UserExam userExam = resolveUserExamForTopic(userId, topicId);
        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));

        StudyProgress progress = progressRepository
            .findByUserExamIdAndTopicId(userExam.getId(), topicId)
            .orElse(null);

        boolean sheetConfigured = googleSheetsService.hasSheetConfigured(userExam.getExam());

        String masteryLevel = null;
        if (progress != null && progress.getMasteryScore() != null) {
            masteryLevel = MockTestMasteryUtil.resolveMasteryLevel(progress.getMasteryScore()).name();
        }

        return TopicMasteryResponse.builder()
            .topicId(topicId)
            .topicTitle(topic.getTitle())
            .subjectName(topic.getChapter().getSubject().getName())
            .testStatus(progress != null ? progress.getTestStatus().name() : StudyProgress.TestStatus.LOCKED.name())
            .lastTestScore(progress != null ? progress.getLastTestScore() : null)
            .bestTestScore(progress != null ? progress.getBestTestScore() : null)
            .masteryScore(progress != null ? progress.getMasteryScore() : null)
            .totalTestsAttempted(progress != null ? progress.getTotalTestsAttempted() : 0)
            .masteryLevel(masteryLevel)
            .canAttempt(sheetConfigured)
            .build();
    }

    public boolean canAttemptTest(Long userId, Long examId, Long topicId) {
        UserExam userExam = userExamRepository.findByUserIdAndExamId(userId, examId)
            .orElseThrow(() -> new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Not enrolled in this exam"));

        if (!Boolean.TRUE.equals(userExam.getIsActive())) {
            return false;
        }

        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));

        if (!examSubjectGroupService.isSubjectVisible(userExam, topic.getChapter().getSubject().getId())) {
            return false;
        }

        return googleSheetsService.hasSheetConfigured(userExam.getExam());
    }

    private void applyImprovementTracking(TestAttempt attempt, Long userId, double percentage) {
        List<TestAttempt> previous = attemptRepository
            .findByUserIdAndTopicIdAndStatusNotOrderBySubmittedAtDesc(
                userId,
                attempt.getTopic().getId(),
                TestAttempt.AttemptStatus.IN_PROGRESS,
                PageRequest.of(0, 5));

        Optional<TestAttempt> priorSheetAttempt = previous.stream()
            .filter(a -> a.getId() != null && !a.getId().equals(attempt.getId()))
            .filter(this::isSheetBasedAttempt)
            .findFirst();

        if (priorSheetAttempt.isPresent()) {
            TestAttempt prior = priorSheetAttempt.get();
            double priorPct = prior.getPercentage() != null ? prior.getPercentage() : 0.0;
            attempt.setPreviousAttemptId(prior.getId());
            attempt.setPreviousPercentage(priorPct);
            attempt.setImprovementScore(MockTestMasteryUtil.roundOneDecimal(percentage - priorPct));
        }
    }

    private void updateStudyProgressAfterSubmit(
            Long userId,
            Long topicId,
            double percentage,
            TestAttempt.MasteryLevel masteryLevel) {
        UserExam userExam = resolveUserExamForTopic(userId, topicId);
        StudyProgress progress = progressRepository
            .findByUserExamIdAndTopicId(userExam.getId(), topicId)
            .orElse(null);
        if (progress == null) {
            return;
        }

        progress.setLastTestScore(percentage);
        double best = progress.getBestTestScore() == null ? percentage
            : Math.max(progress.getBestTestScore(), percentage);
        progress.setBestTestScore(best);
        progress.setMasteryScore(best);
        progress.setTotalTestsAttempted(progress.getTotalTestsAttempted() + 1);
        progress.setTestStatus(masteryLevel == TestAttempt.MasteryLevel.MASTERED
            ? StudyProgress.TestStatus.COMPLETED
            : StudyProgress.TestStatus.AVAILABLE);
        progressRepository.save(progress);
    }

    private UserExam resolveUserExamForTopic(Long userId, Long topicId) {
        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        Long subjectId = topic.getChapter().getSubject().getId();

        Optional<UserExam> active = userExamRepository.findByUserIdAndIsActiveTrue(userId);
        if (active.isPresent() && examSubjectGroupService.isSubjectVisible(active.get(), subjectId)) {
            return active.get();
        }

        for (UserExam enrollment : userExamRepository.findByUserIdWithExamOrderByCreatedAtAsc(userId)) {
            if (examSubjectGroupService.isSubjectVisible(enrollment, subjectId)) {
                return enrollment;
            }
        }
        throw new GoogleSheetsException(
            GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
            "Not enrolled in an exam that includes this topic");
    }

    private List<SheetQuestion> filterPoolForTopic(String sheetId, Exam exam, Topic topic) {
        List<SheetQuestion> all = googleSheetsService.loadQuestions(sheetId);
        return googleSheetsService.filterForTopic(
            all, exam, topic.getChapter().getSubject(), topic);
    }

    private boolean isSheetBasedAttempt(TestAttempt attempt) {
        List<TestAttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        return answers.stream().anyMatch(a -> a.getSheetQuestionId() != null);
    }

    private boolean gradeSheetAnswer(SheetQuestion question, String selectedKeysCsv) {
        if (question.getCorrectOption() == null || selectedKeysCsv == null) {
            return false;
        }
        String correct = question.getCorrectOption().trim().toUpperCase();
        Set<String> selected = Arrays.stream(selectedKeysCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        return selected.size() == 1 && selected.contains(correct);
    }

    private String toSnapshot(SheetQuestion question) {
        try {
            return objectMapper.writeValueAsString(question);
        } catch (JsonProcessingException e) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Failed to serialize question snapshot");
        }
    }

    private SheetQuestion fromSnapshot(String json) {
        if (json == null || json.isBlank()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Missing question snapshot");
        }
        try {
            return objectMapper.readValue(json, SheetQuestion.class);
        } catch (JsonProcessingException e) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Invalid question snapshot");
        }
    }

    private TestAttemptResponse toInProgressResponse(TestAttempt attempt) {
        List<TestAttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        return TestAttemptResponse.builder()
            .id(attempt.getId())
            .topicId(attempt.getTopic().getId())
            .topicTitle(attempt.getTopic().getTitle())
            .status(attempt.getStatus().name())
            .startedAt(attempt.getStartedAt())
            .durationMinutes(attempt.getDurationMinutes())
            .totalQuestions(attempt.getTotalQuestions())
            .questions(answers.stream()
                .map(a -> toQuestionResponse(fromSnapshot(a.getQuestionSnapshot()), a, false))
                .toList())
            .build();
    }

    private TestAttemptResponse toCompletedResponse(TestAttempt attempt, boolean includeReview) {
        List<TestAttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        return TestAttemptResponse.builder()
            .id(attempt.getId())
            .topicId(attempt.getTopic().getId())
            .topicTitle(attempt.getTopic().getTitle())
            .status(attempt.getStatus().name())
            .startedAt(attempt.getStartedAt())
            .submittedAt(attempt.getSubmittedAt())
            .durationMinutes(attempt.getDurationMinutes())
            .timeSpentSeconds(attempt.getTimeSpentSeconds())
            .totalQuestions(attempt.getTotalQuestions())
            .questionCount(attempt.getQuestionCount())
            .correctCount(attempt.getCorrectCount())
            .incorrectCount(attempt.getIncorrectCount())
            .wrongCount(attempt.getWrongCount())
            .skippedCount(attempt.getSkippedCount())
            .unansweredCount(attempt.getUnansweredCount())
            .score(attempt.getScore())
            .maxScore(attempt.getMaxScore())
            .percentage(attempt.getPercentage())
            .accuracy(attempt.getAccuracy())
            .masteryLevel(attempt.getMasteryLevel() != null ? attempt.getMasteryLevel().name() : null)
            .previousAttemptId(attempt.getPreviousAttemptId())
            .previousPercentage(attempt.getPreviousPercentage())
            .improvementScore(attempt.getImprovementScore())
            .review(includeReview ? answers.stream().map(this::toReview).toList() : null)
            .build();
    }

    private TestQuestionResponse toQuestionResponse(
            SheetQuestion q,
            TestAttemptAnswer answer,
            boolean showCorrect) {
        List<String> selected = answer.getSelectedOptionKeys() == null || answer.getSelectedOptionKeys().isBlank()
            ? List.of()
            : Arrays.asList(answer.getSelectedOptionKeys().split(","));

        return TestQuestionResponse.builder()
            .sheetQuestionId(q.getId())
            .questionText(q.getQuestionEn())
            .questionTextHi(q.getQuestionHi())
            .questionType("SINGLE_CORRECT")
            .marks(MARKS_PER_QUESTION)
            .negativeMarks(0.0)
            .options(buildOptions(q))
            .selectedOptionKeys(selected)
            .markedForReview(answer.getMarkedForReview())
            .build();
    }

    private List<TestOptionResponse> buildOptions(SheetQuestion q) {
        return List.of(
            sheetOption("A", q.getOptionAEn()),
            sheetOption("B", q.getOptionBEn()),
            sheetOption("C", q.getOptionCEn()),
            sheetOption("D", q.getOptionDEn())
        );
    }

    private TestOptionResponse sheetOption(String key, String text) {
        return TestOptionResponse.builder()
            .optionKey(key)
            .optionText(text)
            .build();
    }

    private TestAnswerReviewResponse toReview(TestAttemptAnswer answer) {
        SheetQuestion q = fromSnapshot(answer.getQuestionSnapshot());
        List<String> selected = answer.getSelectedOptionKeys() == null || answer.getSelectedOptionKeys().isBlank()
            ? List.of()
            : Arrays.asList(answer.getSelectedOptionKeys().split(","));
        String correct = q.getCorrectOption() != null ? q.getCorrectOption().trim().toUpperCase() : "";

        return TestAnswerReviewResponse.builder()
            .sheetQuestionId(q.getId())
            .questionText(q.getQuestionEn())
            .questionType("SINGLE_CORRECT")
            .selectedOptionKeys(selected)
            .correctOptionKeys(correct.isBlank() ? List.of() : List.of(correct))
            .explanation(q.getExplanationEn())
            .isCorrect(answer.getIsCorrect())
            .marksAwarded(answer.getMarksAwarded())
            .marks(MARKS_PER_QUESTION)
            .negativeMarks(0.0)
            .build();
    }
}
