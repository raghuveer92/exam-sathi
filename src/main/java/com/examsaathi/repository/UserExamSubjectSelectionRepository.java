package com.examsaathi.repository;

import com.examsaathi.entity.UserExamSubjectSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserExamSubjectSelectionRepository extends JpaRepository<UserExamSubjectSelection, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserExamSubjectSelection s WHERE s.userExam.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);

    List<UserExamSubjectSelection> findByUserExamId(Long userExamId);

    List<UserExamSubjectSelection> findByUserExamIdAndGroupId(Long userExamId, Long groupId);

    void deleteByUserExamIdAndGroupId(Long userExamId, Long groupId);

    void deleteByGroupExamIdAndSubjectId(Long examId, Long subjectId);
}