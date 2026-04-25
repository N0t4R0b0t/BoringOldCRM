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
import com.bocrm.backend.entity.Activity;
import com.bocrm.backend.entity.CustomRecord;
import com.bocrm.backend.entity.Contact;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.entity.Opportunity;
import com.bocrm.backend.entity.TenantDocument;
import com.bocrm.backend.repository.ActivityRepository;
import com.bocrm.backend.repository.CustomRecordRepository;
import com.bocrm.backend.repository.ContactRepository;
import com.bocrm.backend.repository.CustomerRepository;
import com.bocrm.backend.repository.OpportunityRepository;
import com.bocrm.backend.repository.TenantDocumentRepository;

import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * BulkOperationsService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class BulkOperationsService {
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final CustomRecordRepository customRecordRepository;
    private final TenantDocumentRepository tenantDocumentRepository;
    private final ObjectMapper objectMapper;
    private final CustomerService customerService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final ActivityService activityService;
    private final CustomRecordService customRecordService;

    public BulkOperationsService(CustomerRepository customerRepository,
                                  OpportunityRepository opportunityRepository,
                                  ContactRepository contactRepository,
                                  ActivityRepository activityRepository,
                                  CustomRecordRepository customRecordRepository,
                                  TenantDocumentRepository tenantDocumentRepository,
                                  ObjectMapper objectMapper,
                                  CustomerService customerService,
                                  ContactService contactService,
                                  OpportunityService opportunityService,
                                  ActivityService activityService,
                                  CustomRecordService customRecordService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
        this.customRecordRepository = customRecordRepository;
        this.tenantDocumentRepository = tenantDocumentRepository;
        this.objectMapper = objectMapper;
        this.customerService = customerService;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.activityService = activityService;
        this.customRecordService = customRecordService;
    }

    @Transactional
    public BulkOperationResponse bulkUpdateCustomers(BulkUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Customer customer = customerRepository.findByIdAndTenantId(id, tenantId);
                if (customer == null) {
                    throw new RuntimeException("Customer not found: " + id);
                }

                applyUpdates(customer, request.getUpdates());
                customerRepository.save(customer);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkUpdateOpportunities(BulkUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Opportunity opp = opportunityRepository.findByIdAndTenantId(id, tenantId);
                if (opp == null) {
                    throw new RuntimeException("Opportunity not found: " + id);
                }

                applyUpdates(opp, request.getUpdates());
                opportunityRepository.save(opp);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkUpdateContacts(BulkUpdateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Contact contact = contactRepository.findByIdAndTenantId(id, tenantId);
                if (contact == null) {
                    throw new RuntimeException("Contact not found: " + id);
                }

                applyUpdates(contact, request.getUpdates());
                contactRepository.save(contact);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteActivities(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Activity activity = activityRepository.findByIdAndTenantId(id, tenantId);
                if (activity == null) {
                    throw new RuntimeException("Activity not found: " + id);
                }

                activityRepository.delete(activity);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteCustomers(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Customer customer = customerRepository.findByIdAndTenantId(id, tenantId);
                if (customer == null) {
                    throw new RuntimeException("Customer not found: " + id);
                }

                customerRepository.delete(customer);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteContacts(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Contact contact = contactRepository.findByIdAndTenantId(id, tenantId);
                if (contact == null) throw new RuntimeException("Contact not found: " + id);
                contactRepository.delete(contact);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteOpportunities(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                Opportunity opp = opportunityRepository.findByIdAndTenantId(id, tenantId);
                if (opp == null) throw new RuntimeException("Opportunity not found: " + id);
                opportunityRepository.delete(opp);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteCustomRecords(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                CustomRecord customRecord = customRecordRepository.findByIdAndTenantId(id, tenantId);
                if (customRecord == null) throw new RuntimeException("CustomRecord not found: " + id);
                customRecordRepository.delete(customRecord);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkDeleteDocuments(BulkDeleteRequest request) {
        Long tenantId = TenantContext.getTenantId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : request.getIds()) {
            try {
                TenantDocument doc = tenantDocumentRepository.findByIdAndTenantId(id, tenantId);
                if (doc == null) throw new RuntimeException("Document not found: " + id);
                tenantDocumentRepository.delete(doc);
                successCount++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResponse.builder()
                .totalRequested(request.getIds().size())
                .successCount(successCount)
                .failureCount(request.getIds().size() - successCount)
                .status(successCount == request.getIds().size() ? "COMPLETED" : "PARTIAL")
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkCreateCustomers(List<CreateCustomerRequest> records) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        List<Long> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (CreateCustomerRequest req : records) {
            try {
                CustomerDTO dto = customerService.createCustomer(req);
                createdIds.add(dto.getId());
            } catch (Exception e) {
                errors.add((req.getName() != null ? req.getName() : "?") + ": " + e.getMessage());
            }
        }
        return BulkOperationResponse.builder()
                .totalRequested(records.size())
                .successCount(createdIds.size())
                .failureCount(errors.size())
                .status(errors.isEmpty() ? "COMPLETED" : "PARTIAL")
                .createdIds(createdIds)
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkCreateContacts(List<CreateContactRequest> records) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        List<Long> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (CreateContactRequest req : records) {
            try {
                ContactDTO dto = contactService.createContact(req);
                createdIds.add(dto.getId());
            } catch (Exception e) {
                errors.add((req.getName() != null ? req.getName() : "?") + ": " + e.getMessage());
            }
        }
        return BulkOperationResponse.builder()
                .totalRequested(records.size())
                .successCount(createdIds.size())
                .failureCount(errors.size())
                .status(errors.isEmpty() ? "COMPLETED" : "PARTIAL")
                .createdIds(createdIds)
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkCreateOpportunities(List<CreateOpportunityRequest> records) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        List<Long> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (CreateOpportunityRequest req : records) {
            try {
                OpportunityDTO dto = opportunityService.createOpportunity(req);
                createdIds.add(dto.getId());
            } catch (Exception e) {
                errors.add((req.getName() != null ? req.getName() : "?") + ": " + e.getMessage());
            }
        }
        return BulkOperationResponse.builder()
                .totalRequested(records.size())
                .successCount(createdIds.size())
                .failureCount(errors.size())
                .status(errors.isEmpty() ? "COMPLETED" : "PARTIAL")
                .createdIds(createdIds)
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkCreateActivities(List<CreateActivityRequest> records) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        List<Long> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (CreateActivityRequest req : records) {
            try {
                ActivityDTO dto = activityService.createActivity(req);
                createdIds.add(dto.getId());
            } catch (Exception e) {
                errors.add((req.getSubject() != null ? req.getSubject() : "?") + ": " + e.getMessage());
            }
        }
        return BulkOperationResponse.builder()
                .totalRequested(records.size())
                .successCount(createdIds.size())
                .failureCount(errors.size())
                .status(errors.isEmpty() ? "COMPLETED" : "PARTIAL")
                .createdIds(createdIds)
                .errorMessages(errors)
                .build();
    }

    @Transactional
    public BulkOperationResponse bulkCreateCustomRecords(List<CreateCustomRecordRequest> records) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        List<Long> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (CreateCustomRecordRequest req : records) {
            try {
                CustomRecordDTO dto = customRecordService.createCustomRecord(req);
                createdIds.add(dto.getId());
            } catch (Exception e) {
                errors.add((req.getName() != null ? req.getName() : "?") + ": " + e.getMessage());
            }
        }
        return BulkOperationResponse.builder()
                .totalRequested(records.size())
                .successCount(createdIds.size())
                .failureCount(errors.size())
                .status(errors.isEmpty() ? "COMPLETED" : "PARTIAL")
                .createdIds(createdIds)
                .errorMessages(errors)
                .build();
    }

    private void applyUpdates(Object entity, JsonNode updates) {
        Map<String, Object> map = objectMapper.convertValue(updates, Map.class);
        for (Map.Entry<String, Object> f : map.entrySet()) {
            applyFieldUpdate(entity, f.getKey(), f.getValue());
        }
    }

    private void applyFieldUpdate(Object target, String fieldName, Object value) {
        try {
            if (target instanceof Customer customer) {
                applyCustomerUpdate(customer, fieldName, value);
            } else if (target instanceof Opportunity opportunity) {
                applyOpportunityUpdate(opportunity, fieldName, value);
            } else if (target instanceof Contact contact) {
                applyContactUpdate(contact, fieldName, value);
            }
        } catch (Exception e) {
            log.warn("Failed to update field {}: {}", fieldName, e.getMessage());
        }
    }

    private void applyCustomerUpdate(Customer customer, String fieldName, Object value) {
        switch (fieldName) {
            case "name" -> customer.setName(value.toString());
            case "status" -> customer.setStatus(value.toString());
            case "industry" -> customer.setIndustry(value.toString());
            case "website" -> customer.setWebsite(value.toString());
            case "notes" -> customer.setNotes(value.toString());
        }
    }

    private void applyOpportunityUpdate(Opportunity opportunity, String fieldName, Object value) {
        switch (fieldName) {
            case "name" -> opportunity.setName(value.toString());
            case "stage" -> opportunity.setStage(value.toString());
            case "status" -> opportunity.setStatus(value.toString());
            case "value" -> opportunity.setValue(value == null ? null : BigDecimal.valueOf(Double.parseDouble(value.toString())));
            case "probability" -> opportunity.setProbability(value == null ? null : BigDecimal.valueOf(Integer.parseInt(value.toString())));
            case "expectedCloseDate" -> opportunity.setExpectedCloseDate(value == null ? null : LocalDateTime.parse(value.toString()));
            case "notes" -> opportunity.setNotes(value.toString());
        }
    }

    private void applyContactUpdate(Contact contact, String fieldName, Object value) {
        switch (fieldName) {
            case "name" -> contact.setName(value.toString());
            case "email" -> contact.setEmail(value.toString());
            case "phone" -> contact.setPhone(value.toString());
            case "title" -> contact.setTitle(value.toString());
            case "isPrimary" -> contact.setPrimary(Boolean.parseBoolean(value.toString()));
            case "notes" -> contact.setNotes(value.toString());
        }
    }
}
