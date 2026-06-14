-- Google Sheets topic mock test schema extensions

ALTER TABLE exams ADD COLUMN IF NOT EXISTS sheet_url VARCHAR(500);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS sheet_id VARCHAR(100);

ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS test_status VARCHAR(20) DEFAULT 'LOCKED';
ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS last_test_score DOUBLE PRECISION;
ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS best_test_score DOUBLE PRECISION;
ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS mastery_score DOUBLE PRECISION;
ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS total_tests_attempted INT NOT NULL DEFAULT 0;

ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS accuracy DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS mastery_level VARCHAR(20);

ALTER TABLE test_attempt_answers ADD COLUMN IF NOT EXISTS sheet_question_id VARCHAR(50);
ALTER TABLE test_attempt_answers ADD COLUMN IF NOT EXISTS question_snapshot TEXT;
ALTER TABLE test_attempt_answers ALTER COLUMN question_id DROP NOT NULL;
