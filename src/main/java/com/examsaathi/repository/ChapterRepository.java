package com.examsaathi.repository;

import com.examsaathi.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdOrderByOrderIndexAsc(Long subjectId);
    List<Chapter> findBySubjectIdAndIsActiveTrueOrderByOrderIndexAsc(Long subjectId);
    int countBySubjectId(Long subjectId);

    /** Eagerly fetch topics to avoid N+1 queries when building subject detail */
    @Query("SELECT DISTINCT c FROM Chapter c LEFT JOIN FETCH c.topics t " +
           "WHERE c.subject.id = :subjectId AND c.isActive = true " +
           "ORDER BY c.orderIndex ASC")
    List<Chapter> findBySubjectIdWithTopics(@Param("subjectId") Long subjectId);
}
