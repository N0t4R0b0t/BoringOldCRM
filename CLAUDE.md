# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference
- New entity: entity → repo → DTO → service → controller → migration → test (see recipe below)
- Every service method: `TenantContext.getTenantId() == null → throw ForbiddenException`
- Jackson: `tools.jackson.*` NOT `com.fasterxml.jackson.*` (Spring Boot 4 rebrand — will not compile otherwise)
- Migrations: run `ls db/migration/` before naming new file; use `IF NOT EXISTS` + `DEFAULT` for NOT NULL columns
- Admin schema entities: `Tenant`, `User`, `TenantMembership`, `TenantSettings` (use `admin.*` tables)
- Tenant schema entities: `Customer`, `Contact`, etc. (use `public.*` tables, routed by Hibernate search_path)
- Latest features: `EnabledAiModel` tier toggles (V8/V9 migrations), `OnboardingService`, per-tenant AI tier assignment in SystemAdminController; V22 migration adds Ollama self-hosted provider (`llama3.2`, `mistral`, `gemma3`)

## Project Overview

BOCRM is a multi-tenant CRM with schema-per-tenant Postgres isolation. Backend: Spring Boot 4.0.2 (Java 21, Gradle). Frontend: React 19 + Vite + TypeScript. Async: RabbitMQ + outbox pattern.

Demo credentials: `demo@bocrm.com` / `demo123`

## Commands

### Backend (from `backend/`)
```bash
./gradlew bootRun          # Run dev server (http://localhost:8080/api)
./gradlew build            # Build JAR
./gradlew test             # Run all tests
./gradlew test --tests "com.bocrm.backend.controller.CustomerControllerTest"  # Run single test class
./gradlew clean            # Clean build output
```

### Frontend (from `frontend/`)
```bash
npm install                # Install dependencies (Node 20 required)
npm run dev                # Dev server (http://localhost:5173)
npm run build              # Production build (tsc + vite)
npm run lint               # ESLint check
```

### Local infrastructure
```bash
docker compose up -d       # Start Postgres (5432) and RabbitMQ (5672, UI at :15672)
bash scripts/install-hooks.sh  # Install pre-commit gitleaks secret scanning
```

### Config files
- Backend local overrides: `backend/src/main/resources/application-local.yml` (gitignored)
- Frontend local env: `frontend/.env.local` (gitignored); template at `frontend/.env.example`
- DB: `jdbc:postgresql://localhost:5432/crm`, user/pass: `crm/crm`

## Architecture

### Multi-tenancy
The central architectural pattern. Each tenant gets a dedicated Postgres schema named `tenant_<id>`. Hibernate's `SCHEMA` multi-tenancy mode handles connection routing:

1. JWT contains `tenantId` and `userId`
2. `JwtAuthenticationFilter` extracts them and stores in `TenantContext` (ThreadLocal)
3. `CurrentTenantSchemaResolver` reads from `TenantContext` to resolve the schema
4. `SchemaPerTenantConnectionProvider` switches the connection's search path per request

`TenantContext.clear()` must always be called after each request (done in the filter). The **admin schema** (`admin.*`) holds tenants, users, and memberships and is always accessed directly.

### Backend structure (`backend/src/main/java/com/bocrm/backend/`)
- `config/` — Security (JWT filter, CORS), Hibernate multi-tenancy wiring, OpenAPI
- `controller/` — REST controllers; all tenant-scoped endpoints rely on `TenantContext`
- `service/` — Business logic; services must not set tenant context themselves
- `entity/` — JPA entities; tenant-schema entities use `public.*` table names (resolved via schema routing)
- `repository/` — Spring Data repositories
- `dto/` — Request/response POJOs
- `shared/` — `TenantContext` (ThreadLocal) and `TenantSchema` (naming convention)
- `util/` — `JwtProvider` (JJWT), CEL evaluator for calculated fields
- `exception/` — Custom exceptions and global handler

Key entities: `Customer`, `Contact`, `Opportunity`, `Activity` (tenant-scoped); `Tenant`, `User`, `TenantMembership`, `TenantSettings` (admin-scoped). Custom field values are stored as JSONB on the main entity (`custom_fields` column). `CalculatedFieldDefinition` uses CEL expressions; values cached in `CalculatedFieldValue`. **Access control** entities: `RecordAccessPolicy` (per-record visibility mode), `RecordAccessGrant` (user/group-level access), `UserGroup` (named groups for bulk granting).

