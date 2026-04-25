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

import com.bocrm.backend.dto.CreateIntegrationConfigRequest;
import com.bocrm.backend.dto.IntegrationConfigDTO;
import com.bocrm.backend.dto.UpdateIntegrationConfigRequest;
import com.bocrm.backend.service.IntegrationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing integrations.
 * All endpoints are tenant-scoped (via TenantContext).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@RestController
@RequestMapping("/integrations")
@Tag(name = "Integrations", description = "Integration configuration management")
@Slf4j
public class IntegrationController {

    private final IntegrationConfigService integrationConfigService;

    public IntegrationController(IntegrationConfigService integrationConfigService) {
        this.integrationConfigService = integrationConfigService;
    }

    @GetMapping
    @Operation(summary = "List all integration configs for the current tenant")
    public ResponseEntity<List<IntegrationConfigDTO>> listConfigs() {
        return ResponseEntity.ok(integrationConfigService.listConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single integration config by ID")
    public ResponseEntity<IntegrationConfigDTO> getConfig(@PathVariable Long id) {
        return ResponseEntity.ok(integrationConfigService.getConfig(id));
    }

    @PostMapping
    @Operation(summary = "Create a new integration config")
    public ResponseEntity<IntegrationConfigDTO> createConfig(@Valid @RequestBody CreateIntegrationConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationConfigService.createConfig(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an integration config")
    public ResponseEntity<IntegrationConfigDTO> updateConfig(@PathVariable Long id, @Valid @RequestBody UpdateIntegrationConfigRequest request) {
        return ResponseEntity.ok(integrationConfigService.updateConfig(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an integration config")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        integrationConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Send a test event through an integration config")
    public ResponseEntity<Map<String, String>> testConfig(@PathVariable Long id) {
        String result = integrationConfigService.testConfig(id);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/failed-events")
    @Operation(summary = "List dead-lettered CRM integration events for the current tenant")
    public ResponseEntity<List<Map<String, Object>>> getFailedEvents() {
        return ResponseEntity.ok(integrationConfigService.getFailedEvents());
    }
}
