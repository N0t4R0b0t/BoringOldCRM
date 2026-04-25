# GitHub Copilot Instruction Set for BOCRM Repository

## Overview
This repository is a multi-tenant CRM system with a Java Spring Boot backend and a React + Vite frontend. It includes comprehensive API tests and documentation. Use these instructions to guide Copilot's code suggestions and completions for this project.

---

## General Guidelines
- Follow the modular monolith pattern for backend code and feature/component-based structure for frontend code.
- Use clear, descriptive names and maintain consistency with existing code and documentation.
- Write robust, secure, and maintainable code. Always handle errors and edge cases.
- Ensure all code is testable and, where possible, provide or update tests.
- Update documentation and READMEs for any new features or changes.
- **If you discover a helpful bit of information, pattern, or convention that is not present in these instructions, consider updating this COPILOT_INSTRUCTIONS.md file to help future contributors and agents.**

---

## Backend (Spring Boot, Gradle)
- Place new features in the appropriate module under `src/main/java/com/bocrm/backend/`.
- Use standard Spring Boot practices: controllers for HTTP endpoints, services for business logic, repositories for data access.
- Use DTOs for API input/output and entities for persistence.
- Respect schema-per-tenant isolation for multi-tenancy.
- Use Postgres JSONB for flexible/custom fields.
- Use RabbitMQ for async workflows and background jobs.
- Document all new endpoints (OpenAPI/Swagger if available).
- Add configuration to `config/` and utilities to `util/`.
- Follow planned modules: auth, tenancy, crm-core, activities, reporting, audit.

---

## Frontend (React, Vite, TypeScript)
- Place new components in `src/components/`, pages in `src/pages/`, and state in `src/store/`.
- Use Zustand for state management and Axios (via `api/apiClient.ts`) for HTTP requests.
- Protect routes using `ProtectedRoute.tsx` and manage authentication state in `authStore.ts`.
- Use and extend existing CSS modules for styling.
- For new entity types or forms, follow the patterns in `CustomerFormPage.tsx`, `CustomersPage.tsx`, etc.
- All API calls should use the base URL and JWT auth as configured.
- Keep UI responsive and accessible.

---

## API Tests
- Place new HTTP API tests in `backend/api-tests/` as `.http` files.
- Use the environment file (`http-client.env.json`) for variables like `baseUrl`, `demoEmail`, and `demoPassword`.
- Always run `auth.http` first to set tokens for subsequent tests.
- To run all tests in sequence, use the provided `run-all-api-tests.sh` script, which concatenates and runs all .http files in a single context.

---

## Documentation
- Update the relevant `README.md` files and `docs/` for any new features, modules, or architectural changes.
- Keep the C4 diagrams and architecture docs up to date.

---

## Dev Environment
- Use Node.js 20.x and npm 10.x for frontend development.
- Use Java 17+ for backend development.
- Use Postgres and RabbitMQ for local development, as described in `infra/`.
- Use the provided scripts and Docker Compose files for setup.

---

## Commit and PR Guidelines
- Write clear, descriptive commit messages.
- Reference issues or requirements where applicable.
- Ensure all code passes linting and tests before submitting a PR.

---

## Copilot-Specific Instructions
- Suggest code that matches the existing architecture, patterns, and naming conventions.
- When generating new files, place them in the correct directory and follow the established structure.
- For backend, prefer Spring Boot idioms and modularization.
- For frontend, use React functional components, hooks, and TypeScript best practices.
- For API tests, ensure variable context is preserved and follow the recommended execution order.
- Always check for and handle errors, especially in API and database interactions.
- When in doubt, refer to the main `README.md` and module-specific documentation.

---

_Last updated: March 1, 2026_
