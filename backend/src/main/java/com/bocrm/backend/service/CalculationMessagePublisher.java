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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
/**
 * CalculationMessagePublisher.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class CalculationMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public CalculationMessagePublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        // In test profile, RabbitTemplate bean won't exist and getIfAvailable() returns null
        this.rabbitTemplate = rabbitTemplateProvider.getIfAvailable(() -> null);
    }

    /**
     * Publish a calculated-field refresh request to RabbitMQ.
     * Non-fatal: logs a warning on failure and returns normally.
     * In test profile, rabbitTemplate is null and publish is a no-op.
     */
    public void publish(Long tenantId, String entityType, Long entityId) {
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate not available (test profile?), skipping publish");
            return;
        }
        try {
            CalculationMessage message = new CalculationMessage(tenantId, entityType, entityId, "entity_data_changed");
            rabbitTemplate.convertAndSend(
                CalculationRabbitConfig.EXCHANGE,
                CalculationRabbitConfig.ROUTING_KEY,
                message
            );
            log.debug("Published calculated field refresh for {} {} (tenant {})", entityType, entityId, tenantId);
        } catch (Exception e) {
            log.warn("Failed to publish calculated field refresh for {} {}: {}", entityType, entityId, e.getMessage());
            // Non-fatal — calculated fields will be stale until next trigger
        }
    }
}
