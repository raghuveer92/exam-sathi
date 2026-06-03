package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BulkQuestionImportResponse {
    private int totalRows;
    private int imported;
    private int failed;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
