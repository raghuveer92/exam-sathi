package com.examsaathi.service;

import com.examsaathi.config.CacheKeyBuilder;
import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.ProgressUpdateRequest;
import com.examsaathi.dto.request.StudyLogRequest;
import com.examsaathi.dto.request.SyncPushItem;
import com.examsaathi.dto.request.SyncPushRequest;
import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final ExamCategoryRepository categoryRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final UserExamRepository userExamRepository;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final StudyProgressService studyProgressService;
    private final UserMapper mapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(
        value = CacheNames.SYNC_CATALOG,
        key = "T(com.examsaathi.config.CacheKeyBuilder).syncCatalog(#since, #examIds)",
        unless = "#since != null"
    )
    public SyncCatalogResponse getCatalogSync(LocalDateTime since, List<Long> examIds) {
        List<Long> scopedExamIds = normalizeExamIds(examIds);
        boolean fullSync = since == null;

        SyncCatalogResponse response = scopedExamIds.isEmpty()
            ? buildFullCatalog(since)
            : buildExamScopedCatalog(since, scopedExamIds);

        return response;
    }

    private SyncCatalogResponse buildFullCatalog(LocalDateTime since) {
        boolean fullSync = since == null;
        LocalDateTime serverTime = LocalDateTime.now();

        List<ExamCategory> categories = fullSync
            ? categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
            : categoryRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since);

        List<Exam> exams = fullSync
            ? examRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
            : examRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since);

        List<Subject> subjects = fullSync
            ? subjectRepository.findByIsActiveTrueOrderByNameAsc()
            : subjectRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since);

        List<Chapter> chapters = fullSync
            ? chapterRepository.findByIsActiveTrueOrderByUpdatedAtAsc()
            : chapterRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since);

        List<Topic> topics = fullSync
            ? topicRepository.findByIsActiveTrueOrderByUpdatedAtAsc()
            : topicRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since);

        return toCatalogResponse(serverTime, fullSync, categories, exams, subjects, chapters, topics, null);
    }

    private SyncCatalogResponse buildExamScopedCatalog(LocalDateTime since, List<Long> examIds) {
        LocalDateTime serverTime = LocalDateTime.now();
        boolean fullSync = since == null;

        ExamScopeData scope = loadExamScopeData(examIds);

        List<ExamCategory> categories;
        List<Exam> exams;
        List<Subject> subjects;
        List<Chapter> chapters;
        List<Topic> topics;

        if (fullSync) {
            exams = scope.exams();
            categories = scope.categories();
            subjects = scope.subjects();
            chapters = scope.chapters();
            topics = scope.topics();
        } else {
            Set<Long> subjectIds = scope.subjectIds();
            Set<Long> topicIds = scope.topicIds();
            Set<Long> categoryIds = scope.categoryIds();

            categories = categoryRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .filter(c -> categoryIds.contains(c.getId()))
                .collect(Collectors.toList());
            exams = examRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .filter(e -> examIds.contains(e.getId()))
                .collect(Collectors.toList());
            subjects = subjectRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .filter(s -> subjectIds.contains(s.getId()))
                .collect(Collectors.toList());
            chapters = chapterRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .filter(c -> subjectIds.contains(c.getSubject().getId()))
                .collect(Collectors.toList());
            topics = topicRepository.findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .filter(t -> topicIds.contains(t.getId()))
                .collect(Collectors.toList());
        }

        return toCatalogResponse(
            serverTime,
            fullSync,
            categories,
            exams,
            subjects,
            chapters,
            topics,
            scope.topicIds()
        );
    }

    private ExamScopeData loadExamScopeData(List<Long> examIds) {
        List<Exam> exams = examRepository.findActiveByIdInWithCategory(examIds);
        if (exams.isEmpty()) {
            return new ExamScopeData(List.of(), List.of(), List.of(), List.of(), List.of(), Set.of(), Set.of(), Set.of());
        }

        Map<Long, Subject> subjectById = new LinkedHashMap<>();
        Map<Long, Topic> topicById = new LinkedHashMap<>();

        for (Long examId : examIds) {
            for (Subject subject : subjectRepository.findActiveByExamIdOrderByDisplayOrderAsc(examId)) {
                subjectById.putIfAbsent(subject.getId(), subject);
            }
            for (Topic topic : topicRepository.findByExamId(examId)) {
                topicById.putIfAbsent(topic.getId(), topic);
            }
        }

        List<Long> subjectIds = new ArrayList<>(subjectById.keySet());
        List<Chapter> chapters = subjectIds.isEmpty()
            ? List.of()
            : chapterRepository.findBySubjectIdInAndIsActiveTrueOrderByOrderIndexAsc(subjectIds);

        Set<Long> categoryIds = exams.stream()
            .map(e -> e.getCategory().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ExamCategory> categories = categoryRepository.findAllById(categoryIds).stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .sorted(Comparator.comparing(ExamCategory::getDisplayOrder).thenComparing(ExamCategory::getName))
            .collect(Collectors.toList());

        return new ExamScopeData(
            exams,
            categories,
            new ArrayList<>(subjectById.values()),
            chapters,
            new ArrayList<>(topicById.values()),
            new LinkedHashSet<>(subjectIds),
            new LinkedHashSet<>(topicById.keySet()),
            categoryIds
        );
    }

    private SyncCatalogResponse toCatalogResponse(
        LocalDateTime serverTime,
        boolean fullSync,
        List<ExamCategory> categories,
        List<Exam> exams,
        List<Subject> subjects,
        List<Chapter> chapters,
        List<Topic> topics,
        Set<Long> scopedTopicIds
    ) {
        List<Long> mockTestTopicIds = resolveMockTestTopicIds(scopedTopicIds);

        return SyncCatalogResponse.builder()
            .serverTime(serverTime)
            .fullSync(fullSync)
            .categories(categories.stream().map(this::toCategoryResponse).collect(Collectors.toList()))
            .exams(exams.stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList()))
            .subjects(subjects.stream().map(s -> mapper.toSubjectResponse(s, false)).collect(Collectors.toList()))
            .chapters(chapters.stream().map(c -> mapper.toChapterResponse(c, false)).collect(Collectors.toList()))
            .topics(topics.stream().map(mapper::toTopicResponse).collect(Collectors.toList()))
            .mockTestTopicIds(mockTestTopicIds)
            .build();
    }

    private List<Long> resolveMockTestTopicIds(Set<Long> scopedTopicIds) {
        List<Long> ready = questionRepository.findReadyMockTestTopicIds();
        if (scopedTopicIds == null || scopedTopicIds.isEmpty()) {
            return ready;
        }
        return ready.stream()
            .filter(scopedTopicIds::contains)
            .collect(Collectors.toList());
    }

    private static List<Long> normalizeExamIds(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    private record ExamScopeData(
        List<Exam> exams,
        List<ExamCategory> categories,
        List<Subject> subjects,
        List<Chapter> chapters,
        List<Topic> topics,
        Set<Long> subjectIds,
        Set<Long> topicIds,
        Set<Long> categoryIds
    ) {}

    @Transactional(readOnly = true)
    public SyncBundleResponse getBundleSync(Long userId, LocalDateTime since) {
        LocalDateTime serverTime = LocalDateTime.now();
        boolean fullSync = since == null;

        User user = userRepository.findById(userId).orElseThrow();
        List<UserExam> userExams = userExamRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<UserExamResponse> myExams = userExams.stream()
            .map(mapper::toUserExamResponse)
            .collect(Collectors.toList());

        List<StudyProgress> changedProgress = fullSync
            ? studyProgressRepository.findByUserId(userId)
            : studyProgressRepository.findByUserIdAndUpdatedAtAfter(userId, since);

        boolean needsDashboard = fullSync
            || (user.getUpdatedAt() != null && user.getUpdatedAt().isAfter(since))
            || !changedProgress.isEmpty();

        DashboardResponse dashboard = needsDashboard
            ? dashboardService.getDashboard(userId)
            : null;

        Map<Long, List<SubjectProgressResponse>> progressByExam = new LinkedHashMap<>();
        if (fullSync) {
            for (UserExam ue : userExams) {
                progressByExam.put(
                    ue.getExam().getId(),
                    studyProgressService.getSubjectProgress(userId, ue.getExam().getId())
                );
            }
        } else if (!changedProgress.isEmpty()) {
            Set<Long> examIds = changedProgress.stream()
                .map(sp -> sp.getUserExam().getExam().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
            for (Long examId : examIds) {
                progressByExam.put(examId, studyProgressService.getSubjectProgress(userId, examId));
            }
        }

        List<StudyProgressSyncItem> progressItems = changedProgress.stream()
            .map(this::toProgressSyncItem)
            .collect(Collectors.toList());

        return SyncBundleResponse.builder()
            .serverTime(serverTime)
            .fullSync(fullSync)
            .dashboard(dashboard)
            .myExams(myExams)
            .subjectProgressByExamId(progressByExam)
            .changedProgress(progressItems)
            .build();
    }

    @Transactional
    public void pushOfflineChanges(Long userId, SyncPushRequest request) {
        for (SyncPushItem item : request.getItems()) {
            try {
                applyPushItem(userId, item);
            } catch (Exception e) {
                log.warn("Failed to apply sync item type={} clientId={}: {}",
                    item.getType(), item.getClientId(), e.getMessage());
            }
        }
    }

    private void applyPushItem(Long userId, SyncPushItem item) {
        Map<String, Object> payload = item.getPayload() != null ? item.getPayload() : Map.of();
        switch (item.getType()) {
            case "TOPIC_PROGRESS" -> {
                ProgressUpdateRequest req = objectMapper.convertValue(payload, ProgressUpdateRequest.class);
                studyProgressService.updateProgress(userId, req);
            }
            case "LOG_STUDY" -> {
                StudyLogRequest req = objectMapper.convertValue(payload, StudyLogRequest.class);
                studyProgressService.logStudyHours(userId, req);
            }
            case "STUDY_HOURS" -> {
                Double hours = payload.get("dailyTargetHours") instanceof Number n
                    ? n.doubleValue() : null;
                if (hours != null) {
                    User user = userRepository.findById(userId).orElseThrow();
                    user.setDailyTargetHours(hours);
                    user.setWeeklyTargetHours(hours * 7);
                    userRepository.save(user);
                    userExamRepository.findByUserIdAndIsActiveTrue(userId).ifPresent(active -> {
                        active.setDailyTargetHours(hours);
                        active.setWeeklyTargetHours(Math.round(hours * 7 * 10.0) / 10.0);
                        userExamRepository.save(active);
                    });
                }
            }
            default -> log.warn("Unknown sync push type: {}", item.getType());
        }
    }

    private ExamCategoryResponse toCategoryResponse(ExamCategory cat) {
        return ExamCategoryResponse.builder()
            .id(cat.getId())
            .name(cat.getName())
            .description(cat.getDescription())
            .icon(cat.getIcon())
            .displayOrder(cat.getDisplayOrder())
            .isActive(cat.getIsActive())
            .build();
    }

    private StudyProgressSyncItem toProgressSyncItem(StudyProgress sp) {
        return StudyProgressSyncItem.builder()
            .id(sp.getId())
            .topicId(sp.getTopic().getId())
            .subjectId(sp.getTopic().getChapter().getSubject().getId())
            .examId(sp.getUserExam().getExam().getId())
            .isCompleted(sp.getIsCompleted())
            .actualHours(sp.getActualHours())
            .status(sp.getStatus() != null ? sp.getStatus().name() : null)
            .notes(sp.getNotes())
            .completedAt(sp.getCompletedAt())
            .lastStudiedAt(sp.getLastStudiedAt())
            .updatedAt(sp.getUpdatedAt())
            .build();
    }
}
