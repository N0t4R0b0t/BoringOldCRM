-- Add seed Ollama models to enabled_ai_models table
INSERT INTO enabled_ai_models (provider, model_id, enabled)
VALUES
  ('ollama', 'llama3.2', false),
  ('ollama', 'mistral', false),
  ('ollama', 'gemma3', false)
ON CONFLICT (provider, model_id) DO NOTHING;
