package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestPerformanceResponse {
    private long totalTestsAttempted;
    private Double averageScorePercent;
    private Double highestScorePercent;
    private List<TopicPerformanceSummary> weakTopics;
    private List<TopicPerformanceSummary> strongTopics;
    private List<TestAttemptSummary> recentAttempts;

    @Data
    @Builder
    public static class TopicPerformanceSummary {
        private Long topicId;
        private String topicTitle;
        private String subjectName;
        private Double averagePercent;
        private long attemptCount;
    }

    @Data
    @Builder
    public static class TestAttemptSummary {
        private Long attemptId;
        private Long topicId;
        private String topicTitle;
        private Double percentage;
        private java.time.LocalDateTime submittedAt;
    }
}
