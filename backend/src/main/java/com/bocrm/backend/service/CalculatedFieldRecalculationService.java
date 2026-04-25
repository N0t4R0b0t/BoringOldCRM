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

import com.bocrm.backend.dto.CalculatedFieldDefinitionDTO;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to recalculate and store calculated field values.
 * Updates both CalculatedFieldValue cache and entity's table_data_jsonb.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class CalculatedFieldRecalculationService {

    private final CalculatedFieldDefinitionService definitionService;
    private final CalculatedFieldValueRepository calculatedFieldValueRepository;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final CustomRecordRepository customRecordRepository;
    private final ObjectMapper objectMapper;

    public CalculatedFieldRecalculationService(
            CalculatedFieldDefinitionService definitionService,
            CalculatedFieldValueRepository calculatedFieldValueRepository,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            OpportunityRepository opportunityRepository,
            ActivityRepository activityRepository,
            CustomRecordRepository customRecordRepository,
            ObjectMapper objectMapper) {
        this.definitionService = definitionService;
        this.calculatedFieldValueRepository = calculatedFieldValueRepository;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.activityRepository = activityRepository;
        this.customRecordRepository = customRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Recalculate and store calculated field values for an entity.
     * Updates entity's table_data_jsonb with merged custom + calculated fields.
     */
    @Transactional
    public void recalculateAndStore(Long tenantId, String entityType, Long entityId, Long calculatedFieldId) {
        log.debug("Recalculating {} calculated fields for {}.{}",
                (calculatedFieldId != null ? "single" : "all"), entityType, entityId);

        // Evaluate all enabled calculated fields for the entity
        Map<String, Object> calculatedFields = definitionService.evaluateForEntity(entityType, entityId);

        // Store each calculated field value
        for (Map.Entry<String, Object> entry : calculatedFields.entrySet()) {
            String fieldKey = entry.getKey();
            Object value = entry.getValue();

            // Find the definition to get its ID
            CalculatedFieldDefinition definition = findDefinitionByKey(tenantId, entityType, fieldKey);
            if (definition != null) {
                String valueJson = serializeValue(value);
                CalculatedFieldValue fieldValue = calculatedFieldValueRepository
                        .findByTenantIdAndEntityTypeAndEntityIdAndCalculatedFieldId(
                                tenantId, entityType, entityId, definition.getId())
                        .orElseGet(() -> CalculatedFieldValue.builder()
                                .tenantId(tenantId)
                                .entityType(entityType)
                                .entityId(entityId)
                                .calculatedFieldId(definition.getId())
                                .build());

                fieldValue.setValueJsonb(valueJson);
                fieldValue.setComputedAt(LocalDateTime.now());
                calculatedFieldValueRepository.save(fieldValue);
            }
        }

        // Update entity's table_data_jsonb with denormalized data (custom + calculated fields)
        updateEntityTableDataJsonb(tenantId, entityType, entityId, calculatedFields);
    }

    /**
     * Update entity's table_data_jsonb column with merged custom + calculated fields.
     */
    private void updateEntityTableDataJsonb(Long tenantId, String entityType, Long entityId, Map<String, Object> calculatedFields) {
        // Load entity and merge custom data with calculated fields
        JsonNode mergedData = mergeTableData(tenantId, entityType, entityId, calculatedFields);

        // Persist to entity based on type
        switch (entityType.toLowerCase()) {
            case "customer" -> {
                Customer customer = customerRepository.findByIdAndTenantId(entityId, tenantId);
                if (customer != null) {
                    customer.setTableDataJsonb(mergedData);
                    customerRepository.save(customer);
                }
            }
            case "contact" -> {
                Contact contact = contactRepository.findByIdAndTenantId(entityId, tenantId);
                if (contact != null) {
                    contact.setTableDataJsonb(mergedData);
                    contactRepository.save(contact);
                }
            }
            case "opportunity" -> {
                Opportunity opportunity = opportunityRepository.findByIdAndTenantId(entityId, tenantId);
                if (opportunity != null) {
                    opportunity.setTableDataJsonb(mergedData);
                    opportunityRepository.save(opportunity);
                }
            }
            case "activity" -> {
                Activity activity = activityRepository.findByIdAndTenantId(entityId, tenantId);
                if (activity != null) {
                    activity.setTableDataJsonb(mergedData);
                    activityRepository.save(activity);
                }
            }
            case "customrecord" -> {
                CustomRecord customRecord = customRecordRepository.findByIdAndTenantId(entityId, tenantId);
                if (customRecord != null) {
                    customRecord.setTableDataJsonb(mergedData);
                    customRecordRepository.save(customRecord);
                }
            }
        }
    }

    /**
     * Load custom fields from entity and merge with calculated fields for table_data_jsonb.
     */
    private JsonNode mergeTableData(Long tenantId, String entityType, Long entityId, Map<String, Object> calculatedFields) {
        ObjectNode merged = objectMapper.createObjectNode();

        // Load entity's custom_data and add to merged
        JsonNode customData = loadEntityCustomData(tenantId, entityType, entityId);
        if (customData != null && customData.isObject()) {
            merged.setAll((ObjectNode) customData);
        }

        // Add calculated fields to merged
        for (Map.Entry<String, Object> entry : calculatedFields.entrySet()) {
            merged.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
        }

        return merged;
    }

    /**
     * Load an entity's custom_data JSONB field.
     */
    private JsonNode loadEntityCustomData(Long tenantId, String entityType, Long entityId) {
        return switch (entityType.toLowerCase()) {
            case "customer" -> {
                Customer customer = customerRepository.findByIdAndTenantId(entityId, tenantId);
                yield customer != null ? customer.getCustomData() : null;
            }
            case "contact" -> {
                Contact contact = contactRepository.findByIdAndTenantId(entityId, tenantId);
                yield contact != null ? contact.getCustomData() : null;
            }
            case "opportunity" -> {
                Opportunity opportunity = opportunityRepository.findByIdAndTenantId(entityId, tenantId);
                yield opportunity != null ? opportunity.getCustomData() : null;
            }
            case "activity" -> {
                Activity activity = activityRepository.findByIdAndTenantId(entityId, tenantId);
                yield activity != null ? activity.getCustomData() : null;
            }
            case "customrecord" -> {
                CustomRecord customRecord = customRecordRepository.findByIdAndTenantId(entityId, tenantId);
                yield customRecord != null ? customRecord.getCustomData() : null;
            }
            default -> null;
        };
    }

    /**
     * Find a calculated field definition by key.
     */
    private CalculatedFieldDefinition findDefinitionByKey(Long tenantId, String entityType, String key) {
        // TenantContext is already set by the caller (processor or service method)
        List<CalculatedFieldDefinitionDTO> defs = definitionService.getDefinitions(entityType);
        return defs.stream()
                .filter(d -> d.getKey().equals(key))
                .findFirst()
                .map(this::dtoToEntity)
                .orElse(null);
    }

    /**
     * Convert DTO back to entity (for finding definition ID).
     * In production, we'd add a repository query method instead.
     */
    private CalculatedFieldDefinition dtoToEntity(CalculatedFieldDefinitionDTO dto) {
        return CalculatedFieldDefinition.builder()
                .id(dto.getId())
                .tenantId(null) // Not needed for ID lookup
                .entityType(dto.getEntityType())
                .key(dto.getKey())
                .label(dto.getLabel())
                .expression(dto.getExpression())
                .build();
    }

    /**
     * Serialize a value to JSON string for storage.
     */
    private String serializeValue(Object value) {
        try {
            return value != null ? objectMapper.writeValueAsString(value) : "null";
        } catch (Exception e) {
            log.warn("Failed to serialize calculated field value: {}", e.getMessage());
            return "null";
        }
    }
}
