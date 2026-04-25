-- Replace weak tool-calling Ollama models with Qwen2.5:14b which has first-class function-calling support.
-- llama3.1:8b and phi4 struggle with multi-step tool invocation chains.
DELETE FROM enabled_ai_models WHERE provider = 'ollama';

INSERT INTO enabled_ai_models (provider, model_id, enabled)
VALUES
  ('ollama', 'qwen2.5:14b',   true),
  ('ollama', 'mistral-nemo',   false)
ON CONFLICT (provider, model_id) DO UPDATE SET enabled = EXCLUDED.enabled;
