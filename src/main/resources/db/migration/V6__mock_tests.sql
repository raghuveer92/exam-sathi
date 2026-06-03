-- Topic-wise mock test / question bank schema

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    chapter_id BIGINT NOT NULL REFERENCES chapters(id),
    topic_id BIGINT NOT NULL REFERENCES topics(id),
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    explanation TEXT,
    marks DOUBLE PRECISION NOT NULL DEFAULT 1,
    negative_marks DOUBLE PRECISION NOT NULL DEFAULT 0,
    difficulty_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    previous_year BOOLEAN NOT NULL DEFAULT FALSE,
    previous_year_value VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_key VARCHAR(5) NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS topic_test_configs (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL UNIQUE REFERENCES topics(id),
    num_questions INT NOT NULL DEFAULT 10,
    duration_minutes INT NOT NULL DEFAULT 15,
    difficulty_filter VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    topic_id BIGINT NOT NULL REFERENCES topics(id),
    topic_test_config_id BIGINT REFERENCES topic_test_configs(id),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    duration_minutes INT NOT NULL,
    time_spent_seconds INT,
    total_questions INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    percentage DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS test_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    selected_option_keys VARCHAR(100),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    marks_awarded DOUBLE PRECISION NOT NULL DEFAULT 0,
    marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMP,
    UNIQUE (attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_questions_topic ON questions(topic_id);
CREATE INDEX IF NOT EXISTS idx_questions_exam ON questions(exam_id);
CREATE INDEX IF NOT EXISTS idx_test_attempts_user_topic ON test_attempts(user_id, topic_id);
