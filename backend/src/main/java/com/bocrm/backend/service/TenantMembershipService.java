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

import com.bocrm.backend.dto.AddUserToTenantRequest;
import com.bocrm.backend.dto.TenantMembershipDTO;
import com.bocrm.backend.entity.TenantMembership;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
/**
 * TenantMembershipService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class TenantMembershipService {
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TenantMembershipService(TenantMembershipRepository membershipRepository, UserRepository userRepository,
                                   AuditLogService auditLogService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TenantMembershipDTO addUserToTenant(Long tenantId, AddUserToTenantRequest request) {
        Long currentUserId = TenantContext.getUserId();
        requireAdmin(currentUserId, tenantId);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        if (membershipRepository.findByTenantIdAndUserId(tenantId, user.getId()).isPresent()) {
            throw new ValidationException("User is already a member of this tenant");
        }

        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenantId)
                .userId(user.getId())
                .role(request.getRole() != null ? request.getRole() : "user")
                .status("active")
                .build();

        TenantMembership saved = membershipRepository.save(membership);
        auditLogService.logAction(currentUserId, "ADD_USER_TO_TENANT", "TenantMembership", saved.getId(), request);

        return toDTO(saved);
    }

    @Transactional
    public void removeUserFromTenant(Long tenantId, Long userId) {
        Long currentUserId = TenantContext.getUserId();
        requireAdmin(currentUserId, tenantId);

        TenantMembership membership = membershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membershipRepository.delete(membership);
        auditLogService.logAction(currentUserId, "REMOVE_USER_FROM_TENANT", "TenantMembership", membership.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipDTO> listTenantUsers(Long tenantId) {
        Long currentUserId = TenantContext.getUserId();

        // Allow system_admin to bypass membership check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSystemAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a));

        if (!isSystemAdmin && membershipRepository.findByTenantIdAndUserId(tenantId, currentUserId).isEmpty()) {
             throw new ForbiddenException("Access denied");
        }

        return membershipRepository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private void requireAdmin(Long userId, Long tenantId) {
        if (userId == null) throw new ForbiddenException("Not authenticated");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSystemAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a));

        // System admin can bypass tenant context check
        if (!isSystemAdmin) {
            Long currentTenantId = TenantContext.getTenantId();
            if (currentTenantId == null || !tenantId.equals(currentTenantId)) {
                throw new ForbiddenException("Access denied: tenant mismatch");
            }

            boolean isAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .anyMatch(a -> "ROLE_ADMIN".equals(a));

            if (!isAdmin) {
                throw new ForbiddenException("Access denied: Admin role required");
            }
        }
    }

    private TenantMembershipDTO toDTO(TenantMembership membership) {
        User user = userRepository.findById(membership.getUserId()).orElse(null);
        return TenantMembershipDTO.builder()
                .id(membership.getId())
                .tenantId(membership.getTenantId())
                .userId(membership.getUserId())
                .userEmail(user != null ? user.getEmail() : "Unknown")
                .userName(user != null && user.getDisplayName() != null ? user.getDisplayName() : (user != null ? user.getEmail() : "Unknown"))
                .role(membership.getRole())
                .status(membership.getStatus())
                .createdAt(membership.getCreatedAt())
                .oauthProvider(user != null ? user.getOauthProvider() : null)
                .oauthId(user != null ? user.getOauthId() : null)
                .build();
    }
}
