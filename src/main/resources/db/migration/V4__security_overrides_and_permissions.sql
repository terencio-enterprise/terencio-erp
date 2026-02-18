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

-- 6. Add Slugs to Organizations, Companies, Stores (Squashed from V101)
-- 1) Columns
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- 2) One-time backfill from name (for existing data)
UPDATE organizations
SET slug = regexp_replace(regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g'), '(^-+|-+$)', '', 'g')
WHERE slug IS NULL OR btrim(slug) = '';

UPDATE companies
SET slug = regexp_replace(regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g'), '(^-+|-+$)', '', 'g')
WHERE slug IS NULL OR btrim(slug) = '';

UPDATE stores
SET slug = regexp_replace(regexp_replace(lower(name), '[^a-z0-9]+', '-', 'g'), '(^-+|-+$)', '', 'g')
WHERE slug IS NULL OR btrim(slug) = '';

-- 3) Default + required
ALTER TABLE organizations ALTER COLUMN slug SET DEFAULT 'slug-pending';
ALTER TABLE companies ALTER COLUMN slug SET DEFAULT 'slug-pending';
ALTER TABLE stores ALTER COLUMN slug SET DEFAULT 'slug-pending';

-- We use DO blocks or separate statements, but Flyway splits by ; usually. 
-- We'll try to enforce NOT NULL. If it fails due to nulls, the previous updates should have fixed it.
ALTER TABLE organizations ALTER COLUMN slug SET NOT NULL;
ALTER TABLE companies ALTER COLUMN slug SET NOT NULL;
ALTER TABLE stores ALTER COLUMN slug SET NOT NULL;

-- 4) Indexes
CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);
CREATE INDEX IF NOT EXISTS idx_companies_slug ON companies(slug);
CREATE INDEX IF NOT EXISTS idx_stores_slug ON stores(slug);
