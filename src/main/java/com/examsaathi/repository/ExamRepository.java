package com.examsaathi.repository;

import com.examsaathi.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByIsActiveTrueOrderByNameAsc();

    List<Exam> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Exam> findByIsActiveTrueAndCategoryIdOrderByDisplayOrderAscNameAsc(Long categoryId);

    List<Exam> findByIsActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscDisplayOrderAscNameAsc();

    List<Exam> findByIsActiveTrueAndPopularTrueOrderByDisplayOrderAscNameAsc();

    @Query("""
        SELECT e FROM Exam e
        WHERE e.isActive = true
          AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(e.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(e.code, '')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY e.displayOrder ASC, e.name ASC
        """)
    List<Exam> searchActive(@Param("q") String query);

    boolean existsByCode(String code);
    java.util.Optional<Exam> findByNameIgnoreCase(String name);
}
