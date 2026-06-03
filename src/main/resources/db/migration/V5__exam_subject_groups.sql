-- Add optional subject group support on top of the existing exam_subjects join.

BEGIN;

CREATE TABLE IF NOT EXISTS exam_subject_groups (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    group_name VARCHAR(150) NOT NULL,
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    min_selection INTEGER NOT NULL DEFAULT 0,
    max_selection INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_exam_subject_groups_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT ck_exam_subject_groups_selection_bounds CHECK (
        min_selection >= 0 AND max_selection >= 0 AND max_selection >= min_selection
    )
);

CREATE TABLE IF NOT EXISTS exam_subject_group_items (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_exam_subject_group_items_group FOREIGN KEY (group_id) REFERENCES exam_subject_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_subject_group_items_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT uq_exam_subject_group_items UNIQUE (group_id, subject_id)
);

CREATE TABLE IF NOT EXISTS user_exam_subject_selections (
    id BIGSERIAL PRIMARY KEY,
    user_exam_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_exam_subject_selections_user_exam FOREIGN KEY (user_exam_id) REFERENCES user_exam(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exam_subject_selections_group FOREIGN KEY (group_id) REFERENCES exam_subject_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exam_subject_selections_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_exam_subject_selections UNIQUE (user_exam_id, group_id, subject_id)
);

CREATE INDEX IF NOT EXISTS idx_exam_subject_groups_exam_id ON exam_subject_groups(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_subject_group_items_group_id ON exam_subject_group_items(group_id);
CREATE INDEX IF NOT EXISTS idx_exam_subject_group_items_subject_id ON exam_subject_group_items(subject_id);
CREATE INDEX IF NOT EXISTS idx_user_exam_subject_selections_user_exam_id ON user_exam_subject_selections(user_exam_id);
CREATE INDEX IF NOT EXISTS idx_user_exam_subject_selections_group_id ON user_exam_subject_selections(group_id);

INSERT INTO exam_subject_groups (
    exam_id,
    group_name,
    is_optional,
    min_selection,
    max_selection,
    display_order,
    created_at
)
SELECT
    e.id,
    'Mandatory Subjects',
    FALSE,
    0,
    0,
    0,
    NOW()
FROM exams e
WHERE EXISTS (
    SELECT 1
    FROM exam_subjects es
    WHERE es.exam_id = e.id
)
AND NOT EXISTS (
    SELECT 1
    FROM exam_subject_groups esg
    WHERE esg.exam_id = e.id
);

INSERT INTO exam_subject_group_items (group_id, subject_id, created_at)
SELECT
    esg.id,
    es.subject_id,
    COALESCE(es.created_at, NOW())
FROM exam_subjects es
JOIN exam_subject_groups esg
  ON esg.exam_id = es.exam_id
 AND esg.group_name = 'Mandatory Subjects'
LEFT JOIN exam_subject_group_items esgi
  ON esgi.group_id = esg.id
 AND esgi.subject_id = es.subject_id
WHERE esgi.id IS NULL;

COMMIT;