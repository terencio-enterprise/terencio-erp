-- ==================================================================================
-- PROJECT: TERENCIO POS - ENTERPRISE CORE (Unified V1)
-- SCOPE: Multi-Company, Multi-Store, VeriFactu, CRM, Inventory, Purchasing, Accounting
-- ENGINE: PostgreSQL 16+
-- STATUS: ENTERPRISE GRADE (Hardened Fiscal Audit, Rectification Snapshots, Strict Sequence)
-- VERSION: 1.5 (Accounting Integrity, Price Overlap Protection, Multi-Tenant Isolation)
-- ==================================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Enable PGCrypto for internal hashing if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================================================================================
-- 1. ORGANIZATION LAYER (Multi-Tenant / Multi-Company)
-- ==================================================================================

-- Companies (Legal Entities)
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL, -- CIF/NIF
    currency_code VARCHAR(3) DEFAULT 'EUR',
    
    -- Fiscal Configuration (Point 4)
    fiscal_regime VARCHAR(50) DEFAULT 'COMMON', -- COMMON, SII, CANARY_IGIC
    price_includes_tax BOOLEAN DEFAULT TRUE,
    rounding_mode VARCHAR(20) DEFAULT 'LINE', -- LINE, TOTAL
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1
);

-- Stores (Tiendas/Branches)
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    code VARCHAR(50) NOT NULL, -- Internal Code 'MAD-001'
    name VARCHAR(255) NOT NULL,
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    
    -- Store Specifics
    is_active BOOLEAN DEFAULT TRUE,
    timezone VARCHAR(50) DEFAULT 'Europe/Madrid',
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1,
    
    UNIQUE(company_id, code)
);

-- Store Settings (Point 6 - Configuration per Store)
CREATE TABLE store_settings (
    store_id UUID PRIMARY KEY REFERENCES stores(id),
    allow_negative_stock BOOLEAN DEFAULT FALSE,
    default_tariff_id BIGINT, -- FK added later or handled logically to avoid circular dep
    print_ticket_automatically BOOLEAN DEFAULT TRUE,
    require_customer_for_large_amount DECIMAL(15,2),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Warehouses (Almacenes) - Can be physical store or logic warehouse
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID REFERENCES stores(id), -- Optional: Link to a specific store
    
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    address TEXT,
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- ==================================================================================
-- 2. SECURITY & ACCESS (RBAC)
-- ==================================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID REFERENCES stores(id), -- If NULL, user has access to company (depending on role)
    
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    
    -- Authentication
    password_hash VARCHAR(255), -- Web/Admin access
    pin_hash VARCHAR(255),      -- Quick POS access
    
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'CASHIER', -- CASHIER, MANAGER, ADMIN, SUPER_ADMIN
    
    -- Fine-grained permissions
    permissions_json JSONB DEFAULT '[]',
    
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    UNIQUE(company_id, username)
);

-- Devices (POS Terminals) - with Auth (V4 merged)
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    
    name VARCHAR(100),
    serial_code VARCHAR(100) NOT NULL, -- Logical ID 'POS-01'
    hardware_id VARCHAR(255) NOT NULL, -- Physical fingerprint
    
    -- Status
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACTIVE, BLOCKED, REVOKED
    version_app VARCHAR(50),
    
    -- Security / Auth (V4)
    device_secret VARCHAR(255), -- HMAC secret
    api_key_version INTEGER DEFAULT 1,
    last_authenticated_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE(store_id, serial_code)
);

-- Device Registration Codes (V2 merged)
CREATE TABLE registration_codes (
    code VARCHAR(10) PRIMARY KEY, -- 'A1B2C3'
    store_id UUID NOT NULL REFERENCES stores(id),
    
    preassigned_name VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    used_by_device_id UUID REFERENCES devices(id),
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 3. MASTER DATA (Catalog, Taxes, Pricing)
-- ==================================================================================

-- Taxes
CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    name VARCHAR(100) NOT NULL,
    rate DECIMAL(10,4) NOT NULL, -- 21.0000
    surcharge DECIMAL(10,4) DEFAULT 0, -- Recargo equivalencia
    
    code_aeat VARCHAR(50), -- Tax code for reporting
    
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Categories (Tree Structure)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    parent_id BIGINT REFERENCES categories(id),
    
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    image_url TEXT,
    
    active BOOLEAN DEFAULT TRUE
);

