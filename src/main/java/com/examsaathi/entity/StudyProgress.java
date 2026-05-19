package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * StudyProgress — tracks which topics a student has completed.
 * One record per (user, topic) pair.
 */
@Entity
@Table(
    name = "study_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    /** Actual hours student spent on this topic */
    @Builder.Default
    private Double actualHours = 0.0;

    /** Date/time when the topic was marked complete */
    private LocalDateTime completedAt;

    /** Optional personal notes by the student */
    @Column(length = 1000)
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
