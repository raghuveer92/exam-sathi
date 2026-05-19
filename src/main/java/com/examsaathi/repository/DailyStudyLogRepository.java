package com.examsaathi.repository;

import com.examsaathi.entity.DailyStudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStudyLogRepository extends JpaRepository<DailyStudyLog, Long> {

    Optional<DailyStudyLog> findByUserIdAndStudyDate(Long userId, LocalDate date);

    List<DailyStudyLog> findByUserIdOrderByStudyDateDesc(Long userId);

    /** Last N days logs for weekly chart */
    @Query("SELECT d FROM DailyStudyLog d WHERE d.user.id = :userId " +
           "AND d.studyDate >= :since ORDER BY d.studyDate ASC")
    List<DailyStudyLog> findByUserIdAndStudyDateAfter(@Param("userId") Long userId,
                                                       @Param("since") LocalDate since);

    /** Sum hours studied in a date range */
    @Query("SELECT COALESCE(SUM(d.hoursStudied), 0) FROM DailyStudyLog d " +
           "WHERE d.user.id = :userId AND d.studyDate BETWEEN :from AND :to")
    Double sumHoursInRange(@Param("userId") Long userId,
                           @Param("from") LocalDate from,
                           @Param("to") LocalDate to);

    /** Platform-wide daily active users */
    @Query("SELECT COUNT(DISTINCT d.user.id) FROM DailyStudyLog d WHERE d.studyDate = :date")
    long countDailyActiveUsers(@Param("date") LocalDate date);
}
