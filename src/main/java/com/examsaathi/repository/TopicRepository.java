package com.examsaathi.repository;

import com.examsaathi.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByChapterIdOrderByOrderIndexAsc(Long chapterId);

    List<Topic> findByChapterIdAndIsActiveTrueOrderByOrderIndexAsc(Long chapterId);

    /** Total topics in a subject (via chapters) */
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.chapter.subject.id = :subjectId AND t.isActive = true")
    int countBySubjectId(@Param("subjectId") Long subjectId);

    /** All topics for an exam */
    @Query("SELECT DISTINCT t FROM ExamSubject es " +
           "JOIN es.subject s " +
           "JOIN s.chapters c " +
           "JOIN c.topics t " +
           "WHERE es.exam.id = :examId AND es.isActive = true AND s.isActive = true AND t.isActive = true")
    List<Topic> findByExamId(@Param("examId") Long examId);

    /** Sum estimated hours for a subject */
    @Query("SELECT COALESCE(SUM(t.estimatedHours), 0) FROM Topic t " +
           "WHERE t.chapter.subject.id = :subjectId AND t.isActive = true")
    Double sumEstimatedHoursBySubjectId(@Param("subjectId") Long subjectId);

    /** Sum estimated hours for an entire exam */
    @Query("SELECT COALESCE(SUM(t.estimatedHours), 0) FROM ExamSubject es " +
           "JOIN es.subject s " +
           "JOIN s.chapters c " +
           "JOIN c.topics t " +
           "WHERE es.exam.id = :examId AND es.isActive = true AND s.isActive = true AND t.isActive = true")
    Double sumEstimatedHoursByExamId(@Param("examId") Long examId);
}