All backend endpoints are served under `/api` (configured as `server.servlet.context-path`).

### Frontend structure (`frontend/src/`)
- `api/apiClient.ts` — Single Axios instance; auto-injects JWT, handles 401→refresh→retry
- `store/` — Zustand stores: `authStore` (user/tenant/tokens), `crmStore`, `customFieldsStore`, `uiStore`
- `pages/` — One file per page/route
- `components/` — Reusable UI components
- `types/` — TypeScript types including pagination

Auth state persists in `localStorage`. Switching tenants re-issues a new JWT and reloads the page.

### Multi-tenant user login and switching

Users can belong to multiple tenants with different roles. Key rule: if user has multiple active memberships and no OIDC org claim → `requiresTenantSelection: true` in `LoginResponse` → redirect to `/select-tenant`. Role comes from the target membership, not the previous session. See `SelectTenantPage.tsx`, `TenantSwitcher.tsx`.

### OIDC / external auth
Optionally enabled via `app.external-auth.*` config (see `application-example.yml`). When enabled, the `/auth/external/login` endpoint accepts an ID token from an OIDC provider and maps it to a local user/tenant. The frontend handles the OIDC callback at `/auth/callback` (`OidcCallbackPage`).

**Org claim mapping**: If OIDC token includes `org_id` or `org_name` claim, it maps directly to a tenant (single-tenant path, skips selection). If no org claim and user has multiple memberships, tenant selection is triggered.

### Tests
Integration tests extend `BaseIntegrationTest` which uses `@SpringBootTest`, `MockMvc`, and `@ActiveProfiles("test")`. Each test gets a fresh tenant and user via `@Transactional` rollback. The test profile uses H2 in-memory DB (bypassing real Postgres and multi-tenancy schema switching).

To add a new controller test, extend `BaseIntegrationTest` — `testTenant`, `testUser`, and `accessToken` are provided in `baseSetUp()`.

### Secret scanning
Pre-commit hook runs gitleaks. Install with `bash scripts/install-hooks.sh`. CI also runs on all pushes via `.github/workflows/secret-scan.yml`.

### Release workflow (`.github/workflows/release.yml`)

Three triggers:
1. **Tag push** (`v*`) — creates a full GitHub release with generated release notes.
2. **Push to `master`** — creates/replaces a single `snapshot` prerelease. The previous snapshot release and tag are deleted before the new one is created (`gh release delete snapshot --yes --cleanup-tag`), so only one snapshot exists at a time. The release body includes the triggering commit SHA.
3. **Manual** (`workflow_dispatch`) — builds and uploads an artifact. Optionally accepts a `version` label; defaults to the short git SHA. Does not create a GitHub release.

All three paths build the backend JAR (`./gradlew bootJar`) and frontend (`npm ci && npm run build`) and bundle them into `bocrm-<version>.tar.gz`, which is always uploaded as a workflow artifact (30-day retention).

---

## Claude Working Instructions

Instructions written from direct codebase analysis. Follow these when implementing anything in this repo.

### Before touching any file

1. Read the file first — never guess at structure or method signatures.
2. Check `TenantContext.getTenantId()` / `TenantContext.getUserId()` are in scope; every tenant-scoped service method must guard against null context with `throw new ForbiddenException("Tenant context not set")`.
3. Confirm the Flyway migration baseline: only `V1__initial_schema.sql` exists — new migrations must follow the next sequential version. Run `ls backend/src/main/resources/db/migration/` before naming a new file.

### Multi-tenancy rules (non-negotiable)

- **Never** set `TenantContext` inside a service — the JWT filter owns that lifecycle.
- **Always** verify tenant ownership after loading an entity: `if (!entity.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied")`.
- Admin-schema entities (`Tenant`, `User`, `TenantMembership`, `TenantSettings`) use `admin.*` tables. Tenant-schema entities use `public.*` table names — Hibernate routes them via schema search_path.
- `TenantContext.clear()` is the filter's responsibility; do not call it in services or controllers.

### Adding a new backend entity (recipe)

Follow this exact order to match existing patterns:

