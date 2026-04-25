-- Add custom_data and table_data_jsonb JSONB columns to CRM entity tables for existing tenants.
-- These columns were present in V1 for new tenants but were never backfilled in V2,
-- so existing tenants whose schemas pre-date Flyway migration are missing them.
-- All statements are idempotent.

ALTER TABLE IF EXISTS customers
    ADD COLUMN IF NOT EXISTS custom_data JSONB,
    ADD COLUMN IF NOT EXISTS table_data_jsonb JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS contacts
    ADD COLUMN IF NOT EXISTS custom_data JSONB,
    ADD COLUMN IF NOT EXISTS table_data_jsonb JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS opportunities
    ADD COLUMN IF NOT EXISTS custom_data JSONB,
    ADD COLUMN IF NOT EXISTS table_data_jsonb JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS activities
    ADD COLUMN IF NOT EXISTS custom_data JSONB,
    ADD COLUMN IF NOT EXISTS table_data_jsonb JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS assets
    ADD COLUMN IF NOT EXISTS custom_data JSONB,
    ADD COLUMN IF NOT EXISTS table_data_jsonb JSONB DEFAULT '{}'::jsonb;
