-- Migration to enforce user_id-based tenant membership assignment
-- This change improves handling of duplicate users (same email with different OAuth providers)
-- by requiring explicit user ID selection instead of ambiguous email lookup

-- Note: tenant_memberships table already uses user_id (bigint, not null)
-- This migration documents the API contract change:
-- - AddUserToTenantRequest now requires 'userId' instead of 'email'
-- - TenantMembershipService.addUserToTenant() now validates user_id existence directly
-- - This eliminates ambiguity when multiple users share the same email

-- Verify that tenant_memberships has the correct FK to users
-- (Should already exist from initial schema, but documenting for clarity)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name = 'tenant_memberships'
    AND constraint_type = 'FOREIGN KEY'
    AND table_schema = 'public'
  ) THEN
    ALTER TABLE tenant_memberships ADD CONSTRAINT fk_tenant_memberships_user_id
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
END$$;
