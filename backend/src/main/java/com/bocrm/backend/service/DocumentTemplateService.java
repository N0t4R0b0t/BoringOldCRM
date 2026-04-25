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

import com.bocrm.backend.dto.CreateDocumentTemplateRequest;
import com.bocrm.backend.dto.DocumentTemplateDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateDocumentTemplateRequest;
import com.bocrm.backend.entity.DocumentTemplate;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.DocumentTemplateRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * DocumentTemplateService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"DocumentTemplateDetail", "DocumentTemplateList"})
public class DocumentTemplateService {

    private final DocumentTemplateRepository documentTemplateRepository;
    private final AuditLogService auditLogService;

    public DocumentTemplateService(DocumentTemplateRepository documentTemplateRepository,
                                    AuditLogService auditLogService) {
        this.documentTemplateRepository = documentTemplateRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @CacheEvict(value = {"DocumentTemplateDetail", "DocumentTemplateList"}, allEntries = true)
    public DocumentTemplateDTO createTemplate(CreateDocumentTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        DocumentTemplate template = DocumentTemplate.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .templateType(request.getTemplateType())
                .styleJson(request.getStyleJson())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        DocumentTemplate saved = documentTemplateRepository.save(template);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_DOCUMENT_TEMPLATE", "DocumentTemplate", saved.getId(), request);
        return toDTO(saved);
    }

    @Cacheable(value = "DocumentTemplateDetail", key = "#id")
    public DocumentTemplateDTO getTemplate(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        DocumentTemplate template = documentTemplateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document template not found"));
        return toDTO(template);
    }

    @Cacheable(value = "DocumentTemplateList")
    public PagedResponse<DocumentTemplateDTO> listTemplates(int page, int size, String sortBy, String sortOrder,
                                                             String search, String templateType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("name", "templateType", "createdAt", "updatedAt"));

        Specification<DocumentTemplate> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        if (templateType != null && !templateType.isBlank()) {
            String ft = templateType;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("templateType"), ft));
        }

        Page<DocumentTemplate> result = documentTemplateRepository.findAll(spec, pageable);
        List<DocumentTemplateDTO> content = result.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PagedResponse.<DocumentTemplateDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    public List<DocumentTemplateDTO> listAllForTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return documentTemplateRepository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"DocumentTemplateDetail", "DocumentTemplateList"}, allEntries = true)
    public DocumentTemplateDTO updateTemplate(Long id, UpdateDocumentTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        DocumentTemplate template = documentTemplateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document template not found"));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getTemplateType() != null) template.setTemplateType(request.getTemplateType());
        if (request.getStyleJson() != null) template.setStyleJson(request.getStyleJson());
        if (request.getIsDefault() != null) template.setIsDefault(request.getIsDefault());

        DocumentTemplate saved = documentTemplateRepository.save(template);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_DOCUMENT_TEMPLATE", "DocumentTemplate", saved.getId(), request);
        return toDTO(saved);
    }

    @Transactional
    @CacheEvict(value = {"DocumentTemplateDetail", "DocumentTemplateList"}, allEntries = true)
    public void deleteTemplate(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        DocumentTemplate template = documentTemplateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document template not found"));

        documentTemplateRepository.delete(template);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_DOCUMENT_TEMPLATE", "DocumentTemplate", id, null);
    }

    @Transactional
    @CacheEvict(value = {"DocumentTemplateDetail", "DocumentTemplateList"}, allEntries = true)
    public DocumentTemplateDTO cloneTemplate(Long id, String newName) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        DocumentTemplate original = documentTemplateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document template not found"));

        String clonedName = newName != null && !newName.isBlank() ? newName : original.getName() + " (Copy)";

        DocumentTemplate cloned = DocumentTemplate.builder()
                .tenantId(tenantId)
                .name(clonedName)
                .description(original.getDescription())
                .templateType(original.getTemplateType())
                .styleJson(original.getStyleJson())
                .isDefault(false)  // New clone is never default
                .build();

        DocumentTemplate saved = documentTemplateRepository.save(cloned);
        auditLogService.logAction(TenantContext.getUserId(), "CLONE_DOCUMENT_TEMPLATE", "DocumentTemplate", saved.getId(),
                "Cloned from template " + id);
        return toDTO(saved);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortOrder, Set<String> allowedSorts) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
    }

    private DocumentTemplateDTO toDTO(DocumentTemplate template) {
        return DocumentTemplateDTO.builder()
                .id(template.getId())
                .tenantId(template.getTenantId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .styleJson(template.getStyleJson())
                .isDefault(template.getIsDefault())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
