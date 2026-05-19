package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopStudentResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String examName;
    private double completionPercent;
    private int studyStreakDays;
    private double totalHoursStudied;
}
