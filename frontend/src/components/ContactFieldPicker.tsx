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
import { QuickContactCreateModal } from './QuickContactCreateModal';

export interface ContactRef {
  id: number;
  name: string;
  email?: string;
}

interface ContactFieldPickerProps {
  value: ContactRef | ContactRef[] | null | undefined;
  onChange: (value: ContactRef | ContactRef[] | null) => void;
  multiple: boolean;
  label: string;
  required?: boolean;
  disabled?: boolean;
  error?: string;
}

export const ContactFieldPicker = ({
  value,
  onChange,
  multiple,
  label,
  required,
  disabled,
  error,
}: ContactFieldPickerProps) => {
  const [search, setSearch] = useState('');
  const [results, setResults] = useState<ContactRef[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [showQuickCreate, setShowQuickCreate] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Normalize selected to array for uniform handling
  const selected: ContactRef[] = multiple
    ? Array.isArray(value) ? value : []
    : value && !Array.isArray(value) ? [value] : [];

  // Search contacts with debounce
  useEffect(() => {
    if (!showResults) return;
    const t = setTimeout(() => {
      apiClient
        .getContacts({ search: search || undefined, size: 20 })
        .then((r) => {
          setResults((r.data.content || []).map((c: any) => ({ id: c.id, name: c.name, email: c.email })));
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

  const handleSelect = (contact: ContactRef) => {
    if (multiple) {
      const exists = selected.some((c) => c.id === contact.id);
      const next = exists ? selected.filter((c) => c.id !== contact.id) : [...selected, contact];
      onChange(next);
    } else {
      onChange(contact);
      setShowResults(false);
      setSearch('');
    }
  };

  const handleRemove = (id: number) => {
    if (multiple) {
      onChange(selected.filter((c) => c.id !== id));
    } else {
      onChange(null);
    }
  };

  const handleQuickCreated = (contact: ContactRef) => {
    setShowQuickCreate(false);
    handleSelect(contact);
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
            selected.map((c) => (
              <a
                key={c.id}
                href={`/contacts?view=${c.id}`}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/60 transition-colors"
              >
                👤 {c.name}
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
          {selected.map((c) => (
            <span
              key={c.id}
              className="inline-flex items-center gap-1 pl-2.5 pr-1 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300"
            >
              👤 {c.name}
              <button
                type="button"
                onClick={() => handleRemove(c.id)}
                className="ml-0.5 rounded-full w-4 h-4 flex items-center justify-center hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors text-blue-600 dark:text-blue-400 font-bold text-xs"
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
              placeholder="Search contacts..."
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
                  <div className="px-3 py-2 text-sm text-gray-400 dark:text-gray-500">No contacts found</div>
                ) : (
                  results.map((c) => {
                    const isSelected = selected.some((s) => s.id === c.id);
                    return (
                      <button
                        key={c.id}
                        type="button"
                        onMouseDown={(e) => { e.preventDefault(); handleSelect(c); }}
                        className={`w-full text-left px-3 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors ${
                          isSelected ? 'text-blue-600 dark:text-blue-400 font-medium' : 'text-gray-700 dark:text-gray-300'
                        }`}
                      >
                        {isSelected ? '✓ ' : ''}
                        {c.name}
                        {c.email && <span className="text-gray-500 dark:text-gray-400 text-xs ml-1">({c.email})</span>}
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
            className="px-3 py-2 bg-blue-600 text-white text-xs font-medium rounded-lg hover:bg-blue-700 transition-colors whitespace-nowrap"
          >
            + Quick Create
          </button>
        </div>
      )}

      {error && <span className={errorClass}>{error}</span>}

      {showQuickCreate && (
        <QuickContactCreateModal onCreated={handleQuickCreated} onClose={() => setShowQuickCreate(false)} />
      )}
    </div>
  );
};
