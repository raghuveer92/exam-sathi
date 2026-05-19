package com.examsaathi.service;

import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the student dashboard response with all progress data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final StudyProgressRepository progressRepository;
    private final DailyStudyLogRepository studyLogRepository;
    private final UserMapper mapper;

    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSelectedExam() == null) {
            return buildEmptyDashboard(user);
        }

        Long examId = user.getSelectedExam().getId();
        List<Subject> subjects = subjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId);

        // Calculate overall topic counts
        List<Topic> allTopics = topicRepository.findByExamId(examId);
        int totalTopics = allTopics.size();
        int completedTopics = progressRepository.countCompletedByUserAndExam(userId, examId);
        double overallPercent = totalTopics > 0
            ? Math.round((completedTopics * 100.0 / totalTopics) * 10.0) / 10.0
            : 0.0;

        // Today's stats
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int todayCompleted = progressRepository.countCompletedSince(userId, todayStart);
        double todayHours = studyLogRepository.findByUserIdAndStudyDate(userId, LocalDate.now())
            .map(DailyStudyLog::getHoursStudied).orElse(0.0);

        // Subject progress
        List<SubjectProgressResponse> subjectProgress = subjects.stream()
            .map(s -> buildSubjectProgress(userId, s))
            .collect(Collectors.toList());

        // Weekly logs (last 7 days)
        List<DailyStudyLog> weeklyLogs = studyLogRepository
            .findByUserIdAndStudyDateAfter(userId, LocalDate.now().minusDays(6));

        // Estimate remaining days
        double avgDailyTopics = calculateAverageDailyTopics(userId);
        Long estimatedDays = avgDailyTopics > 0
            ? (long) Math.ceil((totalTopics - completedTopics) / avgDailyTopics)
            : null;

        return DashboardResponse.builder()
            .user(mapper.toResponse(user))
            .studyStreakDays(user.getStudyStreakDays())
            .overallCompletionPercent(overallPercent)
            .totalTopics(totalTopics)
            .completedTopics(completedTopics)
            .remainingTopics(totalTopics - completedTopics)
            .todayHours(todayHours)
            .todayTopicsCompleted(todayCompleted)
            .estimatedDaysToComplete(estimatedDays)
            .subjectProgress(subjectProgress)
            .weeklyLogs(weeklyLogs.stream().map(mapper::toDailyLogResponse).collect(Collectors.toList()))
            .build();
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

    private DashboardResponse buildEmptyDashboard(User user) {
        return DashboardResponse.builder()
            .user(mapper.toResponse(user))
            .studyStreakDays(user.getStudyStreakDays())
            .overallCompletionPercent(0.0)
            .subjectProgress(new ArrayList<>())
            .weeklyLogs(new ArrayList<>())
            .build();
    }

    private double calculateAverageDailyTopics(Long userId) {
        List<DailyStudyLog> last30Days = studyLogRepository
            .findByUserIdAndStudyDateAfter(userId, LocalDate.now().minusDays(29));
        return last30Days.stream()
            .mapToInt(DailyStudyLog::getTopicsCompleted)
            .average()
            .orElse(0.0);
    }
}
