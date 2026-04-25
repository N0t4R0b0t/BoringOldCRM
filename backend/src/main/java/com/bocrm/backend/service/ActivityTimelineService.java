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

import com.bocrm.backend.dto.TimelineEventDTO;
import com.bocrm.backend.entity.Activity;
import com.bocrm.backend.entity.AuditLog;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.ActivityRepository;
import com.bocrm.backend.repository.AuditLogRepository;
import com.bocrm.backend.shared.TenantContext;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/**
 * ActivityTimelineService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class ActivityTimelineService {
    private final ActivityRepository activityRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ActivityTimelineService(ActivityRepository activityRepository, AuditLogRepository auditLogRepository,
                                   ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDTO> getTimeline(String entityType, Long entityId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<TimelineEventDTO> events = new ArrayList<>();

        // Get activities related to this entity
        List<Activity> activities = activityRepository.findByRelatedTypeAndRelatedId(entityType, entityId);
        for (Activity activity : activities) {
            if (activity.getTenantId().equals(tenantId)) {
                events.add(TimelineEventDTO.builder()
                        .id(activity.getId())
                        .type("ACTIVITY")
                        .action(activity.getType())
                        .title(activity.getSubject())
                        .description(activity.getDescription())
                        .timestamp(activity.getCreatedAt())
                        .actorId(activity.getOwnerId())
                        .actorName(activity.getOwnerId() != null ? "Owner #" + activity.getOwnerId() : "System")
                        .details(null)
                        .build());
            }
        }

        // Get audit logs for changes to this entity
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        for (AuditLog audit : auditLogs) {
            if (audit.getTenantId().equals(tenantId)) {
                try {
                    events.add(TimelineEventDTO.builder()
                            .id(audit.getId())
                            .type("AUDIT")
                            .action(audit.getAction())
                            .title(audit.getAction() + " - " + audit.getEntityType())
                            .description(null)
                            .timestamp(audit.getCreatedAt())
                            .actorId(audit.getActorId())
                            .actorName(audit.getActorId() != null ? "User #" + audit.getActorId() : "System")
                            .details(objectMapper.readTree(audit.getPayloadJsonb()))
                            .build());
                } catch (Exception e) {
                    log.error("Error parsing audit log payload", e);
                }
            }
        }

        // Sort by timestamp descending (most recent first)
        events.sort(Comparator.comparing(TimelineEventDTO::getTimestamp).reversed());

        return events;
    }
}
