-- ==================================================================================
-- MIGRATION V4: Device Authentication System
-- ==================================================================================

-- Add device authentication columns to devices table
ALTER TABLE devices ADD COLUMN device_secret VARCHAR(255);
ALTER TABLE devices ADD COLUMN api_key_version INTEGER DEFAULT 1;
ALTER TABLE devices ADD COLUMN last_authenticated_at TIMESTAMPTZ;

-- Create index for faster API key lookups
CREATE INDEX idx_devices_status_active ON devices(status) WHERE status = 'ACTIVE';

-- Add comment for clarity
COMMENT ON COLUMN devices.device_secret IS 'Device-specific secret used with system secret to generate and validate HMAC-based API keys. Stored encrypted at rest via database-level encryption.';
COMMENT ON COLUMN devices.api_key_version IS 'Version number for API key rotation - increment to invalidate old keys';
COMMENT ON COLUMN devices.last_authenticated_at IS 'Last successful API authentication timestamp';
