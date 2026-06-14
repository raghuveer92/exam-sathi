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
    private Integer questionCount;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer wrongCount;
    private Integer skippedCount;
    private Integer unansweredCount;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private Double accuracy;
    private String masteryLevel;
    private Long previousAttemptId;
    private Double previousPercentage;
    private Double improvementScore;
    private List<TestQuestionResponse> questions;
    private List<TestAnswerReviewResponse> review;
}
