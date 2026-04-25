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
package com.bocrm.backend.integration;

import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.repository.OutboxEventRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled poller for unpublished CRM events.
 * Routes CRM_EVENT entries from the outbox to enabled integrations.
 * Runs every 30 seconds in non-test profiles.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Profile("!test")
@Slf4j
public class IntegrationPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final TenantRepository tenantRepository;
    private final IntegrationEventRouter integrationEventRouter;

    public IntegrationPoller(OutboxEventRepository outboxEventRepository,
                             TenantRepository tenantRepository,
                             IntegrationEventRouter integrationEventRouter) {
        this.outboxEventRepository = outboxEventRepository;
        this.tenantRepository = tenantRepository;
        this.integrationEventRouter = integrationEventRouter;
    }

    /**
     * Poll unpublished CRM events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30_000)
    public void poll() {
        try {
            List<Tenant> allTenants = tenantRepository.findAll();
            int totalProcessed = 0;

            for (Tenant tenant : allTenants) {
                TenantContext.setTenantId(tenant.getId());
                try {
                    // Find unpublished CRM_EVENT entries for this tenant
                    List<OutboxEvent> unpublishedCrmEvents = outboxEventRepository
                            .findByEventTypeAndPublishedAtIsNull("CRM_EVENT");

                    if (!unpublishedCrmEvents.isEmpty()) {
                        log.debug("Processing {} unpublished CRM_EVENT entries for tenant {}", unpublishedCrmEvents.size(), tenant.getId());
                        for (OutboxEvent event : unpublishedCrmEvents) {
                            processEvent(event);
                        }
                        totalProcessed += unpublishedCrmEvents.size();
                    }
                } finally {
                    TenantContext.clear();
                }
            }

            if (totalProcessed > 0) {
                log.debug("Processed {} total unpublished CRM_EVENT entries across all tenants", totalProcessed);
            }
        } catch (Exception e) {
            log.error("Error polling CRM events for integration: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single CRM event: route to integrations and mark as published.
     */
    @Transactional
    protected void processEvent(OutboxEvent event) {
        try {
            // Route to enabled integrations
            integrationEventRouter.route(event);

            // Mark as published
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.debug("Processed CRM event ID {}: marked published", event.getId());
        } catch (Exception e) {
            log.error("Error processing CRM event ID {}: {}", event.getId(), e.getMessage(), e);
            // Increment retry count but don't rethrow - allow other events to be processed
            event.setRetryCount((event.getRetryCount() != null ? event.getRetryCount() : 0) + 1);
            outboxEventRepository.save(event);
        }
    }
}
