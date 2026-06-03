package com.examsaathi.repository;

import com.examsaathi.entity.ExamSubjectGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubjectGroupItemRepository extends JpaRepository<ExamSubjectGroupItem, Long> {

    List<ExamSubjectGroupItem> findByGroupIdOrderByIdAsc(Long groupId);

    Optional<ExamSubjectGroupItem> findByGroupExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsByGroupExamIdAndSubjectId(Long examId, Long subjectId);

    void deleteByGroupExamIdAndSubjectId(Long examId, Long subjectId);
}