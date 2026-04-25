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

import com.bocrm.backend.dto.CreateNotificationTemplateRequest;
import com.bocrm.backend.dto.NotificationTemplateDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.UpdateNotificationTemplateRequest;
import com.bocrm.backend.service.NotificationService;
import com.bocrm.backend.service.NotificationTemplateService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * NotificationTemplateController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/notification-templates")
@Tag(name = "Notification Templates", description = "Manage notification email templates")
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateService service;
    private final ObjectMapper objectMapper;

    public NotificationTemplateController(NotificationTemplateService service,
                                         ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List notification templates for current tenant")
    public ResponseEntity<PagedResponse<NotificationTemplateDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String notificationType) {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.list(tenantId, page, size, search, notificationType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single template by ID")
    public ResponseEntity<NotificationTemplateDTO> get(@PathVariable Long id) {
        NotificationTemplateDTO template = service.get(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a new notification template")
    public ResponseEntity<NotificationTemplateDTO> create(
            @RequestBody CreateNotificationTemplateRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<NotificationTemplateDTO> update(
            @PathVariable Long id,
            @RequestBody UpdateNotificationTemplateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Delete a notification template")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get metadata about available notification types and their placeholder variables.
     */
    @GetMapping("/types")
    @Operation(summary = "Get available notification types and placeholder variables")
    public ResponseEntity<ArrayNode> getNotificationTypes() {
        ArrayNode types = objectMapper.createArrayNode();

        Map<String, List<String>> typeVariables = new HashMap<>();
        typeVariables.put(NotificationService.RECORD_MODIFIED,
                List.of("{{actorName}}", "{{entityType}}", "{{entityName}}", "{{entityId}}", "{{recipientEmail}}", "{{senderName}}"));
        typeVariables.put(NotificationService.OWNERSHIP_ASSIGNED,
                List.of("{{actorName}}", "{{entityType}}", "{{entityName}}", "{{entityId}}", "{{recipientEmail}}", "{{senderName}}"));
        typeVariables.put(NotificationService.ACCESS_GRANTED,
                List.of("{{actorName}}", "{{entityType}}", "{{entityName}}", "{{entityId}}", "{{permission}}", "{{recipientEmail}}", "{{senderName}}"));
        typeVariables.put(NotificationService.ACTIVITY_DUE_SOON,
                List.of("{{entityName}}", "{{dueAt}}", "{{recipientEmail}}", "{{senderName}}"));
        typeVariables.put(NotificationService.DAILY_INSIGHT,
                List.of("{{insightText}}", "{{recipientEmail}}", "{{senderName}}"));
        typeVariables.put(NotificationService.CUSTOM_MESSAGE,
                List.of("{{actorName}}", "{{entityType}}", "{{entityName}}", "{{customSubject}}", "{{customBody}}", "{{recipientEmail}}", "{{senderName}}"));

        for (Map.Entry<String, List<String>> entry : typeVariables.entrySet()) {
            ObjectNode typeObj = objectMapper.createObjectNode();
            typeObj.put("type", entry.getKey());
            typeObj.put("label", entry.getKey().replaceAll("_", " "));
            ArrayNode vars = objectMapper.createArrayNode();
            entry.getValue().forEach(vars::add);
            typeObj.set("variables", vars);
            types.add(typeObj);
        }

        return ResponseEntity.ok(types);
    }
}
