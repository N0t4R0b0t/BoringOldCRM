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

import com.bocrm.backend.dto.TenantMembershipDTO;
import com.bocrm.backend.dto.UpdatePreferencesRequest;
import com.bocrm.backend.dto.UserDTO;
import com.bocrm.backend.service.TenantMembershipService;
import com.bocrm.backend.service.UserService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * UserController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "User management")
@Slf4j
public class UserController {
    private final UserService userService;
    private final TenantMembershipService membershipService;

    public UserController(UserService userService, TenantMembershipService membershipService) {
        this.userService = userService;
        this.membershipService = membershipService;
    }

    @GetMapping("/team")
    @Operation(summary = "List all members of the current tenant")
    public ResponseEntity<List<TenantMembershipDTO>> getTeamMembers() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(membershipService.listTenantUsers(tenantId));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<UserDTO> updatePreferences(@RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(request));
    }
}
