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

import com.bocrm.backend.dto.AvailableAiModelDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * AiModelDiscoveryService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class AiModelDiscoveryService {

    @Value("${spring.ai.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${spring.ai.google.genai.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiModelDiscoveryService(HttpClient lenientHttpClient, ObjectMapper objectMapper) {
        this.httpClient = lenientHttpClient;
        this.objectMapper = objectMapper;
    }

    public List<AvailableAiModelDTO> listAvailableModels(String provider) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "anthropic" -> listAnthropicModels();
            case "google" -> listGeminiModels();
            case "openai" -> listOpenAiModels();
            case "ollama" -> listOllamaModels();
            default -> List.of();
        };
    }

    private List<AvailableAiModelDTO> listAnthropicModels() {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return List.of();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/models?limit=100"))
                    .header("X-Api-Key", anthropicApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Anthropic API returned status {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<AvailableAiModelDTO> models = new ArrayList<>();

            JsonNode dataArray = root.path("data");
            for (JsonNode modelNode : dataArray) {
                String id = modelNode.path("id").asText("");
                if (!id.isBlank()) {
                    models.add(AvailableAiModelDTO.builder()
                            .modelId(id)
                            .hasQuota(null)
                            .build());
                }
            }

            return models;
        } catch (Exception e) {
            log.warn("Failed to list Anthropic models", e);
            return List.of();
        }
    }

    private List<AvailableAiModelDTO> listGeminiModels() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return List.of();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + geminiApiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini API returned status {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<AvailableAiModelDTO> models = new ArrayList<>();

            JsonNode modelsArray = root.path("models");
            for (JsonNode modelNode : modelsArray) {
                JsonNode supportedMethods = modelNode.path("supportedGenerationMethods");
                boolean supportsGenerateContent = false;
                for (JsonNode method : supportedMethods) {
                    if ("generateContent".equals(method.asText())) {
                        supportsGenerateContent = true;
                        break;
                    }
                }

                if (supportsGenerateContent) {
                    String name = modelNode.path("name").asText("");
                    if (name.startsWith("models/")) {
                        name = name.substring(7);
                    }

                    if (!name.isBlank()) {
                        // Check if the model supports text input modality
                        JsonNode inputModalities = modelNode.path("inputModalities");
                        boolean supportsText = false;

                        if (inputModalities.isArray() && inputModalities.size() > 0) {
                            for (JsonNode modality : inputModalities) {
                                String modalityText = modality.asText();
                                if ("text".equals(modalityText)) {
                                    supportsText = true;
                                    break;
                                }
                            }
                            log.debug("Model {} has inputModalities: {}", name, inputModalities);
                        } else {
                            // If inputModalities is missing or empty, assume text is supported
                            // (most Gemini models support text by default)
                            supportsText = true;
                            log.debug("Model {} has no inputModalities field, assuming text support", name);
                        }

                        if (supportsText) {
                            // Check if the API key has quota for this model
                            Boolean hasQuota = checkGeminiQuota(name);
                            models.add(AvailableAiModelDTO.builder()
                                    .modelId(name)
                                    .hasQuota(hasQuota)
                                    .build());
                        }
                    }
                }
            }

            models.sort((a, b) -> a.getModelId().compareTo(b.getModelId()));
            return models;
        } catch (Exception e) {
            log.warn("Failed to list Gemini models", e);
            return List.of();
        }
    }

    private Boolean checkGeminiQuota(String modelId) {
        // Quota checking requires Google Cloud credentials and Cloud Monitoring API
        // The free API key does not have access to quota metrics
        // Return null to indicate quota status is unknown without proper GCP credentials
        log.debug("Quota checking for Gemini requires Google Cloud credentials - returning unknown");
        return null;
    }

    private List<AvailableAiModelDTO> listOpenAiModels() {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return List.of();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/models"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("OpenAI API returned status {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<AvailableAiModelDTO> models = new ArrayList<>();

            JsonNode dataArray = root.path("data");
            for (JsonNode modelNode : dataArray) {
                String id = modelNode.path("id").asText("");
                if (id.startsWith("gpt")) {
                    // For OpenAI, we don't have a direct quota check, so mark as unknown
                    models.add(AvailableAiModelDTO.builder()
                            .modelId(id)
                            .hasQuota(null)  // OpenAI doesn't expose quota info in list endpoint
                            .build());
                }
            }

            models.sort((a, b) -> a.getModelId().compareTo(b.getModelId()));
            return models;
        } catch (Exception e) {
            log.warn("Failed to list OpenAI models", e);
            return List.of();
        }
    }

    private List<AvailableAiModelDTO> listOllamaModels() {
        try {
            String url = ollamaBaseUrl + "/api/tags";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ollama API returned status {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<AvailableAiModelDTO> models = new ArrayList<>();

            JsonNode modelsArray = root.path("models");
            for (JsonNode modelNode : modelsArray) {
                String name = modelNode.path("name").asText("");
                if (!name.isBlank()) {
                    models.add(AvailableAiModelDTO.builder()
                            .modelId(name)
                            .hasQuota(null)  // Ollama doesn't expose quota info
                            .build());
                }
            }

            models.sort((a, b) -> a.getModelId().compareTo(b.getModelId()));
            return models;
        } catch (Exception e) {
            log.warn("Failed to list Ollama models from {}", ollamaBaseUrl, e);
            return List.of();
        }
    }
}
