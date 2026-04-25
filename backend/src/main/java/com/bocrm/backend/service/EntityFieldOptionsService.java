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

import com.bocrm.backend.dto.FieldOptionDTO;
import com.bocrm.backend.entity.EntityFieldOptions;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.EntityFieldOptionsRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * EntityFieldOptionsService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityFieldOptionsService {

    private final EntityFieldOptionsRepository repository;

    @Transactional(readOnly = true)
    public Map<String, List<FieldOptionDTO>> getOptionsForEntity(String entityType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<EntityFieldOptions> rows = repository.findByTenantIdAndEntityTypeIgnoreCase(tenantId, entityType);
        Map<String, List<FieldOptionDTO>> result = new LinkedHashMap<>();
        for (EntityFieldOptions row : rows) {
            result.put(row.getFieldName(), toList(row));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<FieldOptionDTO> getOptionsForField(String entityType, String fieldName) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return repository
                .findByTenantIdAndEntityTypeIgnoreCaseAndFieldNameIgnoreCase(tenantId, entityType, fieldName)
                .map(this::toList)
                .orElse(List.of());
    }

    @Transactional
    public List<FieldOptionDTO> updateOptions(String entityType, String fieldName, List<FieldOptionDTO> options) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode array = mapper.createArrayNode();
        for (FieldOptionDTO opt : options) {
            ObjectNode node = mapper.createObjectNode();
            node.put("value", opt.getValue());
            node.put("label", opt.getLabel());
            array.add(node);
        }

        EntityFieldOptions row = repository
                .findByTenantIdAndEntityTypeIgnoreCaseAndFieldNameIgnoreCase(tenantId, entityType, fieldName)
                .orElseGet(() -> EntityFieldOptions.builder()
                        .tenantId(tenantId)
                        .entityType(entityType)
                        .fieldName(fieldName)
                        .build());

        row.setOptions(array);
        repository.save(row);
        return options;
    }

    private List<FieldOptionDTO> toList(EntityFieldOptions row) {
        List<FieldOptionDTO> list = new ArrayList<>();
        if (row.getOptions() != null && row.getOptions().isArray()) {
            for (var node : row.getOptions()) {
                list.add(FieldOptionDTO.builder()
                        .value(node.path("value").asString(""))
                        .label(node.path("label").asString(""))
                        .build());
            }
        }
        return list;
    }
}
