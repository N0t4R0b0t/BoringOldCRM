-- Replace Ollama models that don't support tools with tool-capable alternatives.
-- gemma3 and llama3.2 (3B) are not reliable for function/tool calling.
DELETE FROM enabled_ai_models WHERE provider = 'ollama' AND model_id IN ('llama3.2', 'gemma3');

INSERT INTO enabled_ai_models (provider, model_id, enabled)
VALUES
  ('ollama', 'llama3.1:8b', false),
  ('ollama', 'phi4',        false),
  ('ollama', 'gemma3:27b',  false)
ON CONFLICT (provider, model_id) DO NOTHING;
