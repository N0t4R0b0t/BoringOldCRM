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
import com.bocrm.backend.exception.*;
import com.bocrm.backend.integration.IntegrationEventPublisher;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;
/**
 * OrderService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@CacheConfig(cacheNames = {"OrderDetail", "OrderTable"})
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomFieldDefinitionService fieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final PolicyValidationService policyValidationService;
    private final CalculationMessagePublisher calculationMessagePublisher;
    private final IntegrationEventPublisher integrationEventPublisher;

    public OrderService(OrderRepository orderRepository,
                       CustomFieldDefinitionService fieldDefinitionService,
                       CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                       AuditLogService auditLogService,
                       NotificationService notificationService,
                       ObjectMapper objectMapper,
                       PolicyValidationService policyValidationService,
                       CalculationMessagePublisher calculationMessagePublisher,
                       IntegrationEventPublisher integrationEventPublisher) {
        this.orderRepository = orderRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.policyValidationService = policyValidationService;
        this.calculationMessagePublisher = calculationMessagePublisher;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    @Transactional
    @CacheEvict(value = {"OrderDetail", "OrderTable"}, allEntries = true)
    public OrderDTO createOrder(CreateOrderRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        // Policy rules validation
        Map<String, Object> createCtx = new HashMap<>();
        createCtx.put("name", request.getName() != null ? request.getName() : "");
        createCtx.put("status", request.getStatus() != null ? request.getStatus() : "DRAFT");
        createCtx.put("currency", request.getCurrency() != null ? request.getCurrency() : "USD");
        if (request.getTotalAmount() != null) createCtx.put("totalAmount", request.getTotalAmount());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), createCtx);
        List<PolicyViolationDetail> createWarnings = policyValidationService.validate(tenantId, "Order", "CREATE", createCtx, null);

        JsonNode sanitizedCustomFields = null;
        if (request.getCustomFields() != null) {
            sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Order", request.getCustomFields());
        }

        Order order = Order.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .opportunityId(request.getOpportunityId())
                .name(request.getName())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .subtotal(request.getSubtotal())
                .taxAmount(request.getTaxAmount())
                .totalAmount(request.getTotalAmount())
                .orderDate(request.getOrderDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(request.getNotes())
                .ownerId(request.getOwnerId())
                .lineItems(request.getLineItems() != null ? request.getLineItems() : objectMapper.createArrayNode())
                .build();

        if (sanitizedCustomFields != null && !sanitizedCustomFields.isEmpty()) {
            order.setCustomData(sanitizedCustomFields);
            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Order", sanitizedCustomFields);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                order.setTableDataJsonb(tableDataNode);
            } else {
                order.setTableDataJsonb(objectMapper.createObjectNode());
            }
        } else {
            order.setTableDataJsonb(objectMapper.createObjectNode());
        }

        Order saved = orderRepository.save(order);
        auditLogService.logAction(TenantContext.getUserId(), "CREATE_ORDER", "Order", saved.getId(), request);
        enqueueCalculatedFieldRefresh(tenantId, saved.getId());

        if (saved.getOwnerId() != null && !saved.getOwnerId().equals(TenantContext.getUserId())) {
            notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(), TenantContext.getUserId(),
                    "System", "Order", saved.getId(), saved.getName());
        }

        try { integrationEventPublisher.publish(tenantId, "Order", saved.getId(), "CREATED", toDTO(saved, tenantId)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Order {}", saved.getId(), e); }

        OrderDTO dto = toDTO(saved, tenantId);
        if (!createWarnings.isEmpty()) dto.setPolicyWarnings(createWarnings);
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "OrderDetail", key = "#orderId")
    public OrderDTO getOrder(Long orderId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        return toDTO(order, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderDTO> listOrders(int page, int size, String sortBy, String sortOrder) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, sortBy, sortOrder,
                Set.of("name", "status", "orderDate", "createdAt", "updatedAt"));

        Specification<Order> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        Page<Order> result = orderRepository.findAll(spec, pageable);

        List<OrderDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<OrderDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderDTO> search(String searchTerm, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Pageable pageable = buildPageable(page, size, "createdAt", "desc",
                Set.of("name", "status", "createdAt", "updatedAt"));

        Specification<Order> spec = (root, query, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return tenantPred;
            }
            var searchPred = cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%");
            return cb.and(tenantPred, searchPred);
        };

        Page<Order> result = orderRepository.findAll(spec, pageable);
        List<OrderDTO> content = result.getContent().stream()
                .map(this::toDTOTableView)
                .collect(Collectors.toList());

        return PagedResponse.<OrderDTO>builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .hasNext(result.hasNext())
                .hasPrev(result.hasPrevious())
                .build();
    }

    @Transactional
    @CacheEvict(value = {"OrderDetail", "OrderTable"}, allEntries = true)
    public OrderDTO updateOrder(Long orderId, UpdateOrderRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Long previousOwnerId = order.getOwnerId();

        // Policy rules validation
        Map<String, Object> previousCtx = new HashMap<>();
        previousCtx.put("name", order.getName() != null ? order.getName() : "");
        previousCtx.put("status", order.getStatus());
        previousCtx.put("currency", order.getCurrency());
        if (order.getTotalAmount() != null) previousCtx.put("totalAmount", order.getTotalAmount());
        flattenCustomFields(order.getCustomData(), previousCtx);

        Map<String, Object> mergedCtx = new HashMap<>(previousCtx);
        if (request.getName() != null) mergedCtx.put("name", request.getName());
        if (request.getStatus() != null) mergedCtx.put("status", request.getStatus());
        if (request.getCurrency() != null) mergedCtx.put("currency", request.getCurrency());
        if (request.getTotalAmount() != null) mergedCtx.put("totalAmount", request.getTotalAmount());
        if (request.getCustomFields() != null) flattenCustomFields(request.getCustomFields(), mergedCtx);
        List<PolicyViolationDetail> updateWarnings = policyValidationService.validate(tenantId, "Order", "UPDATE", mergedCtx, previousCtx);

        if (request.getName() != null) order.setName(request.getName());
        if (request.getStatus() != null) order.setStatus(request.getStatus());
        if (request.getCurrency() != null) order.setCurrency(request.getCurrency());
        if (request.getSubtotal() != null) order.setSubtotal(request.getSubtotal());
        if (request.getTaxAmount() != null) order.setTaxAmount(request.getTaxAmount());
        if (request.getTotalAmount() != null) order.setTotalAmount(request.getTotalAmount());
        if (request.getOrderDate() != null) order.setOrderDate(request.getOrderDate());
        if (request.getExpectedDeliveryDate() != null) order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        if (request.getNotes() != null) order.setNotes(request.getNotes());
        if (request.getOwnerId() != null) order.setOwnerId(request.getOwnerId());
        if (request.getLineItems() != null) order.setLineItems(request.getLineItems());

        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            JsonNode sanitizedCustomFields = fieldDefinitionService.validateAndSanitizeCustomFields(tenantId, "Order", request.getCustomFields());

            ObjectNode mergedNode = objectMapper.createObjectNode();
            if (order.getCustomData() != null && order.getCustomData().isObject()) {
                mergedNode.setAll((ObjectNode) order.getCustomData());
            }
            if (sanitizedCustomFields.isObject()) {
                mergedNode.setAll((ObjectNode) sanitizedCustomFields);
            }

            order.setCustomData(mergedNode);

            JsonNode tableDataNode = fieldDefinitionService.extractTableData(tenantId, "Order", mergedNode);
            if (tableDataNode != null && tableDataNode.size() > 0) {
                order.setTableDataJsonb(tableDataNode);
            } else {
                order.setTableDataJsonb(objectMapper.createObjectNode());
            }
        }

        Order updated = orderRepository.save(order);
        auditLogService.logAction(TenantContext.getUserId(), "UPDATE_ORDER", "Order", orderId, request);
        enqueueCalculatedFieldRefresh(tenantId, updated.getId());

        if (updated.getOwnerId() != null) {
            if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(TenantContext.getUserId())) {
                notificationService.notifyOwnershipAssigned(tenantId, updated.getOwnerId(), TenantContext.getUserId(),
                        "System", "Order", updated.getId(), updated.getName());
            } else if (!TenantContext.getUserId().equals(updated.getOwnerId())) {
                notificationService.notifyRecordModified(tenantId, TenantContext.getUserId(), "System",
                        updated.getOwnerId(), "Order", updated.getId(), updated.getName());
            }
        }

        try { integrationEventPublisher.publish(tenantId, "Order", updated.getId(), "UPDATED", toDTO(updated, tenantId)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Order {}", updated.getId(), e); }

        OrderDTO dto = toDTO(updated, tenantId);
        if (!updateWarnings.isEmpty()) dto.setPolicyWarnings(updateWarnings);
        return dto;
    }

    @Transactional
    @CacheEvict(value = {"OrderDetail", "OrderTable"}, allEntries = true)
    public void deleteOrder(Long orderId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        Map<String, Object> deleteCtx = new HashMap<>();
        deleteCtx.put("name", order.getName() != null ? order.getName() : "");
        deleteCtx.put("status", order.getStatus());
        deleteCtx.put("currency", order.getCurrency());
        if (order.getTotalAmount() != null) deleteCtx.put("totalAmount", order.getTotalAmount());
        flattenCustomFields(order.getCustomData(), deleteCtx);
        policyValidationService.validate(tenantId, "Order", "DELETE", deleteCtx, null);

        try { integrationEventPublisher.publish(tenantId, "Order", orderId, "DELETED", toDTO(order, tenantId)); }
        catch (Exception e) { log.warn("Failed to enqueue integration event for Order {}", orderId, e); }

        orderRepository.delete(order);
        auditLogService.logAction(TenantContext.getUserId(), "DELETE_ORDER", "Order", orderId, null);
    }

    private void flattenCustomFields(JsonNode customData, Map<String, Object> ctx) {
        if (customData == null || !customData.isObject()) return;
        Map<String, Object> flat = objectMapper.convertValue(customData, Map.class);
        if (flat != null) ctx.putAll(flat);
    }

    private OrderDTO toDTO(Order order, Long tenantId) {
        return OrderDTO.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .customerId(order.getCustomerId())
                .opportunityId(order.getOpportunityId())
                .name(order.getName())
                .status(order.getStatus())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .notes(order.getNotes())
                .ownerId(order.getOwnerId())
                .lineItems(order.getLineItems())
                .customFields(order.getCustomData())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderDTO toDTOTableView(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .customerId(order.getCustomerId())
                .opportunityId(order.getOpportunityId())
                .name(order.getName())
                .status(order.getStatus())
                .currency(order.getCurrency())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .notes(order.getNotes())
                .ownerId(order.getOwnerId())
                .lineItems(order.getLineItems())
                .customFields(order.getTableDataJsonb())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortOrder, Set<String> allowedSorts) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        String safeSort = (sortBy != null && allowedSorts.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSort));
    }

    private void enqueueCalculatedFieldRefresh(Long tenantId, Long orderId) {
        calculationMessagePublisher.publish(tenantId, "Order", orderId);
    }
}
