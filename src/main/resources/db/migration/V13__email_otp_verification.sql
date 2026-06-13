-- Replace token-based verification with OTP-based verification

ALTER TABLE users DROP COLUMN IF EXISTS verification_token;
ALTER TABLE users DROP COLUMN IF EXISTS verification_token_expiry;
ALTER TABLE users DROP COLUMN IF EXISTS password_reset_token;
ALTER TABLE users DROP COLUMN IF EXISTS password_reset_token_expiry;

DROP INDEX IF EXISTS idx_users_verification_token;
DROP INDEX IF EXISTS idx_users_password_reset_token;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_purpose VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_send_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_send_window_start TIMESTAMP;
