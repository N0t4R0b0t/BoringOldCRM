# API Reference

All endpoints are served under the `/api` base path (configured as `server.servlet.context-path`).
Authentication is required on all endpoints except `/auth/login`, `/auth/refresh`, `/auth/external/login`, and `/auth/onboard`.
Pass the JWT as `Authorization: Bearer <token>` on every request.

**Base URL (local dev):** `http://localhost:8080/api`

**Pagination parameters** (where applicable):
`page` (default 0) · `size` (default 20, max 200) · `sortBy` · `sortOrder` (`asc` / `desc`)

**Standard paginated response shape:**
```json
{
  "content": [...],
  "totalElements": 142,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": true,
  "hasPrev": false
}
```

---

## Auth

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/login` | Login with email + password. Returns JWT access token, refresh token, and tenant context. |
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token. |
| `POST` | `/auth/switch-tenant` | Issue a new JWT for a different tenant the user belongs to. Body: `{ "tenantId": 42 }` |
| `POST` | `/auth/external/login` | Authenticate with an OIDC ID token. Body: `{ "idToken": "..." }` |
| `POST` | `/auth/onboard` | Create the first tenant for a new user. Body: `{ "tenantName": "Acme Corp" }` |

**Login response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "userId": 1,
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "tenantId": 42,
  "tenantName": "Acme Corp",
  "role": "admin",
  "requiresTenantSelection": false,
  "availableTenants": [{ "tenantId": 42, "tenantName": "Acme Corp", "role": "admin" }]
}
```

---

## Customers

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/customers` | List customers. Params: `search`, `status`, `page`, `size`, `sortBy`, `sortOrder` |
| `POST` | `/customers` | Create a customer. |
| `GET` | `/customers/{id}` | Get a customer by ID (includes custom fields and calculated fields). |
| `PUT` | `/customers/{id}` | Update a customer. |
| `DELETE` | `/customers/{id}` | Delete a customer. |
| `POST` | `/customers/search` | Advanced search with complex filter expressions. |

**Create/update request body:**
```json
{
  "name": "Acme Corp",
  "status": "active",
  "ownerId": 1,
  "industry": "Technology",
  "website": "https://acme.com",
  "notes": "Key account",
  "customFields": {
    "priority": "high",
    "onboarding_stage": { "currentIndex": 2 }
  }
}
```

---

## Contacts

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/contacts` | List contacts. Params: `search`, `customerId`, `status`, `page`, `size` |
| `POST` | `/contacts` | Create a contact. |
| `GET` | `/contacts/{id}` | Get a contact by ID. |
| `PUT` | `/contacts/{id}` | Update a contact. |
| `DELETE` | `/contacts/{id}` | Delete a contact. |
| `POST` | `/contacts/{id}/disassociate` | Remove the contact's association with its customer. |

---

## Opportunities

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/opportunities` | List opportunities. Params: `search`, `stage`, `status`, `customerId`, `typeSlug`, `page`, `size` |
| `POST` | `/opportunities` | Create an opportunity. |
| `GET` | `/opportunities/{id}` | Get an opportunity by ID. |
| `PUT` | `/opportunities/{id}` | Update an opportunity. |
| `DELETE` | `/opportunities/{id}` | Delete an opportunity. |
| `GET` | `/opportunities/{id}/contacts` | List contacts linked to an opportunity. |
| `POST` | `/opportunities/{id}/contacts/{contactId}` | Link a contact to an opportunity. |
| `DELETE` | `/opportunities/{id}/contacts/{contactId}` | Unlink a contact from an opportunity. |

**Create/update request body:**
```json
{
  "name": "Q3 Renewal",
  "customerId": 10,
  "stage": "Proposal",
  "status": "open",
  "value": 25000,
  "probability": 75,
  "closeDate": "2026-09-30",
  "ownerId": 1,
  "opportunityTypeSlug": "renewal",
  "customFields": { "contract_length": 12 }
}
```

---

## Activities

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/activities` | List activities. Params: `search`, `type`, `status`, `relatedType`, `relatedId`, `page`, `size` |
| `POST` | `/activities` | Create an activity. |
| `GET` | `/activities/{id}` | Get an activity by ID. |
| `PUT` | `/activities/{id}` | Update an activity. |
| `DELETE` | `/activities/{id}` | Delete an activity. |

