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
import com.bocrm.backend.service.BulkOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * BulkOperationsController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/bulk")
@Tag(name = "Bulk Operations", description = "Bulk update and delete operations")
@Slf4j
public class BulkOperationsController {
    private final BulkOperationsService bulkOperationsService;

    public BulkOperationsController(BulkOperationsService bulkOperationsService) {
        this.bulkOperationsService = bulkOperationsService;
    }

    @PostMapping("/customers/update")
    @Operation(summary = "Bulk update customers")
    public ResponseEntity<BulkOperationResponse> bulkUpdateCustomers(@RequestBody BulkUpdateRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkUpdateCustomers(request));
    }

    @PostMapping("/opportunities/update")
    @Operation(summary = "Bulk update opportunities")
    public ResponseEntity<BulkOperationResponse> bulkUpdateOpportunities(@RequestBody BulkUpdateRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkUpdateOpportunities(request));
    }

    @PostMapping("/contacts/update")
    @Operation(summary = "Bulk update contacts")
    public ResponseEntity<BulkOperationResponse> bulkUpdateContacts(@RequestBody BulkUpdateRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkUpdateContacts(request));
    }

    @PostMapping("/activities/delete")
    @Operation(summary = "Bulk delete activities")
    public ResponseEntity<BulkOperationResponse> bulkDeleteActivities(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteActivities(request));
    }

    @PostMapping("/customers/delete")
    @Operation(summary = "Bulk delete customers")
    public ResponseEntity<BulkOperationResponse> bulkDeleteCustomers(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteCustomers(request));
    }

    @PostMapping("/contacts/delete")
    @Operation(summary = "Bulk delete contacts")
    public ResponseEntity<BulkOperationResponse> bulkDeleteContacts(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteContacts(request));
    }

    @PostMapping("/opportunities/delete")
    @Operation(summary = "Bulk delete opportunities")
    public ResponseEntity<BulkOperationResponse> bulkDeleteOpportunities(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteOpportunities(request));
    }

    @PostMapping("/custom-records/delete")
    @Operation(summary = "Bulk delete customRecords")
    public ResponseEntity<BulkOperationResponse> bulkDeleteCustomRecords(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteCustomRecords(request));
    }

    @PostMapping("/documents/delete")
    @Operation(summary = "Bulk delete documents")
    public ResponseEntity<BulkOperationResponse> bulkDeleteDocuments(@RequestBody BulkDeleteRequest request) {
        return ResponseEntity.ok(bulkOperationsService.bulkDeleteDocuments(request));
    }

    @PostMapping("/customers/create")
    @Operation(summary = "Bulk create customers")
    public ResponseEntity<BulkOperationResponse> bulkCreateCustomers(@RequestBody List<CreateCustomerRequest> records) {
        return ResponseEntity.ok(bulkOperationsService.bulkCreateCustomers(records));
    }

    @PostMapping("/contacts/create")
    @Operation(summary = "Bulk create contacts")
    public ResponseEntity<BulkOperationResponse> bulkCreateContacts(@RequestBody List<CreateContactRequest> records) {
        return ResponseEntity.ok(bulkOperationsService.bulkCreateContacts(records));
    }

    @PostMapping("/opportunities/create")
    @Operation(summary = "Bulk create opportunities")
    public ResponseEntity<BulkOperationResponse> bulkCreateOpportunities(@RequestBody List<CreateOpportunityRequest> records) {
        return ResponseEntity.ok(bulkOperationsService.bulkCreateOpportunities(records));
    }

    @PostMapping("/activities/create")
    @Operation(summary = "Bulk create activities")
    public ResponseEntity<BulkOperationResponse> bulkCreateActivities(@RequestBody List<CreateActivityRequest> records) {
        return ResponseEntity.ok(bulkOperationsService.bulkCreateActivities(records));
    }

    @PostMapping("/custom-records/create")
    @Operation(summary = "Bulk create custom records")
    public ResponseEntity<BulkOperationResponse> bulkCreateCustomRecords(@RequestBody List<CreateCustomRecordRequest> records) {
        return ResponseEntity.ok(bulkOperationsService.bulkCreateCustomRecords(records));
    }
}
