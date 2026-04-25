-- Add provider column to assistant_tiers if missing (Hibernate adds it but doesn't seed the value)
ALTER TABLE assistant_tiers ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'anthropic';

-- Back-fill provider on existing seeded tiers
UPDATE assistant_tiers SET provider = 'anthropic' WHERE provider IS NULL OR provider = '';

-- Ensure enabled_ai_models is seeded (idempotent — V9 may have already done this)
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
