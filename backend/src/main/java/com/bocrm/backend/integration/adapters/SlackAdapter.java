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
package com.bocrm.backend.integration.adapters;

import com.bocrm.backend.integration.IntegrationAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Slack integration adapter.
 * Sends a formatted message to a Slack incoming webhook when CRM events occur.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class SlackAdapter implements IntegrationAdapter {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "slack";
    }

    @Override
    public Set<String> supportedEventTypes() {
        // Slack supports all event types
        return Collections.emptySet();
    }

    @Override
    public void process(String crmEventType, String entityType, Long entityId, String action, JsonNode entityData, Map<String, String> credentials) throws Exception {
        String webhookUrl = credentials.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack adapter: missing webhookUrl credential");
            return;
        }

        // Extract entity name from entityData (varies by entity type)
        String entityName = extractEntityName(entityType, entityData);
        String text = String.format("%s '%s' was %s in BOCRM (ID: %d)", entityType, entityName, action, entityId);

        // Build Slack message payload
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("text", text);

        String json = MAPPER.writeValueAsString(payload);

        // Send POST to webhook URL
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Slack webhook returned status {}: {}", response.statusCode(), response.body());
            throw new Exception("Slack webhook failed with status " + response.statusCode());
        }

        log.info("Slack adapter: sent message for {} {} (ID: {})", entityType, action, entityId);
    }

    @SuppressWarnings("deprecation")
    private String extractEntityName(String entityType, JsonNode entityData) {
        // Try common name fields by entity type
        if (entityData.has("name")) {
            try {
                return entityData.get("name").asText();
            } catch (Exception e) {
                return "Unknown";
            }
        } else if (entityData.has("firstName") && entityData.has("lastName")) {
            try {
                String firstName = entityData.get("firstName").asText();
                String lastName = entityData.get("lastName").asText();
                return (firstName + " " + lastName).trim();
            } catch (Exception e) {
                return "Unknown";
            }
        }
        return "Unknown";
    }
}
