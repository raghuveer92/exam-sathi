-- Global subject reuse + exam-scoped progress migration
-- Preserves chapter grouping while removing exam ownership from subjects.
-- Run inside a maintenance window after taking a database backup.

BEGIN;

ALTER TABLE subjects ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(100);

UPDATE subjects
SET normalized_name = lower(regexp_replace(trim(name), '\s+', ' ', 'g'))
WHERE normalized_name IS NULL OR normalized_name = '';

ALTER TABLE subjects
    ALTER COLUMN normalized_name SET NOT NULL;

CREATE TABLE IF NOT EXISTS exam_subjects (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_exam_subjects_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT uq_exam_subjects UNIQUE (exam_id, subject_id)
);

CREATE TEMP TABLE subject_canonical_map AS
SELECT
    s.id AS subject_id,
    first_value(s.id) OVER (
        PARTITION BY s.normalized_name
        ORDER BY s.id
    ) AS canonical_subject_id,
    s.exam_id,
    COALESCE(s.display_order, 0) AS display_order,
    COALESCE(s.is_active, TRUE) AS is_active
FROM subjects s;

INSERT INTO exam_subjects (exam_id, subject_id, display_order, is_active, created_at, updated_at)
SELECT
    scm.exam_id,
    scm.canonical_subject_id,
    MIN(scm.display_order) AS display_order,
    BOOL_OR(scm.is_active) AS is_active,
    NOW(),
    NOW()
FROM subject_canonical_map scm
WHERE scm.exam_id IS NOT NULL
GROUP BY scm.exam_id, scm.canonical_subject_id
ON CONFLICT (exam_id, subject_id) DO UPDATE
SET display_order = LEAST(exam_subjects.display_order, EXCLUDED.display_order),
    is_active = exam_subjects.is_active OR EXCLUDED.is_active,
    updated_at = NOW();

CREATE TEMP TABLE chapter_canonical_map AS
SELECT
    c.id AS chapter_id,
    first_value(c.id) OVER (
        PARTITION BY scm.canonical_subject_id, lower(regexp_replace(trim(c.title), '\s+', ' ', 'g'))
        ORDER BY CASE WHEN c.subject_id = scm.canonical_subject_id THEN 0 ELSE 1 END, c.id
    ) AS canonical_chapter_id,
    scm.canonical_subject_id
FROM chapters c
JOIN subject_canonical_map scm ON scm.subject_id = c.subject_id;

UPDATE chapters c
SET subject_id = ccm.canonical_subject_id
FROM chapter_canonical_map ccm
WHERE c.id = ccm.chapter_id
  AND ccm.chapter_id = ccm.canonical_chapter_id
  AND c.subject_id <> ccm.canonical_subject_id;

CREATE TEMP TABLE topic_canonical_map AS
SELECT
    t.id AS topic_id,
    first_value(t.id) OVER (
        PARTITION BY ccm.canonical_chapter_id, lower(regexp_replace(trim(t.title), '\s+', ' ', 'g'))
        ORDER BY CASE WHEN t.chapter_id = ccm.canonical_chapter_id THEN 0 ELSE 1 END, t.id
    ) AS canonical_topic_id,
    ccm.canonical_chapter_id
FROM topics t
JOIN chapter_canonical_map ccm ON ccm.chapter_id = t.chapter_id;

UPDATE topics t
SET chapter_id = tcm.canonical_chapter_id
FROM topic_canonical_map tcm
WHERE t.id = tcm.topic_id
  AND tcm.topic_id = tcm.canonical_topic_id
  AND t.chapter_id <> tcm.canonical_chapter_id;

INSERT INTO user_exam (user_id, exam_id, is_active, created_at, updated_at)
SELECT DISTINCT
    sp.user_id,
    s.exam_id,
    CASE WHEN u.selected_exam_id = s.exam_id THEN TRUE ELSE FALSE END,
    NOW(),
    NOW()
