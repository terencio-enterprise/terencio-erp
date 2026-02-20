-- ==================================================================================
-- PARTE 1: TERENCIO ERP - CORE BÁSICO (USUARIOS, ROLES, ORGANIZACIONES)
-- ==================================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================================================================================
-- 1. ACCESS CONTROL (RBAC & PERMISSIONS)
-- ==================================================================================

CREATE TABLE permissions (
    code VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module VARCHAR(50) NOT NULL
);

CREATE TABLE roles (
    name VARCHAR(50) PRIMARY KEY,
    description TEXT,
    is_system_defined BOOLEAN DEFAULT TRUE
);

CREATE TABLE role_permissions (
    role_name VARCHAR(50) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL REFERENCES permissions(code) ON DELETE CASCADE,
    PRIMARY KEY (role_name, permission_code)
);

-- ==================================================================================
-- 2. ORGANIZATION HIERARCHY
-- ==================================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    owner_user_uuid UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(slug)
);
CREATE INDEX idx_organizations_slug ON organizations(slug);

CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL,
    currency_code VARCHAR(3) DEFAULT 'EUR',
    
    -- Fiscal Settings
    fiscal_regime VARCHAR(50) DEFAULT 'COMMON',
    price_includes_tax BOOLEAN DEFAULT TRUE,
    rounding_mode VARCHAR(20) DEFAULT 'LINE',
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1,
    UNIQUE(organization_id, slug)
);
CREATE INDEX idx_companies_organization_id ON companies(organization_id);
CREATE INDEX idx_companies_slug ON companies(slug);

CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    
    is_active BOOLEAN DEFAULT TRUE,
    timezone VARCHAR(50) DEFAULT 'Europe/Madrid',
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1,
    
    UNIQUE(company_id, code),
    UNIQUE(company_id, slug)
);
CREATE INDEX idx_stores_slug ON stores(slug);

CREATE TABLE store_settings (
    store_id UUID PRIMARY KEY REFERENCES stores(id),
    allow_negative_stock BOOLEAN DEFAULT FALSE,
    default_tariff_id BIGINT, 
    print_ticket_automatically BOOLEAN DEFAULT TRUE,
    require_customer_for_large_amount BIGINT, -- stored in cents
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(store_id)
);

-- ==================================================================================
-- 3. EMPLOYEES & SECURITY
-- ==================================================================================

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    pin_hash VARCHAR(255),
    
    full_name VARCHAR(255),
    
    -- Context
    last_active_company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    last_active_store_id UUID REFERENCES stores(id) ON DELETE SET NULL,

    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    UNIQUE(organization_id, username)
);
CREATE INDEX idx_employees_organization_id ON employees(organization_id);

CREATE TABLE employee_access_grants (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL CHECK (scope IN ('ORGANIZATION', 'COMPANY', 'STORE')),
    target_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    extra_permissions JSONB DEFAULT '[]'::jsonb,
    excluded_permissions JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_employee_access_grants_employee_id ON employee_access_grants(employee_id);
CREATE INDEX idx_employee_access_grants_target ON employee_access_grants(scope, target_id);
CREATE UNIQUE INDEX uq_employee_access_grants_unique ON employee_access_grants(employee_id, scope, target_id, role);

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(100),
    serial_code VARCHAR(100) NOT NULL,
    hardware_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    version_app VARCHAR(50),
    device_secret VARCHAR(255),
    api_key_version INTEGER DEFAULT 1,
    last_authenticated_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(store_id, serial_code)
);

CREATE TABLE registration_codes (
    code VARCHAR(10) PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    preassigned_name VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    used_by_device_id UUID REFERENCES devices(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);