package com.examsaathi.service;

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
    private final StudyProgressRepository studyProgressRepository;
    private final UserExamRepository userExamRepository;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final StudyProgressService studyProgressService;
    private final UserMapper mapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SyncCatalogResponse getCatalogSync(LocalDateTime since) {
        LocalDateTime serverTime = LocalDateTime.now();
        boolean fullSync = since == null;

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

        return SyncCatalogResponse.builder()
            .serverTime(serverTime)
            .fullSync(fullSync)
            .categories(categories.stream().map(this::toCategoryResponse).collect(Collectors.toList()))
            .exams(exams.stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList()))
            .subjects(subjects.stream().map(s -> mapper.toSubjectResponse(s, false)).collect(Collectors.toList()))
            .chapters(chapters.stream().map(c -> mapper.toChapterResponse(c, false)).collect(Collectors.toList()))
            .topics(topics.stream().map(mapper::toTopicResponse).collect(Collectors.toList()))
            .build();
    }

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
