package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "test_attempt_answers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Comma-separated option keys e.g. A,C */
    @Column(name = "selected_option_keys", length = 100)
    private String selectedOptionKeys;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "marks_awarded", nullable = false)
    @Builder.Default
    private Double marksAwarded = 0.0;

    @Column(name = "marked_for_review", nullable = false)
    @Builder.Default
    private Boolean markedForReview = false;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
