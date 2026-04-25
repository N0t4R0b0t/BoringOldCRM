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
import { apiClient } from '../api/apiClient';

interface DocumentOption {
  id: number;
  name: string;
  contentType?: string;
}

interface DocumentFieldPickerProps {
  fieldId: string;
  label: string;
  required?: boolean;
  disabled?: boolean;
  multiple?: boolean;
  /** single: stores a number (document id). multiple: stores number[] */
  value: number | number[] | null;
  onChange: (value: number | number[] | null) => void;
  error?: string;
}

export const DocumentFieldPicker = ({
  fieldId,
  label,
  required,
  disabled,
  multiple = false,
  value,
  onChange,
  error,
}: DocumentFieldPickerProps) => {
  const [options, setOptions] = useState<DocumentOption[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    apiClient.listDocuments({ size: 200 })
      .then((r) => {
        const docs: DocumentOption[] = (r.data?.content || r.data || []).map((d: any) => ({
          id: d.id,
          name: d.name,
          contentType: d.contentType,
        }));
        setOptions(docs);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const labelClass = "block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1";
  const inputClass = `w-full px-3 py-2 bg-white dark:bg-gray-700 dark:text-white border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${error ? 'border-red-300' : ''} ${disabled ? 'opacity-60 cursor-not-allowed bg-gray-50 dark:bg-gray-800' : ''}`;
  const errorClass = "mt-1 text-xs text-red-600 dark:text-red-400";

  if (!multiple) {
    const singleValue = typeof value === 'number' ? value : null;
    return (
      <div className="mb-1">
        <label htmlFor={fieldId} className={labelClass}>
          {label}{required && <span className="text-red-500 ml-1">*</span>}
        </label>
        <select
          id={fieldId}
          value={singleValue ?? ''}
          onChange={(e) => onChange(e.target.value ? parseInt(e.target.value) : null)}
          disabled={disabled || loading}
          className={`${inputClass} appearance-none`}
          required={required}
        >
          <option value="">-- Select document --</option>
          {options.map((doc) => (
            <option key={doc.id} value={doc.id}>
              {doc.name}
            </option>
          ))}
        </select>
        {error && <span className={errorClass}>{error}</span>}
      </div>
    );
  }

  // Multiple selection
  const selectedIds: number[] = Array.isArray(value) ? value : [];
  return (
    <div className="mb-1">
      <label className={labelClass}>
        {label}{required && <span className="text-red-500 ml-1">*</span>}
      </label>
      <div className="space-y-1 border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700 max-h-40 overflow-y-auto">
        {loading && <div className="text-xs text-gray-400">Loading documents...</div>}
        {options.map((doc) => (
          <label key={doc.id} className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={selectedIds.includes(doc.id)}
              disabled={disabled}
              onChange={(e) => {
                const next = e.target.checked
                  ? [...selectedIds, doc.id]
                  : selectedIds.filter((i) => i !== doc.id);
                onChange(next.length > 0 ? next : null);
              }}
              className="h-4 w-4 rounded border-gray-300 dark:border-gray-600 text-blue-600"
            />
            <span className="text-sm text-gray-700 dark:text-gray-300">{doc.name}</span>
          </label>
        ))}
        {!loading && options.length === 0 && (
          <div className="text-xs text-gray-400">No documents available.</div>
        )}
      </div>
      {error && <span className={errorClass}>{error}</span>}
    </div>
  );
};
