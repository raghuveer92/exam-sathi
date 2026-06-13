package com.examsaathi.repository;

import com.examsaathi.entity.Question;
import com.examsaathi.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamId(Long examId);

    List<Question> findByTopicIdOrderByIdAsc(Long topicId);

    @Query("""
        SELECT q FROM Question q
        WHERE q.topic.id = :topicId
          AND q.isActive = true
          AND (:difficulty IS NULL OR q.difficultyLevel = :difficulty)
        """)
    List<Question> findActiveForTopic(
        @Param("topicId") Long topicId,
        @Param("difficulty") Topic.DifficultyLevel difficulty);

    long countByTopicIdAndIsActiveTrue(Long topicId);

    long countByTopicId(Long topicId);

    /** Single query for mock-test topics with enough active questions (replaces per-topic counts). */
    @Query(value = """
        SELECT ttc.topic_id
        FROM topic_test_configs ttc
        INNER JOIN (
            SELECT topic_id, COUNT(*) AS cnt
            FROM questions
            WHERE is_active = true
            GROUP BY topic_id
        ) q ON q.topic_id = ttc.topic_id
        WHERE ttc.is_active = true AND q.cnt >= ttc.num_questions
        ORDER BY ttc.topic_id
        """, nativeQuery = true)
    List<Long> findReadyMockTestTopicIds();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Question")
    void deleteAllRows();
}
