-- 1. Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    code VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module VARCHAR(50) NOT NULL
);

-- 2. Create roles table
CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(50) PRIMARY KEY,
    description TEXT,
    is_system_defined BOOLEAN DEFAULT TRUE
);

-- 3. Create role_permissions table to define the "Base Role" permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    role_name VARCHAR(50) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL REFERENCES permissions(code) ON DELETE CASCADE,
    PRIMARY KEY (role_name, permission_code)
);

-- Seed existing roles
INSERT INTO roles (name, description, is_system_defined) VALUES
('ADMIN', 'Administrator - Full access', TRUE),
('MANAGER', 'Store Manager - Can manage stock and overrides', TRUE),
('CASHIER', 'Cashier - POS sales only', TRUE),
('WAREHOUSE', 'Warehouse Staff - Stock management only', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Seed Marketing Permissions
INSERT INTO permissions (code, name, description, module) VALUES
('marketing:leads:view', 'Ver Leads', 'Permite consultar el listado de leads', 'MARKETING'),
('marketing:campaign:create', 'Crear Campaña', 'Permite diseñar nuevas campañas', 'MARKETING'),
('marketing:email:preview', 'Previsualizar Email', 'Permite previsualizar emails antes del envío', 'MARKETING'),
('marketing:campaign:launch', 'Lanzar Campaña', 'Permite ejecutar el envío de campañas', 'MARKETING')
ON CONFLICT (code) DO NOTHING;

-- Assign Marketing permissions to ADMIN (and maybe Manager later, but for now just Admin gets full suite)
INSERT INTO role_permissions (role_name, permission_code) VALUES
('ADMIN', 'marketing:leads:view'),
('ADMIN', 'marketing:campaign:create'),
('ADMIN', 'marketing:email:preview'),
('ADMIN', 'marketing:campaign:launch')
ON CONFLICT DO NOTHING;

-- 4. Update employee_access_grants with JSONB columns for overrides
ALTER TABLE employee_access_grants
ADD COLUMN IF NOT EXISTS extra_permissions JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS excluded_permissions JSONB DEFAULT '[]'::jsonb;

-- 5. Cleanup: If user has Organization Grant, remove them from specific store (set store_id null)
-- Logic: If I have an ORGANIZATION scope grant, I am an HQ employee, not bound to a store.
UPDATE employees e
SET store_id = NULL
WHERE EXISTS (
    SELECT 1 FROM employee_access_grants g
    WHERE g.employee_id = e.id
    AND g.scope = 'ORGANIZATION'
);
