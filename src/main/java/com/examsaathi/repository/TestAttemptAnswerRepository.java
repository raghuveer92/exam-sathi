package com.examsaathi.repository;

import com.examsaathi.entity.TestAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestAttemptAnswerRepository extends JpaRepository<TestAttemptAnswer, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttemptAnswer a WHERE a.question.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") Long topicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttemptAnswer a WHERE a.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttemptAnswer")
    void deleteAllRows();

    @Modifying
    @Query("DELETE FROM TestAttemptAnswer a WHERE a.question.exam.id = :examId")
    void deleteByQuestionExamId(@Param("examId") Long examId);

    boolean existsByQuestionId(Long questionId);

    @Query("""
        SELECT a FROM TestAttemptAnswer a
        JOIN FETCH a.question q
        LEFT JOIN FETCH q.options
        WHERE a.attempt.id = :attemptId
        ORDER BY a.id ASC
        """)
    List<TestAttemptAnswer> findByAttemptIdWithQuestions(@Param("attemptId") Long attemptId);
}
