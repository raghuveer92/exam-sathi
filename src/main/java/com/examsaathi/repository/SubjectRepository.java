package com.examsaathi.repository;

import com.examsaathi.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByNormalizedName(String normalizedName);

    @Query("SELECT es.subject FROM ExamSubject es WHERE es.exam.id = :examId " +
           "AND es.isActive = true AND es.subject.isActive = true ORDER BY es.displayOrder ASC")
    List<Subject> findActiveByExamIdOrderByDisplayOrderAsc(@Param("examId") Long examId);

    @Query("SELECT COUNT(es) FROM ExamSubject es WHERE es.exam.id = :examId " +
           "AND es.isActive = true AND es.subject.isActive = true")
    int countActiveByExamId(@Param("examId") Long examId);

    List<Subject> findByIsActiveTrueOrderByNameAsc();

    List<Subject> findByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(LocalDateTime since);
}
