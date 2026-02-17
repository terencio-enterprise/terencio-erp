-- ==================================================================================
-- MIGRACIÓN V2: SISTEMA DE MARKETING, LEADS Y GESTIÓN DE FICHEROS S3
-- OBJETIVO: Unificar Leads/Clientes y preparar motor de envíos AWS SES
-- ==================================================================================

-- 1. MODIFICACIÓN DE TABLA CUSTOMERS (Soporte para Leads)
-- ======================================================

-- Relajar restricciones para permitir Leads incompletos
ALTER TABLE customers 
    ALTER COLUMN tax_id DROP NOT NULL,
    ALTER COLUMN address DROP NOT NULL;

-- Añadir campos de segmentación y estado de marketing
ALTER TABLE customers
    ADD COLUMN type VARCHAR(20) DEFAULT 'LEAD', -- 'LEAD', 'CLIENT', 'PROSPECT', 'CHURNED'
    ADD COLUMN origin VARCHAR(50), -- 'LANDING', 'MANUAL', 'IMPORT', 'REFERRAL'
    ADD COLUMN tags TEXT[], -- Array de PostgreSQL para etiquetas rápidas: ['VIP', 'INTERES_TPV']
    
    -- Gestión de Consentimiento y Estado
    ADD COLUMN marketing_consent BOOLEAN DEFAULT FALSE,
    ADD COLUMN marketing_status VARCHAR(20) DEFAULT 'SUBSCRIBED', -- 'SUBSCRIBED', 'UNSUBSCRIBED', 'BOUNCED', 'COMPLAINED'
    ADD COLUMN unsubscribe_token VARCHAR(64), -- Token de seguridad para baja
    ADD COLUMN last_interaction_at TIMESTAMPTZ,
    ADD COLUMN snoozed_until TIMESTAMPTZ; -- Para la opción "Pausar correos"

-- Índices para búsqueda rápida de Leads y unicidad
CREATE UNIQUE INDEX idx_customers_email_company_unique 
    ON customers (company_id, LOWER(email)) 
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX idx_customers_unsubscribe_token 
    ON customers (unsubscribe_token);

-- 2. SISTEMA DE PLANTILLAS (Marketing Templates)
-- ==============================================

CREATE TABLE marketing_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    
    code VARCHAR(50) NOT NULL, -- Identificador único lógico: 'WELCOME_EMAIL', 'NEWSLETTER_JAN'
    name VARCHAR(100) NOT NULL,
    
    subject_template VARCHAR(255) NOT NULL, -- Soporta variables: "Hola {{name}}"
    body_html TEXT NOT NULL, -- Contenido HTML crudo
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE(company_id, code)
);

-- 3. GESTIÓN DE ADJUNTOS (S3 References)
-- ======================================
-- No guardamos binarios, solo punteros a AWS S3
CREATE TABLE marketing_attachments (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT REFERENCES marketing_templates(id) ON DELETE CASCADE,
    
    -- Información del Fichero
    filename VARCHAR(255) NOT NULL, -- "catalogo.pdf"
    content_type VARCHAR(100) NOT NULL, -- "application/pdf"
    file_size_bytes BIGINT,
    
    -- Coordenadas AWS S3
    s3_bucket VARCHAR(100) NOT NULL,
    s3_key VARCHAR(255) NOT NULL, -- Ruta completa dentro del bucket
    s3_region VARCHAR(50) DEFAULT 'eu-west-1',
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. HISTORIAL DE ENVÍOS (Logs)
-- =============================

CREATE TABLE marketing_logs (
    id BIGSERIAL PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    template_id BIGINT REFERENCES marketing_templates(id),
    
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    
    status VARCHAR(20) NOT NULL, -- 'SENT', 'FAILED', 'BOUNCED', 'OPENED'
    message_id VARCHAR(255), -- ID devuelto por AWS SES (útil para trazar bounces)
    error_message TEXT,
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para analítica básica
CREATE INDEX idx_marketing_logs_customer ON marketing_logs(customer_id);
CREATE INDEX idx_marketing_logs_template ON marketing_logs(template_id);
CREATE INDEX idx_marketing_logs_status ON marketing_logs(company_id, status);

-- 5. WEBHOOKS DE REBOTES (Feedback Loop)
-- ======================================
-- Tabla opcional para guardar eventos crudos de AWS SNS si se quiere auditoría total
CREATE TABLE marketing_bounce_events (
    id BIGSERIAL PRIMARY KEY,
    aws_message_id VARCHAR(255),
    email_address VARCHAR(255),
    bounce_type VARCHAR(50), -- 'Permanent', 'Transient'
    bounce_subtype VARCHAR(50), -- 'General', 'MailboxFull'
    raw_json_payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
