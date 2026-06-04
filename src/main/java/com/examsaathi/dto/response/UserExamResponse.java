package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserExamResponse {
    private Long id;
    private Long examId;
    private String examName;
    private LocalDate examDate;
    private Double dailyTargetHours;
    private String experienceLevel;
    private Integer daysLeft;
    private Integer totalSubjects;
    private Double progressPercent;
    private Boolean isActive;
    private List<ExamSubjectGroupResponse> subjectGroups;
    private LocalDateTime createdAt;
}
