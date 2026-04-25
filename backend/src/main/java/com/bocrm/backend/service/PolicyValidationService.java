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

import com.bocrm.backend.dto.EvaluatePolicyResponse;
import com.bocrm.backend.dto.PolicyViolationDetail;
import com.bocrm.backend.exception.PolicyViolationException;
import com.bocrm.backend.util.OpaEvaluator;
import com.bocrm.backend.util.OpaEvaluator.OpaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core policy evaluation service. Evaluates Rego-based business rules against
 * entity context maps at CRUD time by delegating to the OPA sidecar.
 *
 * <p>Expression contract: the stored Rego condition body evaluates to {@code true}
 * when the rule is triggered (i.e. the condition that should block or warn is met).
 * Example: {@code input.entity.status == "locked"} blocks updates on locked records.
 *
 * <p>For UPDATE operations the context includes both {@code input.entity} (new/merged state)
 * and {@code input.previous} (state before the change).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class PolicyValidationService {

    private final OpaEvaluator opaEvaluator;

    public PolicyValidationService(OpaEvaluator opaEvaluator) {
        this.opaEvaluator = opaEvaluator;
    }

    /**
     * Validate enabled rules for the given operation. Throws {@link PolicyViolationException}
     * if any DENY rules trigger. Returns WARN violations so the caller can embed them in the
     * response DTO.
     *
     * @param tenantId        current tenant
     * @param entityType      e.g. "Customer", "Contact"
     * @param operation       "CREATE", "UPDATE", or "DELETE"
     * @param entityContext   final/merged entity state — accessible as {@code input.entity.*} in Rego
     * @param previousContext state before mutation (null for CREATE/DELETE) — accessible as {@code input.previous.*}
     * @return list of WARN violations (may be empty); never returns if DENY violations exist
     */
    public List<PolicyViolationDetail> validate(Long tenantId, String entityType, String operation,
                                                Map<String, Object> entityContext,
                                                Map<String, Object> previousContext) {
        OpaResult result = opaEvaluator.evaluate(tenantId, entityType, operation, entityContext, previousContext);

        if (!result.denyViolations().isEmpty()) {
            throw new PolicyViolationException(result.denyViolations());
        }

        return result.warnViolations();
    }

    /**
     * Same logic as {@link #validate} but never throws — always returns both DENY and WARN
     * violations. Used by the pre-submit evaluation endpoint.
     */
    public EvaluatePolicyResponse evaluateOnly(Long tenantId, String entityType, String operation,
                                               Map<String, Object> entityData,
                                               Map<String, Object> previousData) {
        OpaResult result = opaEvaluator.evaluate(tenantId, entityType, operation, entityData, previousData);

        List<PolicyViolationDetail> violations = result.denyViolations().stream()
                .map(v -> PolicyViolationDetail.builder()
                        .ruleId(v.ruleId())
                        .ruleName(v.ruleName())
                        .message(v.message())
                        .severity("DENY")
                        .build())
                .collect(Collectors.toList());

        return EvaluatePolicyResponse.builder()
                .blocked(!violations.isEmpty())
                .violations(violations)
                .warnings(result.warnViolations())
                .build();
    }
}
