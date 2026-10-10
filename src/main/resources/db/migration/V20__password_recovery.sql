CREATE TABLE password_reset_tokens (
  token_hash VARCHAR(64) PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  email_snapshot VARCHAR(100) NOT NULL,
  auth_version BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id,created_at);
CREATE INDEX idx_password_reset_expiry ON password_reset_tokens(expires_at);
