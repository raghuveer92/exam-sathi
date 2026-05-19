package com.examsaathi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudyLogRequest {

    @NotNull(message = "Study date is required")
    private LocalDate studyDate;

    @NotNull
    @Min(0)
    private Double hoursStudied;

    @Min(0)
    private Integer topicsCompleted = 0;
}
