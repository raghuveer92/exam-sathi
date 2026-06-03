package com.examsaathi.repository;

import com.examsaathi.entity.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    List<ExamSubject> findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(Long examId);

    List<ExamSubject> findBySubjectIdOrderByDisplayOrderAsc(Long subjectId);

    Optional<ExamSubject> findByExamIdAndSubjectId(Long examId, Long subjectId);

    int countByExamIdAndIsActiveTrue(Long examId);
}