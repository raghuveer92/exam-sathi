package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudyProgressSyncItem {
    private Long id;
    private Long topicId;
    private Long subjectId;
    private Long examId;
    private Boolean isCompleted;
    private Double actualHours;
    private String status;
    private String notes;
    private String testStatus;
    private Double lastTestScore;
    private Double bestTestScore;
    private Double masteryScore;
    private Integer totalTestsAttempted;
    private LocalDateTime completedAt;
    private LocalDateTime lastStudiedAt;
    private LocalDateTime updatedAt;
}
