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

import com.bocrm.backend.dto.CreateIntegrationConfigRequest;
import com.bocrm.backend.dto.IntegrationConfigDTO;
import com.bocrm.backend.dto.UpdateIntegrationConfigRequest;
import com.bocrm.backend.entity.IntegrationConfig;
import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.integration.IntegrationAdapter;
import com.bocrm.backend.repository.IntegrationConfigRepository;
import com.bocrm.backend.repository.OutboxEventRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing tenant-scoped integration configurations.
 * Handles encryption/decryption of credentials and CRUD operations.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class IntegrationConfigService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final Set<IntegrationAdapter> adapters;

    public IntegrationConfigService(IntegrationConfigRepository integrationConfigRepository,
                                    EncryptionService encryptionService,
                                    ObjectMapper objectMapper,
                                    OutboxEventRepository outboxEventRepository,
                                    Set<IntegrationAdapter> adapters) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.adapters = adapters;
    }

    /**
     * List all integration configs for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<IntegrationConfigDTO> listConfigs() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        return integrationConfigRepository.findByTenantId(tenantId)
                .stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a single integration config by ID.
     */
    @Transactional(readOnly = true)
    public IntegrationConfigDTO getConfig(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        IntegrationConfig config = integrationConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));

        if (!config.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return entityToDTO(config);
    }

    /**
     * Create a new integration config.
     * Credentials are encrypted before storing.
     */
    @Transactional
    public IntegrationConfigDTO createConfig(CreateIntegrationConfigRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        // Encrypt credentials
        String credentialsJson = objectMapper.writeValueAsString(request.getCredentials());
        String encryptedCredentials = encryptionService.encrypt(credentialsJson);

        IntegrationConfig config = IntegrationConfig.builder()
                .tenantId(tenantId)
                .adapterType(request.getAdapterType())
                .name(request.getName())
                .enabled(request.isEnabled())
                .credentialsEncrypted(encryptedCredentials)
                .eventTypes(request.getEventTypes() != null ? request.getEventTypes() : "")
                .build();

        IntegrationConfig saved = integrationConfigRepository.save(config);
        log.info("Created integration config {} for tenant {}", saved.getId(), tenantId);

        return entityToDTO(saved);
    }

    /**
     * Update an integration config.
     * If credentials are provided, they will be encrypted.
     */
    @Transactional
    public IntegrationConfigDTO updateConfig(Long id, UpdateIntegrationConfigRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        IntegrationConfig config = integrationConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));

        if (!config.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        // Update fields
        if (request.getName() != null) {
            config.setName(request.getName());
        }
        config.setEnabled(request.isEnabled());
        if (request.getEventTypes() != null) {
            config.setEventTypes(request.getEventTypes());
        }

        // Update credentials if provided
        if (request.getCredentials() != null) {
            try {
                String credentialsJson = objectMapper.writeValueAsString(request.getCredentials());
                String encryptedCredentials = encryptionService.encrypt(credentialsJson);
                config.setCredentialsEncrypted(encryptedCredentials);
            } catch (Exception e) {
                log.error("Failed to encrypt credentials: {}", e.getMessage());
                throw new RuntimeException("Failed to encrypt credentials", e);
            }
        }

        IntegrationConfig saved = integrationConfigRepository.save(config);
        log.info("Updated integration config {} for tenant {}", id, tenantId);

        return entityToDTO(saved);
    }

    /**
     * Delete an integration config.
     */
    @Transactional
    public void deleteConfig(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        IntegrationConfig config = integrationConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));

        if (!config.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        integrationConfigRepository.deleteById(id);
        log.info("Deleted integration config {} for tenant {}", id, tenantId);
    }

    /**
     * Fire a test event through a specific integration config to verify credentials.
     */
    public String testConfig(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        IntegrationConfig config = integrationConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));
        if (!config.getTenantId().equals(tenantId)) throw new ForbiddenException("Access denied");

        try {
            String credJson = encryptionService.decrypt(config.getCredentialsEncrypted());
            Map<String, String> credentials = objectMapper.readValue(credJson,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

            ObjectNode testData = objectMapper.createObjectNode();
            testData.put("name", "Test Record");
            testData.put("status", "active");

            adapters.stream()
                    .filter(a -> a.name().equals(config.getAdapterType()))
                    .findFirst()
                    .ifPresent(adapter -> {
                        try {
                            adapter.process("CUSTOMER_CREATED", "Customer", 0L, "CREATED", testData, credentials);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

            log.info("Test event sent for integration config {} ({})", id, config.getAdapterType());
            return "Test event sent successfully via " + config.getName();
        } catch (Exception e) {
            log.error("Test event failed for integration config {}: {}", id, e.getMessage());
            return "Test failed: " + e.getMessage();
        }
    }

    /**
     * Return dead-lettered CRM integration events (retryCount >= 3, still unpublished) for current tenant.
     */
    public List<Map<String, Object>> getFailedEvents() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        return outboxEventRepository
                .findByTenantIdAndEventTypeAndRetryCountGreaterThanEqual(tenantId, "CRM_EVENT", 3)
                .stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", e.getId());
                    m.put("retryCount", e.getRetryCount());
                    m.put("createdAt", e.getCreatedAt());
                    try {
                        m.put("payload", objectMapper.readTree(e.getPayloadJsonb()));
                    } catch (Exception ex) {
                        m.put("payload", e.getPayloadJsonb());
                    }
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO (credentials are not included in the DTO).
     */
    private IntegrationConfigDTO entityToDTO(IntegrationConfig entity) {
        return IntegrationConfigDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .adapterType(entity.getAdapterType())
                .name(entity.getName())
                .enabled(entity.isEnabled())
                .eventTypes(entity.getEventTypes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
