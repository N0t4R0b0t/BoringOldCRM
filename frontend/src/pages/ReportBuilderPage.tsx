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
import { useCallback, useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { apiClient } from '../api/apiClient';
import { useReportBuilderStore } from '../store/reportBuilderStore';
import { useUiStore } from '../store/uiStore';
import { Layout } from '../components/Layout';
import { useAuthStore } from '../store/authStore';
import { Navigate } from 'react-router-dom';

const ENTITY_TYPES = ['Customer', 'Contact', 'Opportunity', 'Activity', 'CustomRecord', 'Order', 'Invoice'];

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function ReportBuilderPage() {
  const { tenantSettings } = useAuthStore();
  if (tenantSettings?.reportBuilderEnabled !== true) {
    return <Navigate to="/dashboard" replace />;
  }

  const {
    entityType, entityId, reportType, templateId, title,
    layout, accentColor, logoPlacement, includeFields, excludeFields, styleOverrides,
    previewContent, previewMimeType, previewLoading, previewError,
    saveTemplateModalOpen, saveTemplateName, saveTemplateDescription,
    setEntityType, setEntityId, setReportType, setTemplateId, setTitle,
    setLayout, setAccentColor, setLogoPlacement, setIncludeFields, setExcludeFields,
    setStyleOverrides,
    setPreview, setPreviewLoading, setPreviewError,
    setSaveTemplateModalOpen, setSaveTemplateName, setSaveTemplateDescription,
    buildStyleJson,
  } = useReportBuilderStore();

  const { triggerDataRefresh } = useUiStore();

  // Save Report state
  const [saving, setSaving] = useState(false);
  const [savedDocId, setSavedDocId] = useState<number | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Entity search
  const [entitySearch, setEntitySearch] = useState('');
  const [entityResults, setEntityResults] = useState<{ id: number; label: string }[]>([]);
  const [entityDropdownOpen, setEntityDropdownOpen] = useState(false);

  // Templates list
  const [templates, setTemplates] = useState<{ id: number; name: string; templateType: string; styleJson?: string }[]>([]);

  // Field text inputs
  const [includeFieldsText, setIncludeFieldsText] = useState('');
  const [excludeFieldsText, setExcludeFieldsText] = useState('');

  const [saveTemplateError, setSaveTemplateError] = useState<string | null>(null);
  const [saveTemplateSaving, setSaveTemplateSaving] = useState(false);

  const debouncedAccentColor = useDebounce(accentColor, 600);

  // Load templates when report type changes
  useEffect(() => {
    apiClient.listDocumentTemplates({ templateType: reportType, size: 50 })
      .then(r => setTemplates((r.data as { content: typeof templates }).content ?? []))
      .catch(() => {});
  }, [reportType]);

  // ── Preview trigger ──────────────────────────────────────────────────────
  const triggerPreview = useCallback(async () => {
    if (!entityType) return;
    setPreviewLoading(true);
    setPreviewError(null);
    try {
      const resp = await apiClient.previewReport({
        entityType,
        entityId,
        reportType,
        styleJson: buildStyleJson(),
        templateId,
        title: title || undefined,
      });
      const { content, mimeType } = resp.data;
      if (!content) {
        setPreviewError('No data found. Try selecting a specific record or check that records exist.');
        return;
      }
      setPreview(content, mimeType);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Preview generation failed';
      setPreviewError(msg);
    }
  }, [entityType, entityId, reportType, templateId, title, buildStyleJson, setPreview, setPreviewLoading, setPreviewError]);

  // Auto-refresh on layout or logo change (only when preview already exists)
  useEffect(() => {
    if (previewContent) triggerPreview();
  }, [layout, logoPlacement]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (previewContent) triggerPreview();
  }, [debouncedAccentColor]); // eslint-disable-line react-hooks/exhaustive-deps

  // Refresh preview when the assistant mutates fields/title/reportType/style via reportBuilderUpdate
  useEffect(() => {
    if (previewContent) triggerPreview();
  }, [includeFields, excludeFields, reportType, styleOverrides]); // eslint-disable-line react-hooks/exhaustive-deps

  // Sync the include/exclude text inputs when the store changes (e.g. from assistant)
  useEffect(() => {
    setIncludeFieldsText(includeFields.join(', '));
  }, [includeFields]);
  useEffect(() => {
    setExcludeFieldsText(excludeFields.join(', '));
  }, [excludeFields]);

  // ── Entity search ────────────────────────────────────────────────────────
  useEffect(() => {
    if (!entitySearch.trim()) { setEntityResults([]); return; }
    const t = setTimeout(async () => {
      try {
        let items: { id: number; label: string }[] = [];
        if (entityType === 'Customer') {
          const r = await apiClient.searchCustomers(entitySearch);
          items = (r.data.content ?? []).map((c: { id: number; name: string }) => ({ id: c.id, label: c.name }));
        } else if (entityType === 'Contact') {
          const r = await apiClient.searchContacts(entitySearch);
          items = (r.data.content ?? []).map((c: { id: number; name: string }) => ({ id: c.id, label: c.name }));
        } else if (entityType === 'Opportunity') {
          const r = await apiClient.searchOpportunities(entitySearch);
          items = (r.data.content ?? []).map((o: { id: number; name: string }) => ({ id: o.id, label: o.name }));
        } else if (entityType === 'Activity') {
          const r = await apiClient.searchActivities(entitySearch);
          items = (r.data.content ?? []).map((a: { id: number; subject: string }) => ({ id: a.id, label: a.subject }));
        } else if (entityType === 'CustomRecord') {
          const r = await apiClient.searchCustomRecords(entitySearch);
          items = (r.data.content ?? []).map((a: { id: number; name: string }) => ({ id: a.id, label: a.name }));
        }
        setEntityResults(items);
        setEntityDropdownOpen(items.length > 0);
      } catch { setEntityResults([]); }
    }, 300);
    return () => clearTimeout(t);
  }, [entitySearch, entityType]);

  // ── Save as Template ─────────────────────────────────────────────────────
  const handleSaveTemplate = async () => {
    setSaveTemplateError(null);
    setSaveTemplateSaving(true);
    try {
      await apiClient.createDocumentTemplate({
        name: saveTemplateName,
        description: saveTemplateDescription || undefined,
        templateType: reportType,
        styleJson: buildStyleJson(),
        isDefault: false,
      });
      setSaveTemplateModalOpen(false);
      setSaveTemplateName('');
      setSaveTemplateDescription('');
      const r = await apiClient.listDocumentTemplates({ templateType: reportType, size: 50 });
      setTemplates((r.data as { content: typeof templates }).content ?? []);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setSaveTemplateError(status === 403 ? 'Admin role required to save templates.' : 'Failed to save template.');
    } finally {
      setSaveTemplateSaving(false);
    }
  };

  // ── Save Report — calls assistant in auto mode to generate + persist ─────
  const handleSaveReport = async () => {
    if (!entityType) return;
    setSaving(true);
    setSaveError(null);
    setSavedDocId(null);
    try {
      const entityDesc = entityId ? `${entityType} with id ${entityId}` : `all ${entityType}s`;
      const tool = reportType === 'slide_deck' ? 'generateSlideDeck' : 'generateOnePager';
      const args = {
        entityType,
        entityId: entityId ?? null,
        title: title || undefined,
        styleJson: buildStyleJson(),
      };
      const prompt = `Call the tool ${tool} with these exact arguments to save a report for ${entityDesc}: ${JSON.stringify(args)}. After saving, respond with ONLY the download link in the format [Download](/documents/{id}/download).`;
      const resp = await apiClient.sendChatMessage(
        prompt,
        undefined,
        undefined,
        crypto.randomUUID(),
        'auto',
        undefined,
        'report_builder'
      );
      const data = resp.data as { message?: string; history?: { role: string; content: string }[] };
      const textToScan = data.message ?? (data.history?.[data.history.length - 1]?.content ?? '');
      const match = textToScan.match(/\/documents\/(\d+)\/download/);
      if (match) {
        const docId = parseInt(match[1], 10);
        setSavedDocId(docId);
        triggerDataRefresh();
      } else {
        setSaveError('Report was not saved — the assistant did not return a document link.');
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Save failed';
      setSaveError(msg);
    } finally {
      setSaving(false);
    }
  };

  const PRESET_LAYOUTS: { key: typeof layout; label: string; bg: string; text: string }[] = [
    { key: 'dark',      label: 'Dark',      bg: '#1a1a2e', text: '#eee' },
    { key: 'light',     label: 'Light',     bg: '#f5f5f5', text: '#222' },
    { key: 'corporate', label: 'Corp',      bg: '#1c2840', text: '#dde4f0' },
    { key: 'minimal',   label: 'Minimal',   bg: '#ffffff', text: '#111' },
  ];

  const PRESET_ACCENTS: Record<typeof layout, string> = {
    dark: '#533483', light: '#3355cc', corporate: '#2e6da4', minimal: '#111',
  };

  return (
    <Layout>
    <div className="flex gap-5 flex-1 min-h-0">

      {/* ── Preview area (main content) ─────────────────────────────────── */}
      <div className="flex-1 flex flex-col min-h-0 min-w-0">

        {/* Toolbar */}
        <div className="flex items-center gap-2 mb-3 flex-shrink-0">
          <button
            onClick={triggerPreview}
            disabled={previewLoading || !entityType}
            className="px-4 py-1.5 text-sm font-medium bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {previewLoading ? 'Generating…' : 'Generate Preview'}
          </button>
          <button
            onClick={handleSaveReport}
            disabled={!entityType || saving}
            className="px-4 py-1.5 text-sm font-medium bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            title="Generate and save this report as a document"
          >
            {saving ? 'Saving…' : 'Save Report'}
          </button>
          {savedDocId && (
            <a
              href={`/documents/${savedDocId}/download`}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-green-600 dark:text-green-400 hover:underline ml-1"
            >
              ✓ Saved — download
            </a>
          )}
          {saveError && (
            <span className="text-xs text-red-600 dark:text-red-400 ml-1">{saveError}</span>
          )}
          <button
            onClick={() => { setSaveTemplateModalOpen(true); setSaveTemplateName(title || entityType + ' Template'); }}
            disabled={!previewContent}
            className="px-4 py-1.5 text-sm font-medium bg-gray-600 text-white rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Save as Template
          </button>
          {previewError && (
            <span className="text-xs text-red-600 dark:text-red-400 ml-1">{previewError}</span>
          )}
          {previewContent && !previewLoading && (
            <span className="text-xs text-gray-400 dark:text-gray-500 ml-auto">
              {previewMimeType === 'text/html' ? 'Slide deck' : 'One-pager'} · use the assistant below to refine
            </span>
          )}
        </div>

        {/* Preview panel */}
        <div className="flex-1 min-h-0 rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 relative">

          {previewLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-50 dark:bg-gray-900 z-10">
              <div className="flex flex-col items-center gap-3">
                <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                <p className="text-sm text-gray-500 dark:text-gray-400">Generating preview…</p>
              </div>
            </div>
          )}

          {!previewContent && !previewLoading && (
            <div className="flex items-center justify-center h-full text-center p-10">
              <div>
                <div className="text-5xl mb-4 opacity-20">📊</div>
                <p className="text-gray-500 dark:text-gray-400 text-sm font-medium">Configure on the right, then click <strong>Generate Preview</strong>.</p>
                <p className="text-gray-400 dark:text-gray-500 text-xs mt-2">Or describe what you want in the assistant below.</p>
              </div>
            </div>
          )}

          {previewContent && previewMimeType === 'text/html' && (
            <iframe
              key={previewContent.length}
              srcDoc={previewContent}
              sandbox="allow-scripts"
              className="w-full h-full border-0"
              title="Report Preview"
            />
          )}

          {previewContent && previewMimeType === 'text/markdown' && (
            <div className="overflow-y-auto h-full p-8 bg-white dark:bg-gray-800">
              <div className="max-w-3xl mx-auto prose prose-sm dark:prose-invert">
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{previewContent}</ReactMarkdown>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Config panel (right side) ───────────────────────────────────── */}
      <div className="w-72 shrink-0 overflow-y-auto space-y-5 pb-4">

        <div>
          <h2 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">Configuration</h2>

          {/* Entity type */}
          <div className="space-y-3">
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Entity type</label>
              <select
                value={entityType}
                onChange={e => { setEntityType(e.target.value); setEntitySearch(''); }}
                className="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {ENTITY_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>

            {/* Entity search */}
            <div className="relative">
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                Specific record <span className="font-normal text-gray-400">(optional)</span>
              </label>
              <input
                type="text"
                value={entitySearch}
                onChange={e => { setEntitySearch(e.target.value); if (!e.target.value) setEntityId(null, ''); }}
                onFocus={() => entityResults.length > 0 && setEntityDropdownOpen(true)}
                onBlur={() => setTimeout(() => setEntityDropdownOpen(false), 150)}
                placeholder={`Search ${entityType}s…`}
                className="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {entityDropdownOpen && entityResults.length > 0 && (
                <div className="absolute z-20 w-full mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-md shadow-lg max-h-40 overflow-y-auto">
                  {entityResults.map(r => (
                    <button
                      key={r.id}
                      className="w-full text-left px-3 py-1.5 text-sm hover:bg-blue-50 dark:hover:bg-gray-700 text-gray-900 dark:text-gray-100"
                      onMouseDown={() => { setEntityId(r.id, r.label); setEntitySearch(r.label); setEntityDropdownOpen(false); }}
                    >
                      {r.label} <span className="text-xs text-gray-400">#{r.id}</span>
                    </button>
                  ))}
                </div>
              )}
              {entityId && (
                <p className="mt-1 text-xs text-blue-600 dark:text-blue-400">Selected: {entitySearch}</p>
              )}
            </div>

            {/* Report type */}
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Report type</label>
              <div className="flex gap-1.5">
                {(['slide_deck', 'one_pager'] as const).map(rt => (
                  <button
                    key={rt}
                    onClick={() => setReportType(rt)}
                    className={`flex-1 text-xs py-1.5 rounded-md border transition-colors ${
                      reportType === rt
                        ? 'bg-blue-600 border-blue-600 text-white'
                        : 'border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                    }`}
                  >
                    {rt === 'slide_deck' ? 'Slide Deck' : 'One-Pager'}
                  </button>
                ))}
              </div>
            </div>

            {/* Title */}
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Title <span className="font-normal text-gray-400">(optional)</span></label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="Auto-generated"
                className="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Template */}
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Template</label>
              <select
                value={templateId ?? ''}
                onChange={e => {
                  const id = e.target.value ? Number(e.target.value) : null;
                  setTemplateId(id);
                  if (id) {
                    const tmpl = templates.find(t => t.id === id);
                    if (tmpl?.styleJson) {
                      try {
                        const s = JSON.parse(tmpl.styleJson);
                        // Restore explicit UI-controlled fields
                        if (s.layout) setLayout(s.layout);
                        if (s.accentColor) setAccentColor(s.accentColor);
                        if (s.logoPlacement) setLogoPlacement(s.logoPlacement);
                        if (s.title) setTitle(s.title);
                        if (s.reportType === 'slide_deck' || s.reportType === 'one_pager') setReportType(s.reportType);
                        if (s.entityType) setEntityType(s.entityType);
                        if (Array.isArray(s.includeFields)) setIncludeFields(s.includeFields);
                        if (Array.isArray(s.excludeFields)) setExcludeFields(s.excludeFields);
                        // Restore all other style overrides (backgroundColor, slideBackground,
                        // textColor, h1Color, h2Color, fontFamily, customCss, etc.)
                        const knownKeys = new Set(['layout','accentColor','logoPlacement','title','reportType','entityType','includeFields','excludeFields']);
                        const overrides: Record<string, unknown> = {};
                        for (const [k, v] of Object.entries(s)) {
                          if (!knownKeys.has(k)) overrides[k] = v;
                        }
                        setStyleOverrides(overrides);
                      } catch { /* ignore malformed styleJson */ }
                    }
                  }
                }}
                className="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">No template</option>
                {templates.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </div>
          </div>
        </div>

        {/* Style section — slide deck only */}
        {reportType === 'slide_deck' && (
          <div>
            <h2 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">Style</h2>
            <div className="space-y-3">

              {/* Layout presets */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5">Layout</label>
                <div className="grid grid-cols-4 gap-1">
                  {PRESET_LAYOUTS.map(({ key, label, bg, text }) => (
                    <button
                      key={key}
                      onClick={() => { setLayout(key); setAccentColor(PRESET_ACCENTS[key]); }}
                      title={label}
                      className={`py-2 rounded-md border-2 text-xs font-medium transition-all ${
                        layout === key ? 'border-blue-500 ring-1 ring-blue-400' : 'border-transparent hover:border-gray-300 dark:hover:border-gray-500'
                      }`}
                      style={{ backgroundColor: bg, color: text }}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Accent color */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Accent color</label>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={accentColor}
                    onChange={e => setAccentColor(e.target.value)}
                    className="w-9 h-8 rounded border border-gray-300 dark:border-gray-600 cursor-pointer p-0.5"
                  />
                  <span className="text-xs font-mono text-gray-500 dark:text-gray-400">{accentColor}</span>
                </div>
              </div>

              {/* Logo placement */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Logo</label>
                <select
                  value={logoPlacement}
                  onChange={e => setLogoPlacement(e.target.value as typeof logoPlacement)}
                  className="w-full text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="none">None</option>
                  <option value="title">Title slide</option>
                  <option value="header">Every slide (header)</option>
                  <option value="footer">Every slide (footer)</option>
                </select>
              </div>

              {/* Include / exclude fields */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Include fields <span className="font-normal text-gray-400">(comma-separated)</span></label>
                <input
                  type="text"
                  value={includeFieldsText}
                  onChange={e => setIncludeFieldsText(e.target.value)}
                  onBlur={() => setIncludeFields(includeFieldsText.split(',').map(s => s.trim()).filter(Boolean))}
                  placeholder="e.g. stage,value — blank = all"
                  className="w-full text-xs border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Exclude fields <span className="font-normal text-gray-400">(comma-separated)</span></label>
                <input
                  type="text"
                  value={excludeFieldsText}
                  onChange={e => setExcludeFieldsText(e.target.value)}
                  onBlur={() => setExcludeFields(excludeFieldsText.split(',').map(s => s.trim()).filter(Boolean))}
                  placeholder="e.g. notes,serialNumber"
                  className="w-full text-xs border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>
        )}

        {/* Tip */}
        <p className="text-xs text-gray-400 dark:text-gray-500 leading-relaxed">
          Use the assistant below to refine the preview in natural language — e.g. "remove the notes field" or "switch to light theme".
        </p>
      </div>

      {/* ── Save as Template modal ──────────────────────────────────────── */}
      {saveTemplateModalOpen && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setSaveTemplateModalOpen(false)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Save as Template</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Name</label>
                <input
                  type="text"
                  value={saveTemplateName}
                  onChange={e => setSaveTemplateName(e.target.value)}
                  autoFocus
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description <span className="text-gray-400 font-normal">(optional)</span></label>
                <textarea
                  value={saveTemplateDescription}
                  onChange={e => setSaveTemplateDescription(e.target.value)}
                  rows={2}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>
              {saveTemplateError && <p className="text-sm text-red-600 dark:text-red-400">{saveTemplateError}</p>}
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setSaveTemplateModalOpen(false)} className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md">Cancel</button>
              <button
                onClick={handleSaveTemplate}
                disabled={!saveTemplateName.trim() || saveTemplateSaving}
                className="px-4 py-2 text-sm font-medium bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saveTemplateSaving ? 'Saving…' : 'Save Template'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
    </Layout>
  );
}