-- Products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    -- Identification
    reference VARCHAR(100) NOT NULL, -- SKU
    name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100),
    description TEXT,
    
    -- Classification
    category_id BIGINT REFERENCES categories(id),
    tax_id BIGINT NOT NULL REFERENCES taxes(id),
    brand VARCHAR(100),
    
    -- Flags
    type VARCHAR(50) DEFAULT 'PRODUCT', -- PRODUCT, SERVICE, KITS
    is_weighted BOOLEAN DEFAULT FALSE,
    is_inventoriable BOOLEAN DEFAULT TRUE,
    
    -- Inventory Settings
    min_stock_alert DECIMAL(15,3) DEFAULT 0,
    
    -- Cost Control (Point 9)
    average_cost DECIMAL(15,4) DEFAULT 0,
    last_purchase_cost DECIMAL(15,4) DEFAULT 0,
    
    -- Media
    image_url TEXT,
    
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1, -- Optimistic Locking
    
    UNIQUE(company_id, reference)
);

-- Barcodes (Multi-barcode support)
CREATE TABLE product_barcodes (
    barcode VARCHAR(100) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'EAN13',
    quantity_factor DECIMAL(10,3) DEFAULT 1, -- For packs (e.g., barcode for a box of 12)
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tariffs (Price Lists)
CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    priority INT DEFAULT 0,
    
    -- Price Classification (Point 5)
    price_type VARCHAR(20) DEFAULT 'RETAIL', -- RETAIL, WHOLESALE, PREMIUM
    
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1 -- Optimistic Locking (Point 8)
);

-- Prices (Standard List Prices)
CREATE TABLE product_prices (
    product_id BIGINT NOT NULL REFERENCES products(id),
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id),
    
    price DECIMAL(15,4) NOT NULL, -- Base price (usually pre-tax, depends on config)
    cost_price DECIMAL(15,4), -- Snapshot of cost at setting time
    
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, tariff_id)
);

-- Product Price History (Point 1 - Critical Audit)
CREATE TABLE product_price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id),
    
    price DECIMAL(15,4) NOT NULL,
    
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    changed_by BIGINT REFERENCES users(id),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Product Cost History (Point 1 - Critical Audit)
CREATE TABLE product_cost_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    
    cost DECIMAL(15,4) NOT NULL,
    source VARCHAR(50), -- PURCHASE, ADJUSTMENT, MANUAL
    reference_uuid UUID,
    
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Payment Methods (Configuration)
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID REFERENCES companies(id), -- Null for system defaults
    code VARCHAR(50) NOT NULL, -- CASH, CARD, BIZUM
    name VARCHAR(100) NOT NULL,
    is_cash BOOLEAN DEFAULT FALSE,
    requires_reference BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    
    UNIQUE(company_id, code)
);

