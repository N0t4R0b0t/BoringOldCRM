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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { usePageTitle } from '../hooks/usePageTitle';
import { AccessControlPanel } from '../components/AccessControlPanel';
import { apiClient } from '../api/apiClient';
import { useUiStore } from '../store/uiStore';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { PolicyInfoButton } from '../components/PolicyInfoButton';
import { normalizePaged } from '../utils/pagination';
import { CsvPreview, MarkdownPreview } from '../components/FilePreviewHelpers';
import '../styles/List.css';

interface TenantDocument {
  id: number;
  name: string;
  description?: string;
  mimeType?: string;
  sizeBytes?: number;
  contentType: string;
  tags?: string[];
  source?: string;
  linkedEntityType?: string;
  linkedEntityId?: number;
  createdAt: string;
  updatedAt: string;
}

type SortField = 'name' | 'sizeBytes' | 'contentType' | 'source' | 'createdAt';
type SortDir = 'asc' | 'desc';

function formatBytes(bytes?: number): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getFileIcon(mimeType?: string, contentType?: string): string {
  if (contentType === 'report') return '📋';
  if (contentType === 'slide_deck') return '📽️';
  if (contentType === 'csv' || mimeType === 'text/csv') return '📊';
  if (!mimeType) return '📎';
  if (mimeType.startsWith('image/')) return '🖼️';
  if (mimeType === 'application/pdf') return '📄';
  if (mimeType === 'application/json') return '{ }';
  if (mimeType.startsWith('text/html')) return '🌐';
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊';
  if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
  if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return '📽️';
  if (mimeType.startsWith('video/')) return '🎬';
  if (mimeType.startsWith('audio/')) return '🎵';
  if (mimeType === 'application/zip' || mimeType === 'application/x-zip-compressed') return '🗜️';
  if (mimeType.startsWith('text/')) return '📝';
  return '📎';
}

const SOURCE_LABELS: Record<string, string> = {
  user_upload: 'Uploaded',
  assistant_generated: 'AI Generated',
};

const CONTENT_TYPE_OPTIONS = [
  { value: '', label: 'All types' },
  { value: 'file', label: 'File' },
  { value: 'report', label: 'Report' },
  { value: 'slide_deck', label: 'Slide Deck' },
  { value: 'one_pager', label: 'One Pager' },
  { value: 'csv', label: 'CSV' },
  { value: 'html', label: 'HTML' },
  { value: 'markdown', label: 'Markdown' },
];

const SOURCE_OPTIONS = [
  { value: '', label: 'All sources' },
  { value: 'user_upload', label: 'Uploaded' },
  { value: 'assistant_generated', label: 'AI Generated' },
];

function SortIcon({ field, sortField, sortDir }: { field: SortField; sortField: SortField; sortDir: SortDir }) {
  if (field !== sortField) return <span className="text-gray-300 dark:text-gray-600 ml-1">⇅</span>;
  return <span className="text-blue-500 ml-1">{sortDir === 'asc' ? '↑' : '↓'}</span>;
}

