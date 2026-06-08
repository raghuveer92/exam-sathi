package com.examsaathi.repository;

import com.examsaathi.entity.TopicTestConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TopicTestConfigRepository extends JpaRepository<TopicTestConfig, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TopicTestConfig")
    void deleteAllRows();

    List<TopicTestConfig> findByIsActiveTrue();

    Optional<TopicTestConfig> findByTopicId(Long topicId);
}
