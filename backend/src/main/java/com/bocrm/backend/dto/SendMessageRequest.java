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
package com.bocrm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * SendMessageRequest.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    private String message;
    private String contextEntityType;
    private Long contextEntityId;
    private Boolean internal; // true for internal notes, false for external messages
    // New assistant fields
    private String sessionId;
    private String confirmMode; // "confirm" or "auto"
    private Boolean executeActions; // true = execute stored pendingActions for sessionId
    private String pageContext; // current page the user is viewing, e.g. "Customers list"
    // File attachment support
    private String attachmentBase64; // base64-encoded file content
    private String attachmentMimeType; // MIME type (e.g., "application/pdf", "image/png", "text/csv")
    private String attachmentFileName; // original filename for reference
}
