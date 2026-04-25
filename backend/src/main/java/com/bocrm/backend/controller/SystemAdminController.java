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

import com.bocrm.backend.dto.AssistantTierDTO;
import com.bocrm.backend.dto.AvailableAiModelDTO;
import com.bocrm.backend.dto.EnabledAiModelDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.QuotaStatusDTO;
import com.bocrm.backend.dto.SystemStatsDTO;
import com.bocrm.backend.dto.SystemUserDTO;
import com.bocrm.backend.dto.UserMembershipDTO;
import com.bocrm.backend.entity.AssistantTier;
import com.bocrm.backend.entity.EnabledAiModel;
import com.bocrm.backend.repository.AssistantTierRepository;
import com.bocrm.backend.repository.EnabledAiModelRepository;
import com.bocrm.backend.service.AiModelDiscoveryService;
import com.bocrm.backend.service.SystemAdminService;
import com.bocrm.backend.service.TokenQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * SystemAdminController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/admin/system")
@Tag(name = "System Admin", description = "System-wide administration endpoints")
@Slf4j
public class SystemAdminController {

    private final SystemAdminService systemAdminService;
    private final TokenQuotaService tokenQuotaService;
    private final AssistantTierRepository assistantTierRepository;
    private final EnabledAiModelRepository enabledAiModelRepository;
    private final AiModelDiscoveryService aiModelDiscoveryService;

    public SystemAdminController(SystemAdminService systemAdminService,
                                TokenQuotaService tokenQuotaService,
                                AssistantTierRepository assistantTierRepository,
                                EnabledAiModelRepository enabledAiModelRepository,
                                AiModelDiscoveryService aiModelDiscoveryService) {
        this.systemAdminService = systemAdminService;
        this.tokenQuotaService = tokenQuotaService;
        this.assistantTierRepository = assistantTierRepository;
        this.enabledAiModelRepository = enabledAiModelRepository;
        this.aiModelDiscoveryService = aiModelDiscoveryService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get system-wide aggregate statistics")
    public ResponseEntity<SystemStatsDTO> getStats() {
        return ResponseEntity.ok(systemAdminService.getSystemStats());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all users across all tenants")
    public ResponseEntity<PagedResponse<SystemUserDTO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemAdminService.listUsers(page, size, search));
    }

    @GetMapping("/users/{userId}/memberships")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get all tenant memberships for a user")
    public ResponseEntity<List<UserMembershipDTO>> getUserMemberships(@PathVariable Long userId) {
        return ResponseEntity.ok(systemAdminService.getUserMemberships(userId));
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update user status system-wide")
    public ResponseEntity<SystemUserDTO> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(systemAdminService.updateUserStatus(userId, request.getStatus()));
    }

    @PostMapping("/users/invite")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Invite a new platform user")
    public ResponseEntity<SystemUserDTO> inviteUser(@RequestBody InviteUserRequest request) {
        return ResponseEntity.ok(systemAdminService.inviteUser(request.getEmail(), request.getDisplayName()));
    }

    @GetMapping("/tenants/{tenantId}/subscription")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get tenant subscription and tier info")
    public ResponseEntity<QuotaStatusDTO> getTenantSubscription(@PathVariable Long tenantId) {
        QuotaStatusDTO quotaStatus = tokenQuotaService.getQuotaStatus(tenantId);
        return ResponseEntity.ok(quotaStatus);
    }

    @PutMapping("/tenants/{tenantId}/tier")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Change tenant subscription tier")
    public ResponseEntity<QuotaStatusDTO> changeTenantTier(
            @PathVariable Long tenantId,
            @RequestBody ChangeTierRequest request) {
        tokenQuotaService.changeTier(tenantId, request.getTierName());
        QuotaStatusDTO updatedQuota = tokenQuotaService.getQuotaStatus(tenantId);
        return ResponseEntity.ok(updatedQuota);
    }

    @GetMapping("/ai-tiers")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all AI model tiers for configuration")
    public ResponseEntity<List<AssistantTierDTO>> listAiTiers() {
        List<AssistantTierDTO> tiers = assistantTierRepository.findAll().stream()
                .map(t -> AssistantTierDTO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .displayName(t.getDisplayName())
                        .monthlyTokenLimit(t.getMonthlyTokenLimit())
                        .modelId(t.getModelId())
                        .provider(t.getProvider())
                        .priceMonthly(t.getPriceMonthly())
                        .enabled(t.getEnabled())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(tiers);
    }

    @PutMapping("/ai-tiers/{tierId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update AI tier configuration")
    public ResponseEntity<AssistantTierDTO> updateAiTier(
            @PathVariable Long tierId,
            @RequestBody UpdateAiTierRequest request) {
        AssistantTier tier = assistantTierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));