**Activity types:** `task`, `call`, `email`, `meeting`, `note`

**Create/update request body:**
```json
{
  "subject": "Follow-up call",
  "type": "call",
  "description": "Discuss renewal terms",
  "dueAt": "2026-04-15T10:00:00",
  "status": "pending",
  "ownerId": 1,
  "relatedType": "opportunity",
  "relatedId": 55
}
```

---

## Custom Records

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/custom-records` | List customRecords. Params: `search`, `status`, `customerId`, `page`, `size` |
| `POST` | `/custom-records` | Create an customRecord. |
| `GET` | `/custom-records/{id}` | Get an customRecord by ID. |
| `PUT` | `/custom-records/{id}` | Update an customRecord. |
| `DELETE` | `/custom-records/{id}` | Delete an customRecord. |
| `POST` | `/custom-records/{id}/opportunities/{opportunityId}` | Link an customRecord to an opportunity. |
| `DELETE` | `/custom-records/{id}/opportunities/{opportunityId}` | Unlink an customRecord from an opportunity. |
| `GET` | `/opportunities/{opportunityId}/custom-records` | List all customRecords linked to an opportunity. |

---

## Documents

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/documents` | List documents. Params: `search`, `contentType`, `source`, `linkedEntityType`, `linkedEntityId`, `page`, `size` |
| `POST` | `/documents` | Create a document record (metadata only). |
| `POST` | `/documents/upload` | Upload a file as a document (`multipart/form-data`). |
| `GET` | `/documents/{id}` | Get document metadata. |
| `GET` | `/documents/{id}/download` | Download document content. |
| `PATCH` | `/documents/{id}/rename` | Rename a document. Body: `{ "name": "New Name" }` |
| `POST` | `/documents/{id}/duplicate` | Duplicate a document. |
| `DELETE` | `/documents/{id}` | Delete a document. |

**Document sources:** `upload`, `ai_generated`
**Content types:** `slide_deck`, `one_pager`, `csv`, `file`

---

## Custom Fields

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/custom-fields` | List custom field definitions. Params: `entityType` (required) |
| `POST` | `/custom-fields` | Create a custom field definition. |
| `PUT` | `/custom-fields/{id}` | Update a custom field definition. |
| `DELETE` | `/custom-fields/{id}` | Delete a custom field definition. |
| `POST` | `/custom-fields/reorder` | Reorder fields. Body: `{ "orderedIds": [3, 1, 2] }` |

**Create request body:**
```json
{
  "entityType": "customer",
  "key": "priority",
  "label": "Priority",
  "fieldType": "select",
  "configJsonb": { "options": ["low", "medium", "high"] },
  "required": false,
  "displayOrder": 1
}
```

**Supported `fieldType` values:** `text`, `long_text`, `richtext`, `url`, `email`, `phone`, `number`, `currency`, `percentage`, `date`, `datetime`, `checkbox`, `select`, `multiselect`, `document`, `document_multi`, `custom_record`, `custom_record_multi`, `workflow`

---

## Calculated Fields

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/calculated-fields/definitions` | List calculated field definitions. Params: `entityType` |
| `POST` | `/calculated-fields/definitions` | Create a calculated field definition. |
| `PUT` | `/calculated-fields/definitions/{id}` | Update a calculated field definition. |
| `DELETE` | `/calculated-fields/definitions/{id}` | Delete a calculated field definition. |
| `POST` | `/calculated-fields/definitions/reorder` | Reorder definitions. |
| `GET` | `/calculated-fields/evaluate` | Evaluate all calculated fields for an entity. Params: `entityType`, `entityId` |

**Create request body:**
```json
{
  "entityType": "opportunity",
  "key": "weighted_value",
  "label": "Weighted Value",
  "expression": "opportunity.value * (opportunity.probability / 100.0)",
  "displayOrder": 1
}
```

---

