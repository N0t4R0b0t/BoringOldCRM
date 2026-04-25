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

import com.bocrm.backend.entity.NotificationInbox;
import com.bocrm.backend.entity.OutboxEvent;
import com.bocrm.backend.entity.TenantSettings;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.repository.NotificationInboxRepository;
import com.bocrm.backend.repository.TenantSettingsRepository;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.service.EmailTemplateService.EmailContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bocrm.backend.shared.TenantContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves recipient preferences, writes the in-app inbox entry, and sends email.
 * Called from NotificationPoller with TenantContext already set for the event's tenant.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class NotificationDispatchService {

    private final UserRepository userRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final NotificationInboxRepository inboxRepository;
    private final EmailTemplateService emailTemplateService;
    private final NotificationTemplateService notificationTemplateService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.default-from:noreply@bocrm.com}")
    private String defaultFrom;

    @Value("${app.mail.default-from-name:BOCRM}")
    private String defaultFromName;

    public NotificationDispatchService(UserRepository userRepository,
                                       TenantSettingsRepository tenantSettingsRepository,
                                       NotificationInboxRepository inboxRepository,
                                       EmailTemplateService emailTemplateService,
                                       NotificationTemplateService notificationTemplateService,
                                       ObjectProvider<JavaMailSender> mailSenderProvider,
                                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.inboxRepository = inboxRepository;
        this.emailTemplateService = emailTemplateService;
        this.notificationTemplateService = notificationTemplateService;
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Dispatch one notification event. TenantContext must already be set for the event's tenant.
     * Returns true if the event was fully processed (sent or intentionally skipped), false on transient failure.
     */
    @Transactional
    public boolean dispatch(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayloadJsonb());
            Long tenantId = event.getTenantId();
            String notificationType = payload.path("notificationType").asText(null);
            if (notificationType == null || notificationType.isBlank()) {
                log.warn("Notification event {} has no notificationType — skipping", event.getId());
                return true;
            }
            Long recipientUserId = payload.has("recipientUserId") ? payload.path("recipientUserId").asLong() : null;

            if (recipientUserId == null) {
                log.warn("Notification event {} has no recipientUserId — skipping", event.getId());
                return true; // terminal skip
            }

            // 1. Check tenant-level email enabled flag and fetch logo
            String senderName = defaultFromName;
            String senderEmail = defaultFrom;
            String logoUrl = null;
            Optional<TenantSettings> settings = tenantSettingsRepository.findByTenantId(tenantId);
            if (settings.isPresent() && settings.get().getSettingsJsonb() != null) {
                JsonNode s = objectMapper.readTree(settings.get().getSettingsJsonb());
                JsonNode emailNode = s.path("email");
                if (emailNode.path("enabled").isBoolean() && !emailNode.path("enabled").asBoolean()) {
                    writeInbox(tenantId, recipientUserId, notificationType, payload); // still write inbox
                    return true;
                }
                if (!emailNode.path("senderName").isMissingNode())  senderName  = emailNode.path("senderName").asText(defaultFromName);
                if (!emailNode.path("senderEmail").isMissingNode()) senderEmail = emailNode.path("senderEmail").asText(defaultFrom);
                if (!s.path("logoUrl").isMissingNode()) logoUrl = s.path("logoUrl").asText(null);
            }

            // 2. Load recipient user
            Optional<User> recipientOpt = userRepository.findById(recipientUserId);
            if (recipientOpt.isEmpty()) {
                log.warn("Notification event {} recipient user {} not found — skipping", event.getId(), recipientUserId);
                return true;
            }
            User recipient = recipientOpt.get();

            // 3. Check user-level opt-in preferences (default: opted in)
            if (isOptedOut(recipient, notificationType)) {
                writeInbox(tenantId, recipientUserId, notificationType, payload); // still write inbox
                return true;
            }

            // 4. Check user-level mute
            if (isMuted(recipient)) {
                writeInbox(tenantId, recipientUserId, notificationType, payload);
                return true;
            }

            // 5. Write in-app inbox entry
            log.info("Writing inbox entry for user {} — type={}", recipientUserId, notificationType);
            writeInbox(tenantId, recipientUserId, notificationType, payload);
            log.info("Inbox entry written successfully for user {} — type={}", recipientUserId, notificationType);

            // 6. Send email (optional — JavaMailSender may not be configured)
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.debug("JavaMailSender not configured — inbox entry written, email skipped");
                return true;
            }
            sendEmail(mailSender, recipient.getEmail(), senderName, senderEmail, notificationType, payload, logoUrl);
            return true;

        } catch (org.springframework.mail.MailException e) {
            log.error("Mail send failed for outbox event {}: {}", event.getId(), e.getMessage());
            return false; // transient — will retry
        } catch (Exception e) {
            log.error("Failed to dispatch notification event {}: {}", event.getId(), e.getMessage(), e);
            return true; // non-retriable parse/logic error — mark done
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void writeInbox(Long tenantId, Long userId, String notificationType, JsonNode payload) {
        String message = buildMessage(notificationType, payload);
        try {
            NotificationInbox inbox = NotificationInbox.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .notificationType(notificationType)
                    .entityType(payload.path("entityType").isMissingNode() ? null : payload.path("entityType").asText())
                    .entityId(payload.has("entityId") ? payload.path("entityId").asLong() : null)
                    .actorUserId(payload.has("actorUserId") ? payload.path("actorUserId").asLong() : null)
                    .actorDisplayName(payload.path("actorDisplayName").isMissingNode() ? null : payload.path("actorDisplayName").asText())
                    .message(message)
                    .build();
            inboxRepository.save(inbox);
            log.info("Inbox entry saved for user {} — type={}, message={}", userId, notificationType, message);
        } catch (Exception e) {
            log.error("Failed to write inbox entry for user {} — type={}: {}", userId, notificationType, e.getMessage(), e);
            throw e;
        }
    }

    private void sendEmail(JavaMailSender sender, String toEmail, String senderName, String senderEmail,
                            String notificationType, JsonNode payload, String logoUrl) throws Exception {
        Long tenantId = TenantContext.getTenantId();
        String actorName  = payload.path("actorDisplayName").asText(senderName);
        String entityType = payload.path("entityType").asText(null);
        String entityName = payload.path("entityName").asText(null);
        String entityId   = payload.has("entityId") ? payload.path("entityId").asText() : null;
        String permission = payload.path("permission").asText(null);
        String dueAt      = payload.path("dueAt").asText(null);
        String insight    = payload.path("insightText").asText(null);
        String customSubject = payload.path("customSubject").asText(null);
        String customBody = payload.path("customBody").asText(null);

        // Check for custom tenant template
        EmailContent content;
        var templateOpt = notificationTemplateService.findActiveForType(tenantId, notificationType);
        if (templateOpt.isPresent()) {
            var template = templateOpt.get();
            Map<String, String> vars = buildPlaceholderVariables(actorName, entityType, entityName, entityId,
                    permission, dueAt, insight, toEmail, senderName, customSubject, customBody, logoUrl);
            String subject = notificationTemplateService.applyPlaceholders(template.getSubjectTemplate(), vars);
            String body = notificationTemplateService.applyPlaceholders(template.getBodyTemplate(), vars);
            content = new EmailContent(subject, body);
        } else {
            // Fall back to built-in defaults
            content = emailTemplateService.render(notificationType, senderName,
                    actorName, entityType, entityName, entityId, permission, dueAt, insight, customSubject, customBody, logoUrl);
        }

        var msg = sender.createMimeMessage();
        var helper = new MimeMessageHelper(msg, false, "UTF-8");
        helper.setFrom(senderEmail, senderName);
        helper.setTo(toEmail);
        helper.setSubject(content.subject());
        helper.setText(content.htmlBody(), true);
        sender.send(msg);
        log.debug("Email sent to {} — type={}", toEmail, notificationType);
    }

    private String buildMessage(String type, JsonNode p) {
        String actor  = p.path("actorDisplayName").asText("Someone");
        String entity = p.path("entityName").asText("a record");
        String eType  = p.path("entityType").asText("record");
        return switch (type) {
            case NotificationService.RECORD_MODIFIED    -> actor + " updated your " + eType + ": " + entity;
            case NotificationService.OWNERSHIP_ASSIGNED -> actor + " assigned you ownership of " + eType + ": " + entity;
            case NotificationService.ACCESS_GRANTED     -> actor + " granted you " + p.path("permission").asText("READ") + " access to " + eType + ": " + entity;
            case NotificationService.ACTIVITY_DUE_SOON  -> "Activity due soon: " + entity;
            case NotificationService.DAILY_INSIGHT       -> "Your daily CRM insight is ready";
            case NotificationService.CUSTOM_MESSAGE      -> "Message from " + actor + ": " + p.path("customSubject").asText("New message");
            default -> "You have a new notification";
        };
    }

    private boolean isOptedOut(User user, String notificationType) {
        if (user.getPreferences() == null) return false;
        try {
            JsonNode prefs = objectMapper.readTree(user.getPreferences());
            JsonNode n = prefs.path("notifications");
            if (n.isMissingNode()) return false;
            String key = prefKey(notificationType);
            if (key == null) return false;
            JsonNode flag = n.path(key);
            return flag.isBoolean() && !flag.asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMuted(User user) {
        if (user.getPreferences() == null) return false;
        try {
            JsonNode prefs = objectMapper.readTree(user.getPreferences());
            JsonNode n = prefs.path("notifications");
            if (n.isMissingNode()) return false;
            // Check simple muted flag
            if (n.path("muted").asBoolean(false)) return true;
            // Check mutedUntil timestamp
            JsonNode until = n.path("mutedUntil");
            if (!until.isMissingNode() && !until.isNull()) {
                LocalDateTime mutedUntil = LocalDateTime.parse(until.asText());
                return LocalDateTime.now().isBefore(mutedUntil);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String prefKey(String notificationType) {
        return switch (notificationType) {
            case NotificationService.RECORD_MODIFIED    -> "recordModified";
            case NotificationService.OWNERSHIP_ASSIGNED -> "ownershipAssigned";
            case NotificationService.ACCESS_GRANTED     -> "accessGranted";
            case NotificationService.ACTIVITY_DUE_SOON  -> "activityDueSoon";
            case NotificationService.DAILY_INSIGHT       -> "dailyInsight";
            case NotificationService.CUSTOM_MESSAGE      -> "customMessage";
            default -> null;
        };
    }

    private Map<String, String> buildPlaceholderVariables(String actorName, String entityType, String entityName,
                                                          String entityId, String permission, String dueAt,
                                                          String insight, String recipientEmail, String senderName,
                                                          String customSubject, String customBody, String logoUrl) {
        Map<String, String> vars = new HashMap<>();
        if (actorName != null) vars.put("actorName", actorName);
        if (entityType != null) vars.put("entityType", entityType);
        if (entityName != null) vars.put("entityName", entityName);
        if (entityId != null) vars.put("entityId", entityId);
        if (permission != null) vars.put("permission", permission);
        if (dueAt != null) vars.put("dueAt", dueAt);
        if (insight != null) vars.put("insightText", insight);
        if (recipientEmail != null) vars.put("recipientEmail", recipientEmail);
        if (senderName != null) vars.put("senderName", senderName);
        if (customSubject != null) vars.put("customSubject", customSubject);
        if (customBody != null) vars.put("customBody", customBody);
        if (logoUrl != null && !logoUrl.isBlank()) {
            vars.put("logoUrl", "<img src=\"" + logoUrl + "\" style=\"height:36px;vertical-align:middle\">");
        }
        return vars;
    }
}
