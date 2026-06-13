package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies email verification and password reset columns on shared DBs where Flyway is not enabled.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(36)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(36)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expiry TIMESTAMP");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token) "
                    + "WHERE verification_token IS NOT NULL");
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_users_password_reset_token ON users(password_reset_token) "
                    + "WHERE password_reset_token IS NOT NULL");
            log.info("Email verification schema migration applied");
        } catch (Exception e) {
            log.warn("Email verification schema migration skipped or partial: {}", e.getMessage());
        }
    }
}
