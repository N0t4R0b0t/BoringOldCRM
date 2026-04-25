# Features

BOCRM is a multi-tenant CRM designed to be boring by default and powerful when needed. The following is a top-down tour of every major feature, starting with what makes the platform distinctive.

---

## What Makes BOCRM Unique

| # | Feature | One-liner |
|---|---------|-----------|
| 1 | [Multi-Tenant Schema Isolation](#1-multi-tenant-schema-isolation) | Every tenant gets its own Postgres schema — no RLS, no filter columns, zero cross-tenant data leakage |
| 2 | [AI Assistant with Tool Execution](#2-ai-assistant-with-tool-execution) | Always-on chat that reads and writes CRM data via structured tools, with audit logging on every action |
| 3 | [Tenant-Configurable Custom Fields](#3-tenant-configurable-custom-fields) | 14+ field types per entity, configured in the UI, stored as JSONB — no schema changes or deployments needed |
| 4 | [CEL Calculated Fields](#4-cel-calculated-fields) | Expression-based derived fields (e.g. `opportunity.value * probability`) with on-write evaluation and background refresh |
| 5 | [Business Policy Rules](#5-business-policy-rules) | Rego expressions (OPA) that DENY or WARN on CRM mutations — enforced server-side and pre-validated client-side |
| 6 | [Milestone Workflow Fields](#6-milestone-workflow-fields) | A custom field type that tracks sequential process steps with an animated stepper UI |
| 7 | [Row-Level Access Control](#7-row-level-access-control) | Per-record visibility modes (OPEN / READ_ONLY / HIDDEN) with user and group-level explicit grants |
| 8 | [AI Document Generation](#8-ai-document-generation) | Generate slide decks, one-pagers, and CSV exports from CRM data via chat, with style templates |
| 9 | [Opportunity Types](#9-opportunity-types) | Define multiple pipeline types (e.g. New Business vs Renewal) each with their own custom fields |
| 10 | [Outbox-Based Notifications](#10-outbox-based-notifications) | Guaranteed email + in-app notification delivery via the transactional outbox pattern |
| 11 | [Tenant Backup & Restore](#11-tenant-backup--restore) | On-demand schema backups downloadable as files, with restore from JSON payload |
| 12 | [Multi-Provider AI Routing](#12-multi-provider-ai-routing) | Anthropic, OpenAI, Gemini, and self-hosted Ollama with automatic per-request fallback and per-tenant model selection |
| 13 | [Dashboard AI Insight](#13-dashboard-ai-insight) | Clippy-style per-tenant pipeline observation enhanced with real-time news context |
| 14 | [OIDC / External Auth](#14-oidc--external-auth) | Plug-in SSO with org claim mapping to tenant memberships |
| 15 | [Multi-Tenant User Support](#15-multi-tenant-user-support) | One user account, multiple tenant memberships, instant in-session switching |

---

## 1. Multi-Tenant Schema Isolation

Every tenant gets a dedicated Postgres schema named `tenant_<id>`. Hibernate's `SCHEMA` multi-tenancy mode routes each request to the correct schema by reading the tenant ID from the JWT via a `TenantContext` ThreadLocal. There are no `tenant_id` filter columns in tenant-scoped queries — the wrong schema simply has no rows. Cross-tenant data leakage is structurally impossible.

**How it works:**

```
JWT → JwtAuthenticationFilter → TenantContext.setTenantId()
                                      ↓
                        CurrentTenantSchemaResolver
                                      ↓
                    SchemaPerTenantConnectionProvider
                      (SET search_path = tenant_42)
```

The shared `admin` schema holds tenants, users, and memberships and is always accessed directly. Flyway manages the admin schema migrations. A separate `TenantSchemaProvisioningService` runs per-tenant DDL on tenant creation.

---

## 2. AI Assistant with Tool Execution

An always-on chat panel is embedded in every page. The AI assistant (powered by Spring AI) has access to a full set of CRM tools:

| Tool | Description |
|------|-------------|
| `searchEntities` | Full-text and filter-based search across any entity type |
| `getEntity` | Fetch a single record by ID |
| `createEntity` | Create customers, contacts, opportunities, activities, custom records |
| `updateEntity` | Update any CRM record by ID |
| `addActivity` | Log a call, note, email, or task |
| `generateReport` | Run pipeline and summary reports |
| `generateSlideDeck` | Create an HTML slide deck from entity data |
| `generateOnePager` | Create a Markdown one-pager |
| `generateCsvExport` | Export entity data to CSV |
| `listDocumentTemplates` | Discover saved style templates |
| `createCustomField` | Define new custom fields including workflow type |
| `createPolicyRule` | Add a DENY/WARN business rule (admin only) |

**Confirm mode** allows users to review queued actions before execution. All AI tool calls are written to `audit_log` with before/after values. Context-aware chat is also available within entity detail views, scoped to that record.

---

## 3. Tenant-Configurable Custom Fields

Tenant admins define custom fields per entity type from the admin UI. No deployments or schema changes are required. Field values are stored as JSONB directly on the entity's `custom_data` column.

**Supported field types:**

| Category | Types |
|----------|-------|
| Text | `text`, `long_text`, `richtext`, `url`, `email`, `phone` |
| Numbers | `number`, `currency`, `percentage` |
| Temporal | `date`, `datetime` |
| Choices | `select`, `multiselect`, `checkbox` |
| Linked | `document` (single), `document_multi`, `custom_record` (single), `custom_record_multi` |
| Process | `workflow` (milestone tracker — see [Milestone Workflow Fields](#6-milestone-workflow-fields)) |

Dropdown and multi-select options are stored in `config_jsonb`. Fields have a configurable `display_order` and can be reordered via drag-and-drop. The frontend `CustomFieldInput` component polymorphically renders the correct input for each type.

---

## 4. CEL Calculated Fields

Calculated fields use [Common Expression Language (CEL)](https://cel.dev) to define expressions that derive values from core and custom fields.

**Example expressions:**
```
# Weighted deal value
opportunity.value * (opportunity.probability / 100.0)

# Days until close
int(opportunity.closeDate) - int(now)

# Workflow progress label
customField_sales_stage.currentIndex > 2 ? "Late Stage" : "Early Stage"
```

Values are evaluated on write and cached in `calculated_field_values`. A background refresh queue handles bulk recalculation. Calculated field results are returned inline in API responses alongside core fields.

---

## 5. Business Policy Rules

Policy rules use [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) with Rego expressions to enforce business logic on CRM mutations without touching application code. Rules are configured entirely in the admin UI or via the AI assistant. OPA runs as a sidecar and is kept in sync with the database on every rule change.

**Rule actions:**
- `DENY` — blocks the mutation with HTTP 422; the user sees a hard error
- `WARN` — shows a confirmation modal; the user can proceed or cancel

**Expression syntax:** Write just the Rego condition body — not a full policy document. Available inputs:
- `input.entity.*` — new/merged entity state (all operations)
- `input.previous.*` — state before the change (UPDATE only)
- `input.operation` — `"CREATE"`, `"UPDATE"`, or `"DELETE"`

Multiple conditions on separate lines are implicitly ANDed. For OR logic, create two separate rules.

**Example rules:**
```rego
# Prevent closing deals below minimum value
input.entity.stage == "Closed Won"
input.entity.value < 10000

# Warn when reassigning owned records
input.operation == "UPDATE"
input.entity.ownerId != input.previous.ownerId

# Block deleting active customers
input.entity.status == "active"
```

Rules are evaluated server-side on every CREATE/UPDATE/DELETE across all CRM services. The frontend pre-evaluates rules via `POST /policy-rules/evaluate` before form submission to surface warnings immediately. Expressions are validated against OPA at save time — invalid Rego is rejected with a clear error before reaching the database.

---

## 6. Milestone Workflow Fields

The `workflow` custom field type implements a sequential milestone tracker within the standard custom fields system.

**Configuration:**
```json
{
  "milestones": ["Lead", "Prospect", "Meeting", "Proposal", "Won"]
}
```

**Value format:**
```json
{ "currentIndex": 2 }
```

The frontend renders an animated stepper: completed milestones show a green checkmark, the active milestone pulses blue, and upcoming milestones are gray. Users click to advance, skip ahead, or revert. The AI assistant can create workflow fields and set their state via `updateCustomer({ customFields: { sales_stage: { currentIndex: 3 } } })`. Workflow progress can be referenced in calculated fields and policy rules (e.g. `input.entity.customField_sales_stage.currentIndex > 2`).

---

## 7. Row-Level Access Control

For sensitive entity types (Opportunity, CustomRecord, Activity, Document), record owners can restrict visibility beyond the default tenant-wide access.

**Access modes (set per record):**
- `OPEN` — visible to all tenant members (default)
- `READ_ONLY` — visible but not editable except by owner/manager
- `HIDDEN` — invisible to non-owners unless an explicit grant exists

**Grant types:**
- Grant a specific user READ or WRITE access
- Grant a named user group READ or WRITE access

`manager` role bypasses all access checks. All list endpoints filter hidden records via `getHiddenEntityIds()`. The `AccessControlPanel` component in the UI allows owners to manage policy and grants per record.

---

## 8. AI Document Generation

The AI assistant can generate documents from CRM data via three tools:

### Slide Decks
HTML slide decks with CSS variable theming. Preset layouts: `dark` (default), `light`, `corporate`, `minimal`. Individual hex color overrides for background, accent, headings, and text.

### One-Pagers
Markdown one-pagers with `compact` or `detailed` layout. Custom field inclusion/exclusion via `includeFields` / `excludeFields`.

### CSV Exports
Two-pass CSV generation: first pass collects all unique custom field keys across matching records; second pass writes rows with `cf_<key>` columns for every custom field. Supports the same filters as the list API.

### Document Templates
Saved style configurations can be applied by `templateId`. Four defaults are seeded on tenant creation (Corporate Dark, Light Minimal, Standard Report, Full Export). Admins can clone and customise templates from the admin UI with a live CSS preview panel.

---

## 9. Opportunity Types

Tenant admins can define multiple opportunity types (e.g. `new-business`, `renewal`, `upsell`), each with:
- A unique slug used in URL routing (`/opportunities/type/renewal`)
- A display name and description
- A distinct set of custom fields defined per type
- A configurable display order in the sidebar

The Kanban view and list view filter by type. The form dynamically loads the correct custom fields for the selected type.

---

## 10. Outbox-Based Notifications

Notifications are guaranteed by the transactional outbox pattern — the `OutboxEvent` row is written in the same transaction as the domain event that triggered it.

**Trigger events:**

| Event | Description |
|-------|-------------|
| `RECORD_MODIFIED` | A record you own was edited by someone else |
| `OWNERSHIP_ASSIGNED` | A record was assigned to you |
| `ACCESS_GRANTED` | Explicit access was granted to you |
| `ACTIVITY_DUE_SOON` | Daily 8 AM reminder for activities due in 24 hours |
| `DAILY_INSIGHT` | Optional AI-generated daily pipeline summary |

A scheduled poller (every 60 seconds) reads unpublished events, writes to `notification_inbox` (in-app), and sends email if SMTP is configured. Supports up to 3 retries on transient SMTP failures. Users manage preferences per event type at `/notifications/preferences`. The bell icon in the header shows a live unread count badge.

---

## 11. Tenant Backup & Restore

Tenant admins can create on-demand backups from the admin UI:
- **Schema backup** — exports the full tenant schema structure
- **Data backup** — exports all entity data as a JSON payload
- Backups are downloadable as files from the job list
- Restore creates a new job that applies the payload to the tenant schema

Jobs track status (`PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`) with timestamps. Old jobs can be deleted to manage storage.

---

## 12. Multi-Provider AI Routing

The `ChatModelRegistry` resolves the correct `ChatModel` bean by provider name. The system admin can enable or disable individual models per provider from the AI Config page.

**Supported providers and models:**
| Provider | Type | Models |
|----------|------|--------|
| Anthropic | Cloud | `claude-haiku-4-5-20251001`, `claude-sonnet-4-6`, `claude-opus-4-6` |
| OpenAI | Cloud | `gpt-4o-mini`, `gpt-4o`, `gpt-4-turbo` |
| Google | Cloud | `gemini-2.5-flash`, `gemini-2.5-pro` |
| Ollama | Self-hosted | `llama3.2`, `mistral` (enabled by default); `gemma3` (disabled); any locally pulled model |

Ollama requires a running Ollama server and no API key. Configure the base URL via `OLLAMA_BASE_URL` (default: `http://localhost:11434`). System admins can use "Load from API" in the AI Models tab to auto-discover all models pulled on the local Ollama instance.

**Provider fallback** (dashboard insight): providers are shuffled on each generation; if the first fails, the next is tried automatically. Ollama is included in the rotation when enabled. This is independent of per-tenant model selection for the chat assistant.

**AI tiers** define monthly token limits and pricing. System admins assign tiers per tenant. Token usage is tracked in `token_usage_ledger` and visible in the assistant subscription panel.

---

## 13. Dashboard AI Insight

An AI-generated observation card appears on the dashboard, refreshed per tenant at most once per hour (in-memory cache with TTL).

**Two modes:**
- **New tenant** (zero CRM records): onboarding suggestions — set up company profile, define custom fields, configure a theme, import data
- **Active tenant**: Clippy-style pipeline commentary — closing deals, stalled opportunities, patterns spotted

**Smart news context**: the insight for active tenants is augmented with real-time news. The query prioritises (1) the customer with the highest-value open opportunity, (2) the most recent activity subject, (3) top customer names. Tavily is tried first; NewsAPI is the fallback.

---

## 14. OIDC / External Auth

When `app.external-auth.*` is configured, the `/auth/external/login` endpoint accepts an ID token from any OIDC provider.

**Org claim mapping:**
- If the token includes `org_id` or `org_name`, it maps directly to a tenant (single-tenant path, skips selection)
- If no org claim and the user has multiple memberships, tenant selection is triggered

The frontend handles the OIDC callback at `/auth/callback`. Local email/password auth remains available alongside OIDC.

---

## 15. Multi-Tenant User Support

A single user account can belong to multiple tenants with different roles in each.

**Login flow:**
1. User logs in with email/password or OIDC
2. If only one active membership → JWT issued for that tenant immediately
3. If multiple memberships → `requiresTenantSelection: true` → redirect to `/select-tenant`
4. User picks a tenant → JWT issued with that tenant's role

**In-session switching**: the `TenantSwitcher` dropdown in the sidebar lists all memberships. Selecting one issues a new JWT and reloads the page. Role is always read from the target membership, never carried over from the previous session.

---

## Core CRM Entities

| Entity | Description |
|--------|-------------|
| **Customer** | The primary account — company or individual. Has contacts, opportunities, customRecords, and activities. |
| **Contact** | A person associated with a customer. Can be linked to multiple opportunities. |
| **Opportunity** | A sales deal or pipeline item. Has a stage, value, probability, close date, and type. |
| **Activity** | A task, call, note, email, or meeting. Can be linked to any entity type. |
| **Custom Record** | Physical or digital items tracked against custom records linked to customers and opportunities. |
| **Document** | Files uploaded or AI-generated, with entity linking and access control. |

All six entity types support custom fields, calculated fields, access control, audit logging, and full-text search.

---

## Admin Capabilities

| Feature | Tenant Admin | System Admin |
|---------|:---:|:---:|
| Custom field definitions | ✓ | — |
| Calculated field expressions | ✓ | — |
| Policy rules | ✓ | — |
| Opportunity types | ✓ | — |
| Document templates | ✓ | — |
| User/group management | ✓ | ✓ |
| Tenant settings and branding | ✓ | ✓ |
| AI tier selection | ✓ | ✓ |
| Backup & restore | ✓ | — |
| All tenant management | — | ✓ |
| AI model enable/disable | — | ✓ |
| Platform user status | — | ✓ |
| Assign tiers to all tenants | — | ✓ |
