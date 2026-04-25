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
import com.bocrm.backend.dto.McpApiKeyDTO;
import com.bocrm.backend.dto.ModelStatusDTO;
import com.bocrm.backend.dto.ProviderStatusDTO;
import com.bocrm.backend.dto.QuotaStatusDTO;
import com.bocrm.backend.dto.TenantSubscriptionDTO;
import com.bocrm.backend.dto.UpdateTierRequest;
import com.bocrm.backend.entity.AssistantTier;
import com.bocrm.backend.entity.McpApiKey;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.entity.TenantSubscription;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.AssistantTierRepository;
import com.bocrm.backend.repository.EnabledAiModelRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.service.ChatModelRegistry;
import com.bocrm.backend.service.DashboardInsightService;
import com.bocrm.backend.service.McpApiKeyService;
import com.bocrm.backend.service.TenantAdminService;
import com.bocrm.backend.service.TokenQuotaService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * AssistantSubscriptionController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/assistant")
@Tag(name = "Assistant", description = "AI assistant subscription and quota management")
@Slf4j
public class AssistantSubscriptionController {

    private final TokenQuotaService tokenQuotaService;
    private final AssistantTierRepository assistantTierRepository;
    private final EnabledAiModelRepository enabledAiModelRepository;
    private final ChatModelRegistry chatModelRegistry;
    private final DashboardInsightService dashboardInsightService;
    private final TenantAdminService tenantAdminService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final McpApiKeyService mcpApiKeyService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String supportEmail;
    private final String fromAddress;
    private final String publicBaseUrl;

