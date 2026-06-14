-- Additional indexes for /sync/bundle, /progress/subjects, subject-groups hot paths.

CREATE INDEX IF NOT EXISTS idx_study_progress_user_exam_id ON study_progress(user_exam_id);
CREATE INDEX IF NOT EXISTS idx_study_progress_user_exam_completed ON study_progress(user_exam_id, is_completed);
CREATE INDEX IF NOT EXISTS idx_user_exam_user_exam ON user_exam(user_id, exam_id);
