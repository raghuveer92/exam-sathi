ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_progress_reminder_enabled BOOLEAN;

UPDATE users
SET daily_progress_reminder_enabled = TRUE
WHERE daily_progress_reminder_enabled IS NULL;

ALTER TABLE users
    ALTER COLUMN daily_progress_reminder_enabled SET DEFAULT TRUE;

ALTER TABLE users
    ALTER COLUMN daily_progress_reminder_enabled SET NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS daily_progress_reminder_time VARCHAR(5);

UPDATE users
SET daily_progress_reminder_time = '22:00'
WHERE daily_progress_reminder_time IS NULL;

ALTER TABLE users
    ALTER COLUMN daily_progress_reminder_time SET DEFAULT '22:00';

ALTER TABLE users
    ALTER COLUMN daily_progress_reminder_time SET NOT NULL;

ALTER TABLE daily_study_logs
    ADD COLUMN IF NOT EXISTS no_study_day BOOLEAN;

UPDATE daily_study_logs
SET no_study_day = FALSE
WHERE no_study_day IS NULL;

ALTER TABLE daily_study_logs
    ALTER COLUMN no_study_day SET DEFAULT FALSE;

ALTER TABLE daily_study_logs
    ALTER COLUMN no_study_day SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_daily_study_logs_user_exam_date
    ON daily_study_logs(user_id, exam_id, study_date);
