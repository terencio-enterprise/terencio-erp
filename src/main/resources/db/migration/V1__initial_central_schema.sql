-- ==================================================================================
-- PROJECT: TERENCIO POS - CENTRAL BACKEND (PostgreSQL)
-- SCOPE: Centralized Management, Master Data, Sales Aggregation, VeriFactu
-- ENGINE: PostgreSQL 16+
-- ADAPTED FROM: SQLite Schema v1.3.0 (Strict Fiscal Integrity)
-- ==================================================================================

-- Enable UUID extension for generating UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==================================================================================
-- 1. TENANCY & STRUCTURE (Multi-Store Support)
-- ==================================================================================

-- Stores (Tiendas)
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'MAD-001'
    name VARCHAR(255) NOT NULL,
    address TEXT,
    tax_id VARCHAR(50), -- CIF/NIF of the store/company
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Devices (POS Terminals) - Registered to stores
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    serial_code VARCHAR(100) NOT NULL, -- Logical ID 'POS-01'
    hardware_id VARCHAR(255) NOT NULL, -- Physical fingerprint (pos_uuid from local)
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACTIVE, BLOCKED
    last_sync_at TIMESTAMPTZ,
    version_app VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(store_id, serial_code)
);

-- ==================================================================================
-- 2. MASTER DATA (CATALOG & PRICING)
-- ==================================================================================

-- Taxes (Impuestos)
CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY, -- Central Numeric ID (synced to local)
    store_id UUID REFERENCES stores(id), -- Nullable for Global Taxes, Specific for Store overrides
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(10,4) NOT NULL,
    surcharge DECIMAL(10,4) DEFAULT 0,
    code_aeat VARCHAR(50),
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    active BOOLEAN DEFAULT TRUE
);

-- Products (Productos)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY, -- Central Numeric ID
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    store_id UUID REFERENCES stores(id), -- Nullable for Global Products
    reference VARCHAR(100),
    
    -- Display
    name VARCHAR(255) NOT NULL,
    short_name VARCHAR(50),
    description TEXT,
    
    -- Categorization
    category_id BIGINT,
    family_code VARCHAR(50),
    
    -- Fiscal
    tax_id BIGINT NOT NULL REFERENCES taxes(id),
    
    -- Logic Flags
    is_weighted BOOLEAN DEFAULT FALSE,
    is_service BOOLEAN DEFAULT FALSE,
    is_age_restricted BOOLEAN DEFAULT FALSE,
    requires_manager BOOLEAN DEFAULT FALSE,
    
    -- Inventory (Global tracking flags)
    stock_tracking BOOLEAN DEFAULT TRUE,
    min_stock_alert DECIMAL(15,3) DEFAULT 0,
    
    -- Meta
    image_url TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE(store_id, reference) -- Unique reference per store (or global)
);

-- Product Barcodes (Códigos de Barras)
CREATE TABLE product_barcodes (
    barcode VARCHAR(100) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'EAN13',
    is_primary BOOLEAN DEFAULT FALSE,
    quantity_factor DECIMAL(10,3) DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tariffs (Tarifas)
CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    store_id UUID REFERENCES stores(id),
    name VARCHAR(100) NOT NULL,
    priority INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

-- Product Prices (Precios)
CREATE TABLE product_prices (
    product_id BIGINT REFERENCES products(id),
    tariff_id BIGINT REFERENCES tariffs(id),
    price DECIMAL(10,4) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, tariff_id)
);

-- Promotions (Promociones)
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    store_id UUID REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    priority INT DEFAULT 10,
    rules_json JSONB NOT NULL, -- Flexible rules engine
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 3. STOCK (Centralized Inventory)
-- ==================================================================================

CREATE TABLE inventory (
    product_id BIGINT REFERENCES products(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    stock_current DECIMAL(15,3) DEFAULT 0,
    reserved_stock DECIMAL(15,3) DEFAULT 0, -- From open baskets
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, store_id)
);

-- ==================================================================================
-- 4. CUSTOMERS (CRM)
-- ==================================================================================

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    store_id UUID REFERENCES stores(id), -- Nullable for Global Customers
    
    tax_id VARCHAR(50), -- NIF/CIF
    legal_name VARCHAR(255),
    commercial_name VARCHAR(255),
    address TEXT,
    zip_code VARCHAR(20),
    email VARCHAR(255),
    phone VARCHAR(50),
    
    tariff_id BIGINT REFERENCES tariffs(id),
    allow_credit BOOLEAN DEFAULT FALSE,
    credit_limit DECIMAL(15,2) DEFAULT 0,
    surcharge_apply BOOLEAN DEFAULT FALSE,
    
    verifactu_ref VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 5. STAFF & ACCESS
-- ==================================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    store_id UUID REFERENCES stores(id), -- Staff belongs to a store (usually)
    username VARCHAR(100) NOT NULL UNIQUE,
    pin_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'CASHIER',
    permissions_json JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 6. SALES AGGREGATION (From POS)
-- ==================================================================================

-- Document Sequences (Central Registry/Backup)
CREATE TABLE doc_sequences (
    store_id UUID NOT NULL REFERENCES stores(id),
    series VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    current_value INT DEFAULT 0,
    PRIMARY KEY (store_id, series, year)
);

-- Shifts (Turnos) - Synced for Audit
CREATE TABLE shifts (
    id UUID PRIMARY KEY, -- Synced UUID from POS
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    user_id BIGINT REFERENCES users(id),
    
    opened_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    
    amount_initial DECIMAL(15,2),
    amount_system DECIMAL(15,2),
    amount_counted DECIMAL(15,2),
    amount_diff DECIMAL(15,2),
    
    status VARCHAR(20),
    
    -- Z Report Data
    z_report_number INT,
    z_series VARCHAR(50),
    z_year INT,
    z_report_hash VARCHAR(255),
    z_report_signature TEXT,
    
    reopened BOOLEAN DEFAULT FALSE,
    reopened_reason TEXT,
    
    synced_at TIMESTAMPTZ DEFAULT NOW()
);

