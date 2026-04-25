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

import com.bocrm.backend.entity.McpApiKey;
import com.bocrm.backend.entity.TenantMembership;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.repository.McpApiKeyRepository;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.service.ExternalIdentityService;
import com.bocrm.backend.service.McpApiKeyService;
import com.bocrm.backend.shared.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
/**
 * McpApiKeyAuthenticationFilter.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Component
@Slf4j
public class McpApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final McpApiKeyRepository mcpApiKeyRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final McpApiKeyService mcpApiKeyService;
    private final ExternalIdentityService externalIdentityService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ExternalAuthProperties externalAuthProperties;
    private final String publicBaseUrl;

    public McpApiKeyAuthenticationFilter(McpApiKeyRepository mcpApiKeyRepository,
                                         TenantMembershipRepository membershipRepository,
                                         UserRepository userRepository,
                                         McpApiKeyService mcpApiKeyService,
                                         ExternalIdentityService externalIdentityService,
                                         org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                                         ExternalAuthProperties externalAuthProperties,
                                         @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.mcpApiKeyRepository = mcpApiKeyRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.mcpApiKeyService = mcpApiKeyService;
        this.externalIdentityService = externalIdentityService;
        this.passwordEncoder = passwordEncoder;
        this.externalAuthProperties = externalAuthProperties;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String uri = request.getRequestURI();
            boolean isMcpEndpoint = uri != null && (uri.contains("/mcp/") || uri.endsWith("/sse") || uri.contains("/sse?"));

            if (!isMcpEndpoint) {
                filterChain.doFilter(request, response);
                return;
            }

            String bearerToken = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
                // No token — if OAuth is configured, challenge with resource metadata URL so
                // mcp-remote can open the browser-based Auth0 login flow.
                if (externalAuthProperties.isEnabled() && StringUtils.hasText(publicBaseUrl)) {
                    String metadataUrl = publicBaseUrl + "/api/.well-known/oauth-protected-resource";
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + metadataUrl + "\"");
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"unauthorized\",\"resource_metadata\":\"" + metadataUrl + "\"}");
                    return;
                }
                // OAuth not configured — allow through (tools will fail if tenant context needed)
                filterChain.doFilter(request, response);
                return;
            }

            String token = bearerToken.substring(7);

            if (token.startsWith("sk-bocrm-")) {
                authenticateApiKey(token);
            } else if (token.startsWith("eyJ")) {
                authenticateJwt(token);
            } else {
                log.debug("Unknown token format for MCP endpoint, skipping authentication");
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticateApiKey(String token) {
        // Extract prefix (first 12 chars)
        String prefix = token.substring(0, 12);

        // Look up candidates by prefix and enabled status
        List<McpApiKey> candidates = mcpApiKeyRepository.findByKeyPrefixAndEnabled(prefix, true);

        McpApiKey matched = null;
        for (McpApiKey candidate : candidates) {
            if (passwordEncoder.matches(token, candidate.getKeyHash())) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            log.warn("No matching API key found for prefix: {}", prefix);
            return;
        }

        // Look up the user's role via membership
        String role = membershipRepository.findByTenantIdAndUserId(matched.getTenantId(), matched.getUserId())
                .map(m -> "ROLE_" + m.getRole().toUpperCase())
                .orElse("ROLE_USER");

        // Set SecurityContextHolder
        var authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                matched.getUserId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Set TenantContext
        TenantContext.setTenantId(matched.getTenantId());
        TenantContext.setUserId(matched.getUserId());

        log.debug("Authenticated MCP API key for user: {} and tenant: {}", matched.getUserId(), matched.getTenantId());

        // Update last_used_at (best-effort, non-blocking)
        try {
            mcpApiKeyService.updateLastUsed(matched);
        } catch (Exception e) {
            log.warn("Failed to update last_used_at for API key {}", matched.getId(), e);
        }
    }

    private void authenticateJwt(String token) {
        try {
            // Validate JWT token via ExternalIdentityService
            ExternalIdentityService.ExternalUserIdentity identity = externalIdentityService.validate(token);

            // Find or create user from external identity
            Optional<User> existingUser = userRepository.findByEmail(identity.email());
            User user = existingUser.orElse(null);

            if (user == null) {
                log.warn("JWT user not found in BOCRM: {}", identity.email());
                return;
            }

            // Resolve tenant from org_id in token
            Long tenantId = null;
            if (!identity.organizations().isEmpty()) {
                ExternalIdentityService.OrganizationIdentity org = identity.organizations().get(0);
                // Try to find tenant by external_org_id
                tenantId = membershipRepository.findByUserId(user.getId()).stream()
                        .filter(m -> org.id().equals(m.getTenantId().toString()) ||
                                     org.name().equals(m.getTenantId().toString()))
                        .map(TenantMembership::getTenantId)
                        .findFirst()
                        .orElse(null);
            }

            // If no org claim, use first active membership
            if (tenantId == null) {
                tenantId = membershipRepository.findByUserId(user.getId()).stream()
                        .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                        .map(TenantMembership::getTenantId)
                        .findFirst()
                        .orElse(null);
            }

            if (tenantId == null) {
                log.warn("Could not resolve tenant for JWT user: {}", identity.email());
                return;
            }

            // Look up the user's role via membership
            String role = membershipRepository.findByTenantIdAndUserId(tenantId, user.getId())
                    .map(m -> "ROLE_" + m.getRole().toUpperCase())
                    .orElse("ROLE_USER");

            // Set SecurityContextHolder
            var authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user.getId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Set TenantContext
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(user.getId());

            log.debug("Authenticated JWT for user: {} and tenant: {}", user.getId(), tenantId);
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        }
    }
}
