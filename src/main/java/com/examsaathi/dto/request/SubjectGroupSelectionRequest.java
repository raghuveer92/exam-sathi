package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubjectGroupSelectionRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    private List<Long> subjectIds = new ArrayList<>();
}