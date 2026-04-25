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
 * @file Zustand store for authentication state.
 * Holds the current user, active tenant, available tenants, role, and token lifecycle.
 * Persists tokens to localStorage; exposes login, logout, and tenant-switch actions.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import { apiClient } from '../api/apiClient';
import { useUiStore } from './uiStore';

export interface TenantSummary {
  id: number;
  name: string;
  role: string;
}

export interface User {
  userId: number;
  email: string;
  displayName?: string | null;
  preferences: string | null;
}

export interface AuthState {
  user: User | null;
  currentTenant: { id: number; name: string } | null;
  currentRole: string | null;
  availableTenants: TenantSummary[];
  isAuthenticated: boolean;
  requiresOnboarding: boolean;
  requiresTenantSelection: boolean;
  isLoading: boolean;
  error: string | null;
  tenantSettings: Record<string, any> | null;
  applyLoginResponse: (responseData: any) => void;
  loginWithExternalToken: (idToken: string) => Promise<void>;
  completeOnboarding: (tenantName: string) => Promise<void>;
  switchTenant: (tenantId: number) => Promise<void>;
  logout: () => Promise<void>;
  initializeAuth: () => void;
  updateUserPreferences: (preferences: string) => Promise<void>;
  setTenantSettings: (settings: Record<string, any>) => void;
  refreshTenantSettings: () => Promise<void>;
  refreshMemberships: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  currentTenant: null,
  currentRole: null,
  availableTenants: [],
  isAuthenticated: false,
  requiresOnboarding: false,
  requiresTenantSelection: false,
  isLoading: false,
  error: null,
  tenantSettings: null,

  applyLoginResponse: (responseData: any) => {
    const { userId, accessToken, refreshToken, currentTenantId, currentTenantName, availableTenants, preferences, email, displayName, requiresOnboarding, requiresTenantSelection } = responseData;

    if (requiresOnboarding) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('userId', userId.toString());
      localStorage.setItem('email', email);
      if (displayName) localStorage.setItem('displayName', displayName);
      set({
        user: { userId, email, displayName: displayName || null, preferences: null },
        isAuthenticated: true,
        requiresOnboarding: true,
        requiresTenantSelection: false,
        isLoading: false,
        error: null,
      });
      return;
    }