1. **Entity** (`entity/Foo.java`): use `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@PrePersist`/`@PreUpdate` for timestamps, `@JdbcTypeCode(SqlTypes.JSON)` for JSONB columns. Include `tenantId` and timestamps.
2. **Repository** (`repository/FooRepository.java`): extend `JpaRepository<Foo, Long>` + `JpaSpecificationExecutor<Foo>`. Add `findByTenantId(Long tenantId)` at minimum.
3. **DTOs** (`dto/`): `FooDTO` (response), `CreateFooRequest`, `UpdateFooRequest`. Use Lombok `@Data @Builder`.
4. **Service** (`service/FooService.java`): annotate `@Service @Slf4j`. Inject `AuditLogService` and call `auditLogService.logAction(...)` on every write. Use Spring Cache annotations (`@CacheEvict`/`@Cacheable`) matching the pattern in `CustomerService`.
5. **Controller** (`controller/FooController.java`): `@RestController @RequestMapping("/foos") @Tag(name=...) @Slf4j`. Constructor injection only (no `@Autowired` on fields). Use `@Operation(summary=...)` on each endpoint. Return `ResponseEntity<PagedResponse<FooDTO>>` for list endpoints.
6. **Migration**: `V{N}__describe_change.sql` — always idempotent (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`).
7. **Test**: extend `BaseIntegrationTest`, use `mockMvc` + `objectMapper`, pass `Authorization: Bearer {accessToken}` header.

### Service implementation patterns (from CustomerService)

- Start every method with: `Long tenantId = TenantContext.getTenantId(); if (tenantId == null) throw new ForbiddenException(...);`
- List endpoints use JPA `Specification` + `buildPageable(page, size, sortBy, sortOrder, allowedSorts)` — cap size at 200, default sort to `createdAt`.
- Custom fields: call `fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "EntityType", node)` before persisting. Merge on update using `ObjectNode.setAll(...)` — do not replace wholesale.
- Two JSONB columns on CRM entities: `custom_data` (full data, used in detail view) and `table_data_jsonb` (denormalized display fields, used in list view via `toDTOTableView`). Keep both in sync on every write.

### Custom fields pattern

Custom field values are stored directly on entities as JSONB (not in a separate table). The `EntityCustomField` entity/table still exists but the primary storage is `custom_data` column on `Customer`, `Contact`, `Opportunity`, `Activity`. When adding a new entity that supports custom fields:
- Add `custom_data jsonb` and `table_data_jsonb jsonb` columns in migration.
- Add `@JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_data", columnDefinition = "jsonb")` and matching `table_data_jsonb` field to the entity.
- Follow the merge pattern in `CustomerService.updateCustomer` exactly.

**Workflow field type** (`workflow`):
- Milestone-based workflow tracker. Config stores `{"milestones": ["Step1", "Step2", "Step3"]}`.
- Field values stored as `{"currentIndex": number | null}` where `currentIndex` is the 0-based active milestone index.
- Frontend: `WorkflowField.tsx` provides interactive stepper UI with animated milestones (completed green ✓, active pulsing blue, upcoming gray).
- Users click to advance, skip ahead, or revert to previous milestones; only one can be active at a time.
- View components (CustomerView, ContactView, etc.) render workflow in read-only disabled mode.
- Calculated fields can reference workflow progress: `customField_workflowKey.currentIndex` (compare as number).
- AI assistant can create workflow fields and set state via `createCustomField(type="workflow")` and `updateCustomer({customFields: {workflowKey: {currentIndex: N}}})`.
- Custom field definition UI: admin enters milestones as comma-separated list, stored as array in config.

### Jackson: use `tools.jackson` not `com.fasterxml.jackson`

This project imports `tools.jackson.databind.JsonNode` / `ObjectMapper` / `ObjectNode` (Spring Boot 4 rebrand). Do not use `com.fasterxml.jackson.*` imports — they will not compile.

### Frontend patterns

- All API calls go through `api/apiClient.ts` (Axios) — never create a second Axios instance.
- New pages: create `src/pages/FooPage.tsx`, add route in `App.tsx` wrapped in `<ProtectedRoute>`.
- New state: add to the appropriate Zustand store in `src/store/` — do not use React `useState` for shared data.
- Follow `CustomersPage.tsx` + `CustomerFormPage.tsx` as the canonical templates for list + form pages.
- CSS: add to existing module files in `src/styles/` — no inline styles, no new CSS frameworks.

