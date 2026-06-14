package com.examsaathi.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitTopicTestRequest {
    private Long attemptId;
    private Boolean timedOut;
    private Integer timeSpentSeconds;
    private List<SubmitTestAnswerRequest> answers;
}
