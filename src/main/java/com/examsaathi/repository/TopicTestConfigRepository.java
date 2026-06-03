package com.examsaathi.repository;

import com.examsaathi.entity.TopicTestConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicTestConfigRepository extends JpaRepository<TopicTestConfig, Long> {
    Optional<TopicTestConfig> findByTopicId(Long topicId);
}
