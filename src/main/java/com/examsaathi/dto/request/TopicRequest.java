package com.examsaathi.dto.request;

import com.examsaathi.entity.Topic.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicRequest {

    @NotNull(message = "Chapter ID is required")
    private Long chapterId;

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    @Size(max = 2000)
    private String description;

    @Min(value = 0)
    private Double estimatedHours = 1.0;

    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;

    private Integer orderIndex = 0;

    private Boolean isActive = true;
}
