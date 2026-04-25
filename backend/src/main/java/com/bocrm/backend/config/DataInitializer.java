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

import com.bocrm.backend.entity.*;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.service.TenantFlywayMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
/**
 * DataInitializer.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantFlywayMigrationService tenantFlywayMigrationService;
    private final AssistantTierRepository assistantTierRepository;
    private final EnabledAiModelRepository enabledAiModelRepository;

    public DataInitializer(TenantRepository tenantRepository,
                           TenantSettingsRepository tenantSettingsRepository,
                           TenantFlywayMigrationService tenantFlywayMigrationService,
                           AssistantTierRepository assistantTierRepository,
                           EnabledAiModelRepository enabledAiModelRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.tenantFlywayMigrationService = tenantFlywayMigrationService;
        this.assistantTierRepository = assistantTierRepository;
        this.enabledAiModelRepository = enabledAiModelRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (tenantRepository.count() == 0) {
            log.info("Initializing sample data for BOCRM...");
            Tenant tenant = Tenant.builder().name("Default Tenant").status("active").build();
            Tenant savedTenant = tenantRepository.save(tenant);
            log.info("Created tenant: {}", savedTenant.getId());

            TenantSettings settings = TenantSettings.builder().tenantId(savedTenant.getId()).settingsJsonb("{}").build();
            tenantSettingsRepository.save(settings);
            log.info("Created tenant settings");
        }

        seedAssistantTiers();
        seedEnabledAiModels();

        tenantFlywayMigrationService.migrateAllSchemas();
        log.info("Tenant schema migration completed");
    }

    private void seedAssistantTiers() {
        if (assistantTierRepository.count() > 0) return;
        log.info("Seeding assistant tiers...");
        assistantTierRepository.save(AssistantTier.builder()
                .name("free").displayName("Free")
                .monthlyTokenLimit(50000L).modelId("claude-haiku-4-5-20251001")
                .provider("anthropic").priceMonthly(BigDecimal.ZERO).build());
        assistantTierRepository.save(AssistantTier.builder()
                .name("pro").displayName("Pro")
                .monthlyTokenLimit(2000000L).modelId("claude-sonnet-4-6")
                .provider("anthropic").priceMonthly(new BigDecimal("29.99")).build());
        assistantTierRepository.save(AssistantTier.builder()
                .name("enterprise").displayName("Enterprise")
                .monthlyTokenLimit(-1L).modelId("claude-opus-4-6")
                .provider("anthropic").priceMonthly(new BigDecimal("99.99")).build());
        log.info("Assistant tiers seeded");
    }

    private void seedEnabledAiModels() {
        if (enabledAiModelRepository.count() > 0) return;
        log.info("Seeding enabled AI models...");
        seedModel("anthropic", "claude-haiku-4-5-20251001");
        seedModel("anthropic", "claude-sonnet-4-6");
        seedModel("anthropic", "claude-opus-4-6");
        seedModel("openai", "gpt-4o-mini");
        seedModel("openai", "gpt-4o");
        seedModel("openai", "gpt-4-turbo");
        seedModel("openai", "gpt-4");
        seedModel("google", "gemini-2.5-flash");
        seedModel("google", "gemini-2.5-flash-lite");
        seedModel("google", "gemini-3-flash");
        seedModel("google", "gemini-3.1-flash-lite-preview");
        log.info("Enabled AI models seeded");
    }

    private void seedModel(String provider, String modelId) {
        enabledAiModelRepository.save(EnabledAiModel.builder()
                .provider(provider).modelId(modelId).enabled(true).build());
    }
}
