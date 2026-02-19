-- ==================================================================================
-- TERENCIO ERP - CORE SCHEMA (V1 SQUASHED)
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

-- ==================================================================================
-- 4. ASSET MANAGEMENT & MARKETING
-- ==================================================================================

-- Generic Asset Manager (Agnostic of S3/GCS/Local)
CREATE TABLE company_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    
    -- Generic storage pointer (e.g., 'assets/img/logo.png')
    storage_path VARCHAR(500) NOT NULL,
    public_url VARCHAR(1000), -- Explicit public URL for quick rendering in email templates
    is_public BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_company_assets_company ON company_assets(company_id);

CREATE TABLE marketing_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_html TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(company_id, code)
);

CREATE TABLE marketing_logs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id BIGINT, -- Will map to customers table defined below
    template_id BIGINT REFERENCES marketing_templates(id),
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    status VARCHAR(20) NOT NULL,
    message_id VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_marketing_logs_customer ON marketing_logs(customer_id);
CREATE INDEX idx_marketing_logs_template ON marketing_logs(template_id);
CREATE INDEX idx_marketing_logs_status ON marketing_logs(company_id, status);

CREATE TABLE email_delivery_events (
    id BIGSERIAL PRIMARY KEY,
    provider_message_id VARCHAR(255), -- Agnostic message ID from sending provider
    email_address VARCHAR(255),
    event_type VARCHAR(50), -- BOUNCE, COMPLAINT, DELIVERY
    bounce_type VARCHAR(50), 
    bounce_subtype VARCHAR(50), 
    raw_payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 5. CRM & CUSTOMERS
-- ==================================================================================

-- Combined Customers + Leads Table
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    -- Business Data
    tax_id VARCHAR(50),
    legal_name VARCHAR(255),
    commercial_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    country VARCHAR(10) DEFAULT 'ES',
    
    -- Commercial settings (References Tariff later)
    tariff_id BIGINT, 
    allow_credit BOOLEAN DEFAULT FALSE,
    credit_limit BIGINT DEFAULT 0,
    surcharge_apply BOOLEAN DEFAULT FALSE,
    
    -- CRM/Lead Data
    type VARCHAR(20) DEFAULT 'LEAD', -- LEAD, CLIENT
    origin VARCHAR(50),
    tags TEXT[],
    marketing_consent BOOLEAN DEFAULT FALSE,
    marketing_status VARCHAR(20) DEFAULT 'SUBSCRIBED',
    unsubscribe_token VARCHAR(64),
    last_interaction_at TIMESTAMPTZ,
    snoozed_until TIMESTAMPTZ,
    
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_customers_email_company_unique ON customers (company_id, LOWER(email)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_customers_unsubscribe_token ON customers (unsubscribe_token);

ALTER TABLE marketing_logs ADD CONSTRAINT fk_marketing_logs_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- ==================================================================================
-- 6. CATALOG & SALES DATA
-- ==================================================================================

CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(10,4) NOT NULL,
    surcharge DECIMAL(10,4) DEFAULT 0,
    code_aeat VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    parent_id BIGINT REFERENCES categories(id),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    image_url TEXT,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    reference VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100),
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    tax_id BIGINT NOT NULL REFERENCES taxes(id),
    brand VARCHAR(100),
    type VARCHAR(50) DEFAULT 'PRODUCT',
    is_weighted BOOLEAN DEFAULT FALSE,
    is_inventoriable BOOLEAN DEFAULT TRUE,
    min_stock_alert DECIMAL(15,3) DEFAULT 0,
    average_cost BIGINT DEFAULT 0,
    last_purchase_cost BIGINT DEFAULT 0,
    image_url TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1,
    UNIQUE(company_id, reference)
);
CREATE INDEX idx_products_search ON products(company_id, name);
CREATE INDEX idx_products_ref ON products(company_id, reference);

CREATE TABLE product_barcodes (
    barcode VARCHAR(100) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'EAN13',
    quantity_factor DECIMAL(10,3) DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    priority INT DEFAULT 0,
    price_type VARCHAR(20) DEFAULT 'RETAIL',
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1
);

