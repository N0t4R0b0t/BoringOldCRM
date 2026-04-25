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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Generic Webhook integration adapter.
 * Posts CRM event payloads to any HTTP endpoint, optionally with HMAC-SHA256 signature.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class WebhookAdapter implements IntegrationAdapter {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "webhook";
    }

    @Override
    public Set<String> supportedEventTypes() {
        // Webhook supports all event types
        return Collections.emptySet();
    }

    @Override
    public void process(String crmEventType, String entityType, Long entityId, String action, JsonNode entityData, Map<String, String> credentials) throws Exception {
        String url = credentials.get("url");
        if (url == null || url.isEmpty()) {
            log.warn("Webhook adapter: missing url credential");
            return;
        }

        // Build full event payload
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("eventType", crmEventType);
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);
        payload.put("action", action);
        payload.set("data", entityData);

        String json = MAPPER.writeValueAsString(payload);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        // Build request with optional HMAC signature
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        String secret = credentials.get("secret");
        if (secret != null && !secret.isEmpty()) {
            String signature = computeHmacSha256(secret, json);
            requestBuilder.header("X-Signature", "sha256=" + signature);
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Webhook returned status {}: {}", response.statusCode(), response.body());
            throw new Exception("Webhook failed with status " + response.statusCode());
        }

        log.info("Webhook adapter: sent event for {} {} (ID: {})", entityType, action, entityId);
    }

    private String computeHmacSha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }
}
