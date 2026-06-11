-- Allow user row deletion to cascade to remaining student-owned rows.

ALTER TABLE test_attempts
    DROP CONSTRAINT IF EXISTS test_attempts_user_id_fkey;

ALTER TABLE test_attempts
    ADD CONSTRAINT test_attempts_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE daily_study_logs
    DROP CONSTRAINT IF EXISTS daily_study_logs_user_id_fkey;

ALTER TABLE daily_study_logs
    ADD CONSTRAINT daily_study_logs_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
