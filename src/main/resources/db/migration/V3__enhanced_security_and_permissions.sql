-- ==================================================================================
-- MIGRATION V3: Enhanced Security (Dual Passwords) & Permissions
-- ==================================================================================

-- 1. Add password_hash for Backoffice access (distinct from pin_hash for POS)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 2. Ensure existing users can still login (copy pin to password temporarily if null)
UPDATE users SET password_hash = pin_hash WHERE password_hash IS NULL;

-- 3. Ensure permissions_json is initialized
UPDATE users SET permissions_json = '[]' WHERE permissions_json IS NULL;