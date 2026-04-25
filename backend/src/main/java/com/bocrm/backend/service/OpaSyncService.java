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

import com.bocrm.backend.config.OpaProperties;
import com.bocrm.backend.entity.PolicyRule;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.repository.PolicyRuleRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.util.OpaClient;
import com.bocrm.backend.util.OpaRegoBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keeps OPA in sync with the policy rules stored in the database.
 *
 * <p>One Rego policy document is maintained per (tenantId, entityType) pair.
 * Call {@link #syncGroup} after any CRUD operation on {@code PolicyRule}.
 * Call {@link #syncAll} on application startup to pre-load all existing rules.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class OpaSyncService {

    private final OpaClient opaClient;
    private final OpaRegoBuilder opaRegoBuilder;
    private final PolicyRuleRepository policyRuleRepository;
    private final TenantRepository tenantRepository;
    private final OpaProperties opaProperties;

    public OpaSyncService(OpaClient opaClient,
                          OpaRegoBuilder opaRegoBuilder,
                          PolicyRuleRepository policyRuleRepository,
                          TenantRepository tenantRepository,
                          OpaProperties opaProperties) {
        this.opaClient = opaClient;
        this.opaRegoBuilder = opaRegoBuilder;
        this.policyRuleRepository = policyRuleRepository;
        this.tenantRepository = tenantRepository;
        this.opaProperties = opaProperties;
    }

    /**
     * Sync all enabled rules for a single (tenantId, entityType) group to OPA.
     * Must be called <em>after</em> the surrounding transaction has committed so that
     * the repository query sees the latest DB state. Use
     * {@link org.springframework.transaction.support.TransactionSynchronizationManager}
     * to schedule this as an {@code afterCommit} hook from transactional callers.
     *
     * <p>If no enabled rules remain for the group, the OPA policy is deleted.
     */
    public void syncGroup(Long tenantId, String entityType) {
        if (!opaProperties.isEnabled()) return;

        List<PolicyRule> enabledRules = policyRuleRepository
                .findByTenantIdAndEntityTypeIgnoreCaseAndEnabled(tenantId, entityType, true);
        String policyId = opaRegoBuilder.policyId(tenantId, entityType);

        if (enabledRules.isEmpty()) {
            opaClient.deletePolicy(policyId);
            log.debug("OPA: no enabled rules for {}/{}, policy removed", tenantId, entityType);
        } else {
            String rego = opaRegoBuilder.buildPolicyDocument(tenantId, entityType, enabledRules);
            try {
                opaClient.putPolicy(policyId, rego);
                log.debug("OPA: synced {} rules for {}/{}", enabledRules.size(), tenantId, entityType);
            } catch (RuntimeException e) {
                log.error("OPA: failed to sync policy for {}/{} — one or more expressions may be invalid Rego. " +
                          "Fix the expressions via the policy admin UI. Error: {}", tenantId, entityType, e.getMessage());
            }
        }
    }

    /**
     * Sync all enabled rules across all tenants and entity types.
     * Called on application startup.
     *
     * <p>WHY: policy_rules live in per-tenant schemas (tenant_N.policy_rules). At startup there is
     * no HTTP request, so TenantContext is null and Hibernate routes to the public schema — which has
     * no rules. We must iterate over every tenant, temporarily set TenantContext to route Hibernate to
     * that tenant's schema, fetch its rules, then immediately clear. This is the only place outside
     * JwtAuthenticationFilter where TenantContext.setTenantId() is permitted.
     */
    public void syncAll() {
        if (!opaProperties.isEnabled()) return;

        List<Tenant> tenants = tenantRepository.findAll();
        if (tenants.isEmpty()) {
            log.info("OPA startup sync: no tenants found");
            return;
        }

        int synced = 0;
        int failed = 0;
        for (Tenant tenant : tenants) {
            Long tenantId = tenant.getId();
            List<PolicyRule> enabledRules;
            try {
                // Route Hibernate to this tenant's schema for the duration of the query.
                TenantContext.setTenantId(tenantId);
                enabledRules = policyRuleRepository.findAllEnabled();
            } finally {
                TenantContext.clear();
            }

            if (enabledRules.isEmpty()) continue;

            Map<String, List<PolicyRule>> grouped = enabledRules.stream()
                    .collect(Collectors.groupingBy(PolicyRule::getEntityType));

            for (Map.Entry<String, List<PolicyRule>> entry : grouped.entrySet()) {
                String entityType = entry.getKey();
                String rego = opaRegoBuilder.buildPolicyDocument(tenantId, entityType, entry.getValue());
                String policyId = opaRegoBuilder.policyId(tenantId, entityType);
                try {
                    opaClient.putPolicy(policyId, rego);
                    synced++;
                } catch (RuntimeException e) {
                    log.error("OPA startup sync: failed to push policy {} — expressions may be invalid Rego: {}",
                            policyId, e.getMessage());
                    failed++;
                }
            }
        }

        log.info("OPA startup sync: {} policy documents synced, {} failed (check logs above for details)",
                synced, failed);
    }
}
