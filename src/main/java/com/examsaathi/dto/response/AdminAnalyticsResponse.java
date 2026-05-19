package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Admin analytics dashboard data */
@Data
@Builder
public class AdminAnalyticsResponse {
    private long totalStudents;
    private long activeStudentsToday;
    private long activeStudentsThisWeek;
    private long totalExams;
    private double averageCompletionPercent;
    private List<DailyActiveUserResponse> dailyActiveUsers;
    private List<TopStudentResponse> topStudents;
}