FROM study_progress sp
JOIN topics t ON t.id = sp.topic_id
JOIN chapters c ON c.id = t.chapter_id
JOIN subjects s ON s.id = c.subject_id
JOIN users u ON u.id = sp.user_id
WHERE s.exam_id IS NOT NULL
ON CONFLICT (user_id, exam_id) DO NOTHING;

ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS user_exam_id BIGINT;

UPDATE study_progress sp
SET user_exam_id = ue.id
FROM topics t,
         chapters c,
         subjects s,
         user_exam ue
WHERE sp.topic_id = t.id
    AND c.id = t.chapter_id
    AND s.id = c.subject_id
    AND ue.user_id = sp.user_id
    AND ue.exam_id = s.exam_id
  AND sp.user_exam_id IS NULL;

CREATE TABLE study_progress_new (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_exam_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    status VARCHAR(20),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    actual_hours DOUBLE PRECISION DEFAULT 0,
    completed_at TIMESTAMP,
    last_studied_at TIMESTAMP,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_study_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_study_progress_user_exam FOREIGN KEY (user_exam_id) REFERENCES user_exam(id) ON DELETE CASCADE,
    CONSTRAINT fk_study_progress_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT uq_study_progress_user_exam_topic UNIQUE (user_exam_id, topic_id)
);

INSERT INTO study_progress_new (
    user_id,
    user_exam_id,
    topic_id,
    status,
    is_completed,
    actual_hours,
    completed_at,
    last_studied_at,
    notes,
    created_at,
    updated_at
)
SELECT
    sp.user_id,
    sp.user_exam_id,
    tcm.canonical_topic_id,
    CASE
        WHEN BOOL_OR(COALESCE(sp.is_completed, FALSE)) THEN 'COMPLETED'
        WHEN BOOL_OR(sp.status = 'IN_PROGRESS') OR COALESCE(SUM(COALESCE(sp.actual_hours, 0)), 0) > 0 THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END AS status,
    BOOL_OR(COALESCE(sp.is_completed, FALSE)) AS is_completed,
    COALESCE(SUM(COALESCE(sp.actual_hours, 0)), 0) AS actual_hours,
    MAX(sp.completed_at) AS completed_at,
    MAX(sp.last_studied_at) AS last_studied_at,
    NULLIF(string_agg(DISTINCT NULLIF(sp.notes, ''), E'\n---\n'), '') AS notes,
    MIN(COALESCE(sp.created_at, NOW())) AS created_at,
    MAX(sp.updated_at) AS updated_at
FROM study_progress sp
JOIN topic_canonical_map tcm ON tcm.topic_id = sp.topic_id
WHERE sp.user_exam_id IS NOT NULL
GROUP BY sp.user_id, sp.user_exam_id, tcm.canonical_topic_id;

DROP TABLE study_progress;
ALTER TABLE study_progress_new RENAME TO study_progress;

DELETE FROM topics t
USING topic_canonical_map tcm
WHERE t.id = tcm.topic_id
  AND tcm.topic_id <> tcm.canonical_topic_id;

DELETE FROM chapters c
USING chapter_canonical_map ccm
WHERE c.id = ccm.chapter_id
  AND ccm.chapter_id <> ccm.canonical_chapter_id;

DELETE FROM subjects s
USING subject_canonical_map scm
WHERE s.id = scm.subject_id
  AND scm.subject_id <> scm.canonical_subject_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subjects_normalized_name ON subjects(normalized_name);
CREATE INDEX IF NOT EXISTS idx_exam_subjects_exam_id ON exam_subjects(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_subjects_subject_id ON exam_subjects(subject_id);
CREATE INDEX IF NOT EXISTS idx_study_progress_user_exam_id ON study_progress(user_exam_id);

DO $$
DECLARE constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_name = 'subjects'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'exam_id'
    LOOP
        EXECUTE format('ALTER TABLE subjects DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE subjects DROP COLUMN IF EXISTS exam_id;
ALTER TABLE subjects DROP COLUMN IF EXISTS display_order;

COMMIT;