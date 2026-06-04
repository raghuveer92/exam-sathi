package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures exam catalog columns exist on shared DBs where Flyway is not enabled.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class ExamCatalogSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS exam_categories (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    description VARCHAR(500),
                    icon VARCHAR(80),
                    display_order INT NOT NULL DEFAULT 0,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP
                )
                """);
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES exam_categories(id)");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS short_description VARCHAR(200)");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(30)");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS popular BOOLEAN NOT NULL DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS featured_order INT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE user_exam ADD COLUMN IF NOT EXISTS experience_level VARCHAR(30)");
            log.info("Exam catalog schema migration applied");
        } catch (Exception e) {
            log.warn("Exam catalog schema migration skipped or partial: {}", e.getMessage());
        }
    }
}
