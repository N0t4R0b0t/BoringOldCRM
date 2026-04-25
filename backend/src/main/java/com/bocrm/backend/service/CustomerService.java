/*
 * BoringOldCRM - Open-source multi-tenant CRM
 * Copyright (C) 2026 Ricardo Salvador
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Source: https://github.com/N0t4R0b0t/BoringOldCRM
 */
package com.bocrm.backend.service;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.*;
import com.bocrm.backend.integration.IntegrationEventPublisher;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import tools.jackson.core.type.TypeReference;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for customer entity CRUD and search.
 *
 * <p>This service demonstrates the canonical patterns used across all tenant-scoped CRM services
 * (Contact, Opportunity, Activity, CustomRecord, Order, Invoice, etc.). It shows:
 * <ul>
 *   <li><strong>Tenant guard</strong>: Every public method starts with TenantContext null check
 *   <li><strong>JSONB custom fields</strong>: merge pattern using ObjectNode.setAll()
 *   <li><strong>Two JSONB columns</strong>: customData (full) vs tableDataJsonb (denormalized)
 *   <li><strong>Policy validation</strong>: CEL-based DENY/WARN rules on CREATE/UPDATE/DELETE
 *   <li><strong>Audit logging</strong>: every write calls auditLogService.logAction()
 *   <li><strong>Cache strategy</strong>: @Cacheable on detail/list, @CacheEvict on write (clear all)
 *   <li><strong>Notifications</strong>: ownership assignment, record modification
 *   <li><strong>Calculated fields</strong>: enqueue async recalculation after write
 * </ul>
 *
 * <p><strong>Cache Design</strong>: {@code @CacheConfig(cacheNames = {"CustomerDetail", "CustomerTable"})}
 * enables two caches:
 * <ul>
 *   <li>CustomerDetail: detail views with full customData
 *   <li>CustomerTable: list views with tableDataJsonb
 * </ul>
 * On write (create/update/delete), we {@code @CacheEvict(allEntries=true)} both caches to avoid stale data.
 * In production, consider smarter cache invalidation (per-tenant, per-id) if cache misses become frequent.
 *
 * <p>Configured in {@link org.springframework.context.annotation.Configuration HibernateConfig} with
 * Spring Cache abstraction; default is local in-memory cache (ConcurrentHashMap).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
