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
export interface SuggestedCustomField {
  key: string;
  label: string;
  type: string;
  description: string;
  config: any;
}

export interface SuggestedCalculatedField {
  key: string;
  label: string;
  expression: string;
  description: string;
}

export interface SuggestedPolicy {
  name: string;
  entityType: string;
  expression: string;
  severity: string;
  description: string;
}

export interface SuggestedCustomer {
  name: string;
  status: string;
  description: string;
}

export interface OnboardingSuggestionsDTO {
  suggestedCustomFields: SuggestedCustomField[];
  suggestedCalculatedFields: SuggestedCalculatedField[];
  suggestedPolicies: SuggestedPolicy[];
  suggestedSampleCustomers: SuggestedCustomer[];
}
