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

import com.bocrm.backend.dto.CalculatedFieldDefinitionDTO;
import com.bocrm.backend.dto.CreateCalculatedFieldDefinitionRequest;
import com.bocrm.backend.dto.UpdateCalculatedFieldDefinitionRequest;
import com.bocrm.backend.service.CalculatedFieldDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
/**
 * CalculatedFieldDefinitionController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/calculated-fields")
@Tag(name = "Calculated Fields", description = "Calculated field definitions and evaluation")
@Slf4j
public class CalculatedFieldDefinitionController {

    private final CalculatedFieldDefinitionService definitionService;

    public CalculatedFieldDefinitionController(CalculatedFieldDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @GetMapping("/definitions")
    @Operation(summary = "List calculated field definitions for entity type")
    public ResponseEntity<List<CalculatedFieldDefinitionDTO>> getDefinitions(@RequestParam String entityType) {
        return ResponseEntity.ok(definitionService.getDefinitions(entityType));
    }

    @PostMapping("/definitions")
    @Operation(summary = "Create a calculated field definition")
    public ResponseEntity<CalculatedFieldDefinitionDTO> createDefinition(
            @Valid @RequestBody CreateCalculatedFieldDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(definitionService.createDefinition(request));
    }

    @PutMapping("/definitions/{id}")
    @Operation(summary = "Update a calculated field definition")
    public ResponseEntity<CalculatedFieldDefinitionDTO> updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCalculatedFieldDefinitionRequest request) {
        return ResponseEntity.ok(definitionService.updateDefinition(id, request));
    }

    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "Delete a calculated field definition")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        definitionService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/definitions/reorder")
    @Operation(summary = "Reorder calculated field definitions")
    public ResponseEntity<Void> reorderDefinitions(@RequestBody List<Long> orderedIds) {
        definitionService.reorderDefinitions(orderedIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/evaluate")
    @Operation(summary = "Evaluate all calculated fields for a specific entity")
    public ResponseEntity<Map<String, Object>> evaluate(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(definitionService.evaluateForEntity(entityType, entityId));
    }
}
