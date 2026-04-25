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

import com.bocrm.backend.dto.ComposeNotificationRequest;
import com.bocrm.backend.dto.NotificationInboxDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UnreadCountResponse;
import com.bocrm.backend.service.NotificationInboxService;
import com.bocrm.backend.service.NotificationService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * REST endpoints for notification management.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "User notification management")
@Slf4j
public class NotificationController {

    private final NotificationInboxService notificationInboxService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationController(NotificationInboxService notificationInboxService,
                                 NotificationService notificationService,
                                 ObjectMapper objectMapper) {
        this.notificationInboxService = notificationInboxService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * List user's notifications with pagination.
     */
    @GetMapping("/inbox")
    @Operation(summary = "List user's notifications")
    public ResponseEntity<PagedResponse<NotificationInboxDTO>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationInboxService.getInbox(page, size));
    }

    /**
     * Get count of unread notifications.
     */
    @GetMapping("/inbox/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        long count = notificationInboxService.getUnreadCount();
        return ResponseEntity.ok(UnreadCountResponse.builder().unreadCount(count).build());
    }

    /**
     * Mark a single notification as read.
     */
    @PostMapping("/inbox/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationInboxDTO> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationInboxService.markRead(id));
    }

    /**
     * Mark all unread notifications as read.
     */
    @PostMapping("/inbox/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead() {
        notificationInboxService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user's notification preferences from their user.preferences JSONB.
     */
    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<ObjectNode> getPreferences() {
        // This is a placeholder endpoint that returns empty preferences.
        // In a real implementation, this would load from the current user's preferences field.
        ObjectNode prefs = objectMapper.createObjectNode();
        ObjectNode notifications = objectMapper.createObjectNode();

        // Default preference structure
        notifications.put("recordModified", true);
        notifications.put("ownershipAssigned", true);
        notifications.put("accessGranted", true);
        notifications.put("activityDueSoon", true);
        notifications.put("dailyInsight", true);
        notifications.put("muted", false);

        prefs.set("notifications", notifications);

        return ResponseEntity.ok(prefs);
    }

    /**
     * Update current user's notification preferences in their user.preferences JSONB.
     */
    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<ObjectNode> updatePreferences(@RequestBody ObjectNode preferences) {
        // This is a placeholder endpoint that echoes back the preferences.
        // In a real implementation, this would save to the current user's preferences field
        // and mark the user as updated.
        log.debug("Notification preferences updated for current user");
        return ResponseEntity.ok(preferences);
    }

    /**
     * Compose and send custom notifications to recipients.
     */
    @PostMapping("/compose")
    @Operation(summary = "Send custom notification about an entity")
    public ResponseEntity<Void> composeNotification(@RequestBody ComposeNotificationRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            throw new IllegalStateException("Tenant context not set");
        }

        // Validate request
        if (request.getRecipientUserIds() == null || request.getRecipientUserIds().isEmpty()) {
            throw new IllegalArgumentException("recipientUserIds cannot be empty");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new IllegalArgumentException("body cannot be blank");
        }

        notificationService.notifyCustomMessage(
            tenantId,
            userId,
            "User", // actor name - will be user's email or display name in a real implementation
            request.getRecipientUserIds(),
            request.getEntityType(),
            request.getEntityId(),
            request.getEntityName(),
            request.getSubject(),
            request.getBody()
        );

        return ResponseEntity.noContent().build();
    }
}
