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
 * @file Zustand store for calculated field values, indexed by entity type and ID.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';

export interface CalculatedField {
  id: number;
  entityType: string;
  key: string;
  label: string;
  expression: string;
  returnType: string;
  enabled: boolean;
  config?: any;
  displayInTable?: boolean;
  displayOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CalculatedFieldsState {
  fields: Record<string, CalculatedField[]>;
  isLoading: Record<string, boolean>;
  error: Record<string, string | null>;
  fetchCalculatedFields: (entityType: string) => Promise<void>;
  getFieldsByEntityType: (entityType: string) => CalculatedField[];
  clearCache: (entityType?: string) => void;
}

export const useCalculatedFieldsStore = create<CalculatedFieldsState>((set, get) => ({
  fields: {},
  isLoading: {},
  error: {},

  fetchCalculatedFields: async (entityType: string) => {
    set((state) => ({
      isLoading: { ...state.isLoading, [entityType]: true },
      error: { ...state.error, [entityType]: null },
    }));

    try {
      const response = await apiClient.getCalculatedFieldDefinitions(entityType);
      const fields = (response.data || [])
        .map((f: any) => ({ ...f, displayOrder: f.displayOrder ?? 0 }))
        .sort((a: CalculatedField, b: CalculatedField) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      set((state) => ({
        fields: { ...state.fields, [entityType]: fields },
        isLoading: { ...state.isLoading, [entityType]: false },
      }));
    } catch (error: any) {
      set((state) => ({
        error: {
          ...state.error,
          [entityType]: error.response?.data?.message || 'Failed to fetch calculated fields',
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
