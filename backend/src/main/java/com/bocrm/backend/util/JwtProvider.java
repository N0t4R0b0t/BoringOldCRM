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
package com.bocrm.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.bocrm.backend.config.JwtProperties;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token provider for generating, validating, and extracting claims from JWT tokens.
 *
 * <p>BOCRM uses JWTs for stateless authentication. Tokens contain tenantId, userId, role, and tokenType.
 * They are signed with an HMAC-SHA256 secret (configured in application.yml).
 *
 * <p><strong>Token Types</strong>:
 * <ul>
 *   <li><strong>access</strong>: Short-lived (default 15 min); used for all API requests. Contains tenantId.
 *   <li><strong>refresh</strong>: Long-lived (default 7 days); used to obtain a new access token without re-authenticating.
 *   <li><strong>onboarding</strong>: Short-lived (15 min); used during signup to create the first tenant/user. No tenantId.
 * </ul>
 *
 * <p><strong>Claims</strong>:
 * <ul>
 *   <li>userId (Long): the user ID, always present
 *   <li>tenantId (Long): the tenant context, present for access/refresh tokens, null for onboarding
 *   <li>role (String): the user's role in the tenant (ADMIN, USER, SYSTEM_ADMIN, onboarding, etc.)
 *   <li>tokenType (String): "access", "refresh", or "onboarding"
 * </ul>
 *
 * <p>JwtAuthenticationFilter extracts these claims on every request and populates TenantContext.
 * Services then read from TenantContext to enforce multi-tenancy and role-based access control.
 *
 * @see com.bocrm.backend.config.JwtAuthenticationFilter
 * @see com.bocrm.backend.shared.TenantContext
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final SecretKey key;

    /**
     * Construct JwtProvider with configuration.
     *
     * @param jwtProperties the JWT config (secret, expiration times)
     */
    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Generate a short-lived access token for API requests.
     *
     * <p>Access tokens are valid for ~15 minutes and contain userId, tenantId, and role.
     * They are sent with every API request in the Authorization header and validated by
     * JwtAuthenticationFilter to populate TenantContext and SecurityContext.
     *
     * @param userId the user ID (required)
     * @param tenantId the tenant ID (required for multi-tenant access)
     * @param role the user's role in the tenant (e.g., "ADMIN", "USER")
     * @return a signed JWT token
     */
    public String generateAccessToken(Long userId, Long tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId);
        claims.put("role", role);
        return createToken(claims, "access", jwtProperties.getExpiration());
    }

    /**
     * Generate a token for onboarding (first-time signup).
     *
     * <p>Onboarding tokens are intentionally short-lived (15 min) and lack a tenantId,
     * because the tenant hasn't been created yet. Used to protect the signup flow.
     *
     * @param userId the new user ID
     * @return a signed JWT token with type "onboarding"
     */
    public String generateOnboardingToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", "onboarding");
        // WHY: No tenantId — intentional. The user is creating their first tenant, so it doesn't exist yet.
        // This token is only valid for signup endpoints that create the initial tenant.
        return createToken(claims, "access", 15L * 60 * 1000);
    }

    /**
     * Generate a long-lived refresh token for obtaining new access tokens.
     *
     * <p>Refresh tokens are valid for ~7 days. When an access token expires, the client sends
     * the refresh token to POST /auth/refresh to get a new access token without re-authenticating.
     * Refresh tokens contain the same claims as access tokens and are validated the same way.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param role the user's role
     * @return a signed JWT token with type "refresh"
     */
    public String generateRefreshToken(Long userId, Long tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId);
        claims.put("role", role);
        return createToken(claims, "refresh", jwtProperties.getRefreshExpiration());
    }

    /**
     * Build a JWT token from claims with expiration and signature.
     *
     * <p>Uses HMAC-SHA256 signing. The resulting token is safe for transmission in HTTP headers.
     *
     * @param claims the data to embed in the token (userId, tenantId, role, etc.)
     * @param tokenType the type of token ("access", "refresh", "onboarding")
     * @param expirationTime the token lifetime in milliseconds
     * @return a compact, signed JWT token
     */
    private String createToken(Map<String, Object> claims, String tokenType, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract the user ID from a token's claims.
     *
     * @param token the JWT token
     * @return the userId claim, or null if not present or not a valid number
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object raw = claims.get("userId");
        return parseLongClaim(raw);
    }

    /**
     * Extract the tenant ID from a token's claims.
     *
     * @param token the JWT token
     * @return the tenantId claim, or null if not present (e.g., onboarding tokens)
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException if the token is invalid
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object raw = claims.get("tenantId");
        return parseLongClaim(raw);
    }

    /**
     * Extract the token type from a token's claims.
     *
     * @param token the JWT token
     * @return the tokenType claim ("access", "refresh", or "onboarding"), or null if not present
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException if the token is invalid
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object val = claims.get("tokenType");
        return val != null ? val.toString() : null;
    }

    /**
     * Extract the role from a token's claims.
     *
     * @param token the JWT token
     * @return the role claim (e.g., "ADMIN", "USER", "SYSTEM_ADMIN"), or null if not present
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException if the token is invalid
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object val = claims.get("role");
        return val != null ? val.toString() : null;
    }

    /**
     * Parse all claims from a token for advanced usage.
     *
     * <p>Allows callers to distinguish between different JWT exceptions (e.g., ExpiredJwtException
     * vs. MalformedJwtException) without validateToken hiding them. Used by JwtAuthenticationFilter
     * to specifically detect and handle expired tokens.
     *
     * @param token the JWT token
     * @return the parsed claims
     * @throws ExpiredJwtException if the token has expired
     * @throws JwtException for other parsing errors
     */
    public Claims parseClaims(String token) {
        return getAllClaimsFromToken(token);
    }

    /**
     * Parse a generic object as a Long claim.
     *
     * <p>JWT claims may come as Number or String types depending on the serializer.
     * This utility normalizes them to Long.
     *
     * @param raw the claim value (Number, String, or null)
     * @return the parsed long value, or null if conversion fails
     */
    public Long parseLongClaim(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse numeric JWT claim: {}", raw);
            return null;
        }
    }

    /**
     * Validate a JWT token's signature and expiration.
     *
     * <p><strong>Exceptions</strong>: This method catches JJWT exceptions and returns a boolean.
     * For ExpiredJwtException, it re-throws a custom {@code ExpiredTokenException} so callers
     * (like JwtAuthenticationFilter) can distinguish expired tokens and return a 401 response.
     *
     * @param token the JWT token to validate
     * @return true if valid; false if malformed, unsupported, or empty
     * @throws ExpiredTokenException if the token has expired (custom exception for explicit handling)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // WHY: Re-throw custom exception so JwtAuthenticationFilter can catch it separately
            // and return a 401 with TOKEN_EXPIRED error code (vs. generic 401 for other validation failures).
            log.debug("Expired JWT token detected: {}", ex.getMessage());
            throw new com.bocrm.backend.exception.ExpiredTokenException("Token expired");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Parse and return all claims from a token without error handling.
     *
     * <p>This is a lower-level method used internally. Exceptions are not caught;
     * the caller (parseClaims) is responsible for handling them.
     *
     * @param token the JWT token
     * @return the parsed claims
     * @throws JwtException if parsing fails (expired, invalid signature, malformed, etc.)
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
