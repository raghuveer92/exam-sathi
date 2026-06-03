package com.examsaathi.repository;

import com.examsaathi.entity.UserExamSubjectSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserExamSubjectSelectionRepository extends JpaRepository<UserExamSubjectSelection, Long> {

    List<UserExamSubjectSelection> findByUserExamId(Long userExamId);

    List<UserExamSubjectSelection> findByUserExamIdAndGroupId(Long userExamId, Long groupId);

    void deleteByUserExamIdAndGroupId(Long userExamId, Long groupId);

    void deleteByGroupExamIdAndSubjectId(Long examId, Long subjectId);
}