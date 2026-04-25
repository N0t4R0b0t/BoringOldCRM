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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
/**
 * ChatService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final AssistantService assistantService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       AssistantService assistantService) {
        this.chatMessageRepository = chatMessageRepository;
        this.assistantService = assistantService;
    }

    public ChatResponseDTO sendMessage(SendMessageRequest request) {
        return assistantService.processMessage(request);
    }

    @Transactional
    public void clearHistory() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) {
            throw new ForbiddenException("Tenant and user context not set");
        }
        chatMessageRepository.deleteByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getConversationHistory(String contextEntityType, Long contextEntityId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            throw new ForbiddenException("Tenant and user context not set");
        }

        List<ChatMessage> messages;
        if (contextEntityType != null && contextEntityId != null) {
            messages = chatMessageRepository.findByTenantIdAndUserIdAndContextEntityTypeAndContextEntityIdOrderByCreatedAtAsc(
                    tenantId, userId, contextEntityType, contextEntityId);
        } else {
            messages = chatMessageRepository.findByTenantIdAndUserIdOrderByCreatedAtAsc(tenantId, userId);
        }

        return messages.stream()
                .map(msg -> ChatMessageDTO.builder()
                        .id(msg.getId())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .contextEntityType(msg.getContextEntityType())
                        .contextEntityId(msg.getContextEntityId())
                        .createdAt(msg.getCreatedAt())
                        .attachmentFileName(msg.getAttachmentFileName())
                        .attachmentMimeType(msg.getAttachmentMimeType())
                        .modelUsed(msg.getModelUsed())
                        .processingTimeMs(msg.getProcessingTimeMs())
                        .build())
                .collect(Collectors.toList());
    }
}
