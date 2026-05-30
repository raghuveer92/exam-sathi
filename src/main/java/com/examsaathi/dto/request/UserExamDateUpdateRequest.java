package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserExamDateUpdateRequest {
    @NotNull
    private LocalDate examDate;
}
