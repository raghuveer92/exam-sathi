-- Indexes for student-app API hot paths (sync, dashboard, subject detail, progress).
-- Safe to re-run: every statement uses IF NOT EXISTS.

-- study_progress: /sync/bundle, /progress/subject/{id}, dashboard counts
CREATE INDEX IF NOT EXISTS idx_study_progress_user_id ON study_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_study_progress_user_id_updated_at ON study_progress(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_study_progress_topic_id ON study_progress(topic_id);

-- Syllabus tree: /progress/subject/{id}, /sync/catalog materialization
CREATE INDEX IF NOT EXISTS idx_chapters_subject_id ON chapters(subject_id);
CREATE INDEX IF NOT EXISTS idx_topics_chapter_id ON topics(chapter_id);

-- exam_subjects: /progress/subjects/{examId}, dashboard topic totals
CREATE INDEX IF NOT EXISTS idx_exam_subjects_exam_active ON exam_subjects(exam_id) WHERE is_active = TRUE;

-- user_exam: active exam on every progress API
CREATE INDEX IF NOT EXISTS idx_user_exam_user_active ON user_exam(user_id) WHERE is_active = TRUE;

-- daily_study_logs: /progress/weekly, dashboard today hours
CREATE INDEX IF NOT EXISTS idx_daily_study_logs_user_study_date ON daily_study_logs(user_id, study_date);

-- Incremental /sync/catalog filters
CREATE INDEX IF NOT EXISTS idx_subjects_active_updated ON subjects(is_active, updated_at) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_chapters_active_updated ON chapters(is_active, updated_at) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_topics_active_updated ON topics(is_active, updated_at) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_exams_active_updated ON exams(is_active, updated_at) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_exam_categories_active_updated ON exam_categories(is_active, updated_at) WHERE is_active = TRUE;

-- Mock tests: /mock-tests/topics/{id}/start
CREATE INDEX IF NOT EXISTS idx_topic_test_configs_active ON topic_test_configs(is_active) WHERE is_active = TRUE;
