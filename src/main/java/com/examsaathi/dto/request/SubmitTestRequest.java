package com.examsaathi.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitTestRequest {
    private Boolean timedOut;
    private Integer timeSpentSeconds;
    private List<SubmitTestAnswerRequest> answers;
}
