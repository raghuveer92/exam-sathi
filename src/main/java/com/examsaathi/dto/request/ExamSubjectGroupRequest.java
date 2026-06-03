package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExamSubjectGroupRequest {

    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotBlank(message = "Group name is required")
    @Size(max = 150)
    private String groupName;

    private Boolean isOptional = false;

    private Integer minSelection = 0;

    private Integer maxSelection = 0;

    private Integer displayOrder = 0;

    private List<Long> subjectIds = new ArrayList<>();
}