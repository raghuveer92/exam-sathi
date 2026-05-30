package com.examsaathi.repository;

import com.examsaathi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Find user with selectedExam and userExams eagerly fetched to avoid LazyInitializationException */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.selectedExam " +
           "LEFT JOIN FETCH u.userExams ue " +
           "LEFT JOIN FETCH ue.exam " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithExam(@Param("email") String email);

    boolean existsByEmail(String email);

    /** Search students by name or email */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT' " +
           "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchStudents(@Param("query") String query, Pageable pageable);

    /** Count students active since a given date */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT' " +
           "AND u.lastStudyDate >= :since")
    long countActiveStudents(@Param("since") LocalDateTime since);

    /** Total student count */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT'")
    long countStudents();
}