### Testing rules

- All integration tests extend `BaseIntegrationTest` — never bypass it.
- `@Transactional` on test class provides automatic rollback; do not clean up manually unless `BaseIntegrationTest.baseSetUp()` already deletes (it does: `membershipRepository.deleteAll()`, etc.).
- Test profile (`application-test.yml`) uses H2 — do not write tests that require real Postgres JSONB operators.
- The `accessToken` from `baseSetUp()` is valid for `testTenant`/`testUser`. For multi-tenant isolation tests, create a second tenant + user inline.

### Pagination response shape

Always use `PagedResponse<T>` builder with fields: `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize`, `hasNext`, `hasPrev`. Match this exactly — the frontend types depend on it.

### Text Search

Simple full-text search across entities via `GET /{entity}/search?term=...` endpoint.

**Backend** (`service/CustomerService.java`, `service/ContactService.java`, etc.):
- Each entity (Customer, Contact, Opportunity, Activity, CustomRecord) has a `GET /{entity}/search` endpoint
- Query params: `term` (optional), `page` (default 0), `size` (default 20)
- Searches across primary fields:
  - Customer: `name`
  - Contact: `name`, `email`
  - Opportunity: `name`
  - Activity: `subject`
  - CustomRecord: `name`
- Uses JPA `Specification<T>` with case-insensitive `LIKE` predicates (`cb.like(cb.lower(...))`)
- **Pagination**: database-side, correct `totalElements`/`totalPages`
- **Access control**: preserved on Opportunity, Activity, CustomRecord (hidden entity IDs filtered after query)
- No custom fields search (kept simple for frontend usability)

**Frontend** (`api/apiClient.ts`, list pages):
- `apiClient.searchXxx(term, page, size)` methods: `searchCustomers()`, `searchContacts()`, `searchOpportunities()`, `searchActivities()`, `searchCustomRecords()`
- List pages use the existing `AdvancedSearch` component (simple text input + filter dropdowns)
- No modal or complex state management—keeps the list page straightforward

**Example request**:
```
GET /customers/search?term=Acme&page=0&size=20
```

**Assistant integration** (`CrmTools.java`):
- `searchCustomers(query, status)`, `searchContacts(query, customerId)`, etc. call the service search methods
- Assistant can perform text searches inline without special wiring

### AI Assistant features

The assistant subsystem lives in `service/AssistantService.java`, `service/DashboardInsightService.java`, and `tools/DocumentGenerationTools.java`.

**Document generation** (`DocumentGenerationTools`):
- `generateSlideDeck(entityType, entityId, title, styleJson, templateId)` — produces HTML slides stored as a Document entity.
- `generateOnePager(entityType, entityId, title, styleJson, templateId)` — produces a Markdown one-pager stored as a Document entity.
- `generateCsvExport(entityType, filtersJson, templateId)` — produces CSV; does a two-pass scan to collect all unique custom field keys across records and writes them as `cf_<key>` columns.
- `templateId` (optional Long) — applies a saved DocumentTemplate's styleJson; if not provided, uses defaults from the tool's internal config.
- `styleJson` is an optional JSON string parsed at runtime. Supported keys:
  - `layout`: preset theme — `"dark"` (default), `"light"`, `"corporate"`, `"minimal"`
  - `accentColor`, `backgroundColor`, `slideBackground`, `textColor`, `h1Color`, `h2Color`: hex overrides
  - `includeFields` / `excludeFields`: arrays of field names to show/hide
  - For one-pagers: `includeCustomFields` (boolean), `layout` (`"compact"` | `"detailed"`)
- CSS is injected via `--slide-bg`, `--slide-accent`, `--slide-text`, `--slide-h1`, `--slide-h2` CSS variables in the slide HTML.