@CacheConfig(cacheNames = {"CustomerDetail", "CustomerTable"})
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final EntityCustomFieldRepository customFieldRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final PolicyValidationService policyValidationService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;

    /**
     * Construct CustomerService with all dependencies via constructor injection.
     *
     * @param customerRepository for CRUD operations
     * @param customFieldRepository for loading custom field definitions (unused but retained for consistency)
     * @param fieldDefinitionService for validating and sanitizing custom fields, extracting display data
     * @param calculatedFieldDefinitionService for looking up calculated field definitions
     * @param auditLogService for logging all mutations
     * @param notificationService for sending notifications (ownership assignment, record changes)
     * @param objectMapper for JSON manipulation (merge, conversion)
     * @param policyValidationService for evaluating CEL policy rules
     * @param calculationMessagePublisher for enqueueing calculated field recalculations
     */
    public CustomerService(CustomerRepository customerRepository, EntityCustomFieldRepository customFieldRepository,
                          CustomFieldDefinitionService fieldDefinitionService,
                          CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                          AuditLogService auditLogService,
                          NotificationService notificationService, ObjectMapper objectMapper,
                          PolicyValidationService policyValidationService,
                          CalculationMessagePublisher calculationMessagePublisher,
                          IntegrationEventPublisher integrationEventPublisher) {
        this.customerRepository = customerRepository;
        this.customFieldRepository = customFieldRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.policyValidationService = policyValidationService;
        this.calculationMessagePublisher = calculationMessagePublisher;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    /**
     * Create a new customer in the current tenant schema.
     *
     * <p><strong>Tenant Guard</strong>: Validates TenantContext is set at method start. Every service
     * method must start this way to prevent data leakage.
     *
     * <p><strong>Policy Validation</strong>: Flattens the new customer data (name, status, ownerId, custom fields)
     * into a context map and evaluates all admin-configured policy rules. Returns a list of WARN violations;
     * DENY violations throw an exception.
     *
     * <p><strong>Custom Field Handling</strong>:
     * <ol>
     *   <li>Validate and sanitize custom fields: {@code fieldDefinitionService.validateAndSanitizeCustomFields()}
     *   <li>Set full data in {@code customData} column
     *   <li>Extract display-relevant fields into {@code tableDataJsonb} for list views
     * </ol>
     *
     * <p><strong>Notifications & Audit</strong>:
     * <ul>
     *   <li>Log to audit trail: action=CREATE_CUSTOMER
     *   <li>If ownerId is set and differs from current user: notify the owner of assignment
     *   <li>Enqueue async calculated field recalculations
     * </ul>
     *
     * <p><strong>Cache Eviction</strong>: Clears both CustomerDetail and CustomerTable caches
     * (all entries) to prevent stale data.
     *
     * @param request the create request with name, status, ownerId, customFields
     * @return the created CustomerDTO with id and timestamps
     * @throws ForbiddenException if TenantContext is not set
     * @throws ValidationException if custom fields fail validation
     */
    @Transactional
    @CacheEvict(value = {"CustomerDetail", "CustomerTable"}, allEntries = true)
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", request.getName());
        createCtx.put("status", request.getStatus() != null ? request.getStatus() : "active");
        if (request.getOwnerId() != null) createCtx.put("ownerId", request.getOwnerId());
        if (request.getCustomFields() != null) flattenCustomFields(objectMapper.valueToTree(request.getCustomFields()), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Customer", "CREATE", createCtx, null);

        // Validate and sanitize custom fields
        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Customer", objectMapper.valueToTree(request.getCustomFields()));
        }

        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .ownerId(request.getOwnerId())
                .build();

        // Set JSONB custom_data directly on entity
        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            customer.setCustomData(sanitizedCustomFields);

            // Extract and set denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(
                    tenantId, "Customer",objectMapper.valueToTree(request.getCustomFields()));
            if (tableDataNode != null && tableDataNode.size() > 0) {
                customer.setTableDataJsonb(tableDataNode);
            } else {
                customer.setTableDataJsonb(objectMapper.createObjectNode());
            }
        } else {
            customer.setTableDataJsonb(objectMapper.createObjectNode());
        }

        Customer saved = customerRepository.save(customer);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_CUSTOMER", "Customer", saved.getId(), request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Customer", saved.getId(), "CREATED", toDTO(saved, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "Customer", saved.getId(), saved.getName());
        }

        CustomerDTO dto = toDTO(saved, tenantId);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    /**
     * Get a single customer by ID.
     *
     * <p><strong>Tenant Guard</strong>: Validates TenantContext and verifies the loaded customer
     * belongs to the current tenant. Critical: never return a record from a different tenant.
     *
     * <p><strong>Cache</strong>: Cached by customerId. Cache key does NOT include tenantId because
     * customer IDs are unique within tenant (auto-generated by schema-scoped table). If a customer
     * somehow exists in multiple tenants (data corruption), caching is bypassed for that ID.
     *
     * @param customerId the customer ID to fetch
     * @return the CustomerDTO with full customData (not denormalized tableDataJsonb)
     * @throws ForbiddenException if TenantContext is not set or customer belongs to different tenant
     * @throws ResourceNotFoundException if customer does not exist
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "CustomerDetail", key = "#customerId")
    public CustomerDTO getCustomer(Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // WHY: Verify the loaded customer belongs to the current tenant. This prevents data leakage
        // if a malicious actor guesses a customer ID from another tenant (even though schema isolation
        // should prevent the record from existing in this schema, defense in depth is safer).
        if (!customer.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(customer, tenantId);
    }

    /**
     * List customers in the current tenant with filtering, search, sorting, and pagination.
     *
     * <p><strong>Pagination</strong>: Uses {@link #buildPageable(int, int, String, String, Set)} to
     * enforce size cap (max 200 per page), validate sort field, and default to "createdAt" DESC.
     *
     * <p><strong>Specification-based Queries</strong>: JPA Specification allows composable WHERE clauses.
     * Base spec filters by tenantId; optional specs add status filters and custom field filters.
     * Text search (name + custom fields) is done post-query via {@link #matchesSearchTerm(CustomerDTO, String)}
     * because full-text search on JSONB is complex in JPA. TODO: Migrate to Elasticsearch for production.
     *
     * <p><strong>Two Filtering Passes</strong>:
     * <ol>
     *   <li>Database (Specification): tenantId (required), status (optional)
     *   <li>In-memory (Stream filter): search term, custom field filters (against tableDataJsonb)
     * </ol>
     *
     * <p><strong>JSONB Handling</strong>: Uses {@code tableDataJsonb} (denormalized) for faster rendering
     * of 100+ rows. Full {@code customData} is not loaded.
     *
     * <p><strong>Response Shape</strong>: {@link PagedResponse} includes totalElements (from Specification),
     * totalPages, currentPage, pageSize, hasNext, hasPrev.
     *
     * @param page zero-indexed page number (capped at 0 if negative)
     * @param size page size (capped at 1–200)
     * @param sortBy field name (must be in allowedSorts: name, status, createdAt, updatedAt)
     * @param sortOrder "asc" or "desc" (defaults to DESC if ambiguous)
     * @param search optional text search term (searches name, status, all custom fields)
     * @param status optional list of statuses to filter by
     * @param customFieldFilters optional map of custom field name → list of acceptable values
     * @return paginated list of CustomerDTOs with denormalized custom fields
     * @throws ForbiddenException if TenantContext is not set
     */
    @Transactional(readOnly = true)
    public PagedResponse<CustomerDTO> listCustomers(int page, int size, String sortBy, String sortOrder,
                                                   String search, List<String> status, Map<String, List<String>> customFieldFilters) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // WHY: buildPageable enforces size cap (max 200) to prevent memory exhaustion attacks.
        // Default sort is "createdAt" to ensure deterministic ordering.
        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("name", "status", "createdAt", "updatedAt"));

        // WHY: Specification composes JPA WHERE clauses. Start with tenantId filter (mandatory);
        // optionally add status and custom field filters.
        Specification<Customer> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (status != null && !status.isEmpty()) {
            List<String> lower = status.stream().map(String::toLowerCase).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("status").in(lower));
        }

        // NOTE: Text search on custom fields is done via post-query filtering in Java.
        // For production, migrate this to Elasticsearch to enable full-text search across all fields
        // with proper indexing and performance. See tableDataJsonb column for denormalized custom field data.
        Page<Customer> result = customerRepository.findAll(spec, pageable);
        List<CustomerDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .filter(dto -> matchesSearchTerm(dto, search))
                .filter(dto -> matchesCustomFieldFilters(dto, customFieldFilters))
                .collect(Collectors.toList());

        return PagedResponse.<CustomerDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    private boolean matchesCustomFieldFilters(CustomerDTO dto, Map<String, List<String>> cfFilters) {
        if (cfFilters == null || cfFilters.isEmpty()) return true;
        for (Map.Entry<String, List<String>> entry : cfFilters.entrySet()) {
            Object fieldVal = dto.getCustomFields() != null ? dto.getCustomFields().get(entry.getKey()) : null;
            if (fieldVal == null) return false;
            String strVal = String.valueOf(fieldVal).toLowerCase();
            boolean matches = entry.getValue().stream()
                    .anyMatch(v -> v.toLowerCase().equals(strVal));
            if (!matches) return false;
        }
        return true;
    }

    /**
     * Filter a DTO by search term across standard and custom fields.
     *
     * <p>Performs case-insensitive substring matching against:
     * <ul>
     *   <li>name (standard field)
     *   <li>status (standard field)
     *   <li>all custom fields in tableDataJsonb (denormalized)
     * </ul>
     *
     * <p><strong>Performance Note</strong>: This is in-memory filtering done in Java after fetching
     * from the database. For production with large result sets, migrate to Elasticsearch for indexed
     * full-text search. See TODO comment in listCustomers().
     *
     * @param dto the DTO to test (should contain tableDataJsonb, not full customData)
     * @param searchTerm the search string (null or blank matches all)
     * @return true if the DTO matches the search term, false otherwise
     */
    private boolean matchesSearchTerm(CustomerDTO dto, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) return true;

        String search = searchTerm.toLowerCase();

        // Search standard fields
        if (dto.getName() != null && dto.getName().toLowerCase().contains(search)) return true;
        if (dto.getStatus() != null && dto.getStatus().toLowerCase().contains(search)) return true;

        // Search denormalized custom fields (tableDataJsonb)
        if (dto.getCustomFields() != null) {
            for (Object value : dto.getCustomFields().values()) {
                if (value != null && String.valueOf(value).toLowerCase().contains(search)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Update an existing customer.
     *
     * <p><strong>Policy Validation</strong>: Builds two context maps:
     * <ul>
     *   <li>previousCtx: current values (before update)
     *   <li>mergedCtx: values after applying request (for "what if" evaluation)
     * </ul>
     * Passes both to {@code policyValidationService.validate()}. If a DENY rule would be violated,
     * an exception is thrown immediately. WARN violations are returned and included in the response DTO.
     *
     * <p><strong>JSONB Merge Pattern (Critical)</strong>:
     * <ol>
     *   <li>Validate new custom fields: {@code fieldDefinitionService.validateAndSanitizeCustomFields()}
     *   <li>Create empty ObjectNode: {@code objectMapper.createObjectNode()}
     *   <li>Copy existing: {@code mergedNode.setAll((ObjectNode) customer.getCustomData())}
     *   <li>Copy new: {@code mergedNode.setAll((ObjectNode) sanitizedCustomFields)}
     *   <li>Result: old fields remain; new fields override; omitted fields are untouched
     * </ol>
     * WHY this pattern? Clients often send partial updates (only changed fields). We must preserve
     * unchanged fields. Using {@code customer.setCustomData(newNode)} would DELETE omitted fields.
     *
     * <p><strong>Denormalized JSONB</strong>: After merging, recalculate {@code tableDataJsonb}
     * to keep display fields in sync. This is expensive but necessary because custom field definitions
     * may have changed (e.g., a field marked "displayInTable=true" is now false).
     *
     * <p><strong>Ownership Notification</strong>: If ownerId changed and the new owner is not the current
     * user, send a notification to the new owner. If ownerId didn't change but is not the current user,
     * notify the owner that the record was modified.
     *
     * <p><strong>Cache Eviction</strong>: Clears both CustomerDetail and CustomerTable caches.
     *
     * @param customerId the customer ID to update
     * @param request partial update request (null fields are skipped)
     * @return the updated CustomerDTO with policy warnings (if any)
     * @throws ForbiddenException if TenantContext not set or customer belongs to different tenant
     * @throws ResourceNotFoundException if customer does not exist
     * @throws ValidationException if custom fields fail validation or a DENY policy is violated
     */
    @Transactional
    @CacheEvict(value = {"CustomerDetail", "CustomerTable"}, allEntries = true)
    public CustomerDTO updateCustomer(Long customerId, UpdateCustomerRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!customer.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long previousOwnerId = customer.getOwnerId();

        // WHY: Policy validation needs both "before" and "after" snapshots to evaluate rules like
        // "if status changes from X to Y, warn the user" or "field can only change by owner".
        Map<String, Object> previousCtx = new HashMap<>();
        previousCtx.put("name", customer.getName());
        previousCtx.put("status", customer.getStatus());
        if (customer.getOwnerId() != null) previousCtx.put("ownerId", customer.getOwnerId());
        flattenCustomFields(customer.getCustomData(), previousCtx);

        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getName() != null) mergedCtx.put("name", request.getName());
        if (request.getStatus() != null) mergedCtx.put("status", request.getStatus());
        if (request.getOwnerId() != null) mergedCtx.put("ownerId", request.getOwnerId());
        flattenCustomFields(request.getCustomFields(), mergedCtx);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Customer", "UPDATE", mergedCtx, previousCtx);

        if (request.getName() != null) customer.setName(request.getName());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());
        if (request.getOwnerId() != null) customer.setOwnerId(request.getOwnerId());

        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            JsonNode sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Customer", request.getCustomFields());

            // WHY: The JSONB merge pattern. NEVER do customer.setCustomData(newNode) because that
            // replaces the entire object, losing any custom fields not in the request. Instead:
            // 1. Copy existing fields into a new object
            // 2. Copy new fields into the same object (overwrites old values with same key)
            // 3. Result: union of old + new, with new values taking precedence
            ObjectNode mergedNode = objectMapper.createObjectNode();
            if (customer.getCustomData() != null && customer.getCustomData().isObject()) {
                mergedNode.setAll((ObjectNode) customer.getCustomData());
            }
            if (sanitizedCustomFields.isObject()) {
                mergedNode.setAll((ObjectNode) sanitizedCustomFields);
            }

            customer.setCustomData(mergedNode);

            // WHY: Recalculate tableDataJsonb after every custom data change because:
            // 1. Custom field definitions may have changed (displayInTable flag)
            // 2. Formatted/computed display values may differ from raw values
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Customer", mergedNode);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                customer.setTableDataJsonb(tableDataNode);
            } else {
                customer.setTableDataJsonb(objectMapper.createObjectNode());
            }
        }

        Customer updated = customerRepository.save(customer);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_CUSTOMER", "Customer", customerId, request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Customer", updated.getId(), "UPDATED", toDTO(updated, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "Customer", updated.getId(), updated.getName());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "Customer", updated.getId(), updated.getName());
            }
        }

        CustomerDTO dto = toDTO(updated, tenantId);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    /**
     * Delete a customer from the current tenant.
     *
     * <p><strong>Policy Validation</strong>: Evaluates DELETE rules (e.g., "only owner can delete").
     * If a DENY rule is violated, throws an exception. WARN rules are not returned (delete is final).
     *
     * <p><strong>Cascade</strong>: This delete does NOT cascade to related records (contacts, opportunities, activities).
     * Foreign key constraints in the schema enforce referential integrity. Clients must delete related records first
     * or the DB will reject the deletion.
     *
     * <p><strong>Audit & Cache</strong>: Logs the deletion to audit trail. Clears both customer caches.
     *
     * @param customerId the customer ID to delete
     * @throws ForbiddenException if TenantContext not set or customer belongs to different tenant
     * @throws ResourceNotFoundException if customer does not exist
     * @throws ValidationException if a DENY policy is violated
     * @throws DataIntegrityViolationException if deletion violates foreign key constraints
     */
    @Transactional
    @CacheEvict(value = {"CustomerDetail", "CustomerTable"}, allEntries = true)
    public void deleteCustomer(Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!customer.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Map<String, Object> deleteCtx = new HashMap<>();
        deleteCtx.put("name", customer.getName());
        deleteCtx.put("status", customer.getStatus());
        if (customer.getOwnerId() != null) deleteCtx.put("ownerId", customer.getOwnerId());
        flattenCustomFields(customer.getCustomData(), deleteCtx);
        policyValidationService.validate(tenantId, "Customer", "DELETE", deleteCtx, null);

        // Publish integration event before deletion
        integrationEventPublisher.publish(tenantId, "Customer", customerId, "DELETED", toDTO(customer, tenantId));

        customerRepository.delete(customer);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_CUSTOMER", "Customer", customerId, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, "createdAt", "desc",
                Set.of("name", "status", "createdAt", "updatedAt"));

        // Simple text search across name field with ILIKE
        Specification<Customer> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchPred = cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%");
            return cb.and(tenantPred, searchPred);
        };

        Page<Customer> result = customerRepository.findAll(spec, pageable);
        List<CustomerDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<CustomerDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    /**
     * Flatten JSONB custom fields into a policy context map.
     *
     * <p>Custom fields are stored as JSON. Policy rules need to reference them with expressions like
     * {@code entity.hype_level == "Low"}. This method deserializes the JSONB into a flat Map so policy
     * rule evaluators can access them.
     *
     * <p>Example:
     * <pre>
     *   customData = {"hype_level": "Low", "annual_revenue": 5000000}
     *   After flattenCustomFields: ctx = {..., "hype_level": "Low", "annual_revenue": 5000000}
     * </pre>
     *
     * @param customData the JSONB custom fields node (may be null)
     * @param ctx the destination map to which flattened fields are added (keys may be overwritten)
     */
    @SuppressWarnings("unchecked")
    private void flattenCustomFields(JsonNode customData, Map<String, Object> ctx) {
        if (customData == null || !customData.isObject()) return;
        Map<String, Object> flat = objectMapper.convertValue(customData, Map.class);
        if (flat != null) ctx.putAll(flat);
    }

    /**
     * Build a Pageable with safety checks on size, sort field, and defaults.
     *
     * <p><strong>Safety</strong>:
     * <ul>
     *   <li>page: clamped to 0 if negative
     *   <li>size: clamped to 1–200 (default 20 if out of range)
     *   <li>sortBy: validated against allowedSorts; defaults to "createdAt" if invalid
     *   <li>sortOrder: defaults to DESC if ambiguous
     * </ul>
     *
     * <p>WHY these caps? size=200 prevents memory exhaustion from large page requests.
     * Validated sortBy prevents SQL injection (though Specifications should already prevent this).
     * Deterministic default sort ensures consistent pagination.
     *
     * @param page requested page (0-indexed)
     * @param size requested page size
     * @param sortBy requested sort field
     * @param sortOrder "asc" or "desc"
     * @param allowedSorts set of valid sort field names
     * @return a safe Pageable for use with Spring Data JPA
     */
    private Pageable buildPageable(int page, int size, String sortBy, String sortOrder, Set<String> allowedSorts) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
    }

    /**
     * Convert a Customer entity to a DTO for detail views.
     *
     * <p>Uses {@code customData} (the full, raw JSONB column) so detail views can render all custom fields
     * with full fidelity. No caching or filtering here—that's the caller's responsibility.
     *
     * @param customer the entity to convert
     * @param tenantId the tenant context (used for lookups if needed; currently unused but preserved for consistency)
     * @return a CustomerDTO with full customData
     */
    private CustomerDTO toDTO(Customer customer, Long tenantId) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .status(customer.getStatus())
                .ownerId(customer.getOwnerId())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .customFields(customer.getCustomData())  // WHY: Full data for detail views
                .build();
    }

    /**
     * Convert a Customer entity to a DTO for table/list views.
     *
     * <p>Uses {@code tableDataJsonb} (denormalized, display-focused JSONB column) for performance.
     * Only includes custom fields marked with {@code displayInTable=true} and may include computed
     * display values (e.g., "sales_stage" → "Proposal" instead of index number).
     *
     * <p>Caller is responsible for filtering the DTO list by search term and custom field filters.
     *
     * @param customer the entity to convert
     * @return a CustomerDTO with denormalized tableDataJsonb
     */
    private CustomerDTO toDTOTableView(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .status(customer.getStatus())
                .ownerId(customer.getOwnerId())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .customFields(customer.getTableDataJsonb())  // WHY: Denormalized fields for list view performance
                .build();
    }

    /**
     * Enqueue async recalculation of calculated fields for a customer.
     *
     * <p>Calculated fields are defined by CEL expressions that reference the customer's data
     * (e.g., {@code ANNUAL_REVENUE > 1000000 ? "Enterprise" : "SMB"}). When a customer's data
     * changes, these expressions must be re-evaluated. Rather than blocking the write request,
     * we publish a message to a RabbitMQ queue. An async worker picks it up and recalculates,
     * storing results in the {@code CalculatedFieldValue} table.
     *
     * <p>Called after every CREATE and UPDATE to ensure calculated field values stay fresh.
     *
     * @param tenantId the tenant context (needed by the async worker)
     * @param customerId the customer whose calculated fields need recalculation
     */
    private void enqueueCalculatedFieldRefresh(Long tenantId, Long customerId) {
        calculationMessagePublisher.publish(tenantId, "Customer", customerId);
    }
}
