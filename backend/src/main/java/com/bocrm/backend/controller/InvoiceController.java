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

import com.bocrm.backend.dto.CreateInvoiceRequest;
import com.bocrm.backend.dto.InvoiceDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateInvoiceRequest;
import com.bocrm.backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * InvoiceController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices", description = "Invoice management — create, read, update, delete invoices linked to orders or customers")
@Slf4j
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Create a new invoice", description = "Creates a new invoice for a customer. Returns 201 with the created invoice details.")
    @ApiResponse(responseCode = "201", description = "Invoice created successfully", content = @Content(schema = @Schema(implementation = InvoiceDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody CreateInvoiceRequest request) {
        log.info("Creating invoice for customer {}", request.getCustomerId());
        InvoiceDTO created = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get invoice by ID", description = "Retrieves a single invoice by its ID. Returns 200 with invoice details or 404 if not found.")
    @ApiResponse(responseCode = "200", description = "Invoice found", content = @Content(schema = @Schema(implementation = InvoiceDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable Long id) {
        log.info("Fetching invoice {}", id);
        InvoiceDTO invoice = invoiceService.getInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    @Operation(summary = "List invoices", description = "Lists all invoices in the current tenant with pagination. Supports sorting and filtering.")
    @ApiResponse(responseCode = "200", description = "List of invoices (may be empty)", content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @GetMapping
    public ResponseEntity<PagedResponse<InvoiceDTO>> listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        log.info("Listing invoices: page={}, size={}, sortBy={}, sortOrder={}", page, size, sortBy, sortOrder);
        PagedResponse<InvoiceDTO> invoices = invoiceService.listInvoices(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(invoices);
    }

    @Operation(summary = "Search invoices", description = "Text search across invoice fields (status, payment terms, etc.)")
    @ApiResponse(responseCode = "200", description = "Search results (may be empty)", content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<InvoiceDTO>> searchInvoices(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Searching invoices with term: {}", term);
        PagedResponse<InvoiceDTO> results = invoiceService.search(term, page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Update an invoice", description = "Updates an existing invoice with partial or full fields. Null fields are ignored.")
    @ApiResponse(responseCode = "200", description = "Invoice updated successfully", content = @Content(schema = @Schema(implementation = InvoiceDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(
            @PathVariable Long id,
            @RequestBody UpdateInvoiceRequest request) {
        log.info("Updating invoice {}", id);
        InvoiceDTO updated = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete an invoice", description = "Deletes an invoice permanently. Returns 204 No Content on success.")
    @ApiResponse(responseCode = "204", description = "Invoice deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        log.info("Deleting invoice {}", id);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