**Dashboard insight** (`DashboardInsightService`):
- Per-tenant in-memory cache: `ConcurrentHashMap<Long, DashboardInsight>` with 1-hour TTL.
- Two modes: new tenant (zero records) → onboarding message; active tenant → Clippy-style pipeline observation.
- **Provider rotation**: shuffles Anthropic/OpenAI/Gemini/Ollama on each generation, falls back to next on failure.
- **Models**: Anthropic `claude-haiku-4-5-20251001`, OpenAI `gpt-4o-mini`, Gemini `gemini-2.5-flash`, Ollama `llama3.2`/`mistral` (self-hosted, no API key).
- Smart news context for active tenants (Tavily first, NewsAPI fallback).
- Exposed via `GET /assistant/insight` and `POST /assistant/insight/refresh` in `AssistantSubscriptionController`.

**AssistantService dispatch**: when adding a new tool, update both the Spring AI `@Tool` method in `DocumentGenerationTools` AND the `switch` dispatch in `AssistantService.handleToolCall()`. The system prompt in `AssistantService.buildSystemPrompt()` documents available style options for the LLM.

**ChatModelRegistry**: resolves `ChatModel` by provider name (`"anthropic"`, `"openai"`, `"google"`, `"ollama"`). Used by both `AssistantService` and `DashboardInsightService` via `chatModelRegistry.getModel(tier.provider())`.

**Custom Field Types** (admin-configurable):
- **Standard**: text, number, date, boolean, select, textarea, multiselect, url, email, phone, currency, percentage, richtext
- **Linked**: document (single), document_multi, custom_record (single), custom_record_multi
- **Workflow**: milestone tracker with sequential steps
  - Config: `{"milestones": ["Lead", "Prospect", "Meeting", "Proposal", "Won"]}`
  - Value: `{"currentIndex": 2}` (active milestone index, 0-based) or `null` (not started)
- When adding new custom field type: add case to `CustomFieldInput.tsx` switch statement, update `FIELD_TYPES` in `CustomFieldsPage.tsx`, update assistant system prompt in `AssistantService.buildSystemPrompt()`, document in `CrmTools.getCustomFieldSchema()` if complex type.

**Document Templates** (`entity/DocumentTemplate.java`, `service/DocumentTemplateService.java`, `controller/DocumentTemplateController.java`):
- 4 default templates seeded on tenant creation via `TenantSchemaProvisioningService.seedDefaultDocumentTemplates()`.
- REST: `GET/POST/PUT/DELETE /document-templates`, `POST /document-templates/{id}/clone`.
- AI assistant calls `listDocumentTemplates()` before generating docs.

**Business Policy Rules** (`service/PolicyRuleService.java`, `tools/PolicyManagementTools.java`, `controller/PolicyRuleController.java`):
- Admin-only via AI assistant. CEL expressions: `entity.fieldName` (new values), `previous.fieldName` (old values on UPDATE).
- `DENY` → HTTP 422 hard block. `WARN` → confirmation modal, user can proceed.
- `PolicyValidationService.validate()` called in all 5 CRM services on CREATE/UPDATE/DELETE.
- Frontend: pre-submit evaluation via `usePolicyRulesStore.evaluateRules()` in all form pages.

**AI tier/model config** (`entity/EnabledAiModel.java`, `entity/AssistantTier.java`):
- System admin can enable/disable individual models per provider via `GET/PUT /admin/system/ai-models`.
- System admin can enable/disable tiers via `GET/PUT /admin/system/ai-tiers`.
- `GET /assistant/tiers` returns `{ tiers, models }` combined — accessible to tenant admins (no SYSTEM_ADMIN required).
- V8 migration: `enabled` column on `assistant_tiers`. V9 migration: `enabled_ai_models` table seeded with Anthropic/OpenAI/Google models. V22 migration: Ollama models seeded (`llama3.2`, `mistral`, `gemma3`).

### Email Notifications

Uses outbox pattern via `OutboxEvent`. `notificationService.notify*()` called in same transaction as write → `NotificationPoller` (60s) dispatches via `NotificationDispatchService` → writes `NotificationInbox` record + optional email.

**REST endpoints**: `GET /notifications/inbox`, `GET /notifications/inbox/unread-count`, `POST /notifications/inbox/{id}/read`, `POST /notifications/inbox/read-all`, `GET/PUT /notifications/preferences`.

**Testing**: test profile has empty `spring.mail.host` to skip mail. Leave `MAIL_HOST` empty to disable email in any environment.

### Row-Level Access Control

