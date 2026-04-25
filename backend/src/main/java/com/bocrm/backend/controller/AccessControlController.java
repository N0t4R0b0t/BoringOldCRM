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

import com.bocrm.backend.dto.AccessGrantDTO;
import com.bocrm.backend.dto.RecordAccessSummaryDTO;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.service.AccessControlService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
/**
 * AccessControlController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/access")
@Tag(name = "Access Control", description = "Row-level record access control")
@Slf4j
public class AccessControlController {

    private final AccessControlService accessControlService;

    public AccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String role = authority.getAuthority();
                if (role.startsWith("ROLE_")) {
                    return role.substring(5).toLowerCase();
                }
            }
        }
        return "user";
    }

    @GetMapping("/{entityType}/{entityId}/summary")
    @Operation(summary = "Get access summary for a record", description = "Fetch visibility mode, owner, grants, and canManage flag")
    public ResponseEntity<RecordAccessSummaryDTO> getAccessSummary(
            @Parameter(description = "Entity type (CustomRecord, Opportunity, Activity, TenantDocument)")
            @PathVariable String entityType,
            @Parameter(description = "Entity ID")
            @PathVariable Long entityId,
            @Parameter(description = "Owner ID of the entity (used to compute canManage)")
            @RequestParam(required = false) Long ownerId) {
        Long userId = TenantContext.getUserId();
        Long tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        String role = getCurrentRole();
        RecordAccessSummaryDTO summary = accessControlService.getSummary(userId, role, tenantId, entityType, entityId, ownerId);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{entityType}/{entityId}/policy")
    @Operation(summary = "Set access policy for a record", description = "Set visibility mode to READ_ONLY or HIDDEN")
    public ResponseEntity<Void> setAccessPolicy(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestBody SetAccessPolicyRequest request) {
        Long userId = TenantContext.getUserId();
        Long tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        String role = getCurrentRole();
        // Service will look up ownerId from policy or throw 403
        accessControlService.setPolicy(userId, role, tenantId, entityType, entityId, null, request.getAccessMode());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{entityType}/{entityId}/policy")
    @Operation(summary = "Remove access policy (revert to OPEN)", description = "Delete visibility restriction")
    public ResponseEntity<Void> removeAccessPolicy(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        Long userId = TenantContext.getUserId();
        Long tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        String role = getCurrentRole();
        // Service will look up ownerId from policy or throw 403
        accessControlService.removePolicy(userId, role, tenantId, entityType, entityId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entityType}/{entityId}/grants")
    @Operation(summary = "Grant access to a user or group", description = "Add user or group grant with READ or WRITE permission")
    public ResponseEntity<AccessGrantDTO> addAccessGrant(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestBody AddAccessGrantRequest request) {
        Long userId = TenantContext.getUserId();
        Long tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        String role = getCurrentRole();
        // Service will look up ownerId from policy or throw 403
        AccessGrantDTO grant = accessControlService.addGrant(
                userId, role, tenantId, entityType, entityId, null,
                request.getGranteeType(), request.getGranteeId(), request.getPermission());
        return ResponseEntity.status(201).body(grant);
    }

    @DeleteMapping("/{entityType}/{entityId}/grants/{grantId}")
    @Operation(summary = "Remove access grant", description = "Revoke user or group access")
    public ResponseEntity<Void> removeAccessGrant(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PathVariable Long grantId) {
        Long userId = TenantContext.getUserId();
        Long tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        String role = getCurrentRole();
        // Service will look up ownerId from policy or throw 403
        accessControlService.removeGrant(userId, role, tenantId, entityType, entityId, null, grantId);
        return ResponseEntity.noContent().build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetAccessPolicyRequest {
        private String accessMode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddAccessGrantRequest {
        private String granteeType;
        private Long granteeId;
        private String permission;
    }
}
