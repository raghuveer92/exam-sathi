package com.examsaathi.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExamGoalRequest {
    private LocalDate examDate;
    private LocalDate syllabusTargetDate;
}
