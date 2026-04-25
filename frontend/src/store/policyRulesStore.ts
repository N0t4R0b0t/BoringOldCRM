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
 * @file Zustand store for business policy rules.
 * Fetches rules from the backend and exposes evaluateRules() for pre-submit validation.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';

export interface PolicyViolationDetail {
  ruleId: number;
  ruleName: string;
  message: string;
  severity: 'DENY' | 'WARN';
}

export interface PolicyRule {
  id: number;
  entityType: string;
  operation: 'CREATE' | 'UPDATE' | 'DELETE';
  name: string;
  description?: string;
  expression: string;
  severity: 'DENY' | 'WARN';
  enabled: boolean;
  displayOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PolicyRulesState {
  rules: Record<string, PolicyRule[]>;
  isLoading: Record<string, boolean>;
  error: Record<string, string | null>;
  fetchPolicyRules: (entityType: string) => Promise<void>;
  getRulesByEntityType: (entityType: string) => PolicyRule[];
  evaluateRules: (entityType: string, operation: string, entityData: any, previousData?: any) => Promise<{ blocked: boolean; violations: PolicyViolationDetail[]; warnings: PolicyViolationDetail[] }>;
  clearCache: (entityType?: string) => void;
}

export const usePolicyRulesStore = create<PolicyRulesState>((set, get) => ({
  rules: {},
  isLoading: {},
  error: {},

  fetchPolicyRules: async (entityType: string) => {
    set((state) => ({
      isLoading: { ...state.isLoading, [entityType]: true },
      error: { ...state.error, [entityType]: null },
    }));

    try {
      const response = await apiClient.getPolicyRuleDefinitions(entityType);
      const rules = (response.data || [])
        .map((r: any) => ({ ...r, displayOrder: r.displayOrder ?? 0 }))
        .sort((a: PolicyRule, b: PolicyRule) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      set((state) => ({
        rules: { ...state.rules, [entityType]: rules },
        isLoading: { ...state.isLoading, [entityType]: false },
      }));
    } catch (error: any) {
      set((state) => ({
        error: {
          ...state.error,
          [entityType]: error.response?.data?.message || 'Failed to fetch policy rules',
        },
        isLoading: { ...state.isLoading, [entityType]: false },
      }));
    }
  },

  getRulesByEntityType: (entityType: string) => {
    return get().rules[entityType] || [];
  },

  evaluateRules: async (entityType: string, operation: string, entityData: any, previousData?: any) => {
    try {
      const response = await apiClient.evaluatePolicyRules({
        entityType,
        operation,
        entityData,
        previousData,
      });
      return response.data;
    } catch (error: any) {
      console.error('Failed to evaluate policy rules:', error);
      return { blocked: false, violations: [], warnings: [] };
    }
  },

  clearCache: (entityType?: string) => {
    if (entityType) {
      set((state) => {
        const rules = { ...state.rules };
        delete rules[entityType];
        return { rules };
      });
    } else {
      set({ rules: {} });
    }
  },
}));