        if (request.getDisplayName() != null) tier.setDisplayName(request.getDisplayName());
        if (request.getModelId() != null) tier.setModelId(request.getModelId());
        if (request.getProvider() != null) tier.setProvider(request.getProvider());
        if (request.getMonthlyTokenLimit() != null) tier.setMonthlyTokenLimit(request.getMonthlyTokenLimit());
        if (request.getPriceMonthly() != null) tier.setPriceMonthly(request.getPriceMonthly());
        if (request.getEnabled() != null) tier.setEnabled(request.getEnabled());

        AssistantTier updated = assistantTierRepository.save(tier);
        AssistantTierDTO dto = AssistantTierDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .displayName(updated.getDisplayName())
                .monthlyTokenLimit(updated.getMonthlyTokenLimit())
                .modelId(updated.getModelId())
                .provider(updated.getProvider())
                .priceMonthly(updated.getPriceMonthly())
                .enabled(updated.getEnabled())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/ai-tiers/apply-all-tenants")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Apply AI tier configuration to all tenants")
    public ResponseEntity<Void> applyAiTiersToAllTenants(
            @RequestBody ApplyAiTiersRequest request) {
        tokenQuotaService.applyTierToAllTenants(request.getTierName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ai-models")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all AI models with enabled/disabled status")
    public ResponseEntity<List<EnabledAiModelDTO>> listAiModels() {
        List<EnabledAiModelDTO> models = enabledAiModelRepository.findAllByOrderByProviderAscModelIdAsc().stream()
                .map(m -> EnabledAiModelDTO.builder()
                        .id(m.getId())
                        .provider(m.getProvider())
                        .modelId(m.getModelId())
                        .enabled(m.getEnabled())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(models);
    }

    @PostMapping("/ai-models")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Add a new AI model")
    public ResponseEntity<EnabledAiModelDTO> addAiModel(
            @RequestBody CreateAiModelRequest request) {
        EnabledAiModel model = EnabledAiModel.builder()
                .provider(request.getProvider())
                .modelId(request.getModelId())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        EnabledAiModel saved = enabledAiModelRepository.save(model);
        EnabledAiModelDTO dto = EnabledAiModelDTO.builder()
                .id(saved.getId())
                .provider(saved.getProvider())
                .modelId(saved.getModelId())
                .enabled(saved.getEnabled())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/ai-models/{modelId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update AI model (enable/disable or change provider/modelId)")
    public ResponseEntity<EnabledAiModelDTO> updateAiModel(
            @PathVariable Long modelId,
            @RequestBody UpdateAiModelRequest request) {
        EnabledAiModel model = enabledAiModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        if (request.getProvider() != null) model.setProvider(request.getProvider());
        if (request.getModelId() != null) model.setModelId(request.getModelId());
        if (request.getEnabled() != null) model.setEnabled(request.getEnabled());

        EnabledAiModel updated = enabledAiModelRepository.save(model);
        EnabledAiModelDTO dto = EnabledAiModelDTO.builder()
                .id(updated.getId())
                .provider(updated.getProvider())
                .modelId(updated.getModelId())
                .enabled(updated.getEnabled())
                .build();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/ai-models/{modelId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Remove an AI model")
    public ResponseEntity<Void> deleteAiModel(@PathVariable Long modelId) {
        EnabledAiModel model = enabledAiModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        enabledAiModelRepository.delete(model);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ai-models/providers/{provider}/available")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List available model IDs from a provider's live API with quota information")
    public ResponseEntity<List<AvailableAiModelDTO>> listAvailableModels(@PathVariable String provider) {
        return ResponseEntity.ok(aiModelDiscoveryService.listAvailableModels(provider));
    }

    static class UpdateUserStatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    static class InviteUserRequest {
        private String email;
        private String displayName;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    static class ChangeTierRequest {
        private String tierName;

        public String getTierName() {
            return tierName;
        }

        public void setTierName(String tierName) {
            this.tierName = tierName;
        }
    }

    static class UpdateAiTierRequest {
        private String displayName;
        private String modelId;
        private String provider;
        private Long monthlyTokenLimit;
        private java.math.BigDecimal priceMonthly;
        private Boolean enabled;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public Long getMonthlyTokenLimit() { return monthlyTokenLimit; }
        public void setMonthlyTokenLimit(Long monthlyTokenLimit) { this.monthlyTokenLimit = monthlyTokenLimit; }

        public java.math.BigDecimal getPriceMonthly() { return priceMonthly; }
        public void setPriceMonthly(java.math.BigDecimal priceMonthly) { this.priceMonthly = priceMonthly; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    static class CreateAiModelRequest {
        private String provider;
        private String modelId;
        private Boolean enabled;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    static class UpdateAiModelRequest {
        private String provider;
        private String modelId;
        private Boolean enabled;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    static class ApplyAiTiersRequest {
        private String tierName;

        public String getTierName() { return tierName; }
        public void setTierName(String tierName) { this.tierName = tierName; }
    }
}
