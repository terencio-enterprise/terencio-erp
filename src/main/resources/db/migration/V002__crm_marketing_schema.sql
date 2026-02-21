-- ==================================================================================
-- PARTE 2: TERENCIO ERP - CRM & MARKETING (V1)
-- Requiere: 01_core_schema.sql (Tabla companies)
-- ==================================================================================

-- ==================================================================================
-- 1. CONFIGURACIÓN GENERAL DE MARKETING
-- ==================================================================================
CREATE TABLE company_marketing_settings (
    company_id UUID PRIMARY KEY REFERENCES companies(id),
    
    -- Configuración de envío
    sender_name VARCHAR(100),
    sender_email VARCHAR(255),
    domain_verified BOOLEAN DEFAULT FALSE,
    daily_send_limit INTEGER DEFAULT 500,
    
    -- Email de bienvenida rápido (Obligatorio en V1)
    welcome_email_active BOOLEAN DEFAULT FALSE,
    welcome_template_id BIGINT, -- Se enlazará a marketing_templates
    welcome_delay_minutes INTEGER DEFAULT 5, -- Ej: 5 minutos
    
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 2. GESTIÓN DE RECURSOS (ASSETS)
-- ==================================================================================
CREATE TABLE company_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    
    storage_path VARCHAR(500) NOT NULL,
    public_url VARCHAR(1000), 
    is_public BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_company_assets_company ON company_assets(company_id);

-- ==================================================================================
-- 3. CRM & CLIENTES / LEADS (El corazón)
-- ==================================================================================
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id),
    
    -- Datos de Negocio
    tax_id VARCHAR(50),
    legal_name VARCHAR(255),
    commercial_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    country VARCHAR(10) DEFAULT 'ES',
    
    -- Datos Comerciales (Facturación)
    tariff_id BIGINT, 
    allow_credit BOOLEAN DEFAULT FALSE,
    credit_limit BIGINT DEFAULT 0,
    surcharge_apply BOOLEAN DEFAULT FALSE,
    
    -- Datos CRM / Marketing
    type VARCHAR(50) DEFAULT 'LEAD', -- Ej: 'LEAD', 'CLIENT_RETAIL', 'PARTNER_PRO', 'PARTNER_FREELANCE'
    origin VARCHAR(50), -- Ej: 'Landing_A', 'Manual', 'Importacion'
    tags TEXT[], -- Array de etiquetas (ej: ['vip', 'black_friday'])
    
    -- RGPD y Estado de Envíos
    marketing_consent BOOLEAN DEFAULT FALSE,
    marketing_status VARCHAR(20) DEFAULT 'SUBSCRIBED', -- 'SUBSCRIBED', 'UNSUBSCRIBED', 'BLOCKED'
    unsubscribe_token VARCHAR(64) UNIQUE, -- Token seguro para el link de baja
    
    last_interaction_at TIMESTAMPTZ,
    notes TEXT,
    active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_customers_email_company_unique ON customers (company_id, LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_marketing_status ON customers(company_id, marketing_status);
CREATE INDEX idx_customers_tags ON customers USING GIN (tags); -- Para búsquedas rápidas por etiqueta

-- ==================================================================================
-- 4. PLANTILLAS DE EMAIL (TEMPLATES)
-- ==================================================================================
CREATE TABLE marketing_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    name VARCHAR(100) NOT NULL,
    subject_template VARCHAR(255) NOT NULL,
    preheader_template VARCHAR(255), -- Útil para el texto de vista previa en Gmail
    body_html TEXT NOT NULL,
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enlazar la configuración de bienvenida a la plantilla
ALTER TABLE company_marketing_settings 
ADD CONSTRAINT fk_welcome_template FOREIGN KEY (welcome_template_id) REFERENCES marketing_templates(id) ON DELETE SET NULL;

-- ==================================================================================
-- 5. SEGMENTOS (Filtros guardados, sin complicaciones)
-- ==================================================================================
CREATE TABLE marketing_segments (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    
    -- Filtros aplicables (si es NULL, no filtra por ese campo)
    filter_types TEXT[], -- Ej: ['LEAD', 'CLIENT_RETAIL'] (Permite selección múltiple)
    filter_tags TEXT[], -- Debe contener ESTAS etiquetas
    filter_city VARCHAR(100),
    filter_origin VARCHAR(50),
    filter_marketing_status VARCHAR(20) DEFAULT 'SUBSCRIBED',
    filter_registered_after TIMESTAMPTZ,
    filter_registered_before TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 6. CAMPAÑAS (Manuales y Programadas)
-- ==================================================================================
CREATE TABLE marketing_campaigns (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    name VARCHAR(150) NOT NULL,
    template_id BIGINT NOT NULL REFERENCES marketing_templates(id),
    segment_id BIGINT REFERENCES marketing_segments(id), -- Si es NULL, va a todos los SUBSCRIBED
    
    status VARCHAR(20) DEFAULT 'DRAFT', -- 'DRAFT', 'SCHEDULED', 'SENDING', 'COMPLETED', 'CANCELLED'
    scheduled_at TIMESTAMPTZ, -- Si es NULL y pasa a SENDING, es envío manual inmediato
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    -- Métricas desnormalizadas (Para que el Dashboard sea instantáneo)
    metrics_total_recipients INTEGER DEFAULT 0,
    metrics_sent INTEGER DEFAULT 0,
    metrics_delivered INTEGER DEFAULT 0,
    metrics_opened INTEGER DEFAULT 0,
    metrics_clicked INTEGER DEFAULT 0,
    metrics_bounced INTEGER DEFAULT 0,
    metrics_unsubscribed INTEGER DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- 7. LOGS DE ENVÍO Y MÉTRICAS INDIVIDUALES
-- ==================================================================================
CREATE TABLE marketing_email_logs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES marketing_templates(id),
    
    -- Origen del envío (Asociado a una campaña)
    campaign_id BIGINT REFERENCES marketing_campaigns(id) ON DELETE CASCADE,
    
    message_id VARCHAR(255), -- ID devuelto por el proveedor (AWS SES, SendGrid, Mailgun)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
    error_message TEXT,
    
    -- Tiempos para trackear el embudo (Métricas)
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    opened_at TIMESTAMPTZ,
    clicked_at TIMESTAMPTZ,
    bounced_at TIMESTAMPTZ,
    unsubscribed_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_email_logs_campaign ON marketing_email_logs(campaign_id);
CREATE INDEX idx_email_logs_customer ON marketing_email_logs(customer_id);
CREATE INDEX idx_email_logs_message_id ON marketing_email_logs(message_id); -- Vital para conciliar webhooks

-- ==================================================================================
-- 8. EVENTOS DE PROVEEDOR (Webhooks de rebotes, quejas, etc.)
-- ==================================================================================
CREATE TABLE email_delivery_events (
    id BIGSERIAL PRIMARY KEY,
    provider_message_id VARCHAR(255), 
    email_address VARCHAR(255),
    event_type VARCHAR(50), -- 'BOUNCE', 'COMPLAINT', 'DELIVERY', 'OPEN', 'CLICK'
    bounce_type VARCHAR(50), -- 'HardBounce', 'SoftBounce'
    raw_payload JSONB, -- Guardamos el JSON íntegro por si hay que auditar
    processed BOOLEAN DEFAULT FALSE, -- Para saber si nuestro worker ya actualizó el log y el cliente
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_delivery_events_msg_id ON email_delivery_events(provider_message_id);
CREATE INDEX idx_delivery_events_processed ON email_delivery_events(processed);


-- 1. Index scheduled campaigns for faster scheduler lookups
CREATE INDEX IF NOT EXISTS idx_marketing_campaigns_scheduled 
ON marketing_campaigns(status, scheduled_at) 
WHERE status = 'SCHEDULED';

-- 2. Database level idempotency for campaign logs
CREATE UNIQUE INDEX IF NOT EXISTS uq_campaign_customer_log 
ON marketing_email_logs(campaign_id, customer_id) 
WHERE status != 'FAILED';

-- 3. Token lookup optimization
CREATE INDEX IF NOT EXISTS idx_customers_unsubscribe_token 
ON customers(unsubscribe_token);

-- ==================================================================================
-- ADD MISSING INDICES AND CONSTRAINTS FOR CRM & MARKETING V1.1
-- ==================================================================================

-- Optimization: Searching customers by email is case-insensitive in Java, so we do it here too
CREATE INDEX IF NOT EXISTS idx_customers_email_lower ON customers (LOWER(email));

-- Optimization: Campaign status filtering for the scheduler
CREATE INDEX IF NOT EXISTS idx_marketing_campaigns_status_scheduled ON marketing_campaigns(status, scheduled_at) 
WHERE status = 'SCHEDULED';

-- Security: Ensure unique tracking links per campaign-customer (Idempotency)
-- If a log exists for a campaign/customer, we shouldn't create another unless previous failed
CREATE UNIQUE INDEX IF NOT EXISTS uq_campaign_customer_log 
ON marketing_email_logs(campaign_id, customer_id) 
WHERE status != 'FAILED';

-- Performance: Rapid lookup of customer by token for preferences page
CREATE INDEX IF NOT EXISTS idx_customers_unsubscribe_token ON customers(unsubscribe_token);

-- Performance: Aggregating logs for campaign metrics
CREATE INDEX IF NOT EXISTS idx_email_logs_status_metrics ON marketing_email_logs(campaign_id, status);