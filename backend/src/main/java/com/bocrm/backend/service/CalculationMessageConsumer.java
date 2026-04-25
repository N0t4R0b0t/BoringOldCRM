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

import com.bocrm.backend.config.CalculationRabbitConfig;
import com.bocrm.backend.dto.CalculationMessage;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
/**
 * CalculationMessageConsumer.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Profile("!test")
@Slf4j
public class CalculationMessageConsumer {

    private final CalculatedFieldRecalculationService recalculationService;
    private final SimpMessagingTemplate messagingTemplate;

    public CalculationMessageConsumer(CalculatedFieldRecalculationService recalculationService, SimpMessagingTemplate messagingTemplate) {
        this.recalculationService = recalculationService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = CalculationRabbitConfig.QUEUE, containerFactory = "calculationListenerContainerFactory")
    public void onMessage(CalculationMessage message) {
        log.debug("Received calculation message for {} {} (tenant {})",
            message.entityType(), message.entityId(), message.tenantId());
        try {
            TenantContext.setTenantId(message.tenantId());
            TenantContext.setUserId(null);

            // Small delay to ensure DB transaction is committed before reading
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // null calculatedFieldId = recalculate ALL calculated fields for the entity
            recalculationService.recalculateAndStore(
                message.tenantId(),
                message.entityType(),
                message.entityId(),
                null
            );

            // Notify frontend via WebSocket to trigger table refresh
            try {
                messagingTemplate.convertAndSend(
                    "/topic/calculations/" + message.tenantId(),
                    (Object) Map.of("entityType", message.entityType(), "entityId", message.entityId())
                );
            } catch (Exception e) {
                log.warn("Failed to send WebSocket notification: {}", e.getMessage());
            }
        } catch (ResourceNotFoundException e) {
            // Entity was deleted after the message was enqueued — discard silently
            log.debug("Discarding stale calculation message for {} {} (entity no longer exists)",
                message.entityType(), message.entityId());
        } catch (Exception e) {
            log.error("Failed to process calculation message for {} {}: {}",
                message.entityType(), message.entityId(), e.getMessage(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }
}
