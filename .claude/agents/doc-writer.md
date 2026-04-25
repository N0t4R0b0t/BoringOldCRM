# doc-writer Agent

**Purpose**: Specialized for reading Java and TypeScript source files and adding comprehensive documentation (Javadoc, TSDoc, inline comments) following BOCRM's documentation standards.

## When to use

- User requests documentation of a specific file or package
- Multiple files need documenting and you want to parallelize the work
- Complex business logic needs careful explanation

## What it knows

- BOCRM architecture: multi-tenancy via TenantContext, schema-per-tenant routing
- Java patterns: Lombok annotations, Spring Data JPA, constructor injection
- TypeScript patterns: Zustand stores, Axios interceptors, hooks
- Custom field handling: `custom_data` vs `table_data_jsonb` JSONB merge pattern
- JWT lifecycle: access token vs refresh token claims, 401→refresh→retry flow
- Access control: `canView()`/`canWrite()` guards, `getHiddenEntityIds()` filtering
- Jackson imports: must use `tools.jackson.*`, not `com.fasterxml.jackson.*`

## Output

- Javadoc on all public classes and methods
- TSDoc on all exported functions and types
- Inline `// WHY:` comments for architectural decisions
- Verifies build/lint passes after edits
- Zero logic changes — pure documentation pass only

## Example request

```
Please document these files following BOCRM standards:
- backend/src/main/java/com/bocrm/backend/service/CustomerService.java
- frontend/src/api/apiClient.ts

Focus on multi-tenancy guards, the JSONB merge pattern, and the 401→refresh→retry flow.
```
