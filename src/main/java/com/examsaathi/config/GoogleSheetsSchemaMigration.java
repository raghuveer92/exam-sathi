package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures Google Sheets mock test columns exist on shared DBs where Flyway is not enabled.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS sheet_url VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS sheet_id VARCHAR(100)");

            jdbcTemplate.execute("ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS test_status VARCHAR(20) DEFAULT 'LOCKED'");
            jdbcTemplate.execute("ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS last_test_score DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS best_test_score DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS mastery_score DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE study_progress ADD COLUMN IF NOT EXISTS total_tests_attempted INT NOT NULL DEFAULT 0");

            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS accuracy DOUBLE PRECISION NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS mastery_level VARCHAR(20)");

            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS question_count INT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS wrong_count INT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS unanswered_count INT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS previous_attempt_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS previous_percentage DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS improvement_score DOUBLE PRECISION");

            jdbcTemplate.update("""
                UPDATE exams SET
                    sheet_id = '1FW8R23wq4NCiEVSgbvBjngmTTncYD6R16eBqSQyERFk',
                    sheet_url = 'https://docs.google.com/spreadsheets/d/1FW8R23wq4NCiEVSgbvBjngmTTncYD6R16eBqSQyERFk/edit'
                WHERE name = 'SSC CGL' AND (sheet_id IS NULL OR sheet_id = '')
                """);

            jdbcTemplate.execute("ALTER TABLE test_attempt_answers ADD COLUMN IF NOT EXISTS sheet_question_id VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE test_attempt_answers ADD COLUMN IF NOT EXISTS question_snapshot TEXT");
            jdbcTemplate.execute("ALTER TABLE test_attempt_answers ALTER COLUMN question_id DROP NOT NULL");

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    ALTER TABLE test_attempt_answers
                        DROP CONSTRAINT IF EXISTS test_attempt_answers_attempt_id_question_id_key;
                EXCEPTION WHEN undefined_object THEN NULL;
                END $$
                """);

            log.info("Google Sheets mock test schema migration applied");
        } catch (Exception e) {
            log.warn("Google Sheets schema migration skipped or partial: {}", e.getMessage());
        }
    }
}
