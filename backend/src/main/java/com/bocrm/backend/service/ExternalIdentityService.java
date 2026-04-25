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

import com.bocrm.backend.config.ExternalAuthProperties;
import com.bocrm.backend.exception.UnauthorizedException;
import com.bocrm.backend.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/**
 * ExternalIdentityService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalIdentityService {

    private final ExternalAuthProperties properties;
    private volatile JwtDecoder jwtDecoder;

    public ExternalUserIdentity validate(String idToken) {
        if (!properties.isEnabled()) {
            throw new ValidationException("External login is disabled for this environment");
        }
        if (!StringUtils.hasText(idToken)) {
            throw new ValidationException("Missing external identity token");
        }

        Jwt jwt;
        try {
            jwt = getJwtDecoder().decode(idToken);
        } catch (JwtException ex) {
            log.warn("External auth: token decode failed: {}", ex.getMessage());
            throw new UnauthorizedException("Unable to validate external identity token (invalid or expired)");
        }

        validateAudience(jwt);

        String email = readStringClaim(jwt.getClaim(properties.getEmailClaim()));
        if (!StringUtils.hasText(email)) {
            throw new UnauthorizedException("External login failed: token does not contain an email address");
        }

        String displayName = firstPresent(
                readStringClaim(jwt.getClaim("name")),
                readStringClaim(jwt.getClaim("nickname")),
                email
        );

        if (properties.isRequireEmailVerified()) {
            Object emailVerifiedRaw = jwt.getClaim("email_verified");
            if (!(emailVerifiedRaw instanceof Boolean) || !((Boolean) emailVerifiedRaw)) {
                log.warn("External auth: email_verified is missing/false: {}", emailVerifiedRaw);
                throw new UnauthorizedException("External login failed: email is not verified by the identity provider");
            }
        }

        String subject = jwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new UnauthorizedException("External login failed: token is missing subject/user id");
        }

        List<OrganizationIdentity> organizations = parseOrganizations(jwt.getClaims());
        if (organizations.size() > 1) {
            throw new UnauthorizedException("External login failed: multiple organizations found in token. Please sign in with a specific organization.");
        }

        // Parse roles from token (either as array or single role string)
        List<String> tokenRoles = parseTokenRoles(jwt.getClaims());

        // If no array-based roles found, check for single "role" claim from Auth0 Action
        if (tokenRoles.isEmpty()) {
            String singleRole = readStringClaim(jwt.getClaim("role"));
            if (StringUtils.hasText(singleRole)) {
                tokenRoles = List.of(singleRole);
            }
        }

        return new ExternalUserIdentity(email, displayName, subject, organizations, tokenRoles);
    }

    private JwtDecoder getJwtDecoder() {
        JwtDecoder decoder = jwtDecoder;
        if (decoder != null) {
            return decoder;
        }
        synchronized (this) {
            if (jwtDecoder == null) {
                if (StringUtils.hasText(properties.getJwkSetUri())) {
                    jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
                } else if (StringUtils.hasText(properties.getIssuerUri())) {
                    jwtDecoder = JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
                } else {
                    throw new ValidationException("External auth misconfigured: issuer-uri or jwk-set-uri is required");
                }
            }
            return jwtDecoder;
        }
    }

    private void validateAudience(Jwt jwt) {
        if (!StringUtils.hasText(properties.getAudience())) {
            return;
        }
        List<String> audiences = jwt.getAudience();
        if (audiences == null || audiences.stream().noneMatch(properties.getAudience()::equals)) {
            throw new UnauthorizedException("External login failed: token audience is invalid for this application");
        }
    }

    private List<String> parseTokenRoles(Map<String, Object> claims) {
        String rolesClaim = properties.getRolesClaim();

        // Try configurable roles claim first (e.g., "roles" or custom claim name)
        Object raw = claims.get(rolesClaim);
        if (raw instanceof Collection<?> list) {
            return list.stream()
                    .map(this::readStringClaim)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        // Fallback: try to extract roles from organizations array
        // This supports Auth0 Actions that embed roles in the organization object
        Object orgsRaw = claims.get("organizations");
        if (orgsRaw instanceof Collection<?> orgs) {
            for (Object orgObj : orgs) {
                if (orgObj instanceof Map<?, ?> orgMap) {
                    Object rolesInOrg = orgMap.get("roles");
                    if (rolesInOrg instanceof Collection<?> rolesList) {
                        return rolesList.stream()
                                .map(this::readStringClaim)
                                .filter(StringUtils::hasText)
                                .toList();
                    }
                }
            }
        }

        return List.of();
    }

    private List<OrganizationIdentity> parseOrganizations(Map<String, Object> claims) {
        Map<String, OrganizationIdentity> orgs = new LinkedHashMap<>();

        String singleOrgId = readStringClaim(claims.get(properties.getOrganizationIdClaim()));
        Object organizationsRaw = claims.get(properties.getOrganizationsClaim());
        if (organizationsRaw instanceof Collection<?> organizations) {
            for (Object raw : organizations) {
                OrganizationIdentity parsed = parseOrganization(raw);
                if (parsed != null && StringUtils.hasText(parsed.id())) {
                    orgs.put(parsed.id(), parsed);
                }
            }
        }

        if (StringUtils.hasText(singleOrgId)) {
            OrganizationIdentity fromOrganizations = orgs.get(singleOrgId);
            if (fromOrganizations != null) {
                return List.of(fromOrganizations);
            }

            String singleOrgName = firstPresent(
                    readStringClaim(claims.get("org_name")),
                    readStringClaim(claims.get("organization_name")),
                    singleOrgId
            );
            return List.of(new OrganizationIdentity(singleOrgId, singleOrgName, "user"));
        }

        Object orgIdsRaw = claims.get(properties.getOrganizationIdsClaim());
        if (orgIdsRaw instanceof Collection<?> orgIds) {
            for (Object raw : orgIds) {
                String id = readStringClaim(raw);
                if (StringUtils.hasText(id) && !orgs.containsKey(id)) {
                    orgs.put(id, new OrganizationIdentity(id, id, "user"));
                }
            }
        }

        return new ArrayList<>(orgs.values());
    }

    private OrganizationIdentity parseOrganization(Object raw) {
        if (raw instanceof String id) {
            return new OrganizationIdentity(id, id, "user");
        }
        if (!(raw instanceof Map<?, ?> mapRaw)) {
            return null;
        }

        String id = firstPresent(
                readStringClaim(mapRaw.get("id")),
                readStringClaim(mapRaw.get("org_id")),
                readStringClaim(mapRaw.get("organization_id"))
        );
        if (!StringUtils.hasText(id)) {
            return null;
        }

        String name = firstPresent(
                readStringClaim(mapRaw.get("name")),
                readStringClaim(mapRaw.get("org_name")),
                readStringClaim(mapRaw.get("display_name")),
                id
        );

        String role = firstPresent(
                readStringClaim(mapRaw.get("role")),
                readStringClaim(mapRaw.get("membership_role")),
                "user"
        );

        return new OrganizationIdentity(id, name, role);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String readStringClaim(Object raw) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof CharSequence) && !(raw instanceof Number) && !(raw instanceof Boolean)) {
            return null;
        }
        String value = Objects.toString(raw, null);
        return StringUtils.hasText(value) ? value : null;
    }

    public record ExternalUserIdentity(String email, String displayName, String subject, List<OrganizationIdentity> organizations, List<String> roles) {}
    public record OrganizationIdentity(String id, String name, String role) {}
}
