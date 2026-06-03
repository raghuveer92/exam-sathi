package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TestAttemptResponse {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer durationMinutes;
    private Integer timeSpentSeconds;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer skippedCount;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private List<TestQuestionResponse> questions;
    private List<TestAnswerReviewResponse> review;
}
