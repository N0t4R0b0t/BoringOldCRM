# BOCRM

A boring, scalable, multi-tenant CRM. Schema-per-tenant Postgres isolation. Always-on AI assistant. Tenant-configurable fields, expressions, and business rules — all without deployments.

Backend: Java 21 · Spring Boot 4 · Gradle
Frontend: React 19 · Vite · TypeScript
Data: Postgres (JSONB) · RabbitMQ (outbox pattern)
AI: Anthropic · OpenAI · Gemini · Ollama (multi-provider with automatic fallback)

---

## Architecture in 60 Seconds

BOCRM isolates each tenant in a dedicated Postgres schema (`tenant_<id>`). When a request arrives:

1. **JWT filter** extracts `tenantId` + `userId` from the token
2. **TenantContext** stores them in a ThreadLocal
3. **Schema resolver** reads TenantContext and issues `SET search_path = tenant_<id>` to route the connection
4. **Service methods** load/query entities from the isolated schema via Hibernate

This schema-per-tenant design provides true data isolation with zero application-layer filtering code. The **admin schema** holds tenants, users, and memberships and is accessed directly (not routed).

Result: one codebase, unlimited tenants, bulletproof isolation.

---

## Documentation

| Doc | Description |
|-----|-------------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | **Start here** if contributing — local setup, code style, multi-tenancy rules |
| [docs/features.md](docs/features.md) | Feature-by-feature showcase — what makes this project unique |
| [docs/architecture.md](docs/architecture.md) | C4 diagrams (context → container → component), data model, security, deployment |
| [docs/api.md](docs/api.md) | Full REST API reference, grouped by domain |
| [docs/notifications.md](docs/notifications.md) | Email + in-app notification system — triggers, architecture, configuration |
| [docs/domain-model.md](docs/domain-model.md) | Entity overview and indexing notes |
| [docs/integration-framework.md](docs/integration-framework.md) | Vendor integrations (Slack, Webhook, Zapier, HubSpot) — architecture, adapter API, security |

---

## Quick Start

**Prerequisites:** BOCRM requires an Auth0 account (or any OIDC-compatible provider) for authentication. [Auth0 offers a free developer plan](https://auth0.com/signup) that covers local development and small deployments.

```bash
# 1. Create an Auth0 application and configure auth
#    Copy backend/src/main/resources/application-example.yml → application-local.yml
#    Copy frontend/.env.example → .env.local
#    Fill in your Auth0 tenant URL, client ID, and audience in both files

# 2. Start local infrastructure
docker compose up -d

# 3. Start the backend (http://localhost:8080/api)
cd backend && ./gradlew bootRun

# 4. Start the frontend (http://localhost:5173)
cd frontend && npm install && npm run dev
```

Infrastructure ports:
- Postgres: `localhost:5432` (db=`crm`, user=`crm`, pass=`crm`)
- RabbitMQ management UI: `http://localhost:15672`

---

## Other Commands

```bash
# Backend
./gradlew test                                          # Run all tests
./gradlew test --tests "com.bocrm.backend.controller.CustomerControllerTest"
./gradlew build                                        # Build JAR

# Frontend
npm run build     # Production build (tsc + vite)
npm run lint      # ESLint check

# Install pre-commit secret scanning (gitleaks)
bash scripts/install-hooks.sh
```

---

## Config Files

| File | Purpose |
|------|---------|
| `backend/src/main/resources/application-local.yml` | Local backend overrides (gitignored) |
| `backend/src/main/resources/application-example.yml` | Template — copy to `application-local.yml` |
| `frontend/.env.local` | Local frontend env (gitignored) |
| `frontend/.env.example` | Template — copy to `.env.local` |

Do not commit real secrets. Pre-commit gitleaks scanning is configured in `.githooks/pre-commit`. CI scanning runs on all pushes via `.github/workflows/secret-scan.yml`.

---

## Release Workflow

GitHub Actions (`.github/workflows/release.yml`) builds backend JAR + frontend bundle and packages `bocrm-<version>.tar.gz`:

| Trigger | Behaviour |
|---------|-----------|
| Tag push `v*` | Full GitHub release with generated release notes |
| Push to `master` | Replaces the single `snapshot` prerelease (old one deleted first) |
| Manual dispatch | Builds artifact only; no GitHub release; accepts optional `version` label |

All paths upload the archive as a workflow artifact (30-day retention). See [docs/architecture.md](docs/architecture.md#deployment) for the pull-based auto-deploy setup.

---

## Contributing

We welcome contributions! Before you start:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md) for setup, code style, and multi-tenancy rules
2. Read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — we're committed to an inclusive community

---

## License

This project is licensed under the **[GNU Affero General Public License v3.0](LICENSE)** (AGPL-3.0-or-later).

© 2026 Ricardo Salvador

You are free to use, modify, and distribute this software under the terms of the AGPL-3.0. The key condition: if you run a modified version as a network service (e.g. a hosted SaaS), you must make the corresponding source code available to your users under the same license.

See [LICENSE](LICENSE) for the full text or visit [gnu.org/licenses/agpl-3.0](https://www.gnu.org/licenses/agpl-3.0.html).
