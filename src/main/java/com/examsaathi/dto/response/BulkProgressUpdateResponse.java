package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkProgressUpdateResponse {
    private int updatedCount;
    private int newlyCompletedCount;
    private List<SubjectProgressResponse> subjectProgress;
}
