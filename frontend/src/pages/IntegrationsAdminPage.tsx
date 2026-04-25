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
import React, { useEffect, useState } from 'react';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { Trash2, Plus, AlertCircle, Edit2, CheckCircle, XCircle, FlaskConical, TriangleAlert, Info } from 'lucide-react';
import { usePageTitle } from '../hooks/usePageTitle';
import { useEntityLabels } from '../hooks/useEntityLabel';
import '../styles/CustomFields.css';

// Brand SVG icons
function SlackIcon({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M19.7 33.6c0 2.7-2.2 4.9-4.9 4.9s-4.9-2.2-4.9-4.9 2.2-4.9 4.9-4.9h4.9v4.9z" fill="#E01E5A"/>
      <path d="M22.2 33.6c0-2.7 2.2-4.9 4.9-4.9s4.9 2.2 4.9 4.9v12.3c0 2.7-2.2 4.9-4.9 4.9s-4.9-2.2-4.9-4.9V33.6z" fill="#E01E5A"/>
      <path d="M27.1 19.7c-2.7 0-4.9-2.2-4.9-4.9S24.4 10 27.1 10s4.9 2.2 4.9 4.9v4.9h-4.9z" fill="#36C5F0"/>
      <path d="M27.1 22.2c2.7 0 4.9 2.2 4.9 4.9s-2.2 4.9-4.9 4.9H14.8c-2.7 0-4.9-2.2-4.9-4.9s2.2-4.9 4.9-4.9h12.3z" fill="#36C5F0"/>
      <path d="M41 27.1c0 2.7-2.2 4.9-4.9 4.9s-4.9-2.2-4.9-4.9v-4.9H41v4.9z" fill="#2EB67D"/>
      <path d="M38.5 27.1c0 2.7-2.2 4.9-4.9 4.9s-4.9-2.2-4.9-4.9V14.8c0-2.7 2.2-4.9 4.9-4.9s4.9 2.2 4.9 4.9v12.3z" fill="#2EB67D"/>
      <path d="M33.6 41c2.7 0 4.9 2.2 4.9 4.9s-2.2 4.9-4.9 4.9-4.9-2.2-4.9-4.9V41h4.9z" fill="#ECB22E"/>
      <path d="M33.6 38.5c-2.7 0-4.9-2.2-4.9-4.9s2.2-4.9 4.9-4.9h12.3c2.7 0 4.9 2.2 4.9 4.9s-2.2 4.9-4.9 4.9H33.6z" fill="#ECB22E"/>
    </svg>
  );
}

function HubSpotIcon({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="27" cy="27" r="27" fill="#FF7A59"/>
      <path d="M32.5 20.3V17a3.5 3.5 0 0 0 2-3.1 3.5 3.5 0 0 0-3.5-3.5 3.5 3.5 0 0 0-3.5 3.5 3.5 3.5 0 0 0 2 3.1v3.4a9.9 9.9 0 0 0-4.7 2.1l-9.3-7.2a3 3 0 0 0 .1-.8A3 3 0 1 0 12.6 18a3 3 0 0 0 1.8-.6l9.1 7.1a9.9 9.9 0 0 0-1.4 5 9.9 9.9 0 0 0 1.3 4.9l-2.7 2.7a2.5 2.5 0 0 0-.8-.1 2.5 2.5 0 1 0 2.5 2.5 2.5 2.5 0 0 0-.1-.8l2.7-2.7A9.9 9.9 0 0 0 31 37.5a9.9 9.9 0 0 0 9.9-9.9 9.9 9.9 0 0 0-8.4-9.3zm-1.5 14.8a5.5 5.5 0 1 1 0-11 5.5 5.5 0 0 1 0 11z" fill="white"/>
    </svg>
  );
}

function ZapierIcon({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="27" cy="27" r="27" fill="#FF4A00"/>
      <path d="M36.8 22.6h-7.3l5.1-8.9-3.2-1.8-5.1 8.9-5.1-8.9-3.2 1.8 5.1 8.9h-7.3v3.7h7.3l-5.1 8.9 3.2 1.8 5.1-8.9 5.1 8.9 3.2-1.8-5.1-8.9h7.3v-3.7z" fill="white"/>
    </svg>
  );
}

