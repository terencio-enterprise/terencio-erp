-- ==================================================================================
-- PROYECTO: TERENCIO POS - NÚCLEO EMPRESARIAL
-- ALCANCE: Multi-Empresa, Multi-Tienda, VeriFactu, Inventario (Simplificado), Contabilidad
-- MOTOR: PostgreSQL 16+
-- ==================================================================================

-- Habilitar extensión para generación de UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Habilitar PGCrypto para hashing interno (necesario para firmas VeriFactu si se hace en DB)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==================================================================================
-- 1. CAPA DE ORGANIZACIÓN (Multi-Inquilino / Multi-Empresa)
-- ==================================================================================

-- Empresas (Entidades Legales)
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL, -- CIF/NIF
    currency_code VARCHAR(3) DEFAULT 'EUR',
    
    -- Configuración Fiscal
    fiscal_regime VARCHAR(50) DEFAULT 'COMMON', -- COMUN, SII, CANARIAS_IGIC, RECARGO
    price_includes_tax BOOLEAN DEFAULT TRUE, -- ¿Precios con IVA incluido?
    rounding_mode VARCHAR(20) DEFAULT 'LINE', -- Redondeo por LÍNEA o por TOTAL
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1
);

-- Tiendas (Sucursales físicas o lógicas)
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    code VARCHAR(50) NOT NULL, -- Código interno ej: 'MAD-001'
    name VARCHAR(255) NOT NULL,
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    
    is_active BOOLEAN DEFAULT TRUE,
    timezone VARCHAR(50) DEFAULT 'Europe/Madrid',
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1,
    
    UNIQUE(company_id, code) -- El código de tienda debe ser único dentro de la empresa
);

-- Configuración específica por Tienda
CREATE TABLE store_settings (
    store_id UUID PRIMARY KEY REFERENCES stores(id),
    allow_negative_stock BOOLEAN DEFAULT FALSE, -- ¿Permitir vender sin stock?
    default_tariff_id BIGINT, 
    print_ticket_automatically BOOLEAN DEFAULT TRUE,
    require_customer_for_large_amount BIGINT, -- Ley antifraude (ej: > 1000€ requiere NIF) - stored in cents
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Almacenes (Logística)
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id), -- Único padre directo
    
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50), -- Código interno opcional
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    UNIQUE(store_id) -- Regla de Negocio: Un almacén por tienda en esta versión
);

-- ==================================================================================
-- 2. SEGURIDAD Y ACCESO (RBAC - Control de Acceso Basado en Roles)
-- ==================================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID REFERENCES stores(id), -- Si es NULL, tiene acceso a toda la empresa (según rol)
    
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255), -- Acceso Web
    pin_hash VARCHAR(255),      -- Acceso rápido TPV
    
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'CASHIER', -- CAJERO, ENCARGADO, ADMIN, SUPER_ADMIN
    permissions_json JSONB DEFAULT '[]', -- Permisos granulares extra
    
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    UNIQUE(company_id, username)
);

-- Dispositivos (Terminales TPV)
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    
    name VARCHAR(100),
    serial_code VARCHAR(100) NOT NULL, -- ID Lógico 'CAJA-01'
    hardware_id VARCHAR(255) NOT NULL, -- Huella digital física (Fingerprint)
    
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACTIVE, BLOCKED
    version_app VARCHAR(50),
    
    -- Seguridad
    device_secret VARCHAR(255), -- Secreto para firma HMAC
    api_key_version INTEGER DEFAULT 1,
    last_authenticated_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE(store_id, serial_code)
);

-- Códigos de Vinculación de Dispositivos (Onboarding)
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
-- 3. DATOS MAESTROS (Catálogo, Impuestos, Tarifas)
-- ==================================================================================

