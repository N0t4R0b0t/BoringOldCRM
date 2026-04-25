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

import com.bocrm.backend.shared.TenantSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hibernate multi-tenant connection provider that routes Postgres connections to the correct schema.
 *
 * <p>This is the final piece of the schema-per-tenant architecture. After {@link CurrentTenantSchemaResolver}
 * resolves the tenant identifier (e.g., "tenant_42"), Hibernate passes it here. We then issue
 * {@code SET search_path = tenant_42, public} to make that schema and public schema visible to the connection.
 *
 * <p><strong>Flow</strong>:
 * <ol>
 *   <li>Service calls customerRepository.findByTenantId(42)</li>
 *   <li>Hibernate calls CurrentTenantSchemaResolver.resolveCurrentTenantIdentifier() → "tenant_42"</li>
 *   <li>Hibernate calls getConnection("tenant_42")</li>
 *   <li>We fetch a connection from the pool and issue {@code SET search_path = tenant_42, public}</li>
 *   <li>The query now searches tenant_42 first, then public (for admin tables)</li>
 *   <li>When done, releaseConnection() is called; we reset search_path to public and return the connection</li>
 * </ol>
 *
 * <p><strong>Why SET search_path?</strong> Postgres's search_path determines which schemas are searched when
 * a table is referenced without a schema prefix. By setting it to "tenant_42, public", unqualified table
 * names resolve in tenant_42 first (Customer, Contact, Opportunity, etc.), then public if not found
 * (for functions, types, or admin queries).
 *
 * <p>Configured in {@link HibernateConfig} with
 * {@code hibernate.multi_tenant_connection_provider = this bean}.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    /**
     * Get any connection from the pool without tenant context. Used internally for releasing connections.
     *
     * @return a bare connection from the pool
     * @throws SQLException if the pool is exhausted or unavailable
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Release a bare connection back to the pool. Called for connections that were not tenant-scoped.
     *
     * @param connection the connection to release
     * @throws SQLException if close fails
     */
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Get a connection scoped to a specific tenant schema.
     *
     * <p>Fetches a connection from the pool and issues {@code SET search_path} to route the connection
     * to the tenant's schema. If setSearchPath fails, the connection is cleaned up and the exception
     * is rethrown.
     *
     * @param tenantIdentifier the schema name (e.g., "tenant_42" or "public"), may be null
     * @return a connection with search_path set to the tenant schema
     * @throws SQLException if connection fetch or search_path command fails
     * @see #setSearchPath(Connection, String)
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        String schema = normalizeSchema(tenantIdentifier);
        try {
            setSearchPath(connection, schema);
            return connection;
        } catch (SQLException ex) {
            releaseAnyConnection(connection);
            throw ex;
        }
    }

    /**
     * Release a connection back to the pool and reset its search_path.
     *
     * <p>Before returning the connection to the pool, we reset search_path to public to ensure
     * the next requester doesn't accidentally inherit a previous request's tenant context.
     * This is a safeguard against connection pool reuse mistakes.
     *
     * @param tenantIdentifier the tenant schema that the connection was scoped to (not used, only for symmetry)
     * @param connection the connection to release
     * @throws SQLException if search_path reset fails
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // WHY: Reset search_path before returning to pool to prevent leakage if connection is reused
            // in the same thread or another thread. Always reset to public (the safe default).
            setSearchPath(connection, TenantSchema.PUBLIC_SCHEMA);
        } finally {
            releaseAnyConnection(connection);
        }
    }

    /**
     * Whether this provider supports aggressive connection release.
     *
     * @return false; we do not support aggressive release because we need to clean up search_path
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * Unwrap this provider to a specific interface type (for Hibernate internals).
     *
     * @param unwrapType the type to unwrap to
     * @return this if unwrapType is MultiTenantConnectionProvider, null otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        return null;
    }

    /**
     * Check if this provider can be unwrapped to a specific type.
     *
     * @param unwrapType the type to check
     * @return true if unwrapType is MultiTenantConnectionProvider or a subclass
     */
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.equals(unwrapType) ||
                SchemaPerTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    /**
     * Issue a SQL SET search_path command on the connection.
     *
     * <p>Sets the schema search path to the given schema plus the public schema. This makes
     * unqualified table references resolve in the tenant schema first, then public.
     *
     * <p>Example: {@code SET search_path = tenant_42, public}
     *
     * @param connection the DB connection to configure
     * @param schema the primary schema to search (e.g., "tenant_42")
     * @throws SQLException if the SET command fails
     */
    private void setSearchPath(Connection connection, String schema) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema + ", " + TenantSchema.PUBLIC_SCHEMA);
        }
    }

    /**
     * Normalize and validate a schema identifier.
     *
     * <p>Validates that the schema name matches the PostgreSQL identifier rules and is safe to
     * inject into SQL. Falls back to public schema if the identifier is invalid to prevent SQL injection.
     *
     * @param schema the raw schema identifier from the resolver
     * @return the normalized schema name, or "public" if invalid
     */
    private String normalizeSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return TenantSchema.PUBLIC_SCHEMA;
        }
        String trimmed = schema.trim();
        // WHY: Regex check prevents SQL injection if a malicious schema name somehow reaches here
        if (!trimmed.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            log.warn("Invalid tenant schema identifier '{}', falling back to public", trimmed);
            return TenantSchema.PUBLIC_SCHEMA;
        }
        return trimmed;
    }
}
