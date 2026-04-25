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
 * ContactService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"ContactDetail", "ContactTable"})
public class ContactService {
    private final ContactRepository contactRepository;
    private final EntityCustomFieldRepository customFieldRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final PolicyValidationService policyValidationService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;
    public ContactService(ContactRepository contactRepository, EntityCustomFieldRepository customFieldRepository,
                         CustomFieldDefinitionService fieldDefinitionService,
                         CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                         AuditLogService auditLogService,
                         ObjectMapper objectMapper, PolicyValidationService policyValidationService,
                         CalculationMessagePublisher calculationMessagePublisher,
                         IntegrationEventPublisher integrationEventPublisher) {
        this.contactRepository = contactRepository;
        this.customFieldRepository = customFieldRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.policyValidationService = policyValidationService;
        this.calculationMessagePublisher = calculationMessagePublisher;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    @Transactional
    @CacheEvict(value = {"ContactDetail", "ContactTable"}, allEntries = true)
    public ContactDTO createContact(CreateContactRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", request.getName());
        createCtx.put("email", request.getEmail());
        createCtx.put("status", request.getStatus() != null ? request.getStatus() : "active");
        if (request.getCustomerId() != null) createCtx.put("customerId", request.getCustomerId());
        if (request.getCustomFields() != null) flattenCustomFields(objectMapper.valueToTree(request.getCustomFields()), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Contact", "CREATE", createCtx, null);

        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Contact", objectMapper.valueToTree(request.getCustomFields()));
        }

        Contact contact = Contact.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .title(request.getTitle())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .build();

        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            contact.setCustomData(sanitizedCustomFields);

            // Extract and set denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(
                    tenantId, "Contact", sanitizedCustomFields);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                contact.setTableDataJsonb(tableDataNode);
            } else {
                contact.setTableDataJsonb(objectMapper.createObjectNode());
            }
        } else {
            contact.setTableDataJsonb(objectMapper.createObjectNode());
        }

        Contact saved = contactRepository.save(contact);

        auditLogService.logAction(TenantContext.getUserId(), "CREATE_CONTACT", "Contact", saved.getId(), request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Contact", saved.getId(), "CREATED", toDTO(saved, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        ContactDTO dto = toDTO(saved, tenantId);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ContactDetail", key = "#contactId")
    public ContactDTO getContact(Long contactId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(contact, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactDTO> listContacts(int page, int size, String sortBy, String sortOrder,
                                                 String search, Long customerId, Boolean hasEmail, List<String> status, Map<String, List<String>> customFieldFilters) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("name", "email", "phone", "status", "createdAt", "updatedAt"));

        Specification<Contact> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customerId"), customerId));
        }

        if (hasEmail != null) {
            if (hasEmail) {
                spec = spec.and((root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("email")),
                        cb.notEqual(root.get("email"), "")
                ));
            } else {
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.isNull(root.get("email")),
                        cb.equal(root.get("email"), "")
                ));
            }
        }

        if (status != null && !status.isEmpty()) {
            List<String> lower = status.stream().map(String::toLowerCase).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> root.get("status").in(lower));
        }

        Page<Contact> result = contactRepository.findAll(spec, pageable);
        List<ContactDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .filter(dto -> matchesSearchTerm(dto, search))
                .filter(dto -> matchesCustomFieldFilters(dto, customFieldFilters))
                .collect(Collectors.toList());

        return PagedResponse.<ContactDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    public PagedResponse<ContactDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, "createdAt", "desc",
                Set.of("name", "email", "phone", "status", "createdAt", "updatedAt"));

        // Simple text search across name and email fields
        Specification<Contact> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchLower = searchTerm.toLowerCase();
            var namePred = cb.like(cb.lower(root.get("name")), "%" + searchLower + "%");
            var emailPred = cb.like(cb.lower(root.get("email")), "%" + searchLower + "%");
            return cb.and(tenantPred, cb.or(namePred, emailPred));
        };

        Page<Contact> result = contactRepository.findAll(spec, pageable);
        List<ContactDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<ContactDTO>builder()
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
    private boolean matchesSearchTerm(ContactDTO dto, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) return true;

        String search = searchTerm.toLowerCase();

        // Search standard fields: name, email, phone
        if (dto.getName() != null && dto.getName().toLowerCase().contains(search)) return true;
        if (dto.getEmail() != null && dto.getEmail().toLowerCase().contains(search)) return true;
        if (dto.getPhone() != null && dto.getPhone().toLowerCase().contains(search)) return true;

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

    private boolean matchesCustomFieldFilters(ContactDTO dto, Map<String, List<String>> cfFilters) {
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
    @CacheEvict(value = {"ContactDetail", "ContactTable"}, allEntries = true)
    public ContactDTO updateContact(Long contactId, UpdateContactRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        // Policy rules validation
        Map<String, Object> previousCtx = objectMapper.convertValue(contact, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getName() != null) mergedCtx.put("name", request.getName());
        if (request.getEmail() != null) mergedCtx.put("email", request.getEmail());
        if (request.getPhone() != null) mergedCtx.put("phone", request.getPhone());
        if (request.getStatus() != null) mergedCtx.put("status", request.getStatus());
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Contact", "UPDATE", mergedCtx, previousCtx);

        if (request.getName() != null) contact.setName(request.getName());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getPhone() != null) contact.setPhone(request.getPhone());
        if (request.getTitle() != null) contact.setTitle(request.getTitle());
        if (request.getStatus() != null) contact.setStatus(request.getStatus());
        if (request.getCustomerId() != null) contact.setCustomerId(request.getCustomerId());

        // Update custom fields if provided
        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            JsonNode sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Contact", request.getCustomFields());

            // Merge existing and incoming custom fields using ObjectNode
            ObjectNode mergedNode = objectMapper.createObjectNode();
            JsonNode existing = contact.getCustomData();
            if (existing != null && existing.isObject()) {
                mergedNode.setAll((ObjectNode) existing);
            }
            // Use sanitized incoming fields
            if (sanitizedCustomFields.isObject()) {
                mergedNode.setAll((ObjectNode) sanitizedCustomFields);
            }

            contact.setCustomData(mergedNode);

            // Recalculate denormalized table_data_jsonb
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Contact", mergedNode);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                contact.setTableDataJsonb(tableDataNode);
            } else {
                contact.setTableDataJsonb(objectMapper.createObjectNode());
            }
        }

        Contact updated = contactRepository.save(contact);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_CONTACT", "Contact", contactId, request);

        // Publish integration event
        integrationEventPublisher.publish(tenantId, "Contact", updated.getId(), "UPDATED", toDTO(updated, tenantId));

        // Enqueue calculated field recalculations
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        ContactDTO dto = toDTO(updated, tenantId);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    @Transactional
    public void deleteContact(Long contactId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        policyValidationService.validate(tenantId, "Contact", "DELETE",
                objectMapper.convertValue(contact, new TypeReference<Map<String, Object>>() {}), null);

        // Publish integration event before deletion
        integrationEventPublisher.publish(tenantId, "Contact", contactId, "DELETED", toDTO(contact, tenantId));

        contactRepository.delete(contact);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_CONTACT", "Contact", contactId, null);
    }

    public ContactDTO disassociateContact(Long contactId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        contact.setCustomerId(null);
        Contact updated = contactRepository.save(contact);

        auditLogService.logAction(TenantContext.getUserId(), "DISASSOCIATE_CONTACT", "Contact", contactId, null);

        return toDTO(updated, tenantId);
    }

    private ContactDTO toDTO(Contact contact, Long tenantId) {
        return ContactDTO.builder()
                .id(contact.getId())
                .customerId(contact.getCustomerId())
                .name(contact.getName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .title(contact.getTitle())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .customFields(contact.getCustomData())  // Read from JSONB column
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

    private ContactDTO toDTOTableView(Contact contact) {
        return ContactDTO.builder()
                .id(contact.getId())
                .customerId(contact.getCustomerId())
                .name(contact.getName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .title(contact.getTitle())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .customFields(contact.getTableDataJsonb())
                .build();
    }

    /**
     * Enqueue calculated field recalculations for an entity.
     * Called after create/update to trigger async evaluation.
     */
    private void enqueueCalculatedFieldRefresh(Long tenantId, Long contactId) {
        calculationMessagePublisher.publish(tenantId, "Contact", contactId);
    }
}
