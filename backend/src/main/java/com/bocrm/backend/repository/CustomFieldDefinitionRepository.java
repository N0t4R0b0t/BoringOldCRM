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

import com.bocrm.backend.entity.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
/**
 * CustomFieldDefinitionRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, Long> {
    List<CustomFieldDefinition> findByTenantIdAndEntityType(Long tenantId, String entityType);
    List<CustomFieldDefinition> findByTenantIdAndEntityTypeOrderByDisplayOrderAscIdAsc(Long tenantId, String entityType);
    Optional<CustomFieldDefinition> findByTenantIdAndEntityTypeAndKey(Long tenantId, String entityType, String key);

    /**
     * Fetch all field definitions for a tenant
     * Used during migration to determine which fields should display in tables
     */
    List<CustomFieldDefinition> findByTenantId(Long tenantId);
}


