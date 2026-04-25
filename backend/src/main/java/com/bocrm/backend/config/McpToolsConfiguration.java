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
package com.bocrm.backend.config;

import com.bocrm.backend.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes all CRM tool beans as ToolCallbackProvider instances for the MCP server.
 * The Spring AI MCP server autoconfiguration collects these and makes them available
 * as MCP tools via the SSE endpoint at /api/mcp/message.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Configuration
public class McpToolsConfiguration {

    /**
     * Wrap all CRM tool beans in a single ToolCallbackProvider.
     * The MCP server autoconfigure will discover this and register the tools.
     */
    @Bean
    public ToolCallbackProvider crmToolsProvider(
            CrmTools crmTools,
            AdminTools adminTools,
            DocumentGenerationTools documentGenerationTools,
            WebSearchTools webSearchTools,
            PolicyManagementTools policyManagementTools,
            SystemAdminTools systemAdminTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(crmTools, adminTools, documentGenerationTools, webSearchTools,
                        policyManagementTools, systemAdminTools)
                .build();
    }
}
