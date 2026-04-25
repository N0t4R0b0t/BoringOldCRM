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
package com.bocrm.backend.service;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.entity.UserGroup;
import com.bocrm.backend.entity.UserGroupMembership;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.UserGroupMembershipRepository;
import com.bocrm.backend.repository.UserGroupRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
/**
 * UserGroupService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGroupService {

    private final UserGroupRepository groupRepository;
    private final UserGroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    // ─── Group CRUD ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserGroupDTO> listGroups() {
        Long tenantId = requireTenantContext();
        return groupRepository.findByTenantId(tenantId).stream()
                .map(g -> toDTO(g, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserGroupDTO getGroup(Long groupId) {
        Long tenantId = requireTenantContext();
        UserGroup group = requireGroup(groupId, tenantId);
        return toDTO(group, true);
    }

    @Transactional
    public UserGroupDTO createGroup(CreateUserGroupRequest request) {
        Long tenantId = requireTenantContext();
        requireAdminOrManager();

        groupRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(g -> {
            throw new IllegalArgumentException("A group with that name already exists");
        });

        UserGroup group = groupRepository.save(UserGroup.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .build());
        log.debug("Created user group '{}' for tenant {}", group.getName(), tenantId);
        return toDTO(group, false);
    }

    @Transactional
    public UserGroupDTO updateGroup(Long groupId, UpdateUserGroupRequest request) {
        Long tenantId = requireTenantContext();
        requireAdminOrManager();
        UserGroup group = requireGroup(groupId, tenantId);

        if (request.getName() != null && !request.getName().equals(group.getName())) {
            groupRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(g -> {
                throw new IllegalArgumentException("A group with that name already exists");
            });
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        return toDTO(groupRepository.save(group), false);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        Long tenantId = requireTenantContext();
        requireAdminOrManager();
        UserGroup group = requireGroup(groupId, tenantId);
        membershipRepository.findByGroupIdAndTenantId(groupId, tenantId)
                .forEach(membershipRepository::delete);
        groupRepository.delete(group);
        log.debug("Deleted user group {} for tenant {}", groupId, tenantId);
    }

    // ─── Membership ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserGroupMemberDTO> listMembers(Long groupId) {
        Long tenantId = requireTenantContext();
        UserGroup group = requireGroup(groupId, tenantId);
        return membershipRepository.findByGroupIdAndTenantId(group.getId(), tenantId).stream()
                .map(m -> {
                    User user = userRepository.findById(m.getUserId()).orElse(null);
                    return UserGroupMemberDTO.builder()
                            .userId(m.getUserId())
                            .userEmail(user != null ? user.getEmail() : null)
                            .displayName(user != null ? user.getDisplayName() : null)
                            .joinedAt(m.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public UserGroupDTO addMember(Long groupId, Long userId) {
        Long tenantId = requireTenantContext();
        requireAdminOrManager();
        UserGroup group = requireGroup(groupId, tenantId);

        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            membershipRepository.save(UserGroupMembership.builder()
                    .tenantId(tenantId)
                    .groupId(groupId)
                    .userId(userId)
                    .build());
        }
        return toDTO(group, true);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        Long tenantId = requireTenantContext();
        requireAdminOrManager();
        requireGroup(groupId, tenantId);
        membershipRepository.deleteByGroupIdAndUserId(groupId, userId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Long requireTenantContext() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        return tenantId;
    }

    private UserGroup requireGroup(Long groupId, Long tenantId) {
        return groupRepository.findByIdAndTenantId(groupId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    private void requireAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SYSTEM_ADMIN".equals(a.getAuthority())
                        || "ROLE_MANAGER".equals(a.getAuthority()));
        if (!allowed) throw new ForbiddenException("Admin or Manager role required");
    }

    private UserGroupDTO toDTO(UserGroup group, boolean includeMembers) {
        Long tenantId = group.getTenantId();
        List<UserGroupMembership> memberships = membershipRepository
                .findByGroupIdAndTenantId(group.getId(), tenantId);

        List<UserGroupMemberDTO> memberDTOs = null;
        if (includeMembers) {
            memberDTOs = memberships.stream()
                    .map(m -> {
                        User user = userRepository.findById(m.getUserId()).orElse(null);
                        return UserGroupMemberDTO.builder()
                                .userId(m.getUserId())
                                .userEmail(user != null ? user.getEmail() : null)
                                .displayName(user != null ? user.getDisplayName() : null)
                                .joinedAt(m.getCreatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return UserGroupDTO.builder()
                .id(group.getId())
                .tenantId(group.getTenantId())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(memberships.size())
                .members(memberDTOs)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
