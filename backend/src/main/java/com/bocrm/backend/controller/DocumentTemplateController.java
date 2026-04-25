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

import com.bocrm.backend.dto.CreateDocumentTemplateRequest;
import com.bocrm.backend.dto.DocumentTemplateDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateDocumentTemplateRequest;
import com.bocrm.backend.service.DocumentTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * DocumentTemplateController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/document-templates")
@Tag(name = "Document Templates", description = "Manage reusable document generation templates")
@Slf4j
public class DocumentTemplateController {

    private final DocumentTemplateService documentTemplateService;

    public DocumentTemplateController(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    @PostMapping
    @Operation(summary = "Create a new document template")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DocumentTemplateDTO> create(@RequestBody CreateDocumentTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentTemplateService.createTemplate(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document template by ID")
    public ResponseEntity<DocumentTemplateDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentTemplateService.getTemplate(id));
    }

    @GetMapping
    @Operation(summary = "List document templates with optional filtering and pagination")
    public ResponseEntity<PagedResponse<DocumentTemplateDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String templateType) {
        return ResponseEntity.ok(documentTemplateService.listTemplates(page, size, sortBy, sortOrder, search, templateType));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document template")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DocumentTemplateDTO> update(@PathVariable Long id,
                                                       @RequestBody UpdateDocumentTemplateRequest request) {
        return ResponseEntity.ok(documentTemplateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document template")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone a document template")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DocumentTemplateDTO> clone(@PathVariable Long id,
                                                      @RequestParam(required = false) String name) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentTemplateService.cloneTemplate(id, name));
    }
}
