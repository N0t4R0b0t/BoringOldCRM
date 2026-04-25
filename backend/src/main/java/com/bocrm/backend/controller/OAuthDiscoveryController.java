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
package com.bocrm.backend.controller;

import com.bocrm.backend.config.ExternalAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
/**
 * OAuthDiscoveryController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/.well-known")
@Slf4j
public class OAuthDiscoveryController {

    private final ExternalAuthProperties externalAuthProperties;
    private final String publicBaseUrl;

    public OAuthDiscoveryController(ExternalAuthProperties externalAuthProperties,
                                    @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.externalAuthProperties = externalAuthProperties;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping("/oauth-protected-resource")
    public ResponseEntity<Map<String, Object>> getOAuthProtectedResource() {
        if (!externalAuthProperties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }

        // RFC 9728: Protected Resource Metadata.
        // Points mcp-remote at Auth0 as the authorization server so it can fetch
        // Auth0's own /.well-known/openid-configuration for the full OAuth metadata.
        String issuer = externalAuthProperties.getIssuerUri();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resource", publicBaseUrl + "/api");
        metadata.put("authorization_servers", new String[]{issuer});
        metadata.put("scopes_supported", new String[]{"openid", "profile", "email"});
        metadata.put("bearer_methods_supported", new String[]{"header"});

        log.debug("OAuth protected resource metadata requested");
        return ResponseEntity.ok(metadata);
    }
}
