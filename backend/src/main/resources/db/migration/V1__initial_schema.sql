-- BOCRM bootstrap schema (fresh install baseline)
-- Global tables in public schema. Tenant-scoped tables are created per-tenant
-- by TenantSchemaProvisioningService in tenant_<id> schemas.

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    external_org_id VARCHAR(255),
    external_org_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_external_org_id
    ON tenants(external_org_id)
    WHERE external_org_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    password_hash VARCHAR(255),
    oauth_provider VARCHAR(100),
    oauth_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    preferences JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Allow same email for different oauth providers, but enforce (provider, oauth_id) uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_oauth_provider_oauth_id
    ON users(oauth_provider, oauth_id)
    WHERE oauth_provider IS NOT NULL AND oauth_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS tenant_memberships (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'user',
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, user_id)
);

CREATE TABLE IF NOT EXISTS tenant_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    settings_jsonb JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_memberships_tenant_id ON tenant_memberships(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_memberships_user_id ON tenant_memberships(user_id);

-- Assistant tier definitions
CREATE TABLE IF NOT EXISTS assistant_tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    monthly_token_limit BIGINT NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    price_monthly DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO assistant_tiers (name, display_name, monthly_token_limit, model_id, price_monthly)
SELECT 'free', 'Free', 50000, 'claude-haiku-4-5-20251001', 0.00
WHERE NOT EXISTS (SELECT 1 FROM assistant_tiers WHERE name = 'free');

INSERT INTO assistant_tiers (name, display_name, monthly_token_limit, model_id, price_monthly)
SELECT 'pro', 'Pro', 2000000, 'claude-sonnet-4-6', 29.99
WHERE NOT EXISTS (SELECT 1 FROM assistant_tiers WHERE name = 'pro');

INSERT INTO assistant_tiers (name, display_name, monthly_token_limit, model_id, price_monthly)
SELECT 'enterprise', 'Enterprise', -1, 'claude-opus-4-6', 99.99
WHERE NOT EXISTS (SELECT 1 FROM assistant_tiers WHERE name = 'enterprise');

-- Per-tenant subscription tracking
CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    tier_id BIGINT NOT NULL REFERENCES assistant_tiers(id),
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    tokens_used_this_period BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant_id ON tenant_subscriptions(tenant_id);

-- Optional reporting schema used by some reporting pipelines.
CREATE SCHEMA IF NOT EXISTS reporting;

CREATE TABLE IF NOT EXISTS reporting.metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    metric_key VARCHAR(100) NOT NULL,
    metric_value NUMERIC(18, 4) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, metric_key, period_start, period_end)
);

CREATE TABLE IF NOT EXISTS reporting.rollups (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    rollup_jsonb JSONB NOT NULL DEFAULT '{}',
    computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_reporting_metrics_tenant ON reporting.metrics(tenant_id, metric_key);
CREATE INDEX IF NOT EXISTS idx_reporting_metrics_period ON reporting.metrics(tenant_id, period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_reporting_rollups_tenant ON reporting.rollups(tenant_id, entity_type);
