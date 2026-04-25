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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HubSpot integration adapter.
 * Syncs Customer and Opportunity data to HubSpot via REST API v3.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class HubSpotAdapter implements IntegrationAdapter {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HUBSPOT_API_BASE = "https://api.hubapi.com";

    @Override
    public String name() {
        return "hubspot";
    }

    @Override
    public Set<String> supportedEventTypes() {
        Set<String> types = new HashSet<>();
        types.add("CUSTOMER_CREATED");
        types.add("CUSTOMER_UPDATED");
        types.add("CONTACT_CREATED");
        types.add("CONTACT_UPDATED");
        types.add("OPPORTUNITY_CREATED");
        types.add("OPPORTUNITY_UPDATED");
        return types;
    }

    @Override
    public void process(String crmEventType, String entityType, Long entityId, String action, JsonNode entityData, Map<String, String> credentials) throws Exception {
        String apiKey = credentials.get("apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("HubSpot adapter: missing apiKey credential");
            return;
        }

        // Only handle supported entity types
        if ("Customer".equals(entityType)) {
            syncCustomer(apiKey, entityId, action, entityData);
        } else if ("Contact".equals(entityType)) {
            syncContact(apiKey, entityId, action, entityData);
        } else if ("Opportunity".equals(entityType)) {
            syncOpportunity(apiKey, entityId, action, entityData);
        } else {
            log.debug("HubSpot adapter: ignoring unsupported entity type {}", entityType);
        }
    }

    @SuppressWarnings("deprecation")
    private void syncCustomer(String apiKey, Long entityId, String action, JsonNode entityData) throws Exception {
        // Map BOCRM customer to HubSpot company
        String companyName = entityData.has("name") ? entityData.get("name").asText() : "Unknown";
        String domain = entityData.has("website") ? entityData.get("website").asText() : "";

        ObjectNode properties = MAPPER.createObjectNode();
        properties.put("name", companyName);
        if (!domain.isEmpty()) {
            properties.put("domain", domain);
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("properties", properties);

        String endpoint = HUBSPOT_API_BASE + "/crm/v3/objects/companies";
        String json = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.warn("HubSpot company sync failed: {}", response.body());
            throw new Exception("HubSpot sync failed with status " + response.statusCode());
        }

        log.info("HubSpot adapter: synced customer {} as company", entityId);
    }

    @SuppressWarnings("deprecation")
    private void syncContact(String apiKey, Long entityId, String action, JsonNode entityData) throws Exception {
        String firstName = entityData.has("firstName") ? entityData.get("firstName").asText() : "";
        String lastName = entityData.has("lastName") ? entityData.get("lastName").asText() : "";
        String email = entityData.has("email") ? entityData.get("email").asText() : "";

        ObjectNode properties = MAPPER.createObjectNode();
        if (!firstName.isEmpty()) properties.put("firstname", firstName);
        if (!lastName.isEmpty()) properties.put("lastname", lastName);
        if (!email.isEmpty()) properties.put("email", email);

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("properties", properties);

        String endpoint = HUBSPOT_API_BASE + "/crm/v3/objects/contacts";
        String json = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.warn("HubSpot contact sync failed: {}", response.body());
            throw new Exception("HubSpot sync failed with status " + response.statusCode());
        }

        log.info("HubSpot adapter: synced contact {} to HubSpot", entityId);
    }

    @SuppressWarnings("deprecation")
    private void syncOpportunity(String apiKey, Long entityId, String action, JsonNode entityData) throws Exception {
        String dealName = entityData.has("name") ? entityData.get("name").asText() : "Unknown";
        Object amount = entityData.has("amount") ? entityData.get("amount").asText() : null;

        ObjectNode properties = MAPPER.createObjectNode();
        properties.put("dealname", dealName);
        if (amount != null) {
            properties.put("amount", amount.toString());
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("properties", properties);

        String endpoint = HUBSPOT_API_BASE + "/crm/v3/objects/deals";
        String json = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.warn("HubSpot deal sync failed: {}", response.body());
            throw new Exception("HubSpot sync failed with status " + response.statusCode());
        }

        log.info("HubSpot adapter: synced opportunity {} as deal", entityId);
    }
}
