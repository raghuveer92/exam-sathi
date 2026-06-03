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
import java.util.Comparator;
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
    private final ExamSubjectRepository examSubjectRepository;
    private final TopicRepository topicRepository;
    private final StudyProgressRepository progressRepository;
    private final DailyStudyLogRepository studyLogRepository;
    private final UserMapper mapper;

    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserExamResponse> examCards = buildExamCards(userId, user);
        OverallProgressSummary overallSummary = buildOverallSummary(userId, user);

        if (user.getSelectedExam() == null) {
            return buildEmptyDashboard(user, examCards, overallSummary);
        }

        Long examId = user.getSelectedExam().getId();
        List<ExamSubject> examSubjects = examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId);

        // Today's stats
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int todayCompleted = progressRepository.countCompletedByUserAndExamSince(userId, examId, todayStart);
        double todayHours = studyLogRepository.findByUserIdAndExamIdAndStudyDate(userId, examId, LocalDate.now())
            .map(DailyStudyLog::getHoursStudied).orElse(0.0);

        // Subject progress
        List<SubjectProgressResponse> subjectProgress = examSubjects.stream()
            .map(es -> buildSubjectProgress(userId, examId, es))
            .collect(Collectors.toList());

        // Weekly logs (last 7 days)
        List<DailyStudyLog> weeklyLogs = studyLogRepository
            .findByUserIdAndExamIdAndStudyDateAfter(userId, examId, LocalDate.now().minusDays(6));

        // Estimate remaining days
        double avgDailyTopics = calculateAverageDailyTopics(userId);
        Long estimatedDays = avgDailyTopics > 0
            ? (long) Math.ceil((overallSummary.totalTopics - overallSummary.completedTopics) / avgDailyTopics)
            : null;

        return DashboardResponse.builder()
            .user(mapper.toResponse(user))
            .studyStreakDays(user.getStudyStreakDays())
            .overallCompletionPercent(overallSummary.overallPercent)
            .totalTopics(overallSummary.totalTopics)
            .completedTopics(overallSummary.completedTopics)
            .remainingTopics(overallSummary.totalTopics - overallSummary.completedTopics)
            .todayHours(todayHours)
            .todayTopicsCompleted(todayCompleted)
            .estimatedDaysToComplete(estimatedDays)
                .myExams(examCards)
            .subjectProgress(subjectProgress)
            .weeklyLogs(weeklyLogs.stream().map(mapper::toDailyLogResponse).collect(Collectors.toList()))
            .build();
    }

    private SubjectProgressResponse buildSubjectProgress(Long userId, Long examId, ExamSubject examSubject) {
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

    private DashboardResponse buildEmptyDashboard(User user, List<UserExamResponse> examCards, OverallProgressSummary overallSummary) {
        return DashboardResponse.builder()
            .user(mapper.toResponse(user))
            .studyStreakDays(user.getStudyStreakDays())
            .overallCompletionPercent(overallSummary.overallPercent)
            .totalTopics(overallSummary.totalTopics)
            .completedTopics(overallSummary.completedTopics)
            .remainingTopics(overallSummary.totalTopics - overallSummary.completedTopics)
            .myExams(examCards)
            .subjectProgress(new ArrayList<>())
            .weeklyLogs(new ArrayList<>())
            .build();
    }

    private OverallProgressSummary buildOverallSummary(Long userId, User user) {
        if (user.getUserExams() == null || user.getUserExams().isEmpty()) {
            return new OverallProgressSummary(0, 0, 0.0);
        }

        int totalTopics = 0;
        int completedTopics = 0;

        for (UserExam userExam : user.getUserExams()) {
            Long examId = userExam.getExam().getId();
            totalTopics += topicRepository.findByExamId(examId).size();
            completedTopics += progressRepository.countCompletedByUserAndExam(userId, examId);
        }

        double overallPercent = totalTopics > 0
            ? Math.round((completedTopics * 100.0 / totalTopics) * 10.0) / 10.0
            : 0.0;

        return new OverallProgressSummary(totalTopics, completedTopics, overallPercent);
    }

    private record OverallProgressSummary(int totalTopics, int completedTopics, double overallPercent) {
    }

    private List<UserExamResponse> buildExamCards(Long userId, User user) {
        if (user.getUserExams() == null || user.getUserExams().isEmpty()) {
            return new ArrayList<>();
        }

        return user.getUserExams().stream()
            .sorted(Comparator
                .comparing((UserExam ue) -> ue.getExamDate() == null ? LocalDate.MAX : ue.getExamDate())
                .thenComparing(UserExam::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(ue -> {
                Long examId = ue.getExam().getId();
                int totalSubjects = examSubjectRepository.countByExamIdAndIsActiveTrue(examId);
                List<Topic> examTopics = topicRepository.findByExamId(examId);
                int totalTopics = examTopics.size();
                int completedTopics = progressRepository.countCompletedByUserAndExam(userId, examId);
                double progressPercent = totalTopics > 0
                    ? Math.round((completedTopics * 100.0 / totalTopics) * 10.0) / 10.0
                    : 0.0;

                Integer daysLeft = null;
                if (ue.getExamDate() != null) {
                    daysLeft = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), ue.getExamDate());
                }

                return UserExamResponse.builder()
                    .id(ue.getId())
                    .examId(examId)
                    .examName(ue.getExam().getName())
                    .examDate(ue.getExamDate())
                    .daysLeft(daysLeft)
                    .totalSubjects(totalSubjects)
                    .progressPercent(progressPercent)
                    .isActive(ue.getIsActive())
                    .createdAt(ue.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());
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
