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
import React, { useEffect, useRef, useState, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { apiClient } from '../api/apiClient';
import { useUiStore } from '../store/uiStore';
import { useAuthStore } from '../store/authStore';
import { useReportBuilderStore } from '../store/reportBuilderStore';

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
  attachmentFileName?: string;
  attachmentMimeType?: string;
  modelUsed?: string;
  processingTimeMs?: number;
}

export interface PendingAction {
  toolCallId: string;
  toolName: string;
  parameters: Record<string, unknown>;
  description: string;
}

interface QuotaStatus {
  tokensUsed: number;
  tokenLimit: number;
  percentUsed: number | null;
  periodEnd: string;
  tierName: string;
  quotaExceeded: boolean;
}

// Stable session ID scoped to the current tenant + user + browser session.
// Keyed by tenantId + userId so switching tenants or users gets a fresh session.
function getOrCreateSessionId(): string {
  let tenantId: number | null = null;
  let userId: number | null = null;
  try {
    const stored = localStorage.getItem('currentTenant');
    tenantId = stored ? JSON.parse(stored).id : null;
    const userIdStr = localStorage.getItem('userId');
    userId = userIdStr ? parseInt(userIdStr, 10) : null;
  } catch { /* ignore */ }

  const key = tenantId && userId ? `assistant-session-id-${tenantId}-${userId}` : 'assistant-session-id';
  const existing = sessionStorage.getItem(key);
  if (existing) return existing;
  const id = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
  sessionStorage.setItem(key, id);
  return id;
}

const SESSION_ID = getOrCreateSessionId();

function getPageLabel(pathname: string): string {
  if (pathname.startsWith('/customers') && pathname.includes('/edit')) return 'Customer detail / edit';
  if (pathname.startsWith('/customers/new')) return 'New customer form';
  if (pathname === '/customers') return 'Customers list';
  if (pathname.startsWith('/contacts') && pathname.includes('/edit')) return 'Contact detail / edit';
  if (pathname.startsWith('/contacts/new')) return 'New contact form';
  if (pathname === '/contacts') return 'Contacts list';
  if (pathname.startsWith('/opportunities') && pathname.includes('/edit')) return 'Opportunity detail / edit';
  if (pathname.startsWith('/opportunities/new')) return 'New opportunity form';
  if (pathname === '/opportunities') return 'Opportunities list';
  if (pathname.startsWith('/activities') && pathname.includes('/edit')) return 'Activity detail / edit';
  if (pathname.startsWith('/activities/new')) return 'New activity form';
  if (pathname === '/activities') return 'Activities list';
  if (pathname === '/dashboard') return 'Dashboard';
  if (pathname === '/reports') return 'Reports / Analytics';
  if (pathname === '/report-builder') return 'Report Builder';
  if (pathname.startsWith('/admin')) return 'Admin panel';
  return 'BOCRM';
}

