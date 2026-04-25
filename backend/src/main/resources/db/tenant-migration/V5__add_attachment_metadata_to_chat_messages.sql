-- Add attachment metadata columns to chat_messages table in tenant schema
ALTER TABLE chat_messages
ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS attachment_mime_type VARCHAR(100);

-- Add index for attachment lookups
CREATE INDEX IF NOT EXISTS idx_chat_messages_attachment_file_name ON chat_messages(attachment_file_name);
