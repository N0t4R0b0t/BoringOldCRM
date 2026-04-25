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

import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enqueues notification OutboxEvents within the current tenant transaction.
 * The poller (NotificationPoller) picks them up asynchronously and dispatches email + inbox.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class NotificationService {

    public static final String RECORD_MODIFIED       = "RECORD_MODIFIED";
    public static final String OWNERSHIP_ASSIGNED    = "OWNERSHIP_ASSIGNED";
    public static final String ACCESS_GRANTED        = "ACCESS_GRANTED";
    public static final String ACTIVITY_DUE_SOON     = "ACTIVITY_DUE_SOON";
    public static final String DAILY_INSIGHT         = "DAILY_INSIGHT";
    public static final String CUSTOM_MESSAGE        = "CUSTOM_MESSAGE";
    static final String EVENT_TYPE                   = "NOTIFICATION_EMAIL";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Notify the owner of a record that someone else modified it. No-op if actor == owner. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyRecordModified(Long tenantId, Long actorId, String actorName,
                                     Long ownerId, String entityType, Long entityId, String entityName) {
        if (ownerId == null || ownerId.equals(actorId)) return;
        enqueue(tenantId, buildPayload(RECORD_MODIFIED, ownerId, actorId, actorName,
                entityType, entityId, entityName, null, null));
    }

    /** Notify a user that they have been assigned ownership of a record. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyOwnershipAssigned(Long tenantId, Long newOwnerId, Long actorId, String actorName,
                                        String entityType, Long entityId, String entityName) {
        if (newOwnerId == null || newOwnerId.equals(actorId)) return;
        enqueue(tenantId, buildPayload(OWNERSHIP_ASSIGNED, newOwnerId, actorId, actorName,
                entityType, entityId, entityName, null, null));
    }

    /** Notify a user that they were granted access to a record (USER grantee only). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyAccessGranted(Long tenantId, Long recipientUserId, Long actorId, String actorName,
                                    String entityType, Long entityId, String entityName, String permission) {
        if (recipientUserId == null || recipientUserId.equals(actorId)) return;
        enqueue(tenantId, buildPayload(ACCESS_GRANTED, recipientUserId, actorId, actorName,
                entityType, entityId, entityName, permission, null));
    }

    /** Enqueue an activity due-soon reminder. Called by the scheduler (no actor). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyActivityDueSoon(Long tenantId, Long ownerId, Long entityId,
                                      String subject, LocalDateTime dueAt) {
        if (ownerId == null) return;
        enqueue(tenantId, buildPayload(ACTIVITY_DUE_SOON, ownerId, null, null,
                "Activity", entityId, subject, null, dueAt));
    }

    /** Enqueue a daily insight notification for a user. Called by the scheduler. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyDailyInsight(Long tenantId, Long userId, String insightText) {
        if (userId == null) return;
        ObjectNode payload = buildPayload(DAILY_INSIGHT, userId, null, null,
                null, null, null, null, null);
        payload.put("insightText", insightText);
        enqueue(tenantId, payload);
    }

    /** Enqueue custom user-composed notifications to a list of recipients. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyCustomMessage(Long tenantId, Long actorId, String actorName,
                                    List<Long> recipientUserIds,
                                    String entityType, Long entityId, String entityName,
                                    String subject, String body) {
        if (recipientUserIds == null || recipientUserIds.isEmpty() || actorId == null) return;

        for (Long recipientId : recipientUserIds) {
            ObjectNode payload = buildPayload(CUSTOM_MESSAGE, recipientId, actorId, actorName,
                    entityType, entityId, entityName, null, null);
            payload.put("customSubject", subject);
            payload.put("customBody", body);
            enqueue(tenantId, payload);
        }
    }

    // ─── internals ───────────────────────────────────────────────────────────

    private void enqueue(Long tenantId, ObjectNode payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .tenantId(tenantId)
                    .eventType(EVENT_TYPE)
                    .payloadJsonb(objectMapper.writeValueAsString(payload))
                    .build();
            OutboxEvent saved = outboxEventRepository.save(event);
            log.debug("Notification enqueued — event id: {}, type: {}", saved.getId(), payload.path("notificationType").asText());
        } catch (Exception e) {
            log.error("Failed to enqueue notification for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    private ObjectNode buildPayload(String notificationType, Long recipientUserId,
                                    Long actorUserId, String actorDisplayName,
                                    String entityType, Long entityId, String entityName,
                                    String permission, LocalDateTime dueAt) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("notificationType", notificationType);
        if (recipientUserId != null)  node.put("recipientUserId", recipientUserId);
        if (actorUserId != null)      node.put("actorUserId", actorUserId);
        if (actorDisplayName != null) node.put("actorDisplayName", actorDisplayName);
        if (entityType != null)       node.put("entityType", entityType);
        if (entityId != null)         node.put("entityId", entityId);
        if (entityName != null)       node.put("entityName", entityName);
        if (permission != null)       node.put("permission", permission);
        if (dueAt != null)            node.put("dueAt", dueAt.toString());
        node.put("retryCount", 0);
        return node;
    }
}
