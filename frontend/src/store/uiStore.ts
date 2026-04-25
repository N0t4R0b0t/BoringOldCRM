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
 * @file Zustand store for transient UI state (toast messages, global loading flag, modal visibility).
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';
import type { ReactNode } from 'react';
import type { PendingAction, ChatMessage } from '../components/AssistantBar';

interface UiState {
  headerActions: ReactNode | null;
  setHeaderActions: (actions: ReactNode | null) => void;
  // AI assistant settings
  assistantConfirmMode: 'confirm' | 'auto';
  setAssistantConfirmMode: (mode: 'confirm' | 'auto') => void;
  // AI assistant in-flight state (persisted across navigation in Zustand memory)
  assistantMessages: ChatMessage[];
  setAssistantMessages: (msgs: ChatMessage[]) => void;
  assistantLoading: boolean;
  setAssistantLoading: (v: boolean) => void;
  assistantPendingActions: PendingAction[] | null;
  setAssistantPendingActions: (v: PendingAction[] | null) => void;
  assistantPendingSessionId: string | null;
  setAssistantPendingSessionId: (v: string | null) => void;
  // Prompt history for up/down arrow recall
  assistantPromptHistory: string[];
  addAssistantPromptToHistory: (prompt: string) => void;
  // Clear all assistant state (messages, pending actions, session, history)
  clearAssistantMessages: () => void;
  // Pending filters from assistant applyFilters tool
  pendingFilters: { entityType: string; filters: Record<string, string[]> } | null;
  setPendingFilters: (v: { entityType: string; filters: Record<string, string[]> } | null) => void;
  // Incremented after each assistant action so pages know to re-fetch
  dataRefreshToken: number;
  triggerDataRefresh: () => void;
  // When set, AssistantBar will expand and auto-send this message once, then clear it
  assistantAutoPrompt: string | null;
  setAssistantAutoPrompt: (prompt: string | null) => void;
}

// All assistant storage is keyed by tenantId + userId to prevent cross-tenant and cross-user bleed.
// Both are read from localStorage at call time so they're always current.

function getCurrentTenantId(): number | null {
  try {
    const stored = localStorage.getItem('currentTenant');
    return stored ? JSON.parse(stored).id : null;
  } catch {
    return null;
  }
}

function getCurrentUserId(): number | null {
  try {
    const userId = localStorage.getItem('userId');
    return userId ? parseInt(userId, 10) : null;
  } catch {
    return null;
  }
}

// One-time cleanup of legacy unkeyed storage keys
if (typeof localStorage !== 'undefined') {
  localStorage.removeItem('assistant-messages');
  localStorage.removeItem('assistant-prompt-history');
}

const loadMessagesFromStorage = (): ChatMessage[] => {
  try {
    const tid = getCurrentTenantId();
    const uid = getCurrentUserId();
    if (!tid || !uid) return [];
    const stored = localStorage.getItem(`assistant-messages-${tid}-${uid}`);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
};

const saveMessagesToStorage = (messages: ChatMessage[]) => {
  try {
    const tid = getCurrentTenantId();
    const uid = getCurrentUserId();
    if (!tid || !uid) return;
    localStorage.setItem(`assistant-messages-${tid}-${uid}`, JSON.stringify(messages));
  } catch {
    // silently fail if localStorage is full
  }
};

const loadPromptHistoryFromStorage = (): string[] => {
  try {
    const tid = getCurrentTenantId();
    const uid = getCurrentUserId();
    if (!tid || !uid) return [];
    const stored = localStorage.getItem(`assistant-prompt-history-${tid}-${uid}`);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
};

const savePromptHistoryToStorage = (history: string[]) => {
  try {
    const tid = getCurrentTenantId();
    const uid = getCurrentUserId();
    if (!tid || !uid) return;
    localStorage.setItem(`assistant-prompt-history-${tid}-${uid}`, JSON.stringify(history));
  } catch {
    // silently fail if localStorage is full
  }
};

export const useUiStore = create<UiState>((set) => ({
  headerActions: null,
  setHeaderActions: (actions) => set({ headerActions: actions }),
  assistantConfirmMode: 'auto',
  setAssistantConfirmMode: (mode) => set({ assistantConfirmMode: mode }),
  assistantMessages: loadMessagesFromStorage(),
  setAssistantMessages: (msgs) => {
    saveMessagesToStorage(msgs);
    set({ assistantMessages: msgs });
  },
  assistantLoading: false,
  setAssistantLoading: (v) => set({ assistantLoading: v }),
  assistantPendingActions: null,
  setAssistantPendingActions: (v) => set({ assistantPendingActions: v }),
  assistantPendingSessionId: null,
  setAssistantPendingSessionId: (v) => set({ assistantPendingSessionId: v }),
  assistantPromptHistory: loadPromptHistoryFromStorage(),
  addAssistantPromptToHistory: (prompt) =>
    set((s) => {
      // Add to history, limit to last 50 prompts
      const updated = [prompt, ...s.assistantPromptHistory].slice(0, 50);
      savePromptHistoryToStorage(updated);
      return { assistantPromptHistory: updated };
    }),
  clearAssistantMessages: () =>
    set({
      assistantMessages: [],
      assistantPendingActions: null,
      assistantPendingSessionId: null,
      assistantPromptHistory: [],
    }),
  pendingFilters: null,
  setPendingFilters: (v) => set({ pendingFilters: v }),
  dataRefreshToken: 0,
  triggerDataRefresh: () => set((s) => ({ dataRefreshToken: s.dataRefreshToken + 1 })),
  assistantAutoPrompt: null,
  setAssistantAutoPrompt: (prompt) => set({ assistantAutoPrompt: prompt }),
}));
