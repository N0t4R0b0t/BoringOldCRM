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
package com.bocrm.backend.tools;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.service.PolicyRuleService;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Spring AI tool methods for policy rule management.
 * Available only to tenant admins for creating, updating, and querying business rules.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class PolicyManagementTools {

    private final PolicyRuleService policyRuleService;
    private final ObjectMapper objectMapper;

    public PolicyManagementTools(PolicyRuleService policyRuleService, ObjectMapper objectMapper) {
        this.policyRuleService = policyRuleService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "List all business policy rules for an entity type (Customer, Contact, Opportunity, Activity, TenantDocument). Shows name, operations (CREATE/UPDATE/DELETE), severity (DENY/WARN), and expression.")
    public String listPolicies(String entityType) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) throw new ForbiddenException("Tenant context not set");

            List<PolicyRuleDTO> rules = policyRuleService.getRules(entityType);
            if (rules.isEmpty()) {
                return "No policies defined for " + entityType + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Policies for ").append(entityType).append(":\n\n");
            for (PolicyRuleDTO rule : rules) {
                sb.append("- **").append(rule.getName()).append("** (ID: ").append(rule.getId()).append(")\n");
                sb.append("  Operations: ").append(String.join(", ", rule.getOperations())).append("\n");
                sb.append("  Severity: ").append(rule.getSeverity()).append("\n");
                sb.append("  Enabled: ").append(rule.getEnabled()).append("\n");
                sb.append("  Expression: `").append(rule.getExpression()).append("`\n");
                if (rule.getDescription() != null && !rule.getDescription().isBlank()) {
                    sb.append("  Description: ").append(rule.getDescription()).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error listing policies: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new business policy rule. entityType must be one of: Customer, Contact, Opportunity, Activity, TenantDocument. operations is a comma-separated list (e.g., 'CREATE,UPDATE' or just 'DELETE'). severity must be: DENY (hard block) or WARN (user confirms). expression is a Rego condition body (NOT JavaScript/CEL) that evaluates to true when the rule is triggered. Use 'input.entity.fieldName' for new/final values and 'input.previous.fieldName' for old values on UPDATE. For multiple conditions, put each on a separate line (no && or || operators — Rego uses implicit AND). For OR logic, create two separate rules. CORRECT examples:\n  input.entity.status == \"locked\"\n  input.entity.status == \"locked\"\n  input.entity.value > 100000\nINCORRECT (don't use these): input.entity.status == \"locked\" && input.entity.value > 100000")
    public String createPolicy(String entityType, String operations, String name, String expression, String severity, String description) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) throw new ForbiddenException("Tenant context not set");

            // Parse comma-separated operations
            List<String> operationsList = new java.util.ArrayList<>();
            for (String op : operations.split(",")) {
                String trimmed = op.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    operationsList.add(trimmed);
                }
            }

            CreatePolicyRuleRequest request = new CreatePolicyRuleRequest();
            request.setEntityType(entityType);
            request.setOperations(operationsList);
            request.setName(name);
            request.setExpression(expression);
            request.setSeverity(severity.toUpperCase());
            request.setDescription(description);
            request.setEnabled(true);

            PolicyRuleDTO created = policyRuleService.createRule(request);
            return "Policy created successfully:\n" +
                    "- Name: " + created.getName() + "\n" +
                    "- Entity Type: " + created.getEntityType() + "\n" +
                    "- Operations: " + String.join(", ", created.getOperations()) + "\n" +
                    "- Severity: " + created.getSeverity() + "\n" +
                    "- Expression: " + created.getExpression();
        } catch (ValidationException e) {
            // Validation errors (e.g., invalid Rego syntax) should be reported clearly without stack trace
            log.debug("Policy validation error: {}", e.getMessage());
            return "Cannot create policy: " + e.getMessage() +
                    "\n\nCommon mistakes:\n" +
                    "- Using && or || operators (use separate lines for AND logic)\n" +
                    "- Using round brackets () instead of square brackets [] for list membership\n" +
                    "- Invalid field references or syntax";
        } catch (Exception e) {
            log.error("Error creating policy: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing business policy rule. Pass only the fields you want to change. policyId is required. operations, if provided, should be comma-separated (e.g., 'CREATE,UPDATE').")
    public String updatePolicy(Long policyId, String name, String operations, String expression, String severity, String description, Boolean enabled) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) throw new ForbiddenException("Tenant context not set");

            UpdatePolicyRuleRequest request = new UpdatePolicyRuleRequest();
            request.setName(name);

            // Parse comma-separated operations if provided
            if (operations != null && !operations.isBlank()) {
                List<String> operationsList = new java.util.ArrayList<>();
                for (String op : operations.split(",")) {
                    String trimmed = op.trim().toUpperCase();
                    if (!trimmed.isEmpty()) {
                        operationsList.add(trimmed);
                    }
                }
                request.setOperations(operationsList);
            }

            request.setExpression(expression);
            request.setSeverity(severity != null ? severity.toUpperCase() : null);
            request.setDescription(description);
            request.setEnabled(enabled);

            PolicyRuleDTO updated = policyRuleService.updateRule(policyId, request);
            return "Policy updated successfully:\n" +
                    "- Name: " + updated.getName() + "\n" +
                    "- Operations: " + String.join(", ", updated.getOperations()) + "\n" +
                    "- Severity: " + updated.getSeverity() + "\n" +
                    "- Expression: " + updated.getExpression() + "\n" +
                    "- Enabled: " + updated.getEnabled();
        } catch (ValidationException e) {
            // Validation errors (e.g., invalid Rego syntax) should be reported clearly without stack trace
            log.debug("Policy validation error: {}", e.getMessage());
            return "Cannot update policy: " + e.getMessage() +
                    "\n\nCommon mistakes:\n" +
                    "- Using && or || operators (use separate lines for AND logic)\n" +
                    "- Using round brackets () instead of square brackets [] for list membership\n" +
                    "- Invalid field references or syntax";
        } catch (Exception e) {
            log.error("Error updating policy: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a business policy rule by ID.")
    public String deletePolicy(Long policyId) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) throw new ForbiddenException("Tenant context not set");

            policyRuleService.deleteRule(policyId);
            return "Policy (ID: " + policyId + ") deleted successfully.";
        } catch (Exception e) {
            log.error("Error deleting policy: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Explain why a policy was triggered. Useful for understanding policy violations. Pass the policy rule name and the entity type that triggered it.")
    public String explainPolicyTrigger(String entityType, String policyName, String entityDataJson) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) throw new ForbiddenException("Tenant context not set");

            // Parse entity data if provided
            Map<String, Object> entityData = null;
            if (entityDataJson != null && !entityDataJson.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(entityDataJson, Map.class);
                    entityData = parsed;
                } catch (Exception e) {
                    return "Error parsing entity data JSON: " + e.getMessage();
                }
            }

            List<PolicyRuleDTO> rules = policyRuleService.getRules(entityType);
            for (PolicyRuleDTO rule : rules) {
                if (rule.getName().equalsIgnoreCase(policyName)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("**Policy: ").append(rule.getName()).append("**\n\n");
                    sb.append("Entity Type: ").append(rule.getEntityType()).append("\n");
                    sb.append("Operations: ").append(String.join(", ", rule.getOperations())).append("\n");
                    sb.append("Severity: ").append(rule.getSeverity()).append(" (");
                    sb.append("DENY".equals(rule.getSeverity()) ? "Hard block" : "User confirmation required");
                    sb.append(")\n\n");
                    sb.append("**Expression:**\n```\n").append(rule.getExpression()).append("\n```\n\n");
                    sb.append("**Description:**\n");
                    sb.append(rule.getDescription() != null ? rule.getDescription() : "(No description provided)");
                    sb.append("\n\n");
                    sb.append("**How it works (Rego):**\n");
                    sb.append("- The expression body evaluates to `true` when the rule is triggered.\n");
                    sb.append("- For CREATE operations, use `input.entity.fieldName` to reference the field being created.\n");
                    sb.append("- For UPDATE operations, use `input.entity.fieldName` for the new value and `input.previous.fieldName` for the old value.\n");
                    sb.append("- For DELETE operations, use `input.entity.fieldName` to reference the field being deleted.\n");
                    sb.append("- Multiple conditions on separate lines are ANDed implicitly. For OR logic, create two separate rules.\n");
                    if (entityData != null) {
                        sb.append("\n**Current entity data:**\n```json\n").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entityData)).append("\n```\n");
                    }
                    return sb.toString();
                }
            }

            return "Policy '" + policyName + "' not found for " + entityType + ".";
        } catch (Exception e) {
            log.error("Error explaining policy trigger: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
