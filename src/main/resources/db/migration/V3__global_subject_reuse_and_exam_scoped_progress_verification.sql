-- Verification queries for V3 migration.

-- Duplicate subjects should be eliminated.
SELECT normalized_name, COUNT(*)
FROM subjects
GROUP BY normalized_name
HAVING COUNT(*) > 1;

-- Every exam should reference subjects through the join table.
SELECT e.id, e.name, COUNT(es.id) AS subject_links
FROM exams e
LEFT JOIN exam_subjects es ON es.exam_id = e.id
GROUP BY e.id, e.name
ORDER BY e.name;

-- Progress rows must be scoped to a user exam.
SELECT COUNT(*) AS progress_without_user_exam
FROM study_progress
WHERE user_exam_id IS NULL;

-- No duplicate logical progress rows should remain.
SELECT user_exam_id, topic_id, COUNT(*)
FROM study_progress
GROUP BY user_exam_id, topic_id
HAVING COUNT(*) > 1;

-- Canonical subject/topic totals after consolidation.
SELECT
    (SELECT COUNT(*) FROM subjects) AS subject_count,
    (SELECT COUNT(*) FROM chapters) AS chapter_count,
    (SELECT COUNT(*) FROM topics) AS topic_count,
    (SELECT COUNT(*) FROM exam_subjects) AS exam_subject_count,
    (SELECT COUNT(*) FROM study_progress) AS progress_count;