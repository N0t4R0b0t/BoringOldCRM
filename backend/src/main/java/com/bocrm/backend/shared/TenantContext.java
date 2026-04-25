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
package com.bocrm.backend.shared;

import lombok.Data;

/**
 * ThreadLocal holder for tenant and user context during HTTP request processing.
 *
 * <p>This is the core mechanism for BOCRM's multi-tenancy implementation. Each HTTP request
 * contains a JWT with {@code tenantId} and {@code userId} claims. The {@link
 * com.bocrm.backend.filter.JwtAuthenticationFilter JwtAuthenticationFilter} extracts these
 * claims and stores them in TenantContext at request start. Services and Hibernate schema
 * resolvers then read from TenantContext to route database access to the correct tenant schema.
 *
 * <p><strong>Lifecycle</strong>:
 * <ol>
 *   <li>Request arrives with JWT token</li>
 *   <li>JwtAuthenticationFilter extracts tenantId/userId → calls {@link #setTenantId(Long)} and {@link #setUserId(Long)}</li>
 *   <li>Hibernate schema resolver reads {@link #getTenantId()} → issues {@code SET search_path = tenant_<id>}</li>
 *   <li>Service methods validate tenant context: {@code if (getTenantId() == null) throw ForbiddenException}</li>
 *   <li>Response is built</li>
 *   <li>JwtAuthenticationFilter calls {@link #clear()} to clean up ThreadLocal</li>
 * </ol>
 *
 * <p><strong>CRITICAL</strong>: {@link #clear()} <em>must</em> be called after every request, even on exceptions.
 * In a thread pool, if TenantContext is not cleared, a subsequent request in the same thread may see
 * the previous request's context, causing cross-tenant data leaks. The filter handles this via finally block.
 *
 * <p><strong>Never</strong> call {@code setTenantId()} or {@code setUserId()} outside of
 * JwtAuthenticationFilter. Services should only <em>read</em> the context to validate it's set.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Data
public class TenantContext {
    // WHY: ThreadLocal instead of request attributes? ThreadLocal is more explicit about lifecycle
    // management and allows schema resolvers (deep in Hibernate) to access context without method signatures.
    private static final ThreadLocal<Long> tenantId = new ThreadLocal<>();
    private static final ThreadLocal<Long> userId = new ThreadLocal<>();

    /**
     * Set the current tenant ID in context. Called only by JwtAuthenticationFilter after JWT validation.
     *
     * @param id the tenant ID from the JWT claim, never null
     */
    public static void setTenantId(Long id) {
        tenantId.set(id);
    }

    /**
     * Get the current tenant ID from context.
     *
     * @return the tenant ID, or null if not set (e.g., unauthenticated request or outside request scope)
     */
    public static Long getTenantId() {
        return tenantId.get();
    }

    /**
     * Remove the tenant ID from context. Called by clear() to prevent leakage in thread pools.
     *
     * @see #clear()
     */
    public static void removeTenantId() {
        tenantId.remove();
    }

    /**
     * Set the current user ID in context. Called only by JwtAuthenticationFilter after JWT validation.
     *
     * @param id the user ID from the JWT claim, never null
     */
    public static void setUserId(Long id) {
        userId.set(id);
    }

    /**
     * Get the current user ID from context.
     *
     * @return the user ID, or null if not set
     */
    public static Long getUserId() {
        return userId.get();
    }

    /**
     * Remove the user ID from context. Called by clear() to prevent leakage in thread pools.
     *
     * @see #clear()
     */
    public static void removeUserId() {
        userId.remove();
    }

    /**
     * Clear both tenant and user IDs from context. Must be called after every HTTP request completes,
     * even if an exception occurred, to prevent ThreadLocal pollution in servlet containers.
     *
     * <p>Called by JwtAuthenticationFilter in a finally block after building the response.
     *
     * @see com.bocrm.backend.filter.JwtAuthenticationFilter
     */
    public static void clear() {
        tenantId.remove();
        userId.remove();
    }
}
