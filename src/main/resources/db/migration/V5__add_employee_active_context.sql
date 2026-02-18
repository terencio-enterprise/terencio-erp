-- Add last_active_company_id and last_active_store_id to employees table

ALTER TABLE employees
ADD COLUMN IF NOT EXISTS last_active_company_id UUID,
ADD COLUMN IF NOT EXISTS last_active_store_id UUID;

-- foreign key constraints (optional but good for integrity)
-- referencing companies(id) and stores(id)
-- We need to ensure they exist if we add constraints.
-- Let's add simple FKs.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_employees_last_active_company') THEN
        ALTER TABLE employees
        ADD CONSTRAINT fk_employees_last_active_company
        FOREIGN KEY (last_active_company_id) REFERENCES companies(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_employees_last_active_store') THEN
        ALTER TABLE employees
        ADD CONSTRAINT fk_employees_last_active_store
        FOREIGN KEY (last_active_store_id) REFERENCES stores(id) ON DELETE SET NULL;
    END IF;
END $$;
