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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.*;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.shared.TenantSchema;
import com.bocrm.backend.util.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * AuthService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final JwtProvider jwtProvider;
    private final AuditLogService auditLogService;
    private final ExternalIdentityService externalIdentityService;
    private final TenantSchemaProvisioningService tenantSchemaProvisioningService;
    private final TenantFlywayMigrationService tenantFlywayMigrationService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, TenantMembershipRepository membershipRepository,
                      TenantRepository tenantRepository, TenantSettingsRepository tenantSettingsRepository,
                      JwtProvider jwtProvider, AuditLogService auditLogService,
                      ExternalIdentityService externalIdentityService,
                      TenantSchemaProvisioningService tenantSchemaProvisioningService,
                      TenantFlywayMigrationService tenantFlywayMigrationService,
                      org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.jwtProvider = jwtProvider;
        this.auditLogService = auditLogService;
        this.externalIdentityService = externalIdentityService;
        this.tenantSchemaProvisioningService = tenantSchemaProvisioningService;
        this.tenantFlywayMigrationService = tenantFlywayMigrationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndPasswordHashIsNotNull(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!StringUtils.hasText(user.getPasswordHash()) ||
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }

        List<TenantMembership> activeMemberships = membershipRepository.findByUserId(user.getId()).stream()
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .toList();

        if (activeMemberships.isEmpty()) {
            throw new UnauthorizedException("No active tenant membership");
        }

        // If user has multiple tenants, return tenant selection response
        if (activeMemberships.size() > 1) {
            String selectionToken = jwtProvider.generateOnboardingToken(user.getId());
            List<TenantSummaryDTO> tenantList = buildTenantSummaryListFromMemberships(activeMemberships);
            return LoginResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .displayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getEmail())
                    .accessToken(selectionToken)
                    .refreshToken(null)
                    .requiresTenantSelection(true)
                    .availableTenants(tenantList)
                    .build();
        }

        // Single tenant - normal login flow
        TenantMembership membership = activeMemberships.get(0);
        Tenant tenant = tenantRepository.findById(membership.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        TenantContext.setTenantId(tenant.getId());
        TenantContext.setUserId(user.getId());
        auditLogService.logAction(user.getId(), "LOGIN", null, null, null);

        return buildLoginResponse(user, tenant, membership.getRole());
    }

    @Transactional
    public LoginResponse externalLogin(ExternalLoginRequest request) {
        String step = "start";
        try {
            step = "token_validation";
            ExternalIdentityService.ExternalUserIdentity identity = externalIdentityService.validate(request.getIdToken());

            step = "user_resolution";
            User user = resolveOrCreateExternalUser(identity);

            step = "organization_mapping";
            if (identity.organizations().isEmpty()) {
                // No org claim — check if user already has memberships
                List<TenantMembership> activeMemberships = membershipRepository.findByUserId(user.getId()).stream()
                        .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                        .toList();

                if (!activeMemberships.isEmpty()) {
                    // Apply Auth0 app-level roles to all memberships before returning
                    if (!identity.roles().isEmpty()) {
                        String auth0AppRole = identity.roles().stream()
                                .map(this::mapExternalRole)
                                .max(Comparator.comparingInt(this::roleRank))
                                .orElse("user");
                        for (TenantMembership m : activeMemberships) {
                            String best = higherRole(m.getRole(), auth0AppRole);
                            if (!best.equals(normalizeRole(m.getRole()))) {
                                log.info("Elevating membership {} role from {} to {} via Auth0 app role",
                                        m.getId(), m.getRole(), best);
                                m.setRole(best);
                                membershipRepository.save(m);
                            }
                        }
                    }

                    // If user has multiple tenants, return tenant selection response
                    if (activeMemberships.size() > 1) {
                        String selectionToken = jwtProvider.generateOnboardingToken(user.getId());
                        List<TenantSummaryDTO> tenantList = buildTenantSummaryListFromMemberships(activeMemberships);
                        return LoginResponse.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .displayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getEmail())
                                .accessToken(selectionToken)
                                .refreshToken(null)
                                .requiresTenantSelection(true)
                                .availableTenants(tenantList)
                                .build();
                    }

                    // Single tenant - return normal login
                    TenantMembership existing = activeMemberships.get(0);
                    Tenant existingTenant = tenantRepository.findById(existing.getTenantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
                    step = "session_creation";
                    TenantContext.setTenantId(existingTenant.getId());
                    TenantContext.setUserId(user.getId());
                    auditLogService.logAction(user.getId(), "EXTERNAL_LOGIN", null, null, null);
                    // Consider Auth0 app-level roles if available; otherwise use stored membership role
                    String effectiveRole = existing.getRole();
                    if (!identity.roles().isEmpty()) {
                        String auth0AppRole = identity.roles().stream()
                                .map(this::mapExternalRole)
                                .max(Comparator.comparingInt(this::roleRank))
                                .orElse("user");
                        effectiveRole = higherRole(existing.getRole(), auth0AppRole);
                    }
                    return buildLoginResponse(user, existingTenant, effectiveRole);
                }

                // New user with no org — return onboarding token
                step = "onboarding";
                String onboardingToken = jwtProvider.generateOnboardingToken(user.getId());
                return LoginResponse.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .displayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getEmail())
                        .accessToken(onboardingToken)
                        .refreshToken(null)
                        .expiresIn(900L)
                        .requiresOnboarding(true)
                        .build();
            }

            ExternalIdentityService.OrganizationIdentity organization = identity.organizations().get(0);
            TenantMembership activeMembership = ensureMembershipForOrganization(user, organization, identity.roles());
            Tenant activeTenant = tenantRepository.findById(activeMembership.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

            step = "session_creation";
            TenantContext.setTenantId(activeTenant.getId());
            TenantContext.setUserId(user.getId());
            auditLogService.logAction(user.getId(), "EXTERNAL_LOGIN", null, null, null);

            return buildLoginResponse(user, activeTenant, activeMembership.getRole());
        } catch (UnauthorizedException | ValidationException ex) {
            log.warn("External login failed at step {}: {}", step, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("External login failed unexpectedly at step {}", step, ex);
            throw new ValidationException("External login failed while processing step: " + step);
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            if (!jwtProvider.validateToken(request.getRefreshToken())) {
                throw new UnauthorizedException("Invalid refresh token");
            }
        } catch (ExpiredTokenException e) {
            throw new UnauthorizedException("Refresh token expired, please log in again");
        }

        String tokenType = jwtProvider.getTokenTypeFromToken(request.getRefreshToken());
        if (!"refresh".equals(tokenType)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());
        Long tenantId = jwtProvider.getTenantIdFromToken(request.getRefreshToken());
        String role = jwtProvider.getRoleFromToken(request.getRefreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        Tenant activeTenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String normalizedRole = normalizeRole(role);
        String newAccessToken = jwtProvider.generateAccessToken(userId, tenantId, normalizedRole);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, tenantId, normalizedRole);

        return buildLoginResponse(user, activeTenant, normalizedRole, newAccessToken, newRefreshToken);
    }

    @Transactional
    public LoginResponse switchTenant(SwitchTenantRequest request) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new ForbiddenException("Not authenticated");
        }

        // Verify membership
        TenantMembership membership = membershipRepository.findByTenantIdAndUserId(request.getTenantId(), userId)
                .orElseThrow(() -> new ForbiddenException("User is not a member of this tenant"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Tenant activeTenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Generate new tokens for the target tenant (use role from the membership, not SecurityContext)
        String role = normalizeRole(membership.getRole());
        String accessToken = jwtProvider.generateAccessToken(userId, request.getTenantId(), role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, request.getTenantId(), role);

        return buildLoginResponse(user, activeTenant, role, accessToken, refreshToken);
    }

    private LoginResponse buildLoginResponse(User user, Tenant activeTenant, String role) {
        String normalizedRole = normalizeRole(role);
        String accessToken = jwtProvider.generateAccessToken(user.getId(), activeTenant.getId(), normalizedRole);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), activeTenant.getId(), normalizedRole);
        return buildLoginResponse(user, activeTenant, normalizedRole, accessToken, refreshToken);
    }

    private LoginResponse buildLoginResponse(
            User user,
            Tenant activeTenant,
            String role,
            String accessToken,
            String refreshToken
    ) {
        List<TenantSummaryDTO> availableTenants = buildTenantSummaryList(user.getId());

        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .currentTenantId(activeTenant.getId())
                .currentTenantName(activeTenant.getName())
                .availableTenants(availableTenants)
                .preferences(user.getPreferences())
                .build();
    }

    private List<TenantSummaryDTO> buildTenantSummaryListFromMemberships(List<TenantMembership> memberships) {
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, Tenant> tenantMap = tenantRepository.findAllById(
                memberships.stream().map(TenantMembership::getTenantId).toList()
        ).stream().collect(Collectors.toMap(Tenant::getId, t -> t));

        return memberships.stream()
                .map(m -> TenantSummaryDTO.builder()
                        .id(m.getTenantId())
                        .name(tenantMap.get(m.getTenantId()).getName())
                        .role(normalizeRole(m.getRole()))
                        .build())
                .toList();
    }

    private List<TenantSummaryDTO> buildTenantSummaryList(Long userId) {
        List<TenantMembership> allMemberships = membershipRepository.findByUserId(userId)
                .stream()
                .filter(m -> "active".equalsIgnoreCase(m.getStatus()))
                .toList();

        if (allMemberships.isEmpty()) {
            return List.of();
        }

        Map<Long, Tenant> tenantMap = tenantRepository.findAllById(
                allMemberships.stream().map(TenantMembership::getTenantId).toList()
        ).stream().collect(Collectors.toMap(Tenant::getId, t -> t));

        return allMemberships.stream()
                .map(m -> TenantSummaryDTO.builder()
                        .id(m.getTenantId())
                        .name(tenantMap.get(m.getTenantId()).getName())
                        .role(normalizeRole(m.getRole()))
                        .build())
                .toList();
    }

    @Transactional
    public LoginResponse createOnboardingTenant(Long userId, String tenantName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean alreadyMember = membershipRepository.findByUserId(userId).stream()
                .anyMatch(m -> "active".equalsIgnoreCase(m.getStatus()));
        if (alreadyMember) {
            throw new ValidationException("User already belongs to a tenant");
        }

        String uniqueName = ensureUniqueTenantName(tenantName.trim());
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(uniqueName)
                .status("active")
                .build());

        tenantSettingsRepository.save(TenantSettings.builder()
                .tenantId(tenant.getId())
                .settingsJsonb(TenantAdminService.DEFAULT_SETTINGS_JSON)
                .build());
        tenantFlywayMigrationService.migrateSchema(tenant.getId());
        tenantSchemaProvisioningService.seedDefaultDocumentTemplates(tenant.getId(), TenantSchema.fromTenantId(tenant.getId()));
        tenantSchemaProvisioningService.seedDefaultFieldOptions(tenant.getId(), TenantSchema.fromTenantId(tenant.getId()));

        TenantMembership membership = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(userId)
                .role("admin")
                .status("active")
                .build());

        TenantContext.setTenantId(tenant.getId());
        TenantContext.setUserId(userId);
        auditLogService.logAction(userId, "ONBOARDING_TENANT_CREATED", null, null, null);

        return buildLoginResponse(user, tenant, membership.getRole());
    }

    private User resolveOrCreateExternalUser(ExternalIdentityService.ExternalUserIdentity identity) {
        String provider = "oidc";

        // Only look up by provider + subject, never by email alone
        // This prevents collisions when the same email is used across different Auth0 orgs/apps
        User user = userRepository.findByOauthProviderAndOauthId(provider, identity.subject()).orElse(null);

        if (user == null) {
            // Create new external user (never auto-link to existing local accounts)
            // Note: Email is NOT unique anymore; it's OK for multiple OAuth users to share an email
            // from different providers/orgs. The constraint is on (provider, oauthId).
            user = User.builder()
                    .email(identity.email())
                    .displayName(identity.displayName())
                    .passwordHash(null)
                    .oauthProvider(provider)
                    .oauthId(identity.subject())
                    .status("active")
                    .build();
            return userRepository.save(user);
        }

        // Update existing external user
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("active");
        }
        if (StringUtils.hasText(identity.displayName()) && !identity.displayName().equals(user.getDisplayName())) {
            user.setDisplayName(identity.displayName());
        }
        // Update email if it changed (supports email address changes in Auth0)
        if (!identity.email().equals(user.getEmail())) {
            user.setEmail(identity.email());
        }

        return userRepository.save(user);
    }

    private TenantMembership ensureMembershipForOrganization(User user, ExternalIdentityService.OrganizationIdentity org, List<String> tokenRoles) {
        Tenant tenant = tenantRepository.findByExternalOrgId(org.id())
                .orElseGet(() -> createTenantFromOrganization(org));
        syncTenantNameFromOrganization(tenant, org);

        // Determine the highest role: Auth0 org role vs Auth0 app-level roles vs stored membership role
        String auth0OrgRole = mapExternalRole(org.role());

        // Only consider app-level roles if they exist; otherwise fall back to org role
        String auth0Best = auth0OrgRole;
        if (!tokenRoles.isEmpty()) {
            String auth0AppRole = tokenRoles.stream()
                    .map(this::mapExternalRole)
                    .max(Comparator.comparingInt(this::roleRank))
                    .orElse("user");
            auth0Best = higherRole(auth0OrgRole, auth0AppRole);
        }

        TenantMembership membership = membershipRepository.findByTenantIdAndUserId(tenant.getId(), user.getId())
                .orElseGet(() -> TenantMembership.builder()
                        .tenantId(tenant.getId())
                        .userId(user.getId())
                        .role("user")
                        .status("active")
                        .build());

        String bestRole = higherRole(membership.getRole(), auth0Best);

        membership.setStatus("active");
        membership.setRole(bestRole);

        return membershipRepository.save(membership);
    }

    private Tenant createTenantFromOrganization(ExternalIdentityService.OrganizationIdentity org) {
        String proposedName = StringUtils.hasText(org.name()) ? org.name() : ("Organization " + org.id());
        String tenantName = ensureUniqueTenantName(proposedName);

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(tenantName)
                .externalOrgId(org.id())
                .externalOrgName(org.name())
                .status("active")
                .build());

        tenantSettingsRepository.save(TenantSettings.builder()
                .tenantId(tenant.getId())
                .settingsJsonb(TenantAdminService.DEFAULT_SETTINGS_JSON)
                .build());
        tenantFlywayMigrationService.migrateSchema(tenant.getId());
        tenantSchemaProvisioningService.seedDefaultDocumentTemplates(tenant.getId(), TenantSchema.fromTenantId(tenant.getId()));
        tenantSchemaProvisioningService.seedDefaultFieldOptions(tenant.getId(), TenantSchema.fromTenantId(tenant.getId()));

        return tenant;
    }

    private void syncTenantNameFromOrganization(Tenant tenant, ExternalIdentityService.OrganizationIdentity org) {
        if (!StringUtils.hasText(org.name())) {
            return;
        }

        String currentName = tenant.getName();
        boolean looksAutoGenerated = currentName != null && (
                currentName.equals(tenant.getExternalOrgId()) ||
                currentName.equals("Organization " + tenant.getExternalOrgId()) ||
                currentName.matches("^Organization\\s+.+\\(\\d+\\)$")
        );

        if (looksAutoGenerated || !org.name().equals(tenant.getExternalOrgName())) {
            String desiredName = ensureUniqueTenantNameForExistingTenant(tenant.getId(), org.name());
            tenant.setName(desiredName);
            tenant.setExternalOrgName(org.name());
            tenantRepository.save(tenant);
        }
    }

    private String ensureUniqueTenantName(String baseName) {
        String name = baseName;
        int suffix = 2;
        while (tenantRepository.findByName(name).isPresent()) {
            name = baseName + " (" + suffix + ")";
            suffix++;
        }
        return name;
    }

    private String ensureUniqueTenantNameForExistingTenant(Long tenantId, String baseName) {
        String name = baseName;
        int suffix = 2;
        while (true) {
            Tenant existing = tenantRepository.findByName(name).orElse(null);
            if (existing == null || existing.getId().equals(tenantId)) {
                return name;
            }
            name = baseName + " (" + suffix + ")";
            suffix++;
        }
    }

    private String mapExternalRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        String normalized = role.trim().toLowerCase();
        if (normalized.contains("system_admin") || normalized.equals("system-admin") || normalized.equals("systemadmin")) {
            return "system_admin";
        }
        if (normalized.contains("admin") || normalized.contains("owner")) {
            return "admin";
        }
        return "user";
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        return role.trim().toLowerCase();
    }

    @Transactional
    public void logout() {
        Long userId = TenantContext.getUserId();
        if (userId != null) {
            auditLogService.logAction(userId, "LOGOUT", null, null, null);
        }
    }

    private String resolveRoleFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return "user";
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()).toLowerCase())
                .findFirst()
                .orElse("user");
    }

    private int roleRank(String role) {
        if (role == null) return 0;
        return switch (role.trim().toLowerCase()) {
            case "system_admin" -> 4;
            case "admin"        -> 3;
            case "manager"      -> 2;
            default             -> 1; // "user" or anything else
        };
    }

    private String higherRole(String a, String b) {
        return roleRank(a) >= roleRank(b) ? normalizeRole(a) : normalizeRole(b);
    }
}
