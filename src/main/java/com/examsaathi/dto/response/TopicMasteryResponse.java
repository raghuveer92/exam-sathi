package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicMasteryResponse {
    private Long topicId;
    private String topicTitle;
    private String subjectName;
    private String testStatus;
    private Double lastTestScore;
    private Double bestTestScore;
    private Double masteryScore;
    private Integer totalTestsAttempted;
    private String masteryLevel;
    private Boolean canAttempt;
}
