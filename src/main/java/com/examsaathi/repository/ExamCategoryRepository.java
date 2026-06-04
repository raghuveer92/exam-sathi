package com.examsaathi.repository;

import com.examsaathi.entity.ExamCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamCategoryRepository extends JpaRepository<ExamCategory, Long> {
    List<ExamCategory> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<ExamCategory> findAllByOrderByDisplayOrderAscNameAsc();
    boolean existsByNameIgnoreCase(String name);

    List<ExamCategory> findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(LocalDateTime since);
}
