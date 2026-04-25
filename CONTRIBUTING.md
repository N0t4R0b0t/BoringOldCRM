# Contributing to BOCRM

Thanks for your interest in contributing! BOCRM is a multi-tenant CRM built on Spring Boot 4 + React 19. This guide will get you from zero to a working dev environment and explain the core patterns you need to follow.

---

## Table of Contents

- [Local Setup](#local-setup)
- [How Multi-Tenancy Works](#how-multi-tenancy-works)
- [Code Style](#code-style)
- [Adding a New Backend Entity](#adding-a-new-backend-entity)
- [Adding a New Frontend Page](#adding-a-new-frontend-page)
- [Running Tests](#running-tests)
- [PR Checklist](#pr-checklist)

---

## Local Setup

**Prerequisites**: Java 21, Node 20, Docker

```bash
# 1. Start infrastructure (Postgres + RabbitMQ)
docker compose up -d

# 2. Copy config templates
cp backend/src/main/resources/application-example.yml \
   backend/src/main/resources/application-local.yml
cp frontend/.env.example frontend/.env.local

# 3. Start the backend (http://localhost:8080/api)
cd backend && ./gradlew bootRun

# 4. Start the frontend (http://localhost:5173)
cd frontend && npm install && npm run dev
```

**Demo login**: `demo@bocrm.com` / `demo123`

Install the pre-commit secret scanner (required before your first commit):

```bash
bash scripts/install-hooks.sh
```

---

## How Multi-Tenancy Works

Understanding this is essential before writing any backend code — it affects every service method and every database query.

**The three-sentence version**: Each tenant gets its own Postgres schema named `tenant_<id>`. When a request arrives, the JWT filter extracts `tenantId` from the token and stores it in `TenantContext` (a ThreadLocal). Hibernate's schema resolver reads from `TenantContext` on every query and issues `SET search_path = tenant_<id>` to route to the right schema.

**Practical rules for contributors**:

1. Every public service method must start with:
   ```java
   Long tenantId = TenantContext.getTenantId();
   if (tenantId == null) throw new ForbiddenException("Tenant context not set");
   ```

2. Never use `findAll()` without a `Specification` that filters by `tenantId` — this would leak data across tenants.

3. After loading an entity, verify ownership:
   ```java
   if (!entity.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
   ```

4. Never call `TenantContext.set*()` outside of `JwtAuthenticationFilter` — the filter owns that lifecycle.

5. The **admin schema** (`admin.*`) holds `Tenant`, `User`, and `TenantMembership` — always accessed directly, not via schema routing.

---

## Code Style

### Backend (Java)

- **Jackson**: always import `tools.jackson.*` — NOT `com.fasterxml.jackson.*`. Spring Boot 4 rebranded the package; the old import will not compile.
- **Dependency injection**: constructor injection only — no `@Autowired` on fields.
- **Logging**: `@Slf4j` from Lombok; never log passwords, tokens, or raw JWT payloads.
- **Entities**: use Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`; `@PrePersist`/`@PreUpdate` for timestamps.
- **Custom fields**: stored as JSONB in `custom_data` column; use `ObjectNode.setAll()` to merge on update — never replace the entire node wholesale.
- **Pagination**: all list endpoints use `PagedResponse<T>` builder with `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize`, `hasNext`, `hasPrev` — frontend types depend on this exact shape.

### Frontend (TypeScript)

- All API calls through `api/apiClient.ts` — never create a second Axios instance.
- Shared state belongs in a Zustand store in `src/store/` — not in component `useState`.
- New pages: create `src/pages/FooPage.tsx` and add the route in `App.tsx` inside `<ProtectedRoute>`.
- CSS: add to existing module files in `src/styles/` — no inline styles, no new CSS frameworks.
- Follow `CustomersPage.tsx` + `CustomerFormPage.tsx` as the canonical list + form templates.

---

## Adding a New Backend Entity

Follow this exact order — skipping steps causes subtle bugs:

1. **Entity** — `entity/Foo.java`
2. **Repository** — `repository/FooRepository.java`
3. **DTOs** — `dto/FooDTO.java`, `CreateFooRequest.java`, `UpdateFooRequest.java`
4. **Service** — `service/FooService.java` (tenant guard at top of every method, audit log on every write)
5. **Controller** — `controller/FooController.java` (constructor injection, `@Operation` on each endpoint)
6. **Migration** — `V{N}__add_foos.sql` (run `ls backend/src/main/resources/db/migration/` to find the next N)
7. **Test** — extend `BaseIntegrationTest`

If the entity needs row-level access control (like Opportunity or Asset), also wire `accessControlService.canView()`/`canWrite()` in the service and `getHiddenEntityIds()` in list endpoints.

Use the `/new-entity` Claude Code skill to scaffold this automatically.

---

## Adding a New Frontend Page

1. Create `src/pages/FooPage.tsx` (follow `CustomersPage.tsx` as template)
2. Add a route in `App.tsx` inside `<ProtectedRoute>`
3. Add API methods to `api/apiClient.ts`
4. Add state to the appropriate Zustand store if the data is shared

Use the `/new-page` Claude Code skill to scaffold this automatically.

---

## Running Tests

```bash
# All backend tests
cd backend && ./gradlew test

# Single test class
./gradlew test --tests "com.bocrm.backend.controller.CustomerControllerTest"

# Frontend lint
cd frontend && npm run lint

# Frontend build check
npm run build
```

Backend integration tests use H2 in-memory DB with `@ActiveProfiles("test")`. Do not write tests that depend on Postgres-specific JSONB operators — they will fail under H2.

---

## PR Checklist

Before opening a pull request, verify:

- [ ] New service methods start with `TenantContext.getTenantId()` null check
- [ ] No `findAll()` without a `tenantId` filter in the Specification
- [ ] `tools.jackson.*` imports (not `com.fasterxml.jackson.*`)
- [ ] Flyway migration file is idempotent (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`)
- [ ] New migration version is sequential (check existing files first)
- [ ] `./gradlew test` passes
- [ ] `npm run build` and `npm run lint` pass
- [ ] No secrets, passwords, or tokens in logs or committed files
- [ ] Access control checks wired if entity supports row-level visibility
- [ ] Audit log called on every write operation
