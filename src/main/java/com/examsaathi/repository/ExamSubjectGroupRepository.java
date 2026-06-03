package com.examsaathi.repository;

import com.examsaathi.entity.ExamSubjectGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubjectGroupRepository extends JpaRepository<ExamSubjectGroup, Long> {

    @EntityGraph(attributePaths = {"items", "items.subject"})
    List<ExamSubjectGroup> findByExamIdOrderByDisplayOrderAscIdAsc(Long examId);

    @EntityGraph(attributePaths = {"items", "items.subject"})
    Optional<ExamSubjectGroup> findByExamIdAndGroupNameIgnoreCase(Long examId, String groupName);

    boolean existsByExamId(Long examId);
}