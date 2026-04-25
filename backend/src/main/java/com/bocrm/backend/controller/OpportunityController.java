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
import com.bocrm.backend.service.OpportunityService;
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
 * OpportunityController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/opportunities")
@Tag(name = "Opportunities", description = "Opportunity management")
@Slf4j
public class OpportunityController {
    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping
    @Operation(summary = "Create a new opportunity")
    public ResponseEntity<OpportunityDTO> createOpportunity(@Valid @RequestBody CreateOpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.createOpportunity(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity by ID")
    public ResponseEntity<OpportunityDTO> getOpportunity(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getOpportunity(id));
    }

        @GetMapping("/search")
    @Operation(summary = "Search opportunities by text")
    public ResponseEntity<PagedResponse<OpportunityDTO>> searchOpportunities(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(opportunityService.search(term, page, size));
    }

    @GetMapping
    @Operation(summary = "List all opportunities")
    public ResponseEntity<PagedResponse<OpportunityDTO>> listOpportunities(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) List<String> stage,
        @RequestParam(required = false) String typeSlug,
        @RequestParam Map<String, List<String>> allParams
    ) {
        // Extract custom field filters (cf_*)
        Map<String, List<String>> customFieldFilters = allParams.entrySet().stream()
            .filter(e -> e.getKey().startsWith("cf_"))
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().substring(3),
                Map.Entry::getValue
            ));
        return ResponseEntity.ok(opportunityService.listOpportunities(page, size, sortBy, sortOrder, search, customerId, stage, typeSlug, customFieldFilters));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an opportunity")
    public ResponseEntity<OpportunityDTO> updateOpportunity(@PathVariable Long id, @Valid @RequestBody UpdateOpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an opportunity")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    @Operation(summary = "List contacts associated with an opportunity")
    public ResponseEntity<PagedResponse<ContactDTO>> listContactsByOpportunity(@PathVariable Long id) {
        var contacts = opportunityService.listContactsByOpportunity(id);
        return ResponseEntity.ok(PagedResponse.<ContactDTO>builder()
                .content(contacts)
                .totalElements((long) contacts.size())
                .totalPages(1)
                .currentPage(0)
                .pageSize(contacts.size())
                .hasNext(false)
                .hasPrev(false)
                .build());
    }

    @PostMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Add a contact to an opportunity")
    public ResponseEntity<Void> addContactToOpportunity(@PathVariable Long id, @PathVariable Long contactId) {
        opportunityService.addContactToOpportunity(id, contactId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Remove a contact from an opportunity")
    public ResponseEntity<Void> removeContactFromOpportunity(@PathVariable Long id, @PathVariable Long contactId) {
        opportunityService.removeContactFromOpportunity(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
