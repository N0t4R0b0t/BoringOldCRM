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

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * CreateCalculatedFieldDefinitionRequest.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCalculatedFieldDefinitionRequest {
    @NotBlank(message = "Entity type is required")
    @Pattern(regexp = "^(Customer|Contact|Opportunity(:[\\w-]+)?|Activity|CustomRecord|Order|Invoice)$",
             message = "Entity type must be one of: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice")
    private String entityType;

    @NotBlank(message = "Key is required")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Key must be alphanumeric and underscore only")
    private String key;

    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "Expression is required")
    private String expression;

    @NotBlank(message = "Return type is required")
    @Pattern(regexp = "^(text|number|boolean|date)$",
             message = "Return type must be one of: text, number, boolean, date")
    private String returnType;

    private JsonNode config;
    private Boolean enabled;
    private Boolean displayInTable;
}
