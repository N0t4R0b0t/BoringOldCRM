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

import com.bocrm.backend.entity.Activity;
import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.repository.ActivityRepository;
import com.bocrm.backend.repository.OutboxEventRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;

/**
 * Scheduled poller for outbox events and activity due dates.
 * - Polls unpublished outbox events every 60 seconds
 * - Scans for activities due within 24 hours daily at 8 AM
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Profile("!test")
@Slf4j
public class NotificationPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final ActivityRepository activityRepository;
    private final TenantRepository tenantRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationPoller(OutboxEventRepository outboxEventRepository,
                             ActivityRepository activityRepository,
                             TenantRepository tenantRepository,
                             NotificationDispatchService notificationDispatchService,
                             NotificationService notificationService,
                             ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.activityRepository = activityRepository;
        this.tenantRepository = tenantRepository;
        this.notificationDispatchService = notificationDispatchService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Poll unpublished outbox events every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    public void poll() {
        try {
            List<Tenant> allTenants = tenantRepository.findAll();
            int totalProcessed = 0;

            for (Tenant tenant : allTenants) {
                TenantContext.setTenantId(tenant.getId());
                try {
                    List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedAtIsNull();
                    if (!unpublished.isEmpty()) {
                        log.debug("Processing {} unpublished outbox events for tenant {}", unpublished.size(), tenant.getId());
                        for (OutboxEvent event : unpublished) {
                            processEvent(event);
                        }
                        totalProcessed += unpublished.size();
                    }
                } finally {
                    TenantContext.clear();
                }
            }
            if (totalProcessed > 0) {
                log.debug("Processed {} total unpublished outbox events across all tenants", totalProcessed);
            }
        } catch (Exception e) {
            log.error("Error polling outbox events: {}", e.getMessage(), e);
        }
    }

    /**
     * Scan for activities due within 24 hours daily at 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void scanDueActivities() {
        try {
            log.debug("Scanning for activities due within 24 hours");
            List<Tenant> allTenants = tenantRepository.findAll();

            for (Tenant tenant : allTenants) {
                TenantContext.setTenantId(tenant.getId());
                try {
                    scanDueActivitiesInternal(tenant.getId());
                } finally {
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            log.error("Error scanning due activities: {}", e.getMessage(), e);
        }
    }

    // ─── internals ─────────────────────────────────────────────────────────

    private void processEvent(OutboxEvent event) {
        Long tenantId = event.getTenantId();
        TenantContext.setTenantId(tenantId);
        try {
            processEventInternal(event);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    private void processEventInternal(OutboxEvent event) {
        boolean success = notificationDispatchService.dispatch(event);
        if (success) {
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            log.debug("Outbox event {} marked published", event.getId());
        } else {
            // Transient failure — increment retry count in payload
            try {
                ObjectNode payload = (ObjectNode) objectMapper.readTree(event.getPayloadJsonb());
                int retryCount = payload.path("retryCount").asInt(0);
                if (retryCount < 3) {
                    payload.put("retryCount", retryCount + 1);
                    event.setPayloadJsonb(objectMapper.writeValueAsString(payload));
                    outboxEventRepository.save(event);
                    log.debug("Outbox event {} retry count incremented to {}", event.getId(), retryCount + 1);
                } else {
                    // Max retries exceeded — mark published anyway
                    event.setPublishedAt(LocalDateTime.now());
                    outboxEventRepository.save(event);
                    log.warn("Outbox event {} max retries exceeded, marking published", event.getId());
                }
            } catch (Exception e) {
                log.error("Error updating retry count for outbox event {}: {}", event.getId(), e.getMessage());
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            }
        }
    }

    @Transactional
    private void scanDueActivitiesInternal(Long tenantId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime inTwentyFourHours = now.plusHours(24);

            // Find activities that are due within 24 hours and are not completed
            List<Activity> dueActivities = activityRepository.findByTenantId(tenantId).stream()
                    .filter(a -> a.getDueAt() != null)
                    .filter(a -> a.getDueAt().isAfter(now) && a.getDueAt().isBefore(inTwentyFourHours))
                    .filter(a -> !"completed".equalsIgnoreCase(a.getStatus()))
                    .toList();

            for (Activity activity : dueActivities) {
                if (activity.getOwnerId() != null) {
                    notificationService.notifyActivityDueSoon(
                            tenantId,
                            activity.getOwnerId(),
                            activity.getId(),
                            activity.getSubject(),
                            activity.getDueAt()
                    );
                    log.debug("Activity {} due soon notification enqueued for owner {}",
                            activity.getId(), activity.getOwnerId());
                }
            }
        } catch (Exception e) {
            log.error("Error scanning due activities for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
}
