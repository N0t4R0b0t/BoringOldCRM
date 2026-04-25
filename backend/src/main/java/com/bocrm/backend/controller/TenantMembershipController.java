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

import com.bocrm.backend.dto.AddUserToTenantRequest;
import com.bocrm.backend.dto.TenantMembershipDTO;
import com.bocrm.backend.service.TenantMembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * TenantMembershipController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/admin/tenants/{tenantId}/users")
@Tag(name = "Tenant Membership", description = "Manage users in tenants")
@Slf4j
public class TenantMembershipController {
    private final TenantMembershipService membershipService;

    public TenantMembershipController(TenantMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    @Operation(summary = "List users in a tenant")
    public ResponseEntity<List<TenantMembershipDTO>> listUsers(@PathVariable Long tenantId) {
        return ResponseEntity.ok(membershipService.listTenantUsers(tenantId));
    }

    @PostMapping
    @Operation(summary = "Add user to tenant by user ID", description = "Requires userId (not email) to handle multiple users with same email address")
    public ResponseEntity<TenantMembershipDTO> addUser(@PathVariable Long tenantId, @RequestBody AddUserToTenantRequest request) {
        return ResponseEntity.ok(membershipService.addUserToTenant(tenantId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove user from tenant")
    public ResponseEntity<Void> removeUser(@PathVariable Long tenantId, @PathVariable Long userId) {
        membershipService.removeUserFromTenant(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
