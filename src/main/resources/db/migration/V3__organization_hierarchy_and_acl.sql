-- ==================================================================================
-- ARC-002: Organization Hierarchy & Granular Access Control (ACL)
-- Hard cutover: users -> employees, remove legacy employee.company_id
-- ==================================================================================

-- 1) Root organization entity
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    owner_user_uuid UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2) Company linked to organization (nullable for compatibility during rollout)
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE companies
    ADD CONSTRAINT fk_companies_organization
    FOREIGN KEY (organization_id)
    REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_companies_organization_id ON companies(organization_id);

-- 3) Rename users to employees (domain terminology)
ALTER TABLE IF EXISTS users RENAME TO employees;

-- 4) Employee linked to organization
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_organization
    FOREIGN KEY (organization_id)
    REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_employees_organization_id ON employees(organization_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_employees_org_username
    ON employees(organization_id, username)
    WHERE deleted_at IS NULL;

-- 5) Grants table
CREATE TABLE IF NOT EXISTS employee_access_grants (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_employee_access_grants_scope CHECK (scope IN ('ORGANIZATION', 'COMPANY', 'STORE'))
);

CREATE INDEX IF NOT EXISTS idx_employee_access_grants_employee_id ON employee_access_grants(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_access_grants_target ON employee_access_grants(scope, target_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_access_grants_unique
    ON employee_access_grants(employee_id, scope, target_id, role);

-- 6) Backfill organizations (1:1 from existing companies)
CREATE TEMP TABLE tmp_company_org_map (
    company_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_company_org_map (company_id, organization_id)
SELECT c.id, uuid_generate_v4()
FROM companies c;

INSERT INTO organizations (id, name, subscription_plan, created_at, updated_at)
SELECT m.organization_id,
       CONCAT(c.name, ' Organization'),
       'STANDARD',
       NOW(),
       NOW()
FROM tmp_company_org_map m
JOIN companies c ON c.id = m.company_id
ON CONFLICT (id) DO NOTHING;

UPDATE companies c
SET organization_id = m.organization_id
FROM tmp_company_org_map m
WHERE c.id = m.company_id
  AND c.organization_id IS NULL;

UPDATE employees e
SET organization_id = c.organization_id
FROM companies c
WHERE e.company_id = c.id
    AND e.organization_id IS NULL;

-- 7) Backfill grants from legacy employees table
INSERT INTO employee_access_grants (employee_id, scope, target_id, role, created_at)
SELECT e.id,
             CASE WHEN e.store_id IS NOT NULL THEN 'STORE' ELSE 'COMPANY' END,
             CASE WHEN e.store_id IS NOT NULL THEN e.store_id ELSE e.company_id END,
             e.role,
       NOW()
FROM employees e
WHERE (e.store_id IS NOT NULL OR e.company_id IS NOT NULL)
ON CONFLICT DO NOTHING;

-- 8) Remove legacy employee.company_id (organization + grants are source of truth)
ALTER TABLE employees DROP CONSTRAINT IF EXISTS users_company_id_fkey;
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_company_id_fkey;
ALTER TABLE employees DROP CONSTRAINT IF EXISTS users_company_id_username_key;
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_company_id_username_key;
ALTER TABLE employees DROP COLUMN IF EXISTS company_id;
