package com.examsaathi.repository;

import com.examsaathi.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttempt")
    void deleteAllRows();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestAttempt ta WHERE ta.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") Long topicId);

    List<TestAttempt> findByTopicId(Long topicId);

    List<TestAttempt> findByUserIdAndTopicIdOrderByStartedAtDesc(Long userId, Long topicId);

    List<TestAttempt> findByUserIdAndStatusOrderBySubmittedAtDesc(
        Long userId, TestAttempt.AttemptStatus status);

    @Query("""
        SELECT ta FROM TestAttempt ta
        JOIN FETCH ta.topic t
        JOIN FETCH t.chapter c
        WHERE ta.user.id = :userId AND ta.status <> 'IN_PROGRESS'
        ORDER BY ta.submittedAt DESC
        """)
    List<TestAttempt> findCompletedByUserId(@Param("userId") Long userId);

    Optional<TestAttempt> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, TestAttempt.AttemptStatus status);

    long countByTopicTestConfigId(Long topicTestConfigId);
}
