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
package com.bocrm.backend.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Designates Anthropic as the @Primary ChatModel so that Spring AI's
 * ChatClientAutoConfiguration can resolve the ambiguity when multiple
 * provider models are on the classpath. ChatModelRegistry injects each
 * provider model by its canonical Spring AI bean name.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Configuration
public class ChatModelConfig {

    /**
     * Marks the Anthropic model as primary so ChatClient.Builder auto-config
     * doesn't fail when OpenAI/Gemini models are also present.
     */
    @Bean
    @Primary
    public ChatModel defaultChatModel(AnthropicChatModel anthropicChatModel) {
        return anthropicChatModel;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