    public AssistantSubscriptionController(TokenQuotaService tokenQuotaService,
                                            AssistantTierRepository assistantTierRepository,
                                            EnabledAiModelRepository enabledAiModelRepository,
                                            ChatModelRegistry chatModelRegistry,
                                            DashboardInsightService dashboardInsightService,
                                            TenantAdminService tenantAdminService,
                                            UserRepository userRepository,
                                            TenantRepository tenantRepository,
                                            McpApiKeyService mcpApiKeyService,
                                            ObjectProvider<JavaMailSender> mailSenderProvider,
                                            @Value("${app.mail.support-address:support@bocrm.com}") String supportEmail,
                                            @Value("${app.mail.default-from:noreply@bocrm.com}") String fromAddress,
                                            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.tokenQuotaService = tokenQuotaService;
        this.assistantTierRepository = assistantTierRepository;
        this.enabledAiModelRepository = enabledAiModelRepository;
        this.chatModelRegistry = chatModelRegistry;
        this.dashboardInsightService = dashboardInsightService;
        this.tenantAdminService = tenantAdminService;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.mcpApiKeyService = mcpApiKeyService;
        this.mailSenderProvider = mailSenderProvider;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get current subscription and quota status")
    public ResponseEntity<TenantSubscriptionDTO> getSubscription() {
        Long tenantId = requireTenantContext();
        QuotaStatusDTO quota = tokenQuotaService.getQuotaStatus(tenantId);
        TokenQuotaService.TierInfo tierInfo = tokenQuotaService.resolveTierInfoForTenant(tenantId);
        TenantSubscriptionDTO dto = TenantSubscriptionDTO.builder()
                .tierName(quota.getTierName())
                .tierDisplayName(quota.getTierName())
                .tokensUsedThisPeriod(quota.getTokensUsed())
                .monthlyTokenLimit(quota.getTokenLimit())
                .periodEndDate(quota.getPeriodEnd())
                .modelId(tierInfo.modelId())
                .provider(tierInfo.provider())
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/quota")
    @Operation(summary = "Get token quota status for the current tenant")
    public ResponseEntity<QuotaStatusDTO> getQuota() {
        Long tenantId = requireTenantContext();
        return ResponseEntity.ok(tokenQuotaService.getQuotaStatus(tenantId));
    }

    @GetMapping("/tiers")
    @Operation(summary = "List all enabled subscription tiers with their available models")
    public ResponseEntity<Map<String, Object>> getTiers() {
        List<AssistantTierDTO> tiers = assistantTierRepository.findAll().stream()
                .filter(t -> t.getEnabled() == null || t.getEnabled()) // null or true = enabled
                .map(t -> AssistantTierDTO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .displayName(t.getDisplayName())
                        .monthlyTokenLimit(t.getMonthlyTokenLimit())
                        .modelId(t.getModelId())
                        .provider(t.getProvider() != null ? t.getProvider() : "anthropic")
                        .priceMonthly(t.getPriceMonthly())
                        .enabled(t.getEnabled() != null ? t.getEnabled() : true)
                        .build())
                .collect(Collectors.toList());

        List<com.bocrm.backend.dto.EnabledAiModelDTO> models = enabledAiModelRepository.findAllByOrderByProviderAscModelIdAsc().stream()
                .filter(m -> m.getEnabled() != null && m.getEnabled())
                .map(m -> com.bocrm.backend.dto.EnabledAiModelDTO.builder()
                        .id(m.getId())
                        .provider(m.getProvider())
                        .modelId(m.getModelId())
                        .enabled(m.getEnabled())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("tiers", tiers, "models", models));
    }

    @GetMapping("/models/status")
    @Operation(summary = "Get AI provider and model status (availability and enabled/disabled state)")
    public ResponseEntity<Map<String, Object>> getModelsStatus() {
        // WHY: This endpoint reports which providers have API keys configured (connectorAvailable)
        // and which models are enabled/disabled. It helps the user understand fallback options.
        // No probe/ping is made — status is config-derived only (no quota consumption).
        List<ProviderStatusDTO> providers = List.of("anthropic", "openai", "google", "ollama").stream()
                .map(providerName -> {
                    boolean available = chatModelRegistry.isProviderAvailable(providerName);
                    // Include all models for this provider (enabled or not), so admins see the full picture
                    List<ModelStatusDTO> models = enabledAiModelRepository.findAll().stream()
                            .filter(m -> providerName.equals(m.getProvider()))
                            .map(m -> ModelStatusDTO.builder()
                                    .modelId(m.getModelId())
                                    .enabled(m.getEnabled() != null && m.getEnabled())
                                    .build())
                            .sorted((a, b) -> a.getModelId().compareTo(b.getModelId()))
                            .collect(Collectors.toList());
                    return ProviderStatusDTO.builder()
                            .name(providerName)
                            .connectorAvailable(available)
                            .models(models)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("providers", providers));
    }

    @PutMapping("/tiers/{tierId}")
    @Operation(summary = "Update a tier's provider and model (admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<AssistantTierDTO> updateTier(@PathVariable Long tierId,
                                                        @RequestBody UpdateTierRequest req) {
        requireTenantContext();
        AssistantTier tier = assistantTierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found: " + tierId));
        if (req.getProvider() != null && !req.getProvider().isBlank()) {
            tier.setProvider(req.getProvider());
        }
        if (req.getModelId() != null && !req.getModelId().isBlank()) {
            tier.setModelId(req.getModelId());
        }
        AssistantTier saved = assistantTierRepository.save(tier);
        AssistantTierDTO dto = AssistantTierDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .displayName(saved.getDisplayName())
                .monthlyTokenLimit(saved.getMonthlyTokenLimit())
                .modelId(saved.getModelId())
                .provider(saved.getProvider())
                .priceMonthly(saved.getPriceMonthly())
                .enabled(saved.getEnabled())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/subscription/upgrade")
    @Operation(summary = "Upgrade the tenant's subscription tier (system admin only)")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<TenantSubscriptionDTO> upgradeTier(@RequestBody Map<String, String> body) {
        Long tenantId = requireTenantContext();
        String tierName = body.get("tierName");
        if (tierName == null || tierName.isBlank()) {
            throw new IllegalArgumentException("tierName is required");
        }
        TenantSubscription sub = tokenQuotaService.changeTier(tenantId, tierName);
        AssistantTier tier = sub.getTier();
        TenantSubscriptionDTO dto = TenantSubscriptionDTO.builder()
                .tierId(tier.getId())
                .tierName(tier.getName())
                .tierDisplayName(tier.getDisplayName())
                .tokensUsedThisPeriod(sub.getTokensUsedThisPeriod())
                .monthlyTokenLimit(tier.getMonthlyTokenLimit())
                .periodStartDate(sub.getPeriodStartDate())
                .periodEndDate(sub.getPeriodEndDate())
                .modelId(tier.getModelId())
                .provider(tier.getProvider() != null ? tier.getProvider() : "anthropic")
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/insight")
    @Operation(summary = "Get AI-generated dashboard insight (cached 1 hour per tenant)")
    public ResponseEntity<Map<String, Object>> getDashboardInsight() {
        Long tenantId = requireTenantContext();
        String tenantName = tenantAdminService.getCurrentTenant().getName();
        DashboardInsightService.DashboardInsight insight = dashboardInsightService.getInsight(tenantId, tenantName);
        return ResponseEntity.ok(Map.of(
                "text", insight.text(),
                "generatedAt", insight.generatedAt().toString()
        ));
    }

    @PostMapping("/insight/refresh")
    @Operation(summary = "Force-refresh the dashboard insight")
    public ResponseEntity<Map<String, Object>> refreshDashboardInsight() {
        Long tenantId = requireTenantContext();
        String tenantName = tenantAdminService.getCurrentTenant().getName();
        DashboardInsightService.DashboardInsight insight = dashboardInsightService.refreshInsight(tenantId, tenantName);
        return ResponseEntity.ok(Map.of(
                "text", insight.text(),
                "generatedAt", insight.generatedAt().toString()
        ));
    }

    @PostMapping("/tier-change-request")
    @Operation(summary = "Request a tier change (sends email to support)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> requestTierChange(@RequestBody Map<String, String> body) {
        Long tenantId = requireTenantContext();
        Long userId = TenantContext.getUserId();
        if (userId == null) throw new ForbiddenException("User context not set");

        String tierName = body.get("tierName");
        if (tierName == null || tierName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "tierName is required"));
        }

        // Load tenant, user, current tier, and requested tier
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        QuotaStatusDTO currentQuota = tokenQuotaService.getQuotaStatus(tenantId);
        AssistantTier requestedTier = assistantTierRepository.findByName(tierName)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found: " + tierName));

        // Send email if mail server is configured
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            sendTierChangeRequestEmail(mailSender, tenant, user, currentQuota.getTierName(), requestedTier);
            log.info("Tier change request email sent: tenant={}, user={}, requested={}", tenantId, user.getEmail(), tierName);
        } else {
            log.warn("Mail server not configured; tier change request not emailed: tenant={}, user={}", tenantId, user.getEmail());
        }

        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    private void sendTierChangeRequestEmail(JavaMailSender mailSender, Tenant tenant, User user,
                                             String currentTierName, AssistantTier requestedTier) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddress, "BOCRM");
            helper.setTo(supportEmail);
            helper.setSubject("Tier Change Request - " + tenant.getName());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            String htmlBody = String.format(
                    "<h2>Tier Change Request</h2>" +
                            "<table style=\"border-collapse: collapse; width: 100%;\">" +
                            "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><b>Tenant:</b></td><td style=\"padding: 8px; border: 1px solid #ddd;\">%s (ID: %d)</td></tr>" +
                            "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><b>Requested by:</b></td><td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>" +
                            "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><b>Current Tier:</b></td><td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>" +
                            "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><b>Requested Tier:</b></td><td style=\"padding: 8px; border: 1px solid #ddd;\">%s (%s)</td></tr>" +
                            "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><b>Requested at:</b></td><td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>" +
                            "</table>",
                    tenant.getName(), tenant.getId(),
                    user.getEmail(),
                    currentTierName,
                    requestedTier.getDisplayName(), requestedTier.getName(),
                    timestamp
            );
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send tier change request email", e);
            throw new RuntimeException("Failed to send tier change request email", e);
        }
    }

