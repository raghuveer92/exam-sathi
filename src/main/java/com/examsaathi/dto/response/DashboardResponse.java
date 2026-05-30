package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive dashboard response for the student home screen.
 */
@Data
@Builder
public class DashboardResponse {
    private UserResponse user;
    private int studyStreakDays;
    private double overallCompletionPercent;
    private int totalTopics;
    private int completedTopics;
    private int remainingTopics;
    private double totalEstimatedHours;
    private double completedHours;
    private double todayHours;
    private int todayTopicsCompleted;
    /** Estimated days to completion based on daily average */
    private Long estimatedDaysToComplete;
    private List<UserExamResponse> myExams;
    private List<SubjectProgressResponse> subjectProgress;
    private List<DailyStudyLogResponse> weeklyLogs;
}
