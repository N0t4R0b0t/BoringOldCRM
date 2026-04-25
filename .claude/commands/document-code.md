# /document-code

Add comprehensive Javadoc to a Java file or TSDoc to a TypeScript file, following BOCRM's documentation standards.

## Ask me for:
1. File path (relative to repo root, e.g., `backend/src/main/java/com/bocrm/backend/entity/Customer.java`)
2. Optional: specific methods or sections to focus on (or "full file" for complete coverage)

## What I'll do:
1. **Read the file** and understand the logic, relationships, and non-obvious patterns
2. **Add Javadoc** (Java) or **TSDoc** (TypeScript) to:
   - Every public class (one-line summary + detailed description if needed)
   - Every public method (`@param`, `@return`, `@throws` tags where non-obvious)
   - Key private methods if they contain important logic
3. **Add inline `// WHY:` comments** for:
   - Architectural decisions that would surprise a contributor
   - Non-obvious side effects or constraints
   - Multi-tenancy guards and access control checks
4. **Preserve all logic** — documentation pass only, zero logic changes
5. **Run verification**: `./gradlew build` (Java) or `npm run build` (TypeScript) to confirm no errors introduced

## Example output:
```java
/**
 * Manages customers within a tenant schema.
 * Enforces multi-tenancy isolation via TenantContext and access control rules.
 */
@Service
@Slf4j
public class CustomerService {
  
  /**
   * Creates a new customer in the current tenant schema.
   * 
   * @param tenantId the tenant context ID (must match TenantContext)
   * @param request the create request with name, email, etc.
   * @return the created CustomerDTO with all fields set
   * @throws ForbiddenException if TenantContext is not set or null
   * @throws ValidationException if custom fields fail validation
   */
  public CustomerDTO createCustomer(Long tenantId, CreateCustomerRequest request) {
    Long contextTenantId = TenantContext.getTenantId();
    if (contextTenantId == null) throw new ForbiddenException("Tenant context not set");
    // WHY: Verify the caller's tenant context matches the request to prevent data leakage
    if (!contextTenantId.equals(tenantId)) throw new ForbiddenException("Access denied");
    ...
  }
}
```

## Notes:
- Don't document Lombok-generated getters/setters
- Don't change indentation or reformat existing code
- If a method already has Javadoc, enhance it (add missing `@param` tags, etc.)
- For TypeScript, use `/** ... */` JSDoc blocks and document callback parameters
