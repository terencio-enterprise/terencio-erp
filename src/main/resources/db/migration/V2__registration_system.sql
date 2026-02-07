-- ==================================================================================
-- MIGRATION V2: POS Registration System
-- ==================================================================================

-- Table to manage One-Time Registration Codes (OTP)
CREATE TABLE registration_codes (
    code VARCHAR(6) PRIMARY KEY, -- The 6-digit code (e.g., 'A1B2C3')
    store_id UUID NOT NULL REFERENCES stores(id),
    
    -- Pre-configuration for the device that will register
    preassigned_name VARCHAR(100), -- e.g., "Caja 1 - Entrada"
    
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    used_by_device_id UUID, -- Links to devices table after usage
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==================================================================================
-- SEED DATA (For Testing)
-- ==================================================================================

-- 1. Create a Default Store
INSERT INTO stores (id, code, name, address, tax_id)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 
    'MAD-001', 
    'Supermercados Terencio - Madrid Centro', 
    'Calle Gran Vía 1, Madrid', 
    'B12345678'
) ON CONFLICT DO NOTHING;

-- 2. Create a Default User (Admin) linked to that store
INSERT INTO users (username, pin_hash, full_name, role, store_id)
VALUES (
    'admin', 
    '$2a$10$X7V.jO.vj.j.j.j.j.j.j.j.j.j.j', -- Hash for '123456' (Example)
    'Administrador Tienda', 
    'ADMIN',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
) ON CONFLICT DO NOTHING;

-- 3. Create a TEST REGISTRATION CODE '123456'
-- This allows you to register the POS immediately without building the Admin UI yet
INSERT INTO registration_codes (code, store_id, preassigned_name, expires_at)
VALUES (
    '123456', 
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 
    'Caja Principal 01', 
    NOW() + INTERVAL '1 year' -- Long expiration for dev
) ON CONFLICT DO NOTHING;