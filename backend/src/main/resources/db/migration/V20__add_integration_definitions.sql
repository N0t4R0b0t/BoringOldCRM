CREATE TABLE IF NOT EXISTS integration_definitions (
    id BIGSERIAL PRIMARY KEY,
    adapter_type VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    credential_schema JSONB
);

INSERT INTO integration_definitions (adapter_type, display_name, description, credential_schema) VALUES
    ('slack', 'Slack', 'Send CRM events to Slack channels via incoming webhooks', '{"webhookUrl": {"type": "string", "required": true}}'),
    ('webhook', 'Generic Webhook', 'Post CRM events to any HTTP endpoint', '{"url": {"type": "string", "required": true}, "secret": {"type": "string", "required": false}}'),
    ('hubspot', 'HubSpot', 'Sync customers and opportunities with HubSpot', '{"apiKey": {"type": "string", "required": true}}'),
    ('zapier', 'Zapier', 'Trigger Zapier workflows from CRM events', '{"webhookUrl": {"type": "string", "required": true}}')
ON CONFLICT DO NOTHING;
