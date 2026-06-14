package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt {

    public enum AttemptStatus { IN_PROGRESS, SUBMITTED, TIMED_OUT }

    public enum MasteryLevel { BEGINNER, DEVELOPING, PROFICIENT, MASTERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_test_config_id")
    private TopicTestConfig topicTestConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    @Builder.Default
    private Integer incorrectCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private Integer skippedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Double maxScore = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double percentage = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double accuracy = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level", length = 20)
    private MasteryLevel masteryLevel;

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private Integer questionCount = 0;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    private Integer wrongCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    @Builder.Default
    private Integer unansweredCount = 0;

    @Column(name = "previous_attempt_id")
    private Long previousAttemptId;

    @Column(name = "previous_percentage")
    private Double previousPercentage;

    /** Percentage change vs previous attempt (positive = improvement) */
    @Column(name = "improvement_score")
    private Double improvementScore;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestAttemptAnswer> answers = new ArrayList<>();
}
