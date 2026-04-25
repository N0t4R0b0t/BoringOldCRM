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
package com.bocrm.backend.util;

import com.bocrm.backend.config.OpaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Low-level HTTP client for the Open Policy Agent REST API.
 * Communicates with the OPA sidecar at the configured URL.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
public class OpaClient {

    private static final Logger log = LoggerFactory.getLogger(OpaClient.class);

    private final OpaProperties opaProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpaClient(OpaProperties opaProperties, ObjectMapper objectMapper) {
        this.opaProperties = opaProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Push a Rego policy document to OPA.
     * The policyId uses slash path segments, e.g. "bocrm/tenant_7/Customer".
     */
    public void putPolicy(String policyId, String regoDocument) {
        String url = opaProperties.getUrl() + "/v1/policies/" + policyId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "text/plain")
                    .PUT(HttpRequest.BodyPublishers.ofString(regoDocument))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("OPA PUT policy failed [" + response.statusCode() + "]: " + response.body());
            }
            log.debug("OPA policy synced: {}", policyId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OPA putPolicy failed for '" + policyId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Delete a policy from OPA. Silently ignores 404 (policy already absent).
     */
    public void deletePolicy(String policyId) {
        String url = opaProperties.getUrl() + "/v1/policies/" + policyId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                throw new RuntimeException("OPA DELETE policy failed [" + response.statusCode() + "]: " + response.body());
            }
            log.debug("OPA policy removed: {}", policyId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OPA deletePolicy failed for '" + policyId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Validate a Rego expression body by test-compiling it against OPA.
     * Builds a minimal Rego document, pushes it to a throwaway policy ID, and
     * immediately deletes it. Throws {@link IllegalArgumentException} with a
     * human-readable message extracted from OPA's error response if invalid.
     */
    public void validateExpression(String expression) {
        String tempId = "bocrm/validate/" + java.util.UUID.randomUUID();
        String tempRego = "package bocrm.validate\nimport rego.v1\n\n" +
                "deny contains v if {\n    input.operation == \"CREATE\"\n" +
                "    " + expression.replace("\n", "\n    ") + "\n" +
                "    v := {\"ruleId\": 0, \"ruleName\": \"test\", \"message\": \"test\", \"severity\": \"DENY\"}\n}\n";
        String url = opaProperties.getUrl() + "/v1/policies/" + tempId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "text/plain")
                    .PUT(HttpRequest.BodyPublishers.ofString(tempRego))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400) {
                String message = extractOpaErrorMessage(response.body());
                throw new IllegalArgumentException("Invalid Rego expression: " + message);
            }
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("OPA rejected expression [" + response.statusCode() + "]: " + response.body());
            }
            // clean up the throwaway policy
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();
            httpClient.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not validate expression against OPA (OPA may be unavailable): {}", e.getMessage());
            // If OPA is unreachable, allow saving — syncGroup will log the error later
        }
    }

    private String extractOpaErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errors = root.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                JsonNode first = errors.get(0);
                String msg = first.path("message").asString("");
                JsonNode details = first.path("details");
                String line = details.isMissingNode() ? null : details.path("line").asString(null);
                return line != null ? msg + " — `" + line.trim() + "`" : msg;
            }
            String msg = root.path("message").asString(null);
            return msg != null ? msg : responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    /**
     * Evaluate a policy data path and return the result node.
     * The dataPath uses slash path segments, e.g. "bocrm/tenant_7/Customer".
     * The input map is wrapped as {"input": ...} per the OPA Data API spec.
     *
     * @return the "result" JsonNode from the OPA response, or null if absent (no rules defined)
     */
    public JsonNode evaluateData(String dataPath, Map<String, Object> input) {
        String url = opaProperties.getUrl() + "/v1/data/" + dataPath;
        try {
            Map<String, Object> body = Map.of("input", input);
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("OPA evaluate failed [" + response.statusCode() + "]: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("result");  // returns MissingNode if absent — callers check isMissingNode()
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OPA evaluateData failed for '" + dataPath + "': " + e.getMessage(), e);
        }
    }
}