## Policy Rules

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/policy-rules/definitions` | List policy rules. Params: `entityType` |
| `POST` | `/policy-rules/definitions` | Create a policy rule. |
| `PUT` | `/policy-rules/definitions/{id}` | Update a policy rule. |
| `DELETE` | `/policy-rules/definitions/{id}` | Delete a policy rule. |
| `POST` | `/policy-rules/definitions/reorder` | Reorder rules. |
| `POST` | `/policy-rules/evaluate` | Evaluate rules against entity data (pre-submission client-side check). |

**Create request body:**
```json
{
  "entityType": "opportunity",
  "name": "Minimum deal size",
  "description": "Deals closed as Won must be at least $10,000",
  "expression": "entity.stage == \"Closed Won\" && entity.value < 10000",
  "action": "DENY",
  "displayOrder": 1
}
```

**`action` values:** `DENY` (HTTP 422, hard block), `WARN` (confirmation modal, user can proceed)

---

## Saved Filters

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/filters` | List saved filters for the current user. Params: `entityType` |
| `POST` | `/filters` | Save a filter configuration. |
| `GET` | `/filters/{id}` | Get a saved filter by ID. |
| `DELETE` | `/filters/{id}` | Delete a saved filter. |

---

## Timeline

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/timeline/{entityType}/{entityId}` | Get the activity timeline for any entity. Returns activities, audit entries, and system events in chronological order. |

---

## Bulk Operations

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/bulk/execute` | Execute a generic bulk operation. Body: `{ "operation": "...", "entityType": "...", "ids": [...], "data": {...} }` |
| `POST` | `/bulk/customers/update` | Bulk update customers. |
| `POST` | `/bulk/customers/delete` | Bulk delete customers. Body: `{ "ids": [1, 2, 3] }` |
| `POST` | `/bulk/contacts/update` | Bulk update contacts. |
| `POST` | `/bulk/contacts/delete` | Bulk delete contacts. |
| `POST` | `/bulk/opportunities/update` | Bulk update opportunities. |
| `POST` | `/bulk/opportunities/delete` | Bulk delete opportunities. |
| `POST` | `/bulk/activities/delete` | Bulk delete activities. |
| `POST` | `/bulk/custom-records/delete` | Bulk delete customRecords. |
| `POST` | `/bulk/documents/delete` | Bulk delete documents. |

---

## Access Control

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/access/{entityType}/{entityId}/summary` | Get the access summary for a record (policy mode, grants, canView, canWrite, canManage). |
| `PUT` | `/access/{entityType}/{entityId}/policy` | Set the access policy. Body: `{ "accessMode": "HIDDEN" }` |
| `DELETE` | `/access/{entityType}/{entityId}/policy` | Remove the access policy (revert to OPEN). |
| `POST` | `/access/{entityType}/{entityId}/grants` | Add an access grant. Body: `{ "granteeType": "USER", "granteeId": 5, "permission": "WRITE" }` |
| `DELETE` | `/access/{entityType}/{entityId}/grants/{grantId}` | Remove an access grant. |

**`accessMode` values:** `OPEN`, `READ_ONLY`, `HIDDEN`
**`granteeType` values:** `USER`, `GROUP`
**`permission` values:** `READ`, `WRITE`

---

## User Groups

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/groups` | List user groups in the current tenant. |
| `POST` | `/groups` | Create a user group. Body: `{ "name": "Sales Team", "description": "..." }` |
| `GET` | `/groups/{id}` | Get a group by ID. |
| `PUT` | `/groups/{id}` | Update a group. |
| `DELETE` | `/groups/{id}` | Delete a group. |
| `GET` | `/groups/{id}/members` | List members in a group. |
| `POST` | `/groups/{id}/members` | Add a user to a group. Body: `{ "userId": 3 }` |
| `DELETE` | `/groups/{id}/members/{userId}` | Remove a user from a group. |

---

## Notifications

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/notifications/inbox` | Get the user's notification inbox. Params: `read` (filter), `page`, `size` |
| `GET` | `/notifications/inbox/unread-count` | Get the unread notification count (for badge). |
| `POST` | `/notifications/inbox/{id}/read` | Mark a notification as read. |
| `POST` | `/notifications/inbox/read-all` | Mark all notifications as read. |
| `GET` | `/notifications/preferences` | Get the user's notification preferences. |
| `PUT` | `/notifications/preferences` | Update notification preferences. |
| `POST` | `/notifications/compose` | Send a custom notification to specific users (admin only). |

**Preferences body:**
```json
{
  "notifications": {
    "recordModified": true,
    "ownershipAssigned": true,
    "accessGranted": true,
    "activityDueSoon": true,
    "dailyInsight": false,
    "muted": false
  }
}
```

---

## Notification Templates

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/notification-templates` | List notification templates. Params: `page`, `size` |
| `GET` | `/notification-templates/{id}` | Get a template by ID. |
| `POST` | `/notification-templates` | Create a notification template. |
| `PUT` | `/notification-templates/{id}` | Update a notification template. |
| `DELETE` | `/notification-templates/{id}` | Delete a notification template. |
| `GET` | `/notification-templates/types` | List available notification types. |

