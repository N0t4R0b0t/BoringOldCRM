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
 * @file Zustand store for core CRM entity data (customers, etc.).
 * Wraps apiClient calls and exposes add/update/remove mutations for optimistic UI updates.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';

export interface Customer {
  id: number;
  name: string;
  status: string;
  ownerId?: number;
  createdAt: string;
  updatedAt: string;
  customFields?: Record<string, any>;
}

export interface CrmState {
  customers: Customer[];
  isLoading: boolean;
  error: string | null;
  fetchCustomers: () => Promise<void>;
  addCustomer: (customer: Customer) => void;
  removeCustomer: (id: number) => void;
  updateCustomer: (id: number, customer: Partial<Customer>) => void;
}

export const useCrmStore = create<CrmState>((set) => ({
  customers: [],
  isLoading: false,
  error: null,

  fetchCustomers: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await apiClient.getCustomers();
      // response.data may be a PagedResponse { content: [...], ... } or an array
      const data = response.data;
      const customersArray = Array.isArray(data) ? data : (data.content || []);
      set({ customers: customersArray, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Failed to fetch customers',
        isLoading: false,
      });
    }
  },

  addCustomer: (customer: Customer) => {
    set((state) => ({
      customers: [...state.customers, customer],
    }));
  },

  removeCustomer: (id: number) => {
    set((state) => ({
      customers: state.customers.filter((c) => c.id !== id),
    }));
  },

  updateCustomer: (id: number, updates: Partial<Customer>) => {
    set((state) => ({
      customers: state.customers.map((c) =>
        c.id === id ? { ...c, ...updates } : c
      ),
    }));
  },
}));
