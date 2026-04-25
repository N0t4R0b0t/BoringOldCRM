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

import com.bocrm.backend.dto.CreateBackupRequest;
import com.bocrm.backend.dto.CreateRestoreRequest;
import com.bocrm.backend.dto.TenantBackupJobDTO;
import com.bocrm.backend.entity.TenantBackupJob;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.service.TenantBackupService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
/**
 * TenantBackupController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/admin/backup")
@Tag(name = "Tenant Backup")
@RequiredArgsConstructor
public class TenantBackupController {

    private final TenantBackupService backupService;
    private final com.bocrm.backend.service.BackupExecutor backupExecutor;
    private final TenantRepository tenantRepository;

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @PostMapping("/create-backup")
    @Operation(summary = "Create a backup job (settings or settings+data)")
    public ResponseEntity<TenantBackupJobDTO> createBackup(@RequestBody CreateBackupRequest req) {
        Long tenantId = TenantContext.getTenantId();
        TenantBackupJob job = backupService.createBackupJob(req.isIncludesData(), tenantId);
        // submit for immediate async processing (also poller will pick it up)
        backupExecutor.submit(job.getId(), tenantId);
        return ResponseEntity.created(URI.create("/admin/backup/jobs/" + job.getId())).body(backupService.getJob(job.getId()).orElse(null));
    }

    @PostMapping("/create-restore")
    @Operation(summary = "Create a restore job from uploaded payload")
    public ResponseEntity<TenantBackupJobDTO> createRestore(@RequestBody CreateRestoreRequest req) {
        Long tenantId = TenantContext.getTenantId();
        TenantBackupJob job = backupService.createRestoreJob(req.getPayload(), tenantId);
        // submit for immediate async processing
        backupExecutor.submit(job.getId(), tenantId);
        return ResponseEntity.created(URI.create("/admin/backup/jobs/" + job.getId())).body(backupService.getJob(job.getId()).orElse(null));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List backup jobs for current tenant")
    public ResponseEntity<List<TenantBackupJobDTO>> listJobs() {
        Long tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(backupService.listJobs(tenantId));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get job details")
    public ResponseEntity<TenantBackupJobDTO> getJob(@PathVariable Long id) {
        return backupService.getJob(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs/{id}/download")
    @Operation(summary = "Download backup result payload as JSON")
    public ResponseEntity<String> download(@PathVariable Long id) {
        String payload = backupService.downloadBackup(id).orElse(null);
        if (payload == null) return ResponseEntity.notFound().build();
        String filename = backupService.getJob(id)
                .map(job -> buildFilename(job.getTenantId(), job.getCompletedAt()))
                .orElse("backup_" + id + ".json");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
    }

    private String buildFilename(Long tenantId, java.time.LocalDateTime completedAt) {
        String tenantName = tenantRepository.findById(tenantId)
                .map(t -> t.getName().replaceAll("[^a-zA-Z0-9_-]", "_"))
                .orElse("tenant");
        String timestamp = (completedAt != null ? completedAt : java.time.LocalDateTime.now())
                .format(TIMESTAMP_FMT);
        return tenantId + "_" + tenantName + "_backup_" + timestamp + ".json";
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete a backup job")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        backupService.deleteJob(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
