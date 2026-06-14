-- Hardening: explicit attempt counts, improvement tracking

ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS question_count INT NOT NULL DEFAULT 0;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS wrong_count INT NOT NULL DEFAULT 0;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS unanswered_count INT NOT NULL DEFAULT 0;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS previous_attempt_id BIGINT;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS previous_percentage DOUBLE PRECISION;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS improvement_score DOUBLE PRECISION;
