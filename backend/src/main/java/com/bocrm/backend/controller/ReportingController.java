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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
/**
 * ReportingController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Business analytics and reporting")
@Slf4j
public class ReportingController {
    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary with key metrics")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(reportingService.getDashboardSummary());
    }

    @PostMapping("/customers")
    @Operation(summary = "Get customers report with optional filters")
    public ResponseEntity<CustomersReportDTO> getCustomersReport(@RequestBody(required = false) ReportFilterDTO filter) {
        if (filter == null) {
            filter = ReportFilterDTO.builder().build();
        }
        return ResponseEntity.ok(reportingService.getCustomersReport(filter));
    }

    @PostMapping("/opportunities")
    @Operation(summary = "Get opportunities report with optional filters")
    public ResponseEntity<OpportunitiesReportDTO> getOpportunitiesReport(@RequestBody(required = false) ReportFilterDTO filter) {
        if (filter == null) {
            filter = ReportFilterDTO.builder().build();
        }
        return ResponseEntity.ok(reportingService.getOpportunitiesReport(filter));
    }

    @GetMapping("/sales/overview")
    @Operation(summary = "Get sales overview report")
    public ResponseEntity<Map<String, Object>> getSalesOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String format
    ) {
        return ResponseEntity.ok(reportingService.getSalesOverview(startDate, endDate));
    }

    @GetMapping("/sales/by-stage")
    @Operation(summary = "Get sales report by stage")
    public ResponseEntity<Map<String, Object>> getSalesByStage() {
        return ResponseEntity.ok(reportingService.getSalesByStage());
    }

    @GetMapping("/sales/pipeline")
    @Operation(summary = "Get sales pipeline report")
    public ResponseEntity<Map<String, Object>> getSalesPipeline() {
        return ResponseEntity.ok(reportingService.getSalesPipeline());
    }

    @GetMapping("/sales/conversion")
    @Operation(summary = "Get conversion rate report")
    public ResponseEntity<Map<String, Object>> getConversionRate() {
        return ResponseEntity.ok(reportingService.getConversionRate());
    }

    @GetMapping("/activities/by-type")
    @Operation(summary = "Get activities report by type")
    public ResponseEntity<Map<String, Object>> getActivitiesByType() {
        return ResponseEntity.ok(reportingService.getActivitiesByType());
    }

    @GetMapping("/activities/by-status")
    @Operation(summary = "Get activities report by status")
    public ResponseEntity<Map<String, Object>> getActivitiesByStatus() {
        return ResponseEntity.ok(reportingService.getActivitiesByStatus());
    }

    @GetMapping("/customers/by-status")
    @Operation(summary = "Get customers report by status")
    public ResponseEntity<Map<String, Object>> getCustomersByStatus() {
        return ResponseEntity.ok(reportingService.getCustomersByStatus());
    }

    @GetMapping("/opportunities/list")
    @Operation(summary = "Get opportunities list for export")
    public ResponseEntity<Map<String, Object>> getOpportunitiesList(
            @RequestParam(required = false) String format
    ) {
        return ResponseEntity.ok(reportingService.getOpportunitiesList());
    }

    @GetMapping("/financial")
    @Operation(summary = "Get financial report for orders and invoices")
    public ResponseEntity<Map<String, Object>> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportingService.getFinancialReport(startDate, endDate));
    }
}
