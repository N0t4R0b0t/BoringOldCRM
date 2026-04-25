-- Drop the unique constraint on email that was missed in V10
-- The email column can now be shared across multiple users (e.g., local login + OAuth logins via different providers)
-- Uniqueness is enforced via (oauth_provider, oauth_id) for OAuth users instead

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
