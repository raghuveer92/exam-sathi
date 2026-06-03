package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestAnswerReviewResponse {
    private Long questionId;
    private String questionText;
    private String questionType;
    private List<String> selectedOptionKeys;
    private List<String> correctOptionKeys;
    private String explanation;
    private Boolean isCorrect;
    private Double marksAwarded;
    private Double marks;
    private Double negativeMarks;
}
