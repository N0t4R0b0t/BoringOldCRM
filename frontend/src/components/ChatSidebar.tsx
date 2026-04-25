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
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { apiClient } from '../api/apiClient';
import { useUiStore } from '../store/uiStore';
import { useAuthStore } from '../store/authStore';

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  contextEntityType?: string;
  contextEntityId?: number;
  createdAt: string;
  attachmentFileName?: string;
  attachmentMimeType?: string;
}

export interface PendingAction {
  toolCallId: string;
  toolName: string;
  parameters: Record<string, unknown>;
  description: string;
}

export interface QuotaStatus {
  tokensUsed: number;
  tokenLimit: number;
  percentUsed: number | null;
  periodEnd: string;
  tierName: string;
  quotaExceeded: boolean;
}

export interface AttachedFile {
  name: string;
  mimeType: string;
  base64: string;
}

export interface ChatSidebarProps {
  contextEntityType?: string;
  contextEntityId?: number;
  isOpen: boolean;
  onClose: () => void;
}

export const ChatSidebar: React.FC<ChatSidebarProps> = ({
  contextEntityType,
  contextEntityId,
  isOpen,
  onClose,
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingActions, setPendingActions] = useState<PendingAction[] | null>(null);
  const [pendingSessionId, setPendingSessionId] = useState<string | null>(null);
  const [quotaStatus, setQuotaStatus] = useState<QuotaStatus | null>(null);
  const [sessionId] = useState(() => crypto.randomUUID());
  const [isDragging, setIsDragging] = useState(false);
  const [sidebarWidth, setSidebarWidth] = useState(540);
  const [attachedFile, setAttachedFile] = useState<AttachedFile | null>(null);
  const [fileInputLoading, setFileInputLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const sidebarRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { assistantConfirmMode, setAssistantConfirmMode } = useUiStore();
  const { currentRole } = useAuthStore();
  const isAdmin = currentRole != null && (currentRole === 'admin' || currentRole.toLowerCase().includes('admin'));

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => { scrollToBottom(); }, [messages]);

  useEffect(() => {
    if (isOpen) loadHistory();
  }, [isOpen, contextEntityType, contextEntityId]);

  const loadHistory = async () => {
    try {
      setError(null);
      const response = await apiClient.getChatHistory(contextEntityType, contextEntityId);
      setMessages(response.data);
    } catch (err: any) {
      setError('Failed to load chat history');
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
          // Extract base64 from data URL (remove the "data:...;base64," prefix)
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
      // Reset input so same file can be selected again
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
    setInputValue('');
    setError(null);
    setPendingActions(null);
    const file = attachedFile;
    setAttachedFile(null);

    try {
      setIsLoading(true);
      const response = await apiClient.sendChatMessage(
        userMessage,
        contextEntityType,
        contextEntityId,
        sessionId,
        assistantConfirmMode,
        false,
        undefined,
        file?.base64,
        file?.mimeType,
        file?.name
      );

      const data = response.data;
      setMessages(data.history || []);
      if (data.pendingActions && data.pendingActions.length > 0) {
        setPendingActions(data.pendingActions);
        setPendingSessionId(data.sessionId || sessionId);
      }
      if (data.quotaStatus) setQuotaStatus(data.quotaStatus);
    } catch (err: any) {
      const status = err.response?.status;
      const serverMsg = err.response?.data?.message;
      if (status === 429) {
        setError(serverMsg || 'Token quota exceeded. Please upgrade your plan.');
      } else if (status === 503) {
        setError(serverMsg || 'The AI provider is temporarily unavailable. Please try again in a moment.');
      } else {
        setError(serverMsg || 'Failed to send message');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleExecuteActions = async () => {
    if (!pendingSessionId) return;
    setError(null);
    try {
      setIsLoading(true);
      const response = await apiClient.executePendingActions(pendingSessionId);
      const data = response.data;
      setMessages(data.history || []);
      setPendingActions(null);
      if (data.quotaStatus) setQuotaStatus(data.quotaStatus);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to execute actions');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancelActions = () => {
    setPendingActions(null);
    setPendingSessionId(null);
  };

  const handleMouseDown = () => setIsDragging(true);
  const handleMouseUp = useCallback(() => setIsDragging(false), []);
  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isDragging) return;
    const newWidth = window.innerWidth - e.clientX;
    if (newWidth > 280 && newWidth < 600) setSidebarWidth(newWidth);
  }, [isDragging]);

  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isDragging, handleMouseMove, handleMouseUp]);


  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/20 backdrop-blur-sm transition-opacity"
      onClick={onClose}
    >
      <div
        className="absolute top-0 right-0 h-full bg-white dark:bg-gray-800 shadow-2xl flex flex-col transition-transform duration-300 ease-in-out transform translate-x-0"
        ref={sidebarRef}
        style={{ width: `${sidebarWidth}px` }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="h-16 flex items-center justify-between px-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 shrink-0">
          <div className="flex items-center gap-2">
            <h2 className="font-semibold text-gray-800 dark:text-white text-sm">BOCRM Assistant</h2>
            {quotaStatus && (
              <span className="text-xs text-gray-400 dark:text-gray-500">{quotaStatus.tierName}</span>
            )}
          </div>
          <div className="flex items-center gap-2">
            {/* Confirm / Auto toggle */}
            <button
              title={assistantConfirmMode === 'confirm' ? 'Confirm mode (click to switch to Auto)' : 'Auto mode (click to switch to Confirm)'}
              onClick={() => setAssistantConfirmMode(assistantConfirmMode === 'confirm' ? 'auto' : 'confirm')}
              className={`flex items-center gap-1 px-2 py-1 rounded text-xs font-medium transition-colors ${
                assistantConfirmMode === 'confirm'
                  ? 'bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-700'
                  : 'bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-700'
              }`}
            >
              <span>{assistantConfirmMode === 'confirm' ? '🛡️' : '⚡'}</span>
              <span className="hidden sm:inline">{assistantConfirmMode === 'confirm' ? 'Confirm' : 'Auto'}</span>
            </button>
            <button
              className="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-full transition-colors"
              onClick={onClose}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Resize Handle */}
        <div
          className="absolute top-0 left-0 w-1 h-full cursor-ew-resize hover:bg-blue-400 transition-colors z-10"
          onMouseDown={handleMouseDown}
        />

        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50 dark:bg-gray-900">
          {messages.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center text-gray-500 dark:text-gray-400 space-y-4">
              <div className="text-4xl">👋</div>
              <div>
                <p className="font-medium text-gray-700 dark:text-gray-300">Welcome to BOCRM Assistant!</p>
                <p className="text-sm mt-1">Ask me to create records, search data, or manage your CRM.</p>
              </div>
            </div>
          ) : (
            <>
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex gap-3 ${message.role === 'user' ? 'flex-row-reverse' : ''}`}
                >
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 text-sm ${
                    message.role === 'user' ? 'bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300' : 'bg-purple-100 dark:bg-purple-900 text-purple-600 dark:text-purple-300'
                  }`}>
                    {message.role === 'user' ? '👤' : '🤖'}
                  </div>
                  <div className={`max-w-[85%] space-y-1 flex flex-col ${message.role === 'user' ? 'items-end' : 'items-start'}`}>
                    {/* Attachment Display */}
                    {message.attachmentFileName && (
                      <div className={`p-2 rounded-lg text-xs flex items-center gap-2 ${
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
                      <div className={`p-3 rounded-2xl text-sm prose prose-sm max-w-none prose-p:my-1 prose-headings:my-2 prose-ul:my-1 prose-ol:my-1 prose-li:my-0 ${
                        message.role === 'user'
                          ? 'bg-blue-600 text-white rounded-tr-none prose-invert prose-code:bg-blue-500 prose-pre:bg-blue-500'
                          : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200 rounded-tl-none shadow-sm dark:prose-invert prose-code:bg-gray-100 prose-code:dark:bg-gray-700 prose-code:px-1 prose-code:rounded prose-pre:bg-gray-100 prose-pre:dark:bg-gray-700'
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
                    <div className="text-xs text-gray-400 px-1">
                      {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </>
          )}

          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-800 rounded-lg text-sm text-red-600 dark:text-red-200 flex items-center gap-2">
              <span>⚠️</span> {error}
            </div>
          )}

          {isLoading && (
            <div className="flex gap-3">
              <div className="w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900 text-purple-600 dark:text-purple-300 flex items-center justify-center shrink-0 text-sm">🤖</div>
              <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-3 rounded-2xl rounded-tl-none shadow-sm flex items-center gap-1">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          )}
        </div>

        {/* Pending Actions */}
        {pendingActions && pendingActions.length > 0 && !isLoading && (
          <div className="px-4 py-3 bg-amber-50 dark:bg-amber-900/30 border-t border-amber-200 dark:border-amber-700 shrink-0">
            <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wider mb-2">
              🛡️ Pending Actions — Review before executing
            </div>
            <div className="space-y-2 mb-3">
              {pendingActions.map((action) => (
                <div
                  key={action.toolCallId}
                  className="flex items-center gap-2 p-2 bg-white dark:bg-gray-800 rounded border border-amber-200 dark:border-amber-700 text-sm"
                >
                  <span className="text-amber-500">▶</span>
                  <span className="text-gray-700 dark:text-gray-200 flex-1">{action.description}</span>
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleExecuteActions}
                className="flex-1 py-1.5 bg-green-600 hover:bg-green-700 text-white text-xs font-medium rounded transition-colors"
              >
                ✓ Execute All
              </button>
              <button
                onClick={handleCancelActions}
                className="flex-1 py-1.5 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs font-medium rounded transition-colors"
              >
                ✕ Cancel
              </button>
            </div>
          </div>
        )}

        {/* Quota footer (admin or >80% usage) */}
        {quotaStatus && (isAdmin || (quotaStatus.percentUsed !== null && quotaStatus.percentUsed > 80)) && (
          <div className="px-4 py-2 bg-gray-50 dark:bg-gray-700 border-t border-gray-200 dark:border-gray-600 shrink-0">
            <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
              <span>{quotaStatus.tierName}</span>
              <span>
                {quotaStatus.tokenLimit === -1
                  ? 'Unlimited'
                  : `${quotaStatus.tokensUsed.toLocaleString()} / ${quotaStatus.tokenLimit.toLocaleString()} tokens`}
              </span>
            </div>
            {quotaStatus.tokenLimit !== -1 && quotaStatus.percentUsed !== null && (
              <div className="mt-1 h-1 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all ${
                    quotaStatus.percentUsed > 90 ? 'bg-red-500' : quotaStatus.percentUsed > 70 ? 'bg-amber-500' : 'bg-green-500'
                  }`}
                  style={{ width: `${Math.min(100, quotaStatus.percentUsed)}%` }}
                />
              </div>
            )}
          </div>
        )}

        {/* Input Area */}
        <div className="p-4 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 shrink-0">
          {/* Attachment Display */}
          {attachedFile && (
            <div className="mb-3 flex items-center justify-between p-2 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg">
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

          <form className="chat-input-form relative flex gap-2" onSubmit={handleSendMessage}>
            <input
              type="text"
              className="flex-1 pl-4 pr-12 py-3 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-gray-600 transition-all text-sm dark:text-white"
              placeholder={attachedFile ? 'Add a message (optional)...' : 'Ask me anything...'}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              disabled={isLoading || fileInputLoading}
            />
            {/* File picker button */}
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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
            {/* Send button */}
            <button
              type="submit"
              className="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              disabled={isLoading || fileInputLoading || (!inputValue.trim() && !attachedFile)}
            >
              <svg className="w-4 h-4 transform rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
