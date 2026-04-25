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
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * BackupPoller.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class BackupPoller {

    private final TenantBackupJobRepository repository;
    private final TenantBackupService backupService;

    @Scheduled(fixedDelayString = "${app.backup.poll-delay-ms:15000}")
    public void scanPendingJobs() {
        List<TenantBackupJob> pending = repository.findByStatus("PENDING");
        for (TenantBackupJob job : pending) {
            try {
                log.info("Processing backup job {} for tenant {}", job.getId(), job.getTenantId());
                TenantContext.setTenantId(job.getTenantId());
                backupService.executeJob(job);
            } catch (Exception e) {
                log.error("Failed to process job {}: {}", job.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
