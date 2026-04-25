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

import com.bocrm.backend.dto.CreateBackupRequest;
import com.bocrm.backend.dto.CreateRestoreRequest;
import com.bocrm.backend.dto.TenantBackupJobDTO;
import com.bocrm.backend.entity.TenantBackupJob;
import com.bocrm.backend.repository.TenantBackupJobRepository;
import com.bocrm.backend.repository.TenantSettingsRepository;
import com.bocrm.backend.repository.CalculatedFieldDefinitionRepository;
import com.bocrm.backend.repository.CustomFieldDefinitionRepository;
import com.bocrm.backend.repository.DocumentTemplateRepository;
import com.bocrm.backend.repository.PolicyRuleRepository;
import com.bocrm.backend.repository.UserGroupRepository;
import com.bocrm.backend.repository.CustomerRepository;
import com.bocrm.backend.repository.ContactRepository;
import com.bocrm.backend.repository.OpportunityRepository;
import com.bocrm.backend.repository.ActivityRepository;
import com.bocrm.backend.repository.TenantDocumentRepository;
import com.bocrm.backend.repository.OrderRepository;
import com.bocrm.backend.repository.InvoiceRepository;
import com.bocrm.backend.repository.CustomRecordRepository;
import com.bocrm.backend.repository.OpportunityTypeRepository;
import com.bocrm.backend.repository.NotificationTemplateRepository;
import com.bocrm.backend.repository.IntegrationConfigRepository;
import com.bocrm.backend.repository.UserGroupMembershipRepository;
import com.bocrm.backend.repository.SavedFilterRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.ChatMessageRepository;
import com.bocrm.backend.repository.RecordAccessPolicyRepository;
import com.bocrm.backend.repository.RecordAccessGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.exception.ForbiddenException;
/**
 * TenantBackupService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBackupService {

    private final TenantBackupJobRepository repository;
    private final ObjectMapper objectMapper;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CalculatedFieldDefinitionRepository calculatedFieldDefinitionRepository;
    private final DocumentTemplateRepository documentTemplateRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final UserGroupRepository userGroupRepository;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final TenantDocumentRepository tenantDocumentRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomRecordRepository customRecordRepository;
    private final OpportunityTypeRepository opportunityTypeRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final SavedFilterRepository savedFilterRepository;
    private final TenantRepository tenantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RecordAccessPolicyRepository recordAccessPolicyRepository;
    private final RecordAccessGrantRepository recordAccessGrantRepository;

    @Transactional
    public TenantBackupJob createBackupJob(boolean includesData, Long tenantId) {
        TenantBackupJob job = TenantBackupJob.builder()
                .tenantId(tenantId)
                .type("BACKUP")
                .status("PENDING")
                .includesData(includesData)
                .label(includesData ? "Full (settings + data)" : "Settings only")
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(job);
    }

    @Transactional
    public TenantBackupJob createRestoreJob(String payload, Long tenantId) {
        TenantBackupJob job = TenantBackupJob.builder()
                .tenantId(tenantId)
                .type("RESTORE")
                .status("PENDING")
                .payload(payload)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(job);
    }

    @Transactional(readOnly = true)
    public List<TenantBackupJobDTO> listJobs(Long tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TenantBackupJobDTO> getJob(Long jobId) {
        return repository.findById(jobId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<String> downloadBackup(Long jobId) {
        return repository.findById(jobId).map(TenantBackupJob::getResultPayload);
    }

    // Called by poller; sets status/progress/resultPayload. TenantContext must be set by caller when needed.
    @Transactional
    public void executeJob(TenantBackupJob job) {
        log.info("Executing job {} type={} tenant={}", job.getId(), job.getType(), job.getTenantId());
        // Minimal implementation: mark running then completed with empty result
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        repository.save(job);

        try {
            if ("BACKUP".equals(job.getType())) {
                // Compose a simple backup payload including settings, definitions and core data
                java.util.Map<String, Object> export = new java.util.LinkedHashMap<>();
                Long tenantId = job.getTenantId();

                // Tenant name (first key, for easy identification and restore-from-backup)
                tenantRepository.findById(tenantId).ifPresent(t -> export.put("tenantName", t.getName()));

                // Tenant settings (admin-scoped)
                tenantSettingsRepository.findByTenantId(tenantId).ifPresent(ts -> {
                    try {
                        export.put("tenantSettings", objectMapper.readTree(ts.getSettingsJsonb()));
                    } catch (Exception ex) {
                        export.put("tenantSettings", ts.getSettingsJsonb());
                    }
                });

                // Custom field definitions and templates, policy rules, groups
                export.put("customFieldDefinitions", customFieldDefinitionRepository.findByTenantId(tenantId));
                export.put("calculatedFieldDefinitions", calculatedFieldDefinitionRepository.findByTenantId(tenantId));
                export.put("documentTemplates", documentTemplateRepository.findByTenantId(tenantId));

                java.util.List<?> policyRules = policyRuleRepository.findAll().stream()
                        .filter(p -> tenantId.equals(((com.bocrm.backend.entity.PolicyRule) p).getTenantId()))
                        .toList();
                export.put("policyRules", policyRules);

                export.put("recordAccessPolicies", recordAccessPolicyRepository.findByTenantId(tenantId));
                export.put("recordAccessGrants", recordAccessGrantRepository.findByTenantId(tenantId));

                export.put("userGroups", userGroupRepository.findByTenantId(tenantId));

                // Core data entities
                export.put("customers", customerRepository.findByTenantId(tenantId));
                export.put("contacts", contactRepository.findByTenantId(tenantId));
                export.put("opportunities", opportunityRepository.findByTenantId(tenantId));
                export.put("activities", activityRepository.findByTenantId(tenantId));
                export.put("documents", tenantDocumentRepository.findByTenantId(tenantId));
                export.put("chatMessages", chatMessageRepository.findByTenantId(tenantId));
                export.put("orders", orderRepository.findByTenantId(tenantId));
                export.put("invoices", invoiceRepository.findByTenantId(tenantId));
                export.put("custom_records", customRecordRepository.findByTenantId(tenantId));

                // Configuration entities
                export.put("opportunityTypes", opportunityTypeRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantId));
                export.put("notificationTemplates", notificationTemplateRepository.findByTenantId(tenantId));
                export.put("integrationConfigs", integrationConfigRepository.findByTenantId(tenantId));
                export.put("savedFilters", savedFilterRepository.findByTenantIdAndIsPublicTrue(tenantId));

                // User group memberships (backup group-user assignments)
                export.put("userGroupMemberships", userGroupMembershipRepository.findByTenantId(tenantId));

                String payload = objectMapper.writeValueAsString(export);
                job.setResultPayload(payload);
                // persist immediately so downloads can retrieve the payload even before job completes
                repository.save(job);
                log.info("Job {} result payload size={} bytes", job.getId(), payload == null ? 0 : payload.length());
            } else if ("RESTORE".equals(job.getType())) {
                Long tenantId = job.getTenantId();
                if (job.getPayload() == null) throw new IllegalArgumentException("Restore payload empty");
                tools.jackson.databind.JsonNode root = objectMapper.readTree(job.getPayload());

                // Tenant settings
                if (root.has("tenantSettings")) {
                    String settingsJson = objectMapper.writeValueAsString(root.get("tenantSettings"));
                    com.bocrm.backend.entity.TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId)
                            .orElse(com.bocrm.backend.entity.TenantSettings.builder().tenantId(tenantId).settingsJsonb(settingsJson).build());
                    ts.setSettingsJsonb(settingsJson);
                    tenantSettingsRepository.save(ts);
                }

                // Replace custom field definitions
                if (root.has("customFieldDefinitions")) {
                    java.util.List<com.bocrm.backend.entity.CustomFieldDefinition> existing = customFieldDefinitionRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) customFieldDefinitionRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.CustomFieldDefinition> defs = objectMapper.convertValue(
                            root.get("customFieldDefinitions"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.CustomFieldDefinition.class));
                    for (int i = 0; i < defs.size(); i++) {
                        com.bocrm.backend.entity.CustomFieldDefinition d = defs.get(i);
                        d.setId(null);
                        d.setTenantId(tenantId);
                        if (d.getDisplayOrder() == null) {
                            d.setDisplayOrder(i);
                        }
                    }
                    customFieldDefinitionRepository.saveAll(defs);
                }

                // Replace calculated field definitions
                if (root.has("calculatedFieldDefinitions")) {
                    java.util.List<com.bocrm.backend.entity.CalculatedFieldDefinition> existing = calculatedFieldDefinitionRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) calculatedFieldDefinitionRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.CalculatedFieldDefinition> defs = objectMapper.convertValue(
                            root.get("calculatedFieldDefinitions"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.CalculatedFieldDefinition.class));
                    for (com.bocrm.backend.entity.CalculatedFieldDefinition d : defs) {
                        d.setId(null);
                        d.setTenantId(tenantId);
                    }
                    calculatedFieldDefinitionRepository.saveAll(defs);
                }

                // Documents: upsert by name+size+linkedEntity
                if (root.has("documents")) {
                    java.util.List<com.bocrm.backend.entity.TenantDocument> existingDocs = tenantDocumentRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.TenantDocument> docMap = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.TenantDocument d : existingDocs) {
                        String key = (d.getName() == null ? "" : d.getName().toLowerCase()) + "|" + (d.getSizeBytes() == null ? "" : d.getSizeBytes()) + "|" + (d.getLinkedEntityType() == null ? "" : d.getLinkedEntityType()) + "|" + (d.getLinkedEntityId() == null ? "" : d.getLinkedEntityId());
                        docMap.put(key, d);
                    }
                    java.util.List<com.bocrm.backend.entity.TenantDocument> docs = objectMapper.convertValue(
                            root.get("documents"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.TenantDocument.class));
                    java.util.List<com.bocrm.backend.entity.TenantDocument> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.TenantDocument d : docs) {
                        String key = (d.getName() == null ? "" : d.getName().toLowerCase()) + "|" + (d.getSizeBytes() == null ? "" : d.getSizeBytes()) + "|" + (d.getLinkedEntityType() == null ? "" : d.getLinkedEntityType()) + "|" + (d.getLinkedEntityId() == null ? "" : d.getLinkedEntityId());
                        com.bocrm.backend.entity.TenantDocument existing = docMap.get(key);
                        if (existing != null) {
                            existing.setName(d.getName());
                            existing.setDescription(d.getDescription());
                            existing.setMimeType(d.getMimeType());
                            existing.setSizeBytes(d.getSizeBytes());
                            existing.setContentBase64(d.getContentBase64());
                            existing.setStorageUrl(d.getStorageUrl());
                            existing.setContentType(d.getContentType());
                            existing.setTags(d.getTags());
                            existing.setSource(d.getSource());
                            existing.setOwnerId(d.getOwnerId());
                            existing.setLinkedEntityType(d.getLinkedEntityType());
                            existing.setLinkedEntityId(d.getLinkedEntityId());
                            existing.setLinkedFieldKey(d.getLinkedFieldKey());
                            toSave.add(existing);
                        } else {
                            d.setId(null);
                            d.setTenantId(tenantId);
                            toSave.add(d);
                        }
                    }
                    tenantDocumentRepository.saveAll(toSave);
                }

                // Activities: upsert by relatedType+relatedId+subject+dueAt
                if (root.has("activities")) {
                    java.util.List<com.bocrm.backend.entity.Activity> existing = activityRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Activity> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Activity a : existing) {
                        String key = (a.getRelatedType() == null ? "" : a.getRelatedType()) + "|" + (a.getRelatedId() == null ? "" : a.getRelatedId()) + "|" + (a.getSubject() == null ? "" : a.getSubject().toLowerCase()) + "|" + (a.getDueAt() == null ? "" : a.getDueAt().toString());
                        map.put(key, a);
                    }
                    java.util.List<com.bocrm.backend.entity.Activity> arr = objectMapper.convertValue(
                            root.get("activities"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Activity.class));
                    java.util.List<com.bocrm.backend.entity.Activity> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Activity a : arr) {
                        String key = (a.getRelatedType() == null ? "" : a.getRelatedType()) + "|" + (a.getRelatedId() == null ? "" : a.getRelatedId()) + "|" + (a.getSubject() == null ? "" : a.getSubject().toLowerCase()) + "|" + (a.getDueAt() == null ? "" : a.getDueAt().toString());
                        com.bocrm.backend.entity.Activity ex = map.get(key);
                        if (ex != null) {
                            ex.setSubject(a.getSubject());
                            ex.setType(a.getType());
                            ex.setDescription(a.getDescription());
                            ex.setDueAt(a.getDueAt());
                            ex.setOwnerId(a.getOwnerId());
                            ex.setRelatedType(a.getRelatedType());
                            ex.setRelatedId(a.getRelatedId());
                            ex.setStatus(a.getStatus());
                            ex.setCustomData(a.getCustomData());
                            ex.setTableDataJsonb(a.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            a.setId(null);
                            a.setTenantId(tenantId);
                            toSave.add(a);
                        }
                    }
                    activityRepository.saveAll(toSave);
                }

                // Chat messages: replace all (conversation history is non-critical to preserve across users)
                if (root.has("chatMessages")) {
                    java.util.List<com.bocrm.backend.entity.ChatMessage> existing = chatMessageRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) chatMessageRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.ChatMessage> msgs = objectMapper.convertValue(
                            root.get("chatMessages"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.ChatMessage.class));
                    for (com.bocrm.backend.entity.ChatMessage m : msgs) {
                        m.setId(null);
                        m.setTenantId(tenantId);
                    }
                    chatMessageRepository.saveAll(msgs);
                }

                // Opportunities: upsert by customerId+name
                if (root.has("opportunities")) {
                    java.util.List<com.bocrm.backend.entity.Opportunity> existing = opportunityRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Opportunity> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Opportunity o : existing) {
                        String key = (o.getCustomerId() == null ? "" : o.getCustomerId()) + "|" + (o.getName() == null ? "" : o.getName().toLowerCase());
                        map.put(key, o);
                    }
                    java.util.List<com.bocrm.backend.entity.Opportunity> arr = objectMapper.convertValue(
                            root.get("opportunities"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Opportunity.class));
                    java.util.List<com.bocrm.backend.entity.Opportunity> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Opportunity o : arr) {
                        String key = (o.getCustomerId() == null ? "" : o.getCustomerId()) + "|" + (o.getName() == null ? "" : o.getName().toLowerCase());
                        com.bocrm.backend.entity.Opportunity ex = map.get(key);
                        if (ex != null) {
                            ex.setName(o.getName());
                            ex.setStage(o.getStage());
                            ex.setStatus(o.getStatus());
                            ex.setValue(o.getValue());
                            ex.setProbability(o.getProbability());
                            ex.setCloseDate(o.getCloseDate());
                            ex.setExpectedCloseDate(o.getExpectedCloseDate());
                            ex.setNotes(o.getNotes());
                            ex.setOwnerId(o.getOwnerId());
                            ex.setOpportunityTypeSlug(o.getOpportunityTypeSlug());
                            ex.setCustomData(o.getCustomData());
                            ex.setTableDataJsonb(o.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            o.setId(null);
                            o.setTenantId(tenantId);
                            toSave.add(o);
                        }
                    }
                    opportunityRepository.saveAll(toSave);
                }

                // Contacts: upsert by email (fallback: name+customerId)
                if (root.has("contacts")) {
                    java.util.List<com.bocrm.backend.entity.Contact> existing = contactRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Contact> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Contact c : existing) {
                        String key = (c.getEmail() != null && !c.getEmail().isBlank()) ? ("e:" + c.getEmail().toLowerCase()) : ("n:" + (c.getName()==null?"":c.getName().toLowerCase()) + "|" + (c.getCustomerId()==null?"":c.getCustomerId()));
                        map.put(key, c);
                    }
                    java.util.List<com.bocrm.backend.entity.Contact> arr = objectMapper.convertValue(
                            root.get("contacts"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Contact.class));
                    java.util.List<com.bocrm.backend.entity.Contact> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Contact c : arr) {
                        String key = (c.getEmail() != null && !c.getEmail().isBlank()) ? ("e:" + c.getEmail().toLowerCase()) : ("n:" + (c.getName()==null?"":c.getName().toLowerCase()) + "|" + (c.getCustomerId()==null?"":c.getCustomerId()));
                        com.bocrm.backend.entity.Contact ex = map.get(key);
                        if (ex != null) {
                            ex.setName(c.getName());
                            ex.setEmail(c.getEmail());
                            ex.setPhone(c.getPhone());
                            ex.setTitle(c.getTitle());
                            ex.setPrimary(c.isPrimary());
                            ex.setNotes(c.getNotes());
                            ex.setStatus(c.getStatus());
                            ex.setCustomData(c.getCustomData());
                            ex.setTableDataJsonb(c.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            c.setId(null);
                            c.setTenantId(tenantId);
                            toSave.add(c);
                        }
                    }
                    contactRepository.saveAll(toSave);
                }

                // Customers: upsert by website if present, otherwise name
                if (root.has("customers")) {
                    java.util.List<com.bocrm.backend.entity.Customer> existing = customerRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Customer> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Customer c : existing) {
                        String key = (c.getWebsite() != null && !c.getWebsite().isBlank()) ? ("w:" + c.getWebsite().toLowerCase()) : ("n:" + (c.getName()==null?"":c.getName().toLowerCase()));
                        map.put(key, c);
                    }
                    java.util.List<com.bocrm.backend.entity.Customer> arr = objectMapper.convertValue(
                            root.get("customers"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Customer.class));
                    java.util.List<com.bocrm.backend.entity.Customer> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Customer c : arr) {
                        String key = (c.getWebsite() != null && !c.getWebsite().isBlank()) ? ("w:" + c.getWebsite().toLowerCase()) : ("n:" + (c.getName()==null?"":c.getName().toLowerCase()));
                        com.bocrm.backend.entity.Customer ex = map.get(key);
                        if (ex != null) {
                            ex.setName(c.getName());
                            ex.setStatus(c.getStatus());
                            ex.setOwnerId(c.getOwnerId());
                            ex.setCustomData(c.getCustomData());
                            ex.setTableDataJsonb(c.getTableDataJsonb());
                            ex.setIndustry(c.getIndustry());
                            ex.setWebsite(c.getWebsite());
                            ex.setNotes(c.getNotes());
                            toSave.add(ex);
                        } else {
                            c.setId(null);
                            c.setTenantId(tenantId);
                            toSave.add(c);
                        }
                    }
                    customerRepository.saveAll(toSave);
                }

                // user groups, templates, policy rules: replace with payload if present
                if (root.has("userGroups")) {
                    java.util.List<com.bocrm.backend.entity.UserGroup> existing = userGroupRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) userGroupRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.UserGroup> arr = objectMapper.convertValue(
                            root.get("userGroups"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.UserGroup.class));
                    for (com.bocrm.backend.entity.UserGroup g : arr) { g.setId(null); g.setTenantId(tenantId); }
                    userGroupRepository.saveAll(arr);
                }

                if (root.has("documentTemplates")) {
                    java.util.List<com.bocrm.backend.entity.DocumentTemplate> existing = documentTemplateRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) documentTemplateRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.DocumentTemplate> arr = objectMapper.convertValue(
                            root.get("documentTemplates"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.DocumentTemplate.class));
                    for (com.bocrm.backend.entity.DocumentTemplate t : arr) { t.setId(null); t.setTenantId(tenantId); }
                    documentTemplateRepository.saveAll(arr);
                }

                if (root.has("policyRules")) {
                    java.util.List<com.bocrm.backend.entity.PolicyRule> existing = policyRuleRepository.findAll().stream()
                            .filter(p -> tenantId.equals(p.getTenantId())).toList();
                    if (!existing.isEmpty()) policyRuleRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.PolicyRule> arr = objectMapper.convertValue(
                            root.get("policyRules"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.PolicyRule.class));
                    for (com.bocrm.backend.entity.PolicyRule r : arr) { r.setId(null); r.setTenantId(tenantId); }
                    policyRuleRepository.saveAll(arr);
                }

                // Record access policies and grants
                if (root.has("recordAccessPolicies")) {
                    java.util.List<com.bocrm.backend.entity.RecordAccessPolicy> existing = recordAccessPolicyRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) recordAccessPolicyRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.RecordAccessPolicy> arr = objectMapper.convertValue(
                            root.get("recordAccessPolicies"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.RecordAccessPolicy.class));
                    for (com.bocrm.backend.entity.RecordAccessPolicy p : arr) { p.setId(null); p.setTenantId(tenantId); }
                    recordAccessPolicyRepository.saveAll(arr);
                }

                if (root.has("recordAccessGrants")) {
                    java.util.List<com.bocrm.backend.entity.RecordAccessGrant> existing = recordAccessGrantRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) recordAccessGrantRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.RecordAccessGrant> arr = objectMapper.convertValue(
                            root.get("recordAccessGrants"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.RecordAccessGrant.class));
                    for (com.bocrm.backend.entity.RecordAccessGrant g : arr) { g.setId(null); g.setTenantId(tenantId); }
                    recordAccessGrantRepository.saveAll(arr);
                }

                // Orders: upsert by customerId+name
                if (root.has("orders")) {
                    java.util.List<com.bocrm.backend.entity.Order> existing = orderRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Order> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Order o : existing) {
                        String key = (o.getCustomerId() == null ? "" : o.getCustomerId()) + "|" + (o.getName() == null ? "" : o.getName().toLowerCase());
                        map.put(key, o);
                    }
                    java.util.List<com.bocrm.backend.entity.Order> arr = objectMapper.convertValue(
                            root.get("orders"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Order.class));
                    java.util.List<com.bocrm.backend.entity.Order> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Order o : arr) {
                        String key = (o.getCustomerId() == null ? "" : o.getCustomerId()) + "|" + (o.getName() == null ? "" : o.getName().toLowerCase());
                        com.bocrm.backend.entity.Order ex = map.get(key);
                        if (ex != null) {
                            ex.setName(o.getName());
                            ex.setStatus(o.getStatus());
                            ex.setCurrency(o.getCurrency());
                            ex.setSubtotal(o.getSubtotal());
                            ex.setTaxAmount(o.getTaxAmount());
                            ex.setTotalAmount(o.getTotalAmount());
                            ex.setOrderDate(o.getOrderDate());
                            ex.setExpectedDeliveryDate(o.getExpectedDeliveryDate());
                            ex.setNotes(o.getNotes());
                            ex.setOwnerId(o.getOwnerId());
                            ex.setCustomData(o.getCustomData());
                            ex.setTableDataJsonb(o.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            o.setId(null);
                            o.setTenantId(tenantId);
                            toSave.add(o);
                        }
                    }
                    orderRepository.saveAll(toSave);
                }

                // Invoices: upsert by customerId+status+dueDate
                if (root.has("invoices")) {
                    java.util.List<com.bocrm.backend.entity.Invoice> existing = invoiceRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.Invoice> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.Invoice i : existing) {
                        String key = (i.getCustomerId() == null ? "" : i.getCustomerId()) + "|" + (i.getStatus() == null ? "" : i.getStatus()) + "|" + (i.getDueDate() == null ? "" : i.getDueDate().toString());
                        map.put(key, i);
                    }
                    java.util.List<com.bocrm.backend.entity.Invoice> arr = objectMapper.convertValue(
                            root.get("invoices"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.Invoice.class));
                    java.util.List<com.bocrm.backend.entity.Invoice> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.Invoice i : arr) {
                        String key = (i.getCustomerId() == null ? "" : i.getCustomerId()) + "|" + (i.getStatus() == null ? "" : i.getStatus()) + "|" + (i.getDueDate() == null ? "" : i.getDueDate().toString());
                        com.bocrm.backend.entity.Invoice ex = map.get(key);
                        if (ex != null) {
                            ex.setStatus(i.getStatus());
                            ex.setCurrency(i.getCurrency());
                            ex.setSubtotal(i.getSubtotal());
                            ex.setTaxAmount(i.getTaxAmount());
                            ex.setTotalAmount(i.getTotalAmount());
                            ex.setDueDate(i.getDueDate());
                            ex.setPaymentTerms(i.getPaymentTerms());
                            ex.setNotes(i.getNotes());
                            ex.setOwnerId(i.getOwnerId());
                            ex.setOrderId(i.getOrderId());
                            ex.setLineItems(i.getLineItems());
                            ex.setCustomData(i.getCustomData());
                            ex.setTableDataJsonb(i.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            i.setId(null);
                            i.setTenantId(tenantId);
                            toSave.add(i);
                        }
                    }
                    invoiceRepository.saveAll(toSave);
                }

                // CustomRecords: upsert by customerId+name
                if (root.has("custom_records")) {
                    java.util.List<com.bocrm.backend.entity.CustomRecord> existing = customRecordRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.CustomRecord> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.CustomRecord a : existing) {
                        String key = (a.getCustomerId() == null ? "" : a.getCustomerId()) + "|" + (a.getName() == null ? "" : a.getName().toLowerCase());
                        map.put(key, a);
                    }
                    java.util.List<com.bocrm.backend.entity.CustomRecord> arr = objectMapper.convertValue(
                            root.get("custom_records"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.CustomRecord.class));
                    java.util.List<com.bocrm.backend.entity.CustomRecord> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.CustomRecord a : arr) {
                        String key = (a.getCustomerId() == null ? "" : a.getCustomerId()) + "|" + (a.getName() == null ? "" : a.getName().toLowerCase());
                        com.bocrm.backend.entity.CustomRecord ex = map.get(key);
                        if (ex != null) {
                            ex.setName(a.getName());
                            ex.setType(a.getType());
                            ex.setSerialNumber(a.getSerialNumber());
                            ex.setStatus(a.getStatus());
                            ex.setNotes(a.getNotes());
                            ex.setOwnerId(a.getOwnerId());
                            ex.setCustomData(a.getCustomData());
                            ex.setTableDataJsonb(a.getTableDataJsonb());
                            toSave.add(ex);
                        } else {
                            a.setId(null);
                            a.setTenantId(tenantId);
                            toSave.add(a);
                        }
                    }
                    customRecordRepository.saveAll(toSave);
                }

                // OpportunityTypes: upsert by slug
                if (root.has("opportunityTypes")) {
                    java.util.List<com.bocrm.backend.entity.OpportunityType> existing = opportunityTypeRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.OpportunityType> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.OpportunityType ot : existing) {
                        map.put(ot.getSlug(), ot);
                    }
                    java.util.List<com.bocrm.backend.entity.OpportunityType> arr = objectMapper.convertValue(
                            root.get("opportunityTypes"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.OpportunityType.class));
                    java.util.List<com.bocrm.backend.entity.OpportunityType> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.OpportunityType ot : arr) {
                        com.bocrm.backend.entity.OpportunityType ex = map.get(ot.getSlug());
                        if (ex != null) {
                            ex.setName(ot.getName());
                            ex.setDescription(ot.getDescription());
                            ex.setDisplayOrder(ot.getDisplayOrder());
                            toSave.add(ex);
                        } else {
                            ot.setId(null);
                            ot.setTenantId(tenantId);
                            toSave.add(ot);
                        }
                    }
                    opportunityTypeRepository.saveAll(toSave);
                }

                // NotificationTemplates: upsert by notificationType
                if (root.has("notificationTemplates")) {
                    java.util.List<com.bocrm.backend.entity.NotificationTemplate> existing = notificationTemplateRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.NotificationTemplate> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.NotificationTemplate nt : existing) {
                        map.put(nt.getNotificationType(), nt);
                    }
                    java.util.List<com.bocrm.backend.entity.NotificationTemplate> arr = objectMapper.convertValue(
                            root.get("notificationTemplates"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.NotificationTemplate.class));
                    java.util.List<com.bocrm.backend.entity.NotificationTemplate> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.NotificationTemplate nt : arr) {
                        com.bocrm.backend.entity.NotificationTemplate ex = map.get(nt.getNotificationType());
                        if (ex != null) {
                            ex.setName(nt.getName());
                            ex.setSubjectTemplate(nt.getSubjectTemplate());
                            ex.setBodyTemplate(nt.getBodyTemplate());
                            ex.setIsActive(nt.getIsActive());
                            toSave.add(ex);
                        } else {
                            nt.setId(null);
                            nt.setTenantId(tenantId);
                            toSave.add(nt);
                        }
                    }
                    notificationTemplateRepository.saveAll(toSave);
                }

                // IntegrationConfigs: upsert by adapterType+name (note: credentials encrypted and portable if encryption key matches)
                if (root.has("integrationConfigs")) {
                    java.util.List<com.bocrm.backend.entity.IntegrationConfig> existing = integrationConfigRepository.findByTenantId(tenantId);
                    java.util.Map<String, com.bocrm.backend.entity.IntegrationConfig> map = new java.util.HashMap<>();
                    for (com.bocrm.backend.entity.IntegrationConfig ic : existing) {
                        String key = (ic.getAdapterType() == null ? "" : ic.getAdapterType()) + "|" + (ic.getName() == null ? "" : ic.getName().toLowerCase());
                        map.put(key, ic);
                    }
                    java.util.List<com.bocrm.backend.entity.IntegrationConfig> arr = objectMapper.convertValue(
                            root.get("integrationConfigs"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.IntegrationConfig.class));
                    java.util.List<com.bocrm.backend.entity.IntegrationConfig> toSave = new java.util.ArrayList<>();
                    for (com.bocrm.backend.entity.IntegrationConfig ic : arr) {
                        String key = (ic.getAdapterType() == null ? "" : ic.getAdapterType()) + "|" + (ic.getName() == null ? "" : ic.getName().toLowerCase());
                        com.bocrm.backend.entity.IntegrationConfig ex = map.get(key);
                        if (ex != null) {
                            ex.setEnabled(ic.isEnabled());
                            ex.setCredentialsEncrypted(ic.getCredentialsEncrypted());
                            ex.setEventTypes(ic.getEventTypes());
                            toSave.add(ex);
                        } else {
                            ic.setId(null);
                            ic.setTenantId(tenantId);
                            toSave.add(ic);
                        }
                    }
                    integrationConfigRepository.saveAll(toSave);
                }

                // SavedFilters: restore only public filters (drop private ones to avoid user ID mismatches)
                if (root.has("savedFilters")) {
                    java.util.List<com.bocrm.backend.entity.SavedFilter> existing = savedFilterRepository.findByTenantIdAndIsPublicTrue(tenantId);
                    if (!existing.isEmpty()) savedFilterRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.SavedFilter> arr = objectMapper.convertValue(
                            root.get("savedFilters"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.SavedFilter.class));
                    for (com.bocrm.backend.entity.SavedFilter sf : arr) {
                        sf.setId(null);
                        sf.setTenantId(tenantId);
                        // Note: createdBy references user ID which may differ on restore — consider clearing this or keeping for audit
                    }
                    savedFilterRepository.saveAll(arr);
                }

                // UserGroupMemberships: restore after groups, matching by group name
                if (root.has("userGroupMemberships")) {
                    java.util.List<com.bocrm.backend.entity.UserGroupMembership> existing = userGroupMembershipRepository.findByTenantId(tenantId);
                    if (!existing.isEmpty()) userGroupMembershipRepository.deleteAll(existing);
                    java.util.List<com.bocrm.backend.entity.UserGroupMembership> arr = objectMapper.convertValue(
                            root.get("userGroupMemberships"), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.bocrm.backend.entity.UserGroupMembership.class));
                    // Note: userId and groupId reference IDs that may differ on restore — this is a known limitation
                    // For now we skip restore; a full restore would require ID mapping via email/username
                    log.warn("UserGroupMemberships skipped on restore due to user ID mapping complexity");
                }
            }
            job.setProgress(100);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            repository.save(job);
        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setCompletedAt(LocalDateTime.now());
            repository.save(job);
        }
    }

    private TenantBackupJobDTO toDto(TenantBackupJob j) {
        return TenantBackupJobDTO.builder()
                .id(j.getId())
                .tenantId(j.getTenantId())
                .type(j.getType())
                .label(j.getLabel())
                .status(j.getStatus())
                .includesData(j.getIncludesData())
                .progress(j.getProgress())
                .createdAt(j.getCreatedAt())
                .startedAt(j.getStartedAt())
                .completedAt(j.getCompletedAt())
                .build();
    }

    @Transactional
    public void deleteJob(Long jobId, Long tenantId) {
        TenantBackupJob job = repository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Backup job not found"));
        if (!tenantId.equals(job.getTenantId())) throw new ForbiddenException("Access denied");
        repository.delete(job);
    }
}
