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
import com.bocrm.backend.service.BulkOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * BulkOperationController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/bulk")
@Tag(name = "Bulk Operations", description = "Batch operations on entities")
@Slf4j
public class BulkOperationController {
    private final BulkOperationService bulkOperationService;

    public BulkOperationController(BulkOperationService bulkOperationService) {
        this.bulkOperationService = bulkOperationService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute a bulk operation")
    public ResponseEntity<BulkOperationResponse> executeBulkOperation(@RequestBody BulkOperationRequest request) {
        return ResponseEntity.ok(bulkOperationService.executeBulkOperation(request));
    }
}