---

## Document Templates

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/document-templates` | List document templates. Params: `search`, `templateType`, `page`, `size` |
| `GET` | `/document-templates/{id}` | Get a template by ID. |
| `POST` | `/document-templates` | Create a document template. |
| `PUT` | `/document-templates/{id}` | Update a document template. |
| `DELETE` | `/document-templates/{id}` | Delete a document template. |
| `POST` | `/document-templates/{id}/clone` | Clone a template. Body: `{ "name": "My Copy" }` |

**Template types:** `slide_deck`, `one_pager`, `csv`

**Style JSON keys:**
```json
{
  "layout": "dark",
  "accentColor": "#2563eb",
  "backgroundColor": "#0f172a",
  "slideBackground": "#1e293b",
  "textColor": "#f8fafc",
  "h1Color": "#60a5fa",
  "h2Color": "#93c5fd",
  "includeFields": ["name", "value", "stage"],
  "excludeFields": ["notes"]
}
```

---

## Opportunity Types

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/opportunity-types` | List all opportunity types in the current tenant. |
| `POST` | `/opportunity-types` | Create an opportunity type. Body: `{ "name": "Renewal", "description": "..." }` |
| `PUT` | `/opportunity-types/{id}` | Update an opportunity type. |
| `DELETE` | `/opportunity-types/{id}` | Delete an opportunity type. |

---

## Reporting

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/reports/summary` | Dashboard summary stats (totals for customers, contacts, opportunities, activities). |
| `POST` | `/reports/customers` | Filtered customers report. Body: filter object. |
| `POST` | `/reports/opportunities` | Filtered opportunities report. Body: filter object. |
| `GET` | `/reports/sales/overview` | Sales overview (total pipeline value, won/lost counts). |
| `GET` | `/reports/sales/by-stage` | Opportunity count and value grouped by stage. |
| `GET` | `/reports/sales/pipeline` | Full pipeline breakdown with velocity metrics. |
| `GET` | `/reports/sales/conversion` | Stage-to-stage conversion rates. |
| `GET` | `/reports/activities/by-type` | Activity count grouped by type. Params: `from`, `to` (date range) |
| `GET` | `/reports/activities/by-status` | Activity count grouped by status. |
| `GET` | `/reports/customers/by-status` | Customer count grouped by status. |
| `GET` | `/reports/opportunities/list` | Flat opportunity list with reporting fields. |

---

## Chat / AI Assistant

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/chat/message` | Send a message to the AI assistant. |
| `GET` | `/chat/history` | Get the global chat history for the current user. |
| `GET` | `/chat/{entityType}/{entityId}` | Get chat history scoped to a specific entity. |
| `POST` | `/chat/{entityType}/{entityId}` | Send a message scoped to a specific entity. |

**Send message request body:**
```json
{
  "message": "Create a follow-up task for Acme Corp due next Monday",
  "contextEntityType": "customer",
  "contextEntityId": 10,
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "confirmMode": true,
  "executeActions": false,
  "pageContext": "customers"
}
```

**Response includes:**
```json
{
  "reply": "I'll create a follow-up task for Acme Corp...",
  "pendingActions": [
    {
      "tool": "createActivity",
      "description": "Create task: Follow-up with Acme Corp",
      "params": { ... }
    }
  ],
  "sessionId": "550e8400-..."
}
```

Set `executeActions: true` (or `confirmMode: false`) to execute pending actions immediately.

---

