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
package com.bocrm.backend.util;

import com.bocrm.backend.entity.PolicyRule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Builds Rego policy documents from PolicyRule entities.
 *
 * <p>One Rego document is generated per (tenantId, entityType) pair and pushed to OPA.
 * The stored {@code expression} column contains just the Rego condition body — e.g.
 * {@code input.entity.status == "locked"} — and this builder wraps it in a complete
 * Rego document with the correct package declaration and rule structure.
 *
 * <p>DENY rules contribute to the {@code deny} set; WARN rules to the {@code warn} set.
 * Each operation in a rule's operations array produces one rule block, keeping the Rego
 * simple (no set-membership checks inside the rule body).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
public class OpaRegoBuilder {

    /**
     * Returns the OPA policy ID (used as path segments in the REST API) for the given
     * tenant and entity type. Example: "bocrm/tenant_7/Customer"
     */
    public String policyId(Long tenantId, String entityType) {
        return "bocrm/tenant_" + tenantId + "/" + slug(entityType);
    }

    /**
     * Returns the OPA data path for evaluation queries.
     * Same format as policyId since OPA routes data under the package path.
     */
    public String dataPath(Long tenantId, String entityType) {
        return "bocrm/tenant_" + tenantId + "/" + slug(entityType);
    }

    /** Normalise an entity type to a URL- and Rego-safe identifier. */
    private String slug(String entityType) {
        if (entityType == null) return "unknown";
        return entityType.trim().replaceAll("[^A-Za-z0-9_]", "_");
    }

    /**
     * Build a complete Rego document for all enabled rules of a (tenantId, entityType) pair.
     */
    public String buildPolicyDocument(Long tenantId, String entityType, List<PolicyRule> enabledRules) {
        StringBuilder sb = new StringBuilder();

        // Package declaration — spaces and special chars not allowed in Rego identifiers
        sb.append("package bocrm.tenant_").append(tenantId).append(".").append(slug(entityType)).append("\n\n");
        sb.append("import rego.v1\n\n");

        for (PolicyRule rule : enabledRules) {
            String setName = "DENY".equalsIgnoreCase(rule.getSeverity()) ? "deny" : "warn";
            String message = escapeRego(rule.getDescription() != null ? rule.getDescription() : rule.getName());
            String ruleName = escapeRego(rule.getName());

            JsonNode ops = rule.getOperations();
            if (ops != null && ops.isArray()) {
                for (JsonNode opNode : ops) {
                    String operation = opNode.asString("");
                    sb.append(setName).append(" contains violation if {\n");
                    sb.append("    input.operation == \"").append(operation).append("\"\n");
                    // Indent each line of the expression body
                    for (String line : rule.getExpression().split("\n")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            sb.append("    ").append(trimmed).append("\n");
                        }
                    }
                    sb.append("    violation := {\"ruleId\": ").append(rule.getId())
                      .append(", \"ruleName\": \"").append(ruleName).append("\"")
                      .append(", \"message\": \"").append(message).append("\"")
                      .append(", \"severity\": \"").append(rule.getSeverity().toUpperCase()).append("\"}\n");
                    sb.append("}\n\n");
                }
            }
        }

        return sb.toString();
    }

    private String escapeRego(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
