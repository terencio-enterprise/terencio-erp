-- ==================================================================================
-- PARTE 3: TERENCIO ERP - MISCELÁNEO (CATÁLOGO, INVENTARIO, POS, CONTABILIDAD)
-- Requiere: 01_core_schema.sql y 02_crm_marketing_schema.sql
-- ==================================================================================

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

-- Constraint aplazado desde la Parte 2
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