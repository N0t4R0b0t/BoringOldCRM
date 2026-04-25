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


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
/**
 * CreateCustomFieldDefinitionRequest.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomFieldDefinitionRequest {
    @NotBlank(message = "Entity type is required")
    @Pattern(regexp = "^(Customer|Contact|Opportunity(:[\\w-]+)?|Activity|CustomRecord|Order|Invoice)$",
             message = "Entity type must be one of: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice")
    private String entityType;

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(text|number|select|date|boolean|textarea|multiselect|url|email|phone|currency|percentage|richtext|document|document_multi|custom_record|custom_record_multi|contact|contact_multi|workflow)$",
             message = "Type must be one of: text, number, select, date, boolean, textarea, multiselect, url, email, phone, currency, percentage, richtext, document, document_multi, custom_record, custom_record_multi, contact, contact_multi, workflow")
    private String type;

    private JsonNode config;
    private Boolean required;
    private Boolean displayInTable; // New field for UI control
}
