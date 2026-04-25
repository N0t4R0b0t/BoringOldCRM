-- Create chat_messages table if it doesn't exist
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    user_message TEXT,
    assistant_message TEXT,
    attachment_file_name VARCHAR(255),
    attachment_mime_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_chat_messages_tenant_id ON chat_messages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_attachment_file_name ON chat_messages(attachment_file_name);

-- Add attachment metadata columns (idempotent for existing tables)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'chat_messages' AND column_name = 'attachment_file_name'
        ) THEN
            ALTER TABLE chat_messages ADD COLUMN attachment_file_name VARCHAR(255);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'chat_messages' AND column_name = 'attachment_mime_type'
        ) THEN
            ALTER TABLE chat_messages ADD COLUMN attachment_mime_type VARCHAR(100);
        END IF;
    END IF;
END $$;
