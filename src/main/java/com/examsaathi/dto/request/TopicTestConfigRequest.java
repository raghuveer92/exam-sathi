package com.examsaathi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicTestConfigRequest {
    @NotNull
    private Long topicId;

    @NotNull
    @Min(1)
    private Integer numQuestions;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    private String difficultyFilter;

    private Boolean isActive;
}
