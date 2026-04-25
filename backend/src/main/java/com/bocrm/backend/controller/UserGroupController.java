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

import com.bocrm.backend.dto.CreateUserGroupRequest;
import com.bocrm.backend.dto.UpdateUserGroupRequest;
import com.bocrm.backend.dto.UserGroupDTO;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.service.UserGroupService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * UserGroupController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/groups")
@Tag(name = "User Groups", description = "Manage user groups for access control")
@Slf4j
public class UserGroupController {

    private final UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @GetMapping
    @Operation(summary = "List user groups", description = "Get all user groups in the current tenant")
    public ResponseEntity<List<UserGroupDTO>> listGroups() {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        List<UserGroupDTO> groups = userGroupService.listGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user group", description = "Get details of a specific group with members")
    public ResponseEntity<UserGroupDTO> getGroup(
            @Parameter(description = "Group ID")
            @PathVariable Long id) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        UserGroupDTO group = userGroupService.getGroup(id);
        return ResponseEntity.ok(group);
    }

    @PostMapping
    @Operation(summary = "Create a new user group", description = "Create a new group for bulk access granting")
    public ResponseEntity<UserGroupDTO> createGroup(@RequestBody CreateUserGroupRequest request) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        UserGroupDTO group = userGroupService.createGroup(request);
        return ResponseEntity.status(201).body(group);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user group", description = "Update group name and description")
    public ResponseEntity<UserGroupDTO> updateGroup(
            @Parameter(description = "Group ID")
            @PathVariable Long id,
            @RequestBody UpdateUserGroupRequest request) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        UserGroupDTO group = userGroupService.updateGroup(id, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user group", description = "Remove a group and all its grants")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Group ID")
            @PathVariable Long id) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        userGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List group members", description = "Get members of a group")
    public ResponseEntity<List<?>> listGroupMembers(
            @Parameter(description = "Group ID")
            @PathVariable Long id) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        return ResponseEntity.ok(userGroupService.getGroup(id).getMembers());
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add user to group", description = "Add a user to a group")
    public ResponseEntity<UserGroupDTO> addGroupMember(
            @Parameter(description = "Group ID")
            @PathVariable Long id,
            @RequestBody AddGroupMemberRequest request) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        UserGroupDTO group = userGroupService.addMember(id, request.getUserId());
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove user from group", description = "Remove a user from a group")
    public ResponseEntity<Void> removeGroupMember(
            @Parameter(description = "Group ID")
            @PathVariable Long id,
            @Parameter(description = "User ID")
            @PathVariable Long userId) {
        if (TenantContext.getTenantId() == null) {
            throw new ForbiddenException("Tenant context not set");
        }
        userGroupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Request DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddGroupMemberRequest {
        private Long userId;
    }
}