-- Document Types (Configuration)
CREATE TABLE document_types (
    code VARCHAR(50) PRIMARY KEY, -- SIMPLIFIED, FULL, CREDIT_NOTE
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- ==================================================================================
-- 4. BUSINESS PARTNERS & PRICING ENGINE (CRM & Suppliers)
-- ==================================================================================

-- Customers
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    tax_id VARCHAR(50), -- NIF/CIF
    legal_name VARCHAR(255),
    commercial_name VARCHAR(255),
    
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    country VARCHAR(10) DEFAULT 'ES',
    
    -- Commercial Conditions
    tariff_id BIGINT REFERENCES tariffs(id),
    allow_credit BOOLEAN DEFAULT FALSE,
    credit_limit DECIMAL(15,2) DEFAULT 0,
    surcharge_apply BOOLEAN DEFAULT FALSE, -- Recargo Equivalencia flag
    
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Customer Specific Pricing (Overrides - Point 2.A)
CREATE TABLE customer_product_prices (
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT, -- Point 7
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT, -- Point 7
    
    custom_price DECIMAL(15,4) NOT NULL,
    valid_from TIMESTAMPTZ DEFAULT NOW(),
    valid_until TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (customer_id, product_id)
);

-- Dynamic Pricing Rules (Volume/Promos - Point 2.B)
CREATE TABLE pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- VOLUME_DISCOUNT, BOGO, CATEGORY_DISCOUNT
    
    condition_json JSONB NOT NULL, -- e.g. {"min_qty": 10, "product_id": 5}
    effect_json JSONB NOT NULL, -- e.g. {"discount_percent": 10}
    
    priority INT DEFAULT 10,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1, -- Optimistic Locking (Point 8)
    
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Customer Account Ledger (Point 9 - B2B Professional)
CREATE TABLE customer_account_movements (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    reference_uuid UUID, -- Sale UUID or Payment UUID
    type VARCHAR(20) NOT NULL, -- INVOICE, PAYMENT, REFUND, ADJUSTMENT
    amount DECIMAL(15,2) NOT NULL, -- Positive (Debit/Owe), Negative (Credit/Pay)
    balance_after DECIMAL(15,2),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Suppliers
CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    tax_id VARCHAR(50),
    legal_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- ==================================================================================
-- 5. INVENTORY & PURCHASING
-- ==================================================================================

-- Inventory Lots (Trazabilidad)
CREATE TABLE inventory_lots (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT, -- Point 7
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    
    lot_code VARCHAR(100) NOT NULL,
    expiration_date DATE,
    quantity DECIMAL(15,3) DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(product_id, warehouse_id, lot_code)
);

-- Current Stock Levels (Snapshot)
CREATE TABLE inventory_stock (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT, -- Point 7
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    
    quantity_on_hand DECIMAL(15,3) DEFAULT 0,
    quantity_reserved DECIMAL(15,3) DEFAULT 0,
    quantity_incoming DECIMAL(15,3) DEFAULT 0, -- Ordered from suppliers
    
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1, -- Optimistic Locking
    
    PRIMARY KEY (product_id, warehouse_id)
    
    -- Removed rigid constraint chk_positive_stock as per user request (allow negative stock logic)
);

-- Stock Movements (The Truth Ledger)
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    type VARCHAR(50) NOT NULL, -- SALE, RETURN, PURCHASE, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT
    
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    lot_id BIGINT REFERENCES inventory_lots(id), -- Optional Lot tracking
    
    quantity DECIMAL(15,3) NOT NULL, -- Positive (add) or Negative (remove)
    previous_balance DECIMAL(15,3) NOT NULL,
    new_balance DECIMAL(15,3) NOT NULL,
    
    cost_unit DECIMAL(15,4), -- Snapshot of cost at movement time
    
    -- Context
    reason VARCHAR(255),
    reference_doc_type VARCHAR(50), -- SALE_UUID, PO_UUID, TRANSFER_UUID
    reference_doc_uuid UUID,
    
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Stock Transfers (Point 2 - Warehouse Management)
CREATE TABLE stock_transfers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    from_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    to_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, IN_TRANSIT, COMPLETED, CANCELLED
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    
    -- Self-transfer prevention (Point 5)
    CONSTRAINT chk_transfer_diff CHECK (from_warehouse_id <> to_warehouse_id)
);

CREATE TABLE stock_transfer_lines (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT REFERENCES stock_transfers(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity DECIMAL(15,3) NOT NULL
);

-- Stock Counts / Inventarios Físicos (Point 2)
CREATE TABLE stock_counts (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id), -- Point 10: Multi-tenant isolation
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, COMPLETED
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    applied_at TIMESTAMPTZ
);

CREATE TABLE stock_count_lines (
    id BIGSERIAL PRIMARY KEY,
    stock_count_id BIGINT REFERENCES stock_counts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    expected_qty DECIMAL(15,3),
    counted_qty DECIMAL(15,3)
);

-- Purchase Orders (To Suppliers)
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT, -- Point 7
    
    reference VARCHAR(50), -- Supplier invoice number etc
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, SENT, PARTIAL, RECEIVED, CANCELLED
    
    expected_date DATE,
    notes TEXT,
    
    total_amount DECIMAL(15,2),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Purchase Order Lines (Point 1 - Critical Missing Piece)
CREATE TABLE purchase_order_lines (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    
    quantity DECIMAL(15,3) NOT NULL,
    unit_cost DECIMAL(15,4) NOT NULL,
    
    tax_rate DECIMAL(10,4) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    
    total_line DECIMAL(15,2) NOT NULL
);

-- ==================================================================================
-- 6. SALES & POS OPERATIONS
-- ==================================================================================

-- Global Sequences (Backup / Online Issues)
CREATE TABLE doc_sequences (
    store_id UUID NOT NULL REFERENCES stores(id),
    series VARCHAR(50) NOT NULL, 
    year INT NOT NULL,
    current_value INT DEFAULT 0,
    version BIGINT DEFAULT 1, 
    PRIMARY KEY (store_id, series, year)
);

-- DEVICE SEQUENCES (OFFLINE AUTONOMY - Point 3.C)
-- Each POS manages its own numbering range to avoid server roundtrips
CREATE TABLE device_sequences (
    device_id UUID NOT NULL REFERENCES devices(id),
    series VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    current_value INT DEFAULT 0,
    version BIGINT DEFAULT 1, -- Optimistic Lock requested
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (device_id, series, year)
);

-- Shifts (Cajas / Turnos)
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    user_id BIGINT REFERENCES users(id), -- Who opened it
    
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    
    amount_initial DECIMAL(15,2) DEFAULT 0,
    amount_system DECIMAL(15,2) DEFAULT 0, -- Expected cash
    amount_counted DECIMAL(15,2), -- Actual count
    amount_diff DECIMAL(15,2),
    
    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, CLOSED
    
    -- Z Report Data
    z_count INT, 
    z_report_signature TEXT,
    
    synced_at TIMESTAMPTZ DEFAULT NOW()
);

