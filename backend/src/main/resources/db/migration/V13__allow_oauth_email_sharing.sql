-- Allow multiple OAuth users to share the same email (from different Auth0 orgs/providers)
-- Constraint is now (oauth_provider, oauth_id) instead of unique email
-- This prevents duplicate user creation when same email is used across different auth sources

-- Drop the old unique constraint on email (if it exists)
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk6dotkott2kjsp8vw4d0m25fb7;

-- Add new unique constraint on (oauth_provider, oauth_id)
-- This ensures (provider, subject) pairs are unique for OAuth users
-- Only add if it doesn't already exist
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name = 'users'
    AND constraint_name = 'uk_oauth_provider_id'
    AND table_schema = 'public'
  ) THEN
    ALTER TABLE users ADD CONSTRAINT uk_oauth_provider_id
      UNIQUE (oauth_provider, oauth_id);
  END IF;
END$$;
