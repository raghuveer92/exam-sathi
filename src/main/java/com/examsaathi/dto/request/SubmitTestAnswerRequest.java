package com.examsaathi.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitTestAnswerRequest {
    private Long questionId;
    private List<String> selectedOptionKeys;
    private Boolean markedForReview;
}
