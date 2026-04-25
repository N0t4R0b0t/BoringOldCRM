-- Rename assets table and related objects to custom_records

ALTER TABLE IF EXISTS assets RENAME TO custom_records;
ALTER TABLE IF EXISTS opportunity_assets RENAME TO opportunity_custom_records;

ALTER TABLE IF EXISTS opportunity_custom_records
    RENAME COLUMN asset_id TO custom_record_id;

-- Rename indexes
ALTER INDEX IF EXISTS idx_assets_tenant_id RENAME TO idx_custom_records_tenant_id;
ALTER INDEX IF EXISTS idx_assets_customer_id RENAME TO idx_custom_records_customer_id;
ALTER INDEX IF EXISTS idx_opportunity_assets_opp RENAME TO idx_opportunity_custom_records_opp;
ALTER INDEX IF EXISTS idx_opportunity_assets_asset RENAME TO idx_opportunity_custom_records_custom_record;

-- Update entity type references stored in access control tables
UPDATE record_access_policies SET entity_type = 'CustomRecord' WHERE entity_type = 'Asset';
UPDATE record_access_grants SET entity_type = 'CustomRecord' WHERE entity_type = 'Asset';

-- Update custom field type values stored in custom field definitions
UPDATE custom_field_definitions SET type = 'custom_record' WHERE type = 'asset';
UPDATE custom_field_definitions SET type = 'custom_record_multi' WHERE type = 'asset_multi';

