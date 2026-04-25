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
import com.bocrm.backend.service.ContactService;
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
 * ContactController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/contacts")
@Tag(name = "Contacts", description = "Contact management")
@Slf4j
public class ContactController {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ContactDTO> createContact(@Valid @RequestBody CreateContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID")
    public ResponseEntity<ContactDTO> getContact(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContact(id));
    }

        @GetMapping
        @Operation(summary = "List all contacts")
        public ResponseEntity<PagedResponse<ContactDTO>> listContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Boolean hasEmail,
            @RequestParam(required = false) List<String> status,
            @RequestParam Map<String, List<String>> allParams
        ) {
        // Extract custom field filters (cf_*)
        Map<String, List<String>> customFieldFilters = allParams.entrySet().stream()
            .filter(e -> e.getKey().startsWith("cf_"))
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().substring(3),
                Map.Entry::getValue
            ));
        return ResponseEntity.ok(contactService.listContacts(page, size, sortBy, sortOrder, search, customerId, hasEmail, status, customFieldFilters));
        }

    @GetMapping("/search")
    @Operation(summary = "Search contacts by text")
    public ResponseEntity<PagedResponse<ContactDTO>> searchContacts(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contactService.search(term, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contact")
    public ResponseEntity<ContactDTO> updateContact(@PathVariable Long id, @Valid @RequestBody UpdateContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/disassociate")
    @Operation(summary = "Disassociate a contact from its customer")
    public ResponseEntity<ContactDTO> disassociateContact(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.disassociateContact(id));
    }
}
