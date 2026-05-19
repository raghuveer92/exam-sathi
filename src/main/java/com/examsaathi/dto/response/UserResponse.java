package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Long selectedExamId;
    private String selectedExamName;
    private LocalDateTime targetCompletionDate;
    private Boolean isActive;
    private Integer studyStreakDays;
    private LocalDateTime lastStudyDate;
    private List<String> roles;
    private LocalDateTime createdAt;
}
