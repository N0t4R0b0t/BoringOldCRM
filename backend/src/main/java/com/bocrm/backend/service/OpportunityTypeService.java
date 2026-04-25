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

import com.bocrm.backend.dto.OpportunityTypeDTO;
import com.bocrm.backend.entity.OpportunityType;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.repository.OpportunityRepository;
import com.bocrm.backend.repository.OpportunityTypeRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
/**
 * OpportunityTypeService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class OpportunityTypeService {

    private final OpportunityTypeRepository opportunityTypeRepository;
    private final OpportunityRepository opportunityRepository;
    private final AuditLogService auditLogService;

    public OpportunityTypeService(OpportunityTypeRepository opportunityTypeRepository,
                                  OpportunityRepository opportunityRepository,
                                  AuditLogService auditLogService) {
        this.opportunityTypeRepository = opportunityTypeRepository;
        this.opportunityRepository = opportunityRepository;
        this.auditLogService = auditLogService;
    }

    public List<OpportunityTypeDTO> getAll() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        return opportunityTypeRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public OpportunityTypeDTO create(OpportunityTypeDTO.CreateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        String slug = generateSlug(request.getName());
        if (opportunityTypeRepository.existsByTenantIdAndSlug(tenantId, slug)) {
            throw new ValidationException("An opportunity type with a similar name already exists (slug: " + slug + ")");
        }

        OpportunityType type = OpportunityType.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .displayOrder(0)
                .build();

        OpportunityType saved = opportunityTypeRepository.save(type);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_OPPORTUNITY_TYPE", "OpportunityType", saved.getId(), request);
        return toDTO(saved);
    }

    @Transactional
    public OpportunityTypeDTO update(Long id, OpportunityTypeDTO.UpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        OpportunityType type = opportunityTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity type not found"));
        if (!type.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        String newSlug = generateSlug(request.getName());
        if (!newSlug.equals(type.getSlug()) && opportunityTypeRepository.existsByTenantIdAndSlugAndIdNot(tenantId, newSlug, id)) {
            throw new ValidationException("An opportunity type with a similar name already exists (slug: " + newSlug + ")");
        }

        type.setName(request.getName());
        type.setSlug(newSlug);
        type.setDescription(request.getDescription());
        type.setDisplayOrder(request.getDisplayOrder());

        OpportunityType saved = opportunityTypeRepository.save(type);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_OPPORTUNITY_TYPE", "OpportunityType", saved.getId(), request);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        OpportunityType type = opportunityTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity type not found"));
        if (!type.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        long inUse = opportunityRepository.countByTenantIdAndOpportunityTypeSlug(tenantId, type.getSlug());
        if (inUse > 0) {
            throw new ValidationException("Cannot delete opportunity type '" + type.getName() + "': it is used by " + inUse + " opportunity records");
        }

        opportunityTypeRepository.delete(type);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_OPPORTUNITY_TYPE", "OpportunityType", id, null);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
    }

    private OpportunityTypeDTO toDTO(OpportunityType type) {
        return OpportunityTypeDTO.builder()
                .id(type.getId())
                .name(type.getName())
                .slug(type.getSlug())
                .description(type.getDescription())
                .displayOrder(type.getDisplayOrder())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
}
