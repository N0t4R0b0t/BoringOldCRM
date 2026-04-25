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

import com.bocrm.backend.dto.AdvancedFilterRequest;
import com.bocrm.backend.dto.SavedFilterDTO;
import com.bocrm.backend.entity.SavedFilter;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.SavedFilterRepository;
import com.bocrm.backend.shared.TenantContext;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
/**
 * SavedFilterService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class SavedFilterService {
    private final SavedFilterRepository savedFilterRepository;
    private final ObjectMapper objectMapper;

    public SavedFilterService(SavedFilterRepository savedFilterRepository, ObjectMapper objectMapper) {
        this.savedFilterRepository = savedFilterRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedFilterDTO createFilter(SavedFilterDTO filterDTO) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        try {
            SavedFilter savedFilter = SavedFilter.builder()
                    .name(filterDTO.getName())
                    .entityType(filterDTO.getEntityType())
                    .filterConfig(objectMapper.writeValueAsString(filterDTO.getFilterConfig()))
                    .createdBy(userId)
                    .isPublic(filterDTO.isPublic())
                    .tenantId(tenantId)
                    .build();

            SavedFilter saved = savedFilterRepository.save(savedFilter);
            return toDTO(saved);
        } catch (Exception e) {
            throw new RuntimeException("Error processing filter config JSON", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SavedFilterDTO> listFilters() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<SavedFilter> userFilters = savedFilterRepository.findByTenantIdAndCreatedBy(tenantId, userId);
        List<SavedFilter> publicFilters = savedFilterRepository.findByTenantIdAndIsPublicTrue(tenantId);

        List<SavedFilter> allFilters = new ArrayList<>(userFilters);
        for (SavedFilter pf : publicFilters) {
            if (allFilters.stream().noneMatch(f -> f.getId().equals(pf.getId()))) {
                allFilters.add(pf);
            }
        }

        return allFilters.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavedFilterDTO getFilter(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        SavedFilter savedFilter = savedFilterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saved filter not found"));

        if (!savedFilter.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(savedFilter);
    }

    @Transactional
    public void deleteFilter(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        SavedFilter savedFilter = savedFilterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saved filter not found"));

        if (!savedFilter.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        if (!savedFilter.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("You can only delete your own filters");
        }

        savedFilterRepository.delete(savedFilter);
    }

    private SavedFilterDTO toDTO(SavedFilter savedFilter) {
        try {
            return SavedFilterDTO.builder()
                    .id(savedFilter.getId())
                    .name(savedFilter.getName())
                    .entityType(savedFilter.getEntityType())
                    .filterConfig(objectMapper.readValue(savedFilter.getFilterConfig(), AdvancedFilterRequest.class))
                    .createdBy(savedFilter.getCreatedBy())
                    .isPublic(savedFilter.isPublic())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing filter config JSON for filter ID: {}", savedFilter.getId(), e);
            return SavedFilterDTO.builder()
                    .id(savedFilter.getId())
                    .name(savedFilter.getName())
                    .entityType(savedFilter.getEntityType())
                    .createdBy(savedFilter.getCreatedBy())
                    .isPublic(savedFilter.isPublic())
                    .build();
        }
    }
}
