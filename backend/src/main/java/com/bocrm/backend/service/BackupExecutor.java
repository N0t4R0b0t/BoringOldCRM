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

import com.bocrm.backend.entity.TenantBackupJob;
import com.bocrm.backend.repository.TenantBackupJobRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
/**
 * BackupExecutor.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupExecutor {

    private final TenantBackupJobRepository repository;
    private final TenantBackupService backupService;

    @Async
    public void submit(Long jobId, Long tenantId) {
        try {
            log.info("Async submit job {} tenant={}", jobId, tenantId);
            TenantContext.setTenantId(tenantId);
            TenantBackupJob job = repository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job {} not found", jobId);
                return;
            }
            backupService.executeJob(job);
        } catch (Exception e) {
            log.error("Async job {} failed: {}", jobId, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
