package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class EnrollExamRequest {
    @NotNull
    private Long examId;

    @NotNull
    private LocalDate examDate;

    private LocalDate syllabusTargetDate;

    private Double dailyTargetHours;

    /** BEGINNER, INTERMEDIATE, ADVANCED */
    private String experienceLevel;

    private List<SubjectGroupSelectionRequest> subjectSelections = new ArrayList<>();
}
