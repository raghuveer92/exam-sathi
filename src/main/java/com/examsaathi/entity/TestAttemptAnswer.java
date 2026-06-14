package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_attempt_answers")
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

    /** Legacy DB question reference — nullable for sheet-based attempts */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    /** Sheet row id from Google Sheets (e.g. column "id") */
    @Column(name = "sheet_question_id", length = 50)
    private String sheetQuestionId;

    /** Frozen question payload at attempt time for historical review */
    @Column(name = "question_snapshot", columnDefinition = "TEXT")
    private String questionSnapshot;

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
