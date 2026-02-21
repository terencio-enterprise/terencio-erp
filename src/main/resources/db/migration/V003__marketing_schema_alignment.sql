-- Align marketing schema with current application model and behavior

ALTER TABLE marketing_templates
    ADD COLUMN IF NOT EXISTS code VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_marketing_templates_company_code
    ON marketing_templates(company_id, LOWER(code))
    WHERE code IS NOT NULL;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS marketing_snooze_until TIMESTAMPTZ;

ALTER TABLE customers
    ALTER COLUMN marketing_status SET DEFAULT 'UNSUBSCRIBED';

UPDATE customers
SET marketing_status = 'UNSUBSCRIBED'
WHERE marketing_status IS NULL
   OR (marketing_consent = FALSE AND marketing_status = 'SUBSCRIBED');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_customers_marketing_status'
    ) THEN
        ALTER TABLE customers
            ADD CONSTRAINT chk_customers_marketing_status
            CHECK (marketing_status IN ('SUBSCRIBED', 'UNSUBSCRIBED', 'SNOOZED', 'BLOCKED'));
    END IF;
END
$$;