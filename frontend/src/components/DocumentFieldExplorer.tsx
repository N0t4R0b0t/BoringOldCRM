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
import { apiClient } from '../api/apiClient';
import { CsvPreview, MarkdownPreview } from './FilePreviewHelpers';

interface DocumentRef {
  id: number;
  name: string;
  mimeType?: string;
  sizeBytes?: number;
  createdAt?: string;
}

interface DocumentFieldExplorerProps {
  entityType: string;
  entityId: number | undefined;
  fieldKey: string;
  multiple: boolean;
  label: string;
}

function getFileIcon(mimeType?: string): string {
  if (!mimeType) return '📎';
  if (mimeType.startsWith('image/')) return '🖼️';
  if (mimeType === 'application/pdf') return '📄';
  if (mimeType === 'application/json') return '{ }';
  if (mimeType.startsWith('text/html')) return '🌐';
  if (mimeType === 'text/csv' || mimeType === 'application/vnd.ms-excel') return '📊';
  if (mimeType.startsWith('application/vnd.openxmlformats') || mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊';
  if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
  if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return '📽️';
  if (mimeType.startsWith('video/')) return '🎬';
  if (mimeType.startsWith('audio/')) return '🎵';
  if (mimeType === 'application/zip' || mimeType === 'application/x-zip-compressed') return '🗜️';
  if (mimeType.startsWith('text/')) return '📝';
  return '📎';
}

function formatSize(bytes?: number): string {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export const DocumentFieldExplorer: React.FC<DocumentFieldExplorerProps> = ({
  entityType,
  entityId,
  fieldKey,
  multiple,
  label,
}) => {
  const [docs, setDocs] = useState<DocumentRef[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [previewDoc, setPreviewDoc] = useState<DocumentRef | null>(null);
  const [previewContent, setPreviewContent] = useState('');
  const [previewLoading, setPreviewLoading] = useState(false);
  const [renamingId, setRenamingId] = useState<number | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchDocs = useCallback(async () => {
    if (entityId == null) return;
    try {
      const res = await apiClient.listDocuments({
        linkedEntityType: entityType,
        linkedEntityId: entityId,
        linkedFieldKey: fieldKey,
        size: 100,
      });
      const content = res.data?.content ?? res.data ?? [];
      setDocs(Array.isArray(content) ? content : []);
    } catch {
      setDocs([]);
    }
  }, [entityType, entityId, fieldKey]);

  useEffect(() => {
    fetchDocs();
  }, [fetchDocs]);

  const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

  const handleFiles = async (files: File[]) => {
    if (!entityId) return;
    const oversized = files.filter(f => f.size > MAX_FILE_SIZE);
    if (oversized.length > 0) {
      setUploadError(`File${oversized.length > 1 ? 's' : ''} too large: ${oversized.map(f => f.name).join(', ')}. Maximum size is 100 MB.`);
      return;
    }
    setUploading(true);
    setUploadError('');
    try {
      for (const file of files) {
        await apiClient.uploadDocument(file, {
          name: file.name,
          linkedEntityType: entityType,
          linkedEntityId: entityId,
          linkedFieldKey: fieldKey,
        });
      }
      await fetchDocs();
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'Upload failed';
      setUploadError(msg);
    } finally {
      setUploading(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => setIsDragOver(false);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) handleFiles(files);
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    if (files.length > 0) handleFiles(files);
    e.target.value = '';
  };

  const startRename = (doc: DocumentRef) => {
    setRenamingId(doc.id);
    setRenameValue(doc.name);
  };

  const commitRename = async (id: number) => {
    if (!renameValue.trim()) {
      setRenamingId(null);
      return;
    }
    try {
      await apiClient.renameDocument(id, renameValue.trim());
      await fetchDocs();
    } catch {
      // silently ignore
    } finally {
      setRenamingId(null);
    }
  };

  const handleRenameKeyDown = (e: React.KeyboardEvent, id: number) => {
    if (e.key === 'Enter') commitRename(id);
    if (e.key === 'Escape') setRenamingId(null);
  };

  const handlePreview = async (doc: DocumentRef) => {
    setPreviewDoc(doc);
    setPreviewContent('');
    setPreviewLoading(true);
    try {
      const res = await apiClient.downloadDocument(doc.id);
      const { contentBase64, mimeType } = res.data;
      if (contentBase64) {
        const dataUrl = `data:${mimeType || 'application/octet-stream'};base64,${contentBase64}`;
        const isText = mimeType?.startsWith('text/') || mimeType === 'application/json' || mimeType === 'application/xml';
        if (isText) {
          setPreviewContent(atob(contentBase64));
        } else {
          setPreviewContent(dataUrl);
        }
      }
    } catch {
      setPreviewContent('');
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleDownload = async (doc: DocumentRef) => {
    try {
      const res = await apiClient.downloadDocument(doc.id);
      const { contentBase64, mimeType } = res.data;
      if (contentBase64) {
        const dataUrl = `data:${mimeType || 'application/octet-stream'};base64,${contentBase64}`;
        const a = document.createElement('a');
        a.href = dataUrl;
        a.download = doc.name;
        a.click();
      }
    } catch {
      // ignore
    }
  };

  const handleDuplicate = async (id: number) => {
    try {
      await apiClient.duplicateDocument(id);
      await fetchDocs();
    } catch {
      // ignore
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this document?')) return;
    try {
      await apiClient.deleteDocument(id);
      await fetchDocs();
    } catch {
      // ignore
    }
  };

  if (entityId == null) {
    return (
      <div className="text-sm text-gray-500 italic p-3 border border-dashed border-gray-300 rounded-lg">
        Save the record first to attach documents.
      </div>
    );
  }

  const showDropZone = multiple || docs.length === 0;

  return (
    <div className="space-y-2">
      <div className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</div>

      {!showDropZone && docs.length >= 1 && (
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="text-sm text-blue-600 dark:text-blue-400 hover:underline disabled:opacity-50"
        >
          {uploading ? 'Uploading...' : 'Replace file'}
        </button>
      )}

      {showDropZone && (
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-lg p-4 text-center cursor-pointer transition-colors ${
            isDragOver
              ? 'border-blue-400 bg-blue-50 dark:bg-blue-900/20'
              : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 hover:bg-gray-50 dark:hover:bg-gray-700/50'
          }`}
        >
          {uploading ? (
            <div className="flex items-center justify-center gap-2 text-sm text-gray-500 dark:text-gray-400">
              <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
              Uploading...
            </div>
          ) : (
            <div className="text-sm text-gray-500 dark:text-gray-400">
              <span className="text-blue-600 dark:text-blue-400 font-medium">Click to upload</span> or drag &amp; drop
            </div>
          )}
        </div>
      )}

      <input
        ref={fileInputRef}
        type="file"
        multiple={multiple}
        className="hidden"
        onChange={handleFileInputChange}
      />

      {uploadError && (
        <div className="text-xs text-red-600 dark:text-red-400">{uploadError}</div>
      )}

      {docs.length > 0 && (
        <div className="grid grid-cols-1 gap-2">
          {docs.map((doc) => (
            <div
              key={doc.id}
              className="flex items-center gap-3 p-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
            >
              <span className="text-xl flex-shrink-0" title={doc.mimeType}>
                {getFileIcon(doc.mimeType)}
              </span>

              <div className="flex-1 min-w-0">
                {renamingId === doc.id ? (
                  <input
                    autoFocus
                    value={renameValue}
                    onChange={(e) => setRenameValue(e.target.value)}
                    onBlur={() => commitRename(doc.id)}
                    onKeyDown={(e) => handleRenameKeyDown(e, doc.id)}
                    className="w-full text-sm border border-blue-400 rounded px-1 py-0.5 bg-white dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                ) : (
                  <button
                    type="button"
                    onClick={() => startRename(doc)}
                    className="text-sm font-medium text-gray-900 dark:text-gray-100 hover:text-blue-600 dark:hover:text-blue-400 truncate block w-full text-left"
                    title="Click to rename"
                  >
                    {doc.name}
                  </button>
                )}
                <div className="text-xs text-gray-400 dark:text-gray-500 mt-0.5 flex gap-2">
                  {doc.sizeBytes != null && <span>{formatSize(doc.sizeBytes)}</span>}
                  {doc.createdAt && <span>{new Date(doc.createdAt).toLocaleDateString()}</span>}
                </div>
              </div>

              <div className="flex items-center gap-1 flex-shrink-0">
                <button
                  type="button"
                  onClick={() => handlePreview(doc)}
                  title="Preview"
                  className="p-1 rounded text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors text-sm"
                >
                  👁
                </button>
                <button
                  type="button"
                  onClick={() => handleDownload(doc)}
                  title="Download"
                  className="p-1 rounded text-gray-400 hover:text-green-600 dark:hover:text-green-400 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors text-sm"
                >
                  ⬇
                </button>
                <button
                  type="button"
                  onClick={() => handleDuplicate(doc.id)}
                  title="Duplicate"
                  className="p-1 rounded text-gray-400 hover:text-purple-600 dark:hover:text-purple-400 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors text-sm"
                >
                  ⧉
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(doc.id)}
                  title="Delete"
                  className="p-1 rounded text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors text-sm"
                >
                  🗑
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Preview Modal */}
      {previewDoc && (
        <div
          className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
          onClick={() => setPreviewDoc(null)}
        >
          <div
            className="bg-white dark:bg-gray-900 rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{previewDoc.name}</span>
              <button
                type="button"
                onClick={() => setPreviewDoc(null)}
                className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 text-xl leading-none ml-4 flex-shrink-0"
              >
                ×
              </button>
            </div>
            <div className="p-4">
              {previewLoading ? (
                <div className="flex items-center justify-center py-12 text-sm text-gray-500 dark:text-gray-400 gap-2">
                  <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                  Loading preview...
                </div>
              ) : previewContent ? (
                previewDoc.mimeType?.startsWith('image/') ? (
                  <img
                    src={previewContent}
                    alt={previewDoc.name}
                    className="max-w-full max-h-[70vh] object-contain mx-auto"
                  />
                ) : previewDoc.mimeType === 'application/pdf' ? (
                  <iframe
                    src={previewContent}
                    className="w-full h-[70vh]"
                    title={previewDoc.name}
                  />
                ) : previewDoc.mimeType?.startsWith('video/') ? (
                  <video
                    src={previewContent}
                    controls
                    className="w-full max-h-[70vh] rounded"
                  >
                    Your browser does not support video playback.
                  </video>
                ) : previewDoc.mimeType?.startsWith('audio/') ? (
                  <div className="flex flex-col items-center gap-4 py-8">
                    <span className="text-6xl">🎵</span>
                    <p className="text-sm text-gray-600 dark:text-gray-300">{previewDoc.name}</p>
                    <audio src={previewContent} controls className="w-full max-w-md" />
                  </div>
                ) : previewDoc.mimeType?.startsWith('text/html') ? (
                  <iframe
                    srcDoc={previewContent}
                    sandbox="allow-scripts"
                    className="w-full h-[60vh] border rounded"
                    title={previewDoc.name}
                  />
                ) : previewDoc.mimeType === 'text/csv' ? (
                  <CsvPreview content={previewContent} />
                ) : previewDoc.mimeType === 'text/markdown' || previewDoc.name.endsWith('.md') ? (
                  <MarkdownPreview content={previewContent} />
                ) : previewDoc.mimeType?.startsWith('text/') || previewDoc.mimeType === 'application/json' || previewDoc.mimeType === 'application/xml' ? (
                  <pre className="bg-gray-50 dark:bg-gray-800 p-4 rounded text-sm overflow-auto whitespace-pre-wrap max-h-[60vh] font-mono">
                    {previewContent}
                  </pre>
                ) : (
                  <div className="text-center py-8">
                    <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">No preview available for this file type.</p>
                    <a
                      href={previewContent}
                      download={previewDoc.name}
                      className="text-blue-600 dark:text-blue-400 hover:underline text-sm"
                    >
                      Download {previewDoc.name}
                    </a>
                  </div>
                )
              ) : (
                <div className="text-center py-8 text-sm text-gray-500 dark:text-gray-400">
                  No preview available.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
