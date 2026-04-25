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
package com.bocrm.backend.integration;

import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Publishes CRM events to the outbox for async integration processing.
 * Called from CRM services on entity create/update/delete.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class IntegrationEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public IntegrationEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a CRM event to the outbox.
     * The event will be processed by IntegrationPoller and routed to enabled integrations.
     *
     * @param tenantId       The tenant ID
     * @param entityType     The CRM entity type (e.g., "Customer", "Opportunity")
     * @param entityId       The entity ID
     * @param action         The action (e.g., "CREATED", "UPDATED", "DELETED")
     * @param entityData     The entity DTO snapshot (any object will be converted to JSON)
     */
    @Transactional
    public void publish(Long tenantId, String entityType, Long entityId, String action, Object entityData) {
        try {
            // Convert entityData to JsonNode
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("entityType", entityType);
            payload.put("entityId", entityId);
            payload.put("action", action);
            payload.put("tenantId", tenantId);
            payload.set("data", objectMapper.valueToTree(entityData));

            // Create outbox event
            OutboxEvent event = OutboxEvent.builder()
                    .tenantId(tenantId)
                    .eventType("CRM_EVENT")
                    .payloadJsonb(objectMapper.writeValueAsString(payload))
                    .publishedAt(null)  // null means unpublished, to be picked up by poller
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Published CRM_EVENT: {} {} (ID: {}) for tenant {}", entityType, action, entityId, tenantId);
        } catch (Exception e) {
            log.error("Failed to publish CRM event for {} {}", entityType, entityId, e);
            // Don't throw - allow the primary operation to succeed even if event publication fails
        }
    }
}
