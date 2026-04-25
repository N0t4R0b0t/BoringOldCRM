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
 * Zapier integration adapter.
 * Posts CRM event payloads to Zapier Catch Hook URLs to trigger workflows.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class ZapierAdapter implements IntegrationAdapter {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "zapier";
    }

    @Override
    public Set<String> supportedEventTypes() {
        // Zapier supports all event types
        return Collections.emptySet();
    }

    @Override
    public void process(String crmEventType, String entityType, Long entityId, String action, JsonNode entityData, Map<String, String> credentials) throws Exception {
        String webhookUrl = credentials.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Zapier adapter: missing webhookUrl credential");
            return;
        }

        // Build full event payload for Zapier
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("eventType", crmEventType);
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);
        payload.put("action", action);
        payload.set("data", entityData);

        String json = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Zapier webhook returned status {}: {}", response.statusCode(), response.body());
            throw new Exception("Zapier webhook failed with status " + response.statusCode());
        }

        log.info("Zapier adapter: sent event for {} {} (ID: {})", entityType, action, entityId);
    }
}
