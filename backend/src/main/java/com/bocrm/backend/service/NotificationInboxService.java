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

import com.bocrm.backend.dto.NotificationInboxDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.entity.NotificationInbox;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.NotificationInboxRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages in-app notifications for users.
 * Resolves all user IDs sharing the same email so that notifications sent to any
 * account variant (local, OAuth) are visible regardless of which account is active.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class NotificationInboxService {

    private final NotificationInboxRepository inboxRepository;
    private final UserRepository userRepository;

    public NotificationInboxService(NotificationInboxRepository inboxRepository,
                                    UserRepository userRepository) {
        this.inboxRepository = inboxRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves all user IDs that share the same email as the current user.
     * This handles multi-account scenarios (local + OAuth) where the same person
     * has multiple user records with the same email.
     */
    private Set<Long> resolveUserIds(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return Set.of(userId);
        return userRepository.findAllByEmail(user.getEmail()).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Get paginated inbox for current user (across all accounts sharing the same email).
     */
    @Transactional(readOnly = true)
    public PagedResponse<NotificationInboxDTO> getInbox(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        if (userId == null) throw new ForbiddenException("User context not set");

        Set<Long> userIds = resolveUserIds(userId);
        Pageable pageable = buildPageable(page, size);
        Page<NotificationInbox> result = inboxRepository.findByTenantIdAndUserIdIn(tenantId, userIds, pageable);

        List<NotificationInboxDTO> content = result.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PagedResponse.<NotificationInboxDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    /**
     * Get count of unread notifications for current user (across all accounts sharing the same email).
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        if (userId == null) throw new ForbiddenException("User context not set");

        Set<Long> userIds = resolveUserIds(userId);
        return inboxRepository.countByTenantIdAndUserIdInAndReadAtIsNull(tenantId, userIds);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public NotificationInboxDTO markRead(Long notificationId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        if (userId == null) throw new ForbiddenException("User context not set");

        NotificationInbox notification = inboxRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Verify ownership
        if (!notification.getTenantId().equals(tenantId) || !notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            inboxRepository.save(notification);
            log.debug("Notification {} marked as read for user {}", notificationId, userId);
        }

        return toDTO(notification);
    }

    /**
     * Mark all unread notifications as read for current user (across all accounts sharing the same email).
     */
    @Transactional
    public void markAllRead() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        if (userId == null) throw new ForbiddenException("User context not set");

        Set<Long> userIds = resolveUserIds(userId);
        inboxRepository.markAllReadByTenantIdAndUserIdIn(tenantId, userIds);
        log.debug("All notifications marked as read for user {} (resolved {} accounts)", userId, userIds.size());
    }

    // ─── internals ─────────────────────────────────────────────────────────

    private NotificationInboxDTO toDTO(NotificationInbox notification) {
        return NotificationInboxDTO.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .actorUserId(notification.getActorUserId())
                .actorDisplayName(notification.getActorDisplayName())
                .message(notification.getMessage())
                .read(notification.getReadAt() != null)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
