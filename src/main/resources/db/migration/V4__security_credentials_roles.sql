CREATE TABLE user_credentials (
                                  user_id             BIGINT       PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                  password_hash       VARCHAR(100) NOT NULL,
                                  password_changed_at TIMESTAMP,
                                  CONSTRAINT chk_password_hash_not_blank CHECK (length(trim(password_hash)) > 0)
);

CREATE TABLE user_roles (
                            user_id BIGINT      NOT NULL REFERENCES user_credentials(user_id) ON DELETE CASCADE,
                            role    VARCHAR(20) NOT NULL,
                            PRIMARY KEY (user_id, role),
                            CONSTRAINT chk_user_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

CREATE INDEX idx_user_roles_role ON user_roles(role);