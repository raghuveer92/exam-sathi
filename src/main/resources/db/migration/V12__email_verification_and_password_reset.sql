-- Email verification and password reset tokens

ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(36);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(36);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_expiry TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token)
    WHERE verification_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token ON users(password_reset_token)
    WHERE password_reset_token IS NOT NULL;
