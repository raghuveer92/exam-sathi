package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserExamCreateRequest {
    @NotNull
    private Long examId;

    private LocalDate examDate;

    private List<SubjectGroupSelectionRequest> subjectSelections = new ArrayList<>();
}