## Assistant Subscription & AI Models

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/assistant/subscription` | Get the current tenant's AI subscription (tier, token usage). |
| `GET` | `/assistant/quota` | Get remaining token quota for the current period. |
| `GET` | `/assistant/tiers` | List available subscription tiers and enabled AI models. |
| `PUT` | `/assistant/tiers/{tierId}` | Update the AI model/provider for a tier (tenant admin). |
| `POST` | `/assistant/subscription/upgrade` | Upgrade the tenant's subscription tier. Body: `{ "tierName": "pro" }` |
| `GET` | `/assistant/insight` | Get the AI-generated dashboard insight for the current tenant. |
| `POST` | `/assistant/insight/refresh` | Force-refresh the dashboard insight (bypasses 1-hour cache). |
| `POST` | `/assistant/onboarding-suggestions` | Generate AI onboarding suggestions. Body: `{ "orgName": "Acme", "orgBio": "..." }` |

---

## User

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/user/team` | List all users and their roles in the current tenant. |
| `PUT` | `/user/preferences` | Update the current user's preferences (dark mode, notification settings, etc.). |

---

## Tenant Admin

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/tenants/current` | Get the current tenant's details. |
| `PUT` | `/admin/tenants/{id}` | Update a tenant. |
| `GET` | `/admin/tenants/{id}/settings` | Get tenant settings (branding, AI config, preferences). |
| `PUT` | `/admin/tenants/{id}/settings` | Update tenant settings. |
| `POST` | `/admin/tenants/current/logo` | Set tenant logo from a URL. Body: `{ "imageUrl": "https://..." }` |
| `POST` | `/admin/tenants/current/logo/upload` | Upload a tenant logo file (`multipart/form-data`). |

---

## Tenant Membership (Admin)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/tenants/{tenantId}/users` | List users in a specific tenant. |
| `POST` | `/admin/tenants/{tenantId}/users` | Add a user to a tenant. Body: `{ "email": "user@example.com", "role": "member" }` |
| `DELETE` | `/admin/tenants/{tenantId}/users/{userId}` | Remove a user from a tenant. |

**Role values:** `admin`, `member`, `manager`

---

## Tenant Backup

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/admin/backup/create-backup` | Create a backup job. Body: `{ "includesData": true, "label": "Pre-migration backup" }` |
| `POST` | `/admin/backup/create-restore` | Create a restore job. Body: `{ "payload": "..." }` |
| `GET` | `/admin/backup/jobs` | List all backup jobs for the current tenant. |
| `GET` | `/admin/backup/jobs/{id}` | Get details of a specific backup job. |
| `GET` | `/admin/backup/jobs/{id}/download` | Download the backup file for a completed job. |
| `DELETE` | `/admin/backup/jobs/{id}` | Delete a backup job record. |

**Job status values:** `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`

---

## System Admin

> Requires `ROLE_SYSTEM_ADMIN`. System admins can manage all tenants without being a member.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/system/stats` | Get platform-wide statistics (tenant count, user count, total records). |
| `GET` | `/admin/system/users` | List all users across the platform. Params: `search`, `status`, `page`, `size` |
| `GET` | `/admin/system/users/{userId}/memberships` | Get all tenant memberships for a user. |
| `PUT` | `/admin/system/users/{userId}/status` | Update a user's status. Body: `{ "status": "active" }` |
| `POST` | `/admin/system/users/invite` | Invite a new user to the platform. Body: `{ "email": "...", "displayName": "..." }` |
| `GET` | `/admin/system/tenants/{tenantId}/subscription` | Get the AI subscription for any tenant. |
| `PUT` | `/admin/system/tenants/{tenantId}/tier` | Change the AI tier for a specific tenant. Body: `{ "tierName": "pro" }` |
| `GET` | `/admin/system/ai-tiers` | List all AI subscription tiers. |
| `PUT` | `/admin/system/ai-tiers/{tierId}` | Update an AI tier (enable/disable, token limits, pricing). |
| `POST` | `/admin/system/ai-tiers/apply-all-tenants` | Apply a tier to all tenants. Body: `{ "tierName": "standard" }` |
| `GET` | `/admin/system/ai-models` | List all AI models and their enabled status. |
| `PUT` | `/admin/system/ai-models/{modelId}` | Enable or disable an AI model. Body: `{ "enabled": false }` |
| `GET` | `/admin/tenants` | List all tenants (paginated). |
| `GET` | `/admin/tenants/{id}` | Get a specific tenant. |
| `POST` | `/admin/tenants` | Create a new tenant. |
| `DELETE` | `/admin/tenants/{id}` | Delete a tenant. |