Granular ownership/visibility for `CustomRecord`, `Opportunity`, `Activity`, `TenantDocument`.

**Rules**:
- Always call `accessControlService.canView()` before returning an entity; `canWrite()` before mutations.
- List endpoints filter via `getHiddenEntityIds()` — never return hidden records unless user is owner or has explicit grant.
- `manager` role bypasses all access checks (independent of admin).
- Always include `ownerId` in entity DTOs — frontend needs it to show access control UI.

**REST endpoints** (`AccessControlController`): `GET/PUT/DELETE /access/{entityType}/{entityId}/policy`, `POST/DELETE /access/{entityType}/{entityId}/grants/{grantId}`.

**User groups** (`UserGroupController`): `GET/POST /groups`, `GET/PUT/DELETE /groups/{id}`, `GET/POST/DELETE /groups/{id}/members/{userId}`.

**API client methods** (`apiClient.ts`): `getGroups`, `createGroup`, `updateGroup`, `deleteGroup`, `getGroupMembers`, `addGroupMember`, `removeGroupMember`, `getAccessSummary`, `setAccessPolicy`, `removeAccessPolicy`, `addAccessGrant`, `removeAccessGrant`.

### Integration Framework (Track 4)

Real-time event distribution to external systems (Slack, HubSpot, Webhooks, Zapier) using the **outbox pattern** with **adapter architecture**.

**Core Services**:
- `EncryptionService`: AES-256-GCM credential encryption (key from `APP_ENCRYPTION_SECRET` env var)
- `IntegrationEventPublisher`: Publishes CRM events to OutboxEvent table (called from entity CRUD methods)
- `IntegrationEventRouter`: Routes events to enabled adapters, handles credential decryption
- `IntegrationPoller`: @Scheduled service (30s interval) that processes unpublished CRM_EVENT entries
- `IntegrationConfigService`: CRUD for tenant-scoped integration configurations

**Adapter Implementations**:
1. **SlackAdapter** — POST to webhook URL with formatted message
2. **WebhookAdapter** — Generic HTTP POST with optional HMAC-SHA256 signature
3. **HubSpotAdapter** — Sync Customer→Company, Contact, Opportunity→Deal to HubSpot API v3
4. **ZapierAdapter** — POST full event JSON to Zapier Catch Hook URL

**Event Flow**:
```
CRM Service.create/update/delete()
  → IntegrationEventPublisher.publish(tenantId, entityType, entityId, action, dto)
  → OutboxEvent{ eventType="CRM_EVENT", payloadJsonb={...} }
  → IntegrationPoller (every 30s)
  → IntegrationEventRouter.route(event)
  → findByTenantIdAndEnabled(true)
  → adapter.process(event, decryptedCredentials)
  → External System
  → event.publishedAt = now()
```

**Entity & Tables**:
- `IntegrationConfig` (tenant-scoped): adapterType, name, enabled, credentialsEncrypted, eventTypes
- V7 migration (tenant schema): integration_configs table
- V20 migration (admin schema): integration_definitions table (reference data)

**REST Endpoints** (`/api/integrations`):
- `GET /integrations` — list all configs for tenant
- `GET /integrations/{id}` — get single config
- `POST /integrations` — create config (credentials encrypted before storing)
- `PUT /integrations/{id}` — update config
- `DELETE /integrations/{id}` — delete config

**Admin UI** (`/admin/integrations`, `IntegrationsAdminPage.tsx`):
- 4 adapter cards for quick configuration
- Credential input fields (dynamically shown per adapter type)
- Event type subscription checkboxes (12 CRM events)
- Enable/disable toggle, edit, delete
- Active configuration list with status indicators

**CRM Event Emission**:
- Added to `CustomerService`, `ContactService`, `OpportunityService`, `ActivityService`
- Calls `integrationEventPublisher.publish()` after save on CREATE/UPDATE, before delete on DELETE
- Events: CUSTOMER_CREATED/UPDATED/DELETED, CONTACT_CREATED/UPDATED/DELETED, OPPORTUNITY_CREATED/UPDATED/DELETED, ACTIVITY_CREATED/UPDATED/DELETED

