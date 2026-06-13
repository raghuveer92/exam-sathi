package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies OTP verification columns on shared DBs where Flyway is not enabled.
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class EmailOtpSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS verification_token");
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS verification_token_expiry");
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS password_reset_token");
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS password_reset_token_expiry");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp VARCHAR(10)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_expiry TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_attempts INT");
            jdbcTemplate.update("UPDATE users SET email_otp_attempts = 0 WHERE email_otp_attempts IS NULL");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_otp_attempts SET DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_purpose VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_send_count INT");
            jdbcTemplate.update("UPDATE users SET otp_send_count = 0 WHERE otp_send_count IS NULL");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN otp_send_count SET DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_send_window_start TIMESTAMP");
            log.info("Email OTP schema migration applied");
        } catch (Exception e) {
            log.warn("Email OTP schema migration skipped or partial: {}", e.getMessage());
        }
    }
}
