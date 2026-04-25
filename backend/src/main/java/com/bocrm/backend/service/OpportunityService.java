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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;
/**
 * OpportunityService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"OpportunityDetail", "OpportunityTable"})
public class OpportunityService {
    private final OpportunityRepository opportunityRepository;
    private final EntityCustomFieldRepository customFieldRepository;
    private final CalculatedFieldValueRepository calculatedFieldValueRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final OpportunityTypeRepository opportunityTypeRepository;
    private final AccessControlService accessControlService;
    private final PolicyValidationService policyValidationService;
    private final JdbcTemplate jdbcTemplate;
    private final ContactRepository contactRepository;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;
    public OpportunityService(OpportunityRepository opportunityRepository, EntityCustomFieldRepository customFieldRepository,
                             CalculatedFieldValueRepository calculatedFieldValueRepository,
                             CustomFieldDefinitionService fieldDefinitionService,
                             CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                             AuditLogService auditLogService,
                             NotificationService notificationService, ObjectMapper objectMapper, OpportunityTypeRepository opportunityTypeRepository,
                             AccessControlService accessControlService, PolicyValidationService policyValidationService,
                             JdbcTemplate jdbcTemplate, ContactRepository contactRepository,
                             CalculationMessagePublisher calculationMessagePublisher,
                             IntegrationEventPublisher integrationEventPublisher) {
        this.opportunityRepository = opportunityRepository;
        this.customFieldRepository = customFieldRepository;
        this.calculatedFieldValueRepository = calculatedFieldValueRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.opportunityTypeRepository = opportunityTypeRepository;
        this.accessControlService = accessControlService;
        this.policyValidationService = policyValidationService;
        this.jdbcTemplate = jdbcTemplate;
        this.contactRepository = contactRepository;
        this.calculationMessagePublisher = calculationMessagePublisher;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "user";
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("user");
    }

    /** Flattens a JSONB custom fields node into a policy context map, so expressions like
     *  {@code entity.hype_level == "Low"} work alongside standard fields. */
    @SuppressWarnings("unchecked")
    private void flattenCustomFields(JsonNode customData, Map<String, Object> ctx) {
        if (customData == null || !customData.isObject()) return;
        Map<String, Object> flat = objectMapper.convertValue(customData, Map.class);
        if (flat != null) ctx.putAll(flat);
    }

    @Transactional
    @CacheEvict(value = {"OpportunityDetail", "OpportunityTable"}, allEntries = true)
    public OpportunityDTO createOpportunity(CreateOpportunityRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", request.getName());
        createCtx.put("stage", request.getStage() != null ? request.getStage() : "prospecting");
        if (request.getValue() != null) createCtx.put("value", request.getValue());
        if (request.getOwnerId() != null) createCtx.put("ownerId", request.getOwnerId());
        if (request.getCustomerId() != null) createCtx.put("customerId", request.getCustomerId());
        flattenCustomFields(request.getCustomFields(), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Opportunity", "CREATE", createCtx, null);

        String typeSlug = request.getOpportunityTypeSlug();
        if (StringUtils.hasText(typeSlug)) {
            if (!opportunityTypeRepository.existsByTenantIdAndSlug(tenantId, typeSlug)) {
                throw new ResourceNotFoundException("Opportunity type not found: " + typeSlug);
            }
        }
        String effectiveEntityType = StringUtils.hasText(typeSlug) ? "Opportunity:" + typeSlug : "Opportunity";

        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, effectiveEntityType, request.getCustomFields());
        }

        Long currentUserId = TenantContext.getUserId();
        Opportunity opportunity = Opportunity.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .name(request.getName())
                .stage(request.getStage() != null ? request.getStage() : "prospecting")
                .value(request.getValue())
                .probability(request.getProbability())
                .closeDate(request.getCloseDate())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : currentUserId)
                .opportunityTypeSlug(StringUtils.hasText(typeSlug) ? typeSlug : null)
                .build();

        // Set JSONB custom_data directly on entity
        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            opportunity.setCustomData(sanitizedCustomFields);

            // Extract and set denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(
                    tenantId, effectiveEntityType, sanitizedCustomFields);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                opportunity.setTableDataJsonb(tableDataNode);
            }
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_OPPORTUNITY", "Opportunity", saved.getId(), request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Opportunity", saved.getId(), "CREATED", toDTO(saved, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "Opportunity", saved.getId(), saved.getName());
        }

        OpportunityDTO dto = toDTO(saved, tenantId);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "OpportunityDetail", key = "#opportunityId")
    public OpportunityDTO getOpportunity(Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        if (!opportunity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canView(userId, role, tenantId, "Opportunity", opportunityId, opportunity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(opportunity, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityDTO> listOpportunities(int page, int size, String sortBy, String sortOrder,
                                                          String search, Long customerId, List<String> stage, String typeSlug, Map<String, List<String>> customFieldFilters) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("name", "stage", "value", "createdAt", "updatedAt", "closeDate"));

        Specification<Opportunity> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customerId"), customerId));
        }

        if (stage != null && !stage.isEmpty()) {
            List<String> lower = stage.stream().map(String::toLowerCase).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("stage").in(lower));
        }

        if (org.springframework.util.StringUtils.hasText(typeSlug)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("opportunityTypeSlug"), typeSlug));
        }

        Page<Opportunity> result = opportunityRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(Opportunity::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? java.util.Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "Opportunity", candidateIds);
        List<OpportunityDTO> content = result.getContent().stream()
                .filter(o -> !hiddenIds.contains(o.getId()) || o.getOwnerId() != null && o.getOwnerId().equals(userId))
                .map(this::toDTOTableView)
                .filter(dto -> matchesSearchTerm(dto, search))
                .filter(dto -> matchesCustomFieldFilters(dto, customFieldFilters))
                .collect(Collectors.toList());

        return PagedResponse.<OpportunityDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, "createdAt", "desc",
                Set.of("name", "stage", "value", "createdAt", "updatedAt", "closeDate"));

        // Simple text search across name field
        Specification<Opportunity> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchPred = cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%");
            return cb.and(tenantPred, searchPred);
        };

        Page<Opportunity> result = opportunityRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(Opportunity::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? java.util.Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "Opportunity", candidateIds);
        List<OpportunityDTO> content = result.getContent().stream()
                .filter(o -> !hiddenIds.contains(o.getId()) || o.getOwnerId() != null && o.getOwnerId().equals(userId))
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<OpportunityDTO>builder()
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
     * Filters DTOs by search term across standard fields and denormalized custom fields.
     * TODO: Migrate to Elasticsearch for production use to enable efficient full-text search.
     * Currently uses in-memory filtering against tableDataJsonb (denormalized custom fields visible in tables).
     */
    private boolean matchesSearchTerm(OpportunityDTO dto, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) return true;

        String search = searchTerm.toLowerCase();

        // Search standard fields: name, stage
        if (dto.getName() != null && dto.getName().toLowerCase().contains(search)) return true;
        if (dto.getStage() != null && dto.getStage().toLowerCase().contains(search)) return true;

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

    private boolean matchesCustomFieldFilters(OpportunityDTO dto, Map<String, List<String>> cfFilters) {
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

    private Pageable buildPageable(int page, int size, String sortBy, String sortOrder, Set<String> allowedSorts) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
    }

    @Transactional
    @CacheEvict(value = {"OpportunityDetail", "OpportunityTable"}, allEntries = true)
    public void deleteOpportunity(Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        if (!opportunity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Opportunity", opportunityId, opportunity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        policyValidationService.validate(tenantId, "Opportunity", "DELETE",
                objectMapper.convertValue(opportunity, new TypeReference<Map<String, Object>>() {}), null);

        // Publish integration event before deletion
        integrationEventPublisher.publish(tenantId, "Opportunity", opportunityId, "DELETED", toDTO(opportunity, tenantId));

        // Clean up opportunity_contacts join table
        jdbcTemplate.update("DELETE FROM opportunity_contacts WHERE opportunity_id=?", opportunityId);

        opportunityRepository.delete(opportunity);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_OPPORTUNITY", "Opportunity", opportunityId, null);
    }

    @Transactional
    @CacheEvict(value = {"OpportunityDetail", "OpportunityTable"}, allEntries = true)
    public OpportunityDTO updateOpportunity(Long opportunityId, UpdateOpportunityRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        if (!opportunity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Opportunity", opportunityId, opportunity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        Long previousOwnerId = opportunity.getOwnerId();

        // Policy rules validation — flatten standard fields + customData into context maps
        Map<String, Object> previousCtx = new HashMap<>();
        previousCtx.put("name", opportunity.getName());
        previousCtx.put("stage", opportunity.getStage());
        if (opportunity.getValue() != null) previousCtx.put("value", opportunity.getValue());
        if (opportunity.getOwnerId() != null) previousCtx.put("ownerId", opportunity.getOwnerId());
        if (opportunity.getStatus() != null) previousCtx.put("status", opportunity.getStatus());
        flattenCustomFields(opportunity.getCustomData(), previousCtx);

        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getName() != null) mergedCtx.put("name", request.getName());
        if (request.getStage() != null) mergedCtx.put("stage", request.getStage());
        if (request.getValue() != null) mergedCtx.put("value", request.getValue());
        if (request.getOwnerId() != null) mergedCtx.put("ownerId", request.getOwnerId());
        flattenCustomFields(request.getCustomFields(), mergedCtx);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Opportunity", "UPDATE", mergedCtx, previousCtx);

        if (request.getName() != null) opportunity.setName(request.getName());
        if (request.getStage() != null) opportunity.setStage(request.getStage());
        if (request.getValue() != null) opportunity.setValue(request.getValue());
        if (request.getProbability() != null) opportunity.setProbability(request.getProbability());
        if (request.getCloseDate() != null) opportunity.setCloseDate(request.getCloseDate());
        if (request.getOwnerId() != null) opportunity.setOwnerId(request.getOwnerId());
        if (request.getOpportunityTypeSlug() != null) {
            String newTypeSlug = request.getOpportunityTypeSlug();
            if (StringUtils.hasText(newTypeSlug) && !opportunityTypeRepository.existsByTenantIdAndSlug(tenantId, newTypeSlug)) {
                throw new ResourceNotFoundException("Opportunity type not found: " + newTypeSlug);
            }
            opportunity.setOpportunityTypeSlug(StringUtils.hasText(newTypeSlug) ? newTypeSlug : null);
        }

        // Update custom fields if provided
        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            String updatedTypeSlug = opportunity.getOpportunityTypeSlug();
            String effectiveEntityType = StringUtils.hasText(updatedTypeSlug) ? "Opportunity:" + updatedTypeSlug : "Opportunity";
            JsonNode sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, effectiveEntityType, request.getCustomFields());

            // Merge existing and incoming custom fields using ObjectNode
            ObjectNode mergedNode = objectMapper.createObjectNode();
            JsonNode existing = opportunity.getCustomData();
            if (existing != null && existing.isObject()) {
                mergedNode.setAll((ObjectNode) existing);
            }
            if (sanitizedCustomFields.isObject()) {
                mergedNode.setAll((ObjectNode) sanitizedCustomFields);
            }

            opportunity.setCustomData(mergedNode);

            // Recalculate denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Opportunity", mergedNode);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                opportunity.setTableDataJsonb(tableDataNode);
            } else {
                opportunity.setTableDataJsonb(objectMapper.createObjectNode());
            }
        }

        Opportunity updated = opportunityRepository.save(opportunity);

        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_OPPORTUNITY", "Opportunity", opportunityId, request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Opportunity", updated.getId(), "UPDATED", toDTO(updated, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "Opportunity", updated.getId(), updated.getName());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "Opportunity", updated.getId(), updated.getName());
            }
        }

        OpportunityDTO dto = toDTO(updated, tenantId);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    private OpportunityDTO toDTO(Opportunity opportunity, Long tenantId) {
        // Fetch contact IDs from opportunity_contacts join table
        List<Long> contactIds = jdbcTemplate.queryForList(
                "SELECT contact_id FROM opportunity_contacts WHERE opportunity_id=? ORDER BY contact_id",
                Long.class, opportunity.getId());

        try {
            return OpportunityDTO.builder()
                    .id(opportunity.getId())
                    .customerId(opportunity.getCustomerId())
                    .name(opportunity.getName())
                    .stage(opportunity.getStage())
                    .value(opportunity.getValue())
                    .probability(opportunity.getProbability())
                    .closeDate(opportunity.getCloseDate())
                    .ownerId(opportunity.getOwnerId())
                    .opportunityTypeSlug(opportunity.getOpportunityTypeSlug())
                    .createdAt(opportunity.getCreatedAt())
                    .updatedAt(opportunity.getUpdatedAt())
                    .customFields(opportunity.getCustomData())  // Read from JSONB column
                    .contactIds(contactIds)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting opportunity to DTO", e);
        }
    }

    /**
     * Alternative DTO for table views - uses denormalized table_data_jsonb
     * Table data includes both custom and calculated fields pre-computed at save time
     */
    private OpportunityDTO toDTOTableView(Opportunity opportunity) {
        try {
            return OpportunityDTO.builder()
                    .id(opportunity.getId())
                    .customerId(opportunity.getCustomerId())
                    .name(opportunity.getName())
                    .stage(opportunity.getStage())
                    .value(opportunity.getValue())
                    .probability(opportunity.getProbability())
                    .closeDate(opportunity.getCloseDate())
                    .ownerId(opportunity.getOwnerId())
                    .opportunityTypeSlug(opportunity.getOpportunityTypeSlug())
                    .createdAt(opportunity.getCreatedAt())
                    .updatedAt(opportunity.getUpdatedAt())
                    .customFields(opportunity.getTableDataJsonb())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting opportunity to table view DTO", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ContactDTO> listContactsByOpportunity(Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Fetch contact IDs from join table
        List<Long> contactIds = jdbcTemplate.queryForList(
                "SELECT contact_id FROM opportunity_contacts WHERE opportunity_id=? ORDER BY contact_id",
                Long.class, opportunityId);

        if (contactIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Load contacts from repository
        List<Contact> contacts = contactRepository.findByTenantId(tenantId);
        List<ContactDTO> result = new ArrayList<>();
        for (Contact c : contacts) {
            if (contactIds.contains(c.getId())) {
                result.add(ContactDTO.builder()
                        .id(c.getId())
                        .customerId(c.getCustomerId())
                        .name(c.getName())
                        .email(c.getEmail())
                        .phone(c.getPhone())
                        .title(c.getTitle())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build());
            }
        }
        return result;
    }

    @Transactional
    public void addContactToOpportunity(Long opportunityId, Long contactId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Verify opportunity exists and belongs to tenant
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        if (!opportunity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        // Verify contact exists and belongs to tenant
        Contact contact = contactRepository.findByIdAndTenantId(contactId, tenantId);
        if (contact == null) {
            throw new ResourceNotFoundException("Contact not found");
        }

        // Insert or ignore if already exists
        jdbcTemplate.update(
                "INSERT INTO opportunity_contacts(opportunity_id, contact_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                opportunityId, contactId);
    }

    @Transactional
    public void removeContactFromOpportunity(Long opportunityId, Long contactId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Verify opportunity exists and belongs to tenant
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        if (!opportunity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        // Delete the contact association
        jdbcTemplate.update(
                "DELETE FROM opportunity_contacts WHERE opportunity_id=? AND contact_id=?",
                opportunityId, contactId);
    }

    /**
     * Enqueue calculated field recalculations for an entity.
     * Called after create/update to trigger async evaluation.
     */
    private void enqueueCalculatedFieldRefresh(Long tenantId, Long opportunityId) {
        calculationMessagePublisher.publish(tenantId, "Opportunity", opportunityId);
    }
}
