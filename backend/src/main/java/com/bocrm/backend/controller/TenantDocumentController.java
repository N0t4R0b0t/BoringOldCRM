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
import com.bocrm.backend.service.TenantDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
/**
 * TenantDocumentController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Tenant document storage")
@Slf4j
public class TenantDocumentController {

    private final TenantDocumentService documentService;

    public TenantDocumentController(TenantDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(summary = "Create a document")
    public ResponseEntity<TenantDocumentDTO> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file as a document")
    public ResponseEntity<TenantDocumentDTO> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String linkedEntityType,
            @RequestParam(required = false) Long linkedEntityId,
            @RequestParam(required = false) String linkedFieldKey) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = file.getContentType();
            long sizeBytes = file.getSize();

            CreateDocumentRequest req = CreateDocumentRequest.builder()
                    .name(name)
                    .mimeType(mimeType)
                    .contentBase64(base64)
                    .sizeBytes(sizeBytes)
                    .contentType(contentType != null ? contentType : "file")
                    .linkedEntityType(linkedEntityType)
                    .linkedEntityId(linkedEntityId)
                    .linkedFieldKey(linkedFieldKey)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(req));
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("Failed to process file upload: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "List documents")
    public ResponseEntity<PagedResponse<TenantDocumentDTO>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String linkedEntityType,
            @RequestParam(required = false) Long linkedEntityId,
            @RequestParam(required = false) String linkedFieldKey) {
        return ResponseEntity.ok(documentService.listDocuments(page, size, search, source, contentType, linkedEntityType, linkedEntityId, linkedFieldKey));
    }

    @PatchMapping("/{id}/rename")
    @Operation(summary = "Rename a document")
    public ResponseEntity<TenantDocumentDTO> renameDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(documentService.renameDocument(id, body.get("name")));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate a document")
    public ResponseEntity<TenantDocumentDTO> duplicateDocument(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.duplicateDocument(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata by ID")
    public ResponseEntity<TenantDocumentDTO> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document content")
    public ResponseEntity<DocumentDownloadDTO> downloadDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.downloadDocument(id));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Stream document as a binary file (signed URL; auth handled by DocumentDownloadTokenFilter).")
    public ResponseEntity<byte[]> streamDocument(@PathVariable Long id) {
        DocumentDownloadDTO dto = documentService.downloadDocument(id);
        byte[] bytes = dto.getContentBase64() != null
                ? Base64.getDecoder().decode(dto.getContentBase64())
                : new byte[0];
        MediaType mediaType = dto.getMimeType() != null
                ? MediaType.parseMediaType(dto.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(dto.getName() != null ? dto.getName() : ("document-" + id), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