function WebhookIcon({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 54 54" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="27" cy="27" r="27" fill="#6366F1"/>
      <path d="M17 27c0-3.3 2-6.1 4.8-7.3L19 14.5A13 13 0 0 0 14 27c0 5 2.8 9.3 7 11.5l2.8-5.2A7 7 0 0 1 17 27zm10 7a7 7 0 0 1-6.7-5H14a13 13 0 0 0 13 11v-6zm0-20v6a7 7 0 0 1 6.7 5H40A13 13 0 0 0 27 14zm6.2 16.3 2.8 5.2A13 13 0 0 0 40 27c0-5-2.8-9.3-7-11.5l-2.8 5.2A7 7 0 0 1 34 27a7 7 0 0 1-.8 3.3z" fill="white"/>
    </svg>
  );
}

const ADAPTER_ICONS: Record<string, (props: { size?: number }) => React.ReactElement> = {
  slack: SlackIcon,
  hubspot: HubSpotIcon,
  zapier: ZapierIcon,
  webhook: WebhookIcon,
};

interface IntegrationConfig {
  id: number;
  adapterType: string;
  name: string;
  enabled: boolean;
  eventTypes: string;
  createdAt: string;
  updatedAt: string;
}

interface CreateIntegrationRequest {
  adapterType: string;
  name: string;
  enabled: boolean;
  credentials: Record<string, string>;
  eventTypes: string;
}

// Adapter definitions with credential fields
const ADAPTERS = [
  {
    type: 'slack',
    label: 'Slack',
    description: 'Send CRM events to Slack channels via webhooks',
    fullDescription: 'Post notifications to Slack when customers, opportunities, or orders change. Slack will notify your team in real-time so everyone stays informed of important CRM activity.',
    bidirectional: false,
    examples: [
      'Post to #sales when a new Customer is created',
      'Alert to #customer-success when an Order status changes',
      'Notify team lead when an Opportunity is updated',
    ],
    credentialFields: [{ key: 'webhookUrl', label: 'Webhook URL', type: 'url', required: true }],
  },
  {
    type: 'webhook',
    label: 'Generic Webhook',
    description: 'Post CRM events to any HTTP endpoint',
    fullDescription: 'Send JSON payloads to any HTTP server. Perfect for custom integrations, automation platforms, or internal tools that need to react to CRM changes.',
    bidirectional: false,
    examples: [
      'Trigger a custom billing system when an Order is created',
      'Feed CRM data to a data warehouse for analytics',
      'Notify a project management tool when a Contact is updated',
    ],
    credentialFields: [
      { key: 'url', label: 'Endpoint URL', type: 'url', required: true },
      { key: 'secret', label: 'HMAC Secret (optional)', type: 'password', required: false },
    ],
  },
  {
    type: 'hubspot',
    label: 'HubSpot',
    description: 'Sync customers and opportunities with HubSpot',
    fullDescription: 'Keep your HubSpot CRM in sync with BOCRM. New customers and opportunities automatically create corresponding records in HubSpot.',
    bidirectional: false,
    examples: [
      'Sync new Customers to HubSpot Companies',
      'Create HubSpot Deals from Opportunities',
      'Update HubSpot when Contact details change',
    ],
    credentialFields: [{ key: 'apiKey', label: 'Private App Token', type: 'password', required: true }],
  },
  {
    type: 'zapier',
    label: 'Zapier',
    description: 'Trigger Zapier workflows from CRM events',
    fullDescription: 'Connect to 1000+ apps through Zapier. When CRM events occur, trigger Zapier workflows to automate tasks across your entire tool stack.',
    bidirectional: false,
    examples: [
      'Create a new row in Google Sheets when an Order is placed',
      'Send an email when a Customer is created',
      'Add a task to Asana when an Opportunity closes',
    ],
    credentialFields: [{ key: 'webhookUrl', label: 'Catch Hook URL', type: 'url', required: true }],
  },
];

const CRM_EVENTS = [
  'CUSTOMER_CREATED',
  'CUSTOMER_UPDATED',
  'CUSTOMER_DELETED',
  'CONTACT_CREATED',
  'CONTACT_UPDATED',
  'CONTACT_DELETED',
  'OPPORTUNITY_CREATED',
  'OPPORTUNITY_UPDATED',
  'OPPORTUNITY_DELETED',
  'ACTIVITY_CREATED',
  'ACTIVITY_UPDATED',
  'ACTIVITY_DELETED',
  'ORDER_CREATED',
  'ORDER_UPDATED',
  'ORDER_DELETED',
  'INVOICE_CREATED',
  'INVOICE_UPDATED',
  'INVOICE_DELETED',
];

