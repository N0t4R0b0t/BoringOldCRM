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
package com.bocrm.backend.tools;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.repository.*;
import com.bocrm.backend.service.*;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Spring AI tool methods that wrap existing CRM services.
 * All methods read TenantContext (set by the JWT filter); they never set it.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class CrmTools {

    /** Captures the last navigation path requested during a request cycle. */
    private static final ThreadLocal<String> pendingNavigation = new ThreadLocal<>();

    /** Called by AssistantService after each request to get (and clear) any pending navigation. */
    public String popPendingNavigation() {
        String path = pendingNavigation.get();
        pendingNavigation.remove();
        return path;
    }

    /** Optional ID filters sent by LLM tools should use null; treat 0/-1 as not provided. */
    private Long normalizeOptionalId(Long id) {
        return (id != null && id <= 0L) ? null : id;
    }

    private final CustomerService customerService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final ActivityService activityService;
    private final CustomRecordService customRecordService;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final CustomFieldDefinitionService customFieldDefinitionService;
    private final McpApiKeyService mcpApiKeyService;
    private final ObjectMapper objectMapper;
    private final CalculatedFieldDefinitionRepository calculatedFieldDefinitionRepository;
    private final CalculatedFieldValueRepository calculatedFieldValueRepository;
    private final UserRepository userRepository;

    public CrmTools(CustomerService customerService, ContactService contactService,
                    OpportunityService opportunityService, ActivityService activityService,
                    CustomRecordService customRecordService, OrderService orderService, InvoiceService invoiceService,
                    CustomerRepository customerRepository, ContactRepository contactRepository,
                    OpportunityRepository opportunityRepository, ActivityRepository activityRepository,
                    CustomFieldDefinitionService customFieldDefinitionService, McpApiKeyService mcpApiKeyService,
                    ObjectMapper objectMapper,
                    CalculatedFieldDefinitionRepository calculatedFieldDefinitionRepository,
                    CalculatedFieldValueRepository calculatedFieldValueRepository,
                    UserRepository userRepository) {
        this.customerService = customerService;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.activityService = activityService;
        this.customRecordService = customRecordService;
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.activityRepository = activityRepository;
        this.customFieldDefinitionService = customFieldDefinitionService;
        this.mcpApiKeyService = mcpApiKeyService;
        this.objectMapper = objectMapper;
        this.calculatedFieldDefinitionRepository = calculatedFieldDefinitionRepository;
        this.calculatedFieldValueRepository = calculatedFieldValueRepository;
        this.userRepository = userRepository;
    }

    private JsonNode parseLenientJson(String json) throws Exception {
        return objectMapper.reader()
                .with(tools.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .with(tools.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .readTree(json);
    }

    /**
     * Bulk-loads calculated field values (displayInTable=true) for a page of entities.
     * Returns entityId → (fieldLabel → displayValue).
     */
    private java.util.Map<Long, java.util.Map<String, String>> loadTableCalcFields(
            Long tenantId, String entityType, List<Long> entityIds) {
        if (tenantId == null || entityIds.isEmpty()) return java.util.Collections.emptyMap();
        try {
            List<com.bocrm.backend.entity.CalculatedFieldDefinition> defs =
                    calculatedFieldDefinitionRepository.findByTenantIdAndEntityTypeIgnoreCase(tenantId, entityType)
                            .stream()
                            .filter(d -> Boolean.TRUE.equals(d.getDisplayInTable()) && Boolean.TRUE.equals(d.getEnabled()))
                            .collect(java.util.stream.Collectors.toList());
            if (defs.isEmpty()) return java.util.Collections.emptyMap();

            java.util.Map<Long, String> labelById = defs.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            com.bocrm.backend.entity.CalculatedFieldDefinition::getId,
                            com.bocrm.backend.entity.CalculatedFieldDefinition::getLabel));

            List<com.bocrm.backend.entity.CalculatedFieldValue> values =
                    calculatedFieldValueRepository.findByTenantIdAndEntityTypeAndEntityIdIn(tenantId, entityType, entityIds);

            java.util.Map<Long, java.util.Map<String, String>> result = new java.util.HashMap<>();
            for (com.bocrm.backend.entity.CalculatedFieldValue val : values) {
                String label = labelById.get(val.getCalculatedFieldId());
                if (label == null) continue;
                result.computeIfAbsent(val.getEntityId(), k -> new java.util.LinkedHashMap<>())
                        .put(label, parseCalcDisplayValue(val.getValueJsonb()));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to load table calculated fields for {}: {}", entityType, e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    private String parseCalcDisplayValue(String valueJsonb) {
        if (valueJsonb == null || valueJsonb.equals("null")) return "N/A";
        try {
            JsonNode node = objectMapper.readTree(valueJsonb);
            if (node.isTextual() || node.isNumber() || node.isBoolean()) return node.asText();
            return valueJsonb;
        } catch (Exception e) {
            return valueJsonb;
        }
    }

    /** Appends custom fields (from table_data_jsonb) and calculated fields to a search result line. */
    private void appendTableFields(StringBuilder sb, JsonNode customFields,
                                   java.util.Map<String, String> calcFields) {
        if (customFields != null && !customFields.isEmpty()) {
            customFields.properties().forEach(entry -> {
                JsonNode v = entry.getValue();
                String display = v.isNull() ? "N/A" : v.isTextual() ? v.asText() : v.toString();
                sb.append(" | ").append(entry.getKey()).append(": ").append(display);
            });
        }
        if (calcFields != null) {
            calcFields.forEach((label, val) -> sb.append(" | ").append(label).append(": ").append(val));
        }
    }

    @Tool(description = """
            Get the custom field schema for a CRM entity type. Returns all defined custom fields including their key, label, type, and whether they are required/mandatory.
            ALWAYS call this before creating or updating a Customer, Contact, Opportunity, or Activity so you know which custom fields exist and which are mandatory.
            entityType must be one of: Customer, Contact, Opportunity, Activity.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getCustomFieldSchema(String entityType) {
        try {
            List<CustomFieldDefinitionDTO> fields = customFieldDefinitionService.getFieldDefinitions(entityType);
            if (fields.isEmpty()) return "No custom fields defined for " + entityType + ".";
            StringBuilder sb = new StringBuilder("Custom fields for " + entityType + ":\n");
            for (CustomFieldDefinitionDTO f : fields) {
                sb.append("- key=\"").append(f.getKey()).append("\"")
                  .append(", label=\"").append(f.getLabel()).append("\"")
                  .append(", type=").append(f.getType())
                  .append(Boolean.TRUE.equals(f.getRequired()) ? " [MANDATORY]" : " [optional]")
                  .append("\n");
            }
            sb.append("\n\nCustom field value formats when creating/updating records:\n");
            sb.append("- Text/Number/Date fields: {\"fieldKey\": \"value\" or 123 or \"2026-03-23\"}\n");
            sb.append("- Checkbox fields: {\"fieldKey\": true}\n");
            sb.append("- Select/Multiselect fields: {\"fieldKey\": \"option\"} or {\"fieldKey\": [\"opt1\", \"opt2\"]}\n");
            sb.append("- Contact Link fields (single): {\"contactField\": {\"id\": 123, \"name\": \"John Doe\", \"email\": \"john@example.com\"}}\n");
            sb.append("- Contact Link fields (multiple): {\"contactsField\": [{\"id\": 123, \"name\": \"John Doe\"}, {\"id\": 456, \"name\": \"Jane Smith\"}]}\n");
            sb.append("- CustomRecord Link fields (single): {\"customRecordField\": {\"id\": 789, \"name\": \"Laptop ABC\"}}\n");
            sb.append("- CustomRecord Link fields (multiple): {\"customRecordsField\": [{\"id\": 789, \"name\": \"Laptop\"}, {\"id\": 790, \"name\": \"Monitor\"}]}\n");
            sb.append("- Workflow fields: {\"workflowKey\": {\"currentIndex\": 1}} for milestone index (0-based), or {\"currentIndex\": null} to reset\n");
            sb.append("- Document Link fields: {\"docField\": {\"id\": 111, \"name\": \"Contract.pdf\"}} (single) or array for multiple");
            return sb.toString();
        } catch (Exception e) {
            log.error("getCustomFieldSchema failed", e);
            return "Error retrieving custom field schema: " + e.getMessage();
        }
    }

    @Tool(description = "Get a summary of all CRM records — counts of customers, contacts, opportunities, and activities for this tenant.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getCrmSummary() {
        try {
            Long tenantId = TenantContext.getTenantId();
            long customers = customerRepository.findByTenantId(tenantId).size();
            long contacts = contactRepository.findByTenantId(tenantId).size();
            long opportunities = opportunityRepository.findByTenantId(tenantId).size();
            long activities = activityRepository.findByTenantId(tenantId).size();
            return String.format("CRM Summary: %d customers, %d contacts, %d opportunities, %d activities.",
                    customers, contacts, opportunities, activities);
        } catch (Exception e) {
            log.error("getCrmSummary failed", e);
            return "Error retrieving CRM summary: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new customer. Name is required; status defaults to 'active' if not provided.
            IMPORTANT: Before calling this, call getCustomFieldSchema("Customer") to discover any custom fields.
            Pass mandatory custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value", "otherKey": 123}.
            If any mandatory custom fields are missing from the user's request, ask the user for them before creating.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createCustomer(String name, String status, String customFieldsJson) {
        try {
            CreateCustomerRequest req = CreateCustomerRequest.builder()
                    .name(name)
                    .status(status != null && !status.isBlank() ? status : "active")
                    .build();
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.convertValue(objectMapper.readTree(customFieldsJson), java.util.Map.class));
            }
            CustomerDTO created = customerService.createCustomer(req);
            return "Customer created successfully: id=" + created.getId() + ", name=" + created.getName();
        } catch (Exception e) {
            log.error("createCustomer failed", e);
            return "Error creating customer: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing customer. Only provided fields are updated.
            Pass custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value", "otherKey": 123}.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateCustomer(Long customerId, String name, String status, String customFieldsJson) {
        try {
            UpdateCustomerRequest req = new UpdateCustomerRequest();
            if (name != null && !name.isBlank()) req.setName(name);
            if (status != null && !status.isBlank()) req.setStatus(status);
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            CustomerDTO updated = customerService.updateCustomer(customerId, req);
            return "Customer updated: id=" + updated.getId() + ", name=" + updated.getName();
        } catch (Exception e) {
            log.error("updateCustomer failed", e);
            return "Error updating customer: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new contact. Name and customerId are required.
            IMPORTANT: Before calling this, call getCustomFieldSchema("Contact") to discover any custom fields.
            Pass mandatory custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.
            If any mandatory custom fields are missing from the user's request, ask the user for them before creating.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createContact(String name, Long customerId, String email, String phone, String title, String customFieldsJson) {
        try {
            CreateContactRequest req = CreateContactRequest.builder()
                    .name(name)
                    .customerId(customerId)
                    .email(email)
                    .phone(phone)
                    .title(title)
                    .build();
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            ContactDTO created = contactService.createContact(req);
            return "Contact created: id=" + created.getId() + ", name=" + created.getName();
        } catch (Exception e) {
            log.error("createContact failed", e);
            return "Error creating contact: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing contact by its id. Only provided fields are updated.
            Pass custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateContact(Long contactId, String name, String email, String phone, String title, String customFieldsJson) {
        try {
            UpdateContactRequest req = new UpdateContactRequest();
            if (name != null && !name.isBlank()) req.setName(name);
            if (email != null && !email.isBlank()) req.setEmail(email);
            if (phone != null && !phone.isBlank()) req.setPhone(phone);
            if (title != null && !title.isBlank()) req.setTitle(title);
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            ContactDTO updated = contactService.updateContact(contactId, req);
            return "Contact updated: id=" + updated.getId() + ", name=" + updated.getName();
        } catch (Exception e) {
            log.error("updateContact failed", e);
            return "Error updating contact: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new sales opportunity. Name and customerId are required. Stage can be: prospecting, qualification, proposal, negotiation, closed_won, closed_lost.
            IMPORTANT: Before calling this, call getCustomFieldSchema("Opportunity") to discover any custom fields.
            Pass mandatory custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.
            If any mandatory custom fields are missing from the user's request, ask the user for them before creating.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createOpportunity(String name, Long customerId, String stage, String value, String closeDate, String customFieldsJson) {
        try {
            CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                    .name(name)
                    .customerId(customerId)
                    .stage(stage != null ? stage : "prospecting")
                    .value(value != null && !value.isBlank() ? new BigDecimal(value) : null)
                    .closeDate(closeDate != null && !closeDate.isBlank() ? LocalDate.parse(closeDate) : null)
                    .build();
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            OpportunityDTO created = opportunityService.createOpportunity(req);
            return "Opportunity created: id=" + created.getId() + ", name=" + created.getName() + ", stage=" + created.getStage();
        } catch (Exception e) {
            log.error("createOpportunity failed", e);
            return "Error creating opportunity: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing opportunity. Stage can be: prospecting, qualification, proposal, negotiation, closed_won, closed_lost.
            Pass custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateOpportunity(Long opportunityId, String name, String stage, String value, String customFieldsJson) {
        try {
            UpdateOpportunityRequest req = new UpdateOpportunityRequest();
            if (name != null && !name.isBlank()) req.setName(name);
            if (stage != null && !stage.isBlank()) req.setStage(stage);
            if (value != null && !value.isBlank()) req.setValue(new BigDecimal(value));
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            OpportunityDTO updated = opportunityService.updateOpportunity(opportunityId, req);
            return "Opportunity updated: id=" + updated.getId() + ", stage=" + updated.getStage();
        } catch (Exception e) {
            log.error("updateOpportunity failed", e);
            return "Error updating opportunity: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new activity (task/meeting/call/email). Subject, type, relatedType, and relatedId are required. Type can be: task, meeting, call, email. Status defaults to 'pending'.
            IMPORTANT: Before calling this, call getCustomFieldSchema("Activity") to discover any custom fields.
            Pass mandatory custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.
            If any mandatory custom fields are missing from the user's request, ask the user for them before creating.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createActivity(String subject, String type, String relatedType, Long relatedId,
                                  String description, String dueAt, String status, String customFieldsJson) {
        try {
            CreateActivityRequest req = CreateActivityRequest.builder()
                    .subject(subject)
                    .type(type)
                    .relatedType(relatedType)
                    .relatedId(relatedId)
                    .description(description)
                    .status(status != null ? status : "pending")
                    .dueAt(dueAt != null && !dueAt.isBlank() ? LocalDateTime.parse(dueAt) : null)
                    .build();
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            ActivityDTO created = activityService.createActivity(req);
            return "Activity created: id=" + created.getId() + ", subject=" + created.getSubject();
        } catch (DateTimeParseException e) {
            return "Error: dueAt must be in ISO-8601 format (e.g. 2026-03-20T10:00:00)";
        } catch (Exception e) {
            log.error("createActivity failed", e);
            return "Error creating activity: " + e.getMessage();
        }
    }

    @Tool(description = """
            Navigate the user's browser to a CRM page. Use this whenever the user asks to "go to", "open", "show", or "navigate to" a record or page.
            To open a specific record's detail drawer use the query param form: /customers?view={id}, /contacts?view={id}, /opportunities?view={id}, /activities?view={id}.
            To open the list page without a specific record: /customers, /contacts, /opportunities, /activities.
            Other pages: /dashboard, /reports, /admin.
            Always call this tool after finding a record the user wants to view.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String navigateTo(String path) {
        if (path == null || path.isBlank()) return "Error: path is required";
        String normalised = path.startsWith("/") ? path : "/" + path;
        pendingNavigation.set(normalised);
        return "NAVIGATE:" + normalised;
    }

    @Tool(description = """
            Apply filters to the current CRM list page. Use this when the user asks to filter or narrow down a list.
            entityType: Customer, Contact, Opportunity, Activity, or CustomRecord.
            filtersJson: JSON object mapping filter keys to arrays of values to include.
            Standard filter keys by entity:
              Opportunity: "stage" → ["prospecting","open","closed-won","closed-lost"]
              Customer: "status" → ["active","inactive","prospect"]
              Activity: "type" → ["call","email","meeting","task"], "status" → ["pending","completed","cancelled"]
              CustomRecord: "status" → ["active","inactive","retired"]
            Custom field filters use the field key prefixed with "cf_" (e.g. "cf_hype_level").
            Always call getCustomFieldSchema(entityType) first to discover valid custom field keys and their options.
            Example: filtersJson = {"stage": ["open"], "cf_hype_level": ["High"]}
            """)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String applyFilters(String entityType, String filtersJson) {
        if (entityType == null || entityType.isBlank()) return "Error: entityType is required";
        if (filtersJson == null || filtersJson.isBlank()) return "Error: filtersJson is required";
        pendingNavigation.set("FILTER:" + entityType + ":" + filtersJson);
        return "Filters applied for " + entityType + ": " + filtersJson;
    }

    @Tool(description = "Delete a customer permanently by its id.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteCustomer(Long customerId) {
        try {
            customerService.deleteCustomer(customerId);
            return "Customer id=" + customerId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteCustomer failed", e);
            return "Error deleting customer: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a contact permanently by its id.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteContact(Long contactId) {
        try {
            contactService.deleteContact(contactId);
            return "Contact id=" + contactId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteContact failed", e);
            return "Error deleting contact: " + e.getMessage();
        }
    }

    @Tool(description = "Delete an opportunity permanently by its id.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteOpportunity(Long opportunityId) {
        try {
            opportunityService.deleteOpportunity(opportunityId);
            return "Opportunity id=" + opportunityId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteOpportunity failed", e);
            return "Error deleting opportunity: " + e.getMessage();
        }
    }

    @Tool(description = "Delete an activity permanently by its id.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteActivity(Long activityId) {
        try {
            activityService.deleteActivity(activityId);
            return "Activity id=" + activityId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteActivity failed", e);
            return "Error deleting activity: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing activity. Only provided fields are updated. Status can be: pending, completed, cancelled.
            Pass custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateActivity(Long activityId, String subject, String status, String description, String dueAt, String customFieldsJson) {
        try {
            UpdateActivityRequest req = new UpdateActivityRequest();
            if (subject != null && !subject.isBlank()) req.setSubject(subject);
            if (status != null && !status.isBlank()) req.setStatus(status);
            if (description != null && !description.isBlank()) req.setDescription(description);
            if (dueAt != null && !dueAt.isBlank()) req.setDueAt(LocalDateTime.parse(dueAt));
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            ActivityDTO updated = activityService.updateActivity(activityId, req);
            return "Activity updated: id=" + updated.getId() + ", subject=" + updated.getSubject() + ", status=" + updated.getStatus();
        } catch (DateTimeParseException e) {
            return "Error: dueAt must be in ISO-8601 format (e.g. 2026-03-20T10:00:00)";
        } catch (Exception e) {
            log.error("updateActivity failed", e);
            return "Error updating activity: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new customRecord. Name is required.
            IMPORTANT: Before calling this, call getCustomFieldSchema("CustomRecord") to discover any custom fields.
            Pass mandatory custom field values in customFieldsJson as a JSON object, e.g. {"fieldKey": "value"}.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createCustomRecord(String name, String type, String serialNumber, String status, Long customerId, String customFieldsJson) {
        try {
            CreateCustomRecordRequest req = CreateCustomRecordRequest.builder()
                    .name(name)
                    .type(type)
                    .serialNumber(serialNumber)
                    .status(status != null && !status.isBlank() ? status : "active")
                    .customerId(customerId)
                    .build();
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            CustomRecordDTO created = customRecordService.createCustomRecord(req);
            return "CustomRecord created: id=" + created.getId() + ", name=" + created.getName();
        } catch (Exception e) {
            log.error("createCustomRecord failed", e);
            return "Error creating customRecord: " + e.getMessage();
        }
    }

    // ── Bulk create tools ─────────────────────────────────────────────────────

    @Tool(description = """
            Create multiple customers in a single call. Use this instead of calling createCustomer repeatedly.
            recordsJson must be a JSON array. Each element supports:
              name (required), status (optional, default "active"),
              customFields (optional object with custom field key/value pairs — preferred over customFieldsJson).
            Example: [{"name":"Acme Corp","status":"active","customFields":{"industry":"Cloud","annual_revenue":125000000}},{"name":"Beta LLC"}]
            Returns a summary of created ids and any per-row errors.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateCustomers(String recordsJson) {
        try {
            var records = parseLenientJson(recordsJson);
            var created = new java.util.ArrayList<String>();
            var errors  = new java.util.ArrayList<String>();
            for (var r : records) {
                try {
                    CreateCustomerRequest req = CreateCustomerRequest.builder()
                            .name(r.path("name").asText())
                            .status(r.has("status") && !r.path("status").asText().isBlank() ? r.path("status").asText() : "active")
                            .build();
                    JsonNode cf = extractCustomFields(r);
                    if (cf != null) req.setCustomFields(objectMapper.convertValue(cf, java.util.Map.class));
                    CustomerDTO c = customerService.createCustomer(req);
                    created.add("id=" + c.getId() + " (" + c.getName() + ")");
                } catch (Exception e) {
                    errors.add(r.path("name").asText("?") + ": " + e.getMessage());
                }
            }
            return buildBulkResult("customers", created, errors);
        } catch (Exception e) {
            log.error("bulkCreateCustomers failed", e);
            return "Error parsing recordsJson: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create multiple contacts in a single call. Use this instead of calling createContact repeatedly.
            recordsJson must be a JSON array. Each element supports:
              name (required),
              customerId (the customer's numeric id) OR customerName (the customer's exact name — looked up automatically),
              email, phone, title (all optional),
              customFields (optional object with custom field key/value pairs — preferred over customFieldsJson).
            Prefer customerName when you just bulk-created customers and know their names but not their ids.
            Example: [{"name":"Alice","customerName":"TechCore Inc","email":"alice@example.com","customFields":{"department":"Engineering"}},{"name":"Bob","customerId":2}]
            Returns a summary of created ids and any per-row errors.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateContacts(String recordsJson) {
        Long tenantId = TenantContext.getTenantId();
        try {
            var records = parseLenientJson(recordsJson);
            var created = new java.util.ArrayList<String>();
            var errors  = new java.util.ArrayList<String>();
            for (var r : records) {
                try {
                    CreateContactRequest req = CreateContactRequest.builder()
                            .name(r.path("name").asText())
                            .customerId(resolveCustomerId(r, tenantId))
                            .email(r.has("email") ? r.path("email").asText(null) : null)
                            .phone(r.has("phone") ? r.path("phone").asText(null) : null)
                            .title(r.has("title") ? r.path("title").asText(null) : null)
                            .build();
                    JsonNode cf = extractCustomFields(r);
                    if (cf != null) req.setCustomFields(cf);
                    ContactDTO c = contactService.createContact(req);
                    created.add("id=" + c.getId() + " (" + c.getName() + ")");
                } catch (Exception e) {
                    errors.add(r.path("name").asText("?") + ": " + e.getMessage());
                }
            }
            return buildBulkResult("contacts", created, errors);
        } catch (Exception e) {
            log.error("bulkCreateContacts failed", e);
            return "Error parsing recordsJson: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create multiple opportunities in a single call. Use this instead of calling createOpportunity repeatedly.
            recordsJson must be a JSON array. Each element supports:
              name (required),
              customerId (the customer's numeric id) OR customerName (the customer's exact name — looked up automatically),
              stage (optional, default "prospecting"), value (optional numeric string), closeDate (optional YYYY-MM-DD),
              customFields (optional object with custom field key/value pairs — preferred over customFieldsJson).
            Workflow custom fields inside customFields must use {"currentIndex": N} format, e.g. {"sales_stage": {"currentIndex": 2}}.
            Prefer customerName when you just bulk-created customers and know their names but not their ids.
            Example: [{"name":"Deal A","customerName":"TechCore Inc","stage":"proposal","value":"50000","customFields":{"key_decision_factors":["Price"],"sales_stage":{"currentIndex":2}}}]
            Returns a summary of created ids and any per-row errors.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateOpportunities(String recordsJson) {
        Long tenantId = TenantContext.getTenantId();
        try {
            var records = parseLenientJson(recordsJson);
            var created = new java.util.ArrayList<String>();
            var errors  = new java.util.ArrayList<String>();
            for (var r : records) {
                try {
                    String valueStr = r.has("value") ? r.path("value").asText(null) : null;
                    String dateStr  = r.has("closeDate") ? r.path("closeDate").asText(null) : null;
                    CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                            .name(r.path("name").asText())
                            .customerId(resolveCustomerId(r, tenantId))
                            .stage(r.has("stage") && !r.path("stage").asText().isBlank() ? r.path("stage").asText() : "prospecting")
                            .value(valueStr != null && !valueStr.isBlank() ? new java.math.BigDecimal(valueStr) : null)
                            .closeDate(dateStr != null && !dateStr.isBlank() ? java.time.LocalDate.parse(dateStr) : null)
                            .build();
                    JsonNode cf = extractCustomFields(r);
                    if (cf != null) req.setCustomFields(cf);
                    OpportunityDTO o = opportunityService.createOpportunity(req);
                    created.add("id=" + o.getId() + " (" + o.getName() + ")");
                } catch (Exception e) {
                    errors.add(r.path("name").asText("?") + ": " + e.getMessage());
                }
            }
            return buildBulkResult("opportunities", created, errors);
        } catch (Exception e) {
            log.error("bulkCreateOpportunities failed", e);
            return "Error parsing recordsJson: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create multiple activities in a single call. Use this instead of calling createActivity repeatedly.
            recordsJson must be a JSON array. Each element supports:
              subject (required), type (required: task/meeting/call/email),
              relatedType (required: Customer/Contact/Opportunity), relatedId (numeric) OR opportunityName/customerName (looked up automatically),
              description, dueAt (ISO-8601 datetime), status (default "pending"),
              customFields (optional object with custom field key/value pairs — preferred over customFieldsJson).
            Prefer opportunityName/customerName when you just bulk-created records and know names but not ids yet.
            Example: [{"subject":"Follow-up","type":"call","relatedType":"Customer","customerName":"TechCore Inc"},{"subject":"Send proposal","type":"email","relatedType":"Opportunity","opportunityName":"ML Pipeline Expansion"}]
            Returns a summary of created ids and any per-row errors.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateActivities(String recordsJson) {
        Long tenantId = TenantContext.getTenantId();
        try {
            var records = parseLenientJson(recordsJson);
            var created = new java.util.ArrayList<String>();
            var errors  = new java.util.ArrayList<String>();
            for (var r : records) {
                try {
                    String dueAt = r.has("dueAt") ? r.path("dueAt").asText(null) : null;
                    String relatedType = r.path("relatedType").asText();
                    Long relatedId = resolveActivityRelatedId(r, relatedType, tenantId);
                    CreateActivityRequest req = CreateActivityRequest.builder()
                            .subject(r.path("subject").asText())
                            .type(r.path("type").asText())
                            .relatedType(relatedType)
                            .relatedId(relatedId)
                            .description(r.has("description") ? r.path("description").asText(null) : null)
                            .status(r.has("status") && !r.path("status").asText().isBlank() ? r.path("status").asText() : "pending")
                            .dueAt(dueAt != null && !dueAt.isBlank() ? java.time.LocalDateTime.parse(dueAt) : null)
                            .build();
                    JsonNode cf = extractCustomFields(r);
                    if (cf != null) req.setCustomFields(cf);
                    ActivityDTO a = activityService.createActivity(req);
                    created.add("id=" + a.getId() + " (" + a.getSubject() + ")");
                } catch (Exception e) {
                    errors.add(r.path("subject").asText("?") + ": " + e.getMessage());
                }
            }
            return buildBulkResult("activities", created, errors);
        } catch (Exception e) {
            log.error("bulkCreateActivities failed", e);
            return "Error parsing recordsJson: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create multiple customRecords in a single call. Use this instead of calling createCustomRecord repeatedly.
            recordsJson must be a JSON array. Each element supports:
              name (required), type (optional), serialNumber (optional), status (optional, default "active"),
              customerId (numeric) OR customerName (looked up automatically), customFields (optional object).
            Prefer customerName when you just bulk-created customers and know their names but not their ids.
            Example: [{"name":"TechCore MSA 2024","type":"MSA","customerName":"TechCore Inc","customFields":{"sla_tier":"Gold"}},{"name":"License B","type":"software"}]
            Returns a summary of created ids and any per-row errors.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateCustomRecords(String recordsJson) {
        Long tenantId = TenantContext.getTenantId();
        try {
            var records = parseLenientJson(recordsJson);
            var created = new java.util.ArrayList<String>();
            var errors  = new java.util.ArrayList<String>();
            for (var r : records) {
                try {
                    CreateCustomRecordRequest req = CreateCustomRecordRequest.builder()
                            .name(r.path("name").asText())
                            .type(r.has("type") ? r.path("type").asText(null) : null)
                            .serialNumber(r.has("serialNumber") ? r.path("serialNumber").asText(null) : null)
                            .status(r.has("status") && !r.path("status").asText().isBlank() ? r.path("status").asText() : "active")
                            .customerId(resolveCustomerId(r, tenantId))
                            .build();
                    JsonNode cf = extractCustomFields(r);
                    if (cf != null) req.setCustomFields(cf);
                    CustomRecordDTO a = customRecordService.createCustomRecord(req);
                    created.add("id=" + a.getId() + " (" + a.getName() + ")");
                } catch (Exception e) {
                    errors.add(r.path("name").asText("?") + ": " + e.getMessage());
                }
            }
            return buildBulkResult("custom_records", created, errors);
        } catch (Exception e) {
            log.error("bulkCreateCustomRecords failed", e);
            return "Error parsing recordsJson: " + e.getMessage();
        }
    }

    /**
     * Resolves the customer id for a bulk record element.
     * Uses "customerId" directly if present, otherwise looks up the tenant's customers
     * by the "customerName" field (case-insensitive). Returns null if neither is provided.
     */
    private Long resolveCustomerId(JsonNode record, Long tenantId) {
        if (record.has("customerId") && !record.path("customerId").isNull()
                && record.path("customerId").asLong() > 0) {
            return record.path("customerId").asLong();
        }
        if (record.has("customerName") && !record.path("customerName").asText().isBlank()) {
            String name = record.path("customerName").asText().trim();
            return customerRepository.findByTenantId(tenantId).stream()
                    .filter(c -> c.getName().equalsIgnoreCase(name))
                    .map(com.bocrm.backend.entity.Customer::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No customer found with name: " + name));
        }
        return null;
    }

    /**
     * Resolves the relatedId for an activity from a numeric relatedId, opportunityName, or customerName field.
     */
    private Long resolveActivityRelatedId(JsonNode record, String relatedType, Long tenantId) {
        if (record.has("relatedId") && !record.path("relatedId").isNull() && record.path("relatedId").asLong() > 0) {
            return record.path("relatedId").asLong();
        }
        if ("Opportunity".equalsIgnoreCase(relatedType) && record.has("opportunityName")) {
            String name = record.path("opportunityName").asText().trim();
            return opportunityRepository.findByTenantId(tenantId).stream()
                    .filter(o -> o.getName().equalsIgnoreCase(name))
                    .map(com.bocrm.backend.entity.Opportunity::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No opportunity found with name: " + name));
        }
        if ("Customer".equalsIgnoreCase(relatedType) && record.has("customerName")) {
            return resolveCustomerId(record, tenantId);
        }
        return null;
    }

    /**
     * Extracts custom fields from a bulk record element.
     * Prefers "customFields" as a nested object (no double-encoding needed).
     * Falls back to "customFieldsJson" as a JSON string for backward compatibility.
     */
    private JsonNode extractCustomFields(JsonNode record) throws Exception {
        if (record.has("customFields") && record.get("customFields").isObject()) {
            return record.get("customFields");
        }
        if (record.has("customFieldsJson")) {
            String raw = record.path("customFieldsJson").asText(null);
            if (raw != null && !raw.isBlank()) {
                return objectMapper.readTree(raw);
            }
        }
        return null;
    }

    private String buildBulkResult(String entityType, List<String> created, List<String> errors) {
        var sb = new StringBuilder();
        sb.append("Bulk create ").append(entityType).append(": ")
          .append(created.size()).append(" created");
        if (!created.isEmpty()) sb.append(" [").append(String.join(", ", created)).append("]");
        if (!errors.isEmpty()) sb.append(". ").append(errors.size()).append(" error(s): ").append(String.join("; ", errors));
        return sb.toString();
    }

    @Tool(description = "Update an existing customRecord by its id. Only provided fields are updated.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateCustomRecord(Long customRecordId, String name, String type, String status, String serialNumber) {
        try {
            UpdateCustomRecordRequest req = new UpdateCustomRecordRequest();
            if (name != null && !name.isBlank()) req.setName(name);
            if (type != null && !type.isBlank()) req.setType(type);
            if (status != null && !status.isBlank()) req.setStatus(status);
            if (serialNumber != null && !serialNumber.isBlank()) req.setSerialNumber(serialNumber);
            CustomRecordDTO updated = customRecordService.updateCustomRecord(customRecordId, req);
            return "CustomRecord updated: id=" + updated.getId() + ", name=" + updated.getName();
        } catch (Exception e) {
            log.error("updateCustomRecord failed", e);
            return "Error updating customRecord: " + e.getMessage();
        }
    }

    @Tool(description = "Delete an customRecord permanently by its id.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteCustomRecord(Long customRecordId) {
        try {
            customRecordService.deleteCustomRecord(customRecordId);
            return "CustomRecord id=" + customRecordId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteCustomRecord failed", e);
            return "Error deleting customRecord: " + e.getMessage();
        }
    }

    @Tool(description = """
            Advanced search for opportunities with sorting. Query opportunities with flexible sorting.
            sortBy can be: name, value, closeDate, stage, createdAt, updatedAt (default: value)
            sortOrder can be: asc or desc (default: desc for value, asc for name)
            Examples: Get top 10 opportunities by value (highest first), or find all closing soon (sort by closeDate asc).
            Pass empty query to see all opportunities in the specified sort order.
            OPTIONAL filters — omit entirely if not filtering: customerId (numeric customer id; do NOT pass 0 or a guess — omit to search across all customers), stage (do NOT pass an empty string — omit to search across all stages).""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String advancedOpportunitySearch(String query, String stage, Long customerId, String sortBy, String sortOrder) {
        try {
            Long effectiveCustomerId = normalizeOptionalId(customerId);
            // Validate and default sortBy
            String safeSortBy = "value";
            if (sortBy != null && !sortBy.isBlank()) {
                String lower = sortBy.toLowerCase();
                if (java.util.List.of("name", "value", "closedate", "stage", "createdat", "updatedat").contains(lower)) {
                    safeSortBy = lower.equals("closedate") ? "closeDate" :
                                 lower.equals("createdat") ? "createdAt" :
                                 lower.equals("updatedat") ? "updatedAt" : lower;
                }
            }
            // Default sort order: desc for value, asc otherwise
            String safeSortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("asc")) ? "asc" : "desc";
            if (safeSortBy.equals("name") && (sortOrder == null || sortOrder.isBlank())) {
                safeSortOrder = "asc"; // alphabetical by default
            }

            PagedResponse<OpportunityDTO> result = opportunityService.listOpportunities(0, 50, safeSortBy, safeSortOrder, query, effectiveCustomerId,
                    (stage == null || stage.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(stage), null, java.util.Collections.emptyMap());
            if (result.getContent().isEmpty()) return "No opportunities found matching the criteria.";
            Long tenantId = TenantContext.getTenantId();
            List<Long> ids = result.getContent().stream().map(OpportunityDTO::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.Map<String, String>> calcFields = loadTableCalcFields(tenantId, "Opportunity", ids);
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " opportunit(ies) (showing top 50, sorted by " + safeSortBy + " " + safeSortOrder + "):\n");
            result.getContent().forEach(o -> {
                sb.append("- [").append(o.getId()).append("] ").append(o.getName())
                        .append(" | Value: $").append(o.getValue() != null ? o.getValue() : "0")
                        .append(" | Stage: ").append(o.getStage())
                        .append(" | Close: ").append(o.getCloseDate() != null ? o.getCloseDate() : "N/A");
                appendTableFields(sb, o.getCustomFields(), calcFields.get(o.getId()));
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("advancedOpportunitySearch failed", e);
            return "Error searching opportunities: " + e.getMessage();
        }
    }

    @Tool(description = """
            Analyze opportunities data: find highest value, total pipeline, etc.
            analysis can be: highest_value, lowest_value, total_value, average_value, by_stage, closing_soon (next 30 days)
            OPTIONAL filters — omit entirely if not filtering: stage (e.g. 'proposal'; do NOT pass an empty string — omit to search across all stages), customerId (numeric customer id; do NOT pass 0 or a guess — omit to search across all customers), query (search term).
            Use this for insights like "What's our biggest opportunity?" or "How much is in each pipeline stage?".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String analyzeOpportunities(String analysis, String stage, Long customerId, String query) {
        try {
            Long effectiveCustomerId = normalizeOptionalId(customerId);
            PagedResponse<OpportunityDTO> result = opportunityService.listOpportunities(0, 1000, "value", "desc", query, effectiveCustomerId,
                    (stage == null || stage.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(stage), null, java.util.Collections.emptyMap());

            if (result.getContent().isEmpty()) return "No opportunities found to analyze.";

            String analysis_type = analysis != null ? analysis.toLowerCase() : "highest_value";
            java.util.List<OpportunityDTO> opps = result.getContent();

            return switch(analysis_type) {
                case "highest_value" -> {
                    OpportunityDTO top = opps.stream().max(java.util.Comparator.comparing(OpportunityDTO::getValue, java.util.Comparator.nullsFirst(BigDecimal::compareTo))).orElse(null);
                    yield top != null ? String.format("Highest value opportunity: [%d] %s - $%s (Stage: %s)", top.getId(), top.getName(), top.getValue(), top.getStage())
                            : "No opportunities found.";
                }
                case "lowest_value" -> {
                    OpportunityDTO bottom = opps.stream().filter(o -> o.getValue() != null && o.getValue().compareTo(BigDecimal.ZERO) > 0)
                            .min(java.util.Comparator.comparing(OpportunityDTO::getValue, java.util.Comparator.nullsLast(BigDecimal::compareTo))).orElse(null);
                    yield bottom != null ? String.format("Lowest value opportunity: [%d] %s - $%s (Stage: %s)", bottom.getId(), bottom.getName(), bottom.getValue(), bottom.getStage())
                            : "No non-zero opportunities found.";
                }
                case "total_value" -> {
                    BigDecimal total = opps.stream()
                            .map(OpportunityDTO::getValue)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    yield String.format("Total opportunity value: $%s across %d opportunities", total, opps.size());
                }
                case "average_value" -> {
                    BigDecimal total = opps.stream()
                            .map(OpportunityDTO::getValue)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(new BigDecimal(opps.size()), 2, java.math.RoundingMode.HALF_UP);
                    yield String.format("Average opportunity value: $%s", avg);
                }
                case "by_stage" -> {
                    java.util.Map<String, java.util.List<OpportunityDTO>> byStage = opps.stream()
                            .collect(java.util.stream.Collectors.groupingBy(OpportunityDTO::getStage));
                    StringBuilder sb = new StringBuilder("Opportunities by stage:\n");
                    byStage.forEach((st, list) -> {
                        BigDecimal stageTotal = list.stream()
                                .map(OpportunityDTO::getValue)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        sb.append("- ").append(st).append(": ").append(list.size()).append(" opps, $").append(stageTotal).append("\n");
                    });
                    yield sb.toString();
                }
                case "closing_soon" -> {
                    LocalDate soon = LocalDate.now().plusDays(30);
                    java.util.List<OpportunityDTO> closingSoon = opps.stream()
                            .filter(o -> o.getCloseDate() != null && o.getCloseDate().isBefore(soon) && o.getCloseDate().isAfter(LocalDate.now()))
                            .sorted(java.util.Comparator.comparing(OpportunityDTO::getCloseDate))
                            .toList();
                    if (closingSoon.isEmpty()) yield "No opportunities closing in the next 30 days.";
                    StringBuilder sb = new StringBuilder("Opportunities closing in next 30 days:\n");
                    closingSoon.forEach(o -> sb.append("- [").append(o.getId()).append("] ").append(o.getName())
                            .append(" - $").append(o.getValue()).append(" (Close: ").append(o.getCloseDate()).append(")\n"));
                    yield sb.toString();
                }
                default -> "Unknown analysis type. Use: highest_value, lowest_value, total_value, average_value, by_stage, or closing_soon.";
            };
        } catch (Exception e) {
            log.error("analyzeOpportunities failed", e);
            return "Error analyzing opportunities: " + e.getMessage();
        }
    }

    @Tool(description = """
            Advanced search for customers with sorting and filtering.
            sortBy can be: name, status, createdAt, updatedAt (default: createdAt)
            sortOrder can be: asc or desc (default: desc)
            Pass empty query to see all customers in the specified sort order.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String advancedCustomerSearch(String query, String status, String sortBy, String sortOrder) {
        try {
            String safeSortBy = "createdAt";
            if (sortBy != null && !sortBy.isBlank()) {
                String lower = sortBy.toLowerCase();
                if (java.util.List.of("name", "status", "createdat", "updatedat").contains(lower)) {
                    safeSortBy = lower.equals("createdat") ? "createdAt" : lower.equals("updatedat") ? "updatedAt" : lower;
                }
            }
            String safeSortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("asc")) ? "asc" : "desc";

            PagedResponse<CustomerDTO> result = customerService.listCustomers(0, 50, safeSortBy, safeSortOrder, query,
                    (status == null || status.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(status), java.util.Collections.emptyMap());
            if (result.getContent().isEmpty()) return "No customers found matching the criteria.";
            Long tenantId = TenantContext.getTenantId();
            List<Long> ids = result.getContent().stream().map(CustomerDTO::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.Map<String, String>> calcFields = loadTableCalcFields(tenantId, "Customer", ids);
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " customer(s) (showing top 50, sorted by " + safeSortBy + " " + safeSortOrder + "):\n");
            result.getContent().forEach(c -> {
                sb.append("- [").append(c.getId()).append("] ").append(c.getName())
                        .append(" | Status: ").append(c.getStatus())
                        .append(" | Created: ").append(c.getCreatedAt());
                appendTableFields(sb, c.getCustomFields(), calcFields.get(c.getId()));
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("advancedCustomerSearch failed", e);
            return "Error searching customers: " + e.getMessage();
        }
    }

    @Tool(description = """
            Analyze customers data: count by status, find recently added, etc.
            analysis can be: by_status, recent, total_count, status_breakdown
            Optional filters: status (e.g. 'active'), query (search term).
            Use this for insights like "How many active customers do we have?" or "Show me customers by status".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String analyzeCustomers(String analysis, String status, String query) {
        try {
            PagedResponse<CustomerDTO> result = customerService.listCustomers(0, 1000, "createdAt", "desc", query,
                    (status == null || status.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(status), java.util.Collections.emptyMap());

            if (result.getContent().isEmpty()) return "No customers found to analyze.";

            String analysis_type = analysis != null ? analysis.toLowerCase() : "total_count";
            java.util.List<CustomerDTO> customers = result.getContent();

            return switch(analysis_type) {
                case "total_count" -> String.format("Total customers: %d", result.getTotalElements());
                case "by_status" -> {
                    java.util.Map<String, Long> byStatus = customers.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomerDTO::getStatus, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("Customers by status:\n");
                    byStatus.forEach((st, count) -> sb.append("- ").append(st).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "recent" -> {
                    int limit = Math.min(10, customers.size());
                    StringBuilder sb = new StringBuilder("Most recently added customers:\n");
                    for (int i = 0; i < limit; i++) {
                        CustomerDTO c = customers.get(i);
                        sb.append("- [").append(c.getId()).append("] ").append(c.getName())
                                .append(" (Status: ").append(c.getStatus()).append(", Added: ").append(c.getCreatedAt()).append(")\n");
                    }
                    yield sb.toString();
                }
                case "status_breakdown" -> {
                    java.util.Map<String, Long> breakdown = customers.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomerDTO::getStatus, java.util.stream.Collectors.counting()));
                    long total = customers.size();
                    StringBuilder sb = new StringBuilder("Customer status breakdown (total: " + total + "):\n");
                    breakdown.forEach((st, count) -> {
                        double pct = (count * 100.0) / total;
                        sb.append("- ").append(st).append(": ").append(count).append(" (").append(String.format("%.1f%%", pct)).append(")\n");
                    });
                    yield sb.toString();
                }
                default -> "Unknown analysis type. Use: total_count, by_status, recent, or status_breakdown.";
            };
        } catch (Exception e) {
            log.error("analyzeCustomers failed", e);
            return "Error analyzing customers: " + e.getMessage();
        }
    }

    @Tool(description = """
            Advanced search for contacts with sorting and filtering.
            sortBy can be: name, email, title, status, createdAt, updatedAt (default: createdAt)
            sortOrder can be: asc or desc (default: desc)
            Pass empty query to see all contacts in the specified sort order.
            OPTIONAL filter — omit entirely if not filtering: customerId (numeric customer id; do NOT pass 0 or a guess — omit to search across all customers).""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String advancedContactSearch(String query, Long customerId, String sortBy, String sortOrder) {
        try {
            Long effectiveCustomerId = normalizeOptionalId(customerId);
            String safeSortBy = "createdAt";
            if (sortBy != null && !sortBy.isBlank()) {
                String lower = sortBy.toLowerCase();
                if (java.util.List.of("name", "email", "title", "status", "createdat", "updatedat").contains(lower)) {
                    safeSortBy = lower.equals("createdat") ? "createdAt" : lower.equals("updatedat") ? "updatedAt" : lower;
                }
            }
            String safeSortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("asc")) ? "asc" : "desc";

            PagedResponse<ContactDTO> result = contactService.listContacts(0, 50, safeSortBy, safeSortOrder, query, effectiveCustomerId, null,
                    java.util.Collections.emptyList(), java.util.Collections.emptyMap());
            if (result.getContent().isEmpty()) return "No contacts found matching the criteria.";
            Long tenantId = TenantContext.getTenantId();
            List<Long> ids = result.getContent().stream().map(ContactDTO::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.Map<String, String>> calcFields = loadTableCalcFields(tenantId, "Contact", ids);
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " contact(s) (showing top 50, sorted by " + safeSortBy + " " + safeSortOrder + "):\n");
            result.getContent().forEach(c -> {
                sb.append("- [").append(c.getId()).append("] ").append(c.getName())
                        .append(" | Email: ").append(c.getEmail() != null ? c.getEmail() : "N/A")
                        .append(" | Status: ").append(c.getStatus());
                appendTableFields(sb, c.getCustomFields(), calcFields.get(c.getId()));
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("advancedContactSearch failed", e);
            return "Error searching contacts: " + e.getMessage();
        }
    }

    @Tool(description = """
            Analyze contacts data: count by status, find by customer, etc.
            analysis can be: total_count, by_status, by_customer, recent, status_breakdown
            OPTIONAL filters — omit entirely if not filtering: customerId (numeric customer id; do NOT pass 0 or a guess — omit to search across all customers), status.
            Use this for insights like "How many contacts are we managing?" or "Show contacts by status".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String analyzeContacts(String analysis, Long customerId, String status, String query) {
        try {
            Long effectiveCustomerId = normalizeOptionalId(customerId);
            PagedResponse<ContactDTO> result = contactService.listContacts(0, 1000, "createdAt", "desc", query, effectiveCustomerId, null,
                    (status == null || status.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(status), java.util.Collections.emptyMap());

            if (result.getContent().isEmpty()) return "No contacts found to analyze.";

            String analysis_type = analysis != null ? analysis.toLowerCase() : "total_count";
            java.util.List<ContactDTO> contacts = result.getContent();

            return switch(analysis_type) {
                case "total_count" -> String.format("Total contacts: %d", result.getTotalElements());
                case "by_status" -> {
                    java.util.Map<String, Long> byStatus = contacts.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ContactDTO::getStatus, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("Contacts by status:\n");
                    byStatus.forEach((st, count) -> sb.append("- ").append(st).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "by_customer" -> {
                    java.util.Map<Long, Long> byCustomer = contacts.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ContactDTO::getCustomerId, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("Contacts by customer:\n");
                    byCustomer.forEach((cid, count) -> sb.append("- Customer [").append(cid).append("]: ").append(count).append(" contact(s)\n"));
                    yield sb.toString();
                }
                case "recent" -> {
                    int limit = Math.min(10, contacts.size());
                    StringBuilder sb = new StringBuilder("Most recently added contacts:\n");
                    for (int i = 0; i < limit; i++) {
                        ContactDTO c = contacts.get(i);
                        sb.append("- [").append(c.getId()).append("] ").append(c.getName())
                                .append(" | ").append(c.getEmail() != null ? c.getEmail() : "N/A")
                                .append(" (Status: ").append(c.getStatus()).append(")\n");
                    }
                    yield sb.toString();
                }
                case "status_breakdown" -> {
                    java.util.Map<String, Long> breakdown = contacts.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ContactDTO::getStatus, java.util.stream.Collectors.counting()));
                    long total = contacts.size();
                    StringBuilder sb = new StringBuilder("Contact status breakdown (total: " + total + "):\n");
                    breakdown.forEach((st, count) -> {
                        double pct = (count * 100.0) / total;
                        sb.append("- ").append(st).append(": ").append(count).append(" (").append(String.format("%.1f%%", pct)).append(")\n");
                    });
                    yield sb.toString();
                }
                default -> "Unknown analysis type. Use: total_count, by_status, by_customer, recent, or status_breakdown.";
            };
        } catch (Exception e) {
            log.error("analyzeContacts failed", e);
            return "Error analyzing contacts: " + e.getMessage();
        }
    }

    @Tool(description = """
            Advanced search for activities with sorting and filtering.
            sortBy can be: subject, type, status, dueAt, createdAt, updatedAt (default: dueAt)
            sortOrder can be: asc or desc (default: asc for dueAt to show urgent first)
            Pass empty query to see all activities in the specified sort order.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String advancedActivitySearch(String query, String type, String status, String sortBy, String sortOrder) {
        try {
            String safeSortBy = "dueAt";
            if (sortBy != null && !sortBy.isBlank()) {
                String lower = sortBy.toLowerCase();
                if (java.util.List.of("subject", "type", "status", "dueat", "createdat", "updatedat").contains(lower)) {
                    safeSortBy = lower.equals("dueat") ? "dueAt" : lower.equals("createdat") ? "createdAt" : lower.equals("updatedat") ? "updatedAt" : lower;
                }
            }
            String safeSortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) ? "desc" : "asc";
            if (safeSortBy.equals("dueAt") && (sortOrder == null || sortOrder.isBlank())) {
                safeSortOrder = "asc"; // urgent items first
            }

            PagedResponse<ActivityDTO> result = activityService.listActivities(0, 50, safeSortBy, safeSortOrder, query,
                    (type == null || type.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(type),
                    (status == null || status.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(status), java.util.Collections.emptyMap());
            if (result.getContent().isEmpty()) return "No activities found matching the criteria.";
            Long tenantId = TenantContext.getTenantId();
            List<Long> ids = result.getContent().stream().map(ActivityDTO::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.Map<String, String>> calcFields = loadTableCalcFields(tenantId, "Activity", ids);
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " activit(ies) (showing top 50, sorted by " + safeSortBy + " " + safeSortOrder + "):\n");
            result.getContent().forEach(a -> {
                sb.append("- [").append(a.getId()).append("] ").append(a.getSubject())
                        .append(" | Type: ").append(a.getType())
                        .append(" | Status: ").append(a.getStatus())
                        .append(" | Due: ").append(a.getDueAt() != null ? a.getDueAt() : "N/A");
                appendTableFields(sb, a.getCustomFields(), calcFields.get(a.getId()));
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("advancedActivitySearch failed", e);
            return "Error searching activities: " + e.getMessage();
        }
    }

    @Tool(description = """
            Analyze activities data: count by type, by status, overdue items, etc.
            analysis can be: by_type, by_status, overdue, upcoming, type_breakdown, total_count
            Optional filters: type (e.g. 'meeting'), status (e.g. 'completed').
            Use this for insights like "How many tasks are overdue?" or "What activities are coming up?".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String analyzeActivities(String analysis, String type, String status, String query) {
        try {
            PagedResponse<ActivityDTO> result = activityService.listActivities(0, 1000, "dueAt", "asc", query,
                    (type == null || type.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(type),
                    (status == null || status.isBlank()) ? java.util.Collections.emptyList() : java.util.List.of(status), java.util.Collections.emptyMap());

            if (result.getContent().isEmpty()) return "No activities found to analyze.";

            String analysis_type = analysis != null ? analysis.toLowerCase() : "total_count";
            java.util.List<ActivityDTO> activities = result.getContent();

            return switch(analysis_type) {
                case "total_count" -> String.format("Total activities: %d", result.getTotalElements());
                case "by_type" -> {
                    java.util.Map<String, Long> byType = activities.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ActivityDTO::getType, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("Activities by type:\n");
                    byType.forEach((t, count) -> sb.append("- ").append(t).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "by_status" -> {
                    java.util.Map<String, Long> byStatus = activities.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ActivityDTO::getStatus, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("Activities by status:\n");
                    byStatus.forEach((st, count) -> sb.append("- ").append(st).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "overdue" -> {
                    LocalDateTime now = LocalDateTime.now();
                    java.util.List<ActivityDTO> overdue = activities.stream()
                            .filter(a -> a.getDueAt() != null && a.getDueAt().isBefore(now) && !"completed".equals(a.getStatus()))
                            .sorted(java.util.Comparator.comparing(ActivityDTO::getDueAt))
                            .toList();
                    if (overdue.isEmpty()) yield "No overdue activities.";
                    StringBuilder sb = new StringBuilder("Overdue activities (" + overdue.size() + "):\n");
                    overdue.forEach(a -> sb.append("- [").append(a.getId()).append("] ").append(a.getSubject())
                            .append(" (Type: ").append(a.getType()).append(", Due: ").append(a.getDueAt()).append(")\n"));
                    yield sb.toString();
                }
                case "upcoming" -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime next7days = now.plusDays(7);
                    java.util.List<ActivityDTO> upcoming = activities.stream()
                            .filter(a -> a.getDueAt() != null && a.getDueAt().isAfter(now) && a.getDueAt().isBefore(next7days) && !"completed".equals(a.getStatus()))
                            .sorted(java.util.Comparator.comparing(ActivityDTO::getDueAt))
                            .toList();
                    if (upcoming.isEmpty()) yield "No upcoming activities in the next 7 days.";
                    StringBuilder sb = new StringBuilder("Upcoming activities (next 7 days, " + upcoming.size() + "):\n");
                    upcoming.forEach(a -> sb.append("- [").append(a.getId()).append("] ").append(a.getSubject())
                            .append(" (Type: ").append(a.getType()).append(", Due: ").append(a.getDueAt()).append(")\n"));
                    yield sb.toString();
                }
                case "type_breakdown" -> {
                    java.util.Map<String, Long> breakdown = activities.stream()
                            .collect(java.util.stream.Collectors.groupingBy(ActivityDTO::getType, java.util.stream.Collectors.counting()));
                    long total = activities.size();
                    StringBuilder sb = new StringBuilder("Activity type breakdown (total: " + total + "):\n");
                    breakdown.forEach((t, count) -> {
                        double pct = (count * 100.0) / total;
                        sb.append("- ").append(t).append(": ").append(count).append(" (").append(String.format("%.1f%%", pct)).append(")\n");
                    });
                    yield sb.toString();
                }
                default -> "Unknown analysis type. Use: total_count, by_type, by_status, overdue, upcoming, or type_breakdown.";
            };
        } catch (Exception e) {
            log.error("analyzeActivities failed", e);
            return "Error analyzing activities: " + e.getMessage();
        }
    }

    @Tool(description = """
            Advanced search for customRecords with sorting and filtering.
            sortBy can be: name, type, status, createdAt, updatedAt (default: createdAt)
            sortOrder can be: asc or desc (default: desc)
            Pass empty query to see all customRecords in the specified sort order.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String advancedCustomRecordSearch(String query, String status, String sortBy, String sortOrder) {
        try {
            String safeSortBy = "createdAt";
            if (sortBy != null && !sortBy.isBlank()) {
                String lower = sortBy.toLowerCase();
                if (java.util.List.of("name", "type", "status", "createdat", "updatedat").contains(lower)) {
                    safeSortBy = lower.equals("createdat") ? "createdAt" : lower.equals("updatedat") ? "updatedAt" : lower;
                }
            }
            String safeSortOrder = (sortOrder != null && sortOrder.equalsIgnoreCase("asc")) ? "asc" : "desc";

            PagedResponse<CustomRecordDTO> result = customRecordService.listCustomRecords(0, 50, safeSortBy, safeSortOrder, query, status, null);
            if (result.getContent().isEmpty()) return "No customRecords found matching the criteria.";
            Long tenantId = TenantContext.getTenantId();
            List<Long> ids = result.getContent().stream().map(CustomRecordDTO::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.Map<String, String>> calcFields = loadTableCalcFields(tenantId, "CustomRecord", ids);
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " customRecord(s) (showing top 50, sorted by " + safeSortBy + " " + safeSortOrder + "):\n");
            result.getContent().forEach(a -> {
                sb.append("- [").append(a.getId()).append("] ").append(a.getName())
                        .append(" | Type: ").append(a.getType())
                        .append(" | Status: ").append(a.getStatus())
                        .append(" | Serial: ").append(a.getSerialNumber() != null ? a.getSerialNumber() : "N/A");
                appendTableFields(sb, a.getCustomFields(), calcFields.get(a.getId()));
                sb.append("\n");
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("advancedCustomRecordSearch failed", e);
            return "Error searching customRecords: " + e.getMessage();
        }
    }

    @Tool(description = """
            Analyze customRecords data: count by type, by status, etc.
            analysis can be: total_count, by_type, by_status, type_breakdown, status_breakdown
            Optional filters: status (e.g. 'active').
            Use this for insights like "How many customRecords do we have?" or "Show customRecords by type".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String analyzeCustomRecords(String analysis, String status, String query) {
        try {
            PagedResponse<CustomRecordDTO> result = customRecordService.listCustomRecords(0, 1000, "createdAt", "desc", query, status, null);

            if (result.getContent().isEmpty()) return "No customRecords found to analyze.";

            String analysis_type = analysis != null ? analysis.toLowerCase() : "total_count";
            java.util.List<CustomRecordDTO> customRecords = result.getContent();

            return switch(analysis_type) {
                case "total_count" -> String.format("Total customRecords: %d", result.getTotalElements());
                case "by_type" -> {
                    java.util.Map<String, Long> byType = customRecords.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomRecordDTO::getType, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("CustomRecords by type:\n");
                    byType.forEach((t, count) -> sb.append("- ").append(t).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "by_status" -> {
                    java.util.Map<String, Long> byStatus = customRecords.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomRecordDTO::getStatus, java.util.stream.Collectors.counting()));
                    StringBuilder sb = new StringBuilder("CustomRecords by status:\n");
                    byStatus.forEach((st, count) -> sb.append("- ").append(st).append(": ").append(count).append("\n"));
                    yield sb.toString();
                }
                case "type_breakdown" -> {
                    java.util.Map<String, Long> breakdown = customRecords.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomRecordDTO::getType, java.util.stream.Collectors.counting()));
                    long total = customRecords.size();
                    StringBuilder sb = new StringBuilder("CustomRecord type breakdown (total: " + total + "):\n");
                    breakdown.forEach((t, count) -> {
                        double pct = (count * 100.0) / total;
                        sb.append("- ").append(t).append(": ").append(count).append(" (").append(String.format("%.1f%%", pct)).append(")\n");
                    });
                    yield sb.toString();
                }
                case "status_breakdown" -> {
                    java.util.Map<String, Long> breakdown = customRecords.stream()
                            .collect(java.util.stream.Collectors.groupingBy(CustomRecordDTO::getStatus, java.util.stream.Collectors.counting()));
                    long total = customRecords.size();
                    StringBuilder sb = new StringBuilder("CustomRecord status breakdown (total: " + total + "):\n");
                    breakdown.forEach((st, count) -> {
                        double pct = (count * 100.0) / total;
                        sb.append("- ").append(st).append(": ").append(count).append(" (").append(String.format("%.1f%%", pct)).append(")\n");
                    });
                    yield sb.toString();
                }
                default -> "Unknown analysis type. Use: total_count, by_type, by_status, type_breakdown, or status_breakdown.";
            };
        } catch (Exception e) {
            log.error("analyzeCustomRecords failed", e);
            return "Error analyzing customRecords: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get instructions for setting up the BOCRM MCP server in Claude Desktop.
            This tool returns the configuration needed to connect Claude Desktop to this BOCRM instance via the Model Context Protocol (MCP).
            Call this when the user asks how to use BOCRM tools with Claude Desktop.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getMcpSetupInstructions() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) {
            return "Error: Tenant or user context not set. Please ensure you're logged in.";
        }
        return """
            # BOCRM MCP Server Setup for Claude Desktop

            The BOCRM backend exposes all CRM tools via the Model Context Protocol (MCP).
            You can connect Claude Desktop to your BOCRM instance to use these tools directly with long-lived API keys.

            ## Step 1: Generate an MCP API Key

            Generate a long-lived API key that will never expire (unlike JWT tokens which expire in 1 hour):

            ```
            Ask the assistant: "Generate an MCP API key called 'My MacBook Claude'"
            ```

            Copy the returned `sk-bocrm-...` key and save it somewhere safe. You'll need it for Step 2.

            ## Step 2: Configure Claude Desktop

            1. Open Claude Desktop settings (⚙️ icon)
            2. Go to **Developer** → **MCP Servers**
            3. Click **Add New Server** and paste the following config:

            ```json
            {
              "mcpServers": {
                "bocrm": {
                  "command": "node",
                  "args": ["-e", "const net = require('net'); const readline = require('readline'); const socket = net.createConnection({host: '<YOUR_BOCRM_HOST>', port: <YOUR_BOCRM_PORT>}); const rl = readline.createInterface({input: process.stdin, output: process.stdout}); rl.on('line', (line) => socket.write(line + '\\n')); socket.on('data', (data) => console.log(data.toString()));"],
                  "env": {
                    "BOCRM_API_KEY": "YOUR_API_KEY_HERE"
                  }
                }
              }
            }
            ```

            4. **Replace `YOUR_API_KEY_HERE`** with the `sk-bocrm-...` key from Step 1.
            5. Click **Save** and restart Claude Desktop.

            ## Step 3: Use BOCRM Tools in Claude

            Once configured, Claude will have access to all BOCRM tools:
            - Customer, contact, opportunity, activity, and customRecord management
            - Custom field definitions and calculated fields
            - Document generation (slide decks, one-pagers, reports, CSVs)
            - Web search tools (Wikipedia, FDA, PubMed, clinical trials, etc.)
            - Policy rule management

            Just ask Claude to work with your CRM data, and it will call the MCP tools automatically.

            ## Managing Your API Keys

            You can view and revoke your API keys:

            ```
            List keys: "List my MCP API keys"
            Revoke key: "Revoke MCP API key 1"
            ```

            ## Troubleshooting

            - **"Tenant context not set"**: Your API key is missing, invalid, or revoked. Generate a new key via Step 1.
            - **Connection refused**: Ensure BOCRM backend is running and reachable at the host/port configured above.
            - **MCP tools not appearing**: Restart Claude Desktop after updating the config.

            ## Security Note

            - Keep your API key private; it grants full CRM access.
            - API keys are long-lived and never expire automatically.
            - If your key is compromised, revoke it immediately using "Revoke MCP API key {id}".
            - For production, use environment variables or secrets management instead of embedding keys in config files.
            """;
    }

    @Tool(description = """
            Generate a new long-lived MCP API key for Claude Desktop integration.
            The key is returned once and will be used as `Authorization: Bearer sk-bocrm-...` on all MCP requests.
            Save the returned key immediately; it cannot be retrieved later.
            name: A label for this key, e.g. "My MacBook Claude" or "Work Laptop".""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateMcpApiKey(String name) {
        try {
            if (name == null || name.isBlank()) {
                return "Error: key name is required. Provide a label like 'My MacBook Claude'.";
            }
            String rawKey = mcpApiKeyService.generateApiKey(name);
            return """
                MCP API key generated successfully!

                **Key Name:** %s
                **Raw Key:** %s

                ⚠️ SAVE THIS KEY IMMEDIATELY — it will never be shown again!

                Use this key in Claude Desktop config:
                ```json
                {
                  "env": {
                    "BOCRM_API_KEY": "%s"
                  }
                }
                ```

                The key is long-lived and never expires. Revoke it if compromised using "Revoke MCP API key {id}".
                """.formatted(name, rawKey, rawKey);
        } catch (ForbiddenException e) {
            return "Error: Tenant context not set. Ensure you're logged in.";
        } catch (Exception e) {
            log.error("generateMcpApiKey failed", e);
            return "Error generating API key: " + e.getMessage();
        }
    }

    @Tool(description = """
            List all MCP API keys for the current tenant.
            Shows the key name, enabled status, when it was last used, and when it was created.
            Use the returned key ID to revoke keys via revokeMcpApiKey.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String listMcpApiKeys() {
        try {
            var keys = mcpApiKeyService.listApiKeys();
            if (keys.isEmpty()) {
                return "No MCP API keys found. Generate one with 'generateMcpApiKey'.";
            }
            StringBuilder sb = new StringBuilder("MCP API Keys:\n\n");
            for (var key : keys) {
                sb.append("- **ID:** ").append(key.getId()).append("\n");
                sb.append("  Name: ").append(key.getName()).append("\n");
                sb.append("  Status: ").append(key.isEnabled() ? "enabled" : "revoked").append("\n");
                sb.append("  Prefix: ").append(key.getKeyPrefix()).append("...\n");
                sb.append("  Last Used: ").append(key.getLastUsedAt() != null ? key.getLastUsedAt() : "never").append("\n");
                sb.append("  Created: ").append(key.getCreatedAt()).append("\n\n");
            }
            return sb.toString();
        } catch (ForbiddenException e) {
            return "Error: Tenant context not set. Ensure you're logged in.";
        } catch (Exception e) {
            log.error("listMcpApiKeys failed", e);
            return "Error listing API keys: " + e.getMessage();
        }
    }

    @Tool(description = """
            Revoke (disable) an MCP API key by its ID.
            The key will be soft-deleted and can no longer be used for authentication.
            keyId: The numeric ID from listMcpApiKeys output.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String revokeMcpApiKey(Long keyId) {
        try {
            if (keyId == null || keyId <= 0) {
                return "Error: keyId must be a positive number. Get IDs from 'List my MCP API keys'.";
            }
            mcpApiKeyService.revokeApiKey(keyId);
            return "MCP API key " + keyId + " has been revoked. It can no longer be used for authentication.";
        } catch (ForbiddenException e) {
            return "Error: Access denied. You can only revoke keys owned by your tenant.";
        } catch (Exception e) {
            log.error("revokeMcpApiKey failed", e);
            return "Error revoking API key: " + e.getMessage();
        }
    }

    // ─── Order Management Tools ───────────────────────────────────────

    @Tool(description = """
            Create a new order for a customer.
            customerId (required): the customer ID
            name (optional): order name or description
            status (optional): DRAFT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED (defaults to DRAFT)
            currency (optional): ISO-4217 code like USD (defaults to USD)
            totalAmount (optional): total order amount
            customFieldsJson (optional): JSON object with custom field values, e.g. {"fieldKey": "value"}""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createOrder(Long customerId, String name, String status, String currency, String totalAmount, String customFieldsJson) {
        try {
            if (customerId == null || customerId <= 0) return "Error: customerId is required.";
            CreateOrderRequest req = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .name(name != null ? name : "Order")
                    .status(status != null ? status : "DRAFT")
                    .currency(currency != null ? currency : "USD")
                    .build();
            if (totalAmount != null && !totalAmount.isBlank()) {
                req.setTotalAmount(new BigDecimal(totalAmount));
            }
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            OrderDTO dto = orderService.createOrder(req);
            return "Order created successfully. Order ID: " + dto.getId() + ", Status: " + dto.getStatus();
        } catch (Exception e) {
            log.error("createOrder failed", e);
            return "Error creating order: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing order.
            orderId (required): the order ID to update
            status (optional): new status
            totalAmount (optional): new total amount
            customFieldsJson (optional): JSON object to merge into custom fields""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateOrder(Long orderId, String status, String totalAmount, String customFieldsJson) {
        try {
            if (orderId == null || orderId <= 0) return "Error: orderId is required.";
            UpdateOrderRequest req = new UpdateOrderRequest();
            if (status != null && !status.isBlank()) req.setStatus(status);
            if (totalAmount != null && !totalAmount.isBlank()) req.setTotalAmount(new BigDecimal(totalAmount));
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            OrderDTO dto = orderService.updateOrder(orderId, req);
            return "Order updated successfully. Order ID: " + dto.getId() + ", Status: " + dto.getStatus();
        } catch (Exception e) {
            log.error("updateOrder failed", e);
            return "Error updating order: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get order details by ID.
            orderId (required): the order ID to retrieve""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getOrder(Long orderId) {
        try {
            if (orderId == null || orderId <= 0) return "Error: orderId is required.";
            OrderDTO dto = orderService.getOrder(orderId);
            return String.format("Order %d: Status=%s, Customer=%d, Total=$%.2f, Created=%s",
                    dto.getId(), dto.getStatus(), dto.getCustomerId(), dto.getTotalAmount(), dto.getCreatedAt());
        } catch (Exception e) {
            log.error("getOrder failed", e);
            return "Error retrieving order: " + e.getMessage();
        }
    }

    @Tool(description = """
            Search orders by status or payment terms.
            query (optional): search term (matches status or payment terms)
            page (optional): page number (0-based)
            size (optional): results per page (default 20, max 200)""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String searchOrders(String query, Integer page, Integer size) {
        try {
            int pageNum = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? Math.min(size, 200) : 20;
            PagedResponse<OrderDTO> result = orderService.search(query, pageNum, pageSize);
            if (result.getContent().isEmpty()) return "No orders found matching query: " + query;
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " orders:\n");
            for (OrderDTO o : result.getContent()) {
                sb.append("- Order ").append(o.getId()).append(": ").append(o.getStatus()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("searchOrders failed", e);
            return "Error searching orders: " + e.getMessage();
        }
    }

    // ─── Invoice Management Tools ─────────────────────────────────────

    @Tool(description = """
            Create a new invoice for a customer.
            customerId (required): the customer ID
            status (optional): DRAFT, SENT, PAID, OVERDUE, CANCELLED (defaults to DRAFT)
            currency (optional): ISO-4217 code like USD (defaults to USD)
            totalAmount (optional): total invoice amount
            paymentTerms (optional): payment terms like NET-30
            customFieldsJson (optional): JSON object with custom field values""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createInvoice(Long customerId, String status, String currency, String totalAmount, String paymentTerms, String customFieldsJson) {
        try {
            if (customerId == null || customerId <= 0) return "Error: customerId is required.";
            CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                    .customerId(customerId)
                    .status(status != null ? status : "DRAFT")
                    .currency(currency != null ? currency : "USD")
                    .paymentTerms(paymentTerms)
                    .build();
            if (totalAmount != null && !totalAmount.isBlank()) {
                req.setTotalAmount(new BigDecimal(totalAmount));
            }
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            InvoiceDTO dto = invoiceService.createInvoice(req);
            return "Invoice created successfully. Invoice ID: " + dto.getId() + ", Status: " + dto.getStatus();
        } catch (Exception e) {
            log.error("createInvoice failed", e);
            return "Error creating invoice: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing invoice.
            invoiceId (required): the invoice ID to update
            status (optional): new status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
            totalAmount (optional): new total amount
            paymentTerms (optional): new payment terms
            customFieldsJson (optional): JSON object to merge into custom fields""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateInvoice(Long invoiceId, String status, String totalAmount, String paymentTerms, String customFieldsJson) {
        try {
            if (invoiceId == null || invoiceId <= 0) return "Error: invoiceId is required.";
            UpdateInvoiceRequest req = new UpdateInvoiceRequest();
            if (status != null && !status.isBlank()) req.setStatus(status);
            if (totalAmount != null && !totalAmount.isBlank()) req.setTotalAmount(new BigDecimal(totalAmount));
            if (paymentTerms != null && !paymentTerms.isBlank()) req.setPaymentTerms(paymentTerms);
            if (customFieldsJson != null && !customFieldsJson.isBlank()) {
                req.setCustomFields(objectMapper.readTree(customFieldsJson));
            }
            InvoiceDTO dto = invoiceService.updateInvoice(invoiceId, req);
            return "Invoice updated successfully. Invoice ID: " + dto.getId() + ", Status: " + dto.getStatus();
        } catch (Exception e) {
            log.error("updateInvoice failed", e);
            return "Error updating invoice: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get invoice details by ID.
            invoiceId (required): the invoice ID to retrieve""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getInvoice(Long invoiceId) {
        try {
            if (invoiceId == null || invoiceId <= 0) return "Error: invoiceId is required.";
            InvoiceDTO dto = invoiceService.getInvoice(invoiceId);
            return String.format("Invoice %d: Status=%s, Customer=%d, Total=$%.2f, Due=%s, Created=%s",
                    dto.getId(), dto.getStatus(), dto.getCustomerId(), dto.getTotalAmount(), dto.getDueDate(), dto.getCreatedAt());
        } catch (Exception e) {
            log.error("getInvoice failed", e);
            return "Error retrieving invoice: " + e.getMessage();
        }
    }

    @Tool(description = """
            Search invoices by status or payment terms.
            query (optional): search term (matches status or payment terms)
            page (optional): page number (0-based)
            size (optional): results per page (default 20, max 200)""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String searchInvoices(String query, Integer page, Integer size) {
        try {
            int pageNum = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? Math.min(size, 200) : 20;
            PagedResponse<InvoiceDTO> result = invoiceService.search(query, pageNum, pageSize);
            if (result.getContent().isEmpty()) return "No invoices found matching query: " + query;
            StringBuilder sb = new StringBuilder("Found " + result.getTotalElements() + " invoices:\n");
            for (InvoiceDTO i : result.getContent()) {
                sb.append("- Invoice ").append(i.getId()).append(": ").append(i.getStatus()).append(", Due: ").append(i.getDueDate()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("searchInvoices failed", e);
            return "Error searching invoices: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get all details for a specific CRM record: core fields, every custom field (with labels), and all calculated fields.
            Use this whenever the user asks for full details or a specific field value on a single record.
            entityType must be one of: Customer, Contact, Opportunity, Activity, CustomRecord
            id is the numeric record ID.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getEntityDetails(String entityType, Long id) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) return "Error: tenant context not set.";
            if (entityType == null || entityType.isBlank() || id == null || id <= 0)
                return "Error: entityType and a valid id are required.";

            String type = capitalizeEntityType(entityType.trim());
            StringBuilder sb = new StringBuilder();
            JsonNode customFields = null;

            switch (type) {
                case "Opportunity" -> {
                    OpportunityDTO dto = opportunityService.getOpportunity(id);
                    sb.append("Opportunity #").append(dto.getId()).append(" — ").append(dto.getName()).append("\n");
                    sb.append("  Stage: ").append(dto.getStage()).append("\n");
                    sb.append("  Value: $").append(dto.getValue() != null ? dto.getValue() : "N/A").append("\n");
                    sb.append("  Probability: ").append(dto.getProbability() != null ? dto.getProbability() + "%" : "N/A").append("\n");
                    sb.append("  Close Date: ").append(dto.getCloseDate() != null ? dto.getCloseDate() : "N/A").append("\n");
                    sb.append("  Customer: ").append(resolveCustomerName(dto.getCustomerId())).append("\n");
                    sb.append("  Owner: ").append(resolveOwnerName(dto.getOwnerId())).append("\n");
                    sb.append("  Created: ").append(dto.getCreatedAt()).append("\n");
                    sb.append("  Updated: ").append(dto.getUpdatedAt()).append("\n");
                    customFields = dto.getCustomFields();
                }
                case "Customer" -> {
                    CustomerDTO dto = customerService.getCustomer(id);
                    sb.append("Customer #").append(dto.getId()).append(" — ").append(dto.getName()).append("\n");
                    sb.append("  Status: ").append(dto.getStatus()).append("\n");
                    sb.append("  Owner: ").append(resolveOwnerName(dto.getOwnerId())).append("\n");
                    sb.append("  Created: ").append(dto.getCreatedAt()).append("\n");
                    sb.append("  Updated: ").append(dto.getUpdatedAt()).append("\n");
                    customFields = dto.getCustomFields();
                }
                case "Contact" -> {
                    ContactDTO dto = contactService.getContact(id);
                    sb.append("Contact #").append(dto.getId()).append(" — ").append(dto.getName()).append("\n");
                    sb.append("  Email: ").append(dto.getEmail() != null ? dto.getEmail() : "N/A").append("\n");
                    sb.append("  Phone: ").append(dto.getPhone() != null ? dto.getPhone() : "N/A").append("\n");
                    sb.append("  Title: ").append(dto.getTitle() != null ? dto.getTitle() : "N/A").append("\n");
                    sb.append("  Status: ").append(dto.getStatus()).append("\n");
                    sb.append("  Customer: ").append(resolveCustomerName(dto.getCustomerId())).append("\n");
                    sb.append("  Created: ").append(dto.getCreatedAt()).append("\n");
                    sb.append("  Updated: ").append(dto.getUpdatedAt()).append("\n");
                    customFields = dto.getCustomFields();
                }
                case "Activity" -> {
                    ActivityDTO dto = activityService.getActivity(id);
                    sb.append("Activity #").append(dto.getId()).append(" — ").append(dto.getSubject()).append("\n");
                    sb.append("  Type: ").append(dto.getType()).append("\n");
                    sb.append("  Status: ").append(dto.getStatus()).append("\n");
                    sb.append("  Due: ").append(dto.getDueAt() != null ? dto.getDueAt() : "N/A").append("\n");
                    sb.append("  Description: ").append(dto.getDescription() != null ? dto.getDescription() : "N/A").append("\n");
                    if (dto.getRelatedType() != null && dto.getRelatedId() != null) {
                        String relatedName = "customer".equalsIgnoreCase(dto.getRelatedType())
                                ? resolveCustomerName(dto.getRelatedId())
                                : "#" + dto.getRelatedId();
                        sb.append("  Related ").append(dto.getRelatedType()).append(": ").append(relatedName).append("\n");
                    }
                    sb.append("  Created: ").append(dto.getCreatedAt()).append("\n");
                    sb.append("  Updated: ").append(dto.getUpdatedAt()).append("\n");
                    customFields = dto.getCustomFields();
                }
                case "CustomRecord" -> {
                    CustomRecordDTO dto = customRecordService.getCustomRecord(id);
                    sb.append("CustomRecord #").append(dto.getId()).append(" — ").append(dto.getName()).append("\n");
                    sb.append("  Type: ").append(dto.getType()).append("\n");
                    sb.append("  Status: ").append(dto.getStatus()).append("\n");
                    sb.append("  Serial: ").append(dto.getSerialNumber() != null ? dto.getSerialNumber() : "N/A").append("\n");
                    sb.append("  Notes: ").append(dto.getNotes() != null ? dto.getNotes() : "N/A").append("\n");
                    sb.append("  Customer: ").append(resolveCustomerName(dto.getCustomerId())).append("\n");
                    sb.append("  Owner: ").append(resolveOwnerName(dto.getOwnerId())).append("\n");
                    sb.append("  Created: ").append(dto.getCreatedAt()).append("\n");
                    sb.append("  Updated: ").append(dto.getUpdatedAt()).append("\n");
                    customFields = dto.getCustomFields();
                }
                default -> { return "Unknown entityType '" + entityType + "'. Use: Customer, Contact, Opportunity, Activity, CustomRecord."; }
            }

            // Build a unified fields list: custom fields (labeled) + calculated fields, in one block
            java.util.List<String[]> allFields = new java.util.ArrayList<>();

            if (customFields != null && !customFields.isEmpty()) {
                java.util.Map<String, String> keyToLabel = customFieldDefinitionService.getFieldDefinitions(type)
                        .stream().collect(java.util.stream.Collectors.toMap(
                                CustomFieldDefinitionDTO::getKey, CustomFieldDefinitionDTO::getLabel,
                                (a, b) -> a));
                customFields.properties().forEach(entry -> {
                    JsonNode v = entry.getValue();
                    String label = keyToLabel.getOrDefault(entry.getKey(), entry.getKey());
                    String display = v.isNull() ? "N/A" : v.isTextual() ? v.asText() : v.toString();
                    allFields.add(new String[]{label, display});
                });
            }

            List<com.bocrm.backend.entity.CalculatedFieldDefinition> calcDefs =
                    calculatedFieldDefinitionRepository.findByTenantIdAndEntityTypeIgnoreCase(tenantId, type)
                            .stream().filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                            .collect(java.util.stream.Collectors.toList());
            if (!calcDefs.isEmpty()) {
                java.util.Map<Long, String> calcLabelById = calcDefs.stream().collect(java.util.stream.Collectors.toMap(
                        com.bocrm.backend.entity.CalculatedFieldDefinition::getId,
                        com.bocrm.backend.entity.CalculatedFieldDefinition::getLabel));
                calculatedFieldValueRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, type, id)
                        .forEach(val -> {
                            String label = calcLabelById.get(val.getCalculatedFieldId());
                            if (label != null) {
                                allFields.add(new String[]{label, parseCalcDisplayValue(val.getValueJsonb())});
                            }
                        });
            }

            if (!allFields.isEmpty()) {
                sb.append("\nFields:\n");
                allFields.forEach(pair -> sb.append("  ").append(pair[0]).append(": ").append(pair[1]).append("\n"));
            }

            return sb.toString();
        } catch (com.bocrm.backend.exception.ResourceNotFoundException e) {
            return entityType + " #" + id + " not found.";
        } catch (com.bocrm.backend.exception.ForbiddenException e) {
            return "Access denied to " + entityType + " #" + id + ".";
        } catch (Exception e) {
            log.error("getEntityDetails failed for {} {}", entityType, id, e);
            return "Error getting details: " + e.getMessage();
        }
    }

    private String capitalizeEntityType(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String resolveCustomerName(Long customerId) {
        if (customerId == null) return "N/A";
        try {
            return customerRepository.findById(customerId)
                    .map(c -> c.getName() + " (#" + customerId + ")")
                    .orElse("#" + customerId);
        } catch (Exception e) {
            return "#" + customerId;
        }
    }

    private String resolveOwnerName(Long ownerId) {
        if (ownerId == null) return "N/A";
        try {
            return userRepository.findById(ownerId)
                    .map(u -> {
                        String name = u.getDisplayName() != null && !u.getDisplayName().isBlank()
                                ? u.getDisplayName() : u.getEmail();
                        return name + " (#" + ownerId + ")";
                    })
                    .orElse("#" + ownerId);
        } catch (Exception e) {
            return "#" + ownerId;
        }
    }

}