-- Sales Header
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE, -- Global UUID generated by POS
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    -- Document ID
    series VARCHAR(50) NOT NULL,
    number INT NOT NULL,
    full_reference VARCHAR(100) NOT NULL,
    
    -- Legal Type
    type VARCHAR(20) NOT NULL DEFAULT 'SIMPLIFIED',
    billing_upgraded_from_simplified BOOLEAN DEFAULT FALSE,
    
    -- Timing
    created_at_pos TIMESTAMPTZ NOT NULL,
    issued_at_pos TIMESTAMPTZ,
    received_at_server TIMESTAMPTZ DEFAULT NOW(),
    
    -- Context
    shift_uuid UUID REFERENCES shifts(id),
    user_id BIGINT REFERENCES users(id),
    customer_id BIGINT REFERENCES customers(id),
    
    -- Status
    status VARCHAR(20) DEFAULT 'DRAFT',
    is_fiscal_issued BOOLEAN DEFAULT FALSE,
    
    -- Rectification / Audit
    rectifies_uuid UUID REFERENCES sales(uuid),
    rectification_reason TEXT,
    void_reason_code VARCHAR(50),
    void_reason_text TEXT,
    
    -- Financials
    total_net DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_surcharge DECIMAL(15,2) DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    
    -- Verification
    qr_data TEXT,
    
    UNIQUE(store_id, full_reference)
);

-- Billing Snapshot (Factura Completa Info)
CREATE TABLE sale_billing_info (
    sale_uuid UUID PRIMARY KEY REFERENCES sales(uuid),
    customer_legal_name VARCHAR(255) NOT NULL,
    customer_nif VARCHAR(50) NOT NULL,
    customer_address TEXT,
    customer_city VARCHAR(100),
    customer_zip VARCHAR(20),
    customer_country VARCHAR(10) DEFAULT 'ES'
);

-- Sale Lines
CREATE TABLE sale_lines (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    
    -- Traceability
    status VARCHAR(20) DEFAULT 'ACTIVE',
    returned_from_sale_uuid UUID,
    returned_from_line_id BIGINT,
    
    product_id BIGINT REFERENCES products(id),
    barcode_used VARCHAR(100),
    description_snapshot VARCHAR(255) NOT NULL,
    
    quantity DECIMAL(15,3) NOT NULL,
    unit_price DECIMAL(15,4) NOT NULL,
    
    -- Discounts & Promo Snapshot
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    promotion_applied_id BIGINT REFERENCES promotions(id),
    promotion_snapshot JSONB, -- Stores name, type, discount details
    
    -- Taxes Snapshot
    tax_id BIGINT NOT NULL REFERENCES taxes(id),
    tax_rate_snapshot DECIMAL(10,4) NOT NULL,
    tax_amount DECIMAL(15,4) NOT NULL,
    
    -- Surcharge Snapshot
    surcharge_rate DECIMAL(10,4) DEFAULT 0,
    surcharge_amount DECIMAL(15,4) DEFAULT 0,
    
    -- Scale Data
    weight_read DECIMAL(15,3),
    scale_id VARCHAR(50),
    plu_prefix VARCHAR(10),
    embedded_weight DECIMAL(15,3),
    embedded_price DECIMAL(15,4),
    
    total_line DECIMAL(15,2) NOT NULL
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    method VARCHAR(50) NOT NULL, -- CASH, CARD, VOUCHER
    amount DECIMAL(15,2) NOT NULL,
    details_json JSONB,
    created_at_pos TIMESTAMPTZ NOT NULL
);

-- ==================================================================================
-- 7. VERIFACTU CENTRAL LEDGER (Backup & Audit)
-- ==================================================================================

CREATE TABLE fiscal_audit_log (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE, -- Matches POS record UUID
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    event_type VARCHAR(20) NOT NULL, -- ALTA, ANULACION
    
    -- Chain Security
    previous_record_hash VARCHAR(255) NOT NULL,
    chain_sequence_id INT NOT NULL,
    fiscal_timestamp TIMESTAMPTZ NOT NULL,
    
    -- Software Audit
    software_name VARCHAR(100) NOT NULL,
    software_version VARCHAR(50) NOT NULL,
    installation_id VARCHAR(100) NOT NULL,
    device_serial VARCHAR(100) NOT NULL,
    
    -- Snapshot Data
    issuer_nif VARCHAR(50) NOT NULL,
    invoice_series VARCHAR(50) NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    invoice_date VARCHAR(50) NOT NULL,
    invoice_amount DECIMAL(15,2) NOT NULL,
    
    -- Cryptography
    record_hash VARCHAR(255) NOT NULL,
    signature TEXT,
    system_fingerprint TEXT,
    
    -- AEAT Sync Status (Backend forwards to AEAT)
    aeat_status VARCHAR(20) DEFAULT 'PENDING',
    aeat_csv VARCHAR(255),
    aeat_response_json JSONB,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Integrity Constraint: No duplicate sequence per device
    UNIQUE(device_id, chain_sequence_id)
);

-- Indexes
CREATE INDEX idx_products_search ON products(name);
CREATE INDEX idx_products_ref ON products(reference);
CREATE INDEX idx_sales_date ON sales(issued_at_pos);
CREATE INDEX idx_sales_store ON sales(store_id);
CREATE INDEX idx_fiscal_hash ON fiscal_audit_log(record_hash);
CREATE INDEX idx_inventory_product_store ON inventory(product_id, store_id);