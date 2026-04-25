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

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders HTML email content for each notification type.
 * All templates are inline HTML with CSS variables — no external template engine needed.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    public record EmailContent(String subject, String htmlBody) {}

    public EmailContent render(String notificationType, String senderName,
                               String actorDisplayName, String entityType, String entityName,
                               String entityId, String permission, String dueAt, String insightText) {
        return render(notificationType, senderName, actorDisplayName, entityType, entityName, entityId, permission, dueAt, insightText, null, null, null);
    }

    public EmailContent render(String notificationType, String senderName,
                               String actorDisplayName, String entityType, String entityName,
                               String entityId, String permission, String dueAt, String insightText,
                               String customSubject, String customBody) {
        return render(notificationType, senderName, actorDisplayName, entityType, entityName, entityId, permission, dueAt, insightText, customSubject, customBody, null);
    }

    public EmailContent render(String notificationType, String senderName,
                               String actorDisplayName, String entityType, String entityName,
                               String entityId, String permission, String dueAt, String insightText,
                               String customSubject, String customBody, String logoUrl) {
        return switch (notificationType) {
            case NotificationService.RECORD_MODIFIED    -> renderRecordModified(senderName, actorDisplayName, entityType, entityName, logoUrl);
            case NotificationService.OWNERSHIP_ASSIGNED -> renderOwnershipAssigned(senderName, actorDisplayName, entityType, entityName, logoUrl);
            case NotificationService.ACCESS_GRANTED     -> renderAccessGranted(senderName, actorDisplayName, entityType, entityName, permission, logoUrl);
            case NotificationService.ACTIVITY_DUE_SOON  -> renderActivityDueSoon(senderName, entityName, dueAt, logoUrl);
            case NotificationService.DAILY_INSIGHT       -> renderDailyInsight(senderName, insightText, logoUrl);
            case NotificationService.CUSTOM_MESSAGE      -> renderCustomMessage(senderName, actorDisplayName, entityType, entityName, customSubject, customBody, logoUrl);
            default -> new EmailContent("[" + senderName + "] Notification", wrap(senderName, logoUrl, "You have a new notification."));
        };
    }

    private EmailContent renderRecordModified(String sender, String actor, String entityType, String name, String logoUrl) {
        String subject = "[" + sender + "] " + actor + " updated your " + entityType + ": " + name;
        String body = wrap(sender, logoUrl,
                "<p style='font-size:16px;margin:0 0 12px'>" +
                "<strong>" + escape(actor) + "</strong> updated a " + escape(entityType) +
                " you own:</p>" +
                "<div style='background:#f8f9fa;border-left:4px solid #3B82F6;padding:12px 16px;border-radius:4px;margin-bottom:16px'>" +
                "<strong>" + escape(name) + "</strong></div>" +
                "<p style='color:#6B7280;font-size:13px'>Log in to review the changes.</p>");
        return new EmailContent(subject, body);
    }

    private EmailContent renderOwnershipAssigned(String sender, String actor, String entityType, String name, String logoUrl) {
        String subject = "[" + sender + "] You've been assigned a " + entityType + ": " + name;
        String body = wrap(sender, logoUrl,
                "<p style='font-size:16px;margin:0 0 12px'>" +
                "<strong>" + escape(actor) + "</strong> assigned you as the owner of a " + escape(entityType) + ":</p>" +
                "<div style='background:#f8f9fa;border-left:4px solid #10B981;padding:12px 16px;border-radius:4px;margin-bottom:16px'>" +
                "<strong>" + escape(name) + "</strong></div>" +
                "<p style='color:#6B7280;font-size:13px'>Log in to view your new record.</p>");
        return new EmailContent(subject, body);
    }

    private EmailContent renderAccessGranted(String sender, String actor, String entityType, String name, String permission, String logoUrl) {
        String perm = permission != null ? permission : "READ";
        String subject = "[" + sender + "] You've been granted " + perm + " access to " + entityType + ": " + name;
        String body = wrap(sender, logoUrl,
                "<p style='font-size:16px;margin:0 0 12px'>" +
                "<strong>" + escape(actor) + "</strong> granted you <strong>" + escape(perm) +
                "</strong> access to a " + escape(entityType) + ":</p>" +
                "<div style='background:#f8f9fa;border-left:4px solid #8B5CF6;padding:12px 16px;border-radius:4px;margin-bottom:16px'>" +
                "<strong>" + escape(name) + "</strong></div>" +
                "<p style='color:#6B7280;font-size:13px'>Log in to view it.</p>");
        return new EmailContent(subject, body);
    }

    private EmailContent renderActivityDueSoon(String sender, String subject_, String dueAt, String logoUrl) {
        String dueLabel = dueAt != null ? dueAt : "soon";
        try {
            LocalDateTime dt = LocalDateTime.parse(dueAt);
            dueLabel = dt.format(DATE_FMT);
        } catch (Exception ignored) {}

        String subject = "[" + sender + "] Reminder: Activity due — " + subject_;
        String body = wrap(sender, logoUrl,
                "<p style='font-size:16px;margin:0 0 12px'>You have an activity due soon:</p>" +
                "<div style='background:#f8f9fa;border-left:4px solid #F59E0B;padding:12px 16px;border-radius:4px;margin-bottom:16px'>" +
                "<strong>" + escape(subject_) + "</strong><br>" +
                "<span style='color:#6B7280;font-size:13px'>Due: " + escape(dueLabel) + "</span></div>" +
                "<p style='color:#6B7280;font-size:13px'>Log in to complete or update it.</p>");
        return new EmailContent(subject, body);
    }

    private EmailContent renderDailyInsight(String sender, String insight, String logoUrl) {
        String subject = "[" + sender + "] Your daily CRM insight";
        String body = wrap(sender, logoUrl,
                "<p style='font-size:16px;margin:0 0 12px'>Your daily CRM summary:</p>" +
                "<div style='background:#f8f9fa;border-left:4px solid #3B82F6;padding:16px;border-radius:4px;margin-bottom:16px;" +
                "font-style:italic;color:#374151'>" + escape(insight) + "</div>" +
                "<p style='color:#6B7280;font-size:13px'>Log in to your dashboard for details.</p>");
        return new EmailContent(subject, body);
    }

    private EmailContent renderCustomMessage(String sender, String actor, String entityType, String entityName, String subject, String body, String logoUrl) {
        String emailSubject = "[" + sender + "] " + escape(subject);
        String emailBody = wrap(sender, logoUrl,
                (actor != null ? "<p style='color:#6B7280;font-size:13px;margin:0 0 16px'><strong>" + escape(actor) + "</strong> sent you a message about " + escape(entityType) + ": <strong>" + escape(entityName) + "</strong></p>" : "") +
                "<div style='background:#f8f9fa;border-left:4px solid #6B7280;padding:16px;border-radius:4px;margin-bottom:16px'>" +
                escape(body).replace("\n", "<br>") +
                "</div>");
        return new EmailContent(emailSubject, emailBody);
    }

    private String wrap(String senderName, String logoUrl, String content) {
        String logoHtml = (logoUrl != null && !logoUrl.isBlank())
                ? "<img src=\"" + logoUrl + "\" style=\"height:36px;vertical-align:middle;margin-right:12px;border-radius:4px\">"
                : "";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
                "margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;" +
                "background:#F3F4F6'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px'><tr><td align='center'>" +
                "<table width='560' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:8px;" +
                "box-shadow:0 1px 3px rgba(0,0,0,.1);overflow:hidden'>" +
                // Header
                "<tr><td style='background:#3B82F6;padding:20px 28px'>" +
                logoHtml +
                "<span style='color:#fff;font-size:18px;font-weight:700'>" + escape(senderName) + "</span>" +
                "</td></tr>" +
                // Body
                "<tr><td style='padding:28px'>" + content + "</td></tr>" +
                // Footer
                "<tr><td style='background:#F9FAFB;padding:16px 28px;border-top:1px solid #E5E7EB'>" +
                "<p style='margin:0;font-size:12px;color:#9CA3AF'>You received this because you are a member of " +
                escape(senderName) + ". To manage your notification preferences, visit your profile settings.</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
