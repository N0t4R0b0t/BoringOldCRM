Add a new backend entity to BOCRM following the exact 7-step recipe in CLAUDE.md.

Ask me for:
1. Entity name (e.g. `Invoice`)
2. Fields and their types
3. Whether it needs `custom_data` / `table_data_jsonb` JSONB columns
4. Whether it needs access control (canView/canWrite checks)

Then implement in this order — do NOT skip steps:
1. **Entity** — `entity/Foo.java`: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@PrePersist`/`@PreUpdate` for timestamps, `tenantId` field, `@JdbcTypeCode(SqlTypes.JSON)` for JSONB. Use `tools.jackson.*` not `com.fasterxml.jackson.*`.
2. **Repository** — `repository/FooRepository.java`: extend `JpaRepository<Foo, Long>` + `JpaSpecificationExecutor<Foo>`, add `findByTenantId(Long tenantId)`.
3. **DTOs** — `dto/FooDTO`, `CreateFooRequest`, `UpdateFooRequest` with Lombok `@Data @Builder`.
4. **Service** — `service/FooService.java`: `@Service @Slf4j`, constructor injection, start every method with `Long tenantId = TenantContext.getTenantId(); if (tenantId == null) throw new ForbiddenException(...)`, call `auditLogService.logAction(...)` on every write.
5. **Controller** — `controller/FooController.java`: `@RestController @RequestMapping("/foos") @Tag(name=...) @Slf4j`, constructor injection only, `@Operation(summary=...)` on each endpoint, `ResponseEntity<PagedResponse<FooDTO>>` for list endpoints.
6. **Migration** — check `ls backend/src/main/resources/db/migration/` for last version, create `V{N+1}__add_foos.sql` with `CREATE TABLE IF NOT EXISTS` and `ADD COLUMN IF NOT EXISTS`.
7. **Test** — extend `BaseIntegrationTest`, use `mockMvc` + `objectMapper`, pass `Authorization: Bearer {accessToken}`.

Template to follow: `CustomerService.java` (service patterns), `CustomerController.java` (controller patterns).
