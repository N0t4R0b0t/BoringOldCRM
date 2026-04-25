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

import com.bocrm.backend.dto.CreateOrderRequest;
import com.bocrm.backend.dto.OrderDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateOrderRequest;
import com.bocrm.backend.service.OrderService;
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
 * OrderController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management — create, read, update, delete orders linked to customers")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create a new order", description = "Creates a new order for a customer. Returns 201 with the created order details.")
    @ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating order for customer {}", request.getCustomerId());
        OrderDTO created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get order by ID", description = "Retrieves a single order by its ID. Returns 200 with order details or 404 if not found.")
    @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        log.info("Fetching order {}", id);
        OrderDTO order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "List orders", description = "Lists all orders in the current tenant with pagination. Supports sorting and filtering.")
    @ApiResponse(responseCode = "200", description = "List of orders (may be empty)", content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @GetMapping
    public ResponseEntity<PagedResponse<OrderDTO>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        log.info("Listing orders: page={}, size={}, sortBy={}, sortOrder={}", page, size, sortBy, sortOrder);
        PagedResponse<OrderDTO> orders = orderService.listOrders(page, size, sortBy, sortOrder);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Search orders", description = "Text search across order fields (status, payment terms, etc.)")
    @ApiResponse(responseCode = "200", description = "Search results (may be empty)", content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @ApiResponse(responseCode = "403", description = "Tenant context not set")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<OrderDTO>> searchOrders(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Searching orders with term: {}", term);
        PagedResponse<OrderDTO> results = orderService.search(term, page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Update an order", description = "Updates an existing order with partial or full fields. Null fields are ignored.")
    @ApiResponse(responseCode = "200", description = "Order updated successfully", content = @Content(schema = @Schema(implementation = OrderDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long id,
            @RequestBody UpdateOrderRequest request) {
        log.info("Updating order {}", id);
        OrderDTO updated = orderService.updateOrder(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete an order", description = "Deletes an order permanently. Returns 204 No Content on success.")
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Deleting order {}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
