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
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

interface Subscription {
  tierName: string;
  tierDisplayName?: string;
  tokensUsedThisPeriod: number;
  monthlyTokenLimit: number;
  periodEndDate: string;
  modelId?: string;
  provider?: string;
}

interface Tier {
  id: number;
  name: string;
  displayName: string;
  monthlyTokenLimit: number;
  modelId: string;
  provider: string;
  priceMonthly: number;
}

interface AiModel {
  id: number;
  provider: string;
  modelId: string;
  enabled: boolean;
}

interface McpApiKey {
  id: number;
  name: string;
  enabled: boolean;
  lastUsedAt: string | null;
  createdAt: string;
  keyPrefix?: string;
}

const modelLabel = (modelId?: string, provider?: string) => {
  if (!modelId) return '';
  if (provider === 'openai' || modelId.startsWith('gpt'))
    return modelId.replace('gpt-', 'GPT-').replace(/-(\w)/g, (_, c) => ' ' + c.toUpperCase()).trim();
  if (provider === 'google' || modelId.startsWith('gemini'))
    return modelId.replace('gemini-', 'Gemini ').replace(/-/g, ' ');
  if (provider === 'ollama') return modelId;
  if (modelId.includes('haiku')) return 'Haiku';
  if (modelId.includes('sonnet')) return 'Sonnet';
  if (modelId.includes('opus')) return 'Opus';
  return modelId;
};

const providerBadge = (provider?: string) => {
  switch (provider) {
    case 'openai':
      return (
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300">
          OpenAI
        </span>
      );
    case 'google':
      return (
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300">
          Google
        </span>
      );
    case 'ollama':
      return (
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-orange-100 dark:bg-orange-900 text-orange-700 dark:text-orange-300">
          Ollama
        </span>
      );
    default:
      return (
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-900 text-purple-700 dark:text-purple-300">
          Anthropic
        </span>
      );
  }
};

