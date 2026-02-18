-- Seed initial company data for "Kit Cash Sl."
-- This file should be run after valid schema creation

-- 1. Upsert Company "Kit Cash Sl."
INSERT INTO companies (id, name, tax_id, is_active)
SELECT 
    gen_random_uuid(),
    'Kit Cash Sl.', 
    'B76594126', 
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tax_id = 'B76594126');

-- 2. Upsert Store (main store)
-- We need the company ID.
WITH target_company AS (
    SELECT id FROM companies WHERE tax_id = 'B76594126' LIMIT 1
)
INSERT INTO stores (id, company_id, code, name, address, is_active)
SELECT 
    gen_random_uuid(),
    tc.id,
    'SC-TF-01', -- Invented code
    'Terencio Cash Market - La Laguna',
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
INSERT INTO organizations (id, name)
SELECT
    gen_random_uuid(),
    'Terencio Organization'
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE name = 'Terencio Organization');

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
SET owner_user_uuid = (SELECT id FROM employees WHERE username = 'sergio')
WHERE o.name = 'Terencio Organization'; 

-- 6. Grant Access to the Owner (Store level, Company level, etc.)
-- Since they are Org Admin, they might have implicit access, but V4 cleanup removes store_id from employees.
-- So we must explicitly grant ORGANIZATION scope access in employee_access_grants.
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

