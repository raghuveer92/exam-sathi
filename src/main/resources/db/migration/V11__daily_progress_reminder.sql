ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_progress_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_progress_reminder_time VARCHAR(5) NOT NULL DEFAULT '22:00';

ALTER TABLE daily_study_logs
    ADD COLUMN IF NOT EXISTS no_study_day BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_daily_study_logs_user_exam_date
    ON daily_study_logs(user_id, exam_id, study_date);
