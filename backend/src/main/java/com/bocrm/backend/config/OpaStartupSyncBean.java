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

import com.bocrm.backend.service.OpaSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads all existing policy rules into OPA on application startup.
 * Runs after the main data initializer (@Order(2)) so the DB is fully ready.
 *
 * <p>Swallows connectivity failures so a temporarily unavailable OPA sidecar
 * does not prevent startup. The first actual request will fail-closed if OPA
 * is still down at that point.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Order(2)
@Slf4j
public class OpaStartupSyncBean implements CommandLineRunner {

    private final OpaSyncService opaSyncService;
    private final OpaProperties opaProperties;

    public OpaStartupSyncBean(OpaSyncService opaSyncService, OpaProperties opaProperties) {
        this.opaSyncService = opaSyncService;
        this.opaProperties = opaProperties;
    }

    @Override
    public void run(String... args) {
        if (!opaProperties.isEnabled()) {
            log.debug("OPA is disabled — skipping startup policy sync");
            return;
        }
        try {
            opaSyncService.syncAll();
        } catch (Exception e) {
            log.warn("OPA startup sync failed (OPA may not be ready yet): {}", e.getMessage());
        }
    }
}
