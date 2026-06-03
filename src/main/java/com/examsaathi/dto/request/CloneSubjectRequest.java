package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloneSubjectRequest {

    @NotNull(message = "Target exam ID is required")
    private Long targetExamId;

    private Integer displayOrder;

    private Boolean isActive;

    private Long groupId;
}