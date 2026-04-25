-- Backfill columns and tables for existing tenant schemas
-- This migration adds new columns to existing tables and creates new feature tables
-- that were added after initial schema creation. All statements are idempotent.

-- Add enabled column to calculated_field_definitions for existing tenants
ALTER TABLE IF EXISTS calculated_field_definitions ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- Add display_order to field definition tables for existing tenants
ALTER TABLE IF EXISTS custom_field_definitions ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS calculated_field_definitions ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

-- Add display_in_table column to calculated_field_definitions for existing tenants
ALTER TABLE IF EXISTS calculated_field_definitions ADD COLUMN IF NOT EXISTS display_in_table BOOLEAN NOT NULL DEFAULT false;

-- Add opportunity_type_slug to opportunities for existing tenants
ALTER TABLE IF EXISTS opportunities ADD COLUMN IF NOT EXISTS opportunity_type_slug VARCHAR(100);

-- Add owner_id to assets and tenant_documents for existing tenants
ALTER TABLE IF EXISTS assets ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE IF EXISTS tenant_documents ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- Ensure label column exists for tenant_backup_jobs for older schemas
ALTER TABLE IF EXISTS tenant_backup_jobs ADD COLUMN IF NOT EXISTS label VARCHAR(255);
