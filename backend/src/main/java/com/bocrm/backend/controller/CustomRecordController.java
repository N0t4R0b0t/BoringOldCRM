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
import com.bocrm.backend.service.CustomRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * CustomRecordController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@Tag(name = "CustomRecords", description = "CustomRecord management")
@Slf4j
public class CustomRecordController {

    private final CustomRecordService customRecordService;

    public CustomRecordController(CustomRecordService customRecordService) {
        this.customRecordService = customRecordService;
    }

    @PostMapping("/custom-records")
    @Operation(summary = "Create a new customRecord")
    public ResponseEntity<CustomRecordDTO> createCustomRecord(@Valid @RequestBody CreateCustomRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customRecordService.createCustomRecord(request));
    }

    @GetMapping("/custom-records/{id}")
    @Operation(summary = "Get customRecord by ID")
    public ResponseEntity<CustomRecordDTO> getCustomRecord(@PathVariable Long id) {
        return ResponseEntity.ok(customRecordService.getCustomRecord(id));
    }

    @GetMapping("/custom-records")
    @Operation(summary = "List all customRecords")
    public ResponseEntity<PagedResponse<CustomRecordDTO>> listCustomRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(customRecordService.listCustomRecords(page, size, sortBy, sortOrder, search, status, customerId));
    }

    @GetMapping("/custom-records/search")
    @Operation(summary = "Search customRecords by text")
    public ResponseEntity<PagedResponse<CustomRecordDTO>> searchCustomRecords(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(customRecordService.search(term, page, size));
    }

    @PutMapping("/custom-records/{id}")
    @Operation(summary = "Update an customRecord")
    public ResponseEntity<CustomRecordDTO> updateCustomRecord(@PathVariable Long id, @Valid @RequestBody UpdateCustomRecordRequest request) {
        return ResponseEntity.ok(customRecordService.updateCustomRecord(id, request));
    }

    @DeleteMapping("/custom-records/{id}")
    @Operation(summary = "Delete an customRecord")
    public ResponseEntity<Void> deleteCustomRecord(@PathVariable Long id) {
        customRecordService.deleteCustomRecord(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/custom-records/{id}/opportunities/{opportunityId}")
    @Operation(summary = "Link an customRecord to an opportunity")
    public ResponseEntity<Void> linkOpportunity(@PathVariable Long id, @PathVariable Long opportunityId) {
        customRecordService.linkOpportunity(id, opportunityId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/custom-records/{id}/opportunities/{opportunityId}")
    @Operation(summary = "Unlink an customRecord from an opportunity")
    public ResponseEntity<Void> unlinkOpportunity(@PathVariable Long id, @PathVariable Long opportunityId) {
        customRecordService.unlinkOpportunity(id, opportunityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/opportunities/{oppId}/customRecords")
    @Operation(summary = "Get customRecords linked to an opportunity")
    public ResponseEntity<List<CustomRecordDTO>> listByOpportunity(@PathVariable Long oppId) {
        return ResponseEntity.ok(customRecordService.listByOpportunity(oppId));
    }
}
