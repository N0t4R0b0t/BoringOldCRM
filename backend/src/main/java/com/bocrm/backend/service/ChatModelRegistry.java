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

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes a provider string ("anthropic", "openai", "google", "ollama") to the corresponding
 * Spring AI ChatModel bean. Falls back to Anthropic when a provider is not configured.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class ChatModelRegistry {

    private final Map<String, ChatModel> registry = new HashMap<>();

    public ChatModelRegistry(
            @Qualifier("anthropicChatModel") ChatModel anthropic,
            @Autowired(required = false) @Qualifier("openAiChatModel") ChatModel openai,
            @Autowired(required = false) @Qualifier("googleGenAiChatModel") ChatModel gemini,
            @Autowired(required = false) @Qualifier("ollamaChatModel") ChatModel ollama) {
        registry.put("anthropic", anthropic);
        if (openai != null) {
            registry.put("openai", openai);
            log.info("ChatModelRegistry: OpenAI model registered");
        }
        if (gemini != null) {
            registry.put("google", gemini);
            log.info("ChatModelRegistry: Gemini model registered");
        }
        if (ollama != null) {
            registry.put("ollama", ollama);
            log.info("ChatModelRegistry: Ollama model registered");
        }
    }

    public ChatModel getModel(String provider) {
        ChatModel model = registry.get(provider);
        if (model == null) {
            log.warn("Provider '{}' not available, falling back to Anthropic", provider);
            return registry.get("anthropic");
        }
        return model;
    }

    public boolean isProviderAvailable(String provider) {
        return registry.containsKey(provider);
    }

    /** Returns all provider names that have a registered model bean. */
    public java.util.Set<String> getAvailableProviders() {
        return java.util.Collections.unmodifiableSet(registry.keySet());
    }
}
