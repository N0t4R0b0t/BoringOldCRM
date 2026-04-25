Create a new Flyway migration for BOCRM.

Steps:
1. Run `ls backend/src/main/resources/db/migration/` to find the current highest version number
2. Name the new file `V{N+1}__<describe_change>.sql` (snake_case description)
3. Write the migration with these rules:
   - Always idempotent: `CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`
   - For NOT NULL columns on existing tables: use `ADD COLUMN IF NOT EXISTS col type DEFAULT value` — never `NOT NULL` without a `DEFAULT` on an existing table (Hibernate ddl-auto will conflict)
   - No destructive operations without explicit confirmation

For **tenant schemas** (chat_messages, custom fields, etc.) the migration goes in:
`backend/src/main/resources/db/tenant-migration/V{N+1}__<describe>.sql`
These are applied via `TenantFlywayMigrationService` to all tenant schemas.

For **admin schema** (tenants, users, memberships, assistant_tiers, enabled_ai_models, etc.) the migration goes in:
`backend/src/main/resources/db/migration/V{N+1}__<describe>.sql`

Tell me what change you need and I'll scaffold the correct file.
