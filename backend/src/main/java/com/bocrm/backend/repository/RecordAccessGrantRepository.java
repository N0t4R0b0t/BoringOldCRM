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

import com.bocrm.backend.entity.RecordAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
/**
 * RecordAccessGrantRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

public interface RecordAccessGrantRepository extends JpaRepository<RecordAccessGrant, Long> {

    List<RecordAccessGrant> findByTenantIdAndEntityTypeAndEntityId(
            Long tenantId, String entityType, Long entityId);

    Optional<RecordAccessGrant> findByTenantIdAndEntityTypeAndEntityIdAndGranteeTypeAndGranteeId(
            Long tenantId, String entityType, Long entityId, String granteeType, Long granteeId);

    /**
     * Returns entity IDs from the candidate list where the given user (directly or via groups)
     * has an explicit grant. Used to compute access when filtering lists.
     */
    @Query("SELECT g.entityId FROM RecordAccessGrant g " +
           "WHERE g.tenantId = :tenantId AND g.entityType = :entityType " +
           "AND g.entityId IN :entityIds " +
           "AND ((g.granteeType = 'USER' AND g.granteeId = :userId) " +
           "  OR (g.granteeType = 'GROUP' AND g.granteeId IN :groupIds))")
    Set<Long> findGrantedEntityIds(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType,
            @Param("entityIds") List<Long> entityIds,
            @Param("userId") Long userId,
            @Param("groupIds") Set<Long> groupIds);

    void deleteByTenantIdAndEntityTypeAndEntityId(Long tenantId, String entityType, Long entityId);

    List<RecordAccessGrant> findByTenantId(Long tenantId);
}
