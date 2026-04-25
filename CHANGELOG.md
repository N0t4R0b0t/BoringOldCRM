# Changelog

All notable changes to BOCRM are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Documentation: Extensive Javadoc and TSDoc for core backend and frontend files
- Skills: new `/document-code`, `/add-assistant-tool` Claude Code skills for contributors
- Agent definitions: `doc-writer`, `entity-builder` specialized agents for common tasks
- CONTRIBUTING.md guide for open source contributors
- CODE_OF_CONDUCT.md (Contributor Covenant)
- CHANGELOG.md (this file)

### Improved
- README.md: Added Architecture in 60 seconds, Roadmap section, Contributing link
- CLAUDE.md: Added Glossary, Debugging guide, Roadmap, Access control checklist

## [1.0.0] - 2026-04-09

### Features
- Multi-tenant CRM with schema-per-tenant Postgres isolation
- Always-on AI assistant with multi-provider fallback (Anthropic, OpenAI, Gemini)
- Rich custom field support: standard types, linked documents/assets, workflow milestones
- Calculated fields using CEL expressions
- Document generation: slide decks, one-pagers, CSV exports with style customization
- Document templates: saved style configs with live preview and cloning
- Business policy rules: CEL-based DENY/WARN validation at API level + frontend
- File upload support for AI assistant: PDFs, images, CSV bulk import
- Row-level access control: record visibility policies, user groups, granular grants
- Full-text search across customer names, contact emails, opportunity names, activity subjects, asset names
- Notifications: email + in-app via outbox pattern with multi-tenant isolation
- OIDC external authentication with tenant/role mapping
- Multi-tenant user support: users can belong to multiple tenants with different roles
- Bulk operations: create/update/delete multiple records in one request
- Audit logging: track all entity mutations with user/timestamp
- Dashboard insight: AI-powered pipeline observations with smart news context

### Technical Stack
- **Backend**: Java 21, Spring Boot 4.0.2, Gradle, Postgres, RabbitMQ, Spring Data JPA, Spring AI
- **Frontend**: React 19, Vite, TypeScript, Zustand, Axios, TailwindCSS
- **Database**: Postgres with JSONB columns, Flyway migrations, schema-per-tenant architecture
- **AI**: Multi-provider ChatModel registry with automatic provider fallback
- **CI/CD**: GitHub Actions for release workflow (tag → release, master → snapshot, manual artifact build)
- **Deployment**: Pull-based auto-deploy on Proxmox LXC containers

### Documentation
- docs/architecture.md: C4 diagrams, multi-tenancy explained, security model
- docs/features.md: Feature showcase
- docs/api.md: Full REST API reference
- docs/notifications.md: Email + in-app notification system
- docs/domain-model.md: Entity overview and custom fields
- CLAUDE.md: Comprehensive working instructions for Claude Code
