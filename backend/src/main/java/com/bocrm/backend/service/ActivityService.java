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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;
/**
 * ActivityService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"ActivityDetail", "ActivityTable"})
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final EntityCustomFieldRepository customFieldRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final PolicyValidationService policyValidationService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;
    public ActivityService(ActivityRepository activityRepository, EntityCustomFieldRepository customFieldRepository,
                          CustomFieldDefinitionService fieldDefinitionService,
                          CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                          AuditLogService auditLogService,
                          NotificationService notificationService, ObjectMapper objectMapper, AccessControlService accessControlService,
                          PolicyValidationService policyValidationService, CalculationMessagePublisher calculationMessagePublisher,
                          IntegrationEventPublisher integrationEventPublisher) {
        this.activityRepository = activityRepository;
        this.customFieldRepository = customFieldRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.policyValidationService = policyValidationService;
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

    @Transactional
    @CacheEvict(value = {"ActivityDetail", "ActivityTable"}, allEntries = true)
    public ActivityDTO createActivity(CreateActivityRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("subject", request.getSubject());
        createCtx.put("type", request.getType());
        createCtx.put("status", request.getStatus() != null ? request.getStatus() : "pending");
        if (request.getOwnerId() != null) createCtx.put("ownerId", request.getOwnerId());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Activity", "CREATE", createCtx, null);

        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Activity", request.getCustomFields());
        }

        Long currentUserId = TenantContext.getUserId();
        Activity activity = Activity.builder()
                .tenantId(tenantId)
                .subject(request.getSubject())
                .type(request.getType())
                .description(request.getDescription())
                .dueAt(request.getDueAt())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : currentUserId)
                .relatedType(request.getRelatedType())
                .relatedId(request.getRelatedId())
                .status(request.getStatus() != null ? request.getStatus() : "pending")
                .build();

        // Set JSONB custom_data directly on entity
        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            // sanitizedCustomFields is JsonNode - set directly
            activity.setCustomData(sanitizedCustomFields);

            // Extract and set denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(
                    tenantId, "Activity", sanitizedCustomFields);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                activity.setTableDataJsonb(tableDataNode);
            } else {
                activity.setTableDataJsonb(objectMapper.createObjectNode());
            }
        } else {
            activity.setTableDataJsonb(objectMapper.createObjectNode());
        }

        Activity saved = activityRepository.save(activity);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_ACTIVITY", "Activity", saved.getId(), request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Activity", saved.getId(), "CREATED", toDTO(saved, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "Activity", saved.getId(), saved.getSubject());
        }

        ActivityDTO dto = toDTO(saved, tenantId);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ActivityDetail", key = "#activityId")
    public ActivityDTO getActivity(Long activityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        if (!activity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canView(userId, role, tenantId, "Activity", activityId, activity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(activity, tenantId);
    }

        @Transactional(readOnly = true)
        public PagedResponse<ActivityDTO> listActivities(int page, int size, String sortBy, String sortOrder,
                                String search, List<String> type, List<String> status, Map<String, List<String>> customFieldFilters) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
            Set.of("subject", "type", "status", "createdAt", "updatedAt", "dueAt"));

        Specification<Activity> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (type != null && !type.isEmpty()) {
            List<String> lower = type.stream().map(String::toLowerCase).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("type").in(lower));
        }

        if (status != null && !status.isEmpty()) {
            List<String> lower = status.stream().map(String::toLowerCase).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("status").in(lower));
        }

        Page<Activity> result = activityRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(Activity::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? java.util.Collections.emptySet()
            : accessControlService.getHiddenEntityIds(userId, role, tenantId, "Activity", candidateIds);
        List<ActivityDTO> content = result.getContent().stream()
            .filter(a -> !hiddenIds.contains(a.getId()) || a.getOwnerId() != null && a.getOwnerId().equals(userId))
            .map(this::toDTOTableView)
            .filter(dto -> matchesSearchTerm(dto, search))
            .filter(dto -> matchesCustomFieldFilters(dto, customFieldFilters))
            .collect(Collectors.toList());

        return PagedResponse.<ActivityDTO>builder()
            .content(content)
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .currentPage(result.getNumber())
            .pageSize(result.getSize())
            .hasNext(result.hasNext())
            .hasPrev(result.hasPrevious())
            .build();
        }

        public PagedResponse<ActivityDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, "createdAt", "desc",
                Set.of("subject", "type", "status", "createdAt", "updatedAt", "dueAt"));

        // Simple text search across subject field
        Specification<Activity> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchPred = cb.like(cb.lower(root.get("subject")), "%" + searchTerm.toLowerCase() + "%");
            return cb.and(tenantPred, searchPred);
        };

        Page<Activity> result = activityRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(Activity::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? java.util.Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "Activity", candidateIds);
        List<ActivityDTO> content = result.getContent().stream()
                .filter(a -> !hiddenIds.contains(a.getId()) || a.getOwnerId() != null && a.getOwnerId().equals(userId))
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<ActivityDTO>builder()
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
        private boolean matchesSearchTerm(ActivityDTO dto, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) return true;

        String search = searchTerm.toLowerCase();

        // Search standard fields: subject, description
        if (dto.getSubject() != null && dto.getSubject().toLowerCase().contains(search)) return true;
        if (dto.getDescription() != null && dto.getDescription().toLowerCase().contains(search)) return true;

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

        private boolean matchesCustomFieldFilters(ActivityDTO dto, Map<String, List<String>> cfFilters) {
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
    @CacheEvict(value = {"ActivityDetail", "ActivityTable"}, allEntries = true)
    public void deleteActivity(Long activityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        if (!activity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Activity", activityId, activity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        policyValidationService.validate(tenantId, "Activity", "DELETE",
                objectMapper.convertValue(activity, new TypeReference<Map<String, Object>>() {}), null);

        // Publish integration event before deletion
        integrationEventPublisher.publish(tenantId, "Activity", activityId, "DELETED", toDTO(activity, tenantId));

        activityRepository.delete(activity);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_ACTIVITY", "Activity", activityId, null);
    }

    public ActivityDTO updateActivity(Long activityId, UpdateActivityRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        if (!activity.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Activity", activityId, activity.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        Long previousOwnerId = activity.getOwnerId();

        // Policy rules validation
        Map<String, Object> previousCtx = objectMapper.convertValue(activity, new TypeReference<Map<String, Object>>() {});
        flattenCustomFields(activity.getCustomData(), previousCtx);
        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getSubject() != null) mergedCtx.put("subject", request.getSubject());
        if (request.getType() != null) mergedCtx.put("type", request.getType());
        if (request.getStatus() != null) mergedCtx.put("status", request.getStatus());
        if (request.getOwnerId() != null) mergedCtx.put("ownerId", request.getOwnerId());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), mergedCtx);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Activity", "UPDATE", mergedCtx, previousCtx);

        if (request.getSubject() != null) activity.setSubject(request.getSubject());
        if (request.getType() != null) activity.setType(request.getType());
        if (request.getDescription() != null) activity.setDescription(request.getDescription());
        if (request.getDueAt() != null) activity.setDueAt(request.getDueAt());
        if (request.getOwnerId() != null) activity.setOwnerId(request.getOwnerId());
        if (request.getStatus() != null) activity.setStatus(request.getStatus());

        // Update custom fields if provided
        if (request.getCustomFields() != null && request.getCustomFields().size() > 0) {
            JsonNode sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Activity", request.getCustomFields());

            // Merge existing and incoming custom fields using ObjectNode
            ObjectNode mergedNode = objectMapper.createObjectNode();

            JsonNode existing = activity.getCustomData();
            if (existing != null && existing.isObject()) {
                mergedNode.setAll((ObjectNode) existing);
            }

            // sanitizedCustomFields is JsonNode
            if (sanitizedCustomFields.isObject()) {
                mergedNode.setAll((ObjectNode) sanitizedCustomFields);
            }

            activity.setCustomData(mergedNode);

            // Recalculate denormalized table_data_jsonb using JsonNode
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Activity", mergedNode);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                activity.setTableDataJsonb(tableDataNode);
            } else {
                activity.setTableDataJsonb(objectMapper.createObjectNode());
            }
        }

        Activity updated = activityRepository.save(activity);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_ACTIVITY", "Activity", activityId, request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Activity", updated.getId(), "UPDATED", toDTO(updated, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "Activity", updated.getId(), updated.getSubject());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "Activity", updated.getId(), updated.getSubject());
            }
        }

        ActivityDTO dto = toDTO(updated, tenantId);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    private ActivityDTO toDTO(Activity activity, Long tenantId) {
        return ActivityDTO.builder()
                .id(activity.getId())
                .subject(activity.getSubject())
                .type(activity.getType())
                .description(activity.getDescription())
                .dueAt(activity.getDueAt())
                .ownerId(activity.getOwnerId())
                .relatedType(activity.getRelatedType())
                .relatedId(activity.getRelatedId())
                .status(activity.getStatus())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .customFields(activity.getCustomData())  // Read from JSONB column
                .build();
    }

    /**
     * Alternative DTO for table views - uses denormalized table_data_jsonb
     */
    @SuppressWarnings("unchecked")
    private void flattenCustomFields(JsonNode customData, Map<String, Object> ctx) {
        if (customData == null || !customData.isObject()) return;
        Map<String, Object> flat = objectMapper.convertValue(customData, Map.class);
        if (flat != null) ctx.putAll(flat);
    }

    private ActivityDTO toDTOTableView(Activity activity) {
        return ActivityDTO.builder()
                .id(activity.getId())
                .subject(activity.getSubject())
                .type(activity.getType())
                .description(activity.getDescription())
                .dueAt(activity.getDueAt())
                .ownerId(activity.getOwnerId())
                .relatedType(activity.getRelatedType())
                .relatedId(activity.getRelatedId())
                .status(activity.getStatus())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .customFields(activity.getTableDataJsonb())
                .build();
    }

    /**
     * Enqueue calculated field recalculations for an entity.
     * Called after create/update to trigger async evaluation.
     */
    private void enqueueCalculatedFieldRefresh(Long tenantId, Long activityId) {
        calculationMessagePublisher.publish(tenantId, "Activity", activityId);
    }
}