-- Cash Movements (Entries/Exits separate from Sales)
CREATE TABLE cash_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    type VARCHAR(20) NOT NULL, -- DROP (retirada), FLOAT (entrada), EXPENSE (pago proveedor)
    amount DECIMAL(15,2) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Sales Headers (Tickets/Invoices)
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE, -- Generated at POS, checked for idempotency
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    -- Document ID
    series VARCHAR(50) NOT NULL,
    number INT NOT NULL,
    full_reference VARCHAR(100) NOT NULL, -- 'T-POS1-2024-1005'
    
    -- Type
    type VARCHAR(50) NOT NULL DEFAULT 'SIMPLIFIED' REFERENCES document_types(code),
    
    -- Context
    shift_id UUID REFERENCES shifts(id),
    user_id BIGINT REFERENCES users(id),
    customer_id BIGINT REFERENCES customers(id) ON DELETE RESTRICT, -- Point 7 - Protect History
    
    -- FISCAL SNAPSHOT
    customer_tax_id VARCHAR(50),
    customer_legal_name VARCHAR(255),
    customer_address TEXT,
    customer_zip VARCHAR(20),
    customer_city VARCHAR(100),
    
    store_tax_id VARCHAR(50),
    store_legal_name VARCHAR(255),
    store_address TEXT,
    
    -- Dates
    created_at_pos TIMESTAMPTZ NOT NULL,
    issued_at_pos TIMESTAMPTZ, -- Fiscal Date
    received_at_server TIMESTAMPTZ DEFAULT NOW(),
    
    -- Status
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, ISSUED, FISCALIZED, SENT_AEAT, AEAT_ACCEPTED, AEAT_REJECTED, CANCELLED
    sync_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SYNCED, ERROR (Point 3)
    sync_batch_id UUID, -- Point 3 - Audit Sync Batches
    is_offline BOOLEAN DEFAULT FALSE,
    
    -- Totals
    total_net DECIMAL(15,2) DEFAULT 0,
    total_tax DECIMAL(15,2) DEFAULT 0,
    total_surcharge DECIMAL(15,2) DEFAULT 0, -- Added for Integrity Check
    total_amount DECIMAL(15,2) DEFAULT 0,
    
    -- Refund / Rectification Links (Enhanced Step 1)
    original_sale_uuid UUID REFERENCES sales(uuid),
    refund_reason TEXT,
    
    -- Rectification Snapshot
    original_series VARCHAR(50),
    original_number INT,
    original_issue_date TIMESTAMPTZ,
    rectification_type VARCHAR(50), -- TOTAL, PARTIAL, DATA_CORRECTION
    rectification_legal_reason VARCHAR(255), -- Added Point A (Legal requirement)
    rectified_by_uuid UUID REFERENCES sales(uuid), -- Added Point B (Bidirectional Link)
    
    UNIQUE(store_id, full_reference),
    
    -- STRICT UNIQUENESS PER DEVICE (Point 1.B)
    UNIQUE(device_id, series, number),
    
    -- DATA INTEGRITY CHECK (Point 6.A)
    -- Ensures mathematical consistency of the document
    CONSTRAINT chk_sales_totals CHECK (
        ABS(total_amount - (total_net + total_tax + total_surcharge)) < 0.05
    ),
    
    -- RECTIFICATION LOGIC CHECK (Point 6)
    CONSTRAINT chk_credit_note_logic CHECK (
        (type != 'CREDIT_NOTE') OR (original_sale_uuid IS NOT NULL)
    )
);

