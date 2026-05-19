package com.examsaathi.repository;

import com.examsaathi.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectIdOrderByOrderIndexAsc(Long subjectId);
    List<Chapter> findBySubjectIdAndIsActiveTrueOrderByOrderIndexAsc(Long subjectId);
    int countBySubjectId(Long subjectId);
}
