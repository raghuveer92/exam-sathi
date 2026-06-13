package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin analytics service providing platform-wide statistics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final StudyProgressRepository progressRepository;
    private final DailyStudyLogRepository studyLogRepository;
    private final TopicRepository topicRepository;
    private final UserMapper mapper;

    @Cacheable(value = CacheNames.ANALYTICS, key = "'admin'")
    public AdminAnalyticsResponse getAdminAnalytics() {
        long totalStudents = userRepository.countStudents();
        long activeToday = studyLogRepository.countDailyActiveUsers(LocalDate.now());
        long activeThisWeek = userRepository.countActiveStudents(
            LocalDate.now().minusDays(7).atStartOfDay());
        long totalExams = examRepository.count();

        // Daily active users for last 30 days
        List<DailyActiveUserResponse> dauList = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = studyLogRepository.countDailyActiveUsers(date);
            dauList.add(DailyActiveUserResponse.builder().date(date).count(count).build());
        }

        // Top 10 students by completion
        List<TopStudentResponse> topStudents = buildTopStudents();

        // Average completion across all students with selected exam
        double avgCompletion = calculateAverageCompletion();

        return AdminAnalyticsResponse.builder()
            .totalStudents(totalStudents)
            .activeStudentsToday(activeToday)
            .activeStudentsThisWeek(activeThisWeek)
            .totalExams(totalExams)
            .averageCompletionPercent(avgCompletion)
            .dailyActiveUsers(dauList)
            .topStudents(topStudents)
            .build();
    }

    private List<TopStudentResponse> buildTopStudents() {
        return userRepository.findAll().stream()
            .filter(u -> u.getSelectedExam() != null)
            .map(u -> {
                Long examId = u.getSelectedExam().getId();
                int total = topicRepository.findByExamId(examId).size();
                int completed = progressRepository.countCompletedByUserAndExam(u.getId(), examId);
                double percent = total > 0 ? (completed * 100.0 / total) : 0.0;
                double totalHours = studyLogRepository.sumHoursInRange(
                    u.getId(), LocalDate.now().minusDays(29), LocalDate.now());
                return TopStudentResponse.builder()
                    .userId(u.getId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .avatarUrl(u.getAvatarUrl())
                    .examName(u.getSelectedExam().getName())
                    .completionPercent(Math.round(percent * 10.0) / 10.0)
                    .studyStreakDays(u.getStudyStreakDays())
                    .totalHoursStudied(totalHours)
                    .build();
            })
            .sorted(Comparator.comparingDouble(TopStudentResponse::getCompletionPercent).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private double calculateAverageCompletion() {
        List<User> studentsWithExam = userRepository.findAll().stream()
            .filter(u -> u.getSelectedExam() != null).toList();
        if (studentsWithExam.isEmpty()) return 0.0;

        double sum = studentsWithExam.stream().mapToDouble(u -> {
            int total = topicRepository.findByExamId(u.getSelectedExam().getId()).size();
            int completed = progressRepository
                .countCompletedByUserAndExam(u.getId(), u.getSelectedExam().getId());
            return total > 0 ? (completed * 100.0 / total) : 0.0;
        }).sum();

        return Math.round((sum / studentsWithExam.size()) * 10.0) / 10.0;
    }
}
