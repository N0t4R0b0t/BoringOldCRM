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

import com.bocrm.backend.dto.CreatePolicyRuleRequest;
import com.bocrm.backend.dto.PolicyRuleDTO;
import com.bocrm.backend.dto.UpdatePolicyRuleRequest;
import com.bocrm.backend.entity.PolicyRule;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.repository.PolicyRuleRepository;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.util.OpaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * PolicyRuleService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class PolicyRuleService {

    private final PolicyRuleRepository policyRuleRepository;
    private final AuditLogService auditLogService;
    private final OpaSyncService opaSyncService;
    private final OpaClient opaClient;

    public PolicyRuleService(PolicyRuleRepository policyRuleRepository,
                              AuditLogService auditLogService,
                              OpaSyncService opaSyncService,
                              OpaClient opaClient) {
        this.policyRuleRepository = policyRuleRepository;
        this.auditLogService = auditLogService;
        this.opaSyncService = opaSyncService;
        this.opaClient = opaClient;
    }

    /**
     * Normalize a Rego expression body supplied by the AI assistant.
     * LLMs frequently write `&&`-joined conditions; Rego uses implicit AND via newlines.
     * Splitting on `&&` produces valid multi-line Rego without changing semantics.
     */
    private String normalizeExpression(String expression) {
        if (expression == null) return null;
        String[] parts = expression.split("\\s*&&\\s*");
        if (parts.length > 1) {
            return String.join("\n", parts).trim();
        }
        return expression.trim();
    }

    /**
     * Validate a Rego expression against OPA before saving. Throws {@link ValidationException}
     * with a user-readable message if OPA rejects the expression.
     */
    private void validateExpression(String expression) {
        try {
            opaClient.validateExpression(expression);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * Schedule an OPA sync for the given (tenantId, entityType) group to run after the
     * current transaction commits, so OPA reads the latest committed DB state.
     */
    private void scheduleOpaSync(Long tenantId, String entityType) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                opaSyncService.syncGroup(tenantId, entityType);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<PolicyRuleDTO> getRules(String entityType) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return policyRuleRepository
                .findByTenantIdAndEntityTypeIgnoreCaseOrderByDisplayOrderAscIdAsc(tenantId, entityType)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "PolicyRules", allEntries = true)
    public PolicyRuleDTO createRule(CreatePolicyRuleRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        boolean nameExists = policyRuleRepository
                .findByTenantIdAndEntityTypeIgnoreCase(tenantId, request.getEntityType())
                .stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new ValidationException("A policy rule with this name already exists for this entity type");
        }

        String expression = normalizeExpression(request.getExpression());
        validateExpression(expression);

        int nextOrder = policyRuleRepository
                .findByTenantIdAndEntityTypeIgnoreCase(tenantId, request.getEntityType()).size();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode operationsArray = mapper.createArrayNode();
        for (String op : request.getOperations()) {
            operationsArray.add(op);
        }

        PolicyRule rule = PolicyRule.builder()
                .tenantId(tenantId)
                .entityType(request.getEntityType())
                .operations(operationsArray)
                .name(request.getName())
                .description(request.getDescription())
                .expression(expression)
                .severity(request.getSeverity() != null ? request.getSeverity() : "DENY")
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .displayOrder(nextOrder)
                .build();

        PolicyRule saved = policyRuleRepository.save(rule);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_POLICY_RULE", "PolicyRule", saved.getId(), request);
        scheduleOpaSync(tenantId, saved.getEntityType());
        return toDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "PolicyRules", allEntries = true)
    public PolicyRuleDTO updateRule(Long id, UpdatePolicyRuleRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        PolicyRule rule = policyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found"));

        if (!rule.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getExpression() != null) {
            String expression = normalizeExpression(request.getExpression());
            validateExpression(expression);
            rule.setExpression(expression);
        }
        if (request.getOperations() != null && !request.getOperations().isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode operationsArray = mapper.createArrayNode();
            for (String op : request.getOperations()) {
                operationsArray.add(op);
            }
            rule.setOperations(operationsArray);
        }
        if (request.getSeverity() != null) rule.setSeverity(request.getSeverity());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());

        PolicyRule updated = policyRuleRepository.save(rule);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_POLICY_RULE", "PolicyRule", id, request);
        scheduleOpaSync(tenantId, updated.getEntityType());
        return toDTO(updated);
    }

    @Transactional
    @CacheEvict(value = "PolicyRules", allEntries = true)
    public void deleteRule(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        PolicyRule rule = policyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found"));

        if (!rule.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        String entityType = rule.getEntityType();
        policyRuleRepository.delete(rule);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_POLICY_RULE", "PolicyRule", id, null);
        scheduleOpaSync(tenantId, entityType);
    }

    @Transactional
    @CacheEvict(value = "PolicyRules", allEntries = true)
    public void reorderRules(List<Long> orderedIds) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        java.util.Set<String> affectedEntityTypes = new java.util.LinkedHashSet<>();
        IntStream.range(0, orderedIds.size()).forEach(i -> {
            Long ruleId = orderedIds.get(i);
            PolicyRule rule = policyRuleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found"));
            if (!rule.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");
            rule.setDisplayOrder(i);
            policyRuleRepository.save(rule);
            affectedEntityTypes.add(rule.getEntityType());
        });

        final Long finalTenantId = tenantId;
        affectedEntityTypes.forEach(et -> scheduleOpaSync(finalTenantId, et));
    }

    private PolicyRuleDTO toDTO(PolicyRule rule) {
        List<String> operations = new ArrayList<>();
        if (rule.getOperations() != null && rule.getOperations().isArray()) {
            for (JsonNode node : rule.getOperations()) {
                if (node.isTextual()) {
                    operations.add(node.asText());
                }
            }
        }

        return PolicyRuleDTO.builder()
                .id(rule.getId())
                .entityType(rule.getEntityType())
                .operations(operations)
                .name(rule.getName())
                .description(rule.getDescription())
                .expression(rule.getExpression())
                .severity(rule.getSeverity())
                .enabled(rule.getEnabled())
                .displayOrder(rule.getDisplayOrder() != null ? rule.getDisplayOrder() : 0)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
