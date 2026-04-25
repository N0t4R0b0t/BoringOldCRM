-- Stores tenant-configurable option lists for core entity fields
-- (e.g. Customer.status, Opportunity.stage, Activity.type, Asset.status)
CREATE TABLE IF NOT EXISTS entity_field_options (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL,
    entity_type VARCHAR(50)   NOT NULL,
    field_name  VARCHAR(100)  NOT NULL,
    options     JSONB         NOT NULL DEFAULT '[]',
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_entity_field_options UNIQUE (tenant_id, entity_type, field_name)
);
