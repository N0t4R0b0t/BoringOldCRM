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
package com.bocrm.backend.controller;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * ChatController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "AI chat assistant")
@Slf4j
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    @Operation(summary = "Send a message to the AI assistant")
    public ResponseEntity<ChatResponseDTO> sendMessage(@RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Clear all chat history for the current user")
    public ResponseEntity<Void> clearHistory() {
        chatService.clearHistory();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @Operation(summary = "Get chat history")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(
            @RequestParam(required = false) String contextEntityType,
            @RequestParam(required = false) Long contextEntityId
    ) {
        return ResponseEntity.ok(chatService.getConversationHistory(contextEntityType, contextEntityId));
    }

    @GetMapping("/{entityType}/{entityId}")
    @Operation(summary = "Get chat history for entity")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        return ResponseEntity.ok(chatService.getConversationHistory(entityType, entityId));
    }

    @PostMapping("/{entityType}/{entityId}")
    @Operation(summary = "Send message for entity")
    public ResponseEntity<ChatMessageDTO> sendEntityMessage(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestBody SendMessageRequest request
    ) {
        request.setContextEntityType(entityType);
        request.setContextEntityId(entityId);
        ChatResponseDTO response = chatService.sendMessage(request);
        // Return the user's message as confirmation
        return ResponseEntity.status(201).body(response.getUserMessage());
    }
}
