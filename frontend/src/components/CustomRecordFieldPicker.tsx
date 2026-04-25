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
import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { apiClient } from '../api/apiClient';
import { QuickCustomRecordCreateModal } from './QuickCustomRecordCreateModal';

export interface CustomRecordRef {
  id: number;
  name: string;
}

interface CustomRecordFieldPickerProps {
  value: CustomRecordRef | CustomRecordRef[] | null | undefined;
  onChange: (value: CustomRecordRef | CustomRecordRef[] | null) => void;
  multiple: boolean;
  label: string;
  required?: boolean;
  disabled?: boolean;
  error?: string;
}

export const CustomRecordFieldPicker = ({
  value,
  onChange,
  multiple,
  label,
  required,
  disabled,
  error,
}: CustomRecordFieldPickerProps) => {
  const [search, setSearch] = useState('');
  const [results, setResults] = useState<CustomRecordRef[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [showQuickCreate, setShowQuickCreate] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Normalize selected to array for uniform handling
  const selected: CustomRecordRef[] = multiple
    ? Array.isArray(value) ? value : []
    : value && !Array.isArray(value) ? [value] : [];

  // Search customRecords with debounce
  useEffect(() => {
    if (!showResults) return;
    const t = setTimeout(() => {
      apiClient
        .listCustomRecords({ search: search || undefined, size: 20 })
        .then((r) => {
          setResults((r.data.content || []).map((a: any) => ({ id: a.id, name: a.name })));
        })
        .catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [search, showResults]);

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowResults(false);
        setSearch('');
      }
    };
    if (showResults) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [showResults]);

  const handleSelect = (customRecord: CustomRecordRef) => {
    if (multiple) {
      const exists = selected.some((a) => a.id === customRecord.id);
      const next = exists ? selected.filter((a) => a.id !== customRecord.id) : [...selected, customRecord];
      onChange(next);
    } else {
      onChange(customRecord);
      setShowResults(false);
      setSearch('');
    }
  };

  const handleRemove = (id: number) => {
    if (multiple) {
      onChange(selected.filter((a) => a.id !== id));
    } else {
      onChange(null);
    }
  };

  const handleQuickCreated = (customRecord: CustomRecordRef) => {
    setShowQuickCreate(false);
    handleSelect(customRecord);
  };

  const labelClass = 'block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1';
  const errorClass = 'mt-1 text-xs text-red-600 dark:text-red-400';

  // View mode: render as clickable badges
  if (disabled) {
    return (
      <div className="mb-1">
        <label className={labelClass}>
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
        <div className="flex flex-wrap gap-1.5 mt-1">
          {selected.length === 0 ? (
            <span className="text-sm text-gray-400 dark:text-gray-500">—</span>
          ) : (
            selected.map((a) => (
              <a
                key={a.id}
                href={`/customRecords?view=${a.id}`}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300 hover:bg-indigo-200 dark:hover:bg-indigo-900/60 transition-colors"
              >
                📦 {a.name}
              </a>
            ))
          )}
        </div>
        {error && <span className={errorClass}>{error}</span>}
      </div>
    );
  }

  // Edit mode
  return (
    <div className="mb-1">
      <label className={labelClass}>
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>

      {/* Selected chips */}
      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-2">
          {selected.map((a) => (
            <span
              key={a.id}
              className="inline-flex items-center gap-1 pl-2.5 pr-1 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300"
            >
              📦 {a.name}
              <button
                type="button"
                onClick={() => handleRemove(a.id)}
                className="ml-0.5 rounded-full w-4 h-4 flex items-center justify-center hover:bg-indigo-200 dark:hover:bg-indigo-800 transition-colors text-indigo-600 dark:text-indigo-400 font-bold text-xs"
              >
                &times;
              </button>
            </span>
          ))}
        </div>
      )}

      {/* Search + quick create — hidden in single mode once a value is chosen */}
      {(multiple || selected.length === 0) && (
        <div className="flex gap-2" ref={dropdownRef}>
          <div className="relative flex-1">
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); setShowResults(true); }}
              onFocus={() => setShowResults(true)}
              placeholder="Search customRecords..."
              className="w-full px-3 py-2 bg-white dark:bg-gray-700 text-sm text-gray-900 dark:text-white border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {showResults && dropdownRef.current && createPortal(
              <div className="fixed z-[9999] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg max-h-48 overflow-y-auto"
                style={{
                  top: `${(dropdownRef.current?.getBoundingClientRect()?.bottom ?? 0) + 4}px`,
                  left: `${dropdownRef.current?.getBoundingClientRect()?.left ?? 0}px`,
                  right: 'auto',
                  width: `${dropdownRef.current?.getBoundingClientRect()?.width ?? 0}px`,
                }}>
                {results.length === 0 ? (
                  <div className="px-3 py-2 text-sm text-gray-400 dark:text-gray-500">No customRecords found</div>
                ) : (
                  results.map((a) => {
                    const isSelected = selected.some((s) => s.id === a.id);
                    return (
                      <button
                        key={a.id}
                        type="button"
                        onMouseDown={(e) => { e.preventDefault(); handleSelect(a); }}
                        className={`w-full text-left px-3 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors ${
                          isSelected ? 'text-blue-600 dark:text-blue-400 font-medium' : 'text-gray-700 dark:text-gray-300'
                        }`}
                      >
                        {isSelected ? '✓ ' : ''}
                        {a.name}
                      </button>
                    );
                  })
                )}
              </div>,
              document.body
            )}
          </div>
          <button
            type="button"
            onClick={() => setShowQuickCreate(true)}
            className="px-3 py-2 bg-indigo-600 text-white text-xs font-medium rounded-lg hover:bg-indigo-700 transition-colors whitespace-nowrap"
          >
            + Quick Create
          </button>
        </div>
      )}

      {error && <span className={errorClass}>{error}</span>}

      {showQuickCreate && (
        <QuickCustomRecordCreateModal onCreated={handleQuickCreated} onClose={() => setShowQuickCreate(false)} />
      )}
    </div>
  );
};