-- Sync Logs (Point 3 - Professional Monitoring)
CREATE TABLE sync_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID REFERENCES devices(id),
    batch_id UUID,
    status VARCHAR(20), -- START, SUCCESS, ERROR
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Sale Lines
CREATE TABLE sale_lines (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT, -- Prevent accidental cascade
    
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT, -- Point 7
    description VARCHAR(255) NOT NULL, -- Snapshot of name
    
    quantity DECIMAL(15,3) NOT NULL,
    unit_price DECIMAL(15,4) NOT NULL, -- Includes pricing rules applied
    
    -- Discounts
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    
    -- Tax Snapshot
    tax_id BIGINT REFERENCES taxes(id),
    tax_rate DECIMAL(10,4) NOT NULL,
    tax_amount DECIMAL(15,4) NOT NULL,
    
    total_line DECIMAL(15,2) NOT NULL,
    
    -- Pricing Traceability (Point 2)
    pricing_context JSONB, -- Stores info about applied tariffs, rules, overrides
    
    -- MATH CONSISTENCY CHECK (Point 5)
    CONSTRAINT chk_line_totals CHECK (
        ABS(total_line - ((quantity * unit_price) - discount_amount + tax_amount)) < 0.05
    )
);

-- Sale Taxes Aggregates
CREATE TABLE sale_taxes (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    tax_id BIGINT REFERENCES taxes(id), 
    
    tax_name VARCHAR(100) NOT NULL,
    tax_rate DECIMAL(10,4) NOT NULL,
    taxable_base DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    surcharge_rate DECIMAL(10,4) DEFAULT 0,
    surcharge_amount DECIMAL(15,2) DEFAULT 0,
    
    -- Consistency Check (Enhanced Step 2)
    -- Uniqueness based on rates, not IDs (which might change/null)
    UNIQUE(sale_uuid, tax_rate, surcharge_rate)
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(), -- Added Step 3
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    
    payment_method_id BIGINT REFERENCES payment_methods(id) ON DELETE RESTRICT, -- Point 6.C
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    exchange_rate DECIMAL(10,4) DEFAULT 1,
    
    payment_data JSONB, -- Card auth codes, etc
    created_at_pos TIMESTAMPTZ NOT NULL,
    
    -- Integrity Check (Enhanced Step 3)
    UNIQUE(sale_uuid, payment_method_id, created_at_pos)
);

