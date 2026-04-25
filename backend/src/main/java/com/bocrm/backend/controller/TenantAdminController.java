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

import com.bocrm.backend.dto.TenantDTO;
import com.bocrm.backend.service.TenantAdminService;
import tools.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import com.bocrm.backend.shared.TenantContext;
/**
 * TenantAdminController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/admin/tenants")
public class TenantAdminController {
    private final TenantAdminService tenantAdminService;

    public TenantAdminController(TenantAdminService tenantAdminService) {
        this.tenantAdminService = tenantAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<List<TenantDTO>> listTenants(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(tenantAdminService.listTenants(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable Long id) {
        TenantDTO dto = tenantAdminService.getTenant(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<TenantDTO> createTenant(@RequestBody CreateTenantRequest request) {
        return ResponseEntity.ok(tenantAdminService.createTenant(request.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<TenantDTO> updateTenant(@PathVariable Long id, @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantAdminService.updateTenant(id, request.getName(), request.getSettings()));
    }

    @GetMapping("/{id}/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<JsonNode> getSettings(@PathVariable Long id) {
        return ResponseEntity.ok(tenantAdminService.getSettings(id));
    }

    @PutMapping("/{id}/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<JsonNode> updateSettings(@PathVariable Long id, @RequestBody JsonNode settings) {
        tenantAdminService.updateTenant(id, null, settings);
        return ResponseEntity.ok(settings);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantAdminService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/current/logo")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<?> setLogoFromUrl(@RequestBody SetLogoRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return ResponseEntity.status(403).build();
        try {
            String dataUri = tenantAdminService.setLogoFromUrl(tenantId, request.getImageUrl());
            return ResponseEntity.ok(Map.of("logoUrl", dataUri));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to fetch logo: " + e.getMessage()));
        }
    }

    @PostMapping("/current/logo/upload")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return ResponseEntity.status(403).build();
        try {
            String dataUri = tenantAdminService.setLogoFromUpload(tenantId, file);
            return ResponseEntity.ok(Map.of("logoUrl", dataUri));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to process logo: " + e.getMessage()));
        }
    }

    // Expose the current tenant for tenant-level clients
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDTO> getCurrentTenant() {
        TenantDTO dto = tenantAdminService.getCurrentTenant();
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    // DTO request classes used by controller
    static class CreateTenantRequest {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static class UpdateTenantRequest {
        private String name;
        private JsonNode settings;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public JsonNode getSettings() { return settings; }
        public void setSettings(JsonNode settings) { this.settings = settings; }
    }

    static class SetLogoRequest {
        private String imageUrl;

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}
