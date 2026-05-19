package com.examsaathi.repository;

import com.examsaathi.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(Long examId);
    List<Subject> findByExamIdOrderByDisplayOrderAsc(Long examId);
    int countByExamId(Long examId);
}
