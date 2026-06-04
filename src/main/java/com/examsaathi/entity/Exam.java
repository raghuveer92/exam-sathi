package com.examsaathi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Exam entity — CBSE 10th, CBSE 12th, NEET, JEE, UPSC, SSC etc.
 */
@Entity
@Table(name = "exams")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "short_description", length = 200)
    private String shortDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExamCategory category;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "difficulty_level", length = 30)
    private String difficultyLevel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean popular = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "featured_order", nullable = false)
    @Builder.Default
    private Integer featuredOrder = 0;

    /** Short display code e.g. CBSE10, NEET, JEE_MAIN */
    @Column(length = 30)
    private String code;

    @Column(length = 500)
    private String iconUrl;

    /** Hex color e.g. #6C63FF */
    @Column(length = 10)
    private String colorCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamSubject> examSubjects = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