**Adding New Integrations**:
1. Create adapter class implementing `IntegrationAdapter` interface
2. Add to ADAPTERS array in `IntegrationsAdminPage.tsx`
3. Add credential field definitions
4. Implement `process(eventType, entityType, entityId, action, data, credentials)` method
5. Test with webhook.site or mock external API
6. Document in integration roadmap

**Security**:
- Credentials: AES-256-GCM encrypted at rest, decrypted on-demand during delivery
- Multi-tenancy: TenantContext enforces isolation
- Idempotency: External systems should handle duplicate deliveries
- No credential logging: Never log API keys or secrets

**Testing**:
- Integration tests: mock external systems, verify adapter.process() called correctly
- Manual testing: use webhook.site to capture POST payloads
- End-to-end: create config, trigger event, verify external system received payload

**Documentation**:
- Main: `/docs/integration-framework.md` (architecture, 4 adapters, API reference, security, troubleshooting)
- Roadmap: `/docs/integration-roadmap.md` (20 suggested future integrations, implementation guidelines, tier prioritization)
- API tests: `/backend/api-tests/integrations.http` (curl examples for all endpoints)

### What NOT to do

- Do not add `@Autowired` on fields — constructor injection only.
- Do not log passwords, tokens, or raw JWT payloads.
- Do not access admin-schema repos from tenant-scoped service methods.
- Do not call `TenantContext.set*()` anywhere outside `JwtAuthenticationFilter`.
- Do not use `findAll()` without a `Specification` that filters by `tenantId` — this is a data-leak bug.
- Do not add RabbitMQ producers/consumers without also adding an `OutboxEvent` row in the same transaction (outbox pattern).
- Do not break the `advancedSearch` response shape — clients parse `content[]` directly.
- Do not use `com.fasterxml.jackson.*` imports — this project uses `tools.jackson.*` (Spring Boot 4 rebrand). Will not compile.
- **Access control**: Do not skip visibility checks in services when fetching records — always call `accessControlService.canView()` before returning an entity. Do not return hidden records in list endpoints unless user is owner or has explicit grant.
- **Access control**: Do not allow non-owner/non-manager users to set policy or add grants — verify `canManage` flag on fetched summary before allowing mutations.
- **Access control**: Always include `ownerId` in entity DTOs when returning — frontend needs it to show access control UI correctly.

---

## Glossary

Definitions of key architectural terms used throughout BOCRM:

**TenantContext**: A ThreadLocal-based holder for `tenantId` and `userId` per HTTP request. Set by `JwtAuthenticationFilter` at request start, read by services and schema resolvers, cleared after the response. Critical: must always be cleared to avoid cross-tenant data leakage in thread pools.

**Schema-per-tenant**: Postgres isolation strategy where each tenant gets a dedicated schema named `tenant_<id>`. On every DB request, Hibernate issues `SET search_path = tenant_<id>` to route the connection. Eliminates application-layer filtering code; data isolation is enforced at the DB level.

**custom_data vs table_data_jsonb**: Two JSONB columns on CRM entities (Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice):
- `custom_data`: Full raw custom field values, used in detail view
- `table_data_jsonb`: Denormalized display fields extracted from custom_data, used in list view
Keep both in sync on every write using `ObjectNode.setAll()` merge pattern.

**Outbox pattern**: Async message reliability pattern. Entity writes create an `OutboxEvent` row in the same transaction. `NotificationPoller` (runs every 60s) picks up unprocessed events and dispatches them (notifications, integrations, webhooks). If dispatch fails, the event remains for retry. Guarantees at-least-once delivery without distributed transactions.

**CEL expression**: Common Expression Language — used for business policy rules (`PolicyRuleService`) and calculated fields (`CalculatedFieldDefinition`). Example: `entity.status == "closed" && entity.value > 100000`. Evaluated server-side at runtime; results cached.

**Access control policy/grant**: Row-level visibility mechanism:
- `RecordAccessPolicy`: per-record setting (OWNER_ONLY, TEAM, PUBLIC, CUSTOM)
- `RecordAccessGrant`: explicit grants to individual users or user groups (independent of policy)
- Checked before returning any record via `accessControlService.canView()`/`canWrite()`

**Confirmed tool call**: AI assistant pattern where the LLM proposes an action (create 10 customers) and waits for user confirmation before executing. Implemented via `confirm_mode` in Spring AI tool definitions. Prevents accidental bulk operations.

---

