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
package com.bocrm.backend.controller;

import com.bocrm.backend.dto.FieldOptionDTO;
import com.bocrm.backend.service.EntityFieldOptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
/**
 * EntityFieldOptionsController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/entity-field-options")
@Tag(name = "Entity Field Options", description = "Tenant-configurable option lists for core entity fields")
@Slf4j
public class EntityFieldOptionsController {

    private final EntityFieldOptionsService entityFieldOptionsService;

    public EntityFieldOptionsController(EntityFieldOptionsService entityFieldOptionsService) {
        this.entityFieldOptionsService = entityFieldOptionsService;
    }

    @GetMapping("/{entityType}")
    @Operation(summary = "Get all configured field options for an entity type")
    public ResponseEntity<Map<String, List<FieldOptionDTO>>> getOptions(@PathVariable String entityType) {
        return ResponseEntity.ok(entityFieldOptionsService.getOptionsForEntity(entityType));
    }

    @GetMapping("/{entityType}/{fieldName}")
    @Operation(summary = "Get options for a specific field")
    public ResponseEntity<List<FieldOptionDTO>> getFieldOptions(
            @PathVariable String entityType,
            @PathVariable String fieldName) {
        return ResponseEntity.ok(entityFieldOptionsService.getOptionsForField(entityType, fieldName));
    }

    @PutMapping("/{entityType}/{fieldName}")
    @Operation(summary = "Replace the option list for a specific field")
    public ResponseEntity<List<FieldOptionDTO>> updateOptions(
            @PathVariable String entityType,
            @PathVariable String fieldName,
            @RequestBody List<FieldOptionDTO> options) {
        return ResponseEntity.ok(entityFieldOptionsService.updateOptions(entityType, fieldName, options));
    }
}
