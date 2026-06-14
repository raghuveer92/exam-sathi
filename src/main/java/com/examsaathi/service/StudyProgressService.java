package com.examsaathi.service;

import com.examsaathi.dto.request.BulkProgressUpdateRequest;
import com.examsaathi.dto.request.BulkTopicProgressItem;
import com.examsaathi.dto.request.StudyLogRequest;
import com.examsaathi.dto.request.ProgressUpdateRequest;
import com.examsaathi.dto.response.BulkProgressUpdateResponse;
import com.examsaathi.dto.response.ChapterWithProgressResponse;
import com.examsaathi.dto.response.DailyStudyLogResponse;
import com.examsaathi.dto.response.SubjectDetailResponse;
import com.examsaathi.dto.response.SubjectProgressResponse;
import com.examsaathi.dto.response.TopicResponse;
import com.examsaathi.entity.*;
import com.examsaathi.config.CacheNames;
import com.examsaathi.util.GroupedCountHelper;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheEvictionService cacheEvictionService;

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

        int newlyCompleted = applyTopicProgressFields(
            progress,
            request.getIsCompleted(),
            request.getActualHours(),
            request.getNotes());

        progressRepository.save(progress);

        if (newlyCompleted > 0) {
            updateDailyLogTopicCount(userId, LocalDate.now(), activeUserExam.getExam().getId(), newlyCompleted);
        }
        updateStreak(userId);
        cacheEvictionService.evictUserSyncData(userId, activeUserExam.getExam().getId());
    }

    /**
     * Atomically update many topics for one enrollment. Validates every topic before writing;
     * rolls back the whole batch on any error.
     */
    @Transactional
    public BulkProgressUpdateResponse bulkUpdateProgress(Long userId, BulkProgressUpdateRequest request) {
        UserExam userExam = resolveUserExam(userId, request.getUserExamId(), request.getExamId());
        User user = userRepository.findById(userId).orElseThrow();
        Long examId = userExam.getExam().getId();

        Map<Long, BulkTopicProgressItem> byTopicId = new LinkedHashMap<>();
        for (BulkTopicProgressItem item : request.getTopics()) {
            byTopicId.put(item.getTopicId(), item);
        }
        List<Long> topicIds = new ArrayList<>(byTopicId.keySet());

        Map<Long, Topic> topicsById = topicRepository.findAllById(topicIds).stream()
            .collect(Collectors.toMap(Topic::getId, t -> t));
        if (topicsById.size() != topicIds.size()) {
            List<Long> missing = topicIds.stream()
                .filter(id -> !topicsById.containsKey(id))
                .collect(Collectors.toList());
            throw new BadRequestException("Unknown topic IDs: " + missing);
        }

        for (Long topicId : topicIds) {
            Topic topic = topicsById.get(topicId);
            ensureTopicBelongsToExam(userExam, topic.getChapter().getSubject().getId());
        }

        Map<Long, StudyProgress> existingByTopicId = progressRepository
            .findByUserExamIdAndTopicIdIn(userExam.getId(), topicIds)
            .stream()
            .collect(Collectors.toMap(sp -> sp.getTopic().getId(), sp -> sp));

        int updatedCount = 0;
        int newlyCompleted = 0;
        List<StudyProgress> toSave = new ArrayList<>(byTopicId.size());

        for (BulkTopicProgressItem item : byTopicId.values()) {
            Topic topic = topicsById.get(item.getTopicId());
            StudyProgress progress = existingByTopicId.get(item.getTopicId());
            if (progress == null) {
                progress = StudyProgress.builder()
                    .user(user)
                    .userExam(userExam)
                    .topic(topic)
                    .build();
            }

            newlyCompleted += applyTopicProgressFields(
                progress,
                item.getIsCompleted(),
                item.getActualHours(),
                item.getNotes());
            toSave.add(progress);
            updatedCount++;
        }

        progressRepository.saveAll(toSave);

        if (newlyCompleted > 0) {
            updateDailyLogTopicCount(userId, LocalDate.now(), examId, newlyCompleted);
        }
        updateStreak(userId);
        cacheEvictionService.evictUserSyncData(userId, examId);

        return BulkProgressUpdateResponse.builder()
            .updatedCount(updatedCount)
            .newlyCompletedCount(newlyCompleted)
            .subjectProgress(getSubjectProgress(userId, examId))
            .build();
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

        DailyStudyLogResponse response = mapper.toDailyLogResponse(log);
        cacheEvictionService.evictUserSyncData(userId, activeExamId);
        return response;
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

    /** Get subject-wise progress for a student (cached per user + exam). */
    @Transactional(readOnly = true)
    @Cacheable(
        value = CacheNames.SUBJECT_PROGRESS,
        key = "T(com.examsaathi.config.CacheKeyBuilder).subjectProgress(#userId, #examId)"
    )
    public List<SubjectProgressResponse> getSubjectProgress(Long userId, Long examId) {
        UserExam userExam = userExamRepository.findByUserIdAndExamId(userId, examId)
            .orElseThrow(() -> new IllegalStateException("Exam is not linked to this user"));
        return buildSubjectProgressList(userId, examId, userExam);
    }

    @Transactional(readOnly = true)
    public List<SubjectProgressResponse> buildSubjectProgressList(Long userId, Long examId, UserExam userExam) {
        List<ExamSubjectGroupService.ResolvedExamSubject> resolvedSubjects =
            examSubjectGroupService.resolveVisibleSubjects(userExam);
        return buildSubjectProgressBatch(userId, examId, resolvedSubjects);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<SubjectProgressResponse>> getSubjectProgressByExams(Long userId, List<UserExam> userExams) {
        Map<Long, List<SubjectProgressResponse>> progressByExam = new LinkedHashMap<>();
        for (UserExam userExam : userExams) {
            Long examId = userExam.getExam().getId();
            progressByExam.put(examId, buildSubjectProgressList(userId, examId, userExam));
        }
        return progressByExam;
    }

    private List<SubjectProgressResponse> buildSubjectProgressBatch(
            Long userId,
            Long examId,
            List<ExamSubjectGroupService.ResolvedExamSubject> resolvedSubjects) {
        if (resolvedSubjects.isEmpty()) {
            return List.of();
        }

        List<Long> subjectIds = resolvedSubjects.stream()
            .map(resolved -> resolved.examSubject().getSubject().getId())
            .distinct()
            .toList();

        Map<Long, Integer> topicCounts = GroupedCountHelper.toIntMap(
            topicRepository.countActiveTopicsGroupedBySubjectId(subjectIds));
        Map<Long, Double> estimatedHours = GroupedCountHelper.toDoubleMap(
            topicRepository.sumEstimatedHoursGroupedBySubjectId(subjectIds));
        Map<Long, Integer> completedCounts = GroupedCountHelper.toIntMap(
            progressRepository.countCompletedGroupedBySubjectId(userId, examId, subjectIds));

        return resolvedSubjects.stream()
            .map(resolved -> toSubjectProgressResponse(
                resolved,
                topicCounts,
                completedCounts,
                estimatedHours))
            .collect(Collectors.toList());
    }

    private SubjectProgressResponse toSubjectProgressResponse(
            ExamSubjectGroupService.ResolvedExamSubject resolvedSubject,
            Map<Long, Integer> topicCounts,
            Map<Long, Integer> completedCounts,
            Map<Long, Double> estimatedHours) {
        ExamSubject examSubject = resolvedSubject.examSubject();
        Subject subject = examSubject.getSubject();
        Long subjectId = subject.getId();
        int totalTopics = topicCounts.getOrDefault(subjectId, 0);
        int completed = completedCounts.getOrDefault(subjectId, 0);
        double percent = totalTopics > 0
            ? Math.round((completed * 100.0 / totalTopics) * 10.0) / 10.0
            : 0.0;

        return SubjectProgressResponse.builder()
            .subjectId(subjectId)
            .subjectName(subject.getName())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
            .displayOrder(examSubject.getDisplayOrder())
            .totalTopics(totalTopics)
            .completedTopics(completed)
            .completionPercent(percent)
            .totalEstimatedHours(estimatedHours.getOrDefault(subjectId, 0.0))
            .build();
    }

    private UserExam getActiveUserExam(Long userId) {
        return userExamRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new IllegalStateException("No active exam selected"));
    }

    private UserExam resolveUserExam(Long userId, Long userExamId, Long examId) {
        if (userExamId != null) {
            return userExamRepository.findById(userExamId)
                .filter(ue -> ue.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("UserExam", userExamId));
        }
        if (examId != null) {
            return userExamRepository.findByUserIdAndExamId(userId, examId)
                .orElseThrow(() -> new BadRequestException("Exam is not linked to this user"));
        }
        throw new BadRequestException("userExamId or examId is required");
    }

    /** @return 1 when the topic transitioned to completed, else 0 */
    private int applyTopicProgressFields(
            StudyProgress progress,
            Boolean isCompleted,
            Double actualHours,
            String notes) {
        boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        progress.setIsCompleted(isCompleted);
        progress.setActualHours(actualHours != null ? actualHours : 0.0);
        if (notes != null) {
            progress.setNotes(notes);
        }

        if (Boolean.TRUE.equals(isCompleted)) {
            progress.setStatus(StudyProgress.TopicStatus.COMPLETED);
            if (!wasCompleted) {
                progress.setCompletedAt(LocalDateTime.now());
                return 1;
            }
        } else if (actualHours != null && actualHours > 0) {
            progress.setStatus(StudyProgress.TopicStatus.IN_PROGRESS);
            progress.setLastStudiedAt(LocalDateTime.now());
        } else {
            progress.setStatus(StudyProgress.TopicStatus.NOT_STARTED);
        }
        return 0;
    }

    private void ensureTopicBelongsToExam(UserExam userExam, Long subjectId) {
        if (!examSubjectGroupService.isSubjectVisible(userExam, subjectId)) {
            throw new IllegalStateException("Topic does not belong to the active exam");
        }
    }

    /** Update daily log topic count (supports bulk increments). */
    private void updateDailyLogTopicCount(Long userId, LocalDate date, Long examId, int count) {
        if (count <= 0) {
            return;
        }
        User user = userRepository.findById(userId).orElseThrow();
        Exam examRef = Exam.builder().id(examId).build();
        DailyStudyLog log = findOrCreateDailyLog(user, examRef, date);
        log.setTopicsCompleted((log.getTopicsCompleted() != null ? log.getTopicsCompleted() : 0) + count);
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
