package com.examsaathi.dto.response;

import com.examsaathi.entity.Topic.DifficultyLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicResponse {
    private Long id;
    private Long chapterId;
    private String chapterTitle;
    private String title;
    private String description;
    private Double estimatedHours;
    private DifficultyLevel difficultyLevel;
    private Integer orderIndex;
    private Boolean isActive;
    /** Populated when response is for a specific student */
    private Boolean isCompleted;
    private Double actualHours;
    /** NOT_STARTED | IN_PROGRESS | COMPLETED */
    private String status;
    private java.time.LocalDateTime completedAt;
    private java.time.LocalDateTime lastStudiedAt;
}
