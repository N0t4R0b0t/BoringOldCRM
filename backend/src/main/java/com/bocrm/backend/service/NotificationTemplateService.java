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

import com.bocrm.backend.dto.CreateNotificationTemplateRequest;
import com.bocrm.backend.dto.NotificationTemplateDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateNotificationTemplateRequest;
import com.bocrm.backend.entity.NotificationTemplate;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.NotificationTemplateRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * NotificationTemplateService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;
    private final AuditLogService auditLogService;

    public NotificationTemplateService(NotificationTemplateRepository repository,
                                      AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public NotificationTemplateDTO create(CreateNotificationTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId)
                .notificationType(request.getNotificationType())
                .name(request.getName())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        NotificationTemplate saved = repository.save(template);
        auditLogService.logAction(userId, "CREATE", "NotificationTemplate", saved.getId(), null);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDTO get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Optional<NotificationTemplate> template = repository.findByIdAndTenantId(id, tenantId);
        return template.map(this::toDTO).orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationTemplateDTO> list(Long tenantId, int page, int size,
                                                        String search, String notificationType) {
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Specification<NotificationTemplate> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("notificationType")), "%" + search.toLowerCase() + "%")
                ));
            }
            if (notificationType != null && !notificationType.isBlank()) {
                predicates.add(cb.equal(root.get("notificationType"), notificationType));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = buildPageable(page, size, "createdAt", "DESC", "name", "notificationType", "createdAt", "updatedAt");
        Page<NotificationTemplate> result = repository.findAll(spec, pageable);

        return PagedResponse.<NotificationTemplateDTO>builder()
                .content(result.getContent().stream().map(this::toDTO).collect(Collectors.toList()))
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateDTO> listAllForTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return repository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationTemplateDTO update(Long id, UpdateNotificationTemplateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Optional<NotificationTemplate> opt = repository.findByIdAndTenantId(id, tenantId);
        if (opt.isEmpty()) throw new ForbiddenException("Template not found");

        NotificationTemplate template = opt.get();
        if (request.getName() != null) template.setName(request.getName());
        if (request.getSubjectTemplate() != null) template.setSubjectTemplate(request.getSubjectTemplate());
        if (request.getBodyTemplate() != null) template.setBodyTemplate(request.getBodyTemplate());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        NotificationTemplate saved = repository.save(template);
        auditLogService.logAction(userId, "UPDATE", "NotificationTemplate", saved.getId(), null);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Optional<NotificationTemplate> opt = repository.findByIdAndTenantId(id, tenantId);
        if (opt.isEmpty()) throw new ForbiddenException("Template not found");

        repository.deleteById(id);
        auditLogService.logAction(userId, "DELETE", "NotificationTemplate", id, null);
    }

    /**
     * Find the active template for a notification type (used by dispatch service).
     */
    @Transactional(readOnly = true)
    public Optional<NotificationTemplateDTO> findActiveForType(Long tenantId, String notificationType) {
        return repository.findByTenantIdAndNotificationTypeAndIsActiveTrue(tenantId, notificationType)
                .map(this::toDTO);
    }

    /**
     * Apply placeholders to a template string.
     */
    public String applyPlaceholders(String template, Map<String, String> variables) {
        if (template == null) return null;
        String result = template;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return result;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private NotificationTemplateDTO toDTO(NotificationTemplate template) {
        return NotificationTemplateDTO.builder()
                .id(template.getId())
                .tenantId(template.getTenantId())
                .notificationType(template.getNotificationType())
                .name(template.getName())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private Pageable buildPageable(int page, int size, String defaultSort, String defaultOrder,
                                   String... allowedSorts) {
        size = Math.min(size, 200); // cap at 200
        Sort.Direction direction = "DESC".equalsIgnoreCase(defaultOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = defaultSort;
        for (String allowed : allowedSorts) {
            if (allowed.equals(defaultSort)) {
                sortBy = allowed;
                break;
            }
        }
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