## Debugging Guide

### Common Pitfalls

**`com.fasterxml.jackson.*` import → compilation error**
- **Problem**: Old Jackson package name doesn't exist in this Spring Boot version
- **Fix**: Always use `import tools.jackson.*` (Spring Boot 4 rebrand)
- **Check**: Grep codebase: `grep -r "com.fasterxml.jackson" backend/src/main/java/` should find zero matches

**`TenantContext.getTenantId()` returns null → ForbiddenException**
- **In production**: JWT missing or invalid; check authentication flow
- **In tests**: Test class missing `@ActiveProfiles("test")`; the test profile uses H2 and does not apply schema routing
- **Fix**: Add `@ActiveProfiles("test")` to test class, extend `BaseIntegrationTest` to inherit setup

**JSONB operators (`@>`, `?`) fail in tests**
- **Problem**: Tests use H2 (in-memory) which doesn't support Postgres JSONB operators
- **Symptom**: `java.lang.IllegalArgumentException: Could not resolve entity name`
- **Fix**: Use Java-side filtering instead: deserialize JSONB to `JsonNode`, iterate, check fields with `.contains()` or `.get(field)`. See `CustomerService.matchesSearchTerm()` for pattern.

**Schema routing not working → querying wrong schema**
- **Symptom**: Tests pass, prod fails; or dev sees data from a different tenant
- **Root cause**: `TenantContext` not set (missing JWT filter or test not calling superclass setup)
- **Check**: Add log statement: `log.info("TenantContext: tenantId={}", TenantContext.getTenantId());` at service entry point
- **Fix**: Verify JWT is passed in request headers; ensure test extends `BaseIntegrationTest`

**Cache returning stale data across tenants**
- **Problem**: `@Cacheable` key doesn't include tenantId
- **Example**: `@Cacheable("customers")` caches `Customer(id=123)` for tenant A, then tenant B requests same ID
- **Fix**: All cache keys must include tenantId: `@Cacheable(value = "customers", key = "#tenantId + ':' + #id")`

**Custom field JSONB merge replaces entire object**
- **Problem**: Calling `entity.setCustomData(newNode)` instead of `customData.setAll(newNode)`
- **Result**: Overwrites existing custom fields, losing data
- **Fix**: Use merge pattern: `ObjectNode existing = (ObjectNode) entity.getCustomData(); existing.setAll(newNode);`

---

## Roadmap

BOCRM is planning three major feature tracks:

1. **Orders & Invoicing** — Full order and invoice management with line items, tax calculation, currency handling, due dates, and payment terms. Will follow the Opportunity/CustomRecord entity pattern exactly (raw FK columns, JSONB custom fields, access control, audit log).

2. **Integration Framework** — Vendor integrations (Slack, generic Webhooks, Zapier, HubSpot) via adapter pattern. Integrations consume `OutboxEvent` from the existing message bus, process events, and push data to external systems. Credentials stored encrypted. Admin UI to toggle and configure per-tenant.

3. **Advanced Search** — Full-field OpenSearch indexing for customers, contacts, opportunities, activities, orders, invoices. Support complex queries across custom fields and calculated fields. Replace current naive ILIKE + in-memory filtering with precision full-text search.

For detailed technical specs, see `.claude/plans/ancient-seeking-island.md`.

---

## Adding a New Entity: Access Control Checklist

When creating a new backend entity that supports row-level visibility (like Opportunity or CustomRecord), include these steps in your implementation:

- [ ] **Entity**: Include `ownerId` field (Long, not null)
- [ ] **Service list endpoint**: Call `accessControlService.getHiddenEntityIds(tenantId, userId, "EntityType")` and filter results
- [ ] **Service get endpoint**: Call `accessControlService.canView(entity, userId)` before returning; throw `ForbiddenException` if false
- [ ] **Service create endpoint**: Set `ownerId = userId` automatically
- [ ] **Service update/delete**: Call `accessControlService.canWrite(entity, userId)` before mutation
- [ ] **Controller list endpoint**: Include `ownerId` in DTO so frontend can render access control UI
- [ ] **Controller detail/create/update**: Pass `Authorization` header to service for access check
- [ ] **Migration**: Add `owner_id` column (NOT NULL, default `1` for data backfill, then remove default)
