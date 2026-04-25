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
import com.bocrm.backend.entity.CustomRecord;
import com.bocrm.backend.entity.CalculatedFieldRefreshQueue;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.CustomRecordRepository;
import com.bocrm.backend.repository.CalculatedFieldRefreshQueueRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;
/**
 * CustomRecordService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"CustomRecordDetail", "CustomRecordTable"})
public class CustomRecordService {

    private final CustomRecordRepository customRecordRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AccessControlService accessControlService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    public CustomRecordService(CustomRecordRepository customRecordRepository,
                        CustomFieldDefinitionService fieldDefinitionService,
                        CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                        AuditLogService auditLogService,
                        NotificationService notificationService,
                        ObjectMapper objectMapper,
                        JdbcTemplate jdbcTemplate,
                        AccessControlService accessControlService,
                        CalculationMessagePublisher calculationMessagePublisher) {
        this.customRecordRepository = customRecordRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.accessControlService = accessControlService;
        this.calculationMessagePublisher = calculationMessagePublisher;
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
    @CacheEvict(value = {"CustomRecordDetail", "CustomRecordTable"}, allEntries = true)
    public CustomRecordDTO createCustomRecord(CreateCustomRecordRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "CustomRecord", request.getCustomFields());
        }

        CustomRecord customRecord = CustomRecord.builder()
                .tenantId(tenantId)
                .ownerId(TenantContext.getUserId())
                .name(request.getName())
                .type(request.getType())
                .serialNumber(request.getSerialNumber())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .customerId(request.getCustomerId())
                .notes(request.getNotes())
                .build();

        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            customRecord.setCustomData(sanitizedCustomFields);
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "CustomRecord", sanitizedCustomFields);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                customRecord.setTableDataJsonb(tableDataNode);
            } else {
                customRecord.setTableDataJsonb(objectMapper.createObjectNode());
            }
        } else {
            customRecord.setTableDataJsonb(objectMapper.createObjectNode());
        }

        CustomRecord saved = customRecordRepository.save(customRecord);

        // Link opportunity junction rows
        if (request.getOpportunityIds() != null && !request.getOpportunityIds().isEmpty()) {
            for (Long oppId : request.getOpportunityIds()) {
                jdbcTemplate.update(
                        "INSERT INTO opportunity_custom_records(opportunity_id, custom_record_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                        oppId, saved.getId());
            }
        }

        auditLogService.logAction(TenantContext.getUserId(), "CREATE_CUSTOM_RECORD", "CustomRecord", saved.getId(), request);

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "CustomRecord", saved.getId(), saved.getName());
        }

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "CustomRecordDetail", key = "#customRecordId")
    public CustomRecordDTO getCustomRecord(Long customRecordId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomRecord customRecord = customRecordRepository.findById(customRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRecord not found"));

        if (!customRecord.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canView(userId, role, tenantId, "CustomRecord", customRecordId, customRecord.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(customRecord);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomRecordDTO> listCustomRecords(int page, int size, String sortBy, String sortOrder,
                                               String search, String status, Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        Set<String> allowedSorts = Set.of("name", "type", "status", "createdAt", "updatedAt");
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));

        Specification<CustomRecord> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }
        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customerId"), customerId));
        }

        Page<CustomRecord> result = customRecordRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(CustomRecord::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "CustomRecord", candidateIds);
        List<CustomRecordDTO> content = result.getContent().stream()
                .filter(a -> !hiddenIds.contains(a.getId()) || a.getOwnerId() != null && a.getOwnerId().equals(userId))
                .map(this::toDTOTableView)
                .filter(dto -> matchesSearchTerm(dto, search))
                .collect(Collectors.toList());

        return PagedResponse.<CustomRecordDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    public PagedResponse<CustomRecordDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        Set<String> allowedSorts = Set.of("name", "type", "status", "createdAt", "updatedAt");
        String safeSort = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));

        // Simple text search across name field
        Specification<CustomRecord> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchPred = cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%");
            return cb.and(tenantPred, searchPred);
        };

        Page<CustomRecord> result = customRecordRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(CustomRecord::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "CustomRecord", candidateIds);
        List<CustomRecordDTO> content = result.getContent().stream()
                .filter(a -> !hiddenIds.contains(a.getId()) || a.getOwnerId() != null && a.getOwnerId().equals(userId))
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<CustomRecordDTO>builder()
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
    public List<CustomRecordDTO> listByOpportunity(Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Long> customRecordIds = jdbcTemplate.queryForList(
                "SELECT custom_record_id FROM opportunity_custom_records WHERE opportunity_id=?",
                Long.class, opportunityId);

        if (customRecordIds.isEmpty()) return Collections.emptyList();

        return customRecordIds.stream()
                .map(id -> customRecordRepository.findById(id).orElse(null))
                .filter(a -> a != null && a.getTenantId().equals(tenantId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"CustomRecordDetail", "CustomRecordTable"}, allEntries = true)
    public CustomRecordDTO updateCustomRecord(Long customRecordId, UpdateCustomRecordRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomRecord customRecord = customRecordRepository.findById(customRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRecord not found"));

        if (!customRecord.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "CustomRecord", customRecordId, customRecord.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getName() != null) customRecord.setName(request.getName());
        if (request.getType() != null) customRecord.setType(request.getType());
        if (request.getSerialNumber() != null) customRecord.setSerialNumber(request.getSerialNumber());
        if (request.getStatus() != null) customRecord.setStatus(request.getStatus());
        if (request.getCustomerId() != null) customRecord.setCustomerId(request.getCustomerId());
        if (request.getNotes() != null) customRecord.setNotes(request.getNotes());

        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            JsonNode sanitized = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "CustomRecord", request.getCustomFields());
            ObjectNode mergedNode = objectMapper.createObjectNode();
            if (customRecord.getCustomData() != null && customRecord.getCustomData().isObject()) {
                mergedNode.setAll((ObjectNode) customRecord.getCustomData());
            }
            if (sanitized.isObject()) {
                mergedNode.setAll((ObjectNode) sanitized);
            }
            customRecord.setCustomData(mergedNode);
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "CustomRecord", mergedNode);
            customRecord.setTableDataJsonb(tableDataNode != null && tableDataNode.size() > 0 ? tableDataNode : objectMapper.createObjectNode());
        }

        Long previousOwnerId = customRecord.getOwnerId();
        CustomRecord updated = customRecordRepository.save(customRecord);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_CUSTOM_RECORD", "CustomRecord", customRecordId, request);

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "CustomRecord", updated.getId(), updated.getName());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "CustomRecord", updated.getId(), updated.getName());
            }
        }

        return toDTO(updated);
    }

    @Transactional
    @CacheEvict(value = {"CustomRecordDetail", "CustomRecordTable"}, allEntries = true)
    public void deleteCustomRecord(Long customRecordId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomRecord customRecord = customRecordRepository.findById(customRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRecord not found"));

        if (!customRecord.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "CustomRecord", customRecordId, customRecord.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        jdbcTemplate.update("DELETE FROM opportunity_custom_records WHERE custom_record_id=?", customRecordId);
        customRecordRepository.delete(customRecord);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_CUSTOM_RECORD", "CustomRecord", customRecordId, null);
    }

    @Transactional
    @CacheEvict(value = {"CustomRecordDetail", "CustomRecordTable"}, allEntries = true)
    public void linkOpportunity(Long customRecordId, Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomRecord customRecord = customRecordRepository.findById(customRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRecord not found"));
        if (!customRecord.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");

        jdbcTemplate.update(
                "INSERT INTO opportunity_custom_records(opportunity_id, custom_record_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                opportunityId, customRecordId);
    }

    @Transactional
    @CacheEvict(value = {"CustomRecordDetail", "CustomRecordTable"}, allEntries = true)
    public void unlinkOpportunity(Long customRecordId, Long opportunityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomRecord customRecord = customRecordRepository.findById(customRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRecord not found"));
        if (!customRecord.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");

        jdbcTemplate.update("DELETE FROM opportunity_custom_records WHERE opportunity_id=? AND custom_record_id=?",
                opportunityId, customRecordId);
    }

    private CustomRecordDTO toDTO(CustomRecord customRecord) {
        List<Long> opportunityIds = jdbcTemplate.queryForList(
                "SELECT opportunity_id FROM opportunity_custom_records WHERE custom_record_id=?",
                Long.class, customRecord.getId());

        return CustomRecordDTO.builder()
                .id(customRecord.getId())
                .tenantId(customRecord.getTenantId())
                .ownerId(customRecord.getOwnerId())
                .name(customRecord.getName())
                .type(customRecord.getType())
                .serialNumber(customRecord.getSerialNumber())
                .status(customRecord.getStatus())
                .customerId(customRecord.getCustomerId())
                .notes(customRecord.getNotes())
                .customFields(customRecord.getCustomData())
                .opportunityIds(opportunityIds)
                .createdAt(customRecord.getCreatedAt())
                .updatedAt(customRecord.getUpdatedAt())
                .build();
    }

    private CustomRecordDTO toDTOTableView(CustomRecord customRecord) {
        return CustomRecordDTO.builder()
                .id(customRecord.getId())
                .tenantId(customRecord.getTenantId())
                .ownerId(customRecord.getOwnerId())
                .name(customRecord.getName())
                .type(customRecord.getType())
                .serialNumber(customRecord.getSerialNumber())
                .status(customRecord.getStatus())
                .customerId(customRecord.getCustomerId())
                .notes(customRecord.getNotes())
                .customFields(customRecord.getTableDataJsonb())
                .createdAt(customRecord.getCreatedAt())
                .updatedAt(customRecord.getUpdatedAt())
                .build();
    }

    /**
     * Filters DTOs by search term across standard fields and denormalized custom fields.
     * TODO: Migrate to Elasticsearch for production use to enable efficient full-text search.
     * Currently uses in-memory filtering against tableDataJsonb (denormalized custom fields visible in tables).
     */
    private boolean matchesSearchTerm(CustomRecordDTO dto, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) return true;

        String search = searchTerm.toLowerCase();

        // Search standard fields: name, type, status
        if (dto.getName() != null && dto.getName().toLowerCase().contains(search)) return true;
        if (dto.getType() != null && dto.getType().toLowerCase().contains(search)) return true;
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
     * Enqueue calculated field recalculations for an entity.
     * Called after create/update to trigger async evaluation.
     */
    private void enqueueCalculatedFieldRefresh(Long tenantId, Long customRecordId) {
        calculationMessagePublisher.publish(tenantId, "CustomRecord", customRecordId);
    }
}
