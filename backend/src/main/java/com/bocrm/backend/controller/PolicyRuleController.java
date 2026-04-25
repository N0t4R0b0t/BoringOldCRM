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
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.service.PolicyRuleService;
import com.bocrm.backend.service.PolicyValidationService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * PolicyRuleController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/policy-rules")
@Tag(name = "Policy Rules", description = "Business policy rule definitions and evaluation")
@Slf4j
public class PolicyRuleController {

    private final PolicyRuleService policyRuleService;
    private final PolicyValidationService policyValidationService;

    public PolicyRuleController(PolicyRuleService policyRuleService,
                                 PolicyValidationService policyValidationService) {
        this.policyRuleService = policyRuleService;
        this.policyValidationService = policyValidationService;
    }

    @GetMapping("/definitions")
    @Operation(summary = "List policy rules for an entity type")
    public ResponseEntity<List<PolicyRuleDTO>> getRules(@RequestParam String entityType) {
        return ResponseEntity.ok(policyRuleService.getRules(entityType));
    }

    @PostMapping("/definitions")
    @Operation(summary = "Create a policy rule")
    public ResponseEntity<PolicyRuleDTO> createRule(@Valid @RequestBody CreatePolicyRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyRuleService.createRule(request));
    }

    @PutMapping("/definitions/{id}")
    @Operation(summary = "Update a policy rule")
    public ResponseEntity<PolicyRuleDTO> updateRule(@PathVariable Long id,
                                                     @Valid @RequestBody UpdatePolicyRuleRequest request) {
        return ResponseEntity.ok(policyRuleService.updateRule(id, request));
    }

    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "Delete a policy rule")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        policyRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/definitions/reorder")
    @Operation(summary = "Reorder policy rules")
    public ResponseEntity<Void> reorderRules(@RequestBody List<Long> orderedIds) {
        policyRuleService.reorderRules(orderedIds);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Pre-submit policy evaluation — returns violations and warnings without blocking")
    public ResponseEntity<EvaluatePolicyResponse> evaluate(@Valid @RequestBody EvaluatePolicyRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        EvaluatePolicyResponse result = policyValidationService.evaluateOnly(
                tenantId,
                request.getEntityType(),
                request.getOperation(),
                request.getEntityData(),
                request.getPreviousData());
        return ResponseEntity.ok(result);
    }
}
