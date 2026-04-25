-- Remove any chat_messages rows where tenant_id does not match this schema's tenant.
-- This is a safety cleanup: the backend scopes by JWT tenantId so this should
-- never have data, but guards against any future frontend session bleed where
-- the wrong session_id was used to POST messages to the wrong tenant context.
--
-- The schema name is tenant_<id>, so we extract the numeric suffix and compare.
-- Uses a DO block so it only runs the DELETE if any mismatched rows exist.
DO $$
DECLARE
    schema_tenant_id BIGINT;
BEGIN
    -- Extract numeric tenant id from current schema name (e.g. "tenant_2" -> 2)
    schema_tenant_id := NULLIF(regexp_replace(current_schema(), '^tenant_', ''), '')::BIGINT;

    IF schema_tenant_id IS NOT NULL THEN
        DELETE FROM chat_messages WHERE tenant_id != schema_tenant_id;
    END IF;
END $$;

-- Index on session_id for faster history lookups
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);

-- Index on tenant_id + user_id for the common query pattern
CREATE INDEX IF NOT EXISTS idx_chat_messages_tenant_user ON chat_messages(tenant_id, user_id);