export const AssistantSettingsPage = () => {
  usePageTitle('Assistant Settings');
  const navigate = useNavigate();
  const { currentRole } = useAuthStore();
  const isSystemAdmin = currentRole === 'system_admin';
  const isAdmin = currentRole != null && (currentRole === 'admin' || currentRole.toLowerCase().includes('admin'));

  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [tiers, setTiers] = useState<Tier[]>([]);
  const [models, setModels] = useState<AiModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [upgrading, setUpgrading] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Edit tier state
  const [editingTierId, setEditingTierId] = useState<number | null>(null);
  const [editProvider, setEditProvider] = useState('anthropic');
  const [editModelId, setEditModelId] = useState('');
  const [saving, setSaving] = useState(false);

  // API Key state
  const [apiKeys, setApiKeys] = useState<McpApiKey[]>([]);
  const [showKeyForm, setShowKeyForm] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [generatingKey, setGeneratingKey] = useState(false);
  const [generatedKeyValue, setGeneratedKeyValue] = useState('');
  const [revokingKeyId, setRevokingKeyId] = useState<number | null>(null);

  useEffect(() => {
    if (!isAdmin) {
      navigate('/dashboard');
      return;
    }
    fetchData();
  }, [isAdmin]);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [subRes, tiersRes] = await Promise.all([
        apiClient.getAssistantSubscription(),
        apiClient.getAssistantTiers(),
      ]);
      setSubscription(subRes.data);
      // getAssistantTiers() returns { tiers, models } combined response
      const tiersData = tiersRes.data;
      if (tiersData && Array.isArray(tiersData.tiers)) {
        setTiers(tiersData.tiers);
        setModels(tiersData.models || []);
      } else {
        // fallback if server returns plain array
        setTiers(Array.isArray(tiersData) ? tiersData : []);
      }

      // Fetch API keys separately with error handling
      try {
        const keysRes = await apiClient.listMcpApiKeys();
        setApiKeys(keysRes.data || []);
      } catch (keyErr: any) {
        // If keys endpoint fails, just skip it (might not be available on older backends)
        console.warn('Failed to load API keys:', keyErr);
        setApiKeys([]);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load assistant settings');
    } finally {
      setLoading(false);
    }
  };

  const handleUpgrade = async (tierName: string) => {
    if (upgrading) return;
    setError('');
    setSuccessMsg('');
    setUpgrading(tierName);
    try {
      const res = await apiClient.upgradeAssistantTier(tierName);
      setSubscription(res.data);
      setSuccessMsg(`Upgraded to ${tierName} tier successfully.`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upgrade tier');
    } finally {
      setUpgrading(null);
    }
  };

  const startEdit = (tier: Tier) => {
    setEditingTierId(tier.id);
    setEditProvider(tier.provider || 'anthropic');
    setEditModelId(tier.modelId);
  };

  const cancelEdit = () => {
    setEditingTierId(null);
  };

  const handleSaveTier = async (tierId: number) => {
    setSaving(true);
    setError('');
    try {
      const res = await apiClient.updateAssistantTier(tierId, { provider: editProvider, modelId: editModelId });
      setTiers((prev) => prev.map((t) => (t.id === tierId ? { ...t, ...res.data } : t)));
      setSuccessMsg('Tier updated successfully.');
      setEditingTierId(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update tier');
    } finally {
      setSaving(false);
    }
  };

  const handleGenerateKey = async () => {
    if (!newKeyName.trim()) {
      setError('Key name is required');
      return;
    }
    setGeneratingKey(true);
    setError('');
    try {
      const res = await apiClient.generateMcpApiKey(newKeyName);
      const message = res.data;
      // Extract key from response (format: "MCP API key generated successfully!\n\n**Key Name:** {name}\n**Raw Key:** {key}")
      const keyMatch = message.match(/\*\*Raw Key:\*\*\s+(\S+)/);
      const keyValue = keyMatch ? keyMatch[1] : '';
      setGeneratedKeyValue(keyValue);
      setNewKeyName('');
      setShowKeyForm(false);
      setSuccessMsg('API key generated successfully! Copy the key immediately—it cannot be retrieved later.');
      // Refresh keys after a short delay
      setTimeout(() => fetchData(), 500);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate API key');
    } finally {
      setGeneratingKey(false);
    }
  };

  const handleRevokeKey = async (keyId: number) => {
    if (!confirm('Are you sure? This key will be revoked immediately.')) return;
    setRevokingKeyId(keyId);
    setError('');
    try {
      await apiClient.revokeMcpApiKey(keyId);
      setApiKeys((prev) => prev.filter((k) => k.id !== keyId));
      setSuccessMsg('API key revoked successfully.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to revoke API key');
    } finally {
      setRevokingKeyId(null);
    }
  };

  const percentUsed =
    subscription && subscription.monthlyTokenLimit > 0
      ? Math.min(100, (subscription.tokensUsedThisPeriod / subscription.monthlyTokenLimit) * 100)
      : null;

  return (
    <Layout>
      <div className="space-y-6 max-w-3xl">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">AI Assistant Settings</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Manage your BOCRM Assistant subscription and token usage.
          </p>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm">
            {error}
          </div>
        )}

        {successMsg && (
          <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-300 px-4 py-3 rounded-lg text-sm">
            {successMsg}
          </div>
        )}

        {loading ? (
          <div className="text-center py-12 text-gray-500 dark:text-gray-400">Loading...</div>
        ) : (
          <>
            {/* Current Usage */}
            {subscription && (
              <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-4">
                  Current Plan
                </h2>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xl font-bold text-gray-900 dark:text-white">
                      {subscription.tierDisplayName || subscription.tierName}
                    </span>
                    {providerBadge(subscription.provider)}
                    {subscription.modelId && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
                        {modelLabel(subscription.modelId, subscription.provider)}
                      </span>
                    )}
                  </div>
                  {subscription.periodEndDate && (
                    <span className="text-xs text-gray-400 dark:text-gray-500">
                      Resets {new Date(subscription.periodEndDate).toLocaleDateString()}
                    </span>
                  )}
                </div>

                <div className="mt-4">
                  <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
                    <span>Token Usage</span>
                    <span>
                      {subscription.monthlyTokenLimit === -1
                        ? 'Unlimited'
                        : `${subscription.tokensUsedThisPeriod.toLocaleString()} / ${subscription.monthlyTokenLimit.toLocaleString()}`}
                    </span>
                  </div>
                  {subscription.monthlyTokenLimit !== -1 && percentUsed !== null && (
                    <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${
                          percentUsed > 90
                            ? 'bg-red-500'
                            : percentUsed > 70
                            ? 'bg-amber-500'
                            : 'bg-green-500'
                        }`}
                        style={{ width: `${percentUsed}%` }}
                      />
                    </div>
                  )}
                  {subscription.monthlyTokenLimit === -1 && (
                    <div className="h-2 bg-green-200 dark:bg-green-800 rounded-full overflow-hidden">
                      <div className="h-full w-full bg-green-500 rounded-full" />
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Available Tiers */}
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Available Plans
                </h2>
                {isAdmin && !isSystemAdmin && (
                  <span className="text-xs text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/30 px-3 py-1 rounded-full">
                    Contact support to upgrade
                  </span>
                )}
              </div>
              <div className="space-y-3">
                {tiers.map((tier) => {
                  const isCurrent = subscription?.tierName === tier.name;
                  const isEditing = editingTierId === tier.id;
                  return (
                    <div
                      key={tier.id}
                      className={`p-4 rounded-lg border transition-colors ${
                        isCurrent
                          ? 'border-blue-300 dark:border-blue-600 bg-blue-50 dark:bg-blue-900/20'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-gray-900 dark:text-white">
                              {tier.displayName}
                            </span>
                            {providerBadge(tier.provider)}
                            <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
                              {modelLabel(tier.modelId, tier.provider)}
                            </span>
                            {isCurrent && (
                              <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-800 text-blue-700 dark:text-blue-200">
                                Current
                              </span>
                            )}
                          </div>
                          <div className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                            {tier.monthlyTokenLimit === -1
                              ? 'Unlimited tokens/month'
                              : `${tier.monthlyTokenLimit.toLocaleString()} tokens/month`}
                          </div>
                        </div>
                        <div className="flex items-center gap-2 ml-4">
                          <span className="font-semibold text-gray-900 dark:text-white whitespace-nowrap">
                            {tier.priceMonthly === 0
                              ? 'Free'
                              : `$${tier.priceMonthly.toFixed(2)}/mo`}
                          </span>
                          {isSystemAdmin && (
                            <button
                              onClick={() => (isEditing ? cancelEdit() : startEdit(tier))}
                              className="px-3 py-1.5 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs font-medium rounded-lg transition-colors"
                            >
                              {isEditing ? 'Cancel' : 'Edit'}
                            </button>
                          )}
                          {!isCurrent && isSystemAdmin && (
                            <button
                              onClick={() => handleUpgrade(tier.name)}
                              disabled={!!upgrading}
                              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-medium rounded-lg transition-colors"
                            >
                              {upgrading === tier.name ? 'Upgrading...' : 'Select'}
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Inline Edit Form */}
                      {isEditing && (
                        <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-600 space-y-3">
                          <div className="grid grid-cols-2 gap-3">
                            <div>
                              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                                Provider
                              </label>
                              <select
                                value={editProvider}
                                onChange={(e) => {
                                  setEditProvider(e.target.value);
                                  const availableModels = models.filter(m => m.provider === e.target.value && m.enabled);
                                  setEditModelId(availableModels.length > 0 ? availableModels[0].modelId : '');
                                }}
                                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                              >
                                {['anthropic', 'openai', 'google', 'ollama'].map(provider => {
                                  const hasEnabledModels = models.some(m => m.provider === provider && m.enabled);
                                  if (!hasEnabledModels) return null;
                                  const labels: Record<string, string> = { anthropic: 'Anthropic', openai: 'OpenAI', google: 'Google', ollama: 'Ollama' };
                                  return (
                                    <option key={provider} value={provider}>
                                      {labels[provider]}
                                    </option>
                                  );
                                })}
                              </select>
                            </div>
                            <div>
                              <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                                Model
                              </label>
                              <select
                                value={editModelId}
                                onChange={(e) => setEditModelId(e.target.value)}
                                className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                              >
                                {models
                                  .filter(m => m.provider === editProvider && m.enabled)
                                  .map((m) => (
                                    <option key={m.id} value={m.modelId}>
                                      {modelLabel(m.modelId, editProvider)}
                                    </option>
                                  ))}
                              </select>
                            </div>
                          </div>
                          <div className="flex justify-end">
                            <button
                              onClick={() => handleSaveTier(tier.id)}
                              disabled={saving || !editModelId}
                              className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-medium rounded-lg transition-colors"
                            >
                              {saving ? 'Saving...' : 'Save Changes'}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* API Key Management */}
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  MCP API Keys
                </h2>
                <button
                  onClick={() => setShowKeyForm(true)}
                  className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg transition-colors"
                >
                  + Generate Key
                </button>
              </div>

              <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                Long-lived API keys for Claude Desktop and MCP integrations. Keys never expire automatically—revoke them if compromised.
              </p>

              {/* Generated Key Display */}
              {generatedKeyValue && (
                <div className="mb-4 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-semibold text-blue-900 dark:text-blue-200 mb-1">Save your API key now</p>
                      <div className="flex items-center gap-2">
                        <code className="text-xs bg-blue-100 dark:bg-blue-800 px-2 py-1 rounded font-mono text-blue-900 dark:text-blue-100 break-all">
                          {generatedKeyValue}
                        </code>
                        <button
                          onClick={() => {
                            navigator.clipboard.writeText(generatedKeyValue);
                            setSuccessMsg('Key copied to clipboard!');
                          }}
                          className="px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded transition-colors"
                        >
                          Copy
                        </button>
                      </div>
                    </div>
                    <button
                      onClick={() => setGeneratedKeyValue('')}
                      className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 text-xs font-medium"
                    >
                      Dismiss
                    </button>
                  </div>
                </div>
              )}

              {/* Generate Key Form */}
              {showKeyForm && (
                <div className="mb-4 p-4 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Key Name
                  </label>
                  <input
                    type="text"
                    value={newKeyName}
                    onChange={(e) => setNewKeyName(e.target.value)}
                    placeholder="e.g., My MacBook Claude"
                    className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 mb-3"
                  />
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => {
                        setShowKeyForm(false);
                        setNewKeyName('');
                      }}
                      className="px-3 py-1.5 bg-gray-200 dark:bg-gray-600 hover:bg-gray-300 dark:hover:bg-gray-500 text-gray-700 dark:text-gray-300 text-xs font-medium rounded-lg transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleGenerateKey}
                      disabled={generatingKey || !newKeyName.trim()}
                      className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-medium rounded-lg transition-colors"
                    >
                      {generatingKey ? 'Generating...' : 'Generate'}
                    </button>
                  </div>
                </div>
              )}

              {/* Keys Table */}
              {apiKeys.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-gray-200 dark:border-gray-700">
                        <th className="text-left py-3 px-0 font-semibold text-gray-600 dark:text-gray-400">Name</th>
                        <th className="text-left py-3 px-0 font-semibold text-gray-600 dark:text-gray-400">Status</th>
                        <th className="text-left py-3 px-0 font-semibold text-gray-600 dark:text-gray-400">Created</th>
                        <th className="text-left py-3 px-0 font-semibold text-gray-600 dark:text-gray-400">Last Used</th>
                        <th className="text-right py-3 px-0 font-semibold text-gray-600 dark:text-gray-400">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {apiKeys.map((key) => (
                        <tr key={key.id} className="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50">
                          <td className="py-3 px-0 text-gray-900 dark:text-white font-medium">{key.name}</td>
                          <td className="py-3 px-0">
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                                key.enabled
                                  ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                              }`}
                            >
                              {key.enabled ? 'Active' : 'Revoked'}
                            </span>
                          </td>
                          <td className="py-3 px-0 text-gray-600 dark:text-gray-400 text-xs">
                            {new Date(key.createdAt).toLocaleDateString()}
                          </td>
                          <td className="py-3 px-0 text-gray-600 dark:text-gray-400 text-xs">
                            {key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleDateString() : '—'}
                          </td>
                          <td className="py-3 px-0 text-right">
                            {key.enabled && (
                              <button
                                onClick={() => handleRevokeKey(key.id)}
                                disabled={revokingKeyId === key.id}
                                className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                {revokingKeyId === key.id ? 'Revoking...' : 'Revoke'}
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400 py-4">
                  No API keys yet. Create one to use BOCRM tools in Claude Desktop.
                </p>
              )}
            </div>
          </>
        )}
      </div>
    </Layout>
  );
};