-- ==================================================================================
-- 7. VERIFACTU & FISCAL COMPLIANCE (The Immutable Chain)
-- ==================================================================================

CREATE TABLE fiscal_audit_log (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE, -- Generated at POS
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    event_type VARCHAR(20) NOT NULL, -- ISSUE, ANNUL
    
    -- THE CHAIN (VeriFactu Requirement)
    -- Chain per Device strategy enabled by device_sequences table
    previous_record_hash VARCHAR(64) NOT NULL, 
    chain_sequence_id INT NOT NULL, 
    
    -- Fingerprinting
    record_hash VARCHAR(64) NOT NULL, 
    signature TEXT, 
    
    -- Software Identification
    software_id VARCHAR(100) NOT NULL,
    software_version VARCHAR(50) NOT NULL,
    developer_id VARCHAR(100) NOT NULL,
    certification_reference VARCHAR(100),
    
    -- Audit Data
    invoice_amount DECIMAL(15,2) NOT NULL,
    invoice_date TIMESTAMPTZ NOT NULL,
    
    -- AEAT Communication
    aeat_status VARCHAR(20) DEFAULT 'PENDING',
    aeat_csv_sent TEXT,
    aeat_response_json JSONB,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Integrity: A device cannot have two records with same sequence
    UNIQUE(device_id, chain_sequence_id),
    
    -- PREVENT DOUBLE FISCALIZATION (Point 1.A)
    UNIQUE(sale_uuid, event_type),
    
    -- STRICT HASH CHECKS (Point 6.D)
    CONSTRAINT chk_hash_length CHECK (LENGTH(record_hash) = 64),
    CONSTRAINT chk_prev_hash_length CHECK (LENGTH(previous_record_hash) = 64)
);

-- ==================================================================================
-- 8. ACCOUNTING & AUDIT (Point 3 & 10)
-- ==================================================================================

