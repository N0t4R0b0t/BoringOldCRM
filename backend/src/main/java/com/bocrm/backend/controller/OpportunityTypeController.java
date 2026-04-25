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

import com.bocrm.backend.dto.OpportunityTypeDTO;
import com.bocrm.backend.service.OpportunityTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * OpportunityTypeController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/opportunity-types")
@Tag(name = "Opportunity Types", description = "Manage custom opportunity type categories")
@Slf4j
public class OpportunityTypeController {

    private final OpportunityTypeService opportunityTypeService;

    public OpportunityTypeController(OpportunityTypeService opportunityTypeService) {
        this.opportunityTypeService = opportunityTypeService;
    }

    @GetMapping
    @Operation(summary = "List all opportunity types")
    public ResponseEntity<List<OpportunityTypeDTO>> getAll() {
        return ResponseEntity.ok(opportunityTypeService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new opportunity type")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<OpportunityTypeDTO> create(@Valid @RequestBody OpportunityTypeDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opportunityTypeService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an opportunity type")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<OpportunityTypeDTO> update(@PathVariable Long id,
                                                      @Valid @RequestBody OpportunityTypeDTO.UpdateRequest request) {
        return ResponseEntity.ok(opportunityTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an opportunity type")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        opportunityTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
