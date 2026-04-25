-- Add enabled column to assistant_tiers table to track which tiers are available for selection
-- In PostgreSQL 11+, adding a NOT NULL column with DEFAULT works atomically even on existing rows
ALTER TABLE assistant_tiers ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;
