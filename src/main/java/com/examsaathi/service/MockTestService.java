package com.examsaathi.service;

import com.examsaathi.dto.request.SubmitTestAnswerRequest;
import com.examsaathi.dto.request.SubmitTestRequest;
import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MockTestService {

    private final TestAttemptRepository attemptRepository;
    private final TestAttemptAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicTestConfigService topicTestConfigService;

    public TestAttemptResponse startTest(Long userId, Long topicId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));

        TopicTestConfig config = topicTestConfigService.resolveConfig(topicId);
        int numQuestions = config != null ? config.getNumQuestions() : 10;
        int durationMinutes = config != null ? config.getDurationMinutes() : 15;
        Topic.DifficultyLevel difficulty = parseDifficultyFilter(
            config != null ? config.getDifficultyFilter() : "ALL");

        List<Question> pool = questionRepository.findActiveForTopic(topicId, difficulty);
        if (pool.size() < numQuestions) {
            throw new BadRequestException(
                "Not enough questions in bank. Required: " + numQuestions + ", available: " + pool.size());
        }

        Collections.shuffle(pool);
        List<Question> selected = pool.subList(0, numQuestions);
        double maxScore = selected.stream().mapToDouble(Question::getMarks).sum();

        TestAttempt attempt = TestAttempt.builder()
            .user(user)
            .topic(topic)
            .topicTestConfig(config)
            .durationMinutes(durationMinutes)
            .totalQuestions(selected.size())
            .maxScore(maxScore)
            .build();

        for (Question question : selected) {
            TestAttemptAnswer answer = TestAttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .build();
            attempt.getAnswers().add(answer);
        }

        TestAttempt saved = attemptRepository.save(attempt);
        return toInProgressResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public TestAttemptResponse getAttempt(Long userId, Long attemptId, boolean includeReview) {
        TestAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Test attempt", attemptId));
        if (attempt.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS) {
            return toInProgressResponse(attempt, false);
        }
        return toCompletedResponse(attempt, includeReview);
    }

    @Transactional(readOnly = true)
    public List<TestAttemptResponse> getAttemptsForTopic(Long userId, Long topicId) {
        return attemptRepository.findByUserIdAndTopicIdOrderByStartedAtDesc(userId, topicId).stream()
            .map(a -> a.getStatus() == TestAttempt.AttemptStatus.IN_PROGRESS
                ? toInProgressResponse(a, false)
                : toCompletedResponse(a, false))
            .toList();
    }

    public TestAttemptResponse submitTest(Long userId, Long attemptId, SubmitTestRequest request) {
        TestAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Test attempt", attemptId));
        if (attempt.getStatus() != TestAttempt.AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Test already submitted");
        }

        Map<Long, SubmitTestAnswerRequest> answerMap = Optional.ofNullable(request.getAnswers())
            .orElse(Collections.emptyList())
            .stream()
            .filter(a -> a.getQuestionId() != null)
            .collect(Collectors.toMap(SubmitTestAnswerRequest::getQuestionId, a -> a, (a, b) -> b));

        int correct = 0;
        int incorrect = 0;
        int skipped = 0;
        double score = 0;

        List<TestAttemptAnswer> answers = answerRepository.findByAttemptIdWithQuestions(attemptId);
        for (TestAttemptAnswer answer : answers) {
            SubmitTestAnswerRequest submitted = answerMap.get(answer.getQuestion().getId());
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

            boolean isCorrect = gradeAnswer(answer.getQuestion(), answer.getSelectedOptionKeys());
            answer.setIsCorrect(isCorrect);
            if (isCorrect) {
                correct++;
                answer.setMarksAwarded(answer.getQuestion().getMarks());
                score += answer.getQuestion().getMarks();
            } else {
                incorrect++;
                answer.setMarksAwarded(-answer.getQuestion().getNegativeMarks());
                score -= answer.getQuestion().getNegativeMarks();
            }
        }

        answerRepository.saveAll(answers);

        attempt.setCorrectCount(correct);
        attempt.setIncorrectCount(incorrect);
        attempt.setSkippedCount(skipped);
        attempt.setScore(Math.max(score, 0));
        attempt.setPercentage(attempt.getMaxScore() > 0
            ? Math.round((attempt.getScore() / attempt.getMaxScore()) * 1000.0) / 10.0
            : 0.0);
        attempt.setTimeSpentSeconds(request.getTimeSpentSeconds());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(Boolean.TRUE.equals(request.getTimedOut())
            ? TestAttempt.AttemptStatus.TIMED_OUT
            : TestAttempt.AttemptStatus.SUBMITTED);

        return toCompletedResponse(attemptRepository.save(attempt), true);
    }

    @Transactional(readOnly = true)
    public TestPerformanceResponse getPerformance(Long userId) {
        List<TestAttempt> completed = attemptRepository.findCompletedByUserId(userId);

        long total = completed.size();
        double avg = completed.stream().mapToDouble(TestAttempt::getPercentage).average().orElse(0);
        double highest = completed.stream().mapToDouble(TestAttempt::getPercentage).max().orElse(0);

        Map<Long, List<TestAttempt>> byTopic = completed.stream()
            .collect(Collectors.groupingBy(a -> a.getTopic().getId()));

        List<TestPerformanceResponse.TopicPerformanceSummary> summaries = byTopic.entrySet().stream()
            .map(entry -> {
                Topic topic = entry.getValue().get(0).getTopic();
                double topicAvg = entry.getValue().stream().mapToDouble(TestAttempt::getPercentage).average().orElse(0);
                return TestPerformanceResponse.TopicPerformanceSummary.builder()
                    .topicId(entry.getKey())
                    .topicTitle(topic.getTitle())
                    .subjectName(topic.getChapter().getSubject().getName())
                    .averagePercent(Math.round(topicAvg * 10.0) / 10.0)
                    .attemptCount(entry.getValue().size())
                    .build();
            })
            .sorted(Comparator.comparing(TestPerformanceResponse.TopicPerformanceSummary::getAveragePercent))
            .toList();

        List<TestPerformanceResponse.TopicPerformanceSummary> weak = summaries.stream()
            .limit(5)
            .toList();
        List<TestPerformanceResponse.TopicPerformanceSummary> strong = summaries.stream()
            .sorted(Comparator.comparing(TestPerformanceResponse.TopicPerformanceSummary::getAveragePercent).reversed())
            .limit(5)
            .toList();

        List<TestPerformanceResponse.TestAttemptSummary> recent = completed.stream()
            .limit(10)
            .map(a -> TestPerformanceResponse.TestAttemptSummary.builder()
                .attemptId(a.getId())
                .topicId(a.getTopic().getId())
                .topicTitle(a.getTopic().getTitle())
                .percentage(a.getPercentage())
                .submittedAt(a.getSubmittedAt())
                .build())
            .toList();

        return TestPerformanceResponse.builder()
            .totalTestsAttempted(total)
            .averageScorePercent(Math.round(avg * 10.0) / 10.0)
            .highestScorePercent(Math.round(highest * 10.0) / 10.0)
            .weakTopics(weak)
            .strongTopics(strong)
            .recentAttempts(recent)
            .build();
    }

    @Transactional(readOnly = true)
    public TopicTestConfigResponse getTopicTestInfo(Long topicId) {
        return topicTestConfigService.getByTopicId(topicId);
    }

    private boolean gradeAnswer(Question question, String selectedKeysCsv) {
        Set<String> selected = Arrays.stream(selectedKeysCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        Set<String> correct = question.getOptions().stream()
            .filter(QuestionOption::getIsCorrect)
            .map(QuestionOption::getOptionKey)
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        return selected.equals(correct);
    }

    private TestAttemptResponse toInProgressResponse(TestAttempt attempt, boolean fresh) {
        List<TestAttemptAnswer> answers = answerRepository.findByAttemptIdWithQuestions(attempt.getId());
        return TestAttemptResponse.builder()
            .id(attempt.getId())
            .topicId(attempt.getTopic().getId())
            .topicTitle(attempt.getTopic().getTitle())
            .status(attempt.getStatus().name())
            .startedAt(attempt.getStartedAt())
            .durationMinutes(attempt.getDurationMinutes())
            .totalQuestions(attempt.getTotalQuestions())
            .questions(answers.stream().map(a -> toQuestionResponse(a, false)).toList())
            .build();
    }

    private TestAttemptResponse toCompletedResponse(TestAttempt attempt, boolean includeReview) {
        List<TestAttemptAnswer> answers = answerRepository.findByAttemptIdWithQuestions(attempt.getId());
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
            .correctCount(attempt.getCorrectCount())
            .incorrectCount(attempt.getIncorrectCount())
            .skippedCount(attempt.getSkippedCount())
            .score(attempt.getScore())
            .maxScore(attempt.getMaxScore())
            .percentage(attempt.getPercentage())
            .review(includeReview ? answers.stream().map(this::toReview).toList() : null)
            .build();
    }

    private TestQuestionResponse toQuestionResponse(TestAttemptAnswer answer, boolean showCorrect) {
        Question q = answer.getQuestion();
        List<String> selected = answer.getSelectedOptionKeys() == null || answer.getSelectedOptionKeys().isBlank()
            ? List.of()
            : Arrays.asList(answer.getSelectedOptionKeys().split(","));
        return TestQuestionResponse.builder()
            .questionId(q.getId())
            .questionText(q.getQuestionText())
            .questionType(q.getQuestionType().name())
            .marks(q.getMarks())
            .negativeMarks(q.getNegativeMarks())
            .options(q.getOptions().stream().map(o -> TestOptionResponse.builder()
                .id(o.getId())
                .optionKey(o.getOptionKey())
                .optionText(o.getOptionText())
                .build()).toList())
            .selectedOptionKeys(selected)
            .markedForReview(answer.getMarkedForReview())
            .build();
    }

    private TestAnswerReviewResponse toReview(TestAttemptAnswer answer) {
        Question q = answer.getQuestion();
        List<String> selected = answer.getSelectedOptionKeys() == null || answer.getSelectedOptionKeys().isBlank()
            ? List.of()
            : Arrays.asList(answer.getSelectedOptionKeys().split(","));
        List<String> correct = q.getOptions().stream()
            .filter(QuestionOption::getIsCorrect)
            .map(QuestionOption::getOptionKey)
            .toList();
        return TestAnswerReviewResponse.builder()
            .questionId(q.getId())
            .questionText(q.getQuestionText())
            .questionType(q.getQuestionType().name())
            .selectedOptionKeys(selected)
            .correctOptionKeys(correct)
            .explanation(q.getExplanation())
            .isCorrect(answer.getIsCorrect())
            .marksAwarded(answer.getMarksAwarded())
            .marks(q.getMarks())
            .negativeMarks(q.getNegativeMarks())
            .build();
    }

    private static Topic.DifficultyLevel parseDifficultyFilter(String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) {
            return null;
        }
        return Topic.DifficultyLevel.valueOf(filter.trim().toUpperCase());
    }
}
