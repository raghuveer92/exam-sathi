package com.examsaathi.service;

import com.examsaathi.dto.request.StudyLogRequest;
import com.examsaathi.dto.request.ProgressUpdateRequest;
import com.examsaathi.dto.response.ChapterWithProgressResponse;
import com.examsaathi.dto.response.DailyStudyLogResponse;
import com.examsaathi.dto.response.SubjectDetailResponse;
import com.examsaathi.dto.response.SubjectProgressResponse;
import com.examsaathi.dto.response.TopicResponse;
import com.examsaathi.entity.*;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Study progress and daily log service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyProgressService {

    private final StudyProgressRepository progressRepository;
    private final DailyStudyLogRepository studyLogRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final UserExamRepository userExamRepository;
    private final ExamSubjectGroupService examSubjectGroupService;
    private final UserMapper mapper;

    /** Mark a topic complete or update study hours */
    @Transactional
    public void updateProgress(Long userId, ProgressUpdateRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Topic", request.getTopicId()));
        UserExam activeUserExam = getActiveUserExam(userId);
        ensureTopicBelongsToExam(activeUserExam, topic.getChapter().getSubject().getId());

        StudyProgress progress = progressRepository
            .findByUserExamIdAndTopicId(activeUserExam.getId(), request.getTopicId())
            .orElseGet(() -> {
                User user = userRepository.findById(userId).orElseThrow();
                return StudyProgress.builder().user(user).userExam(activeUserExam).topic(topic).build();
            });

        boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        progress.setIsCompleted(request.getIsCompleted());
        progress.setActualHours(request.getActualHours());
        progress.setNotes(request.getNotes());

        // Derive and set topic status
        if (Boolean.TRUE.equals(request.getIsCompleted())) {
            progress.setStatus(StudyProgress.TopicStatus.COMPLETED);
            if (!wasCompleted) {
                progress.setCompletedAt(LocalDateTime.now());
                updateDailyLogTopicCount(userId, LocalDate.now(), activeUserExam.getExam().getId());
            }
        } else if (request.getActualHours() != null && request.getActualHours() > 0) {
            progress.setStatus(StudyProgress.TopicStatus.IN_PROGRESS);
            progress.setLastStudiedAt(LocalDateTime.now());
        } else {
            progress.setStatus(StudyProgress.TopicStatus.NOT_STARTED);
        }

        progressRepository.save(progress);
        updateStreak(userId);
    }

    /** Add or update daily study log */
    @Transactional
    public DailyStudyLogResponse logStudyHours(Long userId, StudyLogRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        if (user.getSelectedExam() == null) {
            throw new IllegalStateException("No active exam selected");
        }
        Long activeExamId = user.getSelectedExam().getId();
        Exam activeExamRef = Exam.builder().id(activeExamId).build();

        DailyStudyLog log = findOrCreateDailyLog(user, activeExamRef, request.getStudyDate());

        // Accumulate hours (app sends deltas, not totals)
        double existing = log.getHoursStudied() != null ? log.getHoursStudied() : 0.0;
        log.setHoursStudied(Math.max(0.0, existing + request.getHoursStudied()));
        int existingTopics = log.getTopicsCompleted() != null ? log.getTopicsCompleted() : 0;
        int requestTopics = request.getTopicsCompleted() != null ? request.getTopicsCompleted() : 0;
        log.setTopicsCompleted(Math.max(0, existingTopics + requestTopics));
        studyLogRepository.save(log);

        // Update last study date and streak
        user.setLastStudyDate(request.getStudyDate().atTime(23, 59));
        userRepository.save(user);
        updateStreak(userId);

        return mapper.toDailyLogResponse(log);
    }

    /** Get weekly study logs for a student */
    public List<DailyStudyLogResponse> getWeeklyLogs(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getSelectedExam() == null) {
            return Collections.emptyList();
        }
        return studyLogRepository
            .findByUserIdAndExamIdAndStudyDateAfter(userId, user.getSelectedExam().getId(), LocalDate.now().minusDays(6))
            .stream().map(mapper::toDailyLogResponse).collect(Collectors.toList());
    }

    /** Get full subject detail with chapters, topics and per-user progress */
    @Transactional(readOnly = true)
    public SubjectDetailResponse getSubjectDetail(Long userId, Long subjectId) {
        UserExam activeUserExam = getActiveUserExam(userId);
        ensureTopicBelongsToExam(activeUserExam, subjectId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));

        List<Chapter> chapters = chapterRepository.findBySubjectIdWithTopics(subjectId);

        // Batch-load all progress for this user + subject's topics
        List<Long> allTopicIds = chapters.stream()
            .flatMap(c -> c.getTopics().stream().map(Topic::getId))
            .collect(Collectors.toList());

        Map<Long, StudyProgress> progressMap = allTopicIds.isEmpty()
            ? Collections.emptyMap()
            : progressRepository.findByUserExamIdAndTopicIdIn(activeUserExam.getId(), allTopicIds)
                .stream().collect(Collectors.toMap(sp -> sp.getTopic().getId(), sp -> sp));

        int totalTopics = 0;
        int totalCompleted = 0;
        double totalStudyHours = 0.0;
        List<ChapterWithProgressResponse> chapterResponses = new ArrayList<>();

        for (Chapter chapter : chapters) {
            List<Topic> topics = chapter.getTopics().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .sorted(Comparator.comparing(Topic::getOrderIndex))
                .collect(Collectors.toList());

            int chapterCompleted = 0;
            List<TopicResponse> topicResponses = new ArrayList<>();

            for (Topic topic : topics) {
                StudyProgress sp = progressMap.get(topic.getId());
                boolean isCompleted = sp != null && Boolean.TRUE.equals(sp.getIsCompleted());
                double actualHours = (sp != null && sp.getActualHours() != null) ? sp.getActualHours() : 0.0;

                if (isCompleted) {
                    chapterCompleted++;
                }
                if (actualHours > 0) {
                    totalStudyHours += actualHours;
                }

                String statusStr = (sp != null && sp.getStatus() != null)
                    ? sp.getStatus().name() : "NOT_STARTED";
                LocalDateTime completedAt = (sp != null) ? sp.getCompletedAt() : null;
                LocalDateTime lastStudiedAt = (sp != null) ? sp.getLastStudiedAt() : null;

                topicResponses.add(TopicResponse.builder()
                    .id(topic.getId())
                    .chapterId(chapter.getId())
                    .chapterTitle(chapter.getTitle())
                    .title(topic.getTitle())
                    .description(topic.getDescription())
                    .estimatedHours(topic.getEstimatedHours())
                    .difficultyLevel(topic.getDifficultyLevel())
                    .orderIndex(topic.getOrderIndex())
                    .isActive(topic.getIsActive())
                    .isCompleted(isCompleted)
                    .actualHours(actualHours)
                    .status(statusStr)
                    .completedAt(completedAt)
                    .lastStudiedAt(lastStudiedAt)
                    .build());
            }

            double chapterPercent = topics.isEmpty() ? 0.0
                : Math.round((chapterCompleted * 100.0 / topics.size()) * 10.0) / 10.0;

            chapterResponses.add(ChapterWithProgressResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .orderIndex(chapter.getOrderIndex())
                .totalTopics(topics.size())
                .completedTopics(chapterCompleted)
                .completionPercent(chapterPercent)
                .topics(topicResponses)
                .build());

            totalTopics += topics.size();
            totalCompleted += chapterCompleted;
        }

        double subjectPercent = totalTopics == 0 ? 0.0
            : Math.round((totalCompleted * 100.0 / totalTopics) * 10.0) / 10.0;

        return SubjectDetailResponse.builder()
            .subjectId(subject.getId())
            .subjectName(subject.getName())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
            .totalTopics(totalTopics)
            .completedTopics(totalCompleted)
            .completionPercent(subjectPercent)
            .totalStudyHours(totalStudyHours)
            .chapters(chapterResponses)
            .build();
    }

    /** Get subject-wise progress for a student */
    public List<SubjectProgressResponse> getSubjectProgress(Long userId, Long examId) {
        UserExam userExam = userExamRepository.findByUserIdAndExamId(userId, examId)
            .orElseThrow(() -> new IllegalStateException("Exam is not linked to this user"));
        return examSubjectGroupService.resolveVisibleSubjects(userExam)
            .stream()
            .map(es -> buildSubjectProgress(userId, examId, es))
            .collect(Collectors.toList());
    }

    private SubjectProgressResponse buildSubjectProgress(Long userId, Long examId, ExamSubjectGroupService.ResolvedExamSubject resolvedSubject) {
        ExamSubject examSubject = resolvedSubject.examSubject();
        Subject subject = examSubject.getSubject();
        int totalTopics = topicRepository.countBySubjectId(subject.getId());
        int completed = progressRepository.countCompletedByUserAndExamAndSubject(userId, examId, subject.getId());
        double percent = totalTopics > 0
            ? Math.round((completed * 100.0 / totalTopics) * 10.0) / 10.0
            : 0.0;
        Double totalHours = topicRepository.sumEstimatedHoursBySubjectId(subject.getId());

        return SubjectProgressResponse.builder()
            .subjectId(subject.getId())
            .subjectName(subject.getName())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
                .displayOrder(examSubject.getDisplayOrder())
            .totalTopics(totalTopics)
            .completedTopics(completed)
            .completionPercent(percent)
            .totalEstimatedHours(totalHours != null ? totalHours : 0.0)
            .build();
    }

    private UserExam getActiveUserExam(Long userId) {
        return userExamRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new IllegalStateException("No active exam selected"));
    }

    private void ensureTopicBelongsToExam(UserExam userExam, Long subjectId) {
        if (!examSubjectGroupService.isSubjectVisible(userExam, subjectId)) {
            throw new IllegalStateException("Topic does not belong to the active exam");
        }
    }

    /** Update today's daily log topic count */
    private void updateDailyLogTopicCount(Long userId, LocalDate date, Long examId) {
        User user = userRepository.findById(userId).orElseThrow();
        Exam examRef = Exam.builder().id(examId).build();
        DailyStudyLog log = findOrCreateDailyLog(user, examRef, date);
        log.setTopicsCompleted((log.getTopicsCompleted() != null ? log.getTopicsCompleted() : 0) + 1);
        saveDailyLogHandlingLegacyUniqueConstraint(log);
    }

    private DailyStudyLog findOrCreateDailyLog(User user, Exam examRef, LocalDate studyDate) {
        Long examId = examRef.getId();
        return studyLogRepository
            .findByUserIdAndExamIdAndStudyDate(user.getId(), examId, studyDate)
            .or(() -> studyLogRepository.findByUserIdAndStudyDate(user.getId(), studyDate))
            .map(existing -> {
                if (existing.getExam() == null && examId != null) {
                    existing.setExam(examRef);
                }
                return existing;
            })
            .orElseGet(() -> DailyStudyLog.builder()
                .user(user)
                .exam(examRef)
                .studyDate(studyDate)
                .hoursStudied(0.0)
                .topicsCompleted(0)
                .build());
    }

    private void saveDailyLogHandlingLegacyUniqueConstraint(DailyStudyLog log) {
        try {
            studyLogRepository.save(log);
        } catch (DataIntegrityViolationException ex) {
            DailyStudyLog legacyLog = studyLogRepository
                .findByUserIdAndStudyDate(log.getUser().getId(), log.getStudyDate())
                .orElseThrow(() -> ex);

            if (legacyLog.getExam() == null && log.getExam() != null) {
                legacyLog.setExam(log.getExam());
            }
            legacyLog.setHoursStudied(log.getHoursStudied());
            legacyLog.setTopicsCompleted(log.getTopicsCompleted());
            studyLogRepository.save(legacyLog);
        }
    }

    /** Recalculate and update streak */
    private void updateStreak(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        boolean studiedToday = ((studyLogRepository.sumHoursByUserIdAndStudyDate(userId, today) != null
            ? studyLogRepository.sumHoursByUserIdAndStudyDate(userId, today)
            : 0.0) > 0)
            || ((studyLogRepository.sumTopicsByUserIdAndStudyDate(userId, today) != null
            ? studyLogRepository.sumTopicsByUserIdAndStudyDate(userId, today)
            : 0) > 0);

        boolean studiedYesterday = ((studyLogRepository.sumHoursByUserIdAndStudyDate(userId, yesterday) != null
            ? studyLogRepository.sumHoursByUserIdAndStudyDate(userId, yesterday)
            : 0.0) > 0)
            || ((studyLogRepository.sumTopicsByUserIdAndStudyDate(userId, yesterday) != null
            ? studyLogRepository.sumTopicsByUserIdAndStudyDate(userId, yesterday)
            : 0) > 0);

        if (studiedToday) {
            if (studiedYesterday || user.getStudyStreakDays() == 0) {
                // Increment only once per day
                if (user.getLastStudyDate() == null ||
                        user.getLastStudyDate().toLocalDate().isBefore(today)) {
                    user.setStudyStreakDays(user.getStudyStreakDays() + 1);
                }
            } else {
                user.setStudyStreakDays(1); // Reset to 1 (studied today but not yesterday)
            }
        }
        userRepository.save(user);
    }
}