// Parse any `reportBuilderUpdate` JSON block from an assistant response and apply
// it to the Report Builder store. Silently ignores malformed blocks.
function applyReportBuilderUpdate(text: string) {
  if (!text) return;
  // Match both fenced ```json ... ``` blocks and raw {"reportBuilderUpdate": ...} objects
  const fenced = text.match(/```(?:json)?\s*(\{[\s\S]*?\})\s*```/);
  const raw = text.match(/\{\s*"reportBuilderUpdate"\s*:\s*\{[\s\S]*?\}\s*\}/);
  const jsonStr = fenced?.[1] ?? raw?.[0];
  if (!jsonStr) return;
  try {
    const parsed = JSON.parse(jsonStr);
    const update = parsed.reportBuilderUpdate;
    if (!update || typeof update !== 'object') return;
    const store = useReportBuilderStore.getState();
    // Fields with dedicated setters / UI controls
    const handled = new Set(['layout', 'accentColor', 'logoPlacement', 'includeFields', 'excludeFields', 'title', 'reportType', 'entityType']);
    if (typeof update.layout === 'string') store.setLayout(update.layout);
    if (typeof update.accentColor === 'string') store.setAccentColor(update.accentColor);
    if (typeof update.logoPlacement === 'string') store.setLogoPlacement(update.logoPlacement);
    if (Array.isArray(update.includeFields)) store.setIncludeFields(update.includeFields.filter((f: unknown) => typeof f === 'string'));
    if (Array.isArray(update.excludeFields)) store.setExcludeFields(update.excludeFields.filter((f: unknown) => typeof f === 'string'));
    if (typeof update.title === 'string') store.setTitle(update.title);
    if (typeof update.reportType === 'string' && (update.reportType === 'slide_deck' || update.reportType === 'one_pager')) {
      store.setReportType(update.reportType);
    }
    if (typeof update.entityType === 'string') store.setEntityType(update.entityType);
    // Everything else (backgroundColor, slideBackground, textColor, h1Color, h2Color, fontFamily, customCss, …)
    // goes into styleOverrides and is merged into styleJson on the next preview.
    const extras: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(update)) {
      if (!handled.has(k)) extras[k] = v;
    }
    if (Object.keys(extras).length > 0) store.mergeStyleOverrides(extras);
  } catch { /* ignore parse errors */ }
}

