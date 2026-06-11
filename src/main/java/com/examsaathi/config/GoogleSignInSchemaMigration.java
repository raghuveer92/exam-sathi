package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies Google Sign-In columns on shared DBs where Flyway is not enabled.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class GoogleSignInSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN password DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20)");
            jdbcTemplate.update("UPDATE users SET auth_provider = 'EMAIL' WHERE auth_provider IS NULL");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN auth_provider SET DEFAULT 'EMAIL'");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN auth_provider SET NOT NULL");
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id) WHERE google_id IS NOT NULL");
            log.info("Google Sign-In schema migration applied");
        } catch (Exception e) {
            log.warn("Google Sign-In schema migration skipped or partial: {}", e.getMessage());
        }
    }
}
