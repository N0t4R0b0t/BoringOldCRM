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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.service.CustomFieldDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * CustomFieldDefinitionController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/custom-fields")
@Tag(name = "Custom Fields", description = "Custom field management")
@Slf4j
public class CustomFieldDefinitionController {
    private final CustomFieldDefinitionService fieldDefinitionService;

    public CustomFieldDefinitionController(CustomFieldDefinitionService fieldDefinitionService) {
        this.fieldDefinitionService = fieldDefinitionService;
    }

    @PostMapping
    @Operation(summary = "Create a new custom field definition")
    public ResponseEntity<CustomFieldDefinitionDTO> createFieldDefinition(@Valid @RequestBody CreateCustomFieldDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fieldDefinitionService.createFieldDefinition(request));
    }

    @GetMapping
    @Operation(summary = "List custom field definitions for entity type")
    public ResponseEntity<List<CustomFieldDefinitionDTO>> getFieldDefinitions(@RequestParam String entityType) {
        return ResponseEntity.ok(fieldDefinitionService.getFieldDefinitions(entityType));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a custom field definition")
    public ResponseEntity<CustomFieldDefinitionDTO> updateFieldDefinition(@PathVariable Long id, @Valid @RequestBody UpdateCustomFieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldDefinitionService.updateFieldDefinition(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a custom field definition")
    public ResponseEntity<Void> deleteFieldDefinition(@PathVariable Long id) {
        fieldDefinitionService.deleteFieldDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    @Operation(summary = "Reorder custom field definitions")
    public ResponseEntity<Void> reorderFieldDefinitions(@RequestBody List<Long> orderedIds) {
        fieldDefinitionService.reorderFieldDefinitions(orderedIds);
        return ResponseEntity.noContent().build();
    }
}