export const AssistantBar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const pageLabel = getPageLabel(location.pathname);

  const [inputValue, setInputValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [quotaStatus, setQuotaStatus] = useState<QuotaStatus | null>(null);
  const [hasUnread, setHasUnread] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(() => {
    const stored = sessionStorage.getItem('assistant-collapsed');
    return stored === 'true'; // default: expanded (false)
  });
  const [expandedHeight, setExpandedHeight] = useState(() => {
    const stored = sessionStorage.getItem('assistant-height');
    const parsed = stored ? parseInt(stored, 10) : NaN;
    const maxH = Math.floor(window.innerHeight * 0.4);
    return isNaN(parsed) ? Math.min(200, maxH) : Math.max(120, Math.min(parsed, maxH));
  });
  const [attachedFile, setAttachedFile] = useState<{ name: string; mimeType: string; base64: string } | null>(null);
  const [fileInputLoading, setFileInputLoading] = useState(false);
  const isDraggingRef = useRef(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const promptHistoryIndexRef = useRef<number>(-1); // -1 = no history browsing active

  const {
    assistantConfirmMode,
    setAssistantConfirmMode,
    assistantMessages,
    setAssistantMessages,
    assistantLoading,
    setAssistantLoading,
    assistantPendingActions,
    setAssistantPendingActions,
    assistantPendingSessionId,
    setAssistantPendingSessionId,
    assistantPromptHistory,
    addAssistantPromptToHistory,
    triggerDataRefresh,
  } = useUiStore();
  const { refreshTenantSettings } = useAuthStore();
  const { currentRole } = useAuthStore();
  const isAdmin = currentRole != null && (currentRole === 'admin' || currentRole.toLowerCase().includes('admin'));

  // Aliases for convenience
  const messages = assistantMessages;
  const setMessages = setAssistantMessages;
  const isLoading = assistantLoading;
  const pendingActions = assistantPendingActions;
  const pendingSessionId = assistantPendingSessionId;

  // Persist collapsed state
  useEffect(() => {
    sessionStorage.setItem('assistant-collapsed', String(isCollapsed));
  }, [isCollapsed]);

  // Load history once on mount — only fetch from API if we don't have cached messages
  useEffect(() => {
    if (messages.length === 0) {
      loadHistory();
    }
  }, []);

  // Scroll to bottom whenever messages change or bar expands — always instant (no animation)
  useEffect(() => {
    if (!isCollapsed) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
    }
  }, [messages, isCollapsed]);

  // Mark unread when messages arrive while collapsed (or after the user collapses)
  const prevMessageCountRef = useRef(messages.length);
  useEffect(() => {
    const prevCount = prevMessageCountRef.current;
    prevMessageCountRef.current = messages.length;
    if (isCollapsed && messages.length > prevCount) {
      const last = messages[messages.length - 1];
      if (last?.role === 'assistant') setHasUnread(true);
    }
  }, [messages, isCollapsed]);

  // Also set hasUnread whenever the bar becomes collapsed if there are unseen messages
  useEffect(() => {
    if (isCollapsed && messages.length > lastSeenCountRef.current) {
      setHasUnread(true);
    }
  }, [isCollapsed]);

  // Reset textarea height when input is cleared (e.g. after send)
  useEffect(() => {
    if (inputValue === '' && inputRef.current) {
      inputRef.current.style.height = 'auto';
    }
  }, [inputValue]);

  // Track how many messages the user has "seen" (i.e. viewed while expanded)
  const lastSeenCountRef = useRef(0);

  const handleExpand = () => {
    setIsCollapsed(false);
    setHasUnread(false);
    lastSeenCountRef.current = messages.length;
    setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  };

  const loadHistory = async () => {
    try {
      const response = await apiClient.getChatHistory();
      const msgs: ChatMessage[] = response.data || [];
      setMessages(msgs);
      // Mark all loaded history as "seen" so the badge doesn't falsely trigger
      lastSeenCountRef.current = msgs.length;
      prevMessageCountRef.current = msgs.length;
    } catch {
      // silently fail — history load is best-effort
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    if (file.size > MAX_SIZE) {
      setError('File is too large. Maximum size is 5 MB.');
      return;
    }

    // Reject Excel files with helpful message
    if (file.name.endsWith('.xlsx') || file.name.endsWith('.xls')) {
      setError('Excel files are not yet supported. Please export as CSV and try again.');
      return;
    }

    setFileInputLoading(true);
    setError(null);

    try {
      const reader = new FileReader();
      reader.onload = (event) => {
        const result = event.target?.result as string;
        if (result) {
          const base64 = result.split(',')[1] || result;
          setAttachedFile({
            name: file.name,
            mimeType: file.type || 'application/octet-stream',
            base64,
          });
        }
      };
      reader.onerror = () => {
        setError('Failed to read file. Please try again.');
      };
      reader.readAsDataURL(file);
    } finally {
      setFileInputLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleRemoveAttachment = () => {
    setAttachedFile(null);
    setError(null);
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() && !attachedFile) return;
    if (isLoading) return;

    const userMessage = inputValue;
    if (userMessage.trim()) addAssistantPromptToHistory(userMessage);
    promptHistoryIndexRef.current = -1; // reset history browsing
    setInputValue('');
    setError(null);
    setAssistantPendingActions(null);
    const file = attachedFile;
    setAttachedFile(null);

    // Optimistically add the user message to the conversation
    const optimisticMsg: ChatMessage = {
      id: -Date.now(),
      role: 'user',
      content: userMessage || (file ? `📎 ${file.name}` : ''),
      createdAt: new Date().toISOString(),
    };
    setMessages([...messages, optimisticMsg]);

    try {
      setAssistantLoading(true);
      const response = await apiClient.sendChatMessage(
        userMessage,
        undefined,
        undefined,
        SESSION_ID,
        assistantConfirmMode,
        undefined,
        pageLabel,
        file?.base64,
        file?.mimeType,
        file?.name
      );
      const data = response.data;
      // Replace optimistic message with the real persisted history
      const history: ChatMessage[] = data.history || [];
      setMessages(history);
      setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 0);

      // Report Builder context: parse structured style updates from the response and apply to the store
      if (location.pathname === '/report-builder' && history.length > 0) {
        const last = history[history.length - 1];
        if (last?.role === 'assistant') {
          applyReportBuilderUpdate(last.content);
        }
      }

      if (data.pendingActions?.length > 0) {
        setAssistantPendingActions(data.pendingActions);
        setAssistantPendingSessionId(data.sessionId || SESSION_ID);
      } else {
        // Auto mode completed tool calls — refresh page data and tenant settings
        triggerDataRefresh();
        refreshTenantSettings();
      }
      if (data.navigateTo) {
        if (data.navigateTo.startsWith('FILTER:')) {
          // Parse FILTER:<entityType>:<filtersJson>
          const parts = data.navigateTo.split(':', 3);
          if (parts.length === 3) {
            const [, entityType, filtersJson] = parts;
            try {
              const filters = JSON.parse(filtersJson);
              useUiStore.getState().setPendingFilters({ entityType, filters });
              // Navigate to the entity list page
              const entityPath: Record<string, string> = {
                Opportunity: '/opportunities', Customer: '/customers',
                Contact: '/contacts', Activity: '/activities', CustomRecord: '/custom-records',
              };
              if (entityPath[entityType]) navigate(entityPath[entityType]);
            } catch (e) {
              console.error('Failed to parse filter signal:', e);
            }
          }
        } else {
          navigate(data.navigateTo);
        }
      }
      if (data.quotaStatus) setQuotaStatus(data.quotaStatus);
    } catch (err: any) {
      const status = err.response?.status;
      const serverMsg = err.response?.data?.message;
      if (status === 429) {
        setError(serverMsg || 'Token quota exceeded. Please upgrade your plan.');
      } else if (status === 503) {
        setError(serverMsg || 'AI provider temporarily unavailable.');
      } else {
        setError(serverMsg || 'Failed to send message');
      }
    } finally {
      setAssistantLoading(false);
    }
  };

  const handleExecuteActions = async () => {
    if (!pendingSessionId) return;
    setError(null);
    try {
      setAssistantLoading(true);
      const response = await apiClient.executePendingActions(pendingSessionId);
      const data = response.data;
      const execHistory: ChatMessage[] = data.history || [];
      setMessages(execHistory);
      setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 0);
      setAssistantPendingActions(null);
      if (data.quotaStatus) setQuotaStatus(data.quotaStatus);
      // Confirm mode: actions just executed — refresh page data and tenant settings
      triggerDataRefresh();
      refreshTenantSettings();
      if (data.navigateTo) {
        if (data.navigateTo.startsWith('FILTER:')) {
          // Parse FILTER:<entityType>:<filtersJson>
          const parts = data.navigateTo.split(':', 3);
          if (parts.length === 3) {
            const [, entityType, filtersJson] = parts;
            try {
              const filters = JSON.parse(filtersJson);
              useUiStore.getState().setPendingFilters({ entityType, filters });
              // Navigate to the entity list page
              const entityPath: Record<string, string> = {
                Opportunity: '/opportunities', Customer: '/customers',
                Contact: '/contacts', Activity: '/activities', CustomRecord: '/custom-records',
              };
              if (entityPath[entityType]) navigate(entityPath[entityType]);
            } catch (e) {
              console.error('Failed to parse filter signal:', e);
            }
          }
        } else {
          navigate(data.navigateTo);
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to execute actions');
    } finally {
      setAssistantLoading(false);
    }
  };

  const handleResizeMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isDraggingRef.current = true;
    const startY = e.clientY;
    const startHeight = expandedHeight;

    const onMouseMove = (ev: MouseEvent) => {
      const delta = startY - ev.clientY; // drag up = positive delta = taller
      const newHeight = Math.max(120, Math.min(startHeight + delta, window.innerHeight * 0.6));
      setExpandedHeight(newHeight);
    };

    const onMouseUp = (ev: MouseEvent) => {
      isDraggingRef.current = false;
      const delta = startY - ev.clientY;
      const newHeight = Math.max(120, Math.min(startHeight + delta, window.innerHeight * 0.6));
      sessionStorage.setItem('assistant-height', String(Math.round(newHeight)));
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  const handleResizeTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length !== 1) return;
    isDraggingRef.current = true;
    const startY = e.touches[0].clientY;
    const startHeight = expandedHeight;

    const onTouchMove = (ev: TouchEvent) => {
      if (ev.touches.length !== 1) return;
      ev.preventDefault();
      const delta = startY - ev.touches[0].clientY;
      const newHeight = Math.max(120, Math.min(startHeight + delta, window.innerHeight * 0.6));
      setExpandedHeight(newHeight);
    };

    const onTouchEnd = (ev: TouchEvent) => {
      isDraggingRef.current = false;
      const touch = ev.changedTouches[0];
      const delta = startY - touch.clientY;
      const newHeight = Math.max(120, Math.min(startHeight + delta, window.innerHeight * 0.6));
      sessionStorage.setItem('assistant-height', String(Math.round(newHeight)));
      document.removeEventListener('touchmove', onTouchMove);
      document.removeEventListener('touchend', onTouchEnd);
    };

    document.addEventListener('touchmove', onTouchMove, { passive: false });
    document.addEventListener('touchend', onTouchEnd);
  };

  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(e as unknown as React.FormEvent);
      return;
    }
    if (e.key === 'ArrowUp' && !inputValue.includes('\n')) {
      e.preventDefault();
      // If input is empty or we're already browsing history, go to previous
      if (inputValue === '' || promptHistoryIndexRef.current >= 0) {
        const nextIndex = promptHistoryIndexRef.current + 1;
        if (nextIndex < assistantPromptHistory.length) {
          promptHistoryIndexRef.current = nextIndex;
          setInputValue(assistantPromptHistory[nextIndex]);
        }
      } else {
        // Start browsing history from most recent
        if (assistantPromptHistory.length > 0) {
          promptHistoryIndexRef.current = 0;
          setInputValue(assistantPromptHistory[0]);
        }
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      // Go to next (more recent) in history
      if (promptHistoryIndexRef.current > 0) {
        promptHistoryIndexRef.current--;
        setInputValue(assistantPromptHistory[promptHistoryIndexRef.current]);
      } else if (promptHistoryIndexRef.current === 0) {
        // Exit history browsing, clear input
        promptHistoryIndexRef.current = -1;
        setInputValue('');
      }
    }
  }, [inputValue, assistantPromptHistory]);

  // ── Speech-to-text ──────────────────────────────────────────────────────────
  const [isRecording, setIsRecording] = useState(false);
  const recognitionRef = useRef<any>(null);
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
  const speechSupported = !!SpeechRecognition;

  const toggleRecording = useCallback(() => {
    if (!SpeechRecognition) return;

    if (isRecording) {
      recognitionRef.current?.stop();
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = navigator.language || 'en-US';

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setInputValue(prev => prev ? `${prev} ${transcript}` : transcript);
    };
    recognition.onend = () => setIsRecording(false);
    recognition.onerror = () => setIsRecording(false);

    recognitionRef.current = recognition;
    recognition.start();
    setIsRecording(true);
  }, [isRecording, SpeechRecognition]);

  return (
    <div
      className="flex flex-col border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shrink-0"
      style={{
        height: isCollapsed ? '3rem' : `${expandedHeight}px`,
        transition: isDraggingRef.current ? 'none' : 'height 0.2s ease',
      }}
    >
      {/* Drag-to-resize handle — only when expanded */}
      {!isCollapsed && (
        <div
          onMouseDown={handleResizeMouseDown}
          onTouchStart={handleResizeTouchStart}
          className="h-3 shrink-0 cursor-row-resize bg-transparent hover:bg-blue-400/30 dark:hover:bg-blue-500/30 active:bg-blue-400/50 dark:active:bg-blue-500/50 transition-colors"
          title="Drag to resize"
        />
      )}
      {/* Header bar — always visible */}
      <div className="h-12 flex items-center justify-between px-4 shrink-0 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-3">
          <span className="text-base">🤖</span>
          <span className="font-semibold text-sm text-gray-800 dark:text-white">BOCRM Assistant</span>
          {!isCollapsed && (
            <span className="text-xs text-gray-400 dark:text-gray-500 hidden sm:inline">
              — {pageLabel}
            </span>
          )}
          {isCollapsed && messages.length > 0 && (
            <span className={`inline-flex items-center justify-center px-1.5 py-0.5 rounded-full text-xs font-bold min-w-[1.25rem] ${
              hasUnread
                ? 'bg-blue-500 text-white animate-pulse'
                : 'bg-gray-300 dark:bg-gray-600 text-gray-700 dark:text-gray-200'
            }`} title={hasUnread ? 'New message' : `${messages.length} messages`}>
              {messages.length}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {!isCollapsed && (
            <button
              title={assistantConfirmMode === 'confirm' ? 'Confirm mode — click to switch to Auto' : 'Auto mode — click to switch to Confirm'}
              onClick={() => setAssistantConfirmMode(assistantConfirmMode === 'confirm' ? 'auto' : 'confirm')}
              className={`flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium transition-colors ${
                assistantConfirmMode === 'confirm'
                  ? 'bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-700'
                  : 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-700'
              }`}
            >
              <span>{assistantConfirmMode === 'confirm' ? '🛡️' : '⚡'}</span>
              <span className="hidden sm:inline">{assistantConfirmMode === 'confirm' ? 'Confirm' : 'Auto'}</span>
            </button>
          )}
          {!isCollapsed && messages.length > 0 && (
            <button
              title="Clear chat history"
              onClick={async () => {
                if (!window.confirm('Clear all chat history? This cannot be undone.')) return;
                try {
                  await apiClient.clearChatHistory();
                  setMessages([]);
                  setAssistantPendingActions(null);
                } catch {
                  // silently fail
                }
              }}
              className="p-1.5 rounded text-gray-400 dark:text-gray-500 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 transition-colors"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          )}
          <button
            onClick={isCollapsed ? handleExpand : () => setIsCollapsed(true)}
            className="p-2.5 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-base font-medium leading-none"
            title={isCollapsed ? 'Expand assistant' : 'Collapse assistant'}
          >
            {isCollapsed ? '▲' : '▼'}
          </button>
        </div>
      </div>

      {/* Body — only visible when expanded */}
      {!isCollapsed && (
        <>
          {/* Messages area */}
          <div className="flex-1 overflow-y-auto px-4 py-2 space-y-3 bg-gray-50 dark:bg-gray-900">
            {messages.length === 0 && !isLoading && !error && (
              <div className="flex items-center justify-center h-full text-center text-gray-400 dark:text-gray-500 text-sm py-4">
                Ask me anything about your CRM data…
              </div>
            )}

            {messages.map((message) => (
              <div key={message.id} className={`flex gap-2 ${message.role === 'user' ? 'flex-row-reverse' : ''}`}>
                <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 text-xs ${
                  message.role === 'user' ? 'bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300' : 'bg-purple-100 dark:bg-purple-900 text-purple-600 dark:text-purple-300'
                }`}>
                  {message.role === 'user' ? '👤' : '🤖'}
                </div>
                <div className={`max-w-[75%] ${message.role === 'user' ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
                  {/* Attachment Display */}
                  {message.attachmentFileName && (
                    <div className={`px-2 py-1 rounded-lg text-xs flex items-center gap-1 ${
                      message.role === 'user'
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
                    }`}>
                      <span>📎</span>
                      <span className="truncate">{message.attachmentFileName}</span>
                    </div>
                  )}
                  {/* Message Content */}
                  {message.content && (
                    <div className={`px-3 py-2 rounded-xl text-sm prose prose-sm max-w-none prose-p:my-1 prose-headings:my-1 prose-ul:my-1 prose-ol:my-1 prose-li:my-0 prose-pre:my-1 prose-code:text-xs ${
                      message.role === 'user'
                        ? 'bg-blue-600 text-white rounded-tr-none prose-invert prose-code:bg-blue-500 prose-pre:bg-blue-500'
                        : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200 rounded-tl-none shadow-sm dark:prose-invert'
                    }`}>
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                          a: ({ href, children }) => {
                            const backupMatch = href?.match(/\/api\/admin\/backup\/jobs\/(\d+)\/download/);
                            if (backupMatch) {
                              const jobId = parseInt(backupMatch[1], 10);
                              return (
                                <button
                                  onClick={async () => {
                                    try {
                                      const resp = await apiClient.downloadBackup(jobId);
                                      const disposition = (resp.headers as Record<string, string>)['content-disposition'] ?? '';
                                      const fnMatch = disposition.match(/filename="?([^"]+)"?/);
                                      const filename = fnMatch ? fnMatch[1] : `backup_${jobId}.json`;
                                      const blob = new Blob([resp.data as string], { type: 'application/json' });
                                      const url = URL.createObjectURL(blob);
                                      const a = document.createElement('a');
                                      a.href = url;
                                      a.download = filename;
                                      a.click();
                                      URL.revokeObjectURL(url);
                                    } catch {
                                      alert('Download failed. Please try again.');
                                    }
                                  }}
                                  className="inline-flex items-center gap-1 px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded cursor-pointer border-none"
                                >
                                  ⬇ {children}
                                </button>
                              );
                            }
                            const docMatch = href?.match(/\/documents\/(\d+)\/download/);
                            if (docMatch) {
                              const docId = parseInt(docMatch[1], 10);
                              return (
                                <button
                                  onClick={async () => {
                                    try {
                                      const resp = await apiClient.downloadDocument(docId);
                                      const { name, mimeType, contentBase64 } = resp.data;
                                      const binary = atob(contentBase64);
                                      const bytes = Uint8Array.from(binary, c => c.charCodeAt(0));
                                      const blob = new Blob([bytes], { type: mimeType || 'application/octet-stream' });
                                      const url = URL.createObjectURL(blob);
                                      const a = document.createElement('a');
                                      a.href = url;
                                      a.download = name;
                                      a.click();
                                      URL.revokeObjectURL(url);
                                    } catch {
                                      alert('Download failed. Please try again.');
                                    }
                                  }}
                                  className="inline-flex items-center gap-1 px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded cursor-pointer border-none"
                                >
                                  ⬇ {children}
                                </button>
                              );
                            }
                            return <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>;
                          }
                        }}
                      >
                        {message.content}
                      </ReactMarkdown>
                    </div>
                  )}
                  <div className="text-xs text-gray-400 px-1 mt-0.5 flex items-center gap-2">
                    <span>{new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    {message.role === 'assistant' && message.modelUsed && (
                      <span className="text-gray-400 dark:text-gray-500">
                        {message.modelUsed}
                        {message.processingTimeMs != null && ` · ${message.processingTimeMs < 1000 ? `${message.processingTimeMs}ms` : `${(message.processingTimeMs / 1000).toFixed(1)}s`}`}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {error && (
              <div className="px-3 py-2 bg-red-50 dark:bg-red-900/40 border border-red-200 dark:border-red-800 rounded-lg text-sm text-red-600 dark:text-red-300 flex items-center gap-2">
                <span>⚠️</span> {error}
              </div>
            )}

            {isLoading && (
              <div className="flex gap-2">
                <div className="w-6 h-6 rounded-full bg-purple-100 dark:bg-purple-900 text-purple-600 dark:text-purple-300 flex items-center justify-center shrink-0 text-xs">🤖</div>
                <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 px-3 py-2 rounded-xl rounded-tl-none shadow-sm flex items-center gap-1">
                  <div className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                  <div className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                  <div className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Pending actions */}
          {pendingActions && pendingActions.length > 0 && !isLoading && (
            <div className="px-4 py-2 bg-amber-50 dark:bg-amber-900/30 border-t border-amber-200 dark:border-amber-700 shrink-0">
              <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 mb-1">
                🛡️ Review before executing:
              </div>
              <div className="flex flex-wrap gap-2 mb-2">
                {pendingActions.map((action) => (
                  <div key={action.toolCallId} className="flex items-center gap-1 px-2 py-1 bg-white dark:bg-gray-800 rounded border border-amber-200 dark:border-amber-700 text-xs text-gray-700 dark:text-gray-200">
                    <span className="text-amber-500">▶</span> {action.description}
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <button onClick={handleExecuteActions} className="px-3 py-1 bg-green-600 hover:bg-green-700 text-white text-xs font-medium rounded transition-colors">
                  ✓ Execute All
                </button>
                <button onClick={() => { setAssistantPendingActions(null); setAssistantPendingSessionId(null); }} className="px-3 py-1 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs font-medium rounded transition-colors">
                  ✕ Cancel
                </button>
              </div>
            </div>
          )}

          {/* Quota bar (admin or >80%) */}
          {quotaStatus && (isAdmin || (quotaStatus.percentUsed !== null && quotaStatus.percentUsed > 80)) && (
            <div className="px-4 py-1.5 bg-gray-50 dark:bg-gray-700 border-t border-gray-200 dark:border-gray-600 shrink-0">
              <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 mb-0.5">
                <span>{quotaStatus.tierName}</span>
                <span>
                  {quotaStatus.tokenLimit === -1
                    ? 'Unlimited tokens'
                    : `${quotaStatus.tokensUsed.toLocaleString()} / ${quotaStatus.tokenLimit.toLocaleString()} tokens`}
                </span>
              </div>
              {quotaStatus.tokenLimit !== -1 && quotaStatus.percentUsed !== null && (
                <div className="h-1 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${quotaStatus.percentUsed > 90 ? 'bg-red-500' : quotaStatus.percentUsed > 70 ? 'bg-amber-500' : 'bg-green-500'}`}
                    style={{ width: `${Math.min(100, quotaStatus.percentUsed)}%` }}
                  />
                </div>
              )}
            </div>
          )}

          {/* Input */}
          <div className="px-4 py-2 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 shrink-0">
            {/* Attachment Display */}
            {attachedFile && (
              <div className="mb-2 flex items-center justify-between p-2 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg">
                <div className="flex items-center gap-2 text-sm flex-1">
                  <span className="text-blue-600 dark:text-blue-300">📎</span>
                  <span className="text-gray-700 dark:text-gray-300 truncate">{attachedFile.name}</span>
                </div>
                <button
                  type="button"
                  onClick={handleRemoveAttachment}
                  className="p-1 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
                >
                  ✕
                </button>
              </div>
            )}
            <form className="flex gap-1.5 items-end" onSubmit={handleSendMessage}>
              <textarea
                ref={inputRef}
                rows={1}
                className="flex-1 min-w-0 px-3 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all resize-none overflow-y-auto"
                style={{ maxHeight: '160px' }}
                placeholder={attachedFile ? 'Add a message (optional)...' : `Ask me about ${pageLabel}…`}
                value={inputValue}
                onChange={(e) => {
                  setInputValue(e.target.value);
                  e.target.style.height = 'auto';
                  e.target.style.height = Math.min(e.target.scrollHeight, 160) + 'px';
                }}
                onKeyDown={handleKeyDown}
                disabled={isLoading || fileInputLoading}
                autoComplete="off"
              />
              {/* File picker button */}
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="p-2 text-gray-600 dark:text-gray-300 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                disabled={isLoading || fileInputLoading || !!attachedFile}
                title="Attach file (PDF, image, CSV)"
              >
                📎
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.csv,.json"
                onChange={handleFileSelect}
                className="hidden"
              />
              {speechSupported && (
                <button
                  type="button"
                  onClick={toggleRecording}
                  title={isRecording ? 'Stop recording' : 'Dictate message'}
                  className={`p-2 rounded-lg transition-colors shrink-0 ${
                    isRecording
                      ? 'bg-red-500 hover:bg-red-600 text-white animate-pulse'
                      : 'bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-600 dark:text-gray-300'
                  }`}
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 1a4 4 0 0 1 4 4v6a4 4 0 0 1-8 0V5a4 4 0 0 1 4-4zm-1 17.93V21H9v2h6v-2h-2v-2.07A8 8 0 0 0 20 11h-2a6 6 0 0 1-12 0H4a8 8 0 0 0 7 7.93z"/>
                  </svg>
                </button>
              )}
              <button
                type="submit"
                className="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shrink-0"
                disabled={isLoading || fileInputLoading || (!inputValue.trim() && !attachedFile)}
              >
                <svg className="w-4 h-4 transform rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </button>
            </form>
          </div>
        </>
      )}
    </div>
  );
};
