package com.examsaathi.repository;

import com.examsaathi.entity.StudyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyProgressRepository extends JpaRepository<StudyProgress, Long> {

    Optional<StudyProgress> findByUserExamIdAndTopicId(Long userExamId, Long topicId);

    List<StudyProgress> findByUserId(Long userId);

    /** Count completed topics for a user in a subject */
    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.user.id = :userId " +
           "AND sp.userExam.exam.id = :examId " +
           "AND sp.topic.chapter.subject.id = :subjectId AND sp.isCompleted = true")
    int countCompletedByUserAndExamAndSubject(@Param("userId") Long userId,
                                              @Param("examId") Long examId,
                                              @Param("subjectId") Long subjectId);

    /** Count completed topics for a user in an exam */
    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.user.id = :userId " +
           "AND sp.userExam.exam.id = :examId AND sp.isCompleted = true")
    int countCompletedByUserAndExam(@Param("userId") Long userId,
                                    @Param("examId") Long examId);

    /** Topics completed by user today */
    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.user.id = :userId " +
           "AND sp.isCompleted = true AND sp.completedAt >= :since")
    int countCompletedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.user.id = :userId " +
           "AND sp.userExam.exam.id = :examId " +
           "AND sp.isCompleted = true AND sp.completedAt >= :since")
    int countCompletedByUserAndExamSince(@Param("userId") Long userId,
                                         @Param("examId") Long examId,
                                         @Param("since") LocalDateTime since);

    /** All completed progress for a user in an exam (for analytics) */
    @Query("SELECT sp FROM StudyProgress sp WHERE sp.user.id = :userId " +
           "AND sp.userExam.exam.id = :examId AND sp.isCompleted = true")
    List<StudyProgress> findCompletedByUserAndExam(@Param("userId") Long userId,
                                                    @Param("examId") Long examId);

    /** Batch fetch progress records for a user and a list of topic IDs */
    @Query("SELECT sp FROM StudyProgress sp WHERE sp.userExam.id = :userExamId AND sp.topic.id IN :topicIds")
    List<StudyProgress> findByUserExamIdAndTopicIdIn(@Param("userExamId") Long userExamId,
                                                     @Param("topicIds") List<Long> topicIds);

    /** Delete all progress records for a topic (used before topic deletion) */
    void deleteByTopicId(Long topicId);
}
