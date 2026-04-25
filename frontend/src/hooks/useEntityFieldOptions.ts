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
 * @file Hook that resolves dynamic select-option lists for linked entity custom fields.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';

interface FieldOption {
  value: string;
  label: string;
}

// Simple in-module cache to avoid duplicate fetches across form instances
const cache: Record<string, FieldOption[]> = {};
const DEFAULT_FALLBACKS: Record<string, Record<string, FieldOption[]>> = {
  Customer: {
    status: [
      { value: 'active', label: 'Active' },
      { value: 'inactive', label: 'Inactive' },
      { value: 'prospect', label: 'Prospect' },
    ],
  },
  Opportunity: {
    stage: [
      { value: 'open', label: 'Open' },
      { value: 'closed-won', label: 'Closed Won' },
      { value: 'closed-lost', label: 'Closed Lost' },
    ],
  },
  Activity: {
    type: [
      { value: 'call', label: 'Call' },
      { value: 'email', label: 'Email' },
      { value: 'meeting', label: 'Meeting' },
      { value: 'note', label: 'Note' },
    ],
  },
  CustomRecord: {
    status: [
      { value: 'active', label: 'Active' },
      { value: 'inactive', label: 'Inactive' },
      { value: 'maintenance', label: 'Maintenance' },
      { value: 'disposed', label: 'Disposed' },
    ],
  },
};

export const useEntityFieldOptions = (entityType: string, fieldName: string): FieldOption[] => {
  const cacheKey = `${entityType}:${fieldName}`;
  const fallback = DEFAULT_FALLBACKS[entityType]?.[fieldName] ?? [];

  const [options, setOptions] = useState<FieldOption[]>(cache[cacheKey] ?? fallback);

  useEffect(() => {
    if (cache[cacheKey]) {
      setOptions(cache[cacheKey]);
      return;
    }
    apiClient.getEntityFieldOptions(entityType)
      .then((res) => {
        const allOptions: Record<string, FieldOption[]> = res.data || {};
        // Populate cache for all fields returned
        Object.entries(allOptions).forEach(([fn, opts]) => {
          cache[`${entityType}:${fn}`] = opts as FieldOption[];
        });
        const fetched = allOptions[fieldName];
        if (fetched && fetched.length > 0) {
          setOptions(fetched);
        } else {
          setOptions(fallback);
        }
      })
      .catch(() => {
        // On network error fall back to hardcoded defaults
        setOptions(fallback);
      });
  }, [entityType, fieldName]);

  return options;
};

// Color palette for cycling through badge colors
const BADGE_PALETTE = ['blue', 'green', 'yellow', 'red', 'purple', 'orange', 'teal', 'gray'];

// Get the display label for an option value
export const getOptionLabel = (value: string, options: FieldOption[]): string => {
  return options.find((o) => o.value === value)?.label ?? value;
};

// Get a badge color for an option value, cycling through the palette by index
export const getOptionBadgeColor = (value: string, options: FieldOption[]): string => {
  const idx = options.findIndex((o) => o.value === value);
  if (idx >= 0) {
    return BADGE_PALETTE[idx % BADGE_PALETTE.length];
  }
  // Fallback to last color if value not found
  return BADGE_PALETTE[BADGE_PALETTE.length - 1];
};

// Clear the cache for an entity type (used after saving options)
export const clearOptionCache = (entityType: string): void => {
  Object.keys(cache).forEach((key) => {
    if (key.startsWith(`${entityType}:`)) {
      delete cache[key];
    }
  });
};
