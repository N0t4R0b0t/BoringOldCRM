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
import com.bocrm.backend.dto.CreateCalculatedFieldDefinitionRequest;
import com.bocrm.backend.dto.UpdateCalculatedFieldDefinitionRequest;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.util.CELExpressionEvaluator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * CalculatedFieldDefinitionService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class CalculatedFieldDefinitionService {

    private final CalculatedFieldDefinitionRepository definitionRepository;
    private final AuditLogService auditLogService;
    private final CELExpressionEvaluator evaluator;
    private final ObjectMapper objectMapper;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final CustomRecordRepository customRecordRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final CalculationMessagePublisher calculationMessagePublisher;

    public CalculatedFieldDefinitionService(
            CalculatedFieldDefinitionRepository definitionRepository,
            AuditLogService auditLogService,
            CELExpressionEvaluator evaluator,
            ObjectMapper objectMapper,
            CustomerRepository customerRepository,
            ContactRepository contactRepository,
            OpportunityRepository opportunityRepository,
            ActivityRepository activityRepository,
            CustomRecordRepository customRecordRepository,
            OrderRepository orderRepository,
            InvoiceRepository invoiceRepository,
            CalculationMessagePublisher calculationMessagePublisher) {
        this.definitionRepository = definitionRepository;
        this.auditLogService = auditLogService;
        this.evaluator = evaluator;
        this.objectMapper = objectMapper;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.activityRepository = activityRepository;
        this.customRecordRepository = customRecordRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.calculationMessagePublisher = calculationMessagePublisher;
    }

    @Transactional(readOnly = true)
    public List<CalculatedFieldDefinitionDTO> getDefinitions(String entityType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return definitionRepository.findByTenantIdAndEntityTypeIgnoreCaseOrderByDisplayOrderAscIdAsc(tenantId, entityType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns a key that is unique for the given tenant/entityType. If the requested key
     * already exists, appends _2, _3, … until a free slot is found.
     */
    public String resolveUniqueKey(String entityType, String baseKey) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        if (definitionRepository.findByTenantIdAndEntityTypeAndKey(tenantId, entityType, baseKey).isEmpty()) {
            return baseKey;
        }
        int suffix = 2;
        while (true) {
            String candidate = baseKey + "_" + suffix;
            if (definitionRepository.findByTenantIdAndEntityTypeAndKey(tenantId, entityType, candidate).isEmpty()) {
                return candidate;
            }
            suffix++;
        }
    }

    @Transactional
    public CalculatedFieldDefinitionDTO createDefinition(CreateCalculatedFieldDefinitionRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        definitionRepository.findByTenantIdAndEntityTypeAndKey(tenantId, request.getEntityType(), request.getKey())
                .ifPresent(f -> { throw new ValidationException("Calculated field key already exists for this entity type"); });

        String configJson = serializeConfig(request.getConfig());

        int nextOrder = definitionRepository.findByTenantIdAndEntityTypeIgnoreCase(tenantId, request.getEntityType()).size();

        CalculatedFieldDefinition definition = CalculatedFieldDefinition.builder()
                .tenantId(tenantId)
                .entityType(request.getEntityType())
                .key(request.getKey())
                .label(request.getLabel())
                .expression(request.getExpression())
                .returnType(request.getReturnType())
                .configJsonb(configJson)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .displayInTable(request.getDisplayInTable() != null ? request.getDisplayInTable() : false)
                .displayOrder(nextOrder)
                .build();

        CalculatedFieldDefinition saved = definitionRepository.save(definition);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_CALCULATED_FIELD_DEFINITION",
                "CalculatedField", saved.getId(), request);
        return toDTO(saved);
    }

    @Transactional
    public CalculatedFieldDefinitionDTO updateDefinition(Long id, UpdateCalculatedFieldDefinitionRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CalculatedFieldDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calculated field definition not found"));

        if (!definition.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        boolean wasDisplayInTable = Boolean.TRUE.equals(definition.getDisplayInTable());

        if (request.getLabel() != null) definition.setLabel(request.getLabel());
        if (request.getExpression() != null) definition.setExpression(request.getExpression());
        if (request.getEnabled() != null) definition.setEnabled(request.getEnabled());
        if (request.getDisplayInTable() != null) definition.setDisplayInTable(request.getDisplayInTable());
        if (request.getConfig() != null) {
            definition.setConfigJsonb(serializeConfig(request.getConfig()));
        }

        CalculatedFieldDefinition updated = definitionRepository.save(definition);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_CALCULATED_FIELD_DEFINITION",
                "CalculatedField", id, request);

        boolean nowDisplayInTable = Boolean.TRUE.equals(updated.getDisplayInTable());
        if (!wasDisplayInTable && nowDisplayInTable) {
            enqueueFullTenantRecalculation(tenantId, updated.getEntityType());
        }

        return toDTO(updated);
    }

    @Transactional
    public void deleteDefinition(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        CalculatedFieldDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calculated field definition not found"));

        if (!definition.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        definitionRepository.delete(definition);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_CALCULATED_FIELD_DEFINITION",
                "CalculatedField", id, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evaluateForEntity(String entityType, Long entityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        JsonNode entityNode = loadEntityAsJson(entityType, entityId, tenantId);

        // Extract custom_data as a map of JsonNode for the evaluator
        Map<String, JsonNode> customFieldsMap = new LinkedHashMap<>();
        JsonNode customData = entityNode.get("customData");
        if (customData != null && customData.isObject()) {
            customData.properties().forEach(e -> customFieldsMap.put(e.getKey(), e.getValue()));
        }

        Map<String, Object> context = evaluator.buildContext(entityNode, customFieldsMap);

        List<CalculatedFieldDefinition> definitions = definitionRepository
                .findByTenantIdAndEntityTypeIgnoreCase(tenantId, entityType)
                .stream()
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .toList();

        Map<String, Object> results = new LinkedHashMap<>();
        for (CalculatedFieldDefinition def : definitions) {
            try {
                Object value = evaluator.evaluate(def.getExpression(), context);
                results.put(def.getKey(), value);
            } catch (Exception e) {
                log.warn("Failed to evaluate expression for calculated field '{}': {}", def.getKey(), e.getMessage());
                results.put(def.getKey(), null);
            }
        }
        return results;
    }

    private JsonNode loadEntityAsJson(String entityType, Long entityId, Long tenantId) {
        return switch (entityType.toLowerCase()) {
            case "customer" -> {
                Customer entity = customerRepository.findByIdAndTenantId(entityId, tenantId);
                if (entity == null) throw new ResourceNotFoundException("Customer not found");
                yield objectMapper.valueToTree(entity);
            }
            case "contact" -> {
                Contact entity = contactRepository.findByIdAndTenantId(entityId, tenantId);
                if (entity == null) throw new ResourceNotFoundException("Contact not found");
                yield objectMapper.valueToTree(entity);
            }
            case "opportunity" -> {
                Opportunity entity = opportunityRepository.findByIdAndTenantId(entityId, tenantId);
                if (entity == null) throw new ResourceNotFoundException("Opportunity not found");
                yield objectMapper.valueToTree(entity);
            }
            case "activity" -> {
                Activity entity = activityRepository.findByIdAndTenantId(entityId, tenantId);
                if (entity == null) throw new ResourceNotFoundException("Activity not found");
                yield objectMapper.valueToTree(entity);
            }
            case "customrecord" -> {
                CustomRecord entity = customRecordRepository.findByIdAndTenantId(entityId, tenantId);
                if (entity == null) throw new ResourceNotFoundException("CustomRecord not found");
                yield objectMapper.valueToTree(entity);
            }
            case "order" -> {
                Order entity = orderRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
                if (!entity.getTenantId().equals(tenantId)) {
                    throw new ForbiddenException("Access denied");
                }
                yield objectMapper.valueToTree(entity);
            }
            case "invoice" -> {
                Invoice entity = invoiceRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
                if (!entity.getTenantId().equals(tenantId)) {
                    throw new ForbiddenException("Access denied");
                }
                yield objectMapper.valueToTree(entity);
            }
            default -> throw new ValidationException("Unknown entity type: " + entityType);
        };
    }

    private String serializeConfig(JsonNode config) {
        try {
            return config != null ? objectMapper.writeValueAsString(config) : "{}";
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize config", e);
        }
    }

    @Transactional
    public void reorderDefinitions(List<Long> orderedIds) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        IntStream.range(0, orderedIds.size()).forEach(i -> {
            Long defId = orderedIds.get(i);
            CalculatedFieldDefinition definition = definitionRepository.findById(defId)
                    .orElseThrow(() -> new ResourceNotFoundException("Calculated field definition not found"));
            if (!definition.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
            definition.setDisplayOrder(i);
            definitionRepository.save(definition);
        });
    }

    private CalculatedFieldDefinitionDTO toDTO(CalculatedFieldDefinition definition) {
        return CalculatedFieldDefinitionDTO.builder()
                .id(definition.getId())
                .entityType(definition.getEntityType())
                .key(definition.getKey())
                .label(definition.getLabel())
                .expression(definition.getExpression())
                .returnType(definition.getReturnType())
                .config(parseJsonNode(definition.getConfigJsonb()))
                .enabled(definition.getEnabled())
                .displayInTable(definition.getDisplayInTable())
                .displayOrder(definition.getDisplayOrder() != null ? definition.getDisplayOrder() : 0)
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .build();
    }

    private JsonNode parseJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void enqueueFullTenantRecalculation(Long tenantId, String entityType) {
        List<Long> ids = switch (entityType.toLowerCase()) {
            case "customer"     -> customerRepository.findByTenantId(tenantId).stream().map(Customer::getId).toList();
            case "contact"      -> contactRepository.findByTenantId(tenantId).stream().map(Contact::getId).toList();
            case "opportunity"  -> opportunityRepository.findByTenantId(tenantId).stream().map(Opportunity::getId).toList();
            case "activity"     -> activityRepository.findByTenantId(tenantId).stream().map(Activity::getId).toList();
            case "customrecord"        -> customRecordRepository.findByTenantId(tenantId).stream().map(CustomRecord::getId).toList();
            case "order"        -> orderRepository.findByTenantId(tenantId).stream().map(Order::getId).toList();
            case "invoice"      -> invoiceRepository.findByTenantId(tenantId).stream().map(Invoice::getId).toList();
            default             -> List.of();
        };
        log.info("Enqueuing recalculation for {} {} records (tenant {}) — displayInTable enabled",
                ids.size(), entityType, tenantId);
        ids.forEach(entityId -> calculationMessagePublisher.publish(tenantId, entityType, entityId));
    }
}
