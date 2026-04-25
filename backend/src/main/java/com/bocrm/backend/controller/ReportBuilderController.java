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

import com.bocrm.backend.dto.ReportPreviewRequest;
import com.bocrm.backend.dto.ReportPreviewResponse;
import com.bocrm.backend.service.ReportBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * ReportBuilderController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/report-builder")
@Tag(name = "Report Builder", description = "Stateless preview generation for the Report Builder page")
@Slf4j
public class ReportBuilderController {

    private final ReportBuilderService reportBuilderService;

    public ReportBuilderController(ReportBuilderService reportBuilderService) {
        this.reportBuilderService = reportBuilderService;
    }

    @PostMapping("/preview")
    @Operation(summary = "Generate a report preview without saving to the database")
    public ResponseEntity<ReportPreviewResponse> preview(@RequestBody ReportPreviewRequest request) {
        return ResponseEntity.ok(reportBuilderService.preview(request));
    }
}
