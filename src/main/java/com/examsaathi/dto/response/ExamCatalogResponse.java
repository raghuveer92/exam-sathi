package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamCatalogResponse {
    private List<ExamCategoryResponse> categories;
    private List<ExamResponse> featuredExams;
    private List<ExamResponse> recommendedExams;
}
