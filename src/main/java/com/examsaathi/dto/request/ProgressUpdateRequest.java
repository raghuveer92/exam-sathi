package com.examsaathi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgressUpdateRequest {

    @NotNull(message = "Topic ID is required")
    private Long topicId;

    @NotNull(message = "isCompleted flag is required")
    private Boolean isCompleted;

    @Min(0)
    private Double actualHours = 0.0;

    private String notes;
}
