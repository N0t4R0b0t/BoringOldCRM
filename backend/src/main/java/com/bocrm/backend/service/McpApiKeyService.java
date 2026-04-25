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

import com.bocrm.backend.entity.McpApiKey;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.McpApiKeyRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
/**
 * McpApiKeyService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class McpApiKeyService {
    private final McpApiKeyRepository mcpApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public McpApiKeyService(McpApiKeyRepository mcpApiKeyRepository, PasswordEncoder passwordEncoder) {
        this.mcpApiKeyRepository = mcpApiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateApiKey(String name) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        // Generate 32 random bytes and encode as URL-safe Base64 (no padding)
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String base64Suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Build raw key: "sk-bocrm-" + base64suffix
        String rawKey = "sk-bocrm-" + base64Suffix;

        // Extract prefix = first 12 chars of raw key
        String keyPrefix = rawKey.substring(0, 12);

        // BCrypt hash the raw key
        String keyHash = passwordEncoder.encode(rawKey);

        // Store the key
        McpApiKey key = McpApiKey.builder()
                .tenantId(tenantId)
                .userId(userId)
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .name(name)
                .enabled(true)
                .build();

        mcpApiKeyRepository.save(key);

        // Return the raw key (shown to user once)
        return rawKey;
    }

    @Transactional(readOnly = true)
    public List<McpApiKey> listApiKeys() {
        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        return mcpApiKeyRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void revokeApiKey(Long keyId) {
        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        McpApiKey key = mcpApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with ID: " + keyId));

        // Verify tenant ownership
        if (!key.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        // Soft delete by setting enabled = false
        key.setEnabled(false);
        mcpApiKeyRepository.save(key);
    }

    public void updateLastUsed(McpApiKey key) {
        try {
            key.setLastUsedAt(java.time.LocalDateTime.now());
            mcpApiKeyRepository.save(key);
        } catch (Exception e) {
            // Best-effort, do not fail the request
            log.warn("Failed to update last_used_at for API key {}", key.getId(), e);
        }
    }
}
