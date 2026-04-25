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
import React, { useState, useRef, useEffect } from 'react';
import { FileText, Users, BarChart2, Shield, Send } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { apiClient } from '../api/apiClient';
import type { OnboardingSuggestionsDTO } from '../types/onboarding';

interface OnboardingWizardModalProps {
  tenantId: number;
  tenantName: string;
  onComplete: () => void;
}

type Step = 1 | 2 | 3 | 4 | 5 | 6;
type SetupMode = 'assistant' | 'manual' | null;

interface ChatMsg {
  role: 'user' | 'assistant';
  content: string;
}

function getOnboardingSessionId(tenantId: number): string {
  const key = `onboarding-session-id-${tenantId}`;
  const existing = sessionStorage.getItem(key);
  if (existing) return existing;
  const id = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
  sessionStorage.setItem(key, id);
  return id;
}

export const OnboardingWizardModal: React.FC<OnboardingWizardModalProps> = ({
  tenantId,
  tenantName,
  onComplete,
}) => {
  const [currentStep, setCurrentStep] = useState<Step>(1);
  const [setupMode, setSetupMode] = useState<SetupMode>(null);

  // Embedded chat state (assistant mode)
  const [chatMessages, setChatMessages] = useState<ChatMsg[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);
  const chatBottomRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLInputElement>(null);
  const onboardingSessionId = useRef<string | null>(null);
  const [orgBio, setOrgBio] = useState('');
  const [suggestionsLoading, setSuggestionsLoading] = useState(false);
  const [suggestions, setSuggestions] = useState<OnboardingSuggestionsDTO | null>(null);
  const [logoUrl, setLogoUrl] = useState('');
  const [primaryColor, setPrimaryColor] = useState('#3b82f6');
  const [selectedFields, setSelectedFields] = useState<Set<number>>(new Set());
  const [selectedPolicies, setSelectedPolicies] = useState<Set<number>>(new Set());
  const [selectedCustomers, setSelectedCustomers] = useState<Set<number>>(new Set());
  const [applyingIndex, setApplyingIndex] = useState<number | null>(null);

  // Scroll chat to bottom when messages change
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, chatLoading]);

  // When assistant mode is chosen, send the initial onboarding prompt
  useEffect(() => {
    if (setupMode !== 'assistant') return;
    if (chatMessages.length > 0) return; // already started
    onboardingSessionId.current = getOnboardingSessionId(tenantId);
    const initialPrompt = `Hi! I'm setting up a new workspace called "${tenantName}". Please guide me through the setup — help me think about custom fields, business policies, and any configuration that would be useful. Ask me questions to understand what we do and make tailored suggestions.`;
    sendChatMessage(initialPrompt, true);
  }, [setupMode]);

  const sendChatMessage = async (text: string, isSystemInit = false) => {
    if (!text.trim() || chatLoading) return;
    setChatError(null);
    if (!isSystemInit) {
      setChatInput('');
    }
    const userMsg: ChatMsg = { role: 'user', content: text };
    setChatMessages((prev) => [...prev, userMsg]);
    setChatLoading(true);
    try {
      const sessionId = onboardingSessionId.current || getOnboardingSessionId(tenantId);
      onboardingSessionId.current = sessionId;
      const response = await apiClient.sendChatMessage(
        text,
        undefined,
        undefined,
        sessionId,
        'auto',
        undefined,
        'Onboarding Setup'
      );
      const data = response.data;
      const history: Array<{ role: string; content: string }> = data.history || [];
      // Rebuild chat from full history so we stay in sync
      setChatMessages(
        history.map((m) => ({ role: m.role as 'user' | 'assistant', content: m.content }))
      );
    } catch (err: any) {
      setChatError(err.response?.data?.message || 'Failed to send message');
      // Remove optimistic user message on error
      setChatMessages((prev) => prev.slice(0, -1));
    } finally {
      setChatLoading(false);
      setTimeout(() => chatInputRef.current?.focus(), 50);
    }
  };

  const handleChatSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sendChatMessage(chatInput);
  };

  const handleNextStep = async () => {
    if (currentStep === 1 && setupMode === null) {
      // Mode selection - just stay on step 1 until mode is chosen
      return;
    } else if (currentStep === 1 && setupMode === 'manual') {
      // Save bio and fetch suggestions
      if (orgBio.length < 20) {
        alert('Please enter at least 20 characters for the organization bio');
        return;
      }
      setSuggestionsLoading(true);
      try {
        const response = await apiClient.getOnboardingSuggestions({
          orgName: tenantName,
          orgBio,
        });
        setSuggestions(response.data);
      } catch (err) {
        console.error('Failed to get onboarding suggestions', err);
      } finally {
        setSuggestionsLoading(false);
      }
    } else if (currentStep === 2) {
      // Save branding
      try {
        await apiClient.updateTenantSettings(tenantId, {
          primaryColor,
          logoUrl,
        });
      } catch (err) {
        console.error('Failed to save branding', err);
      }
    } else if (currentStep === 3) {
      // Apply selected custom fields
      if (suggestions) {
        for (const idx of selectedFields) {
          if (idx < suggestions.suggestedCustomFields.length) {
            const field = suggestions.suggestedCustomFields[idx];
            setApplyingIndex(idx);
            try {
              await apiClient.createCustomFieldDefinition({
                entityType: 'Customer',
                key: field.key,
                label: field.label,
                type: field.type,
                config: field.config,
              });
            } catch (err) {
              console.error('Failed to create custom field', err);
            }
          }
        }
        setApplyingIndex(null);
      }
    } else if (currentStep === 4) {
      // Apply selected policies
      if (suggestions) {
        for (const idx of selectedPolicies) {
          if (idx < suggestions.suggestedPolicies.length) {
            const policy = suggestions.suggestedPolicies[idx];
            setApplyingIndex(idx);
            try {
              await apiClient.createPolicyRule({
                entityType: policy.entityType,
                name: policy.name,
                expression: policy.expression,
                severity: policy.severity,
                description: policy.description,
              });
            } catch (err) {
              console.error('Failed to create policy', err);
            }
          }
        }
        setApplyingIndex(null);
      }
    } else if (currentStep === 5) {
      // Apply selected sample customers
      if (suggestions) {
        for (const idx of selectedCustomers) {
          if (idx < suggestions.suggestedSampleCustomers.length) {
            const customer = suggestions.suggestedSampleCustomers[idx];
            setApplyingIndex(idx);
            try {
              await apiClient.createCustomer({
                name: customer.name,
                status: customer.status,
                customFields: {},
              });
            } catch (err) {
              console.error('Failed to create sample customer', err);
            }
          }
        }
        setApplyingIndex(null);
      }
    }

    if (currentStep < 6) {
      setCurrentStep((currentStep + 1) as Step);
    }
  };

  const handleSkipStep = () => {
    if (currentStep < 6) {
      setCurrentStep((currentStep + 1) as Step);
    }
  };

  const handleSkipAll = async () => {
    await markOnboardingComplete();
  };

  const markOnboardingComplete = async () => {
    try {
      // Fetch current settings and merge
      const currentSettings = await apiClient.getTenantSettings(tenantId);
      const updatedSettings = {
        ...currentSettings.data,
        onboardingCompleted: true,
      };
      await apiClient.updateTenantSettings(tenantId, updatedSettings);
      onComplete();
    } catch (err) {
      console.error('Failed to mark onboarding complete', err);
      // Still complete the onboarding even if settings save fails
      onComplete();
    }
  };

  const toggleFieldSelection = (index: number) => {
    const newSet = new Set(selectedFields);
    if (newSet.has(index)) {
      newSet.delete(index);
    } else {
      newSet.add(index);
    }
    setSelectedFields(newSet);
  };

  const togglePolicySelection = (index: number) => {
    const newSet = new Set(selectedPolicies);
    if (newSet.has(index)) {
      newSet.delete(index);
    } else {
      newSet.add(index);
    }
    setSelectedPolicies(newSet);
  };

  const toggleCustomerSelection = (index: number) => {
    const newSet = new Set(selectedCustomers);
    if (newSet.has(index)) {
      newSet.delete(index);
    } else {
      newSet.add(index);
    }
    setSelectedCustomers(newSet);
  };

  const progressDots = Array.from({ length: 6 }, (_, i) => i + 1);

  return (
    <div className="fixed inset-0 bg-black/30 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-4xl w-full max-h-[95vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Welcome to {tenantName}</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Let's set up your workspace (all steps are optional)</p>
          </div>
          <button
            onClick={handleSkipAll}
            className="text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white font-medium"
          >
            Skip All
          </button>
        </div>

        {/* Progress dots — only in manual mode */}
        {setupMode !== 'assistant' && (
          <div className="flex justify-center gap-2 px-6 pt-6">
            {progressDots.map((step) => (
              <div
                key={step}
                className={`h-2 rounded-full transition-colors ${
                  step === currentStep ? 'bg-blue-600 w-6' : step < currentStep ? 'bg-green-500 w-2' : 'bg-gray-300 dark:bg-gray-700 w-2'
                }`}
              />
            ))}
          </div>
        )}

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {/* Step 1: Setup Mode Selection */}
          {currentStep === 1 && !setupMode && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">How would you like to set up your workspace?</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <button
                  onClick={() => setSetupMode('assistant')}
                  className="p-4 border-2 border-gray-300 dark:border-gray-600 rounded-lg hover:border-blue-500 dark:hover:border-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors text-left"
                >
                  <h4 className="font-semibold text-gray-900 dark:text-white mb-2">🤖 Assistant Setup</h4>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Let the AI assistant guide you through setup with intelligent suggestions
                  </p>
                </button>
                <button
                  onClick={() => setSetupMode('manual')}
                  className="p-4 border-2 border-gray-300 dark:border-gray-600 rounded-lg hover:border-blue-500 dark:hover:border-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors text-left"
                >
                  <h4 className="font-semibold text-gray-900 dark:text-white mb-2">⚙️ Manual Setup</h4>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Configure your workspace step-by-step with full control
                  </p>
                </button>
              </div>
            </div>
          )}

          {/* Assistant Mode: Embedded Chat */}
          {setupMode === 'assistant' && (
            <div className="flex flex-col h-full" style={{ minHeight: 500 }}>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
                Your AI assistant will guide you through setting up <strong className="text-gray-800 dark:text-gray-200">{tenantName}</strong>. Ask anything or let it suggest what to configure.
              </p>
              {/* Message list */}
              <div className="flex-1 overflow-y-auto space-y-3 pr-1" style={{ maxHeight: 440 }}>
                {chatMessages.map((msg, idx) => (
                  <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[85%] px-3 py-2 rounded-xl text-sm ${
                        msg.role === 'user'
                          ? 'bg-blue-600 text-white rounded-br-sm'
                          : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-bl-sm'
                      }`}
                    >
                      {msg.role === 'assistant' ? (
                        <div className="prose prose-sm dark:prose-invert max-w-none">
                          <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content}</ReactMarkdown>
                        </div>
                      ) : (
                        msg.content
                      )}
                    </div>
                  </div>
                ))}
                {chatLoading && (
                  <div className="flex justify-start">
                    <div className="bg-gray-100 dark:bg-gray-700 px-3 py-2 rounded-xl rounded-bl-sm">
                      <span className="inline-flex gap-1">
                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                        <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                      </span>
                    </div>
                  </div>
                )}
                {chatError && (
                  <p className="text-xs text-red-500 dark:text-red-400 text-center">{chatError}</p>
                )}
                <div ref={chatBottomRef} />
              </div>
              {/* Input */}
              <form onSubmit={handleChatSubmit} className="flex gap-2 mt-3">
                <input
                  ref={chatInputRef}
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Ask or reply..."
                  disabled={chatLoading}
                  className="flex-1 px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                />
                <button
                  type="submit"
                  disabled={chatLoading || !chatInput.trim()}
                  className="px-3 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
                >
                  <Send size={15} />
                </button>
              </form>
            </div>
          )}

          {/* Step 1: Welcome + Bio (Manual Mode) */}
          {currentStep === 1 && setupMode === 'manual' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Organization Name</label>
                <input
                  type="text"
                  value={tenantName}
                  disabled
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-400"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Organization Bio (20+ characters)</label>
                <textarea
                  value={orgBio}
                  onChange={(e) => setOrgBio(e.target.value)}
                  placeholder="Tell us about your organization, industry, and goals..."
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  rows={4}
                />
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{orgBio.length} / 20 characters</p>
              </div>
            </div>
          )}

          {/* Step 2: Branding */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Logo URL</label>
                <input
                  type="url"
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  placeholder="https://example.com/logo.png"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                />
              </div>
              <div className="flex items-end gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">Primary Color</label>
                  <div className="flex gap-2">
                    <input
                      type="color"
                      value={primaryColor}
                      onChange={(e) => setPrimaryColor(e.target.value)}
                      className="w-12 h-10 border border-gray-300 dark:border-gray-600 rounded-lg cursor-pointer"
                    />
                    <input
                      type="text"
                      value={primaryColor}
                      onChange={(e) => setPrimaryColor(e.target.value)}
                      className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                      placeholder="#3b82f6"
                    />
                  </div>
                </div>
                <div className="w-20 h-20 rounded-lg shadow-md" style={{ backgroundColor: primaryColor }} />
              </div>
            </div>
          )}

          {/* Step 3: Custom Fields */}
          {currentStep === 3 && (
            <div className="space-y-4">
              {suggestionsLoading ? (
                <div className="text-center py-8">
                  <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent mb-2" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">AI is preparing your suggestions...</p>
                </div>
              ) : suggestions && suggestions.suggestedCustomFields.length > 0 ? (
                <div className="space-y-2">
                  <h3 className="font-medium text-gray-900 dark:text-white mb-3">Suggested Custom Fields</h3>
                  {suggestions.suggestedCustomFields.map((field, idx) => (
                    <label key={idx} className="flex items-start gap-3 p-3 border border-gray-200 dark:border-gray-700 rounded-lg cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <input
                        type="checkbox"
                        checked={selectedFields.has(idx)}
                        onChange={() => toggleFieldSelection(idx)}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <div className="font-medium text-gray-900 dark:text-white">{field.label}</div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">{field.type} • {field.description}</div>
                      </div>
                      {applyingIndex === idx && <div className="text-xs text-blue-600 dark:text-blue-400 animate-spin">⚙️</div>}
                    </label>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400">No custom field suggestions available.</p>
              )}
            </div>
          )}

          {/* Step 4: Policies */}
          {currentStep === 4 && (
            <div className="space-y-4">
              {suggestions && suggestions.suggestedPolicies.length > 0 ? (
                <div className="space-y-2">
                  <h3 className="font-medium text-gray-900 dark:text-white mb-3">Suggested Business Policies</h3>
                  {suggestions.suggestedPolicies.map((policy, idx) => (
                    <label key={idx} className="flex items-start gap-3 p-3 border border-gray-200 dark:border-gray-700 rounded-lg cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <input
                        type="checkbox"
                        checked={selectedPolicies.has(idx)}
                        onChange={() => togglePolicySelection(idx)}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <div className="font-medium text-gray-900 dark:text-white">{policy.name}</div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">{policy.description}</div>
                        <div className="text-xs mt-1 font-mono text-gray-600 dark:text-gray-300 truncate">{policy.expression}</div>
                      </div>
                      {applyingIndex === idx && <div className="text-xs text-blue-600 dark:text-blue-400 animate-spin">⚙️</div>}
                    </label>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400">No policy suggestions available.</p>
              )}
            </div>
          )}

          {/* Step 5: Sample Data */}
          {currentStep === 5 && (
            <div className="space-y-4">
              {suggestions && suggestions.suggestedSampleCustomers.length > 0 ? (
                <div className="space-y-2">
                  <h3 className="font-medium text-gray-900 dark:text-white mb-3">Suggested Sample Customers</h3>
                  {suggestions.suggestedSampleCustomers.map((customer, idx) => (
                    <label key={idx} className="flex items-start gap-3 p-3 border border-gray-200 dark:border-gray-700 rounded-lg cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <input
                        type="checkbox"
                        checked={selectedCustomers.has(idx)}
                        onChange={() => toggleCustomerSelection(idx)}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <div className="font-medium text-gray-900 dark:text-white">{customer.name}</div>
                        <div className="text-xs text-gray-500 dark:text-gray-400">{customer.status} • {customer.description}</div>
                      </div>
                      {applyingIndex === idx && <div className="text-xs text-blue-600 dark:text-blue-400 animate-spin">⚙️</div>}
                    </label>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400">No sample data suggestions available.</p>
              )}
            </div>
          )}

          {/* Step 6: Overview */}
          {currentStep === 6 && (
            <div className="space-y-4">
              <h3 className="font-medium text-gray-900 dark:text-white mb-4">Your AI Assistant Can Help With:</h3>
              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 border border-gray-200 dark:border-gray-700 rounded-lg">
                  <FileText className="w-5 h-5 text-blue-600 dark:text-blue-400 mb-2" />
                  <div className="font-medium text-sm text-gray-900 dark:text-white">Document Generation</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">Create decks, reports, and exports</div>
                </div>
                <div className="p-3 border border-gray-200 dark:border-gray-700 rounded-lg">
                  <Users className="w-5 h-5 text-green-600 dark:text-green-400 mb-2" />
                  <div className="font-medium text-sm text-gray-900 dark:text-white">Team Management</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">Add users and manage access</div>
                </div>
                <div className="p-3 border border-gray-200 dark:border-gray-700 rounded-lg">
                  <BarChart2 className="w-5 h-5 text-purple-600 dark:text-purple-400 mb-2" />
                  <div className="font-medium text-sm text-gray-900 dark:text-white">Analytics</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">Insights about your pipeline</div>
                </div>
                <div className="p-3 border border-gray-200 dark:border-gray-700 rounded-lg">
                  <Shield className="w-5 h-5 text-orange-600 dark:text-orange-400 mb-2" />
                  <div className="font-medium text-sm text-gray-900 dark:text-white">Governance</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">Set up policies and rules</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="border-t border-gray-200 dark:border-gray-700 px-6 py-4 bg-gray-50 dark:bg-gray-700/50 flex justify-between">
          {setupMode === 'assistant' ? (
            <>
              <span />
              <button
                onClick={markOnboardingComplete}
                className="px-5 py-2 text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
              >
                Done
              </button>
            </>
          ) : (
            <>
              <button
                onClick={handleSkipStep}
                className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-lg transition-colors"
              >
                {currentStep === 1 && !setupMode ? 'Skip All' : 'Skip'}
              </button>
              <div className="flex gap-3">
                {currentStep > 1 && (
                  <button
                    onClick={() => {
                      if (currentStep === 2 && setupMode === 'manual') {
                        setCurrentStep(1);
                      } else {
                        setCurrentStep((currentStep - 1) as Step);
                      }
                    }}
                    className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    Back
                  </button>
                )}
                {currentStep === 1 && setupMode === null && (
                  <button
                    onClick={handleSkipAll}
                    className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    Not Now
                  </button>
                )}
                {(currentStep === 1 && setupMode !== null || currentStep > 1) && (
                  <button
                    onClick={currentStep === 6 ? markOnboardingComplete : handleNextStep}
                    disabled={currentStep === 1 && setupMode === 'manual' && orgBio.length < 20}
                    className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
                      currentStep === 1 && setupMode === 'manual' && orgBio.length < 20
                        ? 'bg-gray-300 dark:bg-gray-700 text-gray-600 dark:text-gray-500 cursor-not-allowed'
                        : 'bg-blue-600 hover:bg-blue-700 text-white'
                    }`}
                  >
                    {currentStep === 6 ? 'Go to Dashboard' : 'Next'}
                  </button>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
