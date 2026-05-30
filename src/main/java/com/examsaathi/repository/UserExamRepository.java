package com.examsaathi.repository;

import com.examsaathi.entity.UserExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExamRepository extends JpaRepository<UserExam, Long> {

    List<UserExam> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<UserExam> findByUserIdOrderByExamDateAscCreatedAtAsc(Long userId);

    Optional<UserExam> findByUserIdAndExamId(Long userId, Long examId);

    Optional<UserExam> findByUserIdAndIsActiveTrue(Long userId);

    long countByUserId(Long userId);
}
