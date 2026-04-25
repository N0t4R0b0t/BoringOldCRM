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

import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.SystemStatsDTO;
import com.bocrm.backend.dto.SystemUserDTO;
import com.bocrm.backend.dto.UserMembershipDTO;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
/**
 * SystemAdminService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class SystemAdminService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository tenantMembershipRepository;

    public SystemAdminService(UserRepository userRepository,
                              TenantRepository tenantRepository,
                              TenantMembershipRepository tenantMembershipRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
    }

    public SystemStatsDTO getSystemStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return SystemStatsDTO.builder()
                .totalTenants(tenantRepository.count())
                .activeTenants(tenantRepository.countByStatus("active"))
                .inactiveTenants(tenantRepository.countByStatus("inactive"))
                .newTenantsLast30Days(tenantRepository.countByCreatedAtAfter(thirtyDaysAgo))
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus("active"))
                .inactiveUsers(userRepository.countByStatus("inactive"))
                .newUsersLast30Days(userRepository.countByCreatedAtAfter(thirtyDaysAgo))
                .totalMemberships(tenantMembershipRepository.count())
                .build();
    }

    public PagedResponse<SystemUserDTO> listUsers(int page, int size, String search) {
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by("createdAt").descending());

        Page<User> userPage;
        if (search != null && !search.isBlank()) {
            userPage = userRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                    search, search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // N+1 for membershipCount is acceptable on this low-traffic admin endpoint.
        // Future optimization: replace with a GROUP BY JPQL projection query.
        List<SystemUserDTO> content = userPage.getContent().stream()
                .map(u -> SystemUserDTO.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .displayName(u.getDisplayName())
                        .status(u.getStatus())
                        .createdAt(u.getCreatedAt())
                        .membershipCount(tenantMembershipRepository.countByUserId(u.getId()))
                        .oauthProvider(u.getOauthProvider())
                        .oauthId(u.getOauthId())
                        .build())
                .toList();

        return PagedResponse.<SystemUserDTO>builder()
                .content(content)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(page)
                .pageSize(cappedSize)
                .hasNext(userPage.hasNext())
                .hasPrev(userPage.hasPrevious())
                .build();
    }

    public List<UserMembershipDTO> getUserMemberships(Long userId) {
        return tenantMembershipRepository.findByUserId(userId).stream()
                .map(m -> UserMembershipDTO.builder()
                        .membershipId(m.getId())
                        .tenantId(m.getTenantId())
                        .tenantName(m.getTenant() != null ? m.getTenant().getName() : null)
                        .role(m.getRole())
                        .status(m.getStatus())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
    }

    public SystemUserDTO updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(status);
        user = userRepository.save(user);
        log.info("System admin updated user {} status to {}", userId, status);
        return SystemUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .membershipCount(tenantMembershipRepository.countByUserId(user.getId()))
                .oauthProvider(user.getOauthProvider())
                .oauthId(user.getOauthId())
                .build();
    }

    public SystemUserDTO inviteUser(String email, String displayName) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        // Create new user (no password, no OAuth - they'll log in via Auth0 or set password later)
        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .passwordHash(null)
                .oauthProvider(null)
                .oauthId(null)
                .status("active")
                .build();

        user = userRepository.save(user);
        log.info("System admin invited user {} ({})", email, displayName);

        // Note: Email sending would go here if implemented
        // notificationService.sendInvitationEmail(email, displayName);

        return SystemUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .membershipCount(0)
                .oauthProvider(null)
                .oauthId(null)
                .build();
    }
}