-- Accounting Entries (Asientos)
CREATE TABLE accounting_entries (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    reference_type VARCHAR(50), -- SALE, PURCHASE, PAYMENT
    reference_uuid UUID,
    
    entry_date DATE NOT NULL,
    description TEXT,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE accounting_entry_lines (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT REFERENCES accounting_entries(id) ON DELETE CASCADE,
    
    account_code VARCHAR(20) NOT NULL,
    debit DECIMAL(15,2) DEFAULT 0,
    credit DECIMAL(15,2) DEFAULT 0,

    -- Accounting Integrity (Point 1: Debit OR Credit, never both active)
    CONSTRAINT chk_debit_credit_exclusive CHECK (
        (debit = 0 AND credit > 0) OR (credit = 0 AND debit > 0)
    )
);

-- Audit Log (Security)
CREATE TABLE audit_user_actions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(100) NOT NULL, -- LOGIN_FAILED, PRICE_CHANGED, STOCK_ADJUSTED
    entity VARCHAR(50),
    entity_id VARCHAR(100),
    payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- ==================================================================================
-- 9. DOMAIN EVENTS (For BI / Async)
-- ==================================================================================
CREATE TABLE domain_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL, -- SALE, PRODUCT, STOCK
    aggregate_id VARCHAR(100) NOT NULL, -- UUID or ID
    event_type VARCHAR(100) NOT NULL, -- SALE_CREATED, STOCK_ADJUSTED
    payload JSONB NOT NULL,
    
    -- Processing Status (Point 5)
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 10. INDEXES & OPTIMIZATIONS
-- ==================================================================================

-- Performance Indexes
CREATE INDEX idx_products_search ON products(company_id, name);
CREATE INDEX idx_products_ref ON products(company_id, reference);
CREATE INDEX idx_sales_date ON sales(issued_at_pos);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_inventory_lookup ON inventory_stock(product_id, warehouse_id);
CREATE INDEX idx_fiscal_chain ON fiscal_audit_log(device_id, chain_sequence_id);

-- Operational Indexes
CREATE INDEX idx_sales_uuid ON sales(uuid);
CREATE INDEX idx_sales_store_date ON sales(store_id, issued_at_pos);
CREATE INDEX idx_stock_mov_product ON stock_movements(product_id);
CREATE INDEX idx_sale_lines_sale ON sale_lines(sale_uuid);
CREATE INDEX idx_fiscal_log_sale ON fiscal_audit_log(sale_uuid);

-- NEW CRITICAL INDEXES (Points 1.C & 6)
CREATE INDEX idx_fiscal_device_date ON fiscal_audit_log(device_id, invoice_date);
CREATE INDEX idx_events_unprocessed ON domain_events(processed, created_at);

-- Requested Analysis Indexes (Step 7)
CREATE INDEX idx_sales_device_issued ON sales(device_id, issued_at_pos);
CREATE INDEX idx_sale_lines_product ON sale_lines(product_id);

-- Extra Enterprise Hardening Indexes (Point 4 & 5)
CREATE INDEX idx_fiscal_device_created ON fiscal_audit_log(device_id, created_at);
CREATE INDEX idx_customer_product_price_lookup ON customer_product_prices(customer_id, product_id);

-- Multi-Tenancy & Isolation Optimization (Point 5)
CREATE INDEX idx_sales_company_date ON sales(company_id, issued_at_pos);
CREATE INDEX idx_products_company_active ON products(company_id, active);

-- Soft Delete Optimization (Point 7)
CREATE INDEX idx_products_active_only ON products(company_id, reference) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_active_only ON customers(company_id, tax_id) WHERE deleted_at IS NULL;

-- NEW INDEXES (Version 1.5 - Point 4, 8, 9, 11)
CREATE INDEX idx_price_history_lookup ON product_price_history(product_id, tariff_id, valid_from, valid_until);
CREATE INDEX idx_cost_history_lookup ON product_cost_history(product_id, valid_from, valid_until);
CREATE INDEX idx_customer_ledger ON customer_account_movements(customer_id, created_at);
CREATE INDEX idx_accounting_company_date ON accounting_entries(company_id, entry_date);

-- UNIQUE ACTIVE PRICE (Point 11 - Prevent double active price)
CREATE UNIQUE INDEX uniq_price_active ON product_price_history(product_id, tariff_id) WHERE valid_until IS NULL;


-- ==================================================================================
-- 11. TRIGGERS & SECURITY
-- ==================================================================================

-- Function to prevent updates/deletes on fiscal log
CREATE OR REPLACE FUNCTION prevent_change_fiscal_log()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Fiscal Audit Log is Immutable. Operation not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_fiscal_log
BEFORE UPDATE OR DELETE ON fiscal_audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscal_log();

-- ==================================================================================
-- TRIGGER: FISCAL SEQUENCE VALIDATION (Step 4 - DB Level Chain Protection)
-- ==================================================================================
CREATE OR REPLACE FUNCTION validate_fiscal_sequence()
RETURNS TRIGGER AS $$
DECLARE
    last_seq INT;
BEGIN
    -- High Concurrency Protection (Point D)
    -- Select the last record for this device and lock it (FOR UPDATE)
    SELECT chain_sequence_id INTO last_seq
    FROM fiscal_audit_log
    WHERE device_id = NEW.device_id
    ORDER BY chain_sequence_id DESC
    LIMIT 1
    FOR UPDATE;
    
    -- Handle first record case
    IF last_seq IS NULL THEN
        last_seq := 0;
    END IF;

    -- Enforce strict next sequence
    IF NEW.chain_sequence_id != last_seq + 1 THEN
        RAISE EXCEPTION 'Fiscal Chain Breach: Expected sequence % but got % for device %', 
            last_seq + 1, NEW.chain_sequence_id, NEW.device_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_fiscal_sequence
BEFORE INSERT ON fiscal_audit_log
FOR EACH ROW EXECUTE FUNCTION validate_fiscal_sequence();


-- ==================================================================================
-- TRIGGER: PROTECT FISCALIZED SALES (Step 6 - Never Delete)
-- ==================================================================================
CREATE OR REPLACE FUNCTION prevent_change_fiscalized_sales()
RETURNS TRIGGER AS $$
DECLARE
    current_status VARCHAR(20);
BEGIN
    -- Determine status based on table type
    IF TG_TABLE_NAME = 'sales' THEN
        current_status := OLD.status;
    ELSE
        -- For lines, payments, taxes -> fetch parent sales status
        SELECT status INTO current_status FROM sales WHERE uuid = OLD.sale_uuid;
    END IF;

    -- Block modifications if fiscalized
    IF current_status IN ('ISSUED', 'FISCALIZED', 'SENT_AEAT', 'AEAT_ACCEPTED') THEN
        RAISE EXCEPTION 'Operation Denied: Cannot DELETE or UPDATE records of a fiscalized sale (Status: %). Issue a Rectification instead.', current_status;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Apply to all critical sales tables
CREATE TRIGGER trg_protect_sales_del BEFORE DELETE OR UPDATE ON sales
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();

CREATE TRIGGER trg_protect_lines_del BEFORE DELETE OR UPDATE ON sale_lines
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();

CREATE TRIGGER trg_protect_taxes_del BEFORE DELETE OR UPDATE ON sale_taxes
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();

CREATE TRIGGER trg_protect_pay_del BEFORE DELETE OR UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();


-- ==================================================================================
-- TRIGGER: PROTECT COMPLETED STOCK DOCS (Point 7)
-- ==================================================================================
CREATE OR REPLACE FUNCTION prevent_change_completed_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'COMPLETED' THEN
        RAISE EXCEPTION 'Operation Denied: Cannot DELETE or UPDATE a COMPLETED stock document.';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_transfer_del BEFORE DELETE OR UPDATE ON stock_transfers
FOR EACH ROW EXECUTE FUNCTION prevent_change_completed_stock();

CREATE TRIGGER trg_protect_count_del BEFORE DELETE OR UPDATE ON stock_counts
FOR EACH ROW EXECUTE FUNCTION prevent_change_completed_stock();


-- ==================================================================================
-- 12. SEED DATA (Bootstrap)
-- ==================================================================================

-- 1. Create Default Company
INSERT INTO companies (id, name, tax_id) 
VALUES ('11111111-1111-1111-1111-111111111111', 'DEMO COMPANY S.L.', 'B12345678');

-- 2. Create Default Store
INSERT INTO stores (id, company_id, code, name)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'MAIN-01', 'Main Store');

