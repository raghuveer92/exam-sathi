package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestQuestionResponse {
    private Long questionId;
    private String questionText;
    private String questionType;
    private Double marks;
    private Double negativeMarks;
    private List<TestOptionResponse> options;
    private List<String> selectedOptionKeys;
    private Boolean markedForReview;
}
