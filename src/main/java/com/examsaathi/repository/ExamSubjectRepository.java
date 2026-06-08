package com.examsaathi.repository;

import com.examsaathi.entity.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ExamSubject es WHERE es.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);

    List<ExamSubject> findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(Long examId);

    List<ExamSubject> findBySubjectIdOrderByDisplayOrderAsc(Long subjectId);

    Optional<ExamSubject> findByExamIdAndSubjectId(Long examId, Long subjectId);

    int countByExamIdAndIsActiveTrue(Long examId);

    long countBySubjectId(Long subjectId);
}