package com.examsaathi.service;

import com.examsaathi.dto.request.StudyLogRequest;
import com.examsaathi.dto.request.ProgressUpdateRequest;
import com.examsaathi.dto.response.DailyStudyLogResponse;
import com.examsaathi.dto.response.SubjectProgressResponse;
import com.examsaathi.entity.*;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final UserRepository userRepository;
    private final UserMapper mapper;

    /** Mark a topic complete or update study hours */
    @Transactional
    public void updateProgress(Long userId, ProgressUpdateRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Topic", request.getTopicId()));

        StudyProgress progress = progressRepository
            .findByUserIdAndTopicId(userId, request.getTopicId())
            .orElseGet(() -> {
                User user = userRepository.findById(userId).orElseThrow();
                return StudyProgress.builder().user(user).topic(topic).build();
            });

        boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        progress.setIsCompleted(request.getIsCompleted());
        progress.setActualHours(request.getActualHours());
        progress.setNotes(request.getNotes());

        if (request.getIsCompleted() && !wasCompleted) {
            progress.setCompletedAt(LocalDateTime.now());
            // Update daily log topics count
            updateDailyLogTopicCount(userId, LocalDate.now());
        }

        progressRepository.save(progress);
        updateStreak(userId);
    }

    /** Add or update daily study log */
    @Transactional
    public DailyStudyLogResponse logStudyHours(Long userId, StudyLogRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        DailyStudyLog log = studyLogRepository
            .findByUserIdAndStudyDate(userId, request.getStudyDate())
            .orElseGet(() -> DailyStudyLog.builder()
                .user(user)
                .studyDate(request.getStudyDate())
                .build());

        log.setHoursStudied(request.getHoursStudied());
        log.setTopicsCompleted(request.getTopicsCompleted());
        studyLogRepository.save(log);

        // Update last study date and streak
        user.setLastStudyDate(request.getStudyDate().atTime(23, 59));
        userRepository.save(user);
        updateStreak(userId);

        return mapper.toDailyLogResponse(log);
    }

    /** Get weekly study logs for a student */
    public List<DailyStudyLogResponse> getWeeklyLogs(Long userId) {
        return studyLogRepository
            .findByUserIdAndStudyDateAfter(userId, LocalDate.now().minusDays(6))
            .stream().map(mapper::toDailyLogResponse).collect(Collectors.toList());
    }

    /** Get subject-wise progress for a student */
    public List<SubjectProgressResponse> getSubjectProgress(Long userId, Long examId) {
        return subjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId)
            .stream()
            .map(s -> buildSubjectProgress(userId, s))
            .collect(Collectors.toList());
    }

    private SubjectProgressResponse buildSubjectProgress(Long userId, Subject subject) {
        int totalTopics = topicRepository.countBySubjectId(subject.getId());
        int completed = progressRepository.countCompletedByUserAndSubject(userId, subject.getId());
        double percent = totalTopics > 0
            ? Math.round((completed * 100.0 / totalTopics) * 10.0) / 10.0
            : 0.0;
        Double totalHours = topicRepository.sumEstimatedHoursBySubjectId(subject.getId());

        return SubjectProgressResponse.builder()
            .subjectId(subject.getId())
            .subjectName(subject.getName())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
            .displayOrder(subject.getDisplayOrder())
            .totalTopics(totalTopics)
            .completedTopics(completed)
            .completionPercent(percent)
            .totalEstimatedHours(totalHours != null ? totalHours : 0.0)
            .build();
    }

    /** Update today's daily log topic count */
    private void updateDailyLogTopicCount(Long userId, LocalDate date) {
        User user = userRepository.findById(userId).orElseThrow();
        DailyStudyLog log = studyLogRepository
            .findByUserIdAndStudyDate(userId, date)
            .orElseGet(() -> DailyStudyLog.builder()
                .user(user).studyDate(date).hoursStudied(0.0).build());
        log.setTopicsCompleted((log.getTopicsCompleted() != null ? log.getTopicsCompleted() : 0) + 1);
        studyLogRepository.save(log);
    }

    /** Recalculate and update streak */
    private void updateStreak(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        boolean studiedToday = studyLogRepository.findByUserIdAndStudyDate(userId, today)
            .map(l -> l.getHoursStudied() > 0 || l.getTopicsCompleted() > 0)
            .orElse(false);

        boolean studiedYesterday = studyLogRepository.findByUserIdAndStudyDate(userId, yesterday)
            .map(l -> l.getHoursStudied() > 0 || l.getTopicsCompleted() > 0)
            .orElse(false);

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
