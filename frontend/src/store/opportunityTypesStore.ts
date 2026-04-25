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
 * @file Zustand store for the tenant's configured opportunity types (used in sidebar and filters).
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';

export interface OpportunityTypeDTO {
  id: number;
  name: string;
  slug: string;
  description?: string;
  displayOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

interface OpportunityTypesState {
  types: OpportunityTypeDTO[];
  isLoading: boolean;
  fetchTypes: () => Promise<void>;
  createType: (data: { name: string; description?: string }) => Promise<void>;
  updateType: (id: number, data: { name: string; description?: string; displayOrder?: number }) => Promise<void>;
  deleteType: (id: number) => Promise<void>;
}

export const useOpportunityTypesStore = create<OpportunityTypesState>((set) => ({
  types: [],
  isLoading: false,

  fetchTypes: async () => {
    set({ isLoading: true });
    try {
      const res = await apiClient.getOpportunityTypes();
      set({ types: res.data || [] });
    } catch {
      // non-critical — leave types empty
    } finally {
      set({ isLoading: false });
    }
  },

  createType: async (data) => {
    const res = await apiClient.createOpportunityType(data);
    set((state) => ({ types: [...state.types, res.data] }));
  },

  updateType: async (id, data) => {
    const res = await apiClient.updateOpportunityType(id, data);
    set((state) => ({
      types: state.types.map((t) => (t.id === id ? res.data : t)),
    }));
  },

  deleteType: async (id) => {
    await apiClient.deleteOpportunityType(id);
    set((state) => ({ types: state.types.filter((t) => t.id !== id) }));
  },
}));
