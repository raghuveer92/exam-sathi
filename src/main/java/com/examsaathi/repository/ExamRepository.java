package com.examsaathi.repository;

import com.examsaathi.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByIsActiveTrueOrderByNameAsc();
    boolean existsByCode(String code);
}
