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

import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.shared.TenantSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
/**
 * TenantFlywayMigrationService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantFlywayMigrationService {

    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    /**
     * Migrate a single tenant schema using Flyway.
     * Creates the schema if it doesn't exist, then applies all pending migrations.
     */
    public void migrateSchema(Long tenantId) {
        String schema = TenantSchema.fromTenantId(tenantId);
        if (TenantSchema.PUBLIC_SCHEMA.equals(schema)) {
            return;
        }

        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/tenant-migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .validateOnMigrate(true)
                .table("flyway_schema_history")
                .load();

            flyway.migrate();
            log.info("Flyway migrated tenant schema: {}", schema);
        } catch (Exception e) {
            log.error("Flyway migration failed for tenant {}: {}", tenantId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate all existing tenant schemas.
     * Iterates through all tenants and applies pending migrations to each schema.
     * Errors are logged but don't stop migration of other tenants.
     */
    public void migrateAllSchemas() {
        tenantRepository.findAll().forEach(tenant -> {
            try {
                migrateSchema(tenant.getId());
            } catch (Exception e) {
                log.error("Flyway migration failed for tenant {}: {}", tenant.getId(), e.getMessage());
                // Continue with next tenant instead of failing completely
            }
        });
    }
}
