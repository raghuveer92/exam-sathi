-- Deduplicate topics that share the same logical title within a subject.
-- Preserves the most useful topic row and merges exam-scoped progress safely.

BEGIN;

CREATE TEMP TABLE topic_duplicate_stats AS
SELECT
    t.id AS topic_id,
    c.subject_id,
    lower(regexp_replace(trim(t.title), '\s+', ' ', 'g')) AS normalized_title,
    NULLIF(trim(t.description), '') AS cleaned_description,
    COALESCE(length(NULLIF(trim(t.description), '')), 0) AS description_length,
    COUNT(sp.id) AS progress_refs
FROM topics t
JOIN chapters c ON c.id = t.chapter_id
LEFT JOIN study_progress sp ON sp.topic_id = t.id
GROUP BY t.id, c.subject_id, normalized_title, cleaned_description, description_length;

CREATE TEMP TABLE duplicate_topic_map AS
SELECT DISTINCT
    tds.topic_id,
    first_value(tds.topic_id) OVER (
        PARTITION BY tds.subject_id, tds.normalized_title
        ORDER BY tds.progress_refs DESC, tds.description_length DESC, tds.topic_id ASC
    ) AS canonical_topic_id
FROM topic_duplicate_stats tds
WHERE EXISTS (
    SELECT 1
    FROM topic_duplicate_stats dup
    WHERE dup.subject_id = tds.subject_id
      AND dup.normalized_title = tds.normalized_title
    GROUP BY dup.subject_id, dup.normalized_title
    HAVING COUNT(*) > 1
);

CREATE TEMP TABLE canonical_topic_description AS
SELECT DISTINCT
    dtm.canonical_topic_id,
    first_value(tds.cleaned_description) OVER (
        PARTITION BY dtm.canonical_topic_id
        ORDER BY tds.description_length DESC, tds.topic_id ASC
    ) AS preferred_description
FROM duplicate_topic_map dtm
JOIN topic_duplicate_stats tds ON tds.topic_id = dtm.topic_id;

CREATE TEMP TABLE progress_conflicts AS
SELECT
    losing.id AS losing_progress_id,
    winning.id AS winning_progress_id
FROM study_progress losing
JOIN duplicate_topic_map dtm
  ON dtm.topic_id = losing.topic_id
 AND dtm.topic_id <> dtm.canonical_topic_id
JOIN study_progress winning
  ON winning.user_exam_id = losing.user_exam_id
 AND winning.topic_id = dtm.canonical_topic_id;

CREATE TEMP TABLE progress_conflict_members AS
SELECT winning_progress_id, winning_progress_id AS progress_id
FROM progress_conflicts
UNION
SELECT winning_progress_id, losing_progress_id AS progress_id
FROM progress_conflicts;

CREATE TEMP TABLE progress_conflict_rollup AS
SELECT
    pcm.winning_progress_id,
    CASE
        WHEN BOOL_OR(COALESCE(sp.is_completed, FALSE)) THEN 'COMPLETED'
        WHEN BOOL_OR(sp.status = 'IN_PROGRESS')
          OR COALESCE(SUM(COALESCE(sp.actual_hours, 0)), 0) > 0 THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END AS merged_status,
    BOOL_OR(COALESCE(sp.is_completed, FALSE)) AS merged_is_completed,
    COALESCE(SUM(COALESCE(sp.actual_hours, 0)), 0) AS merged_actual_hours,
    MAX(sp.completed_at) AS merged_completed_at,
    MAX(sp.last_studied_at) AS merged_last_studied_at,
    NULLIF(string_agg(DISTINCT NULLIF(sp.notes, ''), E'\n---\n'), '') AS merged_notes,
    MIN(sp.created_at) AS merged_created_at,
    MAX(sp.updated_at) AS merged_updated_at
FROM progress_conflict_members pcm
JOIN study_progress sp ON sp.id = pcm.progress_id
GROUP BY pcm.winning_progress_id;

UPDATE study_progress sp
SET status = pcr.merged_status,
    is_completed = pcr.merged_is_completed,
    actual_hours = pcr.merged_actual_hours,
    completed_at = pcr.merged_completed_at,
    last_studied_at = pcr.merged_last_studied_at,
    notes = pcr.merged_notes,
    created_at = pcr.merged_created_at,
    updated_at = pcr.merged_updated_at
FROM progress_conflict_rollup pcr
WHERE sp.id = pcr.winning_progress_id;

DELETE FROM study_progress sp
USING progress_conflicts pc
WHERE sp.id = pc.losing_progress_id;

UPDATE study_progress sp
SET topic_id = dtm.canonical_topic_id,
    updated_at = NOW()
FROM duplicate_topic_map dtm
WHERE sp.topic_id = dtm.topic_id
  AND dtm.topic_id <> dtm.canonical_topic_id;

UPDATE topics t
SET description = ctd.preferred_description
FROM canonical_topic_description ctd
WHERE t.id = ctd.canonical_topic_id
  AND ctd.preferred_description IS NOT NULL
  AND COALESCE(length(NULLIF(trim(t.description), '')), 0) < length(ctd.preferred_description);

DELETE FROM topics t
USING duplicate_topic_map dtm
WHERE t.id = dtm.topic_id
  AND dtm.topic_id <> dtm.canonical_topic_id;

COMMIT;