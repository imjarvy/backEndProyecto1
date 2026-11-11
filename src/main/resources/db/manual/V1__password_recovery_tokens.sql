USE security_ms_db;

SET @auth_provider_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'auth_provider'
);

SET @add_auth_provider_sql = IF(
    @auth_provider_column_exists = 0,
    'ALTER TABLE users ADD COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT ''UNKNOWN''',
    'SELECT 1'
);
PREPARE add_auth_provider_statement FROM @add_auth_provider_sql;
EXECUTE add_auth_provider_statement;
DEALLOCATE PREPARE add_auth_provider_statement;

UPDATE users SET auth_provider = 'UNKNOWN';
ALTER TABLE users
    MODIFY COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN';

CREATE TABLE IF NOT EXISTS password_recovery_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_recovery_tokens_token_hash UNIQUE (token_hash),
    INDEX idx_password_recovery_tokens_user_id (user_id),
    CONSTRAINT fk_password_recovery_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
);

ALTER TABLE password_recovery_tokens
    MODIFY COLUMN token_hash CHAR(64) NOT NULL;
