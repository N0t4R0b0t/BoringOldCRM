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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.*;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.repository.OrderRepository;
import com.bocrm.backend.repository.InvoiceRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
/**
 * ReportingService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class ReportingService {
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final OpportunityTypeService opportunityTypeService;

    public ReportingService(CustomerRepository customerRepository,
                           OpportunityRepository opportunityRepository,
                           ContactRepository contactRepository,
                           ActivityRepository activityRepository,
                           OrderRepository orderRepository,
                           InvoiceRepository invoiceRepository,
                           OpportunityTypeService opportunityTypeService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.opportunityTypeService = opportunityTypeService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Customer> customers = customerRepository.findByTenantId(tenantId);
        List<Contact> contacts = contactRepository.findByTenantId(tenantId);
        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);
        List<Activity> activities = activityRepository.findByTenantId(tenantId);
        List<Order> orders = orderRepository.findByTenantId(tenantId);
        List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);

        double totalPipelineValue = opportunities.stream()
                .filter(o -> o.getValue() != null && "open".equalsIgnoreCase(o.getStage()))
                .mapToDouble(o -> o.getValue().doubleValue())
                .sum();

        long closedDeals = opportunities.stream()
                .filter(o -> "closed".equalsIgnoreCase(o.getStage()))
                .count();
        long totalDeals = opportunities.size();
        double winRate = totalDeals > 0 ? (double) closedDeals / totalDeals * 100 : 0;

        double totalOrderRevenue = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();

        double totalInvoiceRevenue = invoices.stream()
                .filter(i -> i.getTotalAmount() != null)
                .mapToDouble(i -> i.getTotalAmount().doubleValue())
                .sum();

        long paidInvoices = invoices.stream().filter(i -> "PAID".equalsIgnoreCase(i.getStatus())).count();

        List<ReportMetricsDTO> metrics = List.of(
                ReportMetricsDTO.builder()
                        .metric("Active Customers")
                        .value(customers.stream().filter(c -> "active".equalsIgnoreCase(c.getStatus())).count())
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Open Opportunities")
                        .value(opportunities.stream().filter(o -> "open".equalsIgnoreCase(o.getStage())).count())
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Win Rate")
                        .value(Double.parseDouble(String.format("%.1f", winRate)))
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Total Order Revenue")
                        .value(totalOrderRevenue)
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Paid Invoices")
                        .value(paidInvoices)
                        .build()
        );

        return DashboardSummaryDTO.builder()
                .totalCustomers(customers.size())
                .totalContacts(contacts.size())
                .totalOpportunities(opportunities.size())
                .totalPipelineValue(totalPipelineValue)
                .totalActivities(activities.size())
                .opportunityWinRate(winRate)
                .recentActivities(Math.min(5, activities.size()))
                .totalOrders(orders.size())
                .totalInvoices(invoices.size())
                .totalOrderRevenue(totalOrderRevenue)
                .totalInvoiceRevenue(totalInvoiceRevenue)
                .keyMetrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFinancialReport(LocalDate startDate, LocalDate endDate) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Order> orders = orderRepository.findByTenantId(tenantId);
        List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);

        if (startDate != null || endDate != null) {
            LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
            orders = orders.stream()
                    .filter(o -> (start == null || o.getCreatedAt().isAfter(start)) &&
                                 (end == null || o.getCreatedAt().isBefore(end)))
                    .collect(Collectors.toList());
            invoices = invoices.stream()
                    .filter(i -> (start == null || i.getCreatedAt().isAfter(start)) &&
                                 (end == null || i.getCreatedAt().isBefore(end)))
                    .collect(Collectors.toList());
        }

        // Orders breakdown by status
        Map<String, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus() != null ? o.getStatus() : "UNKNOWN", Collectors.counting()));
        double totalOrderRevenue = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(o -> o.getTotalAmount().doubleValue()).sum();

        // Invoices breakdown by status
        Map<String, Long> invoicesByStatus = invoices.stream()
                .collect(Collectors.groupingBy(i -> i.getStatus() != null ? i.getStatus() : "UNKNOWN", Collectors.counting()));
        double totalInvoiceRevenue = invoices.stream()
                .filter(i -> i.getTotalAmount() != null)
                .mapToDouble(i -> i.getTotalAmount().doubleValue()).sum();
        double paidInvoiceRevenue = invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()) && i.getTotalAmount() != null)
                .mapToDouble(i -> i.getTotalAmount().doubleValue()).sum();

        // Monthly revenue trend (orders)
        Map<String, Double> orderRevenueByMonth = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        o -> YearMonth.from(o.getCreatedAt()).toString(),
                        Collectors.summingDouble(o -> o.getTotalAmount().doubleValue())));

        // Monthly invoice trend
        Map<String, Double> invoiceRevenueByMonth = invoices.stream()
                .filter(i -> i.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        i -> YearMonth.from(i.getCreatedAt()).toString(),
                        Collectors.summingDouble(i -> i.getTotalAmount().doubleValue())));

        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", orders.size());
        result.put("totalOrderRevenue", totalOrderRevenue);
        result.put("ordersByStatus", ordersByStatus);
        result.put("orderRevenueByMonth", orderRevenueByMonth);
        result.put("totalInvoices", invoices.size());
        result.put("totalInvoiceRevenue", totalInvoiceRevenue);
        result.put("paidInvoiceRevenue", paidInvoiceRevenue);
        result.put("invoicesByStatus", invoicesByStatus);
        result.put("invoiceRevenueByMonth", invoiceRevenueByMonth);
        return result;
    }

    @Transactional(readOnly = true)
    public CustomersReportDTO getCustomersReport(ReportFilterDTO filter) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Customer> customers = customerRepository.findByTenantId(tenantId);

        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            LocalDateTime startDateTime = filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
            LocalDateTime endDateTime = filter.getEndDate() != null ? filter.getEndDate().atTime(23, 59, 59) : null;

            customers = customers.stream()
                    .filter(c -> (startDateTime == null || c.getCreatedAt().isAfter(startDateTime)) &&
                                (endDateTime == null || c.getCreatedAt().isBefore(endDateTime)))
                    .collect(Collectors.toList());
        }

        Map<String, List<Customer>> byStatus = customers.stream()
                .collect(Collectors.groupingBy(c -> c.getStatus() != null ? c.getStatus() : "unknown"));

        List<CustomersByStatusDTO> statusBreakdown = byStatus.entrySet().stream()
                .map(e -> CustomersByStatusDTO.builder()
                        .status(e.getKey())
                        .count(e.getValue().size())
                        .build())
                .collect(Collectors.toList());

        long activeCount = customers.stream().filter(c -> "active".equalsIgnoreCase(c.getStatus())).count();
        
        List<ReportMetricsDTO> metrics = List.of(
                ReportMetricsDTO.builder()
                        .metric("Total Customers")
                        .value(customers.size())
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Active Rate")
                        .value(Double.parseDouble(String.format("%.1f", customers.size() > 0 ? (double) activeCount / customers.size() * 100 : 0)))
                        .build()
        );

        return CustomersReportDTO.builder()
                .totalCustomers(customers.size())
                .activeCustomers(activeCount)
                .inactiveCustomers(customers.size() - activeCount)
                .averageLifetimeValue(0.0) // Would need more data to calculate
                .byStatus(statusBreakdown)
                .metrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public OpportunitiesReportDTO getOpportunitiesReport(ReportFilterDTO filter) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            LocalDateTime startDateTime = filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
            LocalDateTime endDateTime = filter.getEndDate() != null ? filter.getEndDate().atTime(23, 59, 59) : null;

            opportunities = opportunities.stream()
                    .filter(o -> (startDateTime == null || o.getCreatedAt().isAfter(startDateTime)) &&
                                (endDateTime == null || o.getCreatedAt().isBefore(endDateTime)))
                    .collect(Collectors.toList());
        }

        if (filter.getTypeSlug() != null && !filter.getTypeSlug().isBlank()) {
            String slug = filter.getTypeSlug();
            opportunities = opportunities.stream()
                    .filter(o -> slug.equals(o.getOpportunityTypeSlug()))
                    .collect(Collectors.toList());
        }

        double totalValue = opportunities.stream()
                .filter(o -> o.getValue() != null)
                .mapToDouble(o -> o.getValue().doubleValue())
                .sum();

        long openCount = opportunities.stream().filter(o -> "open".equalsIgnoreCase(o.getStage())).count();
        long closedCount = opportunities.stream().filter(o -> "closed".equalsIgnoreCase(o.getStage())).count();
        double winRate = opportunities.size() > 0 ? (double) closedCount / opportunities.size() * 100 : 0;
        double avgDealSize = opportunities.size() > 0 ? totalValue / opportunities.size() : 0;

        Map<String, List<Opportunity>> byStage = opportunities.stream()
                .collect(Collectors.groupingBy(o -> o.getStage() != null ? o.getStage() : "unknown"));

        List<OpportunitiesByStageDTO> stageBreakdown = byStage.entrySet().stream()
                .map(e -> OpportunitiesByStageDTO.builder()
                        .stage(e.getKey())
                        .count(e.getValue().size())
                        .value(e.getValue().stream()
                                .filter(o -> o.getValue() != null)
                                .mapToDouble(o -> o.getValue().doubleValue())
                                .sum())
                        .build())
                .collect(Collectors.toList());

        // byType breakdown — resolve slug to display name
        List<OpportunityTypeDTO> allTypes = opportunityTypeService.getAll();
        Map<String, String> slugToName = allTypes.stream()
                .collect(Collectors.toMap(OpportunityTypeDTO::getSlug, OpportunityTypeDTO::getName));
        Map<String, List<Opportunity>> byTypeMap = opportunities.stream()
                .filter(o -> o.getOpportunityTypeSlug() != null && !o.getOpportunityTypeSlug().isBlank())
                .collect(Collectors.groupingBy(Opportunity::getOpportunityTypeSlug));
        List<OpportunitiesByTypeDTO> typeBreakdown = byTypeMap.entrySet().stream()
                .map(e -> {
                    String slug = e.getKey();
                    List<Opportunity> typeOpps = e.getValue();
                    double typeValue = typeOpps.stream().filter(o -> o.getValue() != null)
                            .mapToDouble(o -> o.getValue().doubleValue()).sum();
                    long typeWon = typeOpps.stream().filter(o -> "closed_won".equalsIgnoreCase(o.getStage())).count();
                    long typeLost = typeOpps.stream().filter(o -> "closed_lost".equalsIgnoreCase(o.getStage())).count();
                    double typeWinRate = (typeWon + typeLost) > 0 ? (double) typeWon / (typeWon + typeLost) * 100 : 0;
                    return OpportunitiesByTypeDTO.builder()
                            .typeSlug(slug)
                            .typeName(slugToName.getOrDefault(slug, slug))
                            .count(typeOpps.size())
                            .value(typeValue)
                            .winRate(typeWinRate)
                            .build();
                })
                .sorted(Comparator.comparingDouble(OpportunitiesByTypeDTO::getValue).reversed())
                .collect(Collectors.toList());

        // Monthly trend
        Map<String, List<Opportunity>> byMonth = opportunities.stream()
                .collect(Collectors.groupingBy(o -> 
                        YearMonth.from(o.getCreatedAt()).toString()));

        List<MonthlyTrendDTO> monthlyTrend = byMonth.entrySet().stream()
                .map(e -> MonthlyTrendDTO.builder()
                        .month(e.getKey())
                        .count(e.getValue().size())
                        .value(e.getValue().stream()
                                .filter(o -> o.getValue() != null)
                                .mapToDouble(o -> o.getValue().doubleValue())
                                .sum())
                        .build())
                .sorted(Comparator.comparing(MonthlyTrendDTO::getMonth))
                .collect(Collectors.toList());

        List<ReportMetricsDTO> metrics = List.of(
                ReportMetricsDTO.builder()
                        .metric("Total Pipeline")
                        .value(totalValue)
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Win Rate")
                        .value(Double.parseDouble(String.format("%.1f", winRate)))
                        .build(),
                ReportMetricsDTO.builder()
                        .metric("Avg Deal Size")
                        .value(avgDealSize)
                        .build()
        );

        return OpportunitiesReportDTO.builder()
                .totalOpportunities(opportunities.size())
                .totalPipelineValue(totalValue)
                .averageDealSize(avgDealSize)
                .openDeals(openCount)
                .closedDeals(closedCount)
                .winRate(winRate)
                .byStage(stageBreakdown)
                .byType(typeBreakdown)
                .monthlyTrend(monthlyTrend)
                .metrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesOverview(LocalDate startDate, LocalDate endDate) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        if (startDate != null || endDate != null) {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

            opportunities = opportunities.stream()
                    .filter(o -> (startDateTime == null || o.getCreatedAt().isAfter(startDateTime)) &&
                            (endDateTime == null || o.getCreatedAt().isBefore(endDateTime)))
                    .collect(Collectors.toList());
        }

        double totalRevenue = opportunities.stream()
                .filter(o -> o.getValue() != null && "closed-won".equalsIgnoreCase(o.getStage()))
                .mapToDouble(o -> o.getValue().doubleValue())
                .sum();

        double totalPipeline = opportunities.stream()
                .filter(o -> o.getValue() != null)
                .mapToDouble(o -> o.getValue().doubleValue())
                .sum();

        long totalDeals = opportunities.size();
        long wonDeals = opportunities.stream().filter(o -> "closed-won".equalsIgnoreCase(o.getStage())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("totalPipeline", totalPipeline);
        result.put("totalDeals", totalDeals);
        result.put("wonDeals", wonDeals);
        result.put("winRate", totalDeals > 0 ? (double) wonDeals / totalDeals * 100 : 0);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("revenue", totalRevenue);
        metrics.put("pipeline", totalPipeline);
        metrics.put("deals", totalDeals);
        result.put("metrics", metrics);

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesByStage() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        Map<String, Long> byStage = opportunities.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStage() != null ? o.getStage() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("byStage", byStage);
        result.put("total", opportunities.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesPipeline() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        Map<String, Double> pipelineByStage = opportunities.stream()
                .filter(o -> o.getValue() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getStage() != null ? o.getStage() : "unknown",
                        Collectors.summingDouble(o -> o.getValue().doubleValue())
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("pipeline", pipelineByStage);
        result.put("total", pipelineByStage.values().stream().mapToDouble(Double::doubleValue).sum());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConversionRate() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        long total = opportunities.size();
        long won = opportunities.stream().filter(o -> "closed-won".equalsIgnoreCase(o.getStage())).count();
        long lost = opportunities.stream().filter(o -> "closed-lost".equalsIgnoreCase(o.getStage())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("won", won);
        result.put("lost", lost);
        result.put("conversionRate", total > 0 ? (double) won / total * 100 : 0);
        result.put("lossRate", total > 0 ? (double) lost / total * 100 : 0);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActivitiesByType() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Activity> activities = activityRepository.findByTenantId(tenantId);

        Map<String, Long> byType = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getType() != null ? a.getType() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("byType", byType);
        result.put("total", activities.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActivitiesByStatus() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Activity> activities = activityRepository.findByTenantId(tenantId);

        Map<String, Long> byStatus = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("byStatus", byStatus);
        result.put("total", activities.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomersByStatus() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Customer> customers = customerRepository.findByTenantId(tenantId);

        Map<String, Long> byStatus = customers.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatus() != null ? c.getStatus() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("byStatus", byStatus);
        result.put("total", customers.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOpportunitiesList() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        List<Opportunity> opportunities = opportunityRepository.findByTenantId(tenantId);

        List<Map<String, Object>> list = opportunities.stream()
                .map(o -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", o.getId());
                    item.put("name", o.getName());
                    item.put("stage", o.getStage());
                    item.put("value", o.getValue());
                    item.put("probability", o.getProbability());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("opportunities", list);
        result.put("total", opportunities.size());
        return result;
    }
}
