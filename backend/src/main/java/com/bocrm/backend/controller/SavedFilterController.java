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

import com.bocrm.backend.dto.SavedFilterDTO;
import com.bocrm.backend.service.SavedFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * SavedFilterController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/filters")
@Tag(name = "Saved Filters", description = "Manage saved search filters")
@Slf4j
public class SavedFilterController {
    private final SavedFilterService savedFilterService;

    public SavedFilterController(SavedFilterService savedFilterService) {
        this.savedFilterService = savedFilterService;
    }

    @PostMapping
    @Operation(summary = "Create a saved filter")
    public ResponseEntity<SavedFilterDTO> createFilter(@RequestBody SavedFilterDTO filter) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFilterService.createFilter(filter));
    }

    @GetMapping
    @Operation(summary = "List all saved filters")
    public ResponseEntity<List<SavedFilterDTO>> listFilters() {
        return ResponseEntity.ok(savedFilterService.listFilters());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get saved filter by ID")
    public ResponseEntity<SavedFilterDTO> getFilter(@PathVariable Long id) {
        return ResponseEntity.ok(savedFilterService.getFilter(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved filter")
    public ResponseEntity<Void> deleteFilter(@PathVariable Long id) {
        savedFilterService.deleteFilter(id);
        return ResponseEntity.noContent().build();
    }
}
