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
 * @file Zustand store for custom field definitions.
 * Caches field definitions per entity type and exposes load/reload actions.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';

export interface CustomField {
  id: number;
  entityType: string;
  key: string;
  type: string;
  label: string;
  required: boolean;
  config?: any;
  displayInTable?: boolean;
  displayOrder?: number;
}

export interface CustomFieldsState {
  fields: Record<string, CustomField[]>; // Keyed by entityType
  isLoading: Record<string, boolean>;
  error: Record<string, string | null>;
  fetchCustomFields: (entityType: string) => Promise<void>;
  getFieldsByEntityType: (entityType: string) => CustomField[];
  clearCache: (entityType?: string) => void;
}

export const useCustomFieldsStore = create<CustomFieldsState>((set, get) => ({
  fields: {},
  isLoading: {},
  error: {},

  fetchCustomFields: async (entityType: string) => {
    set((state) => ({
      isLoading: { ...state.isLoading, [entityType]: true },
      error: { ...state.error, [entityType]: null },
    }));

    try {
      const response = await apiClient.getCustomFieldDefinitions(entityType);
      const fields = (response.data || [])
        .map((f: any) => ({ ...f, displayInTable: !!f.displayInTable, displayOrder: f.displayOrder ?? 0 }))
        .sort((a: CustomField, b: CustomField) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      set((state) => ({
        fields: { ...state.fields, [entityType]: fields },
        isLoading: { ...state.isLoading, [entityType]: false },
      }));
    } catch (error: any) {
      set((state) => ({
        error: {
          ...state.error,
          [entityType]: error.response?.data?.message || 'Failed to fetch custom fields',
        },
        isLoading: { ...state.isLoading, [entityType]: false },
      }));
    }
  },

  getFieldsByEntityType: (entityType: string) => {
    return get().fields[entityType] || [];
  },

  clearCache: (entityType?: string) => {
    if (entityType) {
      set((state) => {
        const fields = { ...state.fields };
        delete fields[entityType];
        return { fields };
      });
    } else {
      set({ fields: {} });
    }
  },
}));