const emptyForm: CreateIntegrationRequest = {
  adapterType: '',
  name: '',
  enabled: true,
  credentials: {},
  eventTypes: '',
};

// Helper function to convert event type to human-readable label
const getEventLabel = (eventType: string, entityLabels: Record<string, string>): string => {
  const [entity, action] = eventType.split('_');
  // Map uppercase entity names to capitalized form for lookup
  const entityKey = entity.charAt(0) + entity.slice(1).toLowerCase();
  const label = entityLabels[entityKey] || entity;
  const actionLabel = action.charAt(0) + action.slice(1).toLowerCase();
  return `${label} ${actionLabel}`;
};

// Group events by entity type
const getEventsByEntity = (): Record<string, string[]> => {
  const groups: Record<string, string[]> = {};
  CRM_EVENTS.forEach(event => {
    const entity = event.split('_')[0];
    if (!groups[entity]) groups[entity] = [];
    groups[entity].push(event);
  });
  return groups;
};

export function IntegrationsAdminPage() {
  usePageTitle('Integration Management');
  const entityLabels = useEntityLabels();

  const [configs, setConfigs] = useState<IntegrationConfig[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [selectedAdapter, setSelectedAdapter] = useState<string | null>(null);
  const [formData, setFormData] = useState<CreateIntegrationRequest>({ ...emptyForm });
  const [formLoading, setFormLoading] = useState(false);
  const [testingId, setTestingId] = useState<number | null>(null);
  const [failedEvents, setFailedEvents] = useState<any[]>([]);
  const [showFailedEvents, setShowFailedEvents] = useState(false);

  const loadConfigs = async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.getIntegrations();
      setConfigs(res.data ?? []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load integrations');
    } finally {
      setIsLoading(false);
    }
  };

  const loadFailedEvents = async () => {
    try {
      const res = await apiClient.getFailedIntegrationEvents();
      setFailedEvents(res.data ?? []);
      setShowFailedEvents(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load failed events');
    }
  };

  const handleTest = async (id: number) => {
    setTestingId(id);
    setError('');
    setSuccess('');
    try {
      const res = await apiClient.testIntegration(id);
      setSuccess(res.data?.message || 'Test event sent');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Test failed');
    } finally {
      setTestingId(null);
    }
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  const resetForm = () => {
    setShowForm(false);
    setEditId(null);
    setSelectedAdapter(null);
    setFormData({ ...emptyForm });
    setError('');
  };

  const handleSelectAdapter = (adapterType: string) => {
    setSelectedAdapter(adapterType);
    setFormData({
      ...emptyForm,
      adapterType,
    });
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, type } = e.target;
    const value = type === 'checkbox' ? (e.target as HTMLInputElement).checked : e.target.value;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleCredentialChange = (key: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      credentials: { ...prev.credentials, [key]: value },
    }));
  };

  const handleEventTypesChange = (event: string) => {
    const current = formData.eventTypes ? formData.eventTypes.split(',').map(e => e.trim()) : [];
    const updated = current.includes(event)
      ? current.filter(e => e !== event)
      : [...current, event];
    setFormData(prev => ({ ...prev, eventTypes: updated.join(',') }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    try {
      if (!formData.name.trim()) {
        setError('Configuration name is required');
        return;
      }

      if (editId) {
        await apiClient.updateIntegration(editId, formData);
        setSuccess('Integration updated successfully');
      } else {
        await apiClient.createIntegration(formData);
        setSuccess('Integration created successfully');
      }

      resetForm();
      loadConfigs();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save integration');
    } finally {
      setFormLoading(false);
    }
  };

  const handleEdit = (config: IntegrationConfig) => {
    setEditId(config.id);
    setSelectedAdapter(config.adapterType);
    setFormData({
      adapterType: config.adapterType,
      name: config.name,
      enabled: config.enabled,
      credentials: {},
      eventTypes: config.eventTypes,
    });
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this integration? This cannot be undone.')) return;

    try {
      await apiClient.deleteIntegration(id);
      setSuccess('Integration deleted');
      loadConfigs();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete integration');
    }
  };

  const handleToggleEnabled = async (config: IntegrationConfig) => {
    try {
      await apiClient.updateIntegration(config.id, {
        adapterType: config.adapterType,
        name: config.name,
        enabled: !config.enabled,
        credentials: {},
        eventTypes: config.eventTypes,
      });
      loadConfigs();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update integration');
    }
  };

  const selectedAdapterConfig = ADAPTERS.find(a => a.type === selectedAdapter);
  const currentEventTypes = formData.eventTypes ? formData.eventTypes.split(',').map(e => e.trim()) : [];

  return (
    <Layout>
      <div className="container mx-auto px-4 py-6">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Integration Management</h1>
          <p className="text-gray-600 dark:text-gray-400">Connect external services to automatically receive CRM events</p>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 flex gap-3">
            <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 flex-shrink-0" />
            <p className="text-red-700 dark:text-red-300">{error}</p>
          </div>
        )}

        {success && (
          <div className="mb-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 flex gap-3">
            <CheckCircle className="w-5 h-5 text-green-600 dark:text-green-400 flex-shrink-0" />
            <p className="text-green-700 dark:text-green-300">{success}</p>
          </div>
        )}

        {/* Active Configurations */}
        {!showForm && configs.length > 0 && (
          <div className="mb-8">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">Active Configurations</h2>
            <div className="space-y-3">
              {configs.map(config => {
                const adapter = ADAPTERS.find(a => a.type === config.adapterType);
                return (
                  <div key={config.id} className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        {ADAPTER_ICONS[config.adapterType]?.({ size: 28 })}
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-gray-900 dark:text-gray-100">{config.name}</h3>
                            {config.enabled ? (
                              <CheckCircle className="w-4 h-4 text-green-500" />
                            ) : (
                              <XCircle className="w-4 h-4 text-gray-400" />
                            )}
                          </div>
                          <p className="text-sm text-gray-600 dark:text-gray-400">{adapter?.label} • {config.eventTypes.split(',').length} event(s)</p>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleToggleEnabled(config)}
                        className="px-3 py-1 text-sm font-medium rounded bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                      >
                        {config.enabled ? 'Disable' : 'Enable'}
                      </button>
                      <button
                        onClick={() => handleTest(config.id)}
                        disabled={testingId === config.id}
                        title="Send a test event"
                        className="p-2 text-purple-600 dark:text-purple-400 hover:bg-purple-50 dark:hover:bg-purple-900/20 rounded disabled:opacity-40"
                      >
                        <FlaskConical className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleEdit(config)}
                        className="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(config.id)}
                        className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Failed Events Panel */}
        {!showForm && (
          <div className="mb-6">
            <button
              onClick={loadFailedEvents}
              className="flex items-center gap-2 text-sm text-amber-600 dark:text-amber-400 hover:underline"
            >
              <TriangleAlert className="w-4 h-4" />
              View dead-lettered events (failed after 3 retries)
            </button>
            {showFailedEvents && (
              <div className="mt-3 border border-amber-200 dark:border-amber-800 rounded-lg bg-amber-50 dark:bg-amber-900/20 p-4">
                <div className="flex justify-between items-center mb-3">
                  <h3 className="text-sm font-semibold text-amber-800 dark:text-amber-300">
                    Dead-lettered Events ({failedEvents.length})
                  </h3>
                  <button onClick={() => setShowFailedEvents(false)} className="text-xs text-gray-500 hover:text-gray-700 dark:text-gray-400">Hide</button>
                </div>
                {failedEvents.length === 0 ? (
                  <p className="text-sm text-gray-500 dark:text-gray-400">No failed events — all clear.</p>
                ) : (
                  <div className="space-y-2">
                    {failedEvents.map((event: any) => (
                      <div key={event.id} className="bg-white dark:bg-gray-800 border border-amber-200 dark:border-amber-700 rounded p-3 text-xs font-mono">
                        <div className="flex justify-between mb-1">
                          <span className="font-semibold text-gray-700 dark:text-gray-300">Event #{event.id}</span>
                          <span className="text-red-600 dark:text-red-400">{event.retryCount} retries</span>
                        </div>
                        <div className="text-gray-500 dark:text-gray-400 mb-1">{event.createdAt}</div>
                        <pre className="text-gray-600 dark:text-gray-300 overflow-auto max-h-24 whitespace-pre-wrap">
                          {typeof event.payload === 'object' ? JSON.stringify(event.payload, null, 2) : event.payload}
                        </pre>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Adapter Cards or Form */}
        {!showForm && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {ADAPTERS.map(adapter => (
              <div key={adapter.type} className="space-y-3">
                {/* Info Box */}
                <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
                  <div className="flex gap-3">
                    <Info className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                    <div>
                      <h4 className="font-semibold text-sm text-blue-900 dark:text-blue-300 mb-1">
                        {adapter.label}
                      </h4>
                      <p className="text-sm text-blue-800 dark:text-blue-400 mb-3">
                        {adapter.fullDescription}
                      </p>
                      <div className="space-y-2">
                        <div className="text-xs font-medium text-blue-700 dark:text-blue-300">
                          {adapter.bidirectional ? '↔️ Bidirectional sync' : '→ One-way (BOCRM to external)'}
                        </div>
                        <div className="text-xs text-blue-700 dark:text-blue-400">
                          <div className="font-medium mb-1">Examples:</div>
                          <ul className="list-disc list-inside space-y-0.5">
                            {adapter.examples.map((ex, i) => <li key={i}>{ex}</li>)}
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Configure Card */}
                <div
                  className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 hover:shadow-lg dark:hover:shadow-gray-900/50 transition cursor-pointer"
                  onClick={() => {
                    handleSelectAdapter(adapter.type);
                    setShowForm(true);
                  }}
                >
                  <div className="flex items-center gap-3 mb-3">
                    {ADAPTER_ICONS[adapter.type]?.({ size: 36 })}
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{adapter.label}</h3>
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">{adapter.description}</p>
                  <button className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded flex items-center justify-center gap-2">
                    <Plus className="w-4 h-4" />
                    Configure
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Configuration Form */}
        {showForm && selectedAdapterConfig && (
          <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-6 max-w-2xl">
            <div className="flex items-center gap-3 mb-4">
              {ADAPTER_ICONS[selectedAdapterConfig.type]?.({ size: 36 })}
              <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                {editId ? 'Edit' : 'Configure'} {selectedAdapterConfig.label}
              </h2>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Configuration Name
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder={`e.g., Sales Slack Channel, Production Webhook`}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400"
                  required
                />
              </div>

              {/* Credentials */}
              <div className="space-y-3">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Credentials
                </label>
                {selectedAdapterConfig.credentialFields.map(field => (
                  <div key={field.key}>
                    <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                      {field.label} {field.required && <span className="text-red-600">*</span>}
                    </label>
                    <input
                      type={field.type}
                      value={formData.credentials[field.key] || ''}
                      onChange={e => handleCredentialChange(field.key, e.target.value)}
                      placeholder={field.label}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400"
                      required={field.required}
                    />
                  </div>
                ))}
              </div>

              {/* Event Types */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Subscribe to Events (leave blank for all)
                </label>
                <div className="space-y-3">
                  {Object.entries(getEventsByEntity()).map(([entity, events]) => {
                    const entityKey = entity.charAt(0) + entity.slice(1).toLowerCase();
                    return (
                    <div key={entity}>
                      <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase mb-1.5">
                        {entityLabels[entityKey] || entity}
                      </div>
                      <div className="grid grid-cols-2 gap-2 ml-2">
                        {events.map(event => (
                          <label key={event} className="flex items-center gap-2 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={currentEventTypes.includes(event)}
                              onChange={() => handleEventTypesChange(event)}
                              className="rounded"
                            />
                            <span className="text-sm text-gray-700 dark:text-gray-300">
                              {getEventLabel(event, entityLabels)}
                            </span>
                          </label>
                        ))}
                      </div>
                    </div>
                    );
                  })}
                </div>
              </div>

              {/* Enable Toggle */}
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  name="enabled"
                  checked={formData.enabled}
                  onChange={handleInputChange}
                  className="rounded"
                />
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  Enable this integration
                </label>
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  disabled={formLoading}
                  className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium py-2 px-4 rounded"
                >
                  {formLoading ? 'Saving...' : editId ? 'Update' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  className="bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-900 dark:text-gray-100 font-medium py-2 px-4 rounded"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {isLoading && !showForm && (
          <div className="text-center py-8">
            <p className="text-gray-600 dark:text-gray-400">Loading integrations...</p>
          </div>
        )}
      </div>
    </Layout>
  );
}
