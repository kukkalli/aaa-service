-- =====================================================================
-- V3: Add auditing user columns (created_by, last_modified_by)
-- Applies to: users, roles, permissions, api_clients
-- Notes:
--   * Uses IF NOT EXISTS for idempotency.
--   * Leaves values NULL by default; we backfill created_by to 'system'.
-- =====================================================================

SET SESSION sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) NULL AFTER created_by;

-- roles
ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) NULL AFTER created_by;

-- permissions
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) NULL AFTER created_by;

-- api_clients
ALTER TABLE api_clients
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(100) NULL AFTER created_by;

-- Optional: backfill created_by to 'system' for existing rows where null
UPDATE users       SET created_by = 'system' WHERE created_by IS NULL;
UPDATE roles       SET created_by = 'system' WHERE created_by IS NULL;
UPDATE permissions SET created_by = 'system' WHERE created_by IS NULL;
UPDATE api_clients SET created_by = 'system' WHERE created_by IS NULL;

-- =====================================================================
-- End of V3
-- =====================================================================
