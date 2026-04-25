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
import com.bocrm.backend.util.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import com.bocrm.backend.exception.ExpiredTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Spring Security filter that validates JWT tokens and populates TenantContext and SecurityContext.
 *
 * <p>This filter runs on every HTTP request and is the critical entry point for multi-tenancy and authentication.
 * It extracts the JWT from the Authorization header, validates it, extracts claims (userId, tenantId, role),
 * and stores them in TenantContext (ThreadLocal) and Spring SecurityContext for downstream use by services.
 *
 * <p><strong>Request Flow</strong>:
 * <ol>
 *   <li>Request arrives with Authorization header (e.g., "Bearer eyJhbGc...")
 *   <li>JwtAuthenticationFilter.doFilterInternal() is called
 *   <li>Extract JWT from header
 *   <li>Validate signature and expiration
 *   <li>If valid: extract userId, tenantId, role → set TenantContext + SecurityContext
 *   <li>If expired: return 401 with TOKEN_EXPIRED error code
 *   <li>If invalid: return 401 with "Invalid token"
 *   <li>Always: call TenantContext.clear() in finally block to clean up ThreadLocal
 *   <li>Process the request downstream (services/controllers)
 * </ol>
 *
 * <p><strong>Exception Handling</strong>: Note the two calls to jwtProvider:
 * <ul>
 *   <li>parseClaims() — tries to parse and explicitly catch ExpiredJwtException
 *   <li>validateToken() — validates signature; throws custom ExpiredTokenException on expiry
 * </ul>
 * This dual approach allows us to distinguish expired tokens (TOKEN_EXPIRED errorCode)
 * from signature failures (generic 401).
 *
 * <p><strong>Tenant Context Population</strong>: This filter is the ONLY place in the codebase
 * that should call TenantContext.setTenantId() and TenantContext.setUserId(). No other code
 * should modify TenantContext.
 *
 * <p><strong>Skipped Endpoints</strong>: Some endpoints bypass JWT checks (OPTIONS preflight,
 * auth endpoints, WebSocket upgrades, MCP SSE). These are listed in doFilterInternal.
 *
 * @see com.bocrm.backend.shared.TenantContext
 * @see com.bocrm.backend.util.JwtProvider
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /**
     * Process an HTTP request: validate JWT, populate TenantContext and SecurityContext.
     *
     * <p>Called once per request. Extracts JWT from Authorization header, validates it,
     * and sets up authentication for downstream filters and service methods.
     *
     * <p><strong>Note</strong>: TenantContext.clear() is called in the finally block,
     * which runs even if an exception occurs. This is critical for servlet thread pools
     * where threads are reused and stale ThreadLocal values cause cross-tenant data leakage.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if filter processing fails
     * @throws IOException if I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = null;
        try {
            // WHY: OPTIONS (CORS preflight) must bypass JWT checks; browsers send preflight without auth headers
            String uri = request.getRequestURI();
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            // WHY: Skip token parsing for these endpoints:
            // - /ws: WebSocket upgrade requests
            // - /mcp: MCP server endpoints (use API key auth instead)
            // - /sse: Server-sent events (async push, not traditional request-response)
            // - /auth/refresh: Token refresh (uses refresh token, not access token)
            // - /auth/login: Initial login (uses credentials, not JWT)
            if (uri != null && (uri.contains("/ws") || uri.startsWith("/mcp") || uri.contains("/sse") || uri.contains("/auth/refresh") || uri.contains("/auth/login") || (uri.contains("/documents/") && uri.endsWith("/file")))) {
                filterChain.doFilter(request, response);
                return;
            }

            jwt = getJwtFromRequest(request);

            if (!StringUtils.hasText(jwt)) {
                log.debug("No Authorization header present for request: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // WHY: API keys (sk-bocrm-*) are handled by a separate filter (McpApiKeyAuthenticationFilter).
            // Skip JWT parsing here and let that filter handle it.
            if (jwt.startsWith("sk-bocrm-")) {
                log.debug("API key detected, skipping JWT parsing for request: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // WHY: Parse claims first to explicitly detect expired tokens and return TOKEN_EXPIRED error.
                // validateToken() also detects expiry but as a side effect (throws ExpiredTokenException);
                // here we catch ExpiredJwtException from JJWT and respond immediately.
                jwtProvider.parseClaims(jwt);
            } catch (ExpiredJwtException eje) {
                log.debug("Expired JWT for request {} {}: {}", request.getMethod(), request.getRequestURI(), eje.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                String path = request.getRequestURI();
                String payload = String.format(
                        "{\"message\":\"Token expired\",\"errorCode\":\"TOKEN_EXPIRED\",\"status\":401,\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(), path
                );
                response.getWriter().write(payload);
                return;
            }

            // WHY: validateToken() also checks expiry and throws ExpiredTokenException (custom).
            // We catch it separately below. This method also logs and returns false for other errors.
            boolean valid = jwtProvider.validateToken(jwt);
            log.debug("JWT present for request {} {}, valid={}", request.getMethod(), request.getRequestURI(), valid);

            if (valid) {
                Long userId = jwtProvider.getUserIdFromToken(jwt);
                Long tenantId = jwtProvider.getTenantIdFromToken(jwt);
                String tokenType = jwtProvider.getTokenTypeFromToken(jwt);
                String role = jwtProvider.getRoleFromToken(jwt);

                log.debug("Parsed JWT claims for request {} {}: userId={}, tenantId={}, tokenType={}, role={}",
                        request.getMethod(), request.getRequestURI(), userId, tenantId, tokenType, role);

                // WHY: Accept tokens with tokenType=access AND check that required claims are present.
                // Onboarding tokens have role="onboarding" and no tenantId; they're valid for signup only.
                // Regular access tokens must have tenantId (the tenant context for routing).
                boolean isOnboarding = "onboarding".equalsIgnoreCase(role);
                if (userId != null && "access".equals(tokenType) && StringUtils.hasText(role)
                        && (tenantId != null || isOnboarding)) {
                    String normalizedRole = role.trim().toUpperCase();
                    var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + normalizedRole));

                    // WHY: Create UsernamePasswordAuthenticationToken with userId as principal.
                    // Spring Security requires this to be set so that downstream filters/services can check
                    // isAuthenticated() and get the principal via SecurityContextHolder.getContext().getAuthentication().
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // WHY: These calls are the ONLY place TenantContext should be set.
                    // Services and controllers read from TenantContext; they must never write to it.
                    TenantContext.setUserId(userId);
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                    }

                    log.debug("Set authentication for user: {} and tenant: {} with authorities={}", userId, tenantId, authorities);
                } else {
                    log.debug("JWT did not contain required claims or was not access token: userId={}, tenantId={}, tokenType={}, role={}",
                            userId, tenantId, tokenType, role);
                    // If token claims are missing or invalid, respond 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Invalid token claims\"}");
                    return;
                }
            } else {
                log.debug("JWT failed validation for request {} {}", request.getMethod(), request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid token\"}");
                return;
            }

            // WHY: filterChain.doFilter() passes the request to the next filter in the chain.
            // At this point, TenantContext and SecurityContext have been populated for downstream use.
            // If token was invalid, we've already returned an error response above.
            filterChain.doFilter(request, response);
        } catch (ExpiredTokenException ete) {
            // WHY: Custom exception from validateToken() on expiry. This is for cases where validateToken
            // itself throws (vs. parseClaims() above, which we explicitly catch ExpiredJwtException).
            log.debug("Expired token for request {} {}: {}", request.getMethod(), request.getRequestURI(), ete.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String path = request.getRequestURI();
            String payload = String.format(
                    "{\"message\":\"%s\",\"errorCode\":\"TOKEN_EXPIRED\",\"status\":401,\"timestamp\":\"%s\",\"path\":\"%s\"}",
                    ete.getMessage(), java.time.Instant.now().toString(), path
            );
            response.getWriter().write(payload);
        } catch (JwtException je) {
            // Catch any other JJWT exception (malformed, unsupported, etc.)
            log.error("JWT processing error for request {} {}", request.getMethod(), request.getRequestURI(), je);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token error\"}");
        } catch (Exception ex) {
            // Unexpected errors; proceed without authentication to let downstream security catch it
            log.error("Could not set user authentication in security context", ex);
            // WHY: Don't respond with 401 here; let downstream filters/security managers handle
            // the missing authentication. They'll typically return 403 Forbidden.
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Clear TenantContext to prevent leakage in servlet thread pools.
            // Servlet containers reuse threads; if we don't clear, the next request in the same thread
            // will inherit the previous request's tenantId/userId, leaking data across tenants.
            // This finally block runs even if an exception occurred.
            TenantContext.clear();
        }
    }

    /**
     * Extract JWT token from the Authorization header.
     *
     * <p>Expected format: "Bearer {token}". Returns the token part without the "Bearer " prefix,
     * or null if the header is missing or malformed.
     *
     * @param request the HTTP request
     * @return the JWT token, or null if not present or invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
