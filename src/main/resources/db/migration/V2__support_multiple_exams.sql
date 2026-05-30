-- Multi-exam support migration
-- Note: apply in DB migration pipeline (Flyway/Liquibase/manual) before enabling multi-exam in production.

CREATE TABLE IF NOT EXISTS user_exam (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    exam_date DATE,
    syllabus_target_date DATE,
    daily_target_hours DOUBLE PRECISION,
    weekly_target_hours DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_user_exam_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exam_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_exam UNIQUE (user_id, exam_id)
);

-- Backfill from legacy selected_exam fields
INSERT INTO user_exam (user_id, exam_id, exam_date, syllabus_target_date, daily_target_hours, weekly_target_hours, is_active, created_at, updated_at)
SELECT id, selected_exam_id, exam_date, syllabus_target_date, daily_target_hours, weekly_target_hours, TRUE, COALESCE(created_at, NOW()), updated_at
FROM users
WHERE selected_exam_id IS NOT NULL
ON CONFLICT (user_id, exam_id) DO NOTHING;

-- Add exam_id to daily logs for exam-specific charting
ALTER TABLE daily_study_logs ADD COLUMN IF NOT EXISTS exam_id BIGINT;
ALTER TABLE daily_study_logs
    ADD CONSTRAINT fk_daily_study_logs_exam
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE SET NULL;

-- Backfill exam_id on existing rows from currently selected exam
UPDATE daily_study_logs d
SET exam_id = u.selected_exam_id
FROM users u
WHERE d.user_id = u.id AND d.exam_id IS NULL;

-- Update uniqueness to support multiple exams in same day
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'daily_study_logs_user_id_study_date_key'
    ) THEN
        ALTER TABLE daily_study_logs DROP CONSTRAINT daily_study_logs_user_id_study_date_key;
    END IF;
EXCEPTION WHEN undefined_object THEN
    NULL;
END$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_daily_logs_user_exam_date
ON daily_study_logs(user_id, exam_id, study_date);