-- 3. Create Default Warehouse
INSERT INTO warehouses (company_id, store_id, name)
VALUES ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Main Warehouse');

-- 4. Create Super Admin
INSERT INTO users (company_id, store_id, username, password_hash, full_name, role)
VALUES (
    '11111111-1111-1111-1111-111111111111', 
    '22222222-2222-2222-2222-222222222222', 
    'admin', 
    '$2a$10$X7V.jO.vj.j.j.j.j.j.j.j.j.j.j', -- Dummy BCrypt
    'System Administrator', 
    'SUPER_ADMIN'
);

-- 5. Seed Document Types
INSERT INTO document_types (code, name, description) VALUES
('SIMPLIFIED', 'Factura Simplificada', 'Ticket de venta estándar (antiguo ticket)'),
('FULL', 'Factura Completa', 'Factura con datos fiscales completos del cliente'),
('CREDIT_NOTE', 'Factura Rectificativa', 'Devolución o corrección de factura anterior');

-- 6. Seed Payment Methods
INSERT INTO payment_methods (company_id, code, name, is_cash, requires_reference) VALUES
(NULL, 'CASH', 'Efectivo', TRUE, FALSE),
(NULL, 'CARD', 'Tarjeta Crédito/Débito', FALSE, TRUE);

-- 7. Create a Registration Code for immediate POS onboarding
INSERT INTO registration_codes (code, store_id, preassigned_name, expires_at)
VALUES ('123456', '22222222-2222-2222-2222-222222222222', 'POS 1', NOW() + INTERVAL '1 year');