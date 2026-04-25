-- Add long-lived MCP API key support for Claude Desktop integration
-- Keys are generated in-app by users, BCrypt-hashed for storage, and used instead of expiring JWT tokens

CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE IF NOT EXISTS admin.mcp_api_keys (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    key_hash     VARCHAR(255) NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mcp_api_keys_prefix  ON admin.mcp_api_keys(key_prefix);
CREATE INDEX IF NOT EXISTS idx_mcp_api_keys_tenant  ON admin.mcp_api_keys(tenant_id);
