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
import com.bocrm.backend.exception.*;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * CustomFieldDefinitionService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class CustomFieldDefinitionService {
    private final CustomFieldDefinitionRepository definitionRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public CustomFieldDefinitionService(CustomFieldDefinitionRepository definitionRepository,
                                       AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CustomFieldDefinitionDTO createFieldDefinition(CreateCustomFieldDefinitionRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Check if field already exists
        definitionRepository.findByTenantIdAndEntityTypeAndKey(tenantId, request.getEntityType(), request.getKey())
                .ifPresent(f -> { throw new ValidationException("Field already exists"); });

        // Merge displayInTable into config if present
        JsonNode configNode = request.getConfig();
        ObjectNode configObject = (configNode != null && configNode.isObject()) ? (ObjectNode) configNode : objectMapper.createObjectNode();
        if (request.getDisplayInTable() != null) {
            configObject.put("displayInTable", request.getDisplayInTable());
        }
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(configObject);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize custom field config", e);
        }

        int nextOrder = definitionRepository.findByTenantIdAndEntityType(tenantId, request.getEntityType()).size();

        CustomFieldDefinition definition = CustomFieldDefinition.builder()
                .tenantId(tenantId)
                .entityType(request.getEntityType())
                .key(request.getKey())
                .label(request.getLabel())
                .type(request.getType())
                .configJsonb(configJson)
                .required(request.getRequired() != null ? request.getRequired() : false)
                .displayInTable(request.getDisplayInTable() != null ? request.getDisplayInTable() : false)
                .displayOrder(nextOrder)
                .build();

        CustomFieldDefinition saved = definitionRepository.save(definition);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_CUSTOM_FIELD_DEFINITION", "CustomField", saved.getId(), request);

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionDTO> getFieldDefinitions(String entityType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return definitionRepository.findByTenantIdAndEntityTypeOrderByDisplayOrderAscIdAsc(tenantId, entityType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomFieldDefinitionDTO updateFieldDefinition(Long fieldId, UpdateCustomFieldDefinitionRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomFieldDefinition definition = definitionRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field definition not found"));

        if (!definition.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getLabel() != null) definition.setLabel(request.getLabel());
        // Merge displayInTable into config if present
        if (request.getConfig() != null || request.getDisplayInTable() != null) {
            ObjectNode configObject;
            try {
                JsonNode existingConfig = objectMapper.readTree(definition.getConfigJsonb());
                configObject = (existingConfig != null && existingConfig.isObject()) ? (ObjectNode) existingConfig : objectMapper.createObjectNode();
            } catch (Exception e) {
                configObject = objectMapper.createObjectNode();
            }
            if (request.getConfig() != null && request.getConfig().isObject()) {
                ObjectNode reqConfigObj = (ObjectNode) request.getConfig();
                Collection<String> fieldNames = reqConfigObj.propertyNames();
                for(String fieldName : fieldNames) {
                    JsonNode fieldValue = reqConfigObj.get(fieldName);
                    configObject.set(fieldName, fieldValue);
                }
            }
            if (request.getDisplayInTable() != null) {
                configObject.put("displayInTable", request.getDisplayInTable());
            }
            try {
                definition.setConfigJsonb(objectMapper.writeValueAsString(configObject));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize custom field config", e);
            }
        }
        if (request.getRequired() != null) definition.setRequired(request.getRequired());
        if (request.getDisplayInTable() != null) definition.setDisplayInTable(request.getDisplayInTable());

        CustomFieldDefinition updated = definitionRepository.save(definition);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_CUSTOM_FIELD_DEFINITION", "CustomField", fieldId, request);

        return toDTO(updated);
    }

    @Transactional
    public void deleteFieldDefinition(Long fieldId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CustomFieldDefinition definition = definitionRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field definition not found"));

        if (!definition.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        definitionRepository.delete(definition);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_CUSTOM_FIELD_DEFINITION", "CustomField", fieldId, null);
    }

    private CustomFieldDefinitionDTO toDTO(CustomFieldDefinition definition) {
        try {
            JsonNode configNode = objectMapper.readTree(definition.getConfigJsonb());
            Boolean displayInTable = definition.getDisplayInTable();
            // If not set on entity, fallback to config
            if (displayInTable == null && configNode.has("displayInTable")) {
                displayInTable = configNode.get("displayInTable").asBoolean();
            }
            return CustomFieldDefinitionDTO.builder()
                    .id(definition.getId())
                    .entityType(definition.getEntityType())
                    .key(definition.getKey())
                    .label(definition.getLabel())
                    .type(definition.getType())
                    .config(configNode)
                    .required(definition.getRequired())
                    .displayInTable(displayInTable)
                    .displayOrder(definition.getDisplayOrder() != null ? definition.getDisplayOrder() : 0)
                    .createdAt(definition.getCreatedAt())
                    .updatedAt(definition.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting field definition to DTO", e);
        }
    }

    @Transactional
    public void reorderFieldDefinitions(List<Long> orderedIds) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        IntStream.range(0, orderedIds.size()).forEach(i -> {
            Long fieldId = orderedIds.get(i);
            CustomFieldDefinition definition = definitionRepository.findById(fieldId)
                    .orElseThrow(() -> new ResourceNotFoundException("Field definition not found"));
            if (!definition.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
            definition.setDisplayOrder(i);
            definitionRepository.save(definition);
        });
    }

    /**
     * Extract table-displayable fields from full custom data
     * Returns only fields that have displayInTable=true in their config
     */
    @Transactional(readOnly = true)
    public JsonNode extractTableData(Long tenantId, String entityType, JsonNode fullCustomData) {
        ObjectNode tableData = objectMapper.createObjectNode();

        if (fullCustomData == null || fullCustomData.isEmpty()) {
            return tableData;
        }

        try {
            // Get all field definitions for this entity type
            List<CustomFieldDefinition> definitions = definitionRepository.findByTenantIdAndEntityType(tenantId, entityType);

            // Check each field to see if it should be displayed in table
            for (CustomFieldDefinition definition : definitions) {
                if (fullCustomData.has(definition.getKey())) {
                    JsonNode fieldValue = fullCustomData.get(definition.getKey());
                    if (fieldValue == null || fieldValue.isNull()) continue;

                    try {
                        JsonNode configNode = objectMapper.readTree(definition.getConfigJsonb());
                        JsonNode displayNode = configNode.get("displayInTable");
                        if (displayNode != null && displayNode.isBoolean() && displayNode.asBoolean()) {
                            // For richtext fields, strip HTML tags and truncate to 100 chars
                            JsonNode processedValue = fieldValue;
                            if ("richtext".equals(definition.getType()) && fieldValue.isTextual()) {
                                String htmlContent = fieldValue.asText();
                                String plainText = stripHtmlTags(htmlContent);
                                String truncated = truncateText(plainText, 100);
                                processedValue = objectMapper.valueToTree(truncated);
                            }
                            tableData.set(definition.getKey(), processedValue);
                        }
                    } catch (Exception e) {
                        // ignore malformed config
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting table data for entity type: {}", entityType, e);
        }

        return tableData;
    }

    /**
     * Strip HTML tags from text
     */
    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        // Remove HTML tags using regex
        return html.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Truncate text to specified length and add ellipsis if needed
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }

    /**
     * Convert map to JsonNode for JSONB storage
     */
    public JsonNode mapToJsonNode(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(data);
    }

    /**
     * Validate and sanitize custom fields before create/update
     * Returns a new JsonNode containing only fields that are defined in the CustomFieldDefinition.
     * Undefined custom fields are excluded.
     * Validates required constraints for defined fields.
     */
    @Transactional(readOnly = true)
    public JsonNode validateAndSanitizeCustomFields(Long tenantId, String entityType, JsonNode customFields) {
        if (customFields == null || customFields.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        List<CustomFieldDefinition> definitions = definitionRepository.findByTenantIdAndEntityType(tenantId, entityType);

        // If no definitions exist, return empty object (strict mode: no custom fields allowed if not defined)
        if (definitions == null || definitions.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        Map<String, CustomFieldDefinition> definitionMap = definitions.stream()
                .collect(Collectors.toMap(CustomFieldDefinition::getKey, d -> d));

        ObjectNode sanitizedNode = objectMapper.createObjectNode();
        Map<String, Object> inputMap = objectMapper.convertValue(customFields, Map.class); // Use input JSON

        // Only include fields that are defined
        for (String key : inputMap.keySet()) {
            CustomFieldDefinition def = definitionMap.get(key);
            if (def != null) {
                // It's a defined field, add it to sanitized result
                sanitizedNode.set(key, customFields.get(key));
            }
            // If undefined, it is ignored (excluded)
        }

        // Validate required fields
        for (CustomFieldDefinition def : definitions) {
            if (def.getRequired()) {
                if (!sanitizedNode.has(def.getKey()) || sanitizedNode.get(def.getKey()).isNull()) {
                    throw new ValidationException("Required field is missing: " + def.getKey());
                }
            }
        }

        return sanitizedNode;
    }
}
