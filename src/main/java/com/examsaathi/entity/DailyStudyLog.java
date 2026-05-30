package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DailyStudyLog — records hours studied each day per student.
 * Used for analytics, streak calculation and study charts.
 */
@Entity
@Table(
    name = "daily_study_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "exam_id", "study_date"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @Column(nullable = false)
    private LocalDate studyDate;

    /** Total hours studied on this date */
    @Column(nullable = false)
    @Builder.Default
    private Double hoursStudied = 0.0;

    /** Number of topics completed on this date */
    @Builder.Default
    private Integer topicsCompleted = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
