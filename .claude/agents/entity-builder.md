# entity-builder Agent

**Purpose**: Full-stack agent for creating new backend entities following BOCRM's 7-step recipe plus frontend pages and access control wiring, all in one pass.

## When to use

- User requests a new entity (e.g., Order, Invoice, Document)
- You have clear domain model definition
- You want all 7 backend steps + frontend + tests created together
- You want access control wired from the start

## What it knows

- BOCRM entity recipe: Entity → Repo → DTO → Service → Controller → Migration → Test
- Pattern templates:
  - `Opportunity.java` and `OpportunityService.java` for opportunity-like entities
  - `Asset.java` and `AssetService.java` for asset-like entities
  - `CustomerService.java` for service patterns (tenant guard, cache, audit, search)
- Multi-tenancy: TenantContext guard at top of every service method, tenantId filter in all queries
- JSONB custom fields: `custom_data` + `table_data_jsonb` columns, merge pattern via `ObjectNode.setAll()`
- Access control: if entity needs visibility rules, wires `accessControlService.canView()`/`canWrite()` + `getHiddenEntityIds()`
- Frontend patterns: follow `CustomersPage.tsx` (list) + `CustomerFormPage.tsx` (form) templates
- Audit logging: `auditLogService.logAction()` on every write
- Notifications: `notificationService.notify*()` via outbox pattern

## Output

- 7 backend files: Entity, Repository, DTOs, Service, Controller, Migration, Test
- 4 frontend files: ListPage, FormPage, UpdatePage + API client methods
- All wired together, tested, documented
- Access control guard checks if applicable

## Example request

```
Create a new Order entity with:
- customerId (required), opportunityId (optional)
- status enum (DRAFT/CONFIRMED/SHIPPED/DELIVERED/CANCELLED)
- currency (ISO-4217), subtotal, taxAmount, totalAmount
- lineItems as JSONB array
- custom_data + table_data_jsonb columns
- ownerId for access control
- full CRUD endpoints
- custom field support
```
