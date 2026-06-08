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
}
