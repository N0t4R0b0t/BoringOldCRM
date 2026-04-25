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
package com.bocrm.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
/**
 * TenantBackupJob.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Entity
@Table(name = "tenant_backup_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "type", nullable = false)
    private String type; // BACKUP or RESTORE

    @Column(name = "label")
    private String label;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "includes_data")
    @Builder.Default
    private Boolean includesData = false;

    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // for restore request (raw JSON)

    @Column(name = "result_payload", columnDefinition = "TEXT")
    private String resultPayload; // for backup result (raw JSON)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
