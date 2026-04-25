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

import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

/**
 * Interface for external integration adapters.
 * Implementations handle pushing CRM events to external systems (Slack, HubSpot, etc).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
public interface IntegrationAdapter {
    /**
     * Adapter name (e.g., "slack", "webhook", "hubspot", "zapier").
     */
    String name();

    /**
     * Set of CRM event types this adapter supports (e.g., {"CUSTOMER_CREATED", "OPPORTUNITY_UPDATED"}).
     * If empty, adapter handles all event types.
     */
    Set<String> supportedEventTypes();

    /**
     * Process a CRM event and push to the external system.
     *
     * @param crmEventType The event type from OutboxEvent (always "CRM_EVENT" for these adapters)
     * @param entityType   The CRM entity type (e.g., "Customer", "Opportunity")
     * @param entityId     The entity ID
     * @param action       The action (e.g., "CREATED", "UPDATED", "DELETED")
     * @param entityData   The entity DTO snapshot as JsonNode
     * @param credentials  Decrypted credentials map (e.g., {"webhookUrl": "https://hooks.slack.com/..."})
     * @throws Exception if the external system call fails
     */
    void process(String crmEventType, String entityType, Long entityId, String action, JsonNode entityData, Map<String, String> credentials) throws Exception;
}