    if (requiresTenantSelection) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('userId', userId.toString());
      localStorage.setItem('email', email);
      if (displayName) localStorage.setItem('displayName', displayName);
      localStorage.setItem('availableTenants', JSON.stringify(availableTenants));
      localStorage.setItem('requiresTenantSelection', 'true');
      set({
        user: { userId, email, displayName: displayName || null, preferences: null },
        availableTenants: availableTenants || [],
        isAuthenticated: true,
        requiresTenantSelection: true,
        isLoading: false,
        error: null,
      });
      return;
    }

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('userId', userId.toString());
    localStorage.setItem('email', email);
    if (displayName) {
      localStorage.setItem('displayName', displayName);
    } else {
      localStorage.removeItem('displayName');
    }
    localStorage.setItem('currentTenant', JSON.stringify({ id: currentTenantId, name: currentTenantName }));
    localStorage.setItem('availableTenants', JSON.stringify(availableTenants));
    if (preferences) {
      localStorage.setItem('preferences', preferences);
    } else {
      localStorage.removeItem('preferences');
    }

    const currentRole = availableTenants.find((t: TenantSummary) => t.id === currentTenantId)?.role || null;

    set({
      user: { userId, email, displayName: displayName || null, preferences },
      currentTenant: { id: currentTenantId, name: currentTenantName },
      currentRole,
      availableTenants: availableTenants,
      isAuthenticated: true,
      requiresOnboarding: false,
      requiresTenantSelection: false,
      isLoading: false,
      error: null,
    });
  },

  loginWithExternalToken: async (idToken: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await apiClient.externalLogin(idToken);
      get().applyLoginResponse(response.data);
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'External login failed';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  completeOnboarding: async (tenantName: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await apiClient.onboard(tenantName);
      get().applyLoginResponse(response.data);
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to create workspace';
      set({ error: errorMessage, isLoading: false });
      throw error;
    }
  },

  switchTenant: async (tenantId: number) => {
    set({ isLoading: true, error: null });
    try {
      const response = await apiClient.switchTenant(tenantId);
      const { accessToken, refreshToken, currentTenantId, currentTenantName, availableTenants } = response.data;

      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('currentTenant', JSON.stringify({ id: currentTenantId, name: currentTenantName }));
      localStorage.setItem('availableTenants', JSON.stringify(availableTenants));
      localStorage.removeItem('requiresTenantSelection');

      const currentRole = availableTenants.find((t: TenantSummary) => t.id === currentTenantId)?.role || null;

      set({
        currentTenant: { id: currentTenantId, name: currentTenantName },
        currentRole,
        availableTenants: availableTenants,
        requiresTenantSelection: false,
        isLoading: false,
      });

      // Reload page to refresh all data with new tenant context
      window.location.reload();
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to switch tenant';
      set({
        error: errorMessage,
        isLoading: false,
      });
      throw error;
    }
  },

  logout: async () => {
    try {
      // Clear assistant messages before logout (to prevent cross-user leakage)
      useUiStore.getState().clearAssistantMessages();
      // Notify backend of logout before clearing tokens
      await apiClient.logout();
    } catch (error) {
      // Log logout errors but continue with local logout
      console.warn('Logout notification to server failed', error);
    } finally {
      // Always clear local tokens regardless of server response
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('email');
      localStorage.removeItem('displayName');
      localStorage.removeItem('currentTenant');
      localStorage.removeItem('availableTenants');
      localStorage.removeItem('preferences');
      localStorage.removeItem('tenantSettings');
      localStorage.removeItem('requiresTenantSelection');
      set({
        user: null,
        currentTenant: null,
        currentRole: null,
        availableTenants: [],
        isAuthenticated: false,
        requiresOnboarding: false,
        requiresTenantSelection: false,
        error: null,
        tenantSettings: null,
      });
    }
  },

  initializeAuth: () => {
    const accessToken = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    const email = localStorage.getItem('email');
    const displayName = localStorage.getItem('displayName');
    const currentTenantStr = localStorage.getItem('currentTenant');
    const availableTenantsStr = localStorage.getItem('availableTenants');
    const preferences = localStorage.getItem('preferences');

    if (accessToken && userId) {
      let currentTenant: any = null;
      let availableTenants: TenantSummary[] = [];
      let currentRole = null;
      let tenantSettings: Record<string, any> | null = null;
      const requiresTenantSelection = localStorage.getItem('requiresTenantSelection') === 'true';

      try {
        if (currentTenantStr) currentTenant = JSON.parse(currentTenantStr);
        if (availableTenantsStr) availableTenants = JSON.parse(availableTenantsStr);
        const tenantSettingsStr = localStorage.getItem('tenantSettings');
        if (tenantSettingsStr) tenantSettings = JSON.parse(tenantSettingsStr);

        if (currentTenant && availableTenants.length > 0) {
            currentRole = availableTenants.find((t: TenantSummary) => t.id === currentTenant.id)?.role || null;
        }
      } catch (e) {
        console.error('Failed to parse auth data from local storage', e);
      }

      set({
        user: { userId: parseInt(userId), email: email || '', displayName: displayName || null, preferences },
        currentTenant,
        currentRole,
        availableTenants,
        isAuthenticated: true,
        requiresTenantSelection,
        isLoading: false,
        tenantSettings,
      });
    } else {
      // No auth data in localStorage, mark as not loading
      set({
        isLoading: false,
      });
    }
  },

  updateUserPreferences: async (preferences: string) => {
    const { user } = get();
    if (!user) return;

    try {
      await apiClient.updateUserPreferences({ preferences });
      localStorage.setItem('preferences', preferences);
      set({ user: { ...user, preferences } });
    } catch (error) {
      console.error('Failed to update preferences', error);
    }
  },

  setTenantSettings: (settings: Record<string, any>) => {
    localStorage.setItem('tenantSettings', JSON.stringify(settings));
    set({ tenantSettings: settings });
  },

  refreshTenantSettings: async () => {
    try {
      const res = await apiClient.getCurrentTenant();
      const settings = res.data?.settings;
      if (settings && typeof settings === 'object') {
        localStorage.setItem('tenantSettings', JSON.stringify(settings));
        set({ tenantSettings: settings });
      }
    } catch {
      // non-critical
    }
  },

  refreshMemberships: async () => {
    const { user } = get();
    if (!user) return;

    try {
      const response = await apiClient.getUserMemberships(user.userId);
      const memberships = response.data || [];

      const availableTenants = memberships.map((m: any) => ({
        id: m.tenantId,
        name: m.tenantName,
        role: m.role,
      }));

      localStorage.setItem('availableTenants', JSON.stringify(availableTenants));
      set({ availableTenants });
    } catch (error) {
      console.error('Failed to refresh memberships', error);
    }
  },
}));
