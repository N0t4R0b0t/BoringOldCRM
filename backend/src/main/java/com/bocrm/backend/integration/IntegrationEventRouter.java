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

import com.bocrm.backend.entity.IntegrationConfig;
import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.repository.IntegrationConfigRepository;
import com.bocrm.backend.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Routes CRM events from OutboxEvent to enabled integration adapters.
 * Matches event types against configured integration subscriptions.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class IntegrationEventRouter {
    private final IntegrationConfigRepository integrationConfigRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final Set<IntegrationAdapter> adapters;

    public IntegrationEventRouter(
            IntegrationConfigRepository integrationConfigRepository,
            EncryptionService encryptionService,
            ObjectMapper objectMapper,
            Set<IntegrationAdapter> adapters) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.adapters = adapters;
    }

    /**
     * Route a CRM event to matching enabled integrations.
     *
     * @param event The outbox event with eventType="CRM_EVENT"
     */
    @SuppressWarnings("deprecation")
    public void route(OutboxEvent event) {
        try {
            // Parse the CRM event payload
            JsonNode payload = objectMapper.readTree(event.getPayloadJsonb());
            String entityType = payload.get("entityType").asText();
            Long entityId = payload.get("entityId").asLong();
            String action = payload.get("action").asText();
            JsonNode entityData = payload.get("data");
            Long tenantId = event.getTenantId();

            // Construct the event key (e.g., "CUSTOMER_CREATED")
            String eventKey = entityType.toUpperCase() + "_" + action.toUpperCase();

            // Find enabled integrations for this tenant
            List<IntegrationConfig> enabledConfigs = integrationConfigRepository.findByTenantIdAndEnabled(tenantId, true);

            for (IntegrationConfig config : enabledConfigs) {
                // Check if this config subscribes to this event type
                if (shouldProcessEvent(config, eventKey)) {
                    IntegrationAdapter adapter = findAdapter(config.getAdapterType());
                    if (adapter != null) {
                        try {
                            // Decrypt credentials
                            Map<String, String> credentials = decryptCredentials(config.getCredentialsEncrypted());

                            // Process the event
                            adapter.process(event.getEventType(), entityType, entityId, action, entityData, credentials);
                        } catch (Exception e) {
                            log.error("Error processing event in adapter {}: {}", config.getAdapterType(), e.getMessage(), e);
                            // Don't throw - continue with other adapters
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error routing CRM event: {}", e.getMessage(), e);
        }
    }

    private boolean shouldProcessEvent(IntegrationConfig config, String eventKey) {
        // If eventTypes is empty, process all events
        if (config.getEventTypes() == null || config.getEventTypes().isEmpty()) {
            return true;
        }

        // Check if event is in the comma-separated list
        String[] subscribedEvents = config.getEventTypes().split(",");
        for (String event : subscribedEvents) {
            if (event.trim().equalsIgnoreCase(eventKey)) {
                return true;
            }
        }
        return false;
    }

    private IntegrationAdapter findAdapter(String adapterType) {
        return adapters.stream()
                .filter(adapter -> adapter.name().equals(adapterType))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> decryptCredentials(String encryptedJson) {
        Map<String, String> credentials = new HashMap<>();
        try {
            if (encryptedJson == null || encryptedJson.isEmpty()) {
                return credentials;
            }

            String decryptedJson = encryptionService.decrypt(encryptedJson);
            Map<String, Object> credentialsMap = (Map<String, Object>) objectMapper.readValue(decryptedJson, Map.class);

            // Convert to Map<String, String>
            for (Map.Entry<String, Object> entry : credentialsMap.entrySet()) {
                Object value = entry.getValue();
                credentials.put(entry.getKey(), value != null ? value.toString() : "");
            }
        } catch (Exception e) {
            log.error("Failed to decrypt credentials: {}", e.getMessage());
        }
        return credentials;
    }
}
