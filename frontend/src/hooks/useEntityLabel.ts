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
/**
 * @file Hook that resolves a human-readable label for a given entity type and ID.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useAuthStore } from '../store/authStore';

const DEFAULTS: Record<string, string> = {
  Customer: 'Customer',
  Contact: 'Contact',
  Opportunity: 'Opportunity',
  Activity: 'Activity',
  CustomRecord: 'CustomRecord',
  Order: 'Order',
  Invoice: 'Invoice',
};

export const useEntityLabel = (entityType: string): string => {
  const tenantSettings = useAuthStore((state) => state.tenantSettings);
  const labels = tenantSettings?.entityLabels as Record<string, string> | undefined;
  return labels?.[entityType] ?? DEFAULTS[entityType];
};

export const useEntityLabels = (): Record<string, string> => {
  const tenantSettings = useAuthStore((state) => state.tenantSettings);
  const labels = tenantSettings?.entityLabels as Record<string, string> | undefined;
  return Object.fromEntries(
    Object.entries(DEFAULTS).map(([k, v]) => [k, labels?.[k] ?? v])
  ) as Record<string, string>;
};
