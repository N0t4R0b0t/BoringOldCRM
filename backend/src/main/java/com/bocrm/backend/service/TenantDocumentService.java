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
import com.bocrm.backend.entity.TenantDocument;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.TenantDocumentRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
/**
 * TenantDocumentService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"DocumentDetail", "DocumentList"})
public class TenantDocumentService {

    public static final long MAX_SIZE_BYTES = 100 * 1024 * 1024L;

    private final TenantDocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final PolicyValidationService policyValidationService;

    public TenantDocumentService(TenantDocumentRepository documentRepository,
                                  AuditLogService auditLogService,
                                  NotificationService notificationService,
                                  ObjectMapper objectMapper,
                                  AccessControlService accessControlService,
                                  PolicyValidationService policyValidationService) {
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.policyValidationService = policyValidationService;
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
    @CacheEvict(value = {"DocumentDetail", "DocumentList"}, allEntries = true)
    public TenantDocumentDTO createDocument(CreateDocumentRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        if (request.getSizeBytes() != null && request.getSizeBytes() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds the 100 MB limit");
        }

        // Build context for policy validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", request.getName());
        createCtx.put("description", request.getDescription());
        createCtx.put("mimeType", request.getMimeType());
        createCtx.put("sizeBytes", request.getSizeBytes());
        createCtx.put("contentType", request.getContentType());
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "TenantDocument", "CREATE", createCtx, null);

        // Build tags as JSONB
        JsonNode tagsNode = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagsNode = objectMapper.valueToTree(request.getTags());
        }

        TenantDocument doc = TenantDocument.builder()
                .tenantId(tenantId)
                .ownerId(TenantContext.getUserId())
                .name(request.getName())
                .description(request.getDescription())
                .mimeType(request.getMimeType())
                .sizeBytes(request.getSizeBytes())
                .contentBase64(request.getContentBase64())
                .contentType(request.getContentType() != null ? request.getContentType() : "file")
                .tags(tagsNode)
                .source("user_upload")
                .linkedEntityType(request.getLinkedEntityType())
                .linkedEntityId(request.getLinkedEntityId())
                .linkedFieldKey(request.getLinkedFieldKey())
                .build();

        TenantDocument saved = documentRepository.save(doc);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_DOCUMENT", "TenantDocument", saved.getId(), request);

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "TenantDocument", saved.getId(), saved.getName());
        }

        TenantDocumentDTO dto = toDTO(saved);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional
    @CacheEvict(value = {"DocumentDetail", "DocumentList"}, allEntries = true)
    public TenantDocumentDTO createFromContent(String name, String contentType, String mimeType,
                                                String textContent, String source,
                                                String linkedEntityType, Long linkedEntityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        byte[] bytes = textContent.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        long sizeBytes = bytes.length;

        // Build context for policy validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", name);
        createCtx.put("mimeType", mimeType);
        createCtx.put("sizeBytes", sizeBytes);
        createCtx.put("contentType", contentType);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "TenantDocument", "CREATE", createCtx, null);

        // Build tags as JSONB
        JsonNode tagsNode = null;

        TenantDocument doc = TenantDocument.builder()
                .tenantId(tenantId)
                .ownerId(TenantContext.getUserId())
                .name(name)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .contentBase64(base64)
                .contentType(contentType != null ? contentType : "file")
                .tags(tagsNode)
                .source(source != null ? source : "assistant_generated")
                .linkedEntityType(linkedEntityType)
                .linkedEntityId(linkedEntityId)
                .build();

        TenantDocument saved = documentRepository.save(doc);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_DOCUMENT", "TenantDocument", saved.getId(), name);

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "TenantDocument", saved.getId(), saved.getName());
        }

        TenantDocumentDTO dto = toDTO(saved);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "DocumentDetail", key = "#id")
    public TenantDocumentDTO getDocument(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        TenantDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canView(userId, role, tenantId, "Document", id, doc.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public DocumentDownloadDTO downloadDocument(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        TenantDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canView(userId, role, tenantId, "Document", id, doc.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        return DocumentDownloadDTO.builder()
                .id(doc.getId())
                .name(doc.getName())
                .mimeType(doc.getMimeType())
                .contentBase64(doc.getContentBase64())
                .storageUrl(doc.getStorageUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantDocumentDTO> listDocuments(int page, int size, String search, String source,
                                                           String contentType, String linkedEntityType,
                                                           Long linkedEntityId, String linkedFieldKey) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<TenantDocument> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        if (source != null && !source.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("source"), source));
        }
        if (contentType != null && !contentType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("contentType"), contentType));
        }
        if (linkedEntityType != null && !linkedEntityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("linkedEntityType"), linkedEntityType));
        }
        if (linkedEntityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("linkedEntityId"), linkedEntityId));
        }
        if (linkedFieldKey != null && !linkedFieldKey.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("linkedFieldKey"), linkedFieldKey));
        }

        Page<TenantDocument> result = documentRepository.findAll(spec, pageable);
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        List<Long> candidateIds = result.getContent().stream().map(TenantDocument::getId).collect(Collectors.toList());
        Set<Long> hiddenIds = candidateIds.isEmpty() ? Collections.emptySet()
                : accessControlService.getHiddenEntityIds(userId, role, tenantId, "Document", candidateIds);
        List<TenantDocumentDTO> content = result.getContent().stream()
                .filter(d -> !hiddenIds.contains(d.getId()) || d.getOwnerId() != null && d.getOwnerId().equals(userId))
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PagedResponse.<TenantDocumentDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    @Transactional
    @CacheEvict(value = {"DocumentDetail", "DocumentList"}, allEntries = true)
    public void deleteDocument(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        TenantDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Document", id, doc.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        // Policy rules validation
        Map<String, Object> deleteCtx = objectMapper.convertValue(doc, new TypeReference<Map<String, Object>>() {});
        policyValidationService.validate(tenantId, "TenantDocument", "DELETE", deleteCtx, null);

        documentRepository.delete(doc);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_DOCUMENT", "TenantDocument", id, null);
    }

    public TenantDocumentDTO toDTO(TenantDocument doc) {
        List<String> tagsList = null;
        if (doc.getTags() != null && doc.getTags().isArray()) {
            tagsList = new ArrayList<>();
            for (JsonNode tag : doc.getTags()) {
                tagsList.add(tag.asText());
            }
        }

        return TenantDocumentDTO.builder()
                .id(doc.getId())
                .ownerId(doc.getOwnerId())
                .name(doc.getName())
                .description(doc.getDescription())
                .mimeType(doc.getMimeType())
                .sizeBytes(doc.getSizeBytes())
                .storageUrl(doc.getStorageUrl())
                .contentType(doc.getContentType())
                .tags(tagsList)
                .source(doc.getSource())
                .linkedEntityType(doc.getLinkedEntityType())
                .linkedEntityId(doc.getLinkedEntityId())
                .linkedFieldKey(doc.getLinkedFieldKey())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    @Transactional
    @CacheEvict(value = {"DocumentDetail", "DocumentList"}, allEntries = true)
    public TenantDocumentDTO renameDocument(Long id, String newName) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        TenantDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!doc.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
        Long userId = TenantContext.getUserId();
        String role = getCurrentRole();
        if (!accessControlService.canWrite(userId, role, tenantId, "Document", id, doc.getOwnerId())) {
            throw new ForbiddenException("Access denied");
        }

        // Policy rules validation
        Map<String, Object> previousCtx = objectMapper.convertValue(doc, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        mergedCtx.put("name", newName);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "TenantDocument", "UPDATE", mergedCtx, previousCtx);

        doc.setName(newName);
        TenantDocumentDTO dto = toDTO(documentRepository.save(doc));
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    @Transactional
    @CacheEvict(value = {"DocumentDetail", "DocumentList"}, allEntries = true)
    public TenantDocumentDTO duplicateDocument(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        TenantDocument src = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!src.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
        TenantDocument copy = TenantDocument.builder()
                .tenantId(tenantId)
                .name("Copy of " + src.getName())
                .description(src.getDescription())
                .mimeType(src.getMimeType())
                .sizeBytes(src.getSizeBytes())
                .contentBase64(src.getContentBase64())
                .storageUrl(src.getStorageUrl())
                .contentType(src.getContentType())
                .tags(src.getTags())
                .source(src.getSource())
                .linkedEntityType(src.getLinkedEntityType())
                .linkedEntityId(src.getLinkedEntityId())
                .linkedFieldKey(src.getLinkedFieldKey())
                .build();
        TenantDocument saved = documentRepository.save(copy);
        auditLogService.logAction(TenantContext.getUserId(), "DUPLICATE_DOCUMENT", "TenantDocument", saved.getId(), id);
        return toDTO(saved);
    }
}
