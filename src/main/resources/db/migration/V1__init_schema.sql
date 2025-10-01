-- =====================================================================
-- AAA Service - Initial Schema (MariaDB)
-- Flyway Version: V1__init_schema.sql
-- Notes:
--   * All timestamps are UTC (set in application.yml via JDBC TZ).
--   * Use InnoDB + utf8mb4 for full Unicode + reliability.
--   * No seed data here (that goes into V2__seed_roles_permissions.sql).
-- =====================================================================

-- Ensure consistent SQL mode for predictable constraints
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ---------- USERS -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     username          VARCHAR(64)     NOT NULL,
    email             VARCHAR(191)    NOT NULL,
    password_hash     VARCHAR(255)    NOT NULL,         -- BCrypt/Argon2 hash
    enabled           TINYINT(1)      NOT NULL DEFAULT 1,
    account_non_locked TINYINT(1)     NOT NULL DEFAULT 1,
    account_non_expired TINYINT(1)    NOT NULL DEFAULT 1,
    credentials_non_expired TINYINT(1) NOT NULL DEFAULT 1,

    first_name        VARCHAR(100)    NULL,
    last_name         VARCHAR(100)    NULL,
    phone             VARCHAR(40)     NULL,

    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- ROLES -----------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
                                     id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     code        VARCHAR(64)     NOT NULL,   -- e.g., ROLE_ADMIN, ROLE_USER
    name        VARCHAR(128)    NOT NULL,
    description VARCHAR(512)    NULL,

    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- PERMISSIONS ----------------------------------------------
CREATE TABLE IF NOT EXISTS permissions (
                                           id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                           code        VARCHAR(128)    NOT NULL,   -- e.g., user.read, user.write
    name        VARCHAR(128)    NOT NULL,
    description VARCHAR(512)    NULL,

    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- USER <-> ROLE (M:N) --------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id  BIGINT UNSIGNED NOT NULL,
                                          role_id  BIGINT UNSIGNED NOT NULL,
                                          granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- ROLE <-> PERMISSION (M:N) --------------------------------
CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id       BIGINT UNSIGNED NOT NULL,
                                                permission_id BIGINT UNSIGNED NOT NULL,
                                                granted_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES roles (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- API CLIENTS (machine-to-machine) -------------------------
CREATE TABLE IF NOT EXISTS api_clients (
                                           id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                           client_id          VARCHAR(128)    NOT NULL,          -- public identifier
    client_secret_hash VARCHAR(255)    NOT NULL,          -- store only hash
    name               VARCHAR(191)    NOT NULL,          -- display label
    scopes             VARCHAR(512)    NULL,              -- CSV or space-delimited list
    allowed_ips        VARCHAR(1024)   NULL,              -- optional whitelist (CSV/CIDR)
    enabled            TINYINT(1)      NOT NULL DEFAULT 1,

    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_api_clients_client_id (client_id),
    KEY idx_api_clients_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- REFRESH TOKENS -------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                              user_id         BIGINT UNSIGNED NOT NULL,
                                              token_hash      CHAR(64)        NOT NULL,             -- SHA-256 of opaque token (store hash, not token)
    issued_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP       NOT NULL,
    revoked         TINYINT(1)      NOT NULL DEFAULT 0,
    revoked_at      TIMESTAMP       NULL,
    ip_address      VARCHAR(64)     NULL,
    user_agent      VARCHAR(255)    NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- AUDIT LOG -------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
                                         id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                         occurred_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP, -- event time (UTC)
                                         actor_user_id   BIGINT UNSIGNED NULL,        -- nullable for system/m2m events
                                         actor_client_id BIGINT UNSIGNED NULL,        -- api_clients.id when M2M
                                         action          VARCHAR(128)    NOT NULL,    -- e.g., AUTH_LOGIN, USER_CREATE, ROLE_ASSIGN
    target_type     VARCHAR(128)    NULL,        -- e.g., "USER", "ROLE", "PERMISSION", "CLIENT"
    target_id       VARCHAR(191)    NULL,        -- business identifier of target
    request_id      VARCHAR(64)     NULL,        -- trace/correlation id
    ip_address      VARCHAR(64)     NULL,
    user_agent      VARCHAR(255)    NULL,
    details         JSON            NULL,        -- structured payload (MariaDB JSON)
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_audit_log_time (occurred_at),
    KEY idx_audit_log_action (action),
    KEY idx_audit_log_actor_user (actor_user_id),
    KEY idx_audit_log_actor_client (actor_client_id),

    CONSTRAINT fk_audit_log_actor_user
    FOREIGN KEY (actor_user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_audit_log_actor_client
    FOREIGN KEY (actor_client_id) REFERENCES api_clients (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- OPTIONAL: HELPER VIEW (who has which permission) ----------
-- Materialized in DB as view for quick admin queries (read-only).
CREATE OR REPLACE VIEW v_user_permissions AS
SELECT u.id AS user_id,
       u.username,
       r.code AS role_code,
       p.code AS permission_code
FROM users u
         JOIN user_roles ur ON ur.user_id = u.id
         JOIN roles r ON r.id = ur.role_id
         JOIN role_permissions rp ON rp.role_id = r.id
         JOIN permissions p ON p.id = rp.permission_id;

-- ---------- HOUSEKEEPING INDEX HINTS ----------------------------------
-- Add partial/functional indexes later if needed, e.g., on JSON fields:
-- CREATE INDEX idx_audit_log_details_action ON audit_log ((JSON_VALUE(details, '$.action')));

-- =====================================================================
-- End of V1
-- =====================================================================
