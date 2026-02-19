-- Seed initial data for "Terencio Organization" and related entities.
-- IMPORTANT: Order matters — organizations must exist before companies (FK NOT NULL).

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Organization (must be first — companies.organization_id NOT NULL)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO organizations (id, name, slug)
SELECT
    gen_random_uuid(),
    'Terencio Organization',
    'terencio-organization'
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'terencio-organization');

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Company — linked to the organization created above
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO companies (id, name, slug, tax_id, organization_id, is_active)
SELECT
    gen_random_uuid(),
    'Kit Cash Sl.',
    'kit-cash-sl',
    'B76594126',
    o.id,
    TRUE
FROM organizations o
WHERE o.slug = 'terencio-organization'
  AND NOT EXISTS (SELECT 1 FROM companies WHERE tax_id = 'B76594126');

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Store (main POS store)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO stores (id, company_id, code, name, slug, address, is_active)
SELECT
    gen_random_uuid(),
    c.id,
    'SC-TF-01',
    'Terencio Cash Market - La Laguna',
    'terencio-cash-market',
    'Carr. de la Esperanza, 22, 38206 San Cristóbal de La Laguna, Santa Cruz de Tenerife (ES)',
    TRUE
FROM companies c
WHERE c.tax_id = 'B76594126'
  AND NOT EXISTS (
      SELECT 1 FROM stores s WHERE s.company_id = c.id AND s.code = 'SC-TF-01'
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Store Settings
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO store_settings (store_id, allow_negative_stock, print_ticket_automatically)
SELECT
    s.id,
    FALSE,
    TRUE
FROM stores s
JOIN companies c ON s.company_id = c.id
WHERE c.tax_id = 'B76594126' AND s.code = 'SC-TF-01'
ON CONFLICT (store_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Initial Admin/Owner Employee
--    Password: 'admin123' (BCrypt hash)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO employees (
    username,
    full_name,
    password_hash,
    organization_id,
    is_active
)
SELECT
    'sergio',
    'Sergio Acosta',
    '$2a$10$.3FPPF1ezyVThH2jeTBcbuDxEnwN9sIdbSiG/zXMCVBh0IpZ9CTA2',
    o.id,
    TRUE
FROM organizations o
WHERE o.slug = 'terencio-organization'
  AND NOT EXISTS (SELECT 1 FROM employees WHERE username = 'sergio');

-- ─────────────────────────────────────────────────────────────────────────────
-- 5.1 Link organization owner
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE organizations o
SET owner_user_uuid = (SELECT uuid FROM employees WHERE username = 'sergio')
WHERE o.slug = 'terencio-organization';

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. Access Grants
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. Base Roles
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO roles (name, description, is_system_defined) VALUES
('ADMIN',     'Administrator - Full access',              TRUE),
('MANAGER',   'Store Manager - Can manage stock and overrides', TRUE),
('CASHIER',   'Cashier - POS sales only',                 TRUE),
('WAREHOUSE', 'Warehouse Staff - Stock management only',  TRUE)
ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. Base Permissions
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO permissions (code, name, description, module) VALUES
('marketing:leads:view',      'Ver Leads',            'Permite consultar el listado de leads',               'MARKETING'),
('marketing:campaign:create', 'Crear Campaña',        'Permite diseñar nuevas campañas',                    'MARKETING'),
('marketing:campaign:view',   'Ver Campañas',         'Permite consultar el historial de campañas',         'MARKETING'),
('marketing:campaign:launch', 'Lanzar Campaña',       'Permite ejecutar el envío de campañas',              'MARKETING'),
('marketing:email:preview',   'Previsualizar Email',  'Permite previsualizar emails antes del envío',       'MARKETING'),
('marketing:template:view',   'Ver Plantillas',       'Permite consultar plantillas de marketing',          'MARKETING'),
('marketing:template:create', 'Crear Plantillas',     'Permite crear nuevas plantillas',                    'MARKETING'),
('marketing:template:edit',   'Editar Plantillas',    'Permite modificar plantillas existentes',            'MARKETING'),
('marketing:template:delete', 'Eliminar Plantillas',  'Permite eliminar plantillas',                        'MARKETING')
ON CONFLICT (code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. Grant all seeded permissions to ADMIN role
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO role_permissions (role_name, permission_code)
SELECT 'ADMIN', p.code
FROM permissions p
ON CONFLICT DO NOTHING;
