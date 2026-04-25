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

import com.bocrm.backend.dto.QuotaStatusDTO;
import com.bocrm.backend.entity.AssistantTier;
import com.bocrm.backend.entity.TenantSubscription;
import com.bocrm.backend.entity.TokenUsageLedger;
import com.bocrm.backend.exception.QuotaExceededException;
import com.bocrm.backend.repository.AssistantTierRepository;
import com.bocrm.backend.repository.TenantSubscriptionRepository;
import com.bocrm.backend.repository.TokenUsageLedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Manages per-tenant token quotas and usage tracking.
 * Accesses public-schema admin tables directly; must NOT set TenantContext.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class TokenQuotaService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final AssistantTierRepository tierRepository;
    private final TokenUsageLedgerRepository ledgerRepository;

    public TokenQuotaService(TenantSubscriptionRepository subscriptionRepository,
                              AssistantTierRepository tierRepository,
                              TokenUsageLedgerRepository ledgerRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.tierRepository = tierRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Checks if the tenant has remaining quota. Creates a Free subscription if none exists.
     * Resets the period if it has expired.
     * Throws QuotaExceededException if the tenant has hit their limit.
     */
    @Transactional
    public void checkQuota(Long tenantId) {
        TenantSubscription sub = getOrCreateSubscription(tenantId);
        resetPeriodIfExpired(sub);

        AssistantTier tier = sub.getTier();
        if (tier.getMonthlyTokenLimit() == -1) {
            return; // unlimited
        }
        if (sub.getTokensUsedThisPeriod() >= tier.getMonthlyTokenLimit()) {
            throw new QuotaExceededException(tier.getDisplayName(), sub.getPeriodEndDate());
        }
    }

    /**
     * Records token usage after a successful call.
     * Atomically increments the subscription counter and writes a ledger entry.
     */
    @Transactional
    public void recordUsage(Long tenantId, Long userId, int inputTokens, int outputTokens,
                            String modelId, String sessionId, String operation) {
        long total = (long) inputTokens + outputTokens;
        subscriptionRepository.incrementTokensUsed(tenantId, total);

        TokenUsageLedger entry = TokenUsageLedger.builder()
                .userId(userId)
                .sessionId(sessionId)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .modelUsed(modelId)
                .operation(operation)
                .build();
        ledgerRepository.save(entry);

        log.debug("Recorded {} tokens for tenant {} (in={}, out={})", total, tenantId, inputTokens, outputTokens);
    }

    /**
     * Returns the current quota status for display in the UI.
     * Not readOnly because getOrCreateSubscription may INSERT a new subscription.
     */
    @Transactional
    public QuotaStatusDTO getQuotaStatus(Long tenantId) {
        TenantSubscription sub = getOrCreateSubscription(tenantId);
        AssistantTier tier = sub.getTier();
        long limit = tier.getMonthlyTokenLimit();
        long used = sub.getTokensUsedThisPeriod();
        Double percent = limit == -1 ? null : (used * 100.0 / limit);
        return QuotaStatusDTO.builder()
                .tokensUsed(used)
                .tokenLimit(limit)
                .percentUsed(percent)
                .periodEnd(sub.getPeriodEndDate())
                .tierName(tier.getDisplayName())
                .quotaExceeded(limit != -1 && used >= limit)
                .build();
    }

    /**
     * Returns the Claude model ID assigned to this tenant's tier.
     */
    @Transactional
    public String resolveModelForTenant(Long tenantId) {
        TenantSubscription sub = getOrCreateSubscription(tenantId);
        return sub.getTier().getModelId();
    }

    /**
     * Returns both the model ID and provider for this tenant's tier.
     */
    @Transactional
    public TierInfo resolveTierInfoForTenant(Long tenantId) {
        TenantSubscription sub = getOrCreateSubscription(tenantId);
        AssistantTier tier = sub.getTier();
        String provider = tier.getProvider() != null ? tier.getProvider() : "anthropic";
        return new TierInfo(tier.getModelId(), provider);
    }

    public record TierInfo(String modelId, String provider) {}

    /**
     * Changes the tenant's subscription tier (admin operation).
     */
    @Transactional
    public TenantSubscription changeTier(Long tenantId, String tierName) {
        AssistantTier newTier = tierRepository.findByName(tierName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tier: " + tierName));
        TenantSubscription sub = getOrCreateSubscription(tenantId);
        sub.setTier(newTier);
        LocalDate now = LocalDate.now();
        sub.setPeriodStartDate(now);
        sub.setPeriodEndDate(now.plusMonths(1));
        sub.setTokensUsedThisPeriod(0L);
        return subscriptionRepository.save(sub);
    }

    /**
     * Apply a tier to all tenants (system admin operation).
     */
    public void applyTierToAllTenants(String tierName) {
        AssistantTier newTier = tierRepository.findByName(tierName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tier: " + tierName));

        // Get all subscriptions and update them
        List<TenantSubscription> allSubscriptions = subscriptionRepository.findAll();
        LocalDate now = LocalDate.now();

        for (TenantSubscription sub : allSubscriptions) {
            sub.setTier(newTier);
            sub.setPeriodStartDate(now);
            sub.setPeriodEndDate(now.plusMonths(1));
            sub.setTokensUsedThisPeriod(0L);
            subscriptionRepository.save(sub);
        }
    }

    // --- Private helpers ---

    private TenantSubscription getOrCreateSubscription(Long tenantId) {
        Optional<TenantSubscription> existing = subscriptionRepository.findByTenantId(tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }
        AssistantTier freeTier = tierRepository.findByName("free")
                .orElseThrow(() -> new IllegalStateException("'free' tier not found in database"));
        LocalDate now = LocalDate.now();
        TenantSubscription sub = TenantSubscription.builder()
                .tenantId(tenantId)
                .tier(freeTier)
                .periodStartDate(now)
                .periodEndDate(now.plusMonths(1))
                .tokensUsedThisPeriod(0L)
                .build();
        return subscriptionRepository.save(sub);
    }

    private void resetPeriodIfExpired(TenantSubscription sub) {
        if (LocalDate.now().isAfter(sub.getPeriodEndDate())) {
            sub.setPeriodStartDate(LocalDate.now());
            sub.setPeriodEndDate(LocalDate.now().plusMonths(1));
            sub.setTokensUsedThisPeriod(0L);
            subscriptionRepository.save(sub);
        }
    }
}
