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

import com.bocrm.backend.entity.UserGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
/**
 * UserGroupMembershipRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, Long> {

    List<UserGroupMembership> findByTenantId(Long tenantId);

    List<UserGroupMembership> findByGroupIdAndTenantId(Long groupId, Long tenantId);

    List<UserGroupMembership> findByUserIdAndTenantId(Long userId, Long tenantId);

    Optional<UserGroupMembership> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    @Query("SELECT m.groupId FROM UserGroupMembership m WHERE m.userId = :userId AND m.tenantId = :tenantId")
    Set<Long> findGroupIdsByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