export const DocumentsPage = () => {
  usePageTitle('Documents');
  const { dataRefreshToken } = useUiStore();
  const { evaluateRules } = usePolicyRulesStore();
  const [documents, setDocuments] = useState<TenantDocument[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();
  const [search, setSearch] = useState(() => searchParams.get('search') ?? '');
  const [contentTypeFilter, setContentTypeFilter] = useState('');
  const [sourceFilter, setSourceFilter] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [isDragOver, setIsDragOver] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<TenantDocument | null>(null);
  const [previewContent, setPreviewContent] = useState('');
  const [previewLoading, setPreviewLoading] = useState(false);
  const [renamingId, setRenamingId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const [policyWarnings, setPolicyWarnings] = useState<any[]>([]);
  const [showWarningsModal, setShowWarningsModal] = useState(false);
  const [pendingOperation, setPendingOperation] = useState<{ type: 'upload' | 'delete' | 'rename'; data: any } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const MAX_FILE_SIZE = 100 * 1024 * 1024;
  const PAGE_SIZE = 25;

  const fetchDocuments = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const response = await apiClient.listDocuments({
        page: page - 1,
        size: PAGE_SIZE,
        search: search || undefined,
        contentType: contentTypeFilter || undefined,
        source: sourceFilter || undefined,
      });
      const data = normalizePaged<TenantDocument>(response.data);
      setDocuments(data.content);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || data.content.length);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load documents');
    } finally {
      setIsLoading(false);
    }
  }, [page, search, contentTypeFilter, sourceFilter, dataRefreshToken]);

  useEffect(() => { fetchDocuments(); }, [fetchDocuments]);

  // Reset to page 1 when filters change
  useEffect(() => { setPage(1); }, [search, contentTypeFilter, sourceFilter]);

  const uploadFiles = async (files: File[]) => {
    const oversized = files.filter(f => f.size > MAX_FILE_SIZE);
    if (oversized.length > 0) {
      setUploadError(`File${oversized.length > 1 ? 's' : ''} too large: ${oversized.map(f => f.name).join(', ')}. Max 100 MB.`);
      return;
    }

    // Evaluate policy for first file as sample
    if (files.length > 0) {
      const firstFile = files[0];
      const payload = { name: firstFile.name, sizeBytes: firstFile.size, mimeType: firstFile.type };
      try {
        const policyResult = await evaluateRules('TenantDocument', 'CREATE', payload, undefined);

        if (policyResult.blocked) {
          setUploadError(`Operation blocked by policy: ${policyResult.violations.map((v: any) => v.message).join(', ')}`);
          return;
        }

        if (policyResult.warnings.length > 0) {
          setPolicyWarnings(policyResult.warnings);
          setPendingOperation({ type: 'upload', data: { files } });
          setShowWarningsModal(true);
          return;
        }
      } catch {
        // Continue with upload if policy check fails
      }
    }

    await executeUpload(files);
  };

  const executeUpload = async (files: File[]) => {
    setUploading(true);
    setUploadError('');
    try {
      for (const file of files) {
        await apiClient.uploadDocument(file, { name: file.name });
      }
      fetchDocuments();
    } catch (err: any) {
      setUploadError(err.response?.data?.message || err.message || 'Upload failed');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    if (files.length) uploadFiles(files);
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length) uploadFiles(files);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this document?')) return;

    // Evaluate policy
    try {
      const policyResult = await evaluateRules('TenantDocument', 'DELETE', {}, undefined);

      if (policyResult.blocked) {
        setError(`Operation blocked by policy: ${policyResult.violations.map((v: any) => v.message).join(', ')}`);
        return;
      }

      if (policyResult.warnings.length > 0) {
        setPolicyWarnings(policyResult.warnings);
        setPendingOperation({ type: 'delete', data: { id } });
        setShowWarningsModal(true);
        return;
      }
    } catch {
      // Continue with delete if policy check fails
    }

    await executeDelete(id);
  };

  const executeDelete = async (id: number) => {
    try {
      await apiClient.deleteDocument(id);
      fetchDocuments();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete');
    }
  };

  const handleDuplicate = async (id: number) => {
    try {
      await apiClient.duplicateDocument(id);
      fetchDocuments();
    } catch { /* ignore */ }
  };

  const handleDownload = async (doc: TenantDocument) => {
    try {
      const res = await apiClient.downloadDocument(doc.id);
      const { contentBase64, mimeType } = res.data;
      if (contentBase64) {
        const a = document.createElement('a');
        a.href = `data:${mimeType || 'application/octet-stream'};base64,${contentBase64}`;
        a.download = doc.name;
        a.click();
      }
    } catch { /* ignore */ }
  };

  const startRename = (doc: TenantDocument) => {
    setRenamingId(doc.id);
    setRenameValue(doc.name);
  };

  const commitRename = async (id: number) => {
    if (!renameValue.trim()) { setRenamingId(null); return; }

    const payload = { name: renameValue.trim() };

    // Evaluate policy
    try {
      const policyResult = await evaluateRules('TenantDocument', 'UPDATE', payload, undefined);

      if (policyResult.blocked) {
        setError(`Operation blocked by policy: ${policyResult.violations.map((v: any) => v.message).join(', ')}`);
        setRenamingId(null);
        return;
      }

      if (policyResult.warnings.length > 0) {
        setPolicyWarnings(policyResult.warnings);
        setPendingOperation({ type: 'rename', data: { id, newName: renameValue.trim() } });
        setShowWarningsModal(true);
        return;
      }
    } catch {
      // Continue with rename if policy check fails
    }

    await executeRename(id, renameValue.trim());
  };

  const executeRename = async (id: number, newName: string) => {
    try {
      await apiClient.renameDocument(id, newName);
      fetchDocuments();
    } catch { /* ignore */ }
    setRenamingId(null);
  };

  const handlePreview = async (doc: TenantDocument) => {
    setPreviewDoc(doc);
    setPreviewContent('');
    setPreviewLoading(true);
    try {
      const response = await apiClient.downloadDocument(doc.id);
      const { contentBase64, mimeType } = response.data;
      if (contentBase64) {
        const isText = mimeType?.startsWith('text/') || mimeType === 'application/json' || mimeType === 'application/xml';
        setPreviewContent(isText ? atob(contentBase64) : `data:${mimeType || 'application/octet-stream'};base64,${contentBase64}`);
      }
    } catch {
      setPreviewContent('');
    } finally {
      setPreviewLoading(false);
    }
  };

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const handleConfirmWarnings = async () => {
    setShowWarningsModal(false);
    if (pendingOperation) {
      if (pendingOperation.type === 'upload') {
        await executeUpload(pendingOperation.data.files);
      } else if (pendingOperation.type === 'delete') {
        await executeDelete(pendingOperation.data.id);
      } else if (pendingOperation.type === 'rename') {
        await executeRename(pendingOperation.data.id, pendingOperation.data.newName);
      }
      setPendingOperation(null);
    }
  };

  const handleCancelWarnings = () => {
    setShowWarningsModal(false);
    setPendingOperation(null);
    setPolicyWarnings([]);
  };

  // Client-side sort of current page
  const sorted = [...documents].sort((a, b) => {
    let av: any = a[sortField as keyof TenantDocument];
    let bv: any = b[sortField as keyof TenantDocument];
    if (av == null) av = '';
    if (bv == null) bv = '';
    if (typeof av === 'number' && typeof bv === 'number') return sortDir === 'asc' ? av - bv : bv - av;
    return sortDir === 'asc' ? String(av).localeCompare(String(bv)) : String(bv).localeCompare(String(av));
  });

  const allSelected = sorted.length > 0 && sorted.every((d) => selectedIds.has(d.id));
  const someSelected = !allSelected && sorted.some((d) => selectedIds.has(d.id));

  const toggleAll = () => {
    if (allSelected) {
      setSelectedIds((prev) => { const next = new Set(prev); sorted.forEach((d) => next.delete(d.id)); return next; });
    } else {
      setSelectedIds((prev) => new Set([...prev, ...sorted.map((d) => d.id)]));
    }
  };

  const toggleRow = (id: number) => {
    setSelectedIds((prev) => { const next = new Set(prev); if (next.has(id)) next.delete(id); else next.add(id); return next; });
  };

  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) return;
    if (!confirm(`Delete ${selectedIds.size} document${selectedIds.size > 1 ? 's' : ''}? This cannot be undone.`)) return;
    setBulkDeleting(true);
    try {
      await apiClient.bulkDeleteDocuments(Array.from(selectedIds));
      setSelectedIds(new Set());
      fetchDocuments();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete documents');
    } finally {
      setBulkDeleting(false);
    }
  };

  return (
    <Layout>
      <div className="list-page">
        {/* Header */}
        <div className="list-header">
          <div>
            <h1 className="page-title">Documents</h1>
            <span className="total-count">{totalElements} total</span>
          </div>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <PolicyInfoButton entityType="TenantDocument" />
            <button
              className="btn btn-primary"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
            >
              {uploading ? 'Uploading...' : '+ Upload'}
            </button>
          </div>
        </div>

        <input ref={fileInputRef} type="file" multiple className="hidden" onChange={handleFileInput} />

        {/* Drag & Drop zone */}
        <div
          onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
          onDragLeave={() => setIsDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`mb-4 border-2 border-dashed rounded-lg py-3 px-4 text-center cursor-pointer transition-colors text-sm ${
            isDragOver
              ? 'border-blue-400 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400'
              : 'border-gray-200 dark:border-gray-700 text-gray-400 dark:text-gray-500 hover:border-blue-300 hover:text-blue-500'
          }`}
        >
          {uploading
            ? <span className="flex items-center justify-center gap-2"><span className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin inline-block" /> Uploading…</span>
            : 'Drop files here or click to upload'}
        </div>

        {uploadError && <div className="error-message mb-3">{uploadError}</div>}

        {/* Filters */}
        <div className="list-controls mb-4 flex flex-wrap gap-2 items-center">
          <input
            type="text"
            className="search-input flex-1 min-w-[200px]"
            placeholder="Search by name…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select
            className="form-select text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100"
            value={contentTypeFilter}
            onChange={(e) => setContentTypeFilter(e.target.value)}
          >
            {CONTENT_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          <select
            className="form-select text-sm border border-gray-300 dark:border-gray-600 rounded-md px-3 py-1.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100"
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
          >
            {SOURCE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          {(search || contentTypeFilter || sourceFilter) && (
            <button
              className="text-sm text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
              onClick={() => { setSearch(''); setContentTypeFilter(''); setSourceFilter(''); }}
            >
              Clear
            </button>
          )}
        </div>

        {error && <div className="error-message mb-3">{error}</div>}

        {/* Table */}
        {isLoading ? (
          <div className="loading-state">Loading…</div>
        ) : sorted.length === 0 ? (
          <div className="empty-state">
            <p>{search || contentTypeFilter || sourceFilter ? 'No documents match the current filters.' : 'No documents yet.'}</p>
            {!search && !contentTypeFilter && !sourceFilter && (
              <button className="btn btn-primary mt-2" onClick={() => fileInputRef.current?.click()}>
                Upload your first document
              </button>
            )}
          </div>
        ) : (
          <>
          {selectedIds.size > 0 && (
          <div className="mb-3 px-4 py-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg flex items-center justify-between">
            <span className="text-sm text-red-700 dark:text-red-300">{selectedIds.size} document{selectedIds.size > 1 ? 's' : ''} selected</span>
            <button
              onClick={handleBulkDelete}
              disabled={bulkDeleting}
              className="px-3 py-1.5 text-xs font-medium rounded-md bg-red-600 hover:bg-red-700 text-white disabled:opacity-50 transition-colors"
            >
              {bulkDeleting ? 'Deleting…' : 'Delete selected'}
            </button>
          </div>
        )}

        <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
                <tr>
                  <th className="w-10 px-3 py-3">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      ref={(el) => { if (el) el.indeterminate = someSelected; }}
                      onChange={toggleAll}
                      className="rounded"
                    />
                  </th>
                  <th
                    className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 cursor-pointer select-none hover:text-gray-900 dark:hover:text-gray-100 whitespace-nowrap"
                    onClick={() => toggleSort('name')}
                  >
                    Name <SortIcon field="name" sortField={sortField} sortDir={sortDir} />
                  </th>
                  <th
                    className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 cursor-pointer select-none hover:text-gray-900 dark:hover:text-gray-100 whitespace-nowrap"
                    onClick={() => toggleSort('contentType')}
                  >
                    Type <SortIcon field="contentType" sortField={sortField} sortDir={sortDir} />
                  </th>
                  <th
                    className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 cursor-pointer select-none hover:text-gray-900 dark:hover:text-gray-100 whitespace-nowrap"
                    onClick={() => toggleSort('sizeBytes')}
                  >
                    Size <SortIcon field="sizeBytes" sortField={sortField} sortDir={sortDir} />
                  </th>
                  <th
                    className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 cursor-pointer select-none hover:text-gray-900 dark:hover:text-gray-100 whitespace-nowrap"
                    onClick={() => toggleSort('source')}
                  >
                    Source <SortIcon field="source" sortField={sortField} sortDir={sortDir} />
                  </th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 whitespace-nowrap">
                    Linked To
                  </th>
                  <th
                    className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-300 cursor-pointer select-none hover:text-gray-900 dark:hover:text-gray-100 whitespace-nowrap"
                    onClick={() => toggleSort('createdAt')}
                  >
                    Created <SortIcon field="createdAt" sortField={sortField} sortDir={sortDir} />
                  </th>
                  <th className="px-4 py-3 text-right font-medium text-gray-600 dark:text-gray-300 whitespace-nowrap">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700/50">
                {sorted.map((doc) => (
                  <tr
                    key={doc.id}
                    className={`transition-colors ${selectedIds.has(doc.id) ? 'bg-blue-50 dark:bg-blue-900/20' : 'bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800/60'}`}
                  >
                    <td className="w-10 px-3 py-3" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selectedIds.has(doc.id)}
                        onChange={() => toggleRow(doc.id)}
                        className="rounded"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2 min-w-0">
                        <span className="text-lg flex-shrink-0" title={doc.mimeType}>
                          {getFileIcon(doc.mimeType, doc.contentType)}
                        </span>
                        {renamingId === doc.id ? (
                          <input
                            autoFocus
                            value={renameValue}
                            onChange={(e) => setRenameValue(e.target.value)}
                            onBlur={() => commitRename(doc.id)}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') commitRename(doc.id);
                              if (e.key === 'Escape') setRenamingId(null);
                            }}
                            className="flex-1 text-sm border border-blue-400 rounded px-1.5 py-0.5 bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                            onClick={(e) => e.stopPropagation()}
                          />
                        ) : (
                          <button
                            type="button"
                            onClick={() => handlePreview(doc)}
                            className="text-sm font-medium text-gray-900 dark:text-gray-100 hover:text-blue-600 dark:hover:text-blue-400 text-left break-words"
                            title={doc.name}
                          >
                            {doc.name}
                          </button>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        {doc.contentType || '—'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap text-xs">
                      {formatBytes(doc.sizeBytes)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      {doc.source ? (
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          doc.source === 'assistant_generated'
                            ? 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300'
                            : 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
                        }`}>
                          {SOURCE_LABELS[doc.source] ?? doc.source}
                        </span>
                      ) : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                      {doc.linkedEntityType ? `${doc.linkedEntityType} #${doc.linkedEntityId}` : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">
                      {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          title="Preview"
                          onClick={() => handlePreview(doc)}
                          className="p-1.5 rounded text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          👁
                        </button>
                        <button
                          type="button"
                          title="Rename"
                          onClick={() => startRename(doc)}
                          className="p-1.5 rounded text-gray-400 hover:text-yellow-600 dark:hover:text-yellow-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-xs font-bold"
                        >
                          ✏️
                        </button>
                        <button
                          type="button"
                          title="Download"
                          onClick={() => handleDownload(doc)}
                          className="p-1.5 rounded text-gray-400 hover:text-green-600 dark:hover:text-green-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          ⬇
                        </button>
                        <button
                          type="button"
                          title="Duplicate"
                          onClick={() => handleDuplicate(doc.id)}
                          className="p-1.5 rounded text-gray-400 hover:text-purple-600 dark:hover:text-purple-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          ⧉
                        </button>
                        <button
                          type="button"
                          title="Delete"
                          onClick={() => handleDelete(doc.id)}
                          className="p-1.5 rounded text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          🗑
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          </>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="pagination mt-4">
            <button className="btn btn-secondary" disabled={page <= 1} onClick={() => setPage(p => p - 1)}>
              Previous
            </button>
            <span className="page-info">Page {page} of {totalPages}</span>
            <button className="btn btn-secondary" disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}>
              Next
            </button>
          </div>
        )}

        {/* Preview Modal */}
        {previewDoc && (
          <div
            className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
            onClick={() => setPreviewDoc(null)}
          >
            <div
              className="bg-white dark:bg-gray-900 rounded-xl shadow-2xl w-full max-w-5xl max-h-[92vh] flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal header */}
              <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 dark:border-gray-700 flex-shrink-0">
                <div className="flex items-center gap-3 min-w-0">
                  <span className="text-2xl">{getFileIcon(previewDoc.mimeType, previewDoc.contentType)}</span>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{previewDoc.name}</p>
                    <p className="text-xs text-gray-400 dark:text-gray-500">
                      {previewDoc.mimeType && <>{previewDoc.mimeType} · </>}
                      {formatBytes(previewDoc.sizeBytes)}
                      {previewDoc.linkedEntityType && <> · {previewDoc.linkedEntityType} #{previewDoc.linkedEntityId}</>}
                      {previewDoc.source && <> · {SOURCE_LABELS[previewDoc.source] ?? previewDoc.source}</>}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 ml-4 flex-shrink-0">
                  <button
                    type="button"
                    title="Download"
                    onClick={() => handleDownload(previewDoc)}
                    className="p-1.5 rounded text-gray-400 hover:text-green-600 dark:hover:text-green-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    ⬇
                  </button>
                  <button
                    type="button"
                    onClick={() => setPreviewDoc(null)}
                    className="text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 text-2xl leading-none"
                  >
                    ×
                  </button>
                </div>
              </div>

              {/* Modal body */}
              <div className="flex-1 overflow-auto p-4">
                <AccessControlPanel entityType="Document" entityId={previewDoc.id} />
                {previewLoading ? (
                  <div className="flex items-center justify-center py-16 text-sm text-gray-500 dark:text-gray-400 gap-2">
                    <span className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                    Loading preview…
                  </div>
                ) : previewContent ? (
                  previewDoc.mimeType?.startsWith('image/') ? (
                    <img src={previewContent} alt={previewDoc.name} className="max-w-full max-h-[75vh] object-contain mx-auto" />
                  ) : previewDoc.mimeType === 'application/pdf' ? (
                    <iframe src={previewContent} className="w-full h-[75vh]" title={previewDoc.name} />
                  ) : previewDoc.mimeType?.startsWith('video/') ? (
                    <video src={previewContent} controls className="w-full max-h-[75vh] rounded">Your browser does not support video.</video>
                  ) : previewDoc.mimeType?.startsWith('audio/') ? (
                    <div className="flex flex-col items-center gap-6 py-12">
                      <span className="text-8xl">🎵</span>
                      <p className="text-sm text-gray-600 dark:text-gray-300">{previewDoc.name}</p>
                      <audio src={previewContent} controls className="w-full max-w-lg" />
                    </div>
                  ) : previewDoc.mimeType?.startsWith('text/html') ? (
                    <iframe srcDoc={previewContent} sandbox="allow-scripts" className="w-full h-[75vh] border rounded" title={previewDoc.name} />
                  ) : previewDoc.mimeType === 'text/csv' ? (
                    <CsvPreview content={previewContent} />
                  ) : previewDoc.mimeType === 'text/markdown' || previewDoc.name.endsWith('.md') ? (
                    <MarkdownPreview content={previewContent} />
                  ) : previewDoc.mimeType?.startsWith('text/') || previewDoc.mimeType === 'application/json' || previewDoc.mimeType === 'application/xml' ? (
                    <pre className="bg-gray-50 dark:bg-gray-800 p-4 rounded text-sm overflow-auto max-h-[75vh] whitespace-pre-wrap font-mono">
                      {previewContent}
                    </pre>
                  ) : (
                    <div className="text-center py-12">
                      <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">No preview available for this file type.</p>
                      <a href={previewContent} download={previewDoc.name} className="text-blue-600 dark:text-blue-400 hover:underline text-sm">
                        Download {previewDoc.name}
                      </a>
                    </div>
                  )
                ) : (
                  <div className="text-center py-12 text-sm text-gray-500 dark:text-gray-400">No preview available.</div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Policy Warnings Modal */}
        {showWarningsModal && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
            justifyContent: 'center', alignItems: 'center', zIndex: 1000
          }}>
            <div style={{
              backgroundColor: 'white', borderRadius: '8px', padding: '24px',
              maxWidth: '500px', maxHeight: '80vh', overflow: 'auto',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
            }} className="dark:bg-gray-800">
              <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-gray-100">Policy Warnings</h3>
              <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
                The following policy rules generated warnings. Do you want to proceed anyway?
              </p>
              <div style={{ marginBottom: '16px' }}>
                {policyWarnings.map((w: any) => (
                  <div key={w.ruleId} style={{
                    padding: '12px', marginBottom: '8px',
                    backgroundColor: '#fef3c7', border: '1px solid #f59e0b',
                    borderRadius: '4px', color: '#92400e'
                  }} className="dark:bg-amber-900 dark:border-amber-700 dark:text-amber-200">
                    <strong>{w.ruleName}:</strong> {w.message}
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={handleCancelWarnings}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleConfirmWarnings}
                  className="btn btn-primary"
                >
                  Proceed Anyway
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};
