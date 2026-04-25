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
package com.bocrm.backend.repository;

import com.bocrm.backend.entity.PolicyRule;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * PolicyRuleRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    List<PolicyRule> findByTenantIdAndEntityTypeIgnoreCaseOrderByDisplayOrderAscIdAsc(
            Long tenantId, String entityType);

    List<PolicyRule> findByTenantIdAndEntityTypeIgnoreCase(
            Long tenantId, String entityType);

    @Cacheable(value = "PolicyRules", key = "#tenantId + '_' + #entityType + '_' + #operation")
    @Query("SELECT p FROM PolicyRule p WHERE p.tenantId = :tenantId " +
           "AND LOWER(p.entityType) = LOWER(:entityType) " +
           "AND p.enabled = true " +
           "AND CAST(p.operations AS text) LIKE CONCAT('%', :operation, '%') " +
           "ORDER BY p.displayOrder ASC, p.id ASC")
    List<PolicyRule> findEnabledRulesContainingOperation(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType,
            @Param("operation") String operation);

    List<PolicyRule> findByTenantIdAndEntityTypeIgnoreCaseAndEnabled(
            Long tenantId, String entityType, boolean enabled);

    @Query("SELECT p FROM PolicyRule p WHERE p.enabled = true ORDER BY p.tenantId ASC, p.entityType ASC, p.displayOrder ASC, p.id ASC")
    List<PolicyRule> findAllEnabled();
}
