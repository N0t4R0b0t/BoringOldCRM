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
import { useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { Trash2, Plus, AlertCircle, Edit2 } from 'lucide-react';
import { usePageTitle } from '../hooks/usePageTitle';
import '../styles/CustomFields.css';

// ─── Document Templates types & helpers ────────────────────────────

interface DocumentTemplate {
  id: number;
  name: string;
  description?: string;
  templateType: string;
  styleJson?: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

interface StyleConfig {
  layout?: string;
  accentColor?: string;
  backgroundColor?: string;
  slideBackground?: string;
  textColor?: string;
  h1Color?: string;
  h2Color?: string;
}

const DOC_TEMPLATE_TYPES = [
  { value: 'slide_deck', label: 'Slide Deck' },
  { value: 'one_pager', label: 'One Pager' },
  { value: 'csv_export', label: 'CSV Export' },
  { value: 'report', label: 'Report' },
];

const TYPE_HINTS: Record<string, string> = {
  slide_deck: `Valid styleJson keys:
  layout: "dark" | "light" | "corporate" | "minimal"
  accentColor, backgroundColor, slideBackground, textColor, h1Color, h2Color: hex strings
  includeFields: ["name", "status", ...]
  excludeFields: ["notes", ...]`,
  one_pager: `Valid styleJson keys:
  layout: "compact" | "detailed"
  accentColor, backgroundColor, textColor: hex strings
  includeCustomFields: true | false
  includeFields: ["name", "status", ...]
  excludeFields: ["notes", ...]`,
  csv_export: `No styleJson keys for CSV exports.
  The templateId will seed the export title if no title is provided.`,
  report: `No styleJson keys for reports.
  The templateId will seed the report title if no title is provided.`,
};

const emptyDocForm = { name: '', description: '', templateType: 'slide_deck', styleJson: '', isDefault: false };

const parseStyleJson = (jsonStr?: string): StyleConfig => {
  if (!jsonStr) return {};
  try { return JSON.parse(jsonStr); } catch { return {}; }
};

const PreviewPanel = ({ styleJson, templateType }: { styleJson?: string; templateType: string }) => {
  const style = parseStyleJson(styleJson);
  const bgColor = style.backgroundColor || '#ffffff';
  const accentColor = style.accentColor || '#2563eb';
  const textColor = style.textColor || '#1f2937';

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
      <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Preview</h4>
      <div
        className="rounded-lg p-6 h-48 flex flex-col justify-between overflow-hidden"
        style={{ backgroundColor: bgColor, color: textColor, border: `2px solid ${accentColor}` }}
      >
        <div>
          <h5 style={{ color: style.h1Color || accentColor }} className="text-lg font-bold mb-2">Sample Title</h5>
          <h6 style={{ color: style.h2Color || accentColor }} className="text-sm font-semibold mb-3">Subtitle</h6>
        </div>
        <div className="text-xs space-y-1">
          <p>Layout: {style.layout || 'default'}</p>
          <p>Accent: {accentColor}</p>
          <p>Type: {DOC_TEMPLATE_TYPES.find(t => t.value === templateType)?.label}</p>
        </div>
      </div>
    </div>
  );
};

// ─── Notification Templates types ──────────────────────────────────

interface NotificationTemplate {
  id: number;
  notificationType: string;
  name: string;
  subjectTemplate: string;
  bodyTemplate: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface NotificationTypeMetadata {
  type: string;
  label: string;
  variables: string[];
}

// ─── Document Templates Tab ────────────────────────────────────────

const DocumentTemplatesTab = () => {
  const [templates, setTemplates] = useState<DocumentTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [formData, setFormData] = useState({ ...emptyDocForm });
  const [formLoading, setFormLoading] = useState(false);
  const [cloneFromId, setCloneFromId] = useState<number | null>(null);

  const loadTemplates = async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.listDocumentTemplates({ size: 200 });
      setTemplates(res.data?.content ?? res.data ?? []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load templates');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadTemplates(); }, []);

  const resetForm = () => {
    setShowForm(false);
    setEditId(null);
    setFormData({ ...emptyDocForm });
    setCloneFromId(null);
    setError('');
  };

  const handleCloneFromTemplate = (templateId: number) => {
    const template = templates.find(t => t.id === templateId);
    if (!template) return;
    setCloneFromId(templateId);
    setFormData({
      name: template.name + ' (Copy)',
      description: template.description || '',
      templateType: template.templateType,
      styleJson: template.styleJson || '',
      isDefault: false,
    });
    setShowForm(true);
    setEditId(null);
    setError('');
  };

  const handleEdit = (t: DocumentTemplate) => {
    setEditId(t.id);
    setFormData({
      name: t.name,
      description: t.description || '',
      templateType: t.templateType,
      styleJson: t.styleJson || '',
      isDefault: t.isDefault,
    });
    setShowForm(true);
    setCloneFromId(null);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    setError('');
    try {
      const payload = {
        name: formData.name,
        description: formData.description || undefined,
        templateType: formData.templateType,
        styleJson: formData.styleJson || undefined,
        isDefault: formData.isDefault,
      };
      if (editId !== null) {
        await apiClient.updateDocumentTemplate(editId, payload as any);
      } else {
        await apiClient.createDocumentTemplate(payload as any);
      }
      setSuccess(editId !== null ? 'Template updated.' : 'Template created.');
      resetForm();
      loadTemplates();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save template');
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete template "${name}"? This cannot be undone.`)) return;
    setError('');
    try {
      await apiClient.deleteDocumentTemplate(id);
      setSuccess('Template deleted.');
      setTemplates(prev => prev.filter(t => t.id !== id));
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete template');
    }
  };

  const typeBadgeClass = (type: string) => {
    const map: Record<string, string> = {
      slide_deck: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
      one_pager: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
      csv_export: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
      report: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300',
    };
    return map[type] || 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
  };

  const typeLabel = (type: string) => DOC_TEMPLATE_TYPES.find(t => t.value === type)?.label ?? type;

  return (
    <>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Define reusable style templates for AI-generated slide decks, one-pagers, CSV exports, and reports.
        </p>
        {!showForm && (
          <button
            onClick={() => { setShowForm(true); setEditId(null); setCloneFromId(null); setFormData({ ...emptyDocForm }); }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-sm"
          >
            + New Template
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">
          {error}
        </div>
      )}
      {success && (
        <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-300 px-4 py-3 rounded-lg mb-4">
          {success}
        </div>
      )}

      {showForm && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 mb-4">
          <div className="lg:col-span-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm">
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4">
              {editId !== null ? 'Edit Template' : cloneFromId ? 'Clone Template' : 'New Document Template'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              {!editId && !cloneFromId && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Create from existing template
                  </label>
                  <select
                    value={cloneFromId || ''}
                    onChange={e => {
                      const templateId = e.target.value ? parseInt(e.target.value) : null;
                      if (templateId) handleCloneFromTemplate(templateId);
                    }}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">None - Create from scratch</option>
                    {templates.map(t => (
                      <option key={t.id} value={t.id}>
                        {t.name} ({typeLabel(t.templateType)})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={e => setFormData(p => ({ ...p, name: e.target.value }))}
                    required
                    minLength={2}
                    maxLength={255}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g. Corporate Deck"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Template Type <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.templateType}
                    onChange={e => setFormData(p => ({ ...p, templateType: e.target.value }))}
                    required
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {DOC_TEMPLATE_TYPES.map(t => (
                      <option key={t.value} value={t.value}>{t.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={e => setFormData(p => ({ ...p, description: e.target.value }))}
                  rows={2}
                  maxLength={1000}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-y"
                  placeholder="Optional description visible in the AI assistant template list"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Style JSON
                </label>
                <textarea
                  value={formData.styleJson}
                  onChange={e => setFormData(p => ({ ...p, styleJson: e.target.value }))}
                  rows={6}
                  className="template-style-json-textarea w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder='{"layout":"corporate","accentColor":"#1a3a5c"}'
                />
                <div className="template-type-hint">{TYPE_HINTS[formData.templateType]}</div>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="docIsDefault"
                  checked={formData.isDefault}
                  onChange={e => setFormData(p => ({ ...p, isDefault: e.target.checked }))}
                  className="h-4 w-4 text-blue-600 border-gray-300 rounded"
                />
                <label htmlFor="docIsDefault" className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  Set as default template for this type
                </label>
              </div>

              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={formLoading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-sm disabled:opacity-50"
                >
                  {formLoading ? 'Saving...' : editId !== null ? 'Update' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 font-medium text-sm"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>

          <PreviewPanel styleJson={formData.styleJson} templateType={formData.templateType} />
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">Loading...</div>
        ) : templates.length === 0 ? (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">
            No document templates yet. Create one to let the AI assistant reuse your style preferences.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-400 uppercase text-xs tracking-wider">
              <tr>
                <th className="px-4 py-3 text-left">Name</th>
                <th className="px-4 py-3 text-left">Type</th>
                <th className="px-4 py-3 text-left">Description</th>
                <th className="px-4 py-3 text-left">Default</th>
                <th className="px-4 py-3 text-left">Created</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {templates.map(t => (
                <tr key={t.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">{t.name}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${typeBadgeClass(t.templateType)}`}>
                      {typeLabel(t.templateType)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400 max-w-xs truncate">
                    {t.description || '—'}
                  </td>
                  <td className="px-4 py-3">
                    {t.isDefault && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300">
                        default
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">
                    {new Date(t.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-right space-x-3">
                    <button
                      onClick={() => handleCloneFromTemplate(t.id)}
                      className="text-green-600 hover:text-green-700 dark:text-green-400 dark:hover:text-green-300 font-medium"
                    >
                      Clone
                    </button>
                    <button
                      onClick={() => handleEdit(t)}
                      className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(t.id, t.name)}
                      className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
};

// ─── Notification Templates Tab ────────────────────────────────────

const NotificationTemplatesTab = () => {
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [typeMetadata, setTypeMetadata] = useState<NotificationTypeMetadata[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    notificationType: '',
    name: '',
    subjectTemplate: '',
    bodyTemplate: '',
    isActive: true,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadTemplates();
    loadTypeMetadata();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await apiClient.listNotificationTemplates();
      setTemplates(res.data?.content || []);
    } catch (err) {
      console.error('Failed to load templates:', err);
      setError('Failed to load notification templates');
    } finally {
      setLoading(false);
    }
  };

  const loadTypeMetadata = async () => {
    try {
      const res = await apiClient.getNotificationTemplateTypes();
      setTypeMetadata(res.data || []);
    } catch (err) {
      console.error('Failed to load type metadata:', err);
    }
  };

  const handleCreate = () => {
    setEditingId(null);
    setFormData({
      notificationType: '',
      name: '',
      subjectTemplate: '',
      bodyTemplate: '',
      isActive: true,
    });
    setShowForm(true);
  };

  const handleEdit = (template: NotificationTemplate) => {
    setEditingId(template.id);
    setFormData({
      notificationType: template.notificationType,
      name: template.name,
      subjectTemplate: template.subjectTemplate,
      bodyTemplate: template.bodyTemplate,
      isActive: template.isActive,
    });
    setShowForm(true);
  };

  const handleSave = async () => {
    if (!formData.notificationType || !formData.name) {
      setError('Notification type and name are required');
      return;
    }

    try {
      setSaving(true);
      setError(null);

      if (editingId) {
        await apiClient.updateNotificationTemplate(editingId, {
          name: formData.name,
          subjectTemplate: formData.subjectTemplate,
          bodyTemplate: formData.bodyTemplate,
          isActive: formData.isActive,
        });
      } else {
        await apiClient.createNotificationTemplate(formData);
      }

      setShowForm(false);
      loadTemplates();
    } catch (err) {
      console.error('Failed to save template:', err);
      setError('Failed to save template');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this template?')) return;

    try {
      setError(null);
      await apiClient.deleteNotificationTemplate(id);
      loadTemplates();
    } catch (err) {
      console.error('Failed to delete template:', err);
      setError('Failed to delete template');
    }
  };

  const getTypeLabel = (type: string) => {
    const metadata = typeMetadata.find((m) => m.type === type);
    return metadata?.label || type.replace(/_/g, ' ');
  };

  const getAvailableVariables = () => {
    if (!formData.notificationType) return [];
    const metadata = typeMetadata.find((m) => m.type === formData.notificationType);
    return metadata?.variables || [];
  };

  return (
    <>
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Customize email templates for system notifications with dynamic placeholders.
        </p>
        <button
          onClick={handleCreate}
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors font-medium text-sm"
        >
          <Plus size={18} />
          New Template
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-900 rounded-lg flex items-center gap-2 mb-4">
          <AlertCircle size={16} className="text-red-600 dark:text-red-400" />
          <p className="text-sm text-red-800 dark:text-red-300">{error}</p>
        </div>
      )}

      {loading && <div className="text-center py-8 text-gray-500 dark:text-gray-400">Loading templates...</div>}

      {!loading && !showForm && (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
          {templates.length === 0 ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">No templates yet. Create one to get started.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                      Subject Preview
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                  {templates.map((template) => (
                    <tr key={template.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                      <td className="px-6 py-4 text-sm">
                        <span className="px-2 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded text-xs font-medium">
                          {getTypeLabel(template.notificationType)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">{template.name}</td>
                      <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400 truncate max-w-xs">
                        {template.subjectTemplate || '—'}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        {template.isActive ? (
                          <span className="px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300 rounded text-xs font-medium">
                            Active
                          </span>
                        ) : (
                          <span className="px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300 rounded text-xs font-medium">
                            Inactive
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-right flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleEdit(template)}
                          className="p-2 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(template.id)}
                          className="p-2 text-gray-600 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {showForm && (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {editingId ? 'Edit Template' : 'Create Template'}
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Notification Type *
              </label>
              <select
                value={formData.notificationType}
                onChange={(e) => setFormData({ ...formData, notificationType: e.target.value })}
                disabled={!!editingId}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
              >
                <option value="">Select a type...</option>
                {typeMetadata.map((meta) => (
                  <option key={meta.type} value={meta.type}>
                    {meta.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Template Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g., Default Record Modified"
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Subject Template
              </label>
              <input
                type="text"
                value={formData.subjectTemplate}
                onChange={(e) => setFormData({ ...formData, subjectTemplate: e.target.value })}
                placeholder="e.g., {{actorName}} updated {{entityName}}"
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Body Template (HTML)
              </label>
              <textarea
                value={formData.bodyTemplate}
                onChange={(e) => setFormData({ ...formData, bodyTemplate: e.target.value })}
                placeholder="Enter HTML template..."
                rows={6}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-blue-500 font-mono text-sm"
              />
            </div>

            {getAvailableVariables().length > 0 && (
              <div className="md:col-span-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-900 rounded p-4">
                <p className="text-sm font-medium text-blue-900 dark:text-blue-300 mb-2">Available Placeholders:</p>
                <div className="flex flex-wrap gap-2">
                  {getAvailableVariables().map((v) => (
                    <code
                      key={v}
                      className="px-2 py-1 bg-blue-100 dark:bg-blue-800 text-blue-800 dark:text-blue-200 rounded text-xs font-mono cursor-pointer hover:bg-blue-200 dark:hover:bg-blue-700"
                      onClick={() => {
                        const field = formData.bodyTemplate.includes('Subject') ? 'subjectTemplate' : 'bodyTemplate';
                        const current = formData[field as keyof typeof formData] as string;
                        const end = document.querySelector('textarea');
                        if (end) {
                          setFormData({
                            ...formData,
                            [field]: current + v,
                          });
                          setTimeout(() => end.focus(), 0);
                        }
                      }}
                    >
                      {v}
                    </code>
                  ))}
                </div>
              </div>
            )}

            <div className="md:col-span-2 flex items-center gap-2">
              <input
                type="checkbox"
                id="notifIsActive"
                checked={formData.isActive}
                onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
              />
              <label htmlFor="notifIsActive" className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Active
              </label>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 mt-6">
            <button
              onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
              disabled={saving}
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
      )}
    </>
  );
};

// ─── Main Page ─────────────────────────────────────────────────────

export const DocumentTemplatesAdminPage = () => {
  usePageTitle('Document Templates');
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'notifications' ? 'notifications' : 'documents';

  const switchTab = (tab: 'documents' | 'notifications') => {
    setSearchParams(tab === 'documents' ? {} : { tab });
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Templates</h2>
        </div>

        <div className="tabs">
          <button
            className={`tab${activeTab === 'documents' ? ' active' : ''}`}
            onClick={() => switchTab('documents')}
          >
            Document Templates
          </button>
          <button
            className={`tab${activeTab === 'notifications' ? ' active' : ''}`}
            onClick={() => switchTab('notifications')}
          >
            Notification Templates
          </button>
        </div>

        {activeTab === 'documents' && <DocumentTemplatesTab />}
        {activeTab === 'notifications' && <NotificationTemplatesTab />}
      </div>
    </Layout>
  );
};
