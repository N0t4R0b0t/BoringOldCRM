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

import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.service.DocumentDownloadTokenService;
import com.bocrm.backend.shared.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Authenticates GET /documents/&#123;id&#125;/file requests via a signed {@code ?t=} token.
 * Matches the MCP API-key filter pattern: set TenantContext + SecurityContext on success,
 * always clear TenantContext in finally.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentDownloadTokenFilter extends OncePerRequestFilter {

    private static final Pattern PATH = Pattern.compile(".*/documents/(\\d+)/file$");

    private final DocumentDownloadTokenService tokenService;
    private final TenantMembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        var m = uri == null ? null : PATH.matcher(uri);
        if (m == null || !m.matches() || !"GET".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        long docId;
        try { docId = Long.parseLong(m.group(1)); } catch (NumberFormatException e) {
            chain.doFilter(request, response); return;
        }

        String token = request.getParameter("t");
        try {
            var payload = tokenService.verify(token, docId);

            String role = membershipRepository.findByTenantIdAndUserId(payload.tenantId(), payload.userId())
                    .map(mem -> "ROLE_" + mem.getRole().toUpperCase())
                    .orElse("ROLE_USER");

            var auth = new UsernamePasswordAuthenticationToken(
                    payload.userId(), null,
                    Collections.singletonList(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            TenantContext.setTenantId(payload.tenantId());
            TenantContext.setUserId(payload.userId());

            chain.doFilter(request, response);
        } catch (DocumentDownloadTokenService.InvalidTokenException e) {
            log.warn("Rejecting signed download: {} (docId={})", e.getMessage(), docId);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired download token");
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
