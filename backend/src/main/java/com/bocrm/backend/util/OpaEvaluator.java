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

import com.bocrm.backend.config.OpaProperties;
import com.bocrm.backend.dto.PolicyViolationDetail;
import com.bocrm.backend.exception.PolicyViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evaluates policy rules for a given entity operation by querying the OPA sidecar.
 *
 * <p>Calls {@code POST /v1/data/bocrm/tenant_{id}/{entityType}} with input containing
 * {@code entity}, {@code previous}, and {@code operation}. OPA returns a {@code deny}
 * set and a {@code warn} set based on the Rego rules synced by {@link OpaSyncService}.
 *
 * <p>If OPA is unavailable, a {@link RuntimeException} is thrown (fail-closed).
 * If OPA is disabled (test profile), returns empty result immediately.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class OpaEvaluator {

    private final OpaClient opaClient;
    private final OpaProperties opaProperties;
    private final OpaRegoBuilder opaRegoBuilder;

    public OpaEvaluator(OpaClient opaClient, OpaProperties opaProperties, OpaRegoBuilder opaRegoBuilder) {
        this.opaClient = opaClient;
        this.opaProperties = opaProperties;
        this.opaRegoBuilder = opaRegoBuilder;
    }

    public record OpaResult(
            List<PolicyViolationException.PolicyViolation> denyViolations,
            List<PolicyViolationDetail> warnViolations
    ) {
        public static OpaResult empty() {
            return new OpaResult(List.of(), List.of());
        }
    }

    /**
     * Evaluate all enabled Rego policies for the given entity operation.
     *
     * @param tenantId        current tenant
     * @param entityType      e.g. "Customer", "Contact"
     * @param operation       "CREATE", "UPDATE", or "DELETE"
     * @param entityContext   final/merged entity state — passed as {@code input.entity}
     * @param previousContext state before mutation (may be null for CREATE/DELETE) — passed as {@code input.previous}
     * @return typed deny and warn violation lists
     */
    public OpaResult evaluate(Long tenantId, String entityType, String operation,
                              Map<String, Object> entityContext,
                              Map<String, Object> previousContext) {
        if (!opaProperties.isEnabled()) {
            return OpaResult.empty();
        }

        Map<String, Object> input = Map.of(
                "entity", entityContext != null ? entityContext : Map.of(),
                "previous", previousContext != null ? previousContext : Map.of(),
                "operation", operation
        );

        String dataPath = opaRegoBuilder.dataPath(tenantId, entityType);
        JsonNode result = opaClient.evaluateData(dataPath, input);

        if (result == null || result.isMissingNode() || result.isNull()) {
            return OpaResult.empty();
        }

        List<PolicyViolationException.PolicyViolation> denyViolations = new ArrayList<>();
        List<PolicyViolationDetail> warnViolations = new ArrayList<>();

        parseViolationSet(result.path("deny"), "DENY", denyViolations, warnViolations);
        parseViolationSet(result.path("warn"), "WARN", denyViolations, warnViolations);

        return new OpaResult(denyViolations, warnViolations);
    }

    private void parseViolationSet(JsonNode setNode, String severity,
                                   List<PolicyViolationException.PolicyViolation> denyList,
                                   List<PolicyViolationDetail> warnList) {
        if (setNode == null || setNode.isMissingNode() || !setNode.isArray()) return;

        for (JsonNode v : setNode) {
            Long ruleId = v.path("ruleId").isNumber() ? v.path("ruleId").longValue() : null;
            String ruleName = v.path("ruleName").stringValue();
            String message = v.path("message").stringValue();
            String actualSeverity = v.path("severity").stringValue();
            if (actualSeverity == null) actualSeverity = severity;

            if ("DENY".equalsIgnoreCase(actualSeverity)) {
                denyList.add(new PolicyViolationException.PolicyViolation(ruleId, ruleName, message));
            } else {
                warnList.add(PolicyViolationDetail.builder()
                        .ruleId(ruleId)
                        .ruleName(ruleName)
                        .message(message)
                        .severity("WARN")
                        .build());
            }
        }
    }
}