ALTER TABLE customers ADD CONSTRAINT fk_customers_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs(id);

CREATE TABLE product_prices (
    product_id BIGINT NOT NULL REFERENCES products(id),
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id),
    price BIGINT NOT NULL,
    cost_price BIGINT,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, tariff_id)
);

CREATE TABLE customer_product_prices (
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    custom_price BIGINT NOT NULL,
    valid_from TIMESTAMPTZ DEFAULT NOW(),
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (customer_id, product_id)
);

CREATE TABLE pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    condition_json JSONB NOT NULL,
    effect_json JSONB NOT NULL,
    priority INT DEFAULT 10,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID REFERENCES companies(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_cash BOOLEAN DEFAULT FALSE,
    requires_reference BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE(company_id, code)
);

CREATE TABLE document_types (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE customer_account_movements (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    reference_uuid UUID,
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 7. INVENTORY & LOGISTICS
-- ==================================================================================

CREATE TABLE inventory_stock (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    quantity_on_hand DECIMAL(15,3) DEFAULT 0,
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1,
    PRIMARY KEY (product_id, warehouse_id)
);
CREATE INDEX idx_inventory_lookup ON inventory_stock(company_id, product_id, warehouse_id);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('SALE', 'RETURN', 'ADJUSTMENT')),
    quantity DECIMAL(15,3) NOT NULL,
    previous_balance DECIMAL(15,3) NOT NULL,
    new_balance DECIMAL(15,3) NOT NULL,
    cost_unit BIGINT,
    reason VARCHAR(255),
    reference_doc_type VARCHAR(50) CHECK (reference_doc_type IN ('SALE', 'RECTIFICATION', 'ADJUSTMENT')),
    reference_doc_uuid UUID,
    user_id BIGINT REFERENCES employees(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_stock_movements_wh ON stock_movements(warehouse_id);
CREATE INDEX idx_stock_movements_prod ON stock_movements(product_id);

-- ==================================================================================
-- 8. POS OPERATIONS & SALES
-- ==================================================================================

CREATE TABLE device_sequences (
    device_id UUID NOT NULL REFERENCES devices(id),
    series VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    current_value INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (device_id, series, year)
);

CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    user_id BIGINT REFERENCES employees(id),
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    amount_initial BIGINT DEFAULT 0,
    amount_system BIGINT DEFAULT 0,
    amount_counted BIGINT,
    amount_diff BIGINT,
    status VARCHAR(20) DEFAULT 'OPEN',
    z_count INT,
    z_report_signature TEXT,
    synced_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    series VARCHAR(50) NOT NULL,
    number INT NOT NULL,
    full_reference VARCHAR(100) NOT NULL,
    
    type VARCHAR(50) NOT NULL DEFAULT 'SIMPLIFIED' REFERENCES document_types(code),
    
    shift_id UUID REFERENCES shifts(id),
    user_id BIGINT REFERENCES employees(id),
    customer_id BIGINT REFERENCES customers(id) ON DELETE RESTRICT,
    
    -- Fiscal Snapshot
    customer_tax_id VARCHAR(50),
    customer_legal_name VARCHAR(255),
    customer_address TEXT,
    customer_zip VARCHAR(20),
    customer_city VARCHAR(100),
    store_tax_id VARCHAR(50),
    store_legal_name VARCHAR(255),
    store_address TEXT,
    
    created_at_pos TIMESTAMPTZ NOT NULL,
    issued_at_pos TIMESTAMPTZ,
    received_at_server TIMESTAMPTZ DEFAULT NOW(),
    
    status VARCHAR(20) DEFAULT 'DRAFT',
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    sync_batch_id UUID,
    is_offline BOOLEAN DEFAULT FALSE,
    
    -- Totals (cents)
    total_net BIGINT DEFAULT 0,
    total_tax BIGINT DEFAULT 0,
    total_surcharge BIGINT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    
    -- Refunds
    original_sale_uuid UUID REFERENCES sales(uuid),
    refund_reason TEXT,
    original_series VARCHAR(50),
    original_number INT,
    original_issue_date TIMESTAMPTZ,
    rectification_type VARCHAR(50),
    rectification_legal_reason VARCHAR(255),
    rectified_by_uuid UUID REFERENCES sales(uuid),
    
    UNIQUE(store_id, full_reference),
    UNIQUE(device_id, series, number),
    
    CONSTRAINT chk_sales_totals CHECK (ABS(total_amount - (total_net + total_tax + total_surcharge)) <= 1),
    CONSTRAINT chk_credit_note_logic CHECK ((type != 'CREDIT_NOTE') OR (original_sale_uuid IS NOT NULL))
);
CREATE INDEX idx_sales_date ON sales(issued_at_pos);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_uuid ON sales(uuid);
CREATE INDEX idx_sales_store_date ON sales(store_id, issued_at_pos);

CREATE TABLE sale_lines (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    unit_price BIGINT NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount BIGINT DEFAULT 0,
    tax_id BIGINT REFERENCES taxes(id),
    tax_rate DECIMAL(10,4) NOT NULL,
    tax_amount BIGINT NOT NULL,
    total_line BIGINT NOT NULL,
    pricing_context JSONB,
    CONSTRAINT chk_line_totals CHECK (ABS(total_line - ((quantity * unit_price) - discount_amount + tax_amount)) <= quantity::DECIMAL)
);
CREATE INDEX idx_sale_lines_sale ON sale_lines(sale_uuid);

CREATE TABLE sale_taxes (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    tax_id BIGINT REFERENCES taxes(id), 
    tax_name VARCHAR(100) NOT NULL,
    tax_rate DECIMAL(10,4) NOT NULL,
    taxable_base BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    surcharge_rate DECIMAL(10,4) DEFAULT 0,
    surcharge_amount BIGINT DEFAULT 0,
    UNIQUE(sale_uuid, tax_rate, surcharge_rate)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    payment_method_id BIGINT REFERENCES payment_methods(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    exchange_rate DECIMAL(10,4) DEFAULT 1,
    payment_data JSONB,
    created_at_pos TIMESTAMPTZ NOT NULL,
    UNIQUE(sale_uuid, payment_method_id, created_at_pos)
);

-- ==================================================================================
-- 9. COMPLIANCE & ACCOUNTING
-- ==================================================================================

CREATE TABLE fiscal_audit_log (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    event_type VARCHAR(20) NOT NULL,
    previous_record_hash VARCHAR(64) NOT NULL,
    chain_sequence_id INT NOT NULL,
    record_hash VARCHAR(64) NOT NULL,
    signature TEXT,
    software_id VARCHAR(100) NOT NULL,
    software_version VARCHAR(50) NOT NULL,
    developer_id VARCHAR(100) NOT NULL,
    certification_reference VARCHAR(100),
    invoice_amount BIGINT NOT NULL,
    invoice_date TIMESTAMPTZ NOT NULL,
    aeat_status VARCHAR(20) DEFAULT 'PENDING',
    aeat_csv_sent TEXT,
    aeat_response_json JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, chain_sequence_id),
    UNIQUE(sale_uuid, event_type),
    CONSTRAINT chk_hash_length CHECK (LENGTH(record_hash) = 64),
    CONSTRAINT chk_prev_hash_length CHECK (LENGTH(previous_record_hash) = 64)
);
CREATE INDEX idx_fiscal_chain ON fiscal_audit_log(device_id, chain_sequence_id);

CREATE TABLE accounting_entries (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    reference_type VARCHAR(50),
    reference_uuid UUID,
    entry_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE accounting_entry_lines (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT REFERENCES accounting_entries(id) ON DELETE CASCADE,
    account_code VARCHAR(20) NOT NULL,
    debit BIGINT DEFAULT 0,
    credit BIGINT DEFAULT 0,
    CONSTRAINT chk_debit_credit_exclusive CHECK ((debit = 0 AND credit > 0) OR (credit = 0 AND debit > 0))
);

CREATE TABLE audit_user_actions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES employees(id),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(50),
    entity_id VARCHAR(100),
    payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);