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
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ValidationException;
import com.bocrm.backend.service.CalculatedFieldDefinitionService;
import com.bocrm.backend.service.CustomFieldDefinitionService;
import com.bocrm.backend.service.NotificationTemplateService;
import com.bocrm.backend.service.TenantAdminService;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI tool methods for tenant admin operations.
 * Each method checks that the caller has ROLE_ADMIN as a defense-in-depth guard.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class AdminTools {

    private final CustomFieldDefinitionService customFieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final TenantAdminService tenantAdminService;
    private final NotificationTemplateService notificationTemplateService;
    private final ObjectMapper objectMapper;

    public AdminTools(CustomFieldDefinitionService customFieldDefinitionService,
                      CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                      TenantAdminService tenantAdminService,
                      NotificationTemplateService notificationTemplateService,
                      ObjectMapper objectMapper) {
        this.customFieldDefinitionService = customFieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.tenantAdminService = tenantAdminService;
        this.notificationTemplateService = notificationTemplateService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "List all custom field definitions for an entity type. entityType must be one of: Customer, Contact, Opportunity, Activity. Shows id, key, type, label, required, and options for select fields.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String listCustomFields(String entityType) {
        requireAdmin();
        try {
            List<CustomFieldDefinitionDTO> fields = customFieldDefinitionService.getFieldDefinitions(entityType);
            if (fields.isEmpty()) return "No custom fields defined for " + entityType + ".";
            StringBuilder sb = new StringBuilder("Custom fields for ").append(entityType).append(":\n");
            for (CustomFieldDefinitionDTO f : fields) {
                sb.append("- [").append(f.getId()).append("] ")
                  .append(f.getKey()).append(" (").append(f.getType()).append("): ")
                  .append(f.getLabel());
                if (Boolean.TRUE.equals(f.getRequired())) sb.append(" [required]");
                if ("select".equals(f.getType()) && f.getConfig() != null && f.getConfig().has("options")) {
                    sb.append(" | options: ").append(f.getConfig().get("options").toString());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("listCustomFields failed", e);
            return "Error listing custom fields: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new custom field definition. entityType must be Customer/Contact/Opportunity/Activity. fieldType must be one of: text, number, date, boolean, select, textarea, multiselect, url, email, phone, currency, percentage, richtext, workflow. For select and multiselect fields, provide options as a comma-separated string (e.g. 'Option A,Option B,Option C'). For workflow fields, provide milestones as a comma-separated string of milestone names (e.g. 'Lead,Prospect,Meeting,Proposal,Won'). Set required=true to make it mandatory. Set displayInTable=true to show the field as a column in the entity list table.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createCustomField(String entityType, String key, String label, String fieldType, Boolean required, String options, String milestones, Boolean displayInTable) {
        requireAdmin();
        try {
            CreateCustomFieldDefinitionRequest req = CreateCustomFieldDefinitionRequest.builder()
                    .entityType(entityType)
                    .key(key)
                    .label(label)
                    .type(fieldType)
                    .required(required != null && required)
                    .config(buildConfigNode(options, milestones, fieldType))
                    .displayInTable(displayInTable)
                    .build();
            CustomFieldDefinitionDTO created = customFieldDefinitionService.createFieldDefinition(req);
            return "Custom field created: id=" + created.getId() + ", key=" + created.getKey()
                    + " on " + created.getEntityType();
        } catch (ValidationException e) {
            // Field already exists - skip gracefully without logging stack trace
            if (e.getMessage().contains("already exists")) {
                return "Field '" + key + "' already exists on " + entityType + " (skipped).";
            }
            log.warn("Validation error creating custom field: {}", e.getMessage());
            return "Validation error: " + e.getMessage();
        } catch (Exception e) {
            log.error("createCustomField failed", e);
            return "Error creating custom field: " + e.getMessage();
        }
    }

    @Tool(description = """
            Bulk-create multiple custom field definitions in a single call. Use this instead of calling createCustomField repeatedly — it is far more efficient.
            fieldsJson is a JSON array where each element has:
              entityType (required) — Customer | Contact | Opportunity | Activity | CustomRecord
              key        (required) — snake_case identifier, must be unique per entityType
              label      (required) — display name
              fieldType  (required) — text | number | date | boolean | select | textarea | multiselect | url | email | phone | currency | percentage | richtext | workflow
              required   (optional, default false)
              options    (optional) — comma-separated values for select/multiselect, e.g. "Low,Medium,High"
              milestones (optional) — comma-separated names for workflow, e.g. "Scoping,SOW Signed,Kickoff,In Progress,Delivered"
              displayInTable (optional, default false)
            Example: [{"entityType":"Customer","key":"region","label":"Region","fieldType":"select","options":"NA,EMEA,APAC"},{"entityType":"Customer","key":"nps_score","label":"NPS Score","fieldType":"number"}]
            Returns a summary of created and skipped fields.""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String bulkCreateCustomFields(String fieldsJson) {
        requireAdmin();
        try {
            tools.jackson.databind.JsonNode arr = objectMapper.readTree(fieldsJson);
            if (!arr.isArray()) return "Error: fieldsJson must be a JSON array";
            int created = 0, skipped = 0;
            StringBuilder errors = new StringBuilder();
            for (tools.jackson.databind.JsonNode f : arr) {
                String entityType = f.path("entityType").asString("");
                String key        = f.path("key").asString("");
                String label      = f.path("label").asString("");
                String fieldType  = f.path("fieldType").asString("");
                boolean required  = f.path("required").asBoolean(false);
                String options    = f.path("options").asString(null);
                String milestones = f.path("milestones").asString(null);
                Boolean displayInTable = f.has("displayInTable") ? f.path("displayInTable").asBoolean(false) : null;
                try {
                    CreateCustomFieldDefinitionRequest req = CreateCustomFieldDefinitionRequest.builder()
                            .entityType(entityType).key(key).label(label).type(fieldType)
                            .required(required).config(buildConfigNode(options, milestones, fieldType))
                            .displayInTable(displayInTable).build();
                    customFieldDefinitionService.createFieldDefinition(req);
                    created++;
                } catch (ValidationException e) {
                    if (e.getMessage().contains("already exists")) { skipped++; }
                    else { errors.append(key).append(": ").append(e.getMessage()).append("; "); }
                } catch (Exception e) {
                    errors.append(key).append(": ").append(e.getMessage()).append("; ");
                }
            }
            String result = "Bulk custom fields: " + created + " created, " + skipped + " skipped";
            return errors.length() > 0 ? result + ". Errors: " + errors : result;
        } catch (Exception e) {
            log.error("bulkCreateCustomFields failed", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing custom field definition by its id. Only provided fields are changed. For select/multiselect fields, provide options as a comma-separated string to replace the current options list (e.g. 'Option A,Option B,Option C'). For workflow fields, provide milestones as a comma-separated string to replace the current milestone list (e.g. 'Lead,Prospect,Meeting,Proposal,Won'). Use displayInTable to control whether the field appears as a column in the entity list table.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateCustomField(Long fieldId, String label, Boolean required, String options, String milestones, Boolean displayInTable) {
        requireAdmin();
        try {
            UpdateCustomFieldDefinitionRequest req = UpdateCustomFieldDefinitionRequest.builder()
                    .label(label)
                    .required(required)
                    .config(buildConfigNode(options, milestones, milestones != null && !milestones.isBlank() ? "workflow" : null))
                    .displayInTable(displayInTable)
                    .build();
            CustomFieldDefinitionDTO updated = customFieldDefinitionService.updateFieldDefinition(fieldId, req);
            return "Custom field updated: id=" + updated.getId() + ", label=" + updated.getLabel();
        } catch (Exception e) {
            log.error("updateCustomField failed", e);
            return "Error updating custom field: " + e.getMessage();
        }
    }

    private tools.jackson.databind.JsonNode buildConfigNode(String options, String milestones, String fieldType) {
        boolean isWorkflow = "workflow".equals(fieldType);
        if (isWorkflow) {
            if (milestones == null || milestones.isBlank()) return null;
            ObjectNode config = objectMapper.createObjectNode();
            ArrayNode arr = objectMapper.createArrayNode();
            Arrays.stream(milestones.split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .forEach(arr::add);
            config.set("milestones", arr);
            return config;
        }
        if (options == null || options.isBlank()) return null;
        ObjectNode config = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        Arrays.stream(options.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .forEach(arr::add);
        config.set("options", arr);
        return config;
    }

    @Tool(description = "Delete a custom field definition by its id. This will remove the field from all future records.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteCustomField(Long fieldId) {
        requireAdmin();
        try {
            customFieldDefinitionService.deleteFieldDefinition(fieldId);
            return "Custom field " + fieldId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteCustomField failed", e);
            return "Error deleting custom field: " + e.getMessage();
        }
    }

    @Tool(description = "List all calculated field definitions for an entity type. entityType must be one of: Customer, Contact, Opportunity, Activity. Shows id, key, label, expression, returnType, enabled status, and displayInTable status.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String listCalculatedFields(String entityType) {
        requireAdmin();
        try {
            List<CalculatedFieldDefinitionDTO> fields = calculatedFieldDefinitionService.getDefinitions(entityType);
            if (fields.isEmpty()) return "No calculated fields defined for " + entityType + ".";
            StringBuilder sb = new StringBuilder("Calculated fields for ").append(entityType).append(":\n");
            for (CalculatedFieldDefinitionDTO f : fields) {
                sb.append("- [").append(f.getId()).append("] ")
                  .append(f.getKey()).append(" (").append(f.getReturnType()).append("): ")
                  .append(f.getLabel())
                  .append(" | expression: ").append(f.getExpression())
                  .append(f.getEnabled() ? " [enabled]" : " [disabled]")
                  .append(Boolean.TRUE.equals(f.getDisplayInTable()) ? " [shown in table]" : " [hidden from table]")
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("listCalculatedFields failed", e);
            return "Error listing calculated fields: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new calculated field definition. entityType must be Customer/Contact/Opportunity/Activity. returnType must be text/number/boolean/date. expression is a SpEL expression. Standard entity fields (name, status, value, closeDate, etc.) and custom field keys are both available as bare variables — e.g. 'upfront_payment * pos_percentage / 100', 'value * probability / 100', 'name.toUpperCase()', 'status == \"active\"'. Use the exact key names of the custom fields in the expression, no prefix needed.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createCalculatedField(String entityType, String key, String label, String expression, String returnType) {
        requireAdmin();
        try {
            String resolvedKey = calculatedFieldDefinitionService.resolveUniqueKey(entityType, key);
            CreateCalculatedFieldDefinitionRequest req = CreateCalculatedFieldDefinitionRequest.builder()
                    .entityType(entityType)
                    .key(resolvedKey)
                    .label(label)
                    .expression(expression)
                    .returnType(returnType)
                    .enabled(true)
                    .build();
            CalculatedFieldDefinitionDTO created = calculatedFieldDefinitionService.createDefinition(req);
            String result = "Calculated field created: id=" + created.getId() + ", key=" + created.getKey()
                    + " on " + created.getEntityType();
            if (!resolvedKey.equals(key)) {
                result += " (key '" + key + "' already existed — used '" + resolvedKey + "' instead)";
            }
            return result;
        } catch (ValidationException e) {
            // Validation error - return friendly message without stack trace
            log.debug("Validation error creating calculated field: {}", e.getMessage());
            return "Validation error: " + e.getMessage();
        } catch (Exception e) {
            log.error("createCalculatedField failed", e);
            return "Error creating calculated field: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing calculated field definition by its id. Only provided fields are changed. Pass enabled=false to disable without deleting. Pass displayInTable=true to show the column in list tables (triggers a full tenant recalculation), or displayInTable=false to hide it.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateCalculatedField(Long fieldId, String label, String expression, Boolean enabled, Boolean displayInTable) {
        requireAdmin();
        try {
            UpdateCalculatedFieldDefinitionRequest req = UpdateCalculatedFieldDefinitionRequest.builder()
                    .label(label)
                    .expression(expression)
                    .enabled(enabled)
                    .displayInTable(displayInTable)
                    .build();
            CalculatedFieldDefinitionDTO updated = calculatedFieldDefinitionService.updateDefinition(fieldId, req);
            return "Calculated field updated: id=" + updated.getId() + ", label=" + updated.getLabel()
                    + ", displayInTable=" + updated.getDisplayInTable();
        } catch (Exception e) {
            log.error("updateCalculatedField failed", e);
            return "Error updating calculated field: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a calculated field definition by its id. The field will no longer appear in entity view drawers.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String deleteCalculatedField(Long fieldId) {
        requireAdmin();
        try {
            calculatedFieldDefinitionService.deleteDefinition(fieldId);
            return "Calculated field " + fieldId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteCalculatedField failed", e);
            return "Error deleting calculated field: " + e.getMessage();
        }
    }

    @Tool(description = "Get the current tenant settings as JSON.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getTenantSettings() {
        requireAdmin();
        try {
            Long tenantId = TenantContext.getTenantId();
            var settings = tenantAdminService.getSettings(tenantId);
            return "Tenant settings: " + objectMapper.writeValueAsString(settings);
        } catch (Exception e) {
            log.error("getTenantSettings failed", e);
            return "Error getting tenant settings: " + e.getMessage();
        }
    }

    @Tool(description = "Set the tenant logo by downloading an image from a public URL. " +
            "Provide a publicly accessible http or https URL pointing to a PNG, JPEG, SVG, WebP, or GIF image. " +
            "The server will download, validate (must be an image, max 512 KB), and store it as the tenant logo. " +
            "Private/internal URLs are blocked. Ask the user for the URL if they want to set a logo.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String setLogoFromUrl(String imageUrl) {
        requireAdmin();
        try {
            Long tenantId = TenantContext.getTenantId();
            tenantAdminService.setLogoFromUrl(tenantId, imageUrl);
            return "Logo updated successfully from: " + imageUrl;
        } catch (Exception e) {
            log.error("setLogoFromUrl failed", e);
            return "Error setting logo: " + e.getMessage();
        }
    }

    @Tool(description = "Update one or more tenant settings. Provide only the keys you want to change — existing keys are preserved. " +
            "Supported keys: primaryColor (hex color string, e.g. '#FF5733'), language ('en'/'fr'/'es'), " +
            "orgBio (free-text description of the organization used to contextualize all AI assistant queries). " +
            "Logo upload is handled through the UI and cannot be set here. " +
            "Example: to set the company bio pass orgBio='We are a pharma company focused on oncology sales'.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateTenantSettings(String primaryColor, String language, String orgBio) {
        requireAdmin();
        try {
            Long tenantId = TenantContext.getTenantId();
            // Load existing settings so we only patch provided keys
            tools.jackson.databind.JsonNode existing = tenantAdminService.getSettings(tenantId);
            ObjectNode merged = existing instanceof ObjectNode on ? on : objectMapper.createObjectNode();
            if (primaryColor != null && !primaryColor.isBlank()) merged.put("primaryColor", primaryColor);
            if (language    != null && !language.isBlank())      merged.put("language",     language);
            if (orgBio      != null && !orgBio.isBlank())        merged.put("orgBio",        orgBio);
            tenantAdminService.updateTenant(tenantId, null, merged);
            return "Tenant settings updated: " + objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            log.error("updateTenantSettings failed", e);
            return "Error updating tenant settings: " + e.getMessage();
        }
    }

    @Tool(description = "Update display labels for entity types. Pass null to keep the current label. entityType keys: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateEntityLabels(String customerLabel, String contactLabel, String opportunityLabel, String activityLabel, String customRecordLabel, String orderLabel, String invoiceLabel) {
        requireAdmin();
        try {
            Long tenantId = TenantContext.getTenantId();
            tools.jackson.databind.JsonNode existing = tenantAdminService.getSettings(tenantId);
            ObjectNode merged = existing instanceof ObjectNode on ? on : objectMapper.createObjectNode();
            ObjectNode entityLabels = merged.has("entityLabels") && merged.get("entityLabels").isObject()
                ? (ObjectNode) merged.get("entityLabels")
                : objectMapper.createObjectNode();
            if (customerLabel != null && !customerLabel.isBlank()) entityLabels.put("Customer", customerLabel);
            if (contactLabel != null && !contactLabel.isBlank()) entityLabels.put("Contact", contactLabel);
            if (opportunityLabel != null && !opportunityLabel.isBlank()) entityLabels.put("Opportunity", opportunityLabel);
            if (activityLabel != null && !activityLabel.isBlank()) entityLabels.put("Activity", activityLabel);
            if (customRecordLabel != null && !customRecordLabel.isBlank()) entityLabels.put("CustomRecord", customRecordLabel);
            if (orderLabel != null && !orderLabel.isBlank()) entityLabels.put("Order", orderLabel);
            if (invoiceLabel != null && !invoiceLabel.isBlank()) entityLabels.put("Invoice", invoiceLabel);
            merged.set("entityLabels", entityLabels);
            tenantAdminService.updateTenant(tenantId, null, merged);
            return "Entity labels updated: " + objectMapper.writeValueAsString(entityLabels);
        } catch (Exception e) {
            log.error("updateEntityLabels failed", e);
            return "Error updating entity labels: " + e.getMessage();
        }
    }

    @Tool(description = "List all notification/email templates for the tenant. Returns id, name, notificationType, subjectTemplate, bodyTemplate, and isActive for each template.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String listNotificationTemplates() {
        requireAdmin();
        try {
            List<NotificationTemplateDTO> templates = notificationTemplateService.listAllForTenant();
            if (templates.isEmpty()) return "No notification templates defined yet.";
            StringBuilder sb = new StringBuilder("Notification templates:\n");
            for (NotificationTemplateDTO t : templates) {
                sb.append("- [").append(t.getId()).append("] ")
                  .append(t.getName()).append(" | type: ").append(t.getNotificationType())
                  .append(t.getIsActive() ? " [active]" : " [inactive]").append("\n")
                  .append("  subject: ").append(t.getSubjectTemplate()).append("\n")
                  .append("  body: ").append(t.getBodyTemplate()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("listNotificationTemplates failed", e);
            return "Error listing notification templates: " + e.getMessage();
        }
    }

    @Tool(description = "Create a notification/email template. notificationType is a free-form string matching the event (e.g. OPPORTUNITY_CREATED, CUSTOMER_UPDATED, ACTIVITY_DUE_SOON, ORDER_CREATED, INVOICE_CREATED). name is a human-readable label. subjectTemplate and bodyTemplate support {{placeholder}} syntax for variable substitution (e.g. {{customerName}}, {{amount}}, {{dueDate}}, {{assignee}}, {{link}}). isActive=true to activate immediately.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createNotificationTemplate(String notificationType, String name, String subjectTemplate, String bodyTemplate, Boolean isActive) {
        requireAdmin();
        try {
            CreateNotificationTemplateRequest req = CreateNotificationTemplateRequest.builder()
                    .notificationType(notificationType)
                    .name(name)
                    .subjectTemplate(subjectTemplate)
                    .bodyTemplate(bodyTemplate)
                    .isActive(isActive != null ? isActive : true)
                    .build();
            NotificationTemplateDTO created = notificationTemplateService.create(req);
            return "Notification template created: id=" + created.getId() + ", name=" + created.getName()
                    + ", type=" + created.getNotificationType();
        } catch (Exception e) {
            log.error("createNotificationTemplate failed", e);
            return "Error creating notification template: " + e.getMessage();
        }
    }

    // Defense-in-depth: verify the caller is ROLE_ADMIN via Spring Security
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_SYSTEM_ADMIN".equals(a));
        if (!isAdmin) {
            throw new ForbiddenException("Admin role required for this operation");
        }
    }
}
