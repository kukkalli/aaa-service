-- =====================================================================
-- AAA Service - Seed Roles & Permissions
-- Flyway Version: V2__seed_roles_permissions.sql
-- Notes:
--   * Uses INSERT IGNORE so it’s safe to re-run (no-ops on duplicates).
--   * Role/permission codes are UNIQUE (see V1 schema).
--   * Maps:
--       - ROLE_ADMIN      → all permissions
--       - ROLE_USER       → basic auth + user.read
--       - ROLE_AUDITOR    → audit.read
--       - ROLE_SERVICE    → client.read
-- =====================================================================

SET SESSION sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ---------- ROLES -----------------------------------------------------
INSERT IGNORE INTO roles (code, name, description)
VALUES
  ('ROLE_ADMIN',   'Administrator', 'Full access to manage users, roles, permissions, clients, and audit'),
  ('ROLE_USER',    'User',          'Standard user with limited access'),
  ('ROLE_AUDITOR', 'Auditor',       'Read-only access to audit logs and system state'),
  ('ROLE_SERVICE', 'Service',       'Service-to-service client role (M2M)');

-- ---------- PERMISSIONS ----------------------------------------------
-- Naming convention: <resource>.<action>  (e.g., user.read, role.create)
INSERT IGNORE INTO permissions (code, name, description)
VALUES
  -- Auth/session
  ('auth.login',        'Login',                     'Obtain access/refresh tokens'),
  ('auth.refresh',      'Refresh Token',             'Refresh access tokens using a valid refresh token'),
  ('auth.logout',       'Logout',                    'Invalidate/rotate refresh tokens'),

  -- Users
  ('user.read',         'Read Users',                'List and view users'),
  ('user.create',       'Create User',               'Create a new user'),
  ('user.update',       'Update User',               'Update existing user'),
  ('user.delete',       'Delete User',               'Delete/deactivate user'),

  -- Roles
  ('role.read',         'Read Roles',                'List and view roles'),
  ('role.create',       'Create Role',               'Create a new role'),
  ('role.update',       'Update Role',               'Update existing role'),
  ('role.delete',       'Delete Role',               'Delete role'),

  -- Permissions
  ('permission.read',   'Read Permissions',          'List and view permissions'),

  -- API Clients (machine-to-machine)
  ('client.read',       'Read API Clients',          'List and view API clients'),
  ('client.create',     'Create API Client',         'Create a new API client'),
  ('client.update',     'Update API Client',         'Update existing API client'),
  ('client.delete',     'Delete API Client',         'Delete API client'),

  -- Audit
  ('audit.read',        'Read Audit Log',            'Read audit log entries'),
  ('audit.write',       'Write Audit Log',           'Record custom audit events');

-- ---------- ROLE ↔ PERMISSION MAPPINGS -------------------------------
-- ADMIN → all permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
         JOIN permissions p
WHERE r.code = 'ROLE_ADMIN';

-- USER → basic auth + user.read
INSERT IGNORE INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
         JOIN permissions p ON p.code IN ('auth.login','auth.refresh','auth.logout','user.read')
WHERE r.code = 'ROLE_USER';

-- AUDITOR → audit.read (read-only)
INSERT IGNORE INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
         JOIN permissions p ON p.code IN ('audit.read')
WHERE r.code = 'ROLE_AUDITOR';

-- SERVICE → client.read (typical minimal M2M)
INSERT IGNORE INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
         JOIN permissions p ON p.code IN ('client.read')
WHERE r.code = 'ROLE_SERVICE';

-- =====================================================================
-- End of V2
-- =====================================================================