-- Impuestos (IVA, IGIC)
CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL, -- ej: "IVA General 21%"
    rate DECIMAL(10,4) NOT NULL, -- 21.0000
    surcharge DECIMAL(10,4) DEFAULT 0, -- Recargo de Equivalencia (R.E.)
    code_aeat VARCHAR(50), -- Código para modelo 303/347
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Categorías (Árbol de productos)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    parent_id BIGINT REFERENCES categories(id), -- Jerarquía
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    image_url TEXT,
    active BOOLEAN DEFAULT TRUE
);

-- Productos
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    reference VARCHAR(100) NOT NULL, -- SKU / Referencia interna
    name VARCHAR(255) NOT NULL,
    short_name VARCHAR(100), -- Para ticket (limitado caracteres)
    description TEXT,
    
    category_id BIGINT REFERENCES categories(id),
    tax_id BIGINT NOT NULL REFERENCES taxes(id),
    brand VARCHAR(100),
    
    type VARCHAR(50) DEFAULT 'PRODUCT', -- PRODUCTO, SERVICIO, KIT
    is_weighted BOOLEAN DEFAULT FALSE,  -- ¿Se vende por peso (balanza)?
    is_inventoriable BOOLEAN DEFAULT TRUE, -- ¿Controla stock?
    
    min_stock_alert DECIMAL(15,3) DEFAULT 0,
    average_cost BIGINT DEFAULT 0, -- PMP (Precio Medio Ponderado) - stored in cents
    last_purchase_cost BIGINT DEFAULT 0, -- stored in cents
    
    image_url TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 1, -- Bloqueo optimista
    
    UNIQUE(company_id, reference)
);

-- Códigos de Barras (Multi-código)
CREATE TABLE product_barcodes (
    barcode VARCHAR(100) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'EAN13',
    quantity_factor DECIMAL(10,3) DEFAULT 1, -- Para packs (ej: código de caja de 6 uds)
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tarifas de Precios
CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL, -- ej: "PVP General", "Tarifa Mayorista"
    priority INT DEFAULT 0,
    price_type VARCHAR(20) DEFAULT 'RETAIL', -- RETAIL (IVA inc), WHOLESALE (Base + IVA)
    is_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1
);

-- Precios por Producto y Tarifa
CREATE TABLE product_prices (
    product_id BIGINT NOT NULL REFERENCES products(id),
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id),
    price BIGINT NOT NULL, -- Precio base - stored in cents
    cost_price BIGINT, -- Coste de referencia al fijar precio - stored in cents
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, tariff_id)
);

-- Métodos de Pago
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID REFERENCES companies(id), -- NULL = Sistema por defecto
    code VARCHAR(50) NOT NULL, -- CASH, CARD, BIZUM
    name VARCHAR(100) NOT NULL,
    is_cash BOOLEAN DEFAULT FALSE, -- Controla apertura de cajón
    requires_reference BOOLEAN DEFAULT FALSE, -- Requiere nº operación (tarjeta)
    active BOOLEAN DEFAULT TRUE,
    UNIQUE(company_id, code)
);

