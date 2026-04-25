-- Add retry_count column to outbox_events table for retry management

ALTER TABLE outbox_events
ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
