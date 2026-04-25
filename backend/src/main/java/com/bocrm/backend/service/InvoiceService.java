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

import com.bocrm.backend.dto.CreateInvoiceRequest;
import com.bocrm.backend.dto.InvoiceDTO;
import com.bocrm.backend.dto.PagedResponse;
import com.bocrm.backend.dto.PolicyViolationDetail;
import com.bocrm.backend.dto.UpdateInvoiceRequest;
import com.bocrm.backend.entity.Invoice;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.integration.IntegrationEventPublisher;
import com.bocrm.backend.repository.InvoiceRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * InvoiceService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"InvoiceDetail", "InvoiceTable"})
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final PolicyValidationService policyValidationService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Transactional
    @CacheEvict(value = {"InvoiceDetail", "InvoiceTable"}, allEntries = true)
    public InvoiceDTO createInvoice(CreateInvoiceRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("status", request.getStatus() != null ? request.getStatus() : "DRAFT");
        createCtx.put("currency", request.getCurrency() != null ? request.getCurrency() : "USD");
        if (request.getTotalAmount() != null) createCtx.put("totalAmount", request.getTotalAmount());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Invoice", "CREATE", createCtx, null);

        // Initialize JSONB fields
        JsonNode lineItems = request.getLineItems() != null ? request.getLineItems() : objectMapper.createArrayNode();
        JsonNode customData = request.getCustomFields() != null ? request.getCustomFields() : objectMapper.createObjectNode();

        // Validate and sanitize custom fields
        customData = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Invoice", customData);

        // Extract table data for list view
        JsonNode tableData = fieldDefinitionService.extractTableData(tenantId, "Invoice", customData);

        // Build and save invoice
        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .orderId(request.getOrderId())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .subtotal(request.getSubtotal())
                .taxAmount(request.getTaxAmount())
                .totalAmount(request.getTotalAmount())
                .dueDate(request.getDueDate())
                .paymentTerms(request.getPaymentTerms())
                .notes(request.getNotes())
                .ownerId(request.getOwnerId())
                .lineItems(lineItems)
                .customData(customData)
                .tableDataJsonb(tableData)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        // Audit log
        auditLogService.logAction(
                TenantContext.getUserId(),
                "CREATE_INVOICE",
                "Invoice",
                saved.getId(),
                request
        );

        // Notify owner if assigned
        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "Invoice", saved.getId(), "Invoice #" + saved.getId());
        }

        // Enqueue calculated field refresh
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        try { integrationEventPublisher.publish(tenantId, "Invoice", saved.getId(), "CREATED", toDTO(saved)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Invoice {}", saved.getId(), e); }

        InvoiceDTO dto = toDTO(saved);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Cacheable(value = "InvoiceDetail", key = "#invoiceId")
    public InvoiceDTO getInvoice(Long invoiceId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(invoice);
    }

    public PagedResponse<InvoiceDTO> listInvoices(int page, int size, String sortBy, String sortOrder) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("status", "currency", "totalAmount", "dueDate", "paymentTerms", "createdAt", "updatedAt"));
        Page<Invoice> invoices = invoiceRepository.findAll(
                Specification.where((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId)),
                pageable
        );

        return PagedResponse.<InvoiceDTO>builder()
                .content(invoices.getContent().stream().map(this::toDTOTableView).collect(Collectors.toList()))
                .totalElements(invoices.getTotalElements())
                .totalPages(invoices.getTotalPages())
                .currentPage(invoices.getNumber())
                .pageSize(invoices.getSize())
                .hasNext(invoices.hasNext())
                .hasPrev(invoices.hasPrevious())
                .build();
    }

    public PagedResponse<InvoiceDTO> search(String term, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending());

        Specification<Invoice> spec = (root, query, cb) -> {
            var tenantPredicate = cb.equal(root.get("tenantId"), tenantId);
            if (term == null || term.isEmpty()) {
                return tenantPredicate;
            }
            var searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("status")), "%" + term.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("paymentTerms")), "%" + term.toLowerCase() + "%")
            );
            return cb.and(tenantPredicate, searchPredicate);
        };

        Page<Invoice> results = invoiceRepository.findAll(spec, pageable);
        return PagedResponse.<InvoiceDTO>builder()
                .content(results.getContent().stream().map(this::toDTOTableView).collect(Collectors.toList()))
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .currentPage(results.getNumber())
                .pageSize(results.getSize())
                .hasNext(results.hasNext())
                .hasPrev(results.hasPrevious())
                .build();
    }

    @Transactional
    @CacheEvict(value = {"InvoiceDetail", "InvoiceTable"}, allEntries = true)
    public InvoiceDTO updateInvoice(Long invoiceId, UpdateInvoiceRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long previousOwnerId = invoice.getOwnerId();

        // Policy rules validation
        Map<String, Object> previousCtx = new HashMap<>();
        previousCtx.put("status", invoice.getStatus());
        previousCtx.put("currency", invoice.getCurrency());
        if (invoice.getTotalAmount() != null) previousCtx.put("totalAmount", invoice.getTotalAmount());
        flattenCustomFields(invoice.getCustomData(), previousCtx);

        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getStatus() != null) mergedCtx.put("status", request.getStatus());
        if (request.getCurrency() != null) mergedCtx.put("currency", request.getCurrency());
        if (request.getTotalAmount() != null) mergedCtx.put("totalAmount", request.getTotalAmount());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), mergedCtx);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Invoice", "UPDATE", mergedCtx, previousCtx);

        // Update fields
        if (request.getStatus() != null) invoice.setStatus(request.getStatus());
        if (request.getCurrency() != null) invoice.setCurrency(request.getCurrency());
        if (request.getSubtotal() != null) invoice.setSubtotal(request.getSubtotal());
        if (request.getTaxAmount() != null) invoice.setTaxAmount(request.getTaxAmount());
        if (request.getTotalAmount() != null) invoice.setTotalAmount(request.getTotalAmount());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        if (request.getPaymentTerms() != null) invoice.setPaymentTerms(request.getPaymentTerms());
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());
        if (request.getOwnerId() != null) invoice.setOwnerId(request.getOwnerId());
        if (request.getLineItems() != null) invoice.setLineItems(request.getLineItems());

        // Merge custom fields
        if (request.getCustomFields() != null) {
            JsonNode existingCustomData = invoice.getCustomData() != null ?
                    invoice.getCustomData() : objectMapper.createObjectNode();
            ObjectNode mergedNode = objectMapper.createObjectNode();
            mergedNode.setAll((ObjectNode) existingCustomData);

            JsonNode sanitizedNew = fieldDefinitionService.validateAndSanitizeCustomFields(
                    tenantId, "Invoice", request.getCustomFields()
            );
            mergedNode.setAll((ObjectNode) sanitizedNew);

            invoice.setCustomData(mergedNode);
        }

        // Extract denormalized table data
        JsonNode tableData = fieldDefinitionService.extractTableData(tenantId, "Invoice", invoice.getCustomData());
        invoice.setTableDataJsonb(tableData);

        Invoice updated = invoiceRepository.save(invoice);

        // Audit log
        auditLogService.logAction(
                TenantContext.getUserId(),
                "UPDATE_INVOICE",
                "Invoice",
                updated.getId(),
                request
        );

        // Notify if owner changed
        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "Invoice", updated.getId(), "Invoice #" + updated.getId());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "Invoice", updated.getId(), "Invoice #" + updated.getId());
            }
        }

        // Enqueue calculated field refresh
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        try { integrationEventPublisher.publish(tenantId, "Invoice", updated.getId(), "UPDATED", toDTO(updated)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Invoice {}", updated.getId(), e); }

        InvoiceDTO dto = toDTO(updated);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    @Transactional
    @CacheEvict(value = {"InvoiceDetail", "InvoiceTable"}, allEntries = true)
    public void deleteInvoice(Long invoiceId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Map<String, Object> deleteCtx = new HashMap<>();
        deleteCtx.put("status", invoice.getStatus());
        deleteCtx.put("currency", invoice.getCurrency());
        if (invoice.getTotalAmount() != null) deleteCtx.put("totalAmount", invoice.getTotalAmount());
        flattenCustomFields(invoice.getCustomData(), deleteCtx);
        policyValidationService.validate(tenantId, "Invoice", "DELETE", deleteCtx, null);

        try { integrationEventPublisher.publish(tenantId, "Invoice", invoiceId, "DELETED", toDTO(invoice)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Invoice {}", invoiceId, e); }

        invoiceRepository.delete(invoice);

        // Audit log
        auditLogService.logAction(
                TenantContext.getUserId(),
                "DELETE_INVOICE",
                "Invoice",
                invoiceId,
                null
        );
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .customerId(invoice.getCustomerId())
                .orderId(invoice.getOrderId())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .dueDate(invoice.getDueDate())
                .paymentTerms(invoice.getPaymentTerms())
                .notes(invoice.getNotes())
                .ownerId(invoice.getOwnerId())
                .lineItems(invoice.getLineItems())
                .customFields(invoice.getCustomData())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceDTO toDTOTableView(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .customerId(invoice.getCustomerId())
                .orderId(invoice.getOrderId())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .dueDate(invoice.getDueDate())
                .paymentTerms(invoice.getPaymentTerms())
                .ownerId(invoice.getOwnerId())
                .customFields(invoice.getTableDataJsonb())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private void flattenCustomFields(JsonNode customData, Map<String, Object> ctx) {
        if (customData == null || !customData.isObject()) return;
        Map<String, Object> flat = objectMapper.convertValue(customData, Map.class);
        if (flat != null) ctx.putAll(flat);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortOrder, Set<String> allowedSorts) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
    }

    private void enqueueCalculatedFieldRefresh(Long tenantId, Long invoiceId) {
        try {
            calculationMessagePublisher.publish(tenantId, "Invoice", invoiceId);
        } catch (Exception e) {
            log.error("Failed to publish calculation refresh for invoice {}", invoiceId, e);
        }
    }
}
