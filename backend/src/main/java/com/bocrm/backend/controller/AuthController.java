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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
/**
 * AuthController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication management")
@Slf4j
public class AuthController {
    private final AuthService authService;

    @Value("${app.oidc.logout-return-to:}")
    private String oidcLogoutReturnTo;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/switch-tenant")
    @Operation(summary = "Switch active tenant")
    public ResponseEntity<LoginResponse> switchTenant(@RequestBody SwitchTenantRequest request) {
        return ResponseEntity.ok(authService.switchTenant(request));
    }

    @PostMapping("/external/login")
    @Operation(summary = "Login with external identity token")
    public ResponseEntity<LoginResponse> externalLogin(@Valid @RequestBody ExternalLoginRequest request) {
        return ResponseEntity.ok(authService.externalLogin(request));
    }

    @PostMapping("/onboard")
    @Operation(summary = "Create first tenant for a newly registered user")
    public ResponseEntity<LoginResponse> onboard(@Valid @RequestBody OnboardRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        Long userId;
        try {
            userId = Long.parseLong(principal.toString());
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid authentication context");
        }
        return ResponseEntity.ok(authService.createOnboardingTenant(userId, request.getTenantName()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public-config")
    @Operation(summary = "Public frontend configuration (no auth required)")
    public ResponseEntity<Map<String, String>> publicConfig() {
        Map<String, String> config = new HashMap<>();
        if (oidcLogoutReturnTo != null && !oidcLogoutReturnTo.isBlank()) {
            config.put("oidcLogoutReturnTo", oidcLogoutReturnTo);
        }
        return ResponseEntity.ok(config);
    }

    @Data
    public static class OnboardRequest {
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 80, message = "Organization name must be between 2 and 80 characters")
        private String tenantName;
    }
}
