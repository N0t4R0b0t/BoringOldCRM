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
package com.bocrm.backend.config;

import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.shared.TenantSchema;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Hibernate tenant resolver that routes database connections to the correct schema per request.
 *
 * <p>This is the bridge between {@link TenantContext} (request-scoped tenant ID) and Hibernate's
 * schema routing. Hibernate calls {@link #resolveCurrentTenantIdentifier()} before every database
 * query to determine which schema to use.
 *
 * <p><strong>How it works</strong>:
 * <ol>
 *   <li>JwtAuthenticationFilter sets TenantContext.tenantId = 42</li>
 *   <li>Service calls customerRepository.findByTenantId(42)</li>
 *   <li>Hibernate intercepts the query and calls {@link #resolveCurrentTenantIdentifier()}</li>
 *   <li>We return "tenant_42"</li>
 *   <li>SchemaPerTenantConnectionProvider (the connection provider) receives "tenant_42" and issues {@code SET search_path = tenant_42}</li>
 *   <li>Query executes against the tenant_42 schema</li>
 * </ol>
 *
 * <p>Configured in {@link HibernateConfig} with {@code hibernate.multitenancy.strategy = SCHEMA} and
 * {@code hibernate.tenant_identifier_resolver = this bean}.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
public class CurrentTenantSchemaResolver implements CurrentTenantIdentifierResolver<String> {

    /**
     * Resolve the current tenant's schema name from TenantContext.
     *
     * <p>Called by Hibernate before every database query to determine the schema to use. Returns
     * the schema name (e.g., "tenant_42") that will be passed to the connection provider.
     *
     * @return the schema name in format "tenant_{id}", or null if TenantContext.getTenantId() is null
     * @see TenantSchema#fromTenantId(Long)
     * @see SchemaPerTenantConnectionProvider#getSchema(String)
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        // WHY: TenantContext may be null if called outside HTTP request scope (e.g., scheduled task).
        // In that case, return null to use the connection's default schema (usually the connection pool default).
        return TenantSchema.fromTenantId(TenantContext.getTenantId());
    }

    /**
     * Validate that session-level tenant identifiers remain consistent during a request.
     *
     * @return true to allow Hibernate to validate tenant consistency; false to skip validation
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
