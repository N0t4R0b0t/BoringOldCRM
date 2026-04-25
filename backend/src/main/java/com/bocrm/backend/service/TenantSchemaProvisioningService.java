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

import com.bocrm.backend.shared.TenantSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
/**
 * TenantSchemaProvisioningService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaProvisioningService {

    private final JdbcTemplate jdbcTemplate;

    public void seedDefaultDocumentTemplates(Long tenantId, String schema) {
        String sql1 = "INSERT INTO " + schema + ".document_templates (tenant_id, name, description, template_type, style_json, is_default, created_at, updated_at) " +
                "SELECT " + tenantId + ", 'Corporate Dark', 'Professional dark theme with corporate branding', 'slide_deck', " +
                "'{\"layout\":\"dark\",\"accentColor\":\"#1a3a5c\",\"backgroundColor\":\"#0f1419\",\"textColor\":\"#e0e0e0\",\"h1Color\":\"#ffffff\",\"h2Color\":\"#b0d4ff\"}', true, NOW(), NOW() " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".document_templates WHERE tenant_id = " + tenantId + " AND name = 'Corporate Dark')";
        String sql2 = "INSERT INTO " + schema + ".document_templates (tenant_id, name, description, template_type, style_json, is_default, created_at, updated_at) " +
                "SELECT " + tenantId + ", 'Light Minimal', 'Clean light theme', 'one_pager', " +
                "'{\"layout\":\"detailed\",\"accentColor\":\"#2563eb\",\"backgroundColor\":\"#ffffff\",\"textColor\":\"#1f2937\",\"includeCustomFields\":true}', true, NOW(), NOW() " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".document_templates WHERE tenant_id = " + tenantId + " AND name = 'Light Minimal')";
        String sql3 = "INSERT INTO " + schema + ".document_templates (tenant_id, name, description, template_type, style_json, is_default, created_at, updated_at) " +
                "SELECT " + tenantId + ", 'Standard Report', 'Default business report style', 'report', " +
                "'{\"layout\":\"detailed\",\"accentColor\":\"#059669\",\"backgroundColor\":\"#f9fafb\",\"textColor\":\"#111827\"}', true, NOW(), NOW() " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".document_templates WHERE tenant_id = " + tenantId + " AND name = 'Standard Report')";
        String sql4 = "INSERT INTO " + schema + ".document_templates (tenant_id, name, description, template_type, style_json, is_default, created_at, updated_at) " +
                "SELECT " + tenantId + ", 'Full Export', 'Complete CSV export with all fields', 'csv_export', " +
                "'{}', true, NOW(), NOW() " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".document_templates WHERE tenant_id = " + tenantId + " AND name = 'Full Export')";
        try {
            jdbcTemplate.execute(sql1);
            jdbcTemplate.execute(sql2);
            jdbcTemplate.execute(sql3);
            jdbcTemplate.execute(sql4);
            log.debug("Seeded default document templates for tenant {}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to seed default document templates for tenant {}: {}", tenantId, e.getMessage());
        }
    }

    public void seedDefaultFieldOptions(Long tenantId, String schema) {
        String[][] seeds = {
            { "Customer", "status",
              "[{\"value\":\"active\",\"label\":\"Active\"},{\"value\":\"inactive\",\"label\":\"Inactive\"},{\"value\":\"prospect\",\"label\":\"Prospect\"}]" },
            { "Opportunity", "stage",
              "[{\"value\":\"open\",\"label\":\"Open\"},{\"value\":\"closed-won\",\"label\":\"Closed Won\"},{\"value\":\"closed-lost\",\"label\":\"Closed Lost\"}]" },
            { "Activity", "type",
              "[{\"value\":\"call\",\"label\":\"Call\"},{\"value\":\"email\",\"label\":\"Email\"},{\"value\":\"meeting\",\"label\":\"Meeting\"},{\"value\":\"note\",\"label\":\"Note\"}]" },
            { "CustomRecord", "status",
              "[{\"value\":\"active\",\"label\":\"Active\"},{\"value\":\"inactive\",\"label\":\"Inactive\"},{\"value\":\"maintenance\",\"label\":\"Maintenance\"},{\"value\":\"disposed\",\"label\":\"Disposed\"}]" },
        };
        try {
            for (String[] seed : seeds) {
                String sql = "INSERT INTO " + schema + ".entity_field_options "
                        + "(tenant_id, entity_type, field_name, options, created_at, updated_at) "
                        + "SELECT " + tenantId + ", '" + seed[0] + "', '" + seed[1] + "', '" + seed[2] + "'::jsonb, NOW(), NOW() "
                        + "WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".entity_field_options "
                        + "WHERE tenant_id = " + tenantId + " AND entity_type = '" + seed[0] + "' AND field_name = '" + seed[1] + "')";
                jdbcTemplate.execute(sql);
            }
            log.debug("Seeded default field options for tenant {}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to seed default field options for tenant {}: {}", tenantId, e.getMessage());
        }
    }
}