    @PostMapping("/mcp-keys/generate")
    @Operation(summary = "Generate a new MCP API key")
    public ResponseEntity<String> generateMcpApiKey(@RequestBody Map<String, String> request) {
        requireTenantContext();
        String name = request.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Key name is required");
        }
        String rawKey = mcpApiKeyService.generateApiKey(name);
        String sseUrl = (publicBaseUrl != null && !publicBaseUrl.isBlank())
                ? publicBaseUrl + "/api/sse"
                : "<YOUR_BOCRM_URL>/api/sse";
        String response = String.format(
                "MCP API key generated successfully!%n%n" +
                "**Key Name:** %s%n" +
                "**Raw Key:** %s%n%n" +
                "⚠️ SAVE THIS KEY IMMEDIATELY — it will never be shown again!%n%n" +
                "## Option 1: API Key Authentication (Recommended for headless/power-users)%n%n" +
                "Add this entry to your Claude Desktop config at `~/.config/Claude/claude_desktop_config.json` (Linux) or `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):%n" +
                "```json%n" +
                "{%n" +
                "  \"mcpServers\": {%n" +
                "    \"bocrm\": {%n" +
                "      \"command\": \"npx\",%n" +
                "      \"args\": [%n" +
                "        \"-y\",%n" +
                "        \"mcp-remote\",%n" +
                "        \"%s\",%n" +
                "        \"--transport\",%n" +
                "        \"sse-only\",%n" +
                "        \"--header\",%n" +
                "        \"Authorization:Bearer %s\"%n" +
                "      ]%n" +
                "    }%n" +
                "  }%n" +
                "}%n" +
                "```%n%n" +
                "## Option 2: OAuth Authentication (Recommended for browser-based flow)%n%n" +
                "Instead of an API key, use your Auth0 credentials for a seamless browser-based login:%n" +
                "```json%n" +
                "{%n" +
                "  \"mcpServers\": {%n" +
                "    \"bocrm\": {%n" +
                "      \"command\": \"npx\",%n" +
                "      \"args\": [%n" +
                "        \"-y\",%n" +
                "        \"mcp-remote\",%n" +
                "        \"%s\",%n" +
                "        \"--transport\",%n" +
                "        \"sse-only\"%n" +
                "      ]%n" +
                "    }%n" +
                "  }%n" +
                "}%n" +
                "```%n%n" +
                "When you first connect, mcp-remote will detect the OAuth requirement and open your browser to log in with Auth0.%n" +
                "After successful login, your JWT token will be used for all subsequent tool requests.%n%n" +
                "**Important notes:**%n" +
                "- **Node 20 required** — Node 22+ has an undici bug that breaks SSE header handling (`Key Symbol(headers list)...` error). Use `nvm install 20 && nvm use 20` before starting Claude Desktop.%n" +
                "- Use `--transport sse-only` — BOCRM uses Spring AI's SSE transport (not HTTP streaming).%n" +
                "- For API key option: No space after `Authorization:` — some versions of mcp-remote fail to parse headers with a space.%n" +
                "- Restart Claude Desktop after editing the config.%n" +
                "- If connection hangs or you get lockfile errors, clean up with: `pkill -9 -f mcp-remote && rm -rf ~/.npm/_npx/`%n%n" +
                "The API key is long-lived and never expires. Revoke it if compromised using \"Revoke MCP API key {id}\".",
                name, rawKey, sseUrl, rawKey, sseUrl
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mcp-keys")
    @Operation(summary = "List all MCP API keys for the current tenant")
    public ResponseEntity<List<McpApiKeyDTO>> listMcpApiKeys() {
        requireTenantContext();
        List<McpApiKey> keys = mcpApiKeyService.listApiKeys();
        List<McpApiKeyDTO> dtos = keys.stream()
                .map(key -> McpApiKeyDTO.builder()
                        .id(key.getId())
                        .name(key.getName())
                        .enabled(key.isEnabled())
                        .createdAt(key.getCreatedAt())
                        .lastUsedAt(key.getLastUsedAt())
                        .keyPrefix(key.getKeyPrefix())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/mcp-keys/{keyId}/revoke")
    @Operation(summary = "Revoke an MCP API key")
    public ResponseEntity<Void> revokeMcpApiKey(@PathVariable Long keyId) {
        requireTenantContext();
        mcpApiKeyService.revokeApiKey(keyId);
        return ResponseEntity.noContent().build();
    }

    private Long requireTenantContext() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        return tenantId;
    }
}
