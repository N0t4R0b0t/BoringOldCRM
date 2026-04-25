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
import com.bocrm.backend.service.*;
import com.bocrm.backend.service.DocumentDownloadTokenService;
import com.bocrm.backend.service.DocumentTemplateService;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Spring AI tool methods for generating documents (reports, one-pagers, slide decks, CSVs).
 * Available to all users (not just admins).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class DocumentGenerationTools {

    private final OpportunityService opportunityService;
    private final CustomerService customerService;
    private final ActivityService activityService;
    private final ContactService contactService;
    private final CustomRecordService customRecordService;
    private final TenantDocumentService tenantDocumentService;
    private final CustomFieldDefinitionService customFieldDefinitionService;
    private final CalculatedFieldDefinitionService calculatedFieldDefinitionService;
    private final DocumentTemplateService documentTemplateService;
    private final TenantAdminService tenantAdminService;
    private final DocumentDownloadTokenService downloadTokenService;
    private final ObjectMapper objectMapper;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    /** TTL for signed download URLs emitted in tool responses. */
    private static final java.time.Duration DOWNLOAD_URL_TTL = java.time.Duration.ofHours(24);

    public DocumentGenerationTools(OpportunityService opportunityService,
                                    CustomerService customerService,
                                    ActivityService activityService,
                                    ContactService contactService,
                                    CustomRecordService customRecordService,
                                    TenantDocumentService tenantDocumentService,
                                    CustomFieldDefinitionService customFieldDefinitionService,
                                    CalculatedFieldDefinitionService calculatedFieldDefinitionService,
                                    DocumentTemplateService documentTemplateService,
                                    TenantAdminService tenantAdminService,
                                    DocumentDownloadTokenService downloadTokenService,
                                    ObjectMapper objectMapper) {
        this.opportunityService = opportunityService;
        this.customerService = customerService;
        this.activityService = activityService;
        this.contactService = contactService;
        this.customRecordService = customRecordService;
        this.tenantDocumentService = tenantDocumentService;
        this.customFieldDefinitionService = customFieldDefinitionService;
        this.calculatedFieldDefinitionService = calculatedFieldDefinitionService;
        this.documentTemplateService = documentTemplateService;
        this.tenantAdminService = tenantAdminService;
        this.downloadTokenService = downloadTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the tool response for a newly generated document.
     * The in-app chat sidebar intercepts the relative `[⬇ Download]` link for an authenticated XHR download.
     * When {@code app.public-base-url} is configured, an absolute signed URL is appended so remote MCP clients
     * (e.g. Claude Desktop) can pull the file directly from a browser without a BOCRM session — the signature
     * authenticates the download, and the token carries a short TTL.
     * The closing instruction discourages the LLM from paraphrasing or dropping the link.
     */
    private String documentResultMarkdown(TenantDocumentDTO doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generated **").append(doc.getName()).append("**\n\n");
        sb.append("- In-app: [⬇ Download](/documents/").append(doc.getId()).append("/download)\n");
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            Long tenantId = TenantContext.getTenantId();
            Long userId = TenantContext.getUserId();
            if (tenantId != null && userId != null) {
                String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
                String token = downloadTokenService.sign(tenantId, userId, doc.getId(), DOWNLOAD_URL_TTL);
                sb.append("- Direct download: ").append(base).append("/api/documents/")
                        .append(doc.getId()).append("/file?t=").append(token).append("\n");
            }
        }
        sb.append("\nIMPORTANT: Present these links to the user verbatim. Do not paraphrase or omit the URLs.");
        return sb.toString();
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    /**
     * Parsed style options for slide decks and one-pagers.
     * All fields have sensible defaults; only non-null values override.
     */
    private record SlideStyle(
            String backgroundColor,
            String slideBackground,
            String textColor,
            String h1Color,
            String h2Color,
            String accentColor,
            String buttonHoverColor,
            String fontFamily,
            Set<String> includeFields,  // null = include all
            Set<String> excludeFields,
            String customCss,           // injected verbatim at end of <style> block; null = none
            String logoPlacement        // "none" | "title" | "header" | "footer"
    ) {
        static SlideStyle dark() {
            return new SlideStyle("#1a1a2e", "#16213e", "#eee", "#e0e0ff", "#a0a0ff",
                    "#533483", "#0f3460", "sans-serif", null, Set.of(), null, "title");
        }
        static SlideStyle light() {
            return new SlideStyle("#f5f5f5", "#ffffff", "#222", "#1a1a2e", "#333399",
                    "#3355cc", "#1a3ab8", "sans-serif", null, Set.of(), null, "title");
        }
        static SlideStyle corporate() {
            return new SlideStyle("#1c2840", "#243050", "#dde4f0", "#ffffff", "#a8c0e0",
                    "#2e6da4", "#1a4a7a", "Georgia, serif", null, Set.of(), null, "title");
        }
        static SlideStyle minimal() {
            return new SlideStyle("#ffffff", "#ffffff", "#111", "#000", "#555",
                    "#111", "#444", "sans-serif", null, Set.of(), null, "title");
        }

        boolean includes(String field) {
            if (includeFields != null && !includeFields.isEmpty()) return includeFields.contains(field);
            return !excludeFields.contains(field);
        }

        /** Case-insensitive match against either the field label or its key (for custom/calculated fields). */
        boolean includesField(String label, String key) {
            if (includeFields != null && !includeFields.isEmpty()) {
                return includeFields.stream().anyMatch(f -> f.equalsIgnoreCase(label) || f.equalsIgnoreCase(key));
            }
            return excludeFields.stream().noneMatch(f -> f.equalsIgnoreCase(label) || f.equalsIgnoreCase(key));
        }

        boolean showLogoOnTitle() { return !"none".equals(logoPlacement); }
        boolean showLogoOnHeader() { return "header".equals(logoPlacement); }
        boolean showLogoOnFooter() { return "footer".equals(logoPlacement); }
    }

    private SlideStyle parseSlideStyle(String styleJson) {
        if (styleJson == null || styleJson.isBlank()) return SlideStyle.dark();
        try {
            JsonNode node = objectMapper.readTree(styleJson);
            String layout = node.has("layout") ? node.get("layout").asText() : "dark";
            SlideStyle base = switch (layout) {
                case "light"     -> SlideStyle.light();
                case "corporate" -> SlideStyle.corporate();
                case "minimal"   -> SlideStyle.minimal();
                default          -> SlideStyle.dark();
            };

            String bg        = node.has("backgroundColor") ? node.get("backgroundColor").asText() : base.backgroundColor();
            String slideBg   = node.has("slideBackground")  ? node.get("slideBackground").asText()  : base.slideBackground();
            String text      = node.has("textColor")        ? node.get("textColor").asText()        : base.textColor();
            String h1        = node.has("h1Color")          ? node.get("h1Color").asText()          : base.h1Color();
            String h2        = node.has("h2Color")          ? node.get("h2Color").asText()          : base.h2Color();
            String accent    = node.has("accentColor")      ? node.get("accentColor").asText()      : base.accentColor();
            String btnHover  = node.has("buttonHoverColor") ? node.get("buttonHoverColor").asText() : base.buttonHoverColor();
            String font      = node.has("fontFamily")       ? node.get("fontFamily").asText()       : base.fontFamily();

            Set<String> include = null;
            if (node.has("includeFields") && node.get("includeFields").isArray()) {
                include = new HashSet<>();
                for (JsonNode f : node.get("includeFields")) include.add(f.asText());
            }
            Set<String> exclude = new HashSet<>();
            if (node.has("excludeFields") && node.get("excludeFields").isArray()) {
                for (JsonNode f : node.get("excludeFields")) exclude.add(f.asText());
            }
            String customCss = node.has("customCss") ? node.get("customCss").asText() : null;
            String logoPlacement = node.has("logoPlacement") ? node.get("logoPlacement").asText() : base.logoPlacement();
            return new SlideStyle(bg, slideBg, text, h1, h2, accent, btnHover, font, include, exclude, customCss, logoPlacement);
        } catch (Exception e) {
            log.warn("Could not parse styleJson '{}', using defaults: {}", styleJson, e.getMessage());
            return SlideStyle.dark();
        }
    }

    /** Parse field filter for one-pagers: includeFields list + includeCustomFields flag. */
    private record OnePagerStyle(Set<String> includeFields, boolean includeCustomFields, boolean detailed) {}

    private OnePagerStyle parseOnePagerStyle(String styleJson) {
        if (styleJson == null || styleJson.isBlank()) return new OnePagerStyle(null, true, false);
        try {
            JsonNode node = objectMapper.readTree(styleJson);
            Set<String> include = null;
            if (node.has("includeFields") && node.get("includeFields").isArray()) {
                include = new HashSet<>();
                for (JsonNode f : node.get("includeFields")) include.add(f.asText());
            }
            boolean incCf = !node.has("includeCustomFields") || node.get("includeCustomFields").asBoolean(true);
            boolean detailed = node.has("layout") && "detailed".equals(node.get("layout").asText());
            return new OnePagerStyle(include, incCf, detailed);
        } catch (Exception e) {
            log.warn("Could not parse styleJson for one-pager: {}", e.getMessage());
            return new OnePagerStyle(null, true, false);
        }
    }

    // ── Tools ─────────────────────────────────────────────────────────────────

    @Tool(description = """
            List all document templates saved by this tenant. Returns a formatted list with each template's ID, name, type, and description.
            Call this BEFORE generating any document (slide deck, one-pager, CSV export, report) so you can offer the user a choice of saved templates.
            If no templates exist, the response will instruct the user to create one at /admin/document-templates.""")
    public String listDocumentTemplates() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            List<DocumentTemplateDTO> templates = documentTemplateService.listAllForTenant();
            if (templates.isEmpty()) {
                return "No document templates saved yet. You can ask me to create one, or visit Admin → Document Templates (/admin/document-templates).";
            }
            StringBuilder sb = new StringBuilder("Available document templates:\n\n");
            for (DocumentTemplateDTO t : templates) {
                sb.append("• [").append(t.getId()).append("] **").append(t.getName()).append("**");
                sb.append(" (type: ").append(t.getTemplateType()).append(")");
                if (t.getIsDefault()) sb.append(" ⭐ default");
                if (t.getDescription() != null && !t.getDescription().isBlank())
                    sb.append(" — ").append(t.getDescription());
                sb.append("\n");
            }
            sb.append("\nTo use a template, specify its ID when generating a document.");
            return sb.toString();
        } catch (Exception e) {
            log.error("listDocumentTemplates failed", e);
            return "Error listing templates: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a new document template. Templates store reusable style configurations for document generation.
            name (required): display name for the template.
            templateType (required): one of "slide_deck", "one_pager", "csv_export", "report".
            description (optional): what this template is for.
            styleJson (optional): JSON string with style settings (layout, colors, includeFields, etc.).""")
    public String createDocumentTemplate(String name, String templateType, String description, String styleJson) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            CreateDocumentTemplateRequest request = CreateDocumentTemplateRequest.builder()
                    .name(name)
                    .templateType(templateType)
                    .description(description)
                    .styleJson(styleJson)
                    .isDefault(false)
                    .build();
            DocumentTemplateDTO created = documentTemplateService.createTemplate(request);
            return "Document template created: [" + created.getId() + "] " + created.getName()
                    + " (type: " + created.getTemplateType() + ")";
        } catch (Exception e) {
            log.error("createDocumentTemplate failed", e);
            return "Error creating template: " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing document template. Only provided fields are changed; omit or pass null to keep current values.
            templateId (required): ID of the template to update.
            name, description, templateType, styleJson: fields to update (all optional).""")
    public String updateDocumentTemplate(Long templateId, String name, String description,
                                          String templateType, String styleJson) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            UpdateDocumentTemplateRequest request = UpdateDocumentTemplateRequest.builder()
                    .name(name)
                    .description(description)
                    .templateType(templateType)
                    .styleJson(styleJson)
                    .build();
            DocumentTemplateDTO updated = documentTemplateService.updateTemplate(templateId, request);
            return "Document template updated: [" + updated.getId() + "] " + updated.getName();
        } catch (Exception e) {
            log.error("updateDocumentTemplate failed", e);
            return "Error updating template: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a document template by ID. This cannot be undone.")
    public String deleteDocumentTemplate(Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            documentTemplateService.deleteTemplate(templateId);
            return "Document template " + templateId + " deleted successfully.";
        } catch (Exception e) {
            log.error("deleteDocumentTemplate failed", e);
            return "Error deleting template: " + e.getMessage();
        }
    }

    /** Resolve effective styleJson: if templateId given and no explicit style, load template's styleJson. */
    private String resolveStyleJson(Long templateId, String styleJson) {
        if (styleJson != null && !styleJson.isBlank()) return styleJson;
        if (templateId == null) return styleJson;
        try {
            DocumentTemplateDTO tmpl = documentTemplateService.getTemplate(templateId);
            if (tmpl.getStyleJson() != null && !tmpl.getStyleJson().isBlank()) {
                log.debug("Applying template '{}' (id={}) styleJson", tmpl.getName(), templateId);
                return tmpl.getStyleJson();
            }
        } catch (Exception e) {
            log.warn("Could not load template id={} for styleJson resolution: {}", templateId, e.getMessage());
        }
        return styleJson;
    }

    /** If title is blank and templateId is given, seed the title from the template name. */
    private String resolveTitle(Long templateId, String title, String fallback) {
        if (title != null && !title.isBlank()) return title;
        if (templateId != null) {
            try {
                DocumentTemplateDTO tmpl = documentTemplateService.getTemplate(templateId);
                return tmpl.getName();
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    @Tool(description = "Generate a full Markdown report for a specific opportunity. Include opportunity details, customer info, stage, value, close date, recent activities, and custom fields.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateOpportunityReport(Long opportunityId, String title, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            OpportunityDTO opp = opportunityService.getOpportunity(opportunityId);
            String reportTitle = resolveTitle(templateId, title, "Opportunity Report: " + opp.getName());

            CustomerDTO customer = null;
            if (opp.getCustomerId() != null) {
                try { customer = customerService.getCustomer(opp.getCustomerId()); } catch (Exception ignored) {}
            }

            List<ActivityDTO> activities = activityService.listActivities(0, 20, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();

            StringBuilder md = new StringBuilder();
            md.append("# ").append(reportTitle).append("\n\n");
            md.append("*Generated: ").append(nowStr()).append("*\n\n");

            md.append("## Overview\n");
            md.append("- **Name:** ").append(opp.getName()).append("\n");
            md.append("- **Stage:** ").append(opp.getStage()).append("\n");
            if (opp.getValue() != null) md.append("- **Value:** $").append(opp.getValue()).append("\n");
            if (opp.getProbability() != null) md.append("- **Probability:** ").append(opp.getProbability()).append("%\n");
            if (opp.getCloseDate() != null) md.append("- **Close Date:** ").append(opp.getCloseDate()).append("\n");
            if (opp.getOpportunityTypeSlug() != null) md.append("- **Type:** ").append(opp.getOpportunityTypeSlug()).append("\n");
            md.append("\n");

            if (customer != null) {
                md.append("## Customer\n");
                md.append("- **Name:** ").append(customer.getName()).append("\n");
                md.append("- **Status:** ").append(customer.getStatus()).append("\n");
                md.append("\n");
            }

            List<ActivityDTO> relatedActivities = activities.stream()
                    .filter(a -> "Opportunity".equals(a.getRelatedType()) && opportunityId.equals(a.getRelatedId()))
                    .limit(10)
                    .toList();
            if (!relatedActivities.isEmpty()) {
                md.append("## Recent Activities\n");
                for (ActivityDTO a : relatedActivities) {
                    md.append("- **[").append(a.getType()).append("]** ").append(a.getSubject())
                      .append(" — ").append(a.getStatus()).append("\n");
                }
                md.append("\n");
            }

            appendUnifiedFieldsMd(md, "Opportunity", opportunityId, opp.getCustomFields(), "Details");

            TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                    reportTitle + fileTs() + ".md", "report", "text/markdown", md.toString(),
                    "assistant_generated", "Opportunity", opportunityId);
            return documentResultMarkdown(doc);
        } catch (Exception e) {
            log.error("generateOpportunityReport failed", e);
            return "Error generating opportunity report: " + e.getMessage();
        }
    }

    @Tool(description = "Generate an aggregate Markdown report for a CRM entity type. entityType can be: Customer, Contact, Opportunity, Activity, CustomRecord.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateCrmReport(String entityType, String filtersJson, String title, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            String reportTitle = resolveTitle(templateId, title, entityType + " Report");
            StringBuilder md = new StringBuilder();
            md.append("# ").append(reportTitle).append("\n\n");
            md.append("*Generated: ").append(nowStr()).append("*\n\n");

            switch (entityType) {
                case "Customer" -> {
                    List<CustomerDTO> customers = customerService.listCustomers(0, 200, "name", "asc", "", java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    md.append("## Customers (").append(customers.size()).append(")\n\n");
                    md.append("| ID | Name | Status |\n|---|---|---|\n");
                    for (CustomerDTO c : customers) {
                        md.append("| ").append(c.getId()).append(" | ").append(c.getName()).append(" | ").append(c.getStatus()).append(" |\n");
                    }
                }
                case "Opportunity" -> {
                    List<OpportunityDTO> opps = opportunityService.listOpportunities(0, 200, "name", "asc", "", null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getContent();
                    md.append("## Opportunities (").append(opps.size()).append(")\n\n");
                    md.append("| ID | Name | Stage | Value |\n|---|---|---|---|\n");
                    for (OpportunityDTO o : opps) {
                        md.append("| ").append(o.getId()).append(" | ").append(o.getName()).append(" | ").append(o.getStage()).append(" | ").append(o.getValue() != null ? "$" + o.getValue() : "-").append(" |\n");
                    }
                }
                case "Contact" -> {
                    List<ContactDTO> contacts = contactService.listContacts(0, 200, "name", "asc", "", null, null, java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    md.append("## Contacts (").append(contacts.size()).append(")\n\n");
                    md.append("| ID | Name | Email |\n|---|---|---|\n");
                    for (ContactDTO c : contacts) {
                        md.append("| ").append(c.getId()).append(" | ").append(c.getName()).append(" | ").append(c.getEmail() != null ? c.getEmail() : "-").append(" |\n");
                    }
                }
                case "Activity" -> {
                    List<ActivityDTO> acts = activityService.listActivities(0, 200, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    md.append("## Activities (").append(acts.size()).append(")\n\n");
                    md.append("| ID | Subject | Type | Status |\n|---|---|---|---|\n");
                    for (ActivityDTO a : acts) {
                        md.append("| ").append(a.getId()).append(" | ").append(a.getSubject()).append(" | ").append(a.getType()).append(" | ").append(a.getStatus()).append(" |\n");
                    }
                }
                case "CustomRecord" -> {
                    List<CustomRecordDTO> customRecords = customRecordService.listCustomRecords(0, 200, "name", "asc", "", null, null).getContent();
                    md.append("## CustomRecords (").append(customRecords.size()).append(")\n\n");
                    md.append("| ID | Name | Type | Status |\n|---|---|---|---|\n");
                    for (CustomRecordDTO a : customRecords) {
                        md.append("| ").append(a.getId()).append(" | ").append(a.getName()).append(" | ").append(a.getType() != null ? a.getType() : "-").append(" | ").append(a.getStatus()).append(" |\n");
                    }
                }
                default -> md.append("Unknown entity type: ").append(entityType).append("\n");
            }

            TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                    reportTitle + fileTs() + ".md", "report", "text/markdown", md.toString(),
                    "assistant_generated", null, null);
            return documentResultMarkdown(doc);
        } catch (Exception e) {
            log.error("generateCrmReport failed", e);
            return "Error generating CRM report: " + e.getMessage();
        }
    }

    @Tool(description = """
            Generate a concise Markdown one-pager for a specific CRM record. entityType can be: Customer, Contact, Opportunity, Activity, CustomRecord.
            styleJson (optional) controls layout and field selection.
            Supported keys: layout ("compact"|"detailed"), includeFields (array of field names to show, e.g. ["stage","value","closeDate"]), includeCustomFields (boolean, default true).
            Example: {"layout":"detailed","includeFields":["stage","value","probability","closeDate"],"includeCustomFields":true}
            Omit styleJson or pass null to use defaults (all fields, compact layout).""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateOnePager(String entityType, Long entityId, String title, String styleJson, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            OnePagerStyle style = parseOnePagerStyle(resolveStyleJson(templateId, styleJson));
            String logoUrl = tenantAdminService.getLogoUrl(tenantId);
            StringBuilder md = new StringBuilder();
            // Add logo at the top if available
            if (logoUrl != null && !logoUrl.isBlank()) {
                md.append("<img src=\"").append(logoUrl).append("\" style=\"height:48px;margin-bottom:8px\">\n\n");
            }
            String docTitle;

            switch (entityType) {
                case "Customer" -> {
                    CustomerDTO c = customerService.getCustomer(entityId);
                    docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + c.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (style.includeFields() == null || style.includeFields().contains("status"))
                        md.append("**Status:** ").append(c.getStatus()).append("  \n");
                    if (style.detailed() && (style.includeFields() == null || style.includeFields().contains("createdAt")) && c.getCreatedAt() != null)
                        md.append("**Created:** ").append(c.getCreatedAt().toLocalDate()).append("\n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Customer", entityId, c.getCustomFields(), "Details");
                }
                case "Opportunity" -> {
                    OpportunityDTO o = opportunityService.getOpportunity(entityId);
                    docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + o.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (style.includeFields() == null || style.includeFields().contains("stage"))
                        md.append("**Stage:** ").append(o.getStage()).append("  \n");
                    if (o.getValue() != null && (style.includeFields() == null || style.includeFields().contains("value")))
                        md.append("**Value:** $").append(o.getValue()).append("  \n");
                    if (o.getProbability() != null && (style.includeFields() == null || style.includeFields().contains("probability")))
                        md.append("**Probability:** ").append(o.getProbability()).append("%  \n");
                    if (o.getCloseDate() != null && (style.includeFields() == null || style.includeFields().contains("closeDate")))
                        md.append("**Close Date:** ").append(o.getCloseDate()).append("  \n");
                    if (o.getOpportunityTypeSlug() != null && style.detailed() && (style.includeFields() == null || style.includeFields().contains("type")))
                        md.append("**Type:** ").append(o.getOpportunityTypeSlug()).append("  \n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Opportunity", entityId, o.getCustomFields(), "Details");
                }
                case "Contact" -> {
                    ContactDTO c = contactService.getContact(entityId);
                    docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + c.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (c.getEmail() != null && (style.includeFields() == null || style.includeFields().contains("email")))
                        md.append("**Email:** ").append(c.getEmail()).append("  \n");
                    if (c.getPhone() != null && (style.includeFields() == null || style.includeFields().contains("phone")))
                        md.append("**Phone:** ").append(c.getPhone()).append("  \n");
                    if (c.getTitle() != null && (style.includeFields() == null || style.includeFields().contains("title")))
                        md.append("**Title:** ").append(c.getTitle()).append("  \n");
                    if (c.getStatus() != null && style.detailed() && (style.includeFields() == null || style.includeFields().contains("status")))
                        md.append("**Status:** ").append(c.getStatus()).append("  \n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Contact", entityId, c.getCustomFields(), "Details");
                }
                case "CustomRecord" -> {
                    CustomRecordDTO a = customRecordService.getCustomRecord(entityId);
                    docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + a.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (a.getType() != null && (style.includeFields() == null || style.includeFields().contains("type")))
                        md.append("**Type:** ").append(a.getType()).append("  \n");
                    if (a.getSerialNumber() != null && (style.includeFields() == null || style.includeFields().contains("serialNumber")))
                        md.append("**Serial:** ").append(a.getSerialNumber()).append("  \n");
                    if (style.includeFields() == null || style.includeFields().contains("status"))
                        md.append("**Status:** ").append(a.getStatus()).append("  \n");
                    if (a.getNotes() != null && (style.includeFields() == null || style.includeFields().contains("notes")))
                        md.append("\n").append(a.getNotes()).append("\n");
                    appendUnifiedFieldsMd(md, "CustomRecord", entityId, a.getCustomFields(), "Details");
                }
                default -> {
                    docTitle = (title != null && !title.isBlank()) ? title : "One-Pager";
                    md.append("# ").append(docTitle).append("\n\nEntity type not supported: ").append(entityType);
                }
            }

            String finalTitle = md.toString().startsWith("# ") ? md.toString().lines().findFirst().orElse("One-Pager").substring(2) : (title != null ? title : "One-Pager");
            // Use docTitle captured in each case block
            String safeTitle = (title != null && !title.isBlank()) ? title : entityType + " One-Pager";
            TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                    safeTitle + fileTs() + ".md", "one_pager", "text/markdown", md.toString(),
                    "assistant_generated", entityType, entityId);
            return documentResultMarkdown(doc);
        } catch (Exception e) {
            log.error("generateOnePager failed", e);
            return "Error generating one-pager: " + e.getMessage();
        }
    }

    @Tool(description = """
            Generate a self-contained HTML slide deck presentation. entityType can be: Customer, Contact, Opportunity, Activity, CustomRecord.
            If entityId is provided, generates a deck for that single record.
            If entityId is null/omitted, generates a summary deck covering ALL records of that entity type (one slide per record).
            Use the null-entityId form when the user asks for a deck about 'all' or 'my' records of a type.
            styleJson (optional) controls visual style and field selection.
            Supported keys:
              layout: "dark" (default, deep blue/purple), "light" (white/blue), "corporate" (navy/gray), "minimal" (white/black)
              accentColor: hex color for buttons/borders (e.g. "#2e6da4")
              backgroundColor: outer page background
              slideBackground: slide panel background
              textColor: main text color
              h1Color: title heading color
              h2Color: section heading color
              fontFamily: CSS font-family string
              includeFields: array of field names to show; omit to show all. Use the field label (e.g. "Regulatory Impact") for custom/calculated fields, or camelCase (e.g. "closeDate") for standard fields. Case-insensitive.
              excludeFields: array of field names to hide — same naming convention as includeFields. Case-insensitive.
              customCss: raw CSS string appended after the base stylesheet, letting you override any rule or add new styles.
                Use this for creative layouts, gradients, typography, animations, two-column grids, etc.
                Available CSS variables: --slide-bg, --slide-accent, --slide-text, --slide-h1, --slide-h2, --body-bg, --font.
                Available classes to override: section, h1, h2, p, ul, li, .kpi-card, .kpi-label, .kpi-value, .badge, .tag, .subtitle, .slide-footer, button.
                Example customizations: radial-gradient backgrounds on section, letter-spacing on h1, two-column layout via CSS grid on section, custom ::before decorators.
            Example: {"layout":"corporate","accentColor":"#2e6da4","excludeFields":["Regulatory Impact","Files"],"customCss":"section { background: radial-gradient(ellipse at top, #1a1a4e, #0a0a1a); } h1 { letter-spacing: 0.05em; text-transform: uppercase; }"}""")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateSlideDeck(String entityType, Long entityId, String title, String styleJson, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            SlideStyle style = parseSlideStyle(resolveStyleJson(templateId, styleJson));
            String logoUrl = tenantAdminService.getLogoUrl(tenantId);
            String deckTitle;
            StringBuilder slidesHtml = new StringBuilder();

            // Multi-entity deck when no entityId provided
            if (entityId == null) {
                deckTitle = (title != null && !title.isBlank()) ? title : "All " + entityType + "s";
                slidesHtml.append(titleSlide(deckTitle, "Summary Deck", logoUrl, style));
                switch (entityType) {
                    case "Opportunity" -> {
                        List<OpportunityDTO> opps = opportunityService.listOpportunities(0, 100, "name", "asc", null, null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getContent();
                        if (opps.isEmpty()) return "No opportunities found to include in the deck.";
                        // Summary stats slide
                        long totalCount = opps.size();
                        double totalValue = opps.stream().filter(o -> o.getValue() != null).mapToDouble(o -> o.getValue().doubleValue()).sum();
                        double avgProb = opps.stream().filter(o -> o.getProbability() != null).mapToDouble(o -> o.getProbability().doubleValue()).average().orElse(0);
                        StringBuilder summary = new StringBuilder("<h2>Pipeline Summary</h2><div class=\"kpi-grid\">");
                        summary.append(kpi("Total", String.valueOf(totalCount)));
                        if (totalValue > 0) summary.append(kpi("Total Value", "$" + String.format("%,.0f", totalValue)));
                        if (avgProb > 0) summary.append(kpi("Avg Probability", String.format("%.0f%%", avgProb)));
                        summary.append("</div>");
                        // Stage breakdown tags
                        Map<String, Long> stages = opps.stream().collect(java.util.stream.Collectors.groupingBy(o -> o.getStage() != null ? o.getStage() : "Unknown", java.util.stream.Collectors.counting()));
                        if (!stages.isEmpty()) {
                            summary.append("<div class=\"tag-list\" style=\"margin-top:18px\">");
                            stages.forEach((s, cnt) -> summary.append("<span class=\"tag\">").append(escHtml(s)).append(" &nbsp;<strong>").append(cnt).append("</strong></span>"));
                            summary.append("</div>");
                        }
                        slidesHtml.append(slide(summary.toString()));
                        for (OpportunityDTO oTable : opps) {
                            OpportunityDTO o;
                            try { o = opportunityService.getOpportunity(oTable.getId()); } catch (Exception e) { o = oTable; }
                            StringBuilder s = new StringBuilder();
                            if (style.includes("stage")) s.append("<span class=\"badge\">").append(escHtml(o.getStage())).append("</span>");
                            s.append("<h2>").append(escHtml(o.getName())).append("</h2>");
                            s.append("<div class=\"kpi-grid\">");
                            if (style.includes("value") && o.getValue() != null) s.append(kpi("Value", "$" + String.format("%,.0f", o.getValue().doubleValue())));
                            if (style.includes("probability") && o.getProbability() != null) s.append(kpi("Probability", o.getProbability() + "%"));
                            if (style.includes("closeDate") && o.getCloseDate() != null) s.append(kpi("Close", o.getCloseDate().toString()));
                            s.append("</div>");
                            s.append(unifiedFieldsHtml("Opportunity", o.getId(), o.getCustomFields(), style));
                            slidesHtml.append(slide(s.toString()));
                        }
                    }
                    case "Customer" -> {
                        List<CustomerDTO> customers = customerService.listCustomers(0, 100, "name", "asc", null, java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                        if (customers.isEmpty()) return "No customers found to include in the deck.";
                        // Summary slide
                        Map<String, Long> statuses = customers.stream().collect(java.util.stream.Collectors.groupingBy(c -> c.getStatus() != null ? c.getStatus() : "Unknown", java.util.stream.Collectors.counting()));
                        StringBuilder summary = new StringBuilder("<h2>Customer Overview</h2><div class=\"kpi-grid\">").append(kpi("Total Customers", String.valueOf(customers.size()))).append("</div>");
                        if (!statuses.isEmpty()) {
                            summary.append("<div class=\"tag-list\" style=\"margin-top:18px\">");
                            statuses.forEach((s, cnt) -> summary.append("<span class=\"tag\">").append(escHtml(s)).append(" &nbsp;<strong>").append(cnt).append("</strong></span>"));
                            summary.append("</div>");
                        }
                        slidesHtml.append(slide(summary.toString()));
                        for (CustomerDTO cTable : customers) {
                            CustomerDTO c;
                            try { c = customerService.getCustomer(cTable.getId()); } catch (Exception e) { c = cTable; }
                            StringBuilder s = new StringBuilder();
                            if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                            s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                            s.append(unifiedFieldsHtml("Customer", c.getId(), c.getCustomFields(), style));
                            slidesHtml.append(slide(s.toString()));
                        }
                    }
                    default -> slidesHtml.append(slide("<h2>No detailed template for " + escHtml(entityType) + "</h2>"));
                }
                String html = buildSlideDeckHtml(deckTitle, slidesHtml.toString(), style, logoUrl);
                TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                        deckTitle + fileTs() + ".html", "html", "text/html", html,
                        "assistant_generated", entityType, null);
                return documentResultMarkdown(doc);
            }

            // Single-entity deck
            switch (entityType) {
                case "Opportunity" -> {
                    OpportunityDTO o = opportunityService.getOpportunity(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : o.getName() + " Deck";
                    slidesHtml.append(titleSlide(deckTitle, "Opportunity Overview", logoUrl, style));
                    // KPI overview slide
                    StringBuilder kpis = new StringBuilder();
                    if (style.includes("stage")) kpis.append("<span class=\"badge\">").append(escHtml(o.getStage())).append("</span>");
                    kpis.append("<h2>").append(escHtml(o.getName())).append("</h2>");
                    kpis.append("<div class=\"kpi-grid\">");
                    if (style.includes("value") && o.getValue() != null) kpis.append(kpi("Deal Value", "$" + String.format("%,.0f", o.getValue().doubleValue())));
                    if (style.includes("probability") && o.getProbability() != null) kpis.append(kpi("Win Probability", o.getProbability() + "%"));
                    if (style.includes("closeDate") && o.getCloseDate() != null) kpis.append(kpi("Close Date", o.getCloseDate().toString()));
                    if (style.includes("type") && o.getOpportunityTypeSlug() != null) kpis.append(kpi("Type", o.getOpportunityTypeSlug()));
                    kpis.append("</div>");
                    slidesHtml.append(slide(kpis.toString()));
                    String oppFields = unifiedFieldsHtml("Opportunity", entityId, o.getCustomFields(), style);
                    if (!oppFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + oppFields));
                }
                case "Customer" -> {
                    CustomerDTO c = customerService.getCustomer(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : c.getName() + " Overview";
                    slidesHtml.append(titleSlide(deckTitle, "Customer Profile", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                    s.append(unifiedFieldsHtml("Customer", entityId, c.getCustomFields(), style));
                    slidesHtml.append(slide(s.toString()));
                }
                case "Contact" -> {
                    ContactDTO c = contactService.getContact(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : c.getName() + " Profile";
                    slidesHtml.append(titleSlide(deckTitle, "Contact Profile", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status") && c.getStatus() != null) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                    s.append("<div class=\"kpi-grid\">");
                    if (style.includes("title") && c.getTitle() != null) s.append(kpi("Title", c.getTitle()));
                    if (style.includes("email") && c.getEmail() != null) s.append(kpi("Email", c.getEmail()));
                    if (style.includes("phone") && c.getPhone() != null) s.append(kpi("Phone", c.getPhone()));
                    s.append("</div>");
                    slidesHtml.append(slide(s.toString()));
                    String contFields = unifiedFieldsHtml("Contact", entityId, c.getCustomFields(), style);
                    if (!contFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + contFields));
                }
                case "CustomRecord" -> {
                    CustomRecordDTO a = customRecordService.getCustomRecord(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : a.getName() + " CustomRecord";
                    slidesHtml.append(titleSlide(deckTitle, "CustomRecord Details", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(a.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(a.getName())).append("</h2>");
                    s.append("<div class=\"kpi-grid\">");
                    if (style.includes("type") && a.getType() != null) s.append(kpi("Type", a.getType()));
                    if (style.includes("serialNumber") && a.getSerialNumber() != null) s.append(kpi("Serial #", a.getSerialNumber()));
                    s.append("</div>");
                    slidesHtml.append(slide(s.toString()));
                    if (a.getNotes() != null) slidesHtml.append(slide("<h2>Notes</h2><p>" + escHtml(a.getNotes()) + "</p>"));
                    String customRecordFields = unifiedFieldsHtml("CustomRecord", entityId, a.getCustomFields(), style);
                    if (!customRecordFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + customRecordFields));
                }
                default -> {
                    deckTitle = (title != null && !title.isBlank()) ? title : entityType + " Deck";
                    slidesHtml.append(titleSlide(deckTitle, "Generated by BOCRM", logoUrl, style));
                }
            }

            String html = buildSlideDeckHtml(deckTitle, slidesHtml.toString(), style, logoUrl);
            TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                    deckTitle + fileTs() + ".html", "html", "text/html", html,
                    "assistant_generated", entityType, entityId);
            return documentResultMarkdown(doc);
        } catch (Exception e) {
            log.error("generateSlideDeck failed", e);
            return "Error generating slide deck: " + e.getMessage();
        }
    }

    @Tool(description = "Generate a CSV export for a CRM entity type. entityType can be: Customer, Contact, Opportunity, Activity, CustomRecord. Custom fields are included as extra columns.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateCsvExport(String entityType, String filtersJson, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("# Exported: ").append(nowStr()).append("\n");
            String fileName = "export_" + entityType.toLowerCase() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";

            switch (entityType) {
                case "Customer" -> {
                    List<CustomerDTO> rows = customerService.listCustomers(0, 200, "name", "asc", "", java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    Set<String> cfKeys = collectCustomFieldKeys(rows.stream().map(CustomerDTO::getCustomFields).toList());
                    csv.append("id,name,status,createdAt");
                    cfKeys.forEach(k -> csv.append(",cf_").append(k));
                    csv.append("\n");
                    for (CustomerDTO c : rows) {
                        csv.append(c.getId()).append(",").append(csvEsc(c.getName())).append(",")
                           .append(csvEsc(c.getStatus())).append(",").append(c.getCreatedAt());
                        appendCustomFieldValues(csv, c.getCustomFields(), cfKeys);
                        csv.append("\n");
                    }
                }
                case "Opportunity" -> {
                    List<OpportunityDTO> rows = opportunityService.listOpportunities(0, 200, "name", "asc", "", null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getContent();
                    Set<String> cfKeys = collectCustomFieldKeys(rows.stream().map(OpportunityDTO::getCustomFields).toList());
                    csv.append("id,name,stage,value,probability,closeDate");
                    cfKeys.forEach(k -> csv.append(",cf_").append(k));
                    csv.append("\n");
                    for (OpportunityDTO o : rows) {
                        csv.append(o.getId()).append(",").append(csvEsc(o.getName())).append(",")
                           .append(csvEsc(o.getStage())).append(",").append(o.getValue() != null ? o.getValue() : "")
                           .append(",").append(o.getProbability() != null ? o.getProbability() : "")
                           .append(",").append(o.getCloseDate() != null ? o.getCloseDate() : "");
                        appendCustomFieldValues(csv, o.getCustomFields(), cfKeys);
                        csv.append("\n");
                    }
                }
                case "Contact" -> {
                    List<ContactDTO> rows = contactService.listContacts(0, 200, "name", "asc", "", null, null, java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    Set<String> cfKeys = collectCustomFieldKeys(rows.stream().map(ContactDTO::getCustomFields).toList());
                    csv.append("id,name,email,phone,title");
                    cfKeys.forEach(k -> csv.append(",cf_").append(k));
                    csv.append("\n");
                    for (ContactDTO c : rows) {
                        csv.append(c.getId()).append(",").append(csvEsc(c.getName())).append(",")
                           .append(csvEsc(c.getEmail())).append(",").append(csvEsc(c.getPhone()))
                           .append(",").append(csvEsc(c.getTitle()));
                        appendCustomFieldValues(csv, c.getCustomFields(), cfKeys);
                        csv.append("\n");
                    }
                }
                case "Activity" -> {
                    List<ActivityDTO> rows = activityService.listActivities(0, 200, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                    Set<String> cfKeys = collectCustomFieldKeys(rows.stream().map(ActivityDTO::getCustomFields).toList());
                    csv.append("id,subject,type,status,dueAt");
                    cfKeys.forEach(k -> csv.append(",cf_").append(k));
                    csv.append("\n");
                    for (ActivityDTO a : rows) {
                        csv.append(a.getId()).append(",").append(csvEsc(a.getSubject())).append(",")
                           .append(csvEsc(a.getType())).append(",").append(csvEsc(a.getStatus()))
                           .append(",").append(a.getDueAt() != null ? a.getDueAt() : "");
                        appendCustomFieldValues(csv, a.getCustomFields(), cfKeys);
                        csv.append("\n");
                    }
                }
                case "CustomRecord" -> {
                    List<CustomRecordDTO> rows = customRecordService.listCustomRecords(0, 200, "name", "asc", "", null, null).getContent();
                    Set<String> cfKeys = collectCustomFieldKeys(rows.stream().map(CustomRecordDTO::getCustomFields).toList());
                    csv.append("id,name,type,serialNumber,status,customerId");
                    cfKeys.forEach(k -> csv.append(",cf_").append(k));
                    csv.append("\n");
                    for (CustomRecordDTO a : rows) {
                        csv.append(a.getId()).append(",").append(csvEsc(a.getName())).append(",")
                           .append(csvEsc(a.getType())).append(",").append(csvEsc(a.getSerialNumber()))
                           .append(",").append(csvEsc(a.getStatus())).append(",")
                           .append(a.getCustomerId() != null ? a.getCustomerId() : "");
                        appendCustomFieldValues(csv, a.getCustomFields(), cfKeys);
                        csv.append("\n");
                    }
                }
                default -> csv.append("Unsupported entity type: ").append(entityType).append("\n");
            }

            TenantDocumentDTO doc = tenantDocumentService.createFromContent(
                    fileName, "csv", "text/csv", csv.toString(),
                    "assistant_generated", null, null);
            return documentResultMarkdown(doc);
        } catch (Exception e) {
            log.error("generateCsvExport failed", e);
            return "Error generating CSV export: " + e.getMessage();
        }
    }

    /** Collect all unique custom field keys across a list of JsonNode objects, preserving insertion order. */
    private Set<String> collectCustomFieldKeys(List<JsonNode> nodes) {
        Set<String> keys = new LinkedHashSet<>();
        for (JsonNode node : nodes) {
            if (node != null) keys.addAll(node.propertyNames());
        }
        return keys;
    }

    /** Append custom field values in the given key order to the CSV row. */
    private void appendCustomFieldValues(StringBuilder csv, JsonNode customFields, Set<String> keys) {
        for (String key : keys) {
            csv.append(",");
            if (customFields != null && customFields.has(key)) {
                csv.append(csvEsc(safeNodeValue(customFields.get(key))));
            }
        }
    }

    // --- Helpers ---

    private static final DateTimeFormatter TS_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("_yyyyMMdd_HHmm");

    private String nowStr() {
        return LocalDateTime.now().format(TS_FMT);
    }

    /** Timestamp suffix for filenames, e.g. "_20260319_1430". */
    private String fileTs() {
        return LocalDateTime.now().format(FILE_FMT);
    }

    /**
     * A merged, ordered entry combining a custom or calculated field label and its value.
     * Both types use displayOrder; ties are broken by id.
     */
    private record FieldEntry(int displayOrder, long id, String label, String key, String value) {}

    /**
     * Builds a unified ordered list of all custom + calculated field values for an entity,
     * sorted by displayOrder (then by id for ties). Silently skips on error.
     */
    /**
     * Safely extracts a display string from any JsonNode type.
     * Array nodes (multiselect, document_multi, custom_record_multi) are joined with ", ".
     * Workflow object nodes {"currentIndex": N} show a human-readable step label.
     * Primitives use asText(); objects fall back to toString().
     */
    private String safeNodeValue(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return "";
        if (n.isArray()) {
            List<String> parts = new ArrayList<>();
            n.forEach(item -> parts.add(item.isTextual() ? item.textValue() : item.toString()));
            return String.join(", ", parts);
        }
        if (n.isObject()) {
            JsonNode ci = n.get("currentIndex");
            if (ci != null && !ci.isNull()) return "Step " + (ci.asInt() + 1);
            return n.toString();
        }
        return n.isTextual() ? n.textValue() : n.toString();
    }

    private List<FieldEntry> buildUnifiedFields(String entityType, Long entityId, JsonNode customFieldValues) {
        List<FieldEntry> entries = new ArrayList<>();
        try {
            List<CustomFieldDefinitionDTO> customDefs = customFieldDefinitionService.getFieldDefinitions(entityType);
            for (CustomFieldDefinitionDTO def : customDefs) {
                String val = (customFieldValues != null && customFieldValues.has(def.getKey()))
                        ? safeNodeValue(customFieldValues.get(def.getKey())) : "";
                int order = def.getDisplayOrder() != null ? def.getDisplayOrder() : 0;
                entries.add(new FieldEntry(order, def.getId(), def.getLabel(), def.getKey(), val));
            }
        } catch (Exception e) {
            log.debug("Could not load custom field definitions for {} {}: {}", entityType, entityId, e.getMessage());
        }
        try {
            Map<String, Object> calcValues = calculatedFieldDefinitionService.evaluateForEntity(entityType, entityId);
            List<CalculatedFieldDefinitionDTO> calcDefs = calculatedFieldDefinitionService.getDefinitions(entityType);
            for (CalculatedFieldDefinitionDTO def : calcDefs) {
                Object val = calcValues != null ? calcValues.get(def.getKey()) : null;
                int order = def.getDisplayOrder() != null ? def.getDisplayOrder() : 0;
                entries.add(new FieldEntry(order, def.getId(), def.getLabel(), def.getKey(), val != null ? val.toString() : ""));
            }
        } catch (Exception e) {
            log.debug("Could not evaluate calculated fields for {} {}: {}", entityType, entityId, e.getMessage());
        }
        entries.sort(Comparator.comparingInt(FieldEntry::displayOrder).thenComparingLong(FieldEntry::id));
        return entries;
    }

    /**
     * Appends all custom + calculated fields as a flat Markdown list, respecting displayOrder.
     * Uses an optional section heading (pass null to omit the heading).
     */
    private void appendUnifiedFieldsMd(StringBuilder md, String entityType, Long entityId,
                                        JsonNode customFieldValues, String sectionHeading) {
        List<FieldEntry> entries = buildUnifiedFields(entityType, entityId, customFieldValues);
        if (entries.isEmpty()) return;
        if (sectionHeading != null) md.append("## ").append(sectionHeading).append("\n");
        entries.forEach(e -> md.append("- **").append(e.label()).append(":** ").append(e.value()).append("\n"));
        md.append("\n");
    }

    /**
     * Returns an HTML &lt;ul&gt; snippet of all custom + calculated fields, respecting displayOrder.
     * Returns empty string if none defined.
     */
    private String unifiedFieldsHtml(String entityType, Long entityId, JsonNode customFieldValues) {
        List<FieldEntry> entries = buildUnifiedFields(entityType, entityId, customFieldValues);
        if (entries.isEmpty()) return "";
        StringBuilder html = new StringBuilder("<ul style=\"margin-top:12px\">");
        entries.forEach(e -> html.append("<li><strong>").append(escHtml(e.label()))
                .append(":</strong> ").append(escHtml(e.value())).append("</li>"));
        html.append("</ul>");
        return html.toString();
    }

    /**
     * Like {@link #unifiedFieldsHtml} but filters entries through the SlideStyle include/exclude lists.
     * Matching is case-insensitive against both the field label and its key.
     */
    private String unifiedFieldsHtml(String entityType, Long entityId, JsonNode customFieldValues, SlideStyle style) {
        List<FieldEntry> entries = buildUnifiedFields(entityType, entityId, customFieldValues).stream()
                .filter(e -> style.includesField(e.label(), e.key()))
                .collect(java.util.stream.Collectors.toList());
        if (entries.isEmpty()) return "";
        StringBuilder html = new StringBuilder("<ul style=\"margin-top:12px\">");
        entries.forEach(e -> html.append("<li><strong>").append(escHtml(e.label()))
                .append(":</strong> ").append(escHtml(e.value())).append("</li>"));
        html.append("</ul>");
        return html.toString();
    }

    private String slide(String content) {
        return "<section>" + content + "</section>\n";
    }

    private String titleSlide(String title, String subtitle, String logoUrl, SlideStyle style) {
        String leftItem = (logoUrl != null && style.showLogoOnTitle())
                ? "<img src=\"" + logoUrl + "\" style=\"height:28px;opacity:0.85;vertical-align:middle\">"
                : "<span>BOCRM</span>";
        return slide("<h1>" + escHtml(title) + "</h1>"
                + "<p class=\"subtitle\">" + escHtml(subtitle) + "</p>"
                + "<div class=\"slide-footer\">" + leftItem + "<span>Generated " + nowStr() + "</span></div>");
    }

    private String kpi(String label, String value) {
        return "<div class=\"kpi-card\">"
                + "<div class=\"kpi-label\">" + escHtml(label) + "</div>"
                + "<div class=\"kpi-value\">" + escHtml(value) + "</div>"
                + "</div>";
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * Generates a slide deck HTML string without saving it to the database.
     * Used by the Report Builder preview endpoint.
     */
    public String previewSlideDeck(String entityType, Long entityId, String title, String styleJson, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            SlideStyle style = parseSlideStyle(resolveStyleJson(templateId, styleJson));
            String logoUrl = tenantAdminService.getLogoUrl(tenantId);
            String deckTitle;
            StringBuilder slidesHtml = new StringBuilder();

            if (entityId == null) {
                deckTitle = (title != null && !title.isBlank()) ? title : "All " + entityType + "s";
                slidesHtml.append(titleSlide(deckTitle, "Summary Deck", logoUrl, style));
                switch (entityType) {
                    case "Opportunity" -> {
                        List<OpportunityDTO> opps = opportunityService.listOpportunities(0, 100, "name", "asc", null, null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getContent();
                        if (opps.isEmpty()) return null;
                        long totalCount = opps.size();
                        double totalValue = opps.stream().filter(o -> o.getValue() != null).mapToDouble(o -> o.getValue().doubleValue()).sum();
                        double avgProb = opps.stream().filter(o -> o.getProbability() != null).mapToDouble(o -> o.getProbability().doubleValue()).average().orElse(0);
                        StringBuilder summary = new StringBuilder("<h2>Pipeline Summary</h2><div class=\"kpi-grid\">");
                        summary.append(kpi("Total", String.valueOf(totalCount)));
                        if (totalValue > 0) summary.append(kpi("Total Value", "$" + String.format("%,.0f", totalValue)));
                        if (avgProb > 0) summary.append(kpi("Avg Probability", String.format("%.0f%%", avgProb)));
                        summary.append("</div>");
                        Map<String, Long> stages = opps.stream().collect(java.util.stream.Collectors.groupingBy(o -> o.getStage() != null ? o.getStage() : "Unknown", java.util.stream.Collectors.counting()));
                        if (!stages.isEmpty()) {
                            summary.append("<div class=\"tag-list\" style=\"margin-top:18px\">");
                            stages.forEach((s, cnt) -> summary.append("<span class=\"tag\">").append(escHtml(s)).append(" &nbsp;<strong>").append(cnt).append("</strong></span>"));
                            summary.append("</div>");
                        }
                        slidesHtml.append(slide(summary.toString()));
                        for (OpportunityDTO oTable : opps) {
                            OpportunityDTO o;
                            try { o = opportunityService.getOpportunity(oTable.getId()); } catch (Exception e) { o = oTable; }
                            StringBuilder s = new StringBuilder();
                            if (style.includes("stage")) s.append("<span class=\"badge\">").append(escHtml(o.getStage())).append("</span>");
                            s.append("<h2>").append(escHtml(o.getName())).append("</h2>");
                            s.append("<div class=\"kpi-grid\">");
                            if (style.includes("value") && o.getValue() != null) s.append(kpi("Value", "$" + String.format("%,.0f", o.getValue().doubleValue())));
                            if (style.includes("probability") && o.getProbability() != null) s.append(kpi("Probability", o.getProbability() + "%"));
                            if (style.includes("closeDate") && o.getCloseDate() != null) s.append(kpi("Close", o.getCloseDate().toString()));
                            s.append("</div>");
                            s.append(unifiedFieldsHtml("Opportunity", o.getId(), o.getCustomFields(), style));
                            slidesHtml.append(slide(s.toString()));
                        }
                    }
                    case "Customer" -> {
                        List<CustomerDTO> customers = customerService.listCustomers(0, 100, "name", "asc", null, java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent();
                        if (customers.isEmpty()) return null;
                        Map<String, Long> statuses = customers.stream().collect(java.util.stream.Collectors.groupingBy(c -> c.getStatus() != null ? c.getStatus() : "Unknown", java.util.stream.Collectors.counting()));
                        StringBuilder summary = new StringBuilder("<h2>Customer Overview</h2><div class=\"kpi-grid\">").append(kpi("Total Customers", String.valueOf(customers.size()))).append("</div>");
                        if (!statuses.isEmpty()) {
                            summary.append("<div class=\"tag-list\" style=\"margin-top:18px\">");
                            statuses.forEach((s, cnt) -> summary.append("<span class=\"tag\">").append(escHtml(s)).append(" &nbsp;<strong>").append(cnt).append("</strong></span>"));
                            summary.append("</div>");
                        }
                        slidesHtml.append(slide(summary.toString()));
                        for (CustomerDTO cTable : customers) {
                            CustomerDTO c;
                            try { c = customerService.getCustomer(cTable.getId()); } catch (Exception e) { c = cTable; }
                            StringBuilder s = new StringBuilder();
                            if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                            s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                            s.append(unifiedFieldsHtml("Customer", c.getId(), c.getCustomFields(), style));
                            slidesHtml.append(slide(s.toString()));
                        }
                    }
                    default -> slidesHtml.append(slide("<h2>No detailed template for " + escHtml(entityType) + "</h2>"));
                }
                return buildSlideDeckHtml(deckTitle, slidesHtml.toString(), style, logoUrl);
            }

            switch (entityType) {
                case "Opportunity" -> {
                    OpportunityDTO o = opportunityService.getOpportunity(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : o.getName() + " Deck";
                    slidesHtml.append(titleSlide(deckTitle, "Opportunity Overview", logoUrl, style));
                    StringBuilder kpis = new StringBuilder();
                    if (style.includes("stage")) kpis.append("<span class=\"badge\">").append(escHtml(o.getStage())).append("</span>");
                    kpis.append("<h2>").append(escHtml(o.getName())).append("</h2>");
                    kpis.append("<div class=\"kpi-grid\">");
                    if (style.includes("value") && o.getValue() != null) kpis.append(kpi("Deal Value", "$" + String.format("%,.0f", o.getValue().doubleValue())));
                    if (style.includes("probability") && o.getProbability() != null) kpis.append(kpi("Win Probability", o.getProbability() + "%"));
                    if (style.includes("closeDate") && o.getCloseDate() != null) kpis.append(kpi("Close Date", o.getCloseDate().toString()));
                    if (style.includes("type") && o.getOpportunityTypeSlug() != null) kpis.append(kpi("Type", o.getOpportunityTypeSlug()));
                    kpis.append("</div>");
                    slidesHtml.append(slide(kpis.toString()));
                    String oppFields = unifiedFieldsHtml("Opportunity", entityId, o.getCustomFields(), style);
                    if (!oppFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + oppFields));
                }
                case "Customer" -> {
                    CustomerDTO c = customerService.getCustomer(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : c.getName() + " Overview";
                    slidesHtml.append(titleSlide(deckTitle, "Customer Profile", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                    s.append(unifiedFieldsHtml("Customer", entityId, c.getCustomFields(), style));
                    slidesHtml.append(slide(s.toString()));
                }
                case "Contact" -> {
                    ContactDTO c = contactService.getContact(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : c.getName() + " Profile";
                    slidesHtml.append(titleSlide(deckTitle, "Contact Profile", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status") && c.getStatus() != null) s.append("<span class=\"badge\">").append(escHtml(c.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(c.getName())).append("</h2>");
                    s.append("<div class=\"kpi-grid\">");
                    if (style.includes("title") && c.getTitle() != null) s.append(kpi("Title", c.getTitle()));
                    if (style.includes("email") && c.getEmail() != null) s.append(kpi("Email", c.getEmail()));
                    if (style.includes("phone") && c.getPhone() != null) s.append(kpi("Phone", c.getPhone()));
                    s.append("</div>");
                    slidesHtml.append(slide(s.toString()));
                    String contFields = unifiedFieldsHtml("Contact", entityId, c.getCustomFields(), style);
                    if (!contFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + contFields));
                }
                case "CustomRecord" -> {
                    CustomRecordDTO a = customRecordService.getCustomRecord(entityId);
                    deckTitle = (title != null && !title.isBlank()) ? title : a.getName() + " CustomRecord";
                    slidesHtml.append(titleSlide(deckTitle, "CustomRecord Details", logoUrl, style));
                    StringBuilder s = new StringBuilder();
                    if (style.includes("status")) s.append("<span class=\"badge\">").append(escHtml(a.getStatus())).append("</span>");
                    s.append("<h2>").append(escHtml(a.getName())).append("</h2>");
                    s.append("<div class=\"kpi-grid\">");
                    if (style.includes("type") && a.getType() != null) s.append(kpi("Type", a.getType()));
                    if (style.includes("serialNumber") && a.getSerialNumber() != null) s.append(kpi("Serial #", a.getSerialNumber()));
                    s.append("</div>");
                    slidesHtml.append(slide(s.toString()));
                    if (a.getNotes() != null) slidesHtml.append(slide("<h2>Notes</h2><p>" + escHtml(a.getNotes()) + "</p>"));
                    String customRecordFields = unifiedFieldsHtml("CustomRecord", entityId, a.getCustomFields(), style);
                    if (!customRecordFields.isEmpty()) slidesHtml.append(slide("<h2>Details</h2>" + customRecordFields));
                }
                default -> {
                    deckTitle = (title != null && !title.isBlank()) ? title : entityType + " Deck";
                    slidesHtml.append(titleSlide(deckTitle, "Generated by BOCRM", logoUrl, style));
                }
            }

            return buildSlideDeckHtml(deckTitle, slidesHtml.toString(), style, logoUrl);
        } catch (Exception e) {
            log.error("previewSlideDeck failed", e);
            return null;
        }
    }

    /**
     * Generates a one-pager Markdown string without saving it to the database.
     * Used by the Report Builder preview endpoint.
     */
    public String previewOnePager(String entityType, Long entityId, String title, String styleJson, Long templateId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");
        try {
            OnePagerStyle style = parseOnePagerStyle(resolveStyleJson(templateId, styleJson));
            String logoUrl = tenantAdminService.getLogoUrl(tenantId);
            StringBuilder md = new StringBuilder();
            if (logoUrl != null && !logoUrl.isBlank()) {
                md.append("<img src=\"").append(logoUrl).append("\" style=\"height:48px;margin-bottom:8px\">\n\n");
            }

            switch (entityType) {
                case "Customer" -> {
                    CustomerDTO c = customerService.getCustomer(entityId);
                    String docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + c.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (style.includeFields() == null || style.includeFields().contains("status"))
                        md.append("**Status:** ").append(c.getStatus()).append("  \n");
                    if (style.detailed() && (style.includeFields() == null || style.includeFields().contains("createdAt")) && c.getCreatedAt() != null)
                        md.append("**Created:** ").append(c.getCreatedAt().toLocalDate()).append("\n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Customer", entityId, c.getCustomFields(), "Details");
                }
                case "Opportunity" -> {
                    OpportunityDTO o = opportunityService.getOpportunity(entityId);
                    String docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + o.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (style.includeFields() == null || style.includeFields().contains("stage"))
                        md.append("**Stage:** ").append(o.getStage()).append("  \n");
                    if (o.getValue() != null && (style.includeFields() == null || style.includeFields().contains("value")))
                        md.append("**Value:** $").append(o.getValue()).append("  \n");
                    if (o.getProbability() != null && (style.includeFields() == null || style.includeFields().contains("probability")))
                        md.append("**Probability:** ").append(o.getProbability()).append("%  \n");
                    if (o.getCloseDate() != null && (style.includeFields() == null || style.includeFields().contains("closeDate")))
                        md.append("**Close Date:** ").append(o.getCloseDate()).append("  \n");
                    if (o.getOpportunityTypeSlug() != null && style.detailed() && (style.includeFields() == null || style.includeFields().contains("type")))
                        md.append("**Type:** ").append(o.getOpportunityTypeSlug()).append("  \n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Opportunity", entityId, o.getCustomFields(), "Details");
                }
                case "Contact" -> {
                    ContactDTO c = contactService.getContact(entityId);
                    String docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + c.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (c.getEmail() != null && (style.includeFields() == null || style.includeFields().contains("email")))
                        md.append("**Email:** ").append(c.getEmail()).append("  \n");
                    if (c.getPhone() != null && (style.includeFields() == null || style.includeFields().contains("phone")))
                        md.append("**Phone:** ").append(c.getPhone()).append("  \n");
                    if (c.getTitle() != null && (style.includeFields() == null || style.includeFields().contains("title")))
                        md.append("**Title:** ").append(c.getTitle()).append("  \n");
                    if (c.getStatus() != null && style.detailed() && (style.includeFields() == null || style.includeFields().contains("status")))
                        md.append("**Status:** ").append(c.getStatus()).append("  \n");
                    md.append("\n");
                    appendUnifiedFieldsMd(md, "Contact", entityId, c.getCustomFields(), "Details");
                }
                case "CustomRecord" -> {
                    CustomRecordDTO a = customRecordService.getCustomRecord(entityId);
                    String docTitle = (title != null && !title.isBlank()) ? title : "One-Pager: " + a.getName();
                    md.append("# ").append(docTitle).append("\n\n");
                    md.append("*Generated: ").append(nowStr()).append("*\n\n");
                    if (a.getType() != null && (style.includeFields() == null || style.includeFields().contains("type")))
                        md.append("**Type:** ").append(a.getType()).append("  \n");
                    if (a.getSerialNumber() != null && (style.includeFields() == null || style.includeFields().contains("serialNumber")))
                        md.append("**Serial:** ").append(a.getSerialNumber()).append("  \n");
                    if (style.includeFields() == null || style.includeFields().contains("status"))
                        md.append("**Status:** ").append(a.getStatus()).append("  \n");
                    if (a.getNotes() != null && (style.includeFields() == null || style.includeFields().contains("notes")))
                        md.append("\n").append(a.getNotes()).append("\n");
                    appendUnifiedFieldsMd(md, "CustomRecord", entityId, a.getCustomFields(), "Details");
                }
                default -> {
                    String docTitle = (title != null && !title.isBlank()) ? title : "One-Pager";
                    md.append("# ").append(docTitle).append("\n\nEntity type not supported: ").append(entityType);
                }
            }
            return md.toString();
        } catch (Exception e) {
            log.error("previewOnePager failed", e);
            return null;
        }
    }

    private String buildSlideDeckHtml(String title, String slides, SlideStyle style, String logoUrl) {
        // Build logo overlay HTML based on placement preference
        String logoOverlay = "";
        if (logoUrl != null && !logoUrl.isBlank()) {
            if (style.showLogoOnHeader()) {
                logoOverlay = "<div class=\"logo-overlay logo-header\"><img src=\"" + logoUrl + "\" style=\"height:32px;opacity:0.85\"></div>\n";
            } else if (style.showLogoOnFooter()) {
                logoOverlay = "<div class=\"logo-overlay logo-footer\"><img src=\"" + logoUrl + "\" style=\"height:32px;opacity:0.85\"></div>\n";
            }
        }

        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>" + escHtml(title) + "</title>\n" +
                "<style>\n" +
                "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                ":root {\n" +
                "  --slide-bg: " + style.slideBackground() + ";\n" +
                "  --slide-accent: " + style.accentColor() + ";\n" +
                "  --slide-text: " + style.textColor() + ";\n" +
                "  --slide-h1: " + style.h1Color() + ";\n" +
                "  --slide-h2: " + style.h2Color() + ";\n" +
                "  --body-bg: " + style.backgroundColor() + ";\n" +
                "  --font: " + style.fontFamily() + ";\n" +
                "}\n" +
                "body { font-family: var(--font); background: var(--body-bg); color: var(--slide-text); overflow: hidden; }\n" +
                ".progress-bar { position: fixed; top: 0; left: 0; height: 4px; background: var(--slide-accent); transition: width 0.3s ease; z-index: 100; }\n" +
                ".deck-wrapper { width: 100vw; height: 100vh; display: flex; align-items: center; justify-content: center; }\n" +
                "section {\n" +
                "  display: none;\n" +
                "  width: 100%;\n" +
                "  height: 100vh;\n" +
                "  padding: 60px 80px;\n" +
                "  background: var(--slide-bg);\n" +
                "  color: var(--slide-text);\n" +
                "  flex-direction: column;\n" +
                "  justify-content: center;\n" +
                "  gap: 20px;\n" +
                "  position: relative;\n" +
                "}\n" +
                "section.active { display: flex; animation: fadeIn 0.35s ease; }\n" +
                "@keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }\n" +
                "h1 { color: var(--slide-h1); font-size: 2.6em; line-height: 1.2; }\n" +
                "h2 { color: var(--slide-h2); font-size: 1.6em; border-bottom: 2px solid var(--slide-accent); padding-bottom: 10px; margin-bottom: 4px; }\n" +
                "p { font-size: 1.05em; line-height: 1.7; max-width: 860px; }\n" +
                "ul { list-style: none; padding: 0; display: flex; flex-direction: column; gap: 10px; }\n" +
                "ul li::before { content: '\\25B8\\0020'; color: var(--slide-accent); font-size: 0.9em; }\n" +
                "ul li { font-size: 1.05em; line-height: 1.6; display: flex; align-items: flex-start; gap: 6px; }\n" +
                ".subtitle { color: var(--slide-h2); font-size: 1.2em; opacity: 0.85; margin-top: -8px; }\n" +
                ".slide-footer {\n" +
                "  position: absolute;\n" +
                "  bottom: 22px;\n" +
                "  left: 80px;\n" +
                "  right: 80px;\n" +
                "  display: flex;\n" +
                "  justify-content: space-between;\n" +
                "  font-size: 0.78em;\n" +
                "  opacity: 0.5;\n" +
                "  letter-spacing: 0.04em;\n" +
                "}\n" +
                ".badge {\n" +
                "  display: inline-block;\n" +
                "  padding: 3px 10px;\n" +
                "  border-radius: 12px;\n" +
                "  font-size: 0.78em;\n" +
                "  font-weight: 600;\n" +
                "  text-transform: uppercase;\n" +
                "  letter-spacing: 0.06em;\n" +
                "  background: var(--slide-accent);\n" +
                "  color: #fff;\n" +
                "  margin-bottom: 8px;\n" +
                "}\n" +
                ".kpi-grid { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 8px; }\n" +
                ".kpi-card {\n" +
                "  flex: 1 1 160px;\n" +
                "  background: rgba(255,255,255,0.06);\n" +
                "  border: 1px solid rgba(255,255,255,0.12);\n" +
                "  border-radius: 10px;\n" +
                "  padding: 18px 22px;\n" +
                "  display: flex;\n" +
                "  flex-direction: column;\n" +
                "  gap: 6px;\n" +
                "}\n" +
                ".kpi-card .kpi-label { font-size: 0.78em; text-transform: uppercase; letter-spacing: 0.08em; opacity: 0.6; }\n" +
                ".kpi-card .kpi-value { font-size: 1.7em; font-weight: 700; color: var(--slide-h1); }\n" +
                ".tag-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }\n" +
                ".tag { display: inline-block; padding: 3px 10px; border-radius: 8px; font-size: 0.82em; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); }\n" +
                ".nav {\n" +
                "  position: fixed;\n" +
                "  bottom: 18px;\n" +
                "  right: 24px;\n" +
                "  display: flex;\n" +
                "  gap: 10px;\n" +
                "  z-index: 200;\n" +
                "  align-items: center;\n" +
                "}\n" +
                "button {\n" +
                "  background: var(--slide-accent);\n" +
                "  color: #fff;\n" +
                "  border: none;\n" +
                "  padding: 9px 20px;\n" +
                "  border-radius: 6px;\n" +
                "  cursor: pointer;\n" +
                "  font-size: 0.9em;\n" +
                "  font-family: var(--font);\n" +
                "  transition: background 0.2s, transform 0.1s;\n" +
                "}\n" +
                "button:hover { background: " + style.buttonHoverColor() + "; transform: translateY(-1px); }\n" +
                "button:active { transform: translateY(0); }\n" +
                ".slide-counter { position: fixed; bottom: 22px; left: 24px; font-size: 0.82em; opacity: 0.5; z-index: 200; font-family: var(--font); }\n" +
                ".logo-overlay { position: fixed; z-index: 200; }\n" +
                ".logo-header { top: 18px; left: 28px; }\n" +
                ".logo-footer { bottom: 18px; left: 28px; }\n" +
                (style.customCss() != null && !style.customCss().isBlank() ? style.customCss() + "\n" : "") +
                "</style>\n</head>\n<body>\n" +
                "<div class=\"progress-bar\" id=\"progress\"></div>\n" +
                logoOverlay +
                "<div class=\"deck-wrapper\">\n" + slides + "</div>\n" +
                "<div class=\"nav\">\n" +
                "  <button onclick=\"show(cur-1)\">&#9664; Prev</button>\n" +
                "  <button onclick=\"show(cur+1)\">Next &#9654;</button>\n" +
                "</div>\n" +
                "<div class=\"slide-counter\" id=\"counter\"></div>\n" +
                "<script>\n" +
                "  let cur = 0;\n" +
                "  const secs = document.querySelectorAll('section');\n" +
                "  const prog = document.getElementById('progress');\n" +
                "  const ctr  = document.getElementById('counter');\n" +
                "  function show(n) {\n" +
                "    secs.forEach(s => s.classList.remove('active'));\n" +
                "    cur = Math.max(0, Math.min(n, secs.length - 1));\n" +
                "    secs[cur].classList.add('active');\n" +
                "    ctr.textContent = (cur + 1) + ' / ' + secs.length;\n" +
                "    prog.style.width = ((cur + 1) / secs.length * 100) + '%';\n" +
                "  }\n" +
                "  document.addEventListener('keydown', e => {\n" +
                "    if (e.key === 'ArrowRight' || e.key === ' ') { e.preventDefault(); show(cur + 1); }\n" +
                "    else if (e.key === 'ArrowLeft') { e.preventDefault(); show(cur - 1); }\n" +
                "    else if (e.key === 'Home') show(0);\n" +
                "    else if (e.key === 'End') show(secs.length - 1);\n" +
                "  });\n" +
                "  show(0);\n" +
                "</script>\n</body>\n</html>\n";
    }
}
