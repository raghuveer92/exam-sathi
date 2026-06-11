package com.examsaathi.repository;

import com.examsaathi.entity.StudyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.userExam.id = :userExamId " +
           "AND sp.topic.chapter.subject.id IN :subjectIds AND sp.isCompleted = true")
    int countCompletedByUserExamIdAndSubjectIds(@Param("userExamId") Long userExamId,
                                                @Param("subjectIds") List<Long> subjectIds);

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

    @Query("SELECT COUNT(sp) FROM StudyProgress sp WHERE sp.userExam.id = :userExamId " +
           "AND sp.topic.chapter.subject.id IN :subjectIds " +
           "AND sp.isCompleted = true AND sp.completedAt >= :since")
    int countCompletedByUserExamIdAndSubjectIdsSince(@Param("userExamId") Long userExamId,
                                                     @Param("subjectIds") List<Long> subjectIds,
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StudyProgress sp WHERE sp.userExam.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);

    @Query("""
        SELECT sp FROM StudyProgress sp
        JOIN FETCH sp.topic t
        JOIN FETCH t.chapter c
        JOIN FETCH c.subject
        JOIN FETCH sp.userExam ue
        JOIN FETCH ue.exam
        WHERE sp.user.id = :userId AND sp.updatedAt > :since
        """)
    List<StudyProgress> findByUserIdAndUpdatedAtAfter(@Param("userId") Long userId,
                                                      @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM StudyProgress sp WHERE sp.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
