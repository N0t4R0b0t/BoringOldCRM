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

import com.bocrm.backend.dto.ReportPreviewRequest;
import com.bocrm.backend.dto.ReportPreviewResponse;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.tools.DocumentGenerationTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
/**
 * ReportBuilderService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class ReportBuilderService {

    private final DocumentGenerationTools documentGenerationTools;

    public ReportBuilderService(DocumentGenerationTools documentGenerationTools) {
        this.documentGenerationTools = documentGenerationTools;
    }

    public ReportPreviewResponse preview(ReportPreviewRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ForbiddenException("Tenant context not set");

        String reportType = request.getReportType();
        if (reportType == null || reportType.isBlank()) {
            reportType = "slide_deck";
        }

        return switch (reportType) {
            case "one_pager" -> {
                String content = documentGenerationTools.previewOnePager(
                        request.getEntityType(), request.getEntityId(),
                        request.getTitle(), request.getStyleJson(), request.getTemplateId());
                yield new ReportPreviewResponse(content, "text/markdown", "one_pager");
            }
            default -> {
                String content = documentGenerationTools.previewSlideDeck(
                        request.getEntityType(), request.getEntityId(),
                        request.getTitle(), request.getStyleJson(), request.getTemplateId());
                yield new ReportPreviewResponse(content, "text/html", "slide_deck");
            }
        };
    }
}
