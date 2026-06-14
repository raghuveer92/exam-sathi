package com.examsaathi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkProgressUpdateRequest {

    /** Enrollment row id (preferred). */
    private Long userExamId;

    /** Catalog exam id — used when [userExamId] is omitted. */
    private Long examId;

    @Valid
    @NotEmpty(message = "At least one topic update is required")
    private List<BulkTopicProgressItem> topics;
}
