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
package com.bocrm.backend.tools;

import com.bocrm.backend.dto.TenantDTO;
import com.bocrm.backend.entity.TenantBackupJob;
import com.bocrm.backend.entity.TenantMembership;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.service.TenantAdminService;
import com.bocrm.backend.service.TenantBackupService;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring AI tool methods for system-admin operations.
 * Each method requires ROLE_SYSTEM_ADMIN.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class SystemAdminTools {

    private final TenantAdminService tenantAdminService;
    private final TenantBackupService tenantBackupService;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public SystemAdminTools(TenantAdminService tenantAdminService,
                            TenantBackupService tenantBackupService,
                            TenantMembershipRepository tenantMembershipRepository,
                            TenantRepository tenantRepository,
                            ObjectMapper objectMapper) {
        this.tenantAdminService = tenantAdminService;
        this.tenantBackupService = tenantBackupService;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "SYSTEM_ADMIN only. Create a new blank tenant with the given name and add the " +
            "calling system admin as an admin member. The tenant is provisioned with default templates " +
            "and field options. IMPORTANT: Always confirm with the user before executing.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createTenant(String tenantName) {
        requireSystemAdmin();
        Long adminUserId = TenantContext.getUserId();
        if (adminUserId == null) throw new ForbiddenException("Not authenticated");

        try {
            TenantDTO newTenant = tenantAdminService.createTenant(tenantName);
            Long newTenantId = newTenant.getId();
            log.info("Created blank tenant id={} name='{}'", newTenantId, tenantName);

            addAdminMembership(newTenantId, adminUserId);

            return "Tenant created: id=" + newTenantId + ", name=" + tenantName + ". You have been added as admin.";
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("createTenant failed", e);
            return "Error creating tenant: " + e.getMessage();
        }
    }

    @Tool(description = "SYSTEM_ADMIN only. Create a new tenant from a BOCRM backup JSON string. " +
            "Provisions a fresh tenant schema, restores all data (customers, contacts, opportunities, " +
            "activities, orders, invoices, customRecords, custom fields, templates, policies, etc.), and adds " +
            "the calling system admin as an admin member. " +
            "tenantNameOverride is optional — if provided it takes precedence over the name in the backup. " +
            "IMPORTANT: Always confirm with the user before executing. This is irreversible.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String restoreTenantFromBackup(String backupJson, String tenantNameOverride) {
        log.info("restoreTenantFromBackup called with backupJson length={}, tenantNameOverride={}",
                 backupJson != null ? backupJson.length() : 0, tenantNameOverride);
        requireSystemAdmin();
        Long adminUserId = TenantContext.getUserId();
        if (adminUserId == null) throw new ForbiddenException("Not authenticated");

        if (backupJson == null || backupJson.isBlank()) {
            return "Error: backupJson parameter is empty. Please provide the full backup JSON content.";
        }

        try {
            JsonNode root = objectMapper.readTree(backupJson);

            String tenantName = (tenantNameOverride != null && !tenantNameOverride.isBlank())
                    ? tenantNameOverride
                    : extractTenantName(root);
            if (tenantName == null) {
                return "Error: backup JSON does not contain a 'tenantName' field and no override was provided. " +
                       "Re-export the backup from an updated BOCRM instance or pass tenantNameOverride.";
            }

            TenantDTO newTenant = tenantAdminService.createTenant(tenantName);
            Long newTenantId = newTenant.getId();
            log.info("Created tenant id={} name='{}' for restore", newTenantId, tenantName);

            runRestore(backupJson, newTenantId);
            addAdminMembership(newTenantId, adminUserId);

            return buildRestoreSummary(newTenantId, tenantName, root);
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("restoreTenantFromBackup failed", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (e.getMessage() != null && (e.getMessage().contains("JSON") || e.getMessage().contains("parse"))) {
                return "Error: backupJson is not valid JSON. Check that the backup file was exported correctly. Details: " + errorMsg;
            }
            return "Error restoring tenant: " + errorMsg;
        }
    }

    @Tool(description = "SYSTEM_ADMIN only. Create a full backup of a tenant and return a download URL. " +
            "tenantName is optional — if omitted, backs up the current tenant. " +
            "Returns a download link the user can click to save the backup JSON file.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String backupTenant(String tenantName) {
        requireSystemAdmin();

        try {
            Long tenantId;
            String resolvedName;

            if (tenantName != null && !tenantName.isBlank()) {
                Tenant source = tenantRepository.findByName(tenantName)
                        .orElseGet(() -> tenantRepository.findAll().stream()
                                .filter(t -> t.getName().equalsIgnoreCase(tenantName))
                                .findFirst()
                                .orElse(null));
                if (source == null) {
                    return "Tenant not found: '" + tenantName + "'. Check the name and try again.";
                }
                tenantId = source.getId();
                resolvedName = source.getName();
            } else {
                tenantId = TenantContext.getTenantId();
                if (tenantId == null) throw new ForbiddenException("No tenant context — pass a tenantName explicitly.");
                resolvedName = "current tenant";
            }

            TenantBackupJob job = runBackupJob(tenantId);
            return "Backup of '" + resolvedName + "' complete (job id=" + job.getId() + ").\n" +
                   "[Download Backup](/api/admin/backup/jobs/" + job.getId() + "/download)";
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("backupTenant failed", e);
            return "Error backing up tenant: " + e.getMessage();
        }
    }

    @Tool(description = "SYSTEM_ADMIN only. Clone an existing tenant into a brand-new tenant. " +
            "sourceTenantName is the name of the tenant to clone (case-insensitive). " +
            "Creates a full backup of that tenant then restores it as a new tenant. " +
            "newTenantName is optional — if omitted, appends '(Clone)' to the source tenant name. " +
            "The calling system admin is added as an admin member of the new tenant. " +
            "IMPORTANT: Always confirm with the user before executing. This is irreversible.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String cloneTenant(String sourceTenantName, String newTenantName) {
        requireSystemAdmin();
        Long adminUserId = TenantContext.getUserId();
        if (adminUserId == null) throw new ForbiddenException("Not authenticated");

        try {
            Tenant source = tenantRepository.findByName(sourceTenantName)
                    .orElseGet(() -> tenantRepository.findAll().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(sourceTenantName))
                            .findFirst()
                            .orElse(null));
            if (source == null) {
                return "Tenant not found: '" + sourceTenantName + "'. Check the name and try again.";
            }

            TenantBackupJob backupJob = runBackupJob(source.getId());
            String backupPayload = backupJob.getResultPayload();
            if (backupPayload == null || backupPayload.isBlank()) {
                return "Backup of tenant '" + sourceTenantName + "' produced no payload.";
            }

            String cloneName = (newTenantName != null && !newTenantName.isBlank())
                    ? newTenantName
                    : source.getName() + " (Clone)";

            TenantDTO newTenant = tenantAdminService.createTenant(cloneName);
            Long newTenantId = newTenant.getId();
            log.info("Cloning tenant '{}' (id={}) → new tenant id={} name='{}'",
                    sourceTenantName, source.getId(), newTenantId, cloneName);

            runRestore(backupPayload, newTenantId);
            addAdminMembership(newTenantId, adminUserId);

            return "Tenant cloned. Source '" + sourceTenantName + "' → new tenant id=" + newTenantId + ", name=" + cloneName + ".";
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("cloneTenant failed", e);
            return "Error cloning tenant: " + e.getMessage();
        }
    }

    // --- helpers ---

    private TenantBackupJob runBackupJob(Long tenantId) throws Exception {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            TenantBackupJob job = tenantBackupService.createBackupJob(true, tenantId);
            tenantBackupService.executeJob(job);
            if (!"COMPLETED".equals(job.getStatus())) {
                throw new IllegalStateException("Backup of tenant " + tenantId + " failed with status: " + job.getStatus());
            }
            return job;
        } finally {
            restoreContext(previousTenantId);
        }
    }

    private void runRestore(String backupPayload, Long newTenantId) {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(newTenantId);
            TenantBackupJob job = tenantBackupService.createRestoreJob(backupPayload, newTenantId);
            tenantBackupService.executeJob(job);
            if (!"COMPLETED".equals(job.getStatus())) {
                log.warn("Restore into tenant {} ended with status {}", newTenantId, job.getStatus());
            }
        } finally {
            restoreContext(previousTenantId);
        }
    }

    private void addAdminMembership(Long tenantId, Long userId) {
        boolean alreadyMember = tenantMembershipRepository
                .findByTenantIdAndUserId(tenantId, userId)
                .isPresent();
        if (!alreadyMember) {
            tenantMembershipRepository.save(TenantMembership.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .role("admin")
                    .status("active")
                    .build());
        }
    }

    private void restoreContext(Long previousTenantId) {
        if (previousTenantId != null) {
            TenantContext.setTenantId(previousTenantId);
        } else {
            TenantContext.removeTenantId();
        }
    }

    private String extractTenantName(JsonNode root) {
        JsonNode node = root.get("tenantName");
        return (node != null && node.isString()) ? node.stringValue() : null;
    }

    private String buildRestoreSummary(Long newTenantId, String tenantName, JsonNode root) {
        StringBuilder sb = new StringBuilder("Tenant restore complete.\n");
        sb.append("New tenant: id=").append(newTenantId).append(", name=").append(tenantName).append("\n");
        sb.append("Records restored:\n");
        for (String key : new String[]{
                "customers", "contacts", "opportunities", "activities",
                "orders", "invoices", "custom_records", "documents", "chatMessages",
                "customFieldDefinitions", "calculatedFieldDefinitions",
                "documentTemplates", "policyRules", "recordAccessPolicies", "recordAccessGrants",
                "userGroups", "opportunityTypes", "notificationTemplates", "savedFilters"}) {
            if (root.has(key) && root.get(key).isArray()) {
                sb.append("  ").append(key).append(": ").append(root.get(key).size()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void requireSystemAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean ok = auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
        if (!ok) throw new ForbiddenException("SYSTEM_ADMIN role required");
    }
}