-- Tipos de Documento
CREATE TABLE document_types (
    code VARCHAR(50) PRIMARY KEY, -- SIMPLIFIED (Ticket), FULL (Factura), CREDIT_NOTE (Abono)
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- ==================================================================================
-- 4. SOCIOS DE NEGOCIO (CRM & Clientes)
-- ==================================================================================

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    tax_id VARCHAR(50), -- NIF / CIF
    legal_name VARCHAR(255), -- Razón Social
    commercial_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    country VARCHAR(10) DEFAULT 'ES',
    
    tariff_id BIGINT REFERENCES tariffs(id), -- Tarifa especial asignada
    allow_credit BOOLEAN DEFAULT FALSE, -- ¿Permite pago diferido?
    credit_limit BIGINT DEFAULT 0, -- stored in cents
    surcharge_apply BOOLEAN DEFAULT FALSE, -- ¿Aplica Recargo de Equivalencia?
    
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Precios Especiales pactados con Cliente
CREATE TABLE customer_product_prices (
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    custom_price BIGINT NOT NULL, -- stored in cents
    valid_from TIMESTAMPTZ DEFAULT NOW(),
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (customer_id, product_id)
);

-- Reglas de Precios Dinámicas (Ofertas/Promociones)
CREATE TABLE pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- VOLUME_DISCOUNT, BOGO (2x1)
    condition_json JSONB NOT NULL, -- ej: {"min_qty": 10}
    effect_json JSONB NOT NULL, -- ej: {"discount_percent": 10}
    priority INT DEFAULT 10,
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Libro Mayor de Cuenta de Cliente (Deudas/Saldos)
CREATE TABLE customer_account_movements (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    reference_uuid UUID, -- ID de Venta o Pago
    type VARCHAR(20) NOT NULL, -- INVOICE (Debe), PAYMENT (Haber)
    amount BIGINT NOT NULL, -- stored in cents
    balance_after BIGINT, -- stored in cents
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 5. INVENTARIO
-- ==================================================================================

-- Stock Actual (Foto instantánea)
CREATE TABLE inventory_stock (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    quantity_on_hand DECIMAL(15,3) DEFAULT 0, -- Cantidad física real
    
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1, -- Bloqueo optimista para concurrencia
    
    PRIMARY KEY (product_id, warehouse_id)
);

-- Movimientos de Stock (La Fuente de la Verdad)
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    
    type VARCHAR(20) NOT NULL CHECK (type IN ('SALE', 'RETURN', 'ADJUSTMENT')),
    
    quantity DECIMAL(15,3) NOT NULL, -- Positivo (Entrada) o Negativo (Salida)
    previous_balance DECIMAL(15,3) NOT NULL,
    new_balance DECIMAL(15,3) NOT NULL,
    
    cost_unit BIGINT, -- Coste unitario en el momento del movimiento - stored in cents
    
    reason VARCHAR(255),
    
    reference_doc_type VARCHAR(50) CHECK (reference_doc_type IN ('SALE', 'RECTIFICATION', 'ADJUSTMENT')),
    reference_doc_uuid UUID,
    
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 6. VENTAS Y OPERATIVA TPV
-- ==================================================================================

-- Secuenciadores por Dispositivo (Autonomía Offline)
-- Cada TPV gestiona su propia serie para no depender del servidor al facturar
CREATE TABLE device_sequences (
    device_id UUID NOT NULL REFERENCES devices(id),
    series VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    current_value INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    last_updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (device_id, series, year)
);

-- Turnos de Caja (Aperturas y Cierres)
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    user_id BIGINT REFERENCES users(id),
    
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    
    amount_initial BIGINT DEFAULT 0, -- Fondo de caja - stored in cents
    amount_system BIGINT DEFAULT 0, -- Teórico calculado por sistema - stored in cents
    amount_counted BIGINT, -- Real contado por usuario - stored in cents
    amount_diff BIGINT, -- Descuadre - stored in cents
    
    status VARCHAR(20) DEFAULT 'OPEN',
    z_count INT, -- Número de informe Z
    z_report_signature TEXT,
    synced_at TIMESTAMPTZ DEFAULT NOW()
);

-- Movimientos de Caja (Entradas/Salidas manuales)
CREATE TABLE cash_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    type VARCHAR(20) NOT NULL, -- DROP (Retirada), FLOAT (Ingreso)
    amount BIGINT NOT NULL, -- stored in cents
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Cabeceras de Venta (Tickets/Facturas)
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE, -- Generado en TPV (Offline First)
    company_id UUID NOT NULL REFERENCES companies(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    -- Identificación Fiscal del Documento
    series VARCHAR(50) NOT NULL,
    number INT NOT NULL,
    full_reference VARCHAR(100) NOT NULL, -- Ej: 'CAJA1-2024-1005'
    
    type VARCHAR(50) NOT NULL DEFAULT 'SIMPLIFIED' REFERENCES document_types(code),
    
    shift_id UUID REFERENCES shifts(id),
    user_id BIGINT REFERENCES users(id),
    customer_id BIGINT REFERENCES customers(id) ON DELETE RESTRICT,
    
    -- FOTO FISCAL (Snapshot) - Datos inmutables del momento de la venta
    customer_tax_id VARCHAR(50),
    customer_legal_name VARCHAR(255),
    customer_address TEXT,
    customer_zip VARCHAR(20),
    customer_city VARCHAR(100),
    
    store_tax_id VARCHAR(50),
    store_legal_name VARCHAR(255),
    store_address TEXT,
    
    created_at_pos TIMESTAMPTZ NOT NULL,
    issued_at_pos TIMESTAMPTZ, -- Fecha devengo impuestos
    received_at_server TIMESTAMPTZ DEFAULT NOW(),
    
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, ISSUED, FISCALIZED (VeriFactu)
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    sync_batch_id UUID,
    is_offline BOOLEAN DEFAULT FALSE,
    
    -- Totales (stored in cents)
    total_net BIGINT DEFAULT 0, -- Base Imponible - stored in cents
    total_tax BIGINT DEFAULT 0, -- Cuota IVA - stored in cents
    total_surcharge BIGINT DEFAULT 0, -- Cuota Recargo - stored in cents
    total_amount BIGINT DEFAULT 0, -- Total a Pagar - stored in cents
    
    -- Enlaces Rectificativas (Factura de Abono)
    original_sale_uuid UUID REFERENCES sales(uuid),
    refund_reason TEXT,
    
    -- Datos Específicos Rectificación
    original_series VARCHAR(50),
    original_number INT,
    original_issue_date TIMESTAMPTZ,
    rectification_type VARCHAR(50), -- TOTAL, PARCIAL
    rectification_legal_reason VARCHAR(255),
    rectified_by_uuid UUID REFERENCES sales(uuid), -- Enlace bidireccional
    
    UNIQUE(store_id, full_reference),
    UNIQUE(device_id, series, number), -- Unicidad estricta por serie/número
    
    -- VALIDACIÓN MATEMÁTICA (Integridad de Datos) - using cents (tolerance of 1 cent)
    CONSTRAINT chk_sales_totals CHECK (
        ABS(total_amount - (total_net + total_tax + total_surcharge)) <= 1
    ),
    -- LÓGICA DE ABONO
    CONSTRAINT chk_credit_note_logic CHECK (
        (type != 'CREDIT_NOTE') OR (original_sale_uuid IS NOT NULL)
    )
);

-- Líneas de Venta (Detalle)
CREATE TABLE sale_lines (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    
    product_id BIGINT REFERENCES products(id) ON DELETE RESTRICT,
    description VARCHAR(255) NOT NULL, -- Nombre del producto en el momento de venta
    
    quantity DECIMAL(15,3) NOT NULL,
    unit_price BIGINT NOT NULL, -- Precio unitario - stored in cents
    
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount BIGINT DEFAULT 0, -- stored in cents
    
    -- Desglose Impuestos Línea
    tax_id BIGINT REFERENCES taxes(id),
    tax_rate DECIMAL(10,4) NOT NULL,
    tax_amount BIGINT NOT NULL, -- stored in cents
    
    total_line BIGINT NOT NULL, -- stored in cents
    pricing_context JSONB, -- Traza de por qué se aplicó este precio (oferta, tarifa, etc)
    
    CONSTRAINT chk_line_totals CHECK (
        ABS(total_line - ((quantity * unit_price) - discount_amount + tax_amount)) <= quantity::DECIMAL
    )
);

-- Desglose de Impuestos Agregado (Bases por Tipo)
CREATE TABLE sale_taxes (
    id BIGSERIAL PRIMARY KEY,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    tax_id BIGINT REFERENCES taxes(id), 
    
    tax_name VARCHAR(100) NOT NULL,
    tax_rate DECIMAL(10,4) NOT NULL,
    taxable_base BIGINT NOT NULL, -- stored in cents
    tax_amount BIGINT NOT NULL, -- stored in cents
    surcharge_rate DECIMAL(10,4) DEFAULT 0,
    surcharge_amount BIGINT DEFAULT 0, -- stored in cents
    
    UNIQUE(sale_uuid, tax_rate, surcharge_rate) -- Evita duplicados por tipo impositivo
);

-- Pagos (Cobros asociados a la venta)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    sale_uuid UUID NOT NULL REFERENCES sales(uuid) ON DELETE RESTRICT,
    
    payment_method_id BIGINT REFERENCES payment_methods(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL, -- stored in cents
    currency VARCHAR(3) DEFAULT 'EUR',
    exchange_rate DECIMAL(10,4) DEFAULT 1,
    
    payment_data JSONB, -- Datos pasarela, auth code, etc.
    created_at_pos TIMESTAMPTZ NOT NULL,
    
    UNIQUE(sale_uuid, payment_method_id, created_at_pos)
);

-- ==================================================================================
-- 7. REGISTRO DE AUDITORÍA FISCAL (VeriFactu / TicketBAI)
-- ==================================================================================
-- Tabla INMUTABLE que garantiza el encadenamiento criptográfico
CREATE TABLE fiscal_audit_log (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    sale_uuid UUID NOT NULL REFERENCES sales(uuid),
    store_id UUID NOT NULL REFERENCES stores(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    
    event_type VARCHAR(20) NOT NULL, -- ISSUE (Emisión), ANNUL (Anulación)
    
    -- LA CADENA (Requisito Legal Antifraude)
    previous_record_hash VARCHAR(64) NOT NULL, -- Hash del registro anterior
    chain_sequence_id INT NOT NULL, -- Secuencial estricto 1, 2, 3...
    
    -- Huella Digital
    record_hash VARCHAR(64) NOT NULL, -- Hash SHA-256 de este registro
    signature TEXT, -- Firma digital (si aplica)
    
    -- Identificación Software
    software_id VARCHAR(100) NOT NULL,
    software_version VARCHAR(50) NOT NULL,
    developer_id VARCHAR(100) NOT NULL,
    certification_reference VARCHAR(100),
    
    -- Datos Auditados
    invoice_amount BIGINT NOT NULL, -- stored in cents
    invoice_date TIMESTAMPTZ NOT NULL,
    
    -- Comunicación AEAT
    aeat_status VARCHAR(20) DEFAULT 'PENDING',
    aeat_csv_sent TEXT,
    aeat_response_json JSONB,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Integridad: Un dispositivo no puede repetir secuencia
    UNIQUE(device_id, chain_sequence_id),
    -- Integridad: Una venta no puede fiscalizarse dos veces por el mismo evento
    UNIQUE(sale_uuid, event_type),
    
    CONSTRAINT chk_hash_length CHECK (LENGTH(record_hash) = 64),
    CONSTRAINT chk_prev_hash_length CHECK (LENGTH(previous_record_hash) = 64)
);

-- ==================================================================================
-- 8. CONTABILIDAD Y AUDITORÍA INTERNA
-- ==================================================================================

-- Asientos Contables
CREATE TABLE accounting_entries (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    reference_type VARCHAR(50), -- SALE, PURCHASE, PAYMENT
    reference_uuid UUID,
    entry_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Líneas de Asiento (Apuntes)
CREATE TABLE accounting_entry_lines (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT REFERENCES accounting_entries(id) ON DELETE CASCADE,
    account_code VARCHAR(20) NOT NULL, -- Ej: '430000', '700000'
    debit BIGINT DEFAULT 0, -- Debe - stored in cents
    credit BIGINT DEFAULT 0, -- Haber - stored in cents
    
    -- Regla Contable: Una línea va al Debe O al Haber, no ambos (o cero)
    CONSTRAINT chk_debit_credit_exclusive CHECK (
        (debit = 0 AND credit > 0) OR (credit = 0 AND debit > 0)
    )
);

-- Auditoría de Acciones de Usuario (Log de Seguridad)
CREATE TABLE audit_user_actions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(100) NOT NULL, -- LOGIN_FAILED, PRICE_OVERRIDE
    entity VARCHAR(50),
    entity_id VARCHAR(100),
    payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 9. ÍNDICES Y OPTIMIZACIONES
-- ==================================================================================

CREATE INDEX idx_products_search ON products(company_id, name);
CREATE INDEX idx_products_ref ON products(company_id, reference);
CREATE INDEX idx_sales_date ON sales(issued_at_pos);
CREATE INDEX idx_sales_customer ON sales(customer_id);

-- Índice de Inventario Actualizado (Usa el nuevo company_id)
CREATE INDEX idx_inventory_lookup ON inventory_stock(company_id, product_id, warehouse_id);

CREATE INDEX idx_stock_movements_wh ON stock_movements(warehouse_id);
CREATE INDEX idx_stock_movements_prod ON stock_movements(product_id);

CREATE INDEX idx_fiscal_chain ON fiscal_audit_log(device_id, chain_sequence_id);
CREATE INDEX idx_sales_uuid ON sales(uuid);
CREATE INDEX idx_sales_store_date ON sales(store_id, issued_at_pos);
CREATE INDEX idx_sale_lines_sale ON sale_lines(sale_uuid);

-- ==================================================================================
-- 10. TRIGGERS Y SEGURIDAD DE DATOS
-- ==================================================================================

-- Función: Proteger el Log Fiscal (Inmutabilidad)
CREATE OR REPLACE FUNCTION prevent_change_fiscal_log()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'EL REGISTRO FISCAL ES INMUTABLE. Operación denegada por seguridad.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_fiscal_log
BEFORE UPDATE OR DELETE ON fiscal_audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscal_log();

-- Función: Validar Cadena Fiscal (Prevención de Huecos)
CREATE OR REPLACE FUNCTION validate_fiscal_sequence()
RETURNS TRIGGER AS $$
DECLARE
    last_seq INT;
BEGIN
    -- Bloquear registro anterior para evitar condiciones de carrera
    SELECT chain_sequence_id INTO last_seq
    FROM fiscal_audit_log
    WHERE device_id = NEW.device_id
    ORDER BY chain_sequence_id DESC
    LIMIT 1
    FOR UPDATE;
    
    IF last_seq IS NULL THEN last_seq := 0; END IF;

    -- Verificar que el nuevo registro es exactamente el siguiente (+1)
    IF NEW.chain_sequence_id != last_seq + 1 THEN
        RAISE EXCEPTION 'Rotura de Cadena Fiscal: Se esperaba % pero se recibió %', last_seq + 1, NEW.chain_sequence_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_fiscal_sequence
BEFORE INSERT ON fiscal_audit_log
FOR EACH ROW EXECUTE FUNCTION validate_fiscal_sequence();

-- Función: Proteger Ventas ya Fiscalizadas (No editar Facturas emitidas)
CREATE OR REPLACE FUNCTION prevent_change_fiscalized_sales()
RETURNS TRIGGER AS $$
DECLARE
    current_status VARCHAR(20);
BEGIN
    IF TG_TABLE_NAME = 'sales' THEN
        current_status := OLD.status;
    ELSE
        -- Si es una línea hija, buscar el estado del padre
        SELECT status INTO current_status FROM sales WHERE uuid = OLD.sale_uuid;
    END IF;

    IF current_status IN ('ISSUED', 'FISCALIZED', 'SENT_AEAT', 'AEAT_ACCEPTED') THEN
        RAISE EXCEPTION 'Operación Denegada: No se puede modificar una venta fiscalizada (Estado: %). Emitir Rectificativa.', current_status;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Aplicar protección a todas las tablas de venta
CREATE TRIGGER trg_protect_sales_del BEFORE DELETE OR UPDATE ON sales
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();

CREATE TRIGGER trg_protect_lines_del BEFORE DELETE OR UPDATE ON sale_lines
FOR EACH ROW EXECUTE FUNCTION prevent_change_fiscalized_sales();