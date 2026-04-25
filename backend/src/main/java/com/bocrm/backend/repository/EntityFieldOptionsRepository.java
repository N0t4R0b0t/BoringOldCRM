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

import com.bocrm.backend.entity.EntityFieldOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * EntityFieldOptionsRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

public interface EntityFieldOptionsRepository extends JpaRepository<EntityFieldOptions, Long> {

    List<EntityFieldOptions> findByTenantIdAndEntityTypeIgnoreCase(Long tenantId, String entityType);

    Optional<EntityFieldOptions> findByTenantIdAndEntityTypeIgnoreCaseAndFieldNameIgnoreCase(
            Long tenantId, String entityType, String fieldName);
}
