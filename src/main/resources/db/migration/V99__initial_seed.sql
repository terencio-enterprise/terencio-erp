-- Seed initial company data for "Kit Cash Sl."
-- This file should be run after valid schema creation

-- 1. Upsert Company "Kit Cash Sl."
INSERT INTO companies (id, name, slug, tax_id, is_active)
SELECT 
    gen_random_uuid(),
    'Kit Cash Sl.', 
    'kit-cash-sl',
    'B76594126', 
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tax_id = 'B76594126');

-- 2. Upsert Store (main store)
-- We need the company ID.
WITH target_company AS (
    SELECT id FROM companies WHERE tax_id = 'B76594126' LIMIT 1
)
INSERT INTO stores (id, company_id, code, name, slug, address, is_active)
SELECT 
    gen_random_uuid(),
    tc.id,
    'SC-TF-01', -- Invented code
    'Terencio Cash Market - La Laguna',
    'terencio-cash-market',
    'Carr. de la Esperanza, 22, 38206 San Cristóbal de La Laguna, Santa Cruz de Tenerife (ES)',
    TRUE
FROM target_company tc
WHERE NOT EXISTS (SELECT 1 FROM stores s WHERE s.company_id = tc.id AND s.code = 'SC-TF-01');

-- 3. Upsert Store Settings for the new store
INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically)
SELECT 
    s.id,
    FALSE,
    TRUE
FROM stores s
JOIN companies c ON s.company_id = c.id
WHERE c.tax_id = 'B76594126' AND s.code = 'SC-TF-01'
ON CONFLICT (store_id) DO NOTHING;

-- 4. Upsert Organization
-- Assuming the company belongs to an organization. If not exists, we create one.
INSERT INTO organizations (id, name, slug)
SELECT
    gen_random_uuid(),
    'Terencio Organization',
    'terencio-organization'
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE name = 'Terencio Organization');

-- 4.1 Ensure seeded company is linked to the seeded organization
UPDATE companies c
SET organization_id = o.id
FROM organizations o
WHERE c.tax_id = 'B76594126'
    AND o.name = 'Terencio Organization'
    AND c.organization_id IS NULL;

-- 5. Initial Admin/Owner User
-- This user should have access to the Organization, Company, and Store.
-- We'll give them ROLE 'ADMIN' and assign them to the Organization.
-- Password 'admin123' (BCrypt hash)
WITH target_org AS (
    SELECT id FROM organizations WHERE name = 'Terencio Organization' LIMIT 1
),
target_store AS (
    SELECT id FROM stores WHERE code = 'SC-TF-01' LIMIT 1
)
INSERT INTO employees (
    username, 
    full_name, 
    role, 
    password_hash, 
    organization_id, 
    is_active,
    permissions_json -- Legacy field, kept for compatibility or empty
)
SELECT 
    'sergio', 
    'Sergio Acosta',
    'ADMIN', 
    '$2a$10$.3FPPF1ezyVThH2jeTBcbuDxEnwN9sIdbSiG/zXMCVBh0IpZ9CTA2',
    o.id,
    TRUE,
    '[]'::jsonb
FROM target_org o
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE username = 'sergio');

UPDATE organizations o
SET owner_user_uuid = (SELECT uuid FROM employees WHERE username = 'sergio')
WHERE o.name = 'Terencio Organization'; 

-- 6. Grant Access to the Owner at all hierarchy levels.
-- Seed policy: only 'sergio' profile is created in this migration.
-- We grant ORGANIZATION + COMPANY + STORE to ensure complete access context.
INSERT INTO employee_access_grants (employee_id, scope, target_id, role, extra_permissions, excluded_permissions)
SELECT 
    e.id,
    'ORGANIZATION',
    e.organization_id,
    'ADMIN',
    '[]'::jsonb,
    '[]'::jsonb
FROM employees e
WHERE e.username = 'sergio'
ON CONFLICT DO NOTHING;

INSERT INTO employee_access_grants (employee_id, scope, target_id, role, extra_permissions, excluded_permissions)
SELECT
    e.id,
    'COMPANY',
    c.id,
    'ADMIN',
    '[]'::jsonb,
    '[]'::jsonb
FROM employees e
JOIN companies c ON c.organization_id = e.organization_id
WHERE e.username = 'sergio'
ON CONFLICT DO NOTHING;

INSERT INTO employee_access_grants (employee_id, scope, target_id, role, extra_permissions, excluded_permissions)
SELECT
    e.id,
    'STORE',
    s.id,
    'ADMIN',
    '[]'::jsonb,
    '[]'::jsonb
FROM employees e
JOIN companies c ON c.organization_id = e.organization_id
JOIN stores s ON s.company_id = c.id
WHERE e.username = 'sergio'
ON CONFLICT DO NOTHING;

-- 7. Base roles seed (moved from V4)
INSERT INTO roles (name, description, is_system_defined) VALUES
('ADMIN', 'Administrator - Full access', TRUE),
('MANAGER', 'Store Manager - Can manage stock and overrides', TRUE),
('CASHIER', 'Cashier - POS sales only', TRUE),
('WAREHOUSE', 'Warehouse Staff - Stock management only', TRUE)
ON CONFLICT (name) DO NOTHING;

-- 8. Base marketing permissions seed (moved from V4)
INSERT INTO permissions (code, name, description, module) VALUES
('marketing:leads:view', 'Ver Leads', 'Permite consultar el listado de leads', 'MARKETING'),
('marketing:campaign:create', 'Crear Campaña', 'Permite diseñar nuevas campañas', 'MARKETING'),
('marketing:email:preview', 'Previsualizar Email', 'Permite previsualizar emails antes del envío', 'MARKETING'),
('marketing:campaign:launch', 'Lanzar Campaña', 'Permite ejecutar el envío de campañas', 'MARKETING')
ON CONFLICT (code) DO NOTHING;

-- 9. Marketing permissions expansion seed (consolidated here from V100)
INSERT INTO permissions (code, name, description, module) VALUES
('marketing:campaign:view', 'Ver Campañas', 'Permite consultar el historial de campañas', 'MARKETING'),
('marketing:template:view', 'Ver Plantillas', 'Permite consultar plantillas de marketing', 'MARKETING'),
('marketing:template:create', 'Crear Plantillas', 'Permite crear nuevas plantillas', 'MARKETING'),
('marketing:template:edit', 'Editar Plantillas', 'Permite modificar plantillas existentes', 'MARKETING'),
('marketing:template:delete', 'Eliminar Plantillas', 'Permite eliminar plantillas', 'MARKETING')
ON CONFLICT (code) DO NOTHING;

-- Ensure ADMIN has all currently seeded permissions.
INSERT INTO role_permissions (role_name, permission_code)
SELECT 'ADMIN', p.code
FROM permissions p
ON CONFLICT DO NOTHING;

