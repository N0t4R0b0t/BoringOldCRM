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

import com.bocrm.backend.dto.OnboardingSuggestionsDTO;
import com.bocrm.backend.dto.OnboardingSuggestionsRequest;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates AI-powered onboarding suggestions for new tenants.
 * Uses lightweight models (haiku/gpt-4o-mini/gemini-flash) to minimize token spend.
 * Never throws exceptions—always returns a DTO, with empty arrays on LLM failure.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class OnboardingService {

    private final ChatModelRegistry chatModelRegistry;
    private final TokenQuotaService tokenQuotaService;
    private final ObjectMapper objectMapper;

    public OnboardingService(ChatModelRegistry chatModelRegistry,
                            TokenQuotaService tokenQuotaService,
                            ObjectMapper objectMapper) {
        this.chatModelRegistry = chatModelRegistry;
        this.tokenQuotaService = tokenQuotaService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates onboarding suggestions for a tenant based on org name and bio.
     * Always returns a DTO; on LLM failure, returns empty suggestion arrays.
     */
    public OnboardingSuggestionsDTO generateSuggestions(Long tenantId, OnboardingSuggestionsRequest request) {
        Long ctxTenantId = TenantContext.getTenantId();
        if (ctxTenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        try {
            var tierInfo = tokenQuotaService.resolveTierInfoForTenant(tenantId);
            String provider = tierInfo.provider();

            ChatModel model = chatModelRegistry.getModel(provider);
            if (model == null) {
                log.warn("No chat model found for provider: {}", provider);
                return emptyResult();
            }

            String systemPrompt = buildSystemPrompt();
            String userPrompt = String.format(
                    "Organization Name: %s\nOrganization Bio: %s",
                    request.getOrgName() != null ? request.getOrgName() : "",
                    request.getOrgBio() != null ? request.getOrgBio() : ""
            );

            SystemMessage sysMsg = new SystemMessage(systemPrompt);
            UserMessage userMsg = new UserMessage(userPrompt);

            Prompt prompt = new Prompt(List.of(sysMsg, userMsg));
            String response = model.call(prompt).getResult().getOutput().getText();

            return parseResponse(response);
        } catch (Exception e) {
            log.error("Error generating onboarding suggestions for tenant {}: {}", tenantId, e.getMessage(), e);
            return emptyResult();
        }
    }

    private String buildSystemPrompt() {
        return """
            You are an AI assistant helping to set up a new CRM tenant.
            Based on the organization's name and bio, suggest:
            1. Custom fields to track (e.g., "Company Size", "Industry", "Budget")
            2. Calculated fields (e.g., "Days Since Last Activity", "Win Probability")
            3. Business policies (e.g., "Block status change to 'archived' for high-value customers")
            4. Sample customers to seed the database with

            Respond with a JSON object (no markdown code fences) matching this exact structure:
            {
              "suggestedCustomFields": [
                {"key": "field_key", "label": "Field Label", "type": "text|number|select|date", "description": "...", "config": {"options": ["opt1", "opt2"]}}
              ],
              "suggestedCalculatedFields": [
                {"key": "calc_key", "label": "Calc Label", "expression": "entity.fieldName", "description": "..."}
              ],
              "suggestedPolicies": [
                {"name": "Policy Name", "entityType": "Customer|Contact|Opportunity", "expression": "entity.status == 'archived'", "severity": "DENY|WARN", "description": "..."}
              ],
              "suggestedSampleCustomers": [
                {"name": "Company Name", "status": "active|prospect", "description": "..."}
              ]
            }

            Keep suggestions practical and relevant to the organization type.
            If unsure, return an empty JSON object with all arrays as empty lists.
            """;
    }

    private OnboardingSuggestionsDTO parseResponse(String rawResponse) {
        try {
            String cleanResponse = rawResponse.trim();
            // Remove markdown code fences if present
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            } else if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();

            JsonNode node = objectMapper.readTree(cleanResponse);
            OnboardingSuggestionsDTO result = objectMapper.treeToValue(node, OnboardingSuggestionsDTO.class);
            return result != null ? result : emptyResult();
        } catch (Exception e) {
            log.warn("Failed to parse onboarding suggestions response: {}", e.getMessage());
            return emptyResult();
        }
    }

    private OnboardingSuggestionsDTO emptyResult() {
        return OnboardingSuggestionsDTO.builder()
                .suggestedCustomFields(new ArrayList<>())
                .suggestedCalculatedFields(new ArrayList<>())
                .suggestedPolicies(new ArrayList<>())
                .suggestedSampleCustomers(new ArrayList<>())
                .build();
    }
}
