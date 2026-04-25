-- Create table to track which AI models are enabled/disabled per provider
CREATE TABLE IF NOT EXISTS enabled_ai_models (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    model_id VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_model_id UNIQUE(provider, model_id)
);

-- Seed with common models (all enabled by default)
INSERT INTO enabled_ai_models (provider, model_id, enabled) VALUES
    ('anthropic', 'claude-haiku-4-5-20251001', true),
    ('anthropic', 'claude-sonnet-4-6', true),
    ('anthropic', 'claude-opus-4-6', true),
    ('openai', 'gpt-4o-mini', true),
    ('openai', 'gpt-4o', true),
    ('openai', 'gpt-4-turbo', true),
    ('openai', 'gpt-4', true),
    ('google', 'gemini-2.5-flash', true),
    ('google', 'gemini-2.5-flash-lite', true),
    ('google', 'gemini-3-flash-preview', true),
    ('google', 'gemini-3.1-flash-lite-preview', true)
ON CONFLICT DO NOTHING;
