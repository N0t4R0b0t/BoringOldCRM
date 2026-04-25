-- Add label column to tenant_backup_jobs used by backup feature
ALTER TABLE IF EXISTS tenant_backup_jobs ADD COLUMN IF NOT EXISTS label VARCHAR(255);

-- Also add label to any tenant schema tables (idempotent)
DO $$
DECLARE
  s RECORD;
BEGIN
  FOR s IN SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog','information_schema') LOOP
    BEGIN
      EXECUTE format('ALTER TABLE IF EXISTS %I.tenant_backup_jobs ADD COLUMN IF NOT EXISTS label VARCHAR(255);', s.schema_name);
    EXCEPTION WHEN undefined_table THEN
      -- ignore schemas that don't have the table
      NULL;
    END;
  END LOOP;
END$$;
