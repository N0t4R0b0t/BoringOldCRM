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
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

export interface EntityOption {
  id: number;
  label: string;
  sublabel?: string;
}

interface EntityPickerProps {
  label: string;
  value: EntityOption | null;
  onChange: (value: EntityOption | null) => void;
  onSearch: (term: string) => Promise<EntityOption[]>;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
}

export const EntityPicker = ({
  label,
  value,
  onChange,
  onSearch,
  placeholder = 'Search...',
  required,
  disabled,
}: EntityPickerProps) => {
  const [search, setSearch] = useState('');
  const [results, setResults] = useState<EntityOption[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!showResults) return;
    const t = setTimeout(() => {
      setIsSearching(true);
      onSearch(search)
        .then(setResults)
        .catch(() => setResults([]))
        .finally(() => setIsSearching(false));
    }, 200);
    return () => clearTimeout(t);
  }, [search, showResults]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setShowResults(false);
        setSearch('');
      }
    };
    if (showResults) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [showResults]);

  const handleSelect = (option: EntityOption) => {
    onChange(option);
    setShowResults(false);
    setSearch('');
  };

  const handleClear = () => {
    onChange(null);
    setSearch('');
  };

  const labelClass = 'block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1';
  const inputClass = 'w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm';

  return (
    <div>
      <label className={labelClass}>
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>

      {value ? (
        <div className="flex items-center gap-2 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700">
          <span className="flex-1 text-sm text-gray-900 dark:text-white font-medium">{value.label}</span>
          {value.sublabel && (
            <span className="text-xs text-gray-500 dark:text-gray-400">{value.sublabel}</span>
          )}
          {!disabled && (
            <button
              type="button"
              onClick={handleClear}
              className="w-5 h-5 flex items-center justify-center rounded-full hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-500 dark:text-gray-400 transition-colors text-xs font-bold"
            >
              &times;
            </button>
          )}
        </div>
      ) : (
        <div ref={containerRef} className="relative">
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setShowResults(true); }}
            onFocus={() => setShowResults(true)}
            placeholder={placeholder}
            disabled={disabled}
            className={inputClass}
          />
          {showResults && containerRef.current && createPortal(
            <div
              className="fixed z-[9999] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg max-h-52 overflow-y-auto"
              style={{
                top: `${(containerRef.current.getBoundingClientRect().bottom) + 4}px`,
                left: `${containerRef.current.getBoundingClientRect().left}px`,
                width: `${containerRef.current.getBoundingClientRect().width}px`,
              }}
            >
              {isSearching ? (
                <div className="px-3 py-2 text-sm text-gray-400 dark:text-gray-500">Searching...</div>
              ) : results.length === 0 ? (
                <div className="px-3 py-2 text-sm text-gray-400 dark:text-gray-500">
                  {search ? 'No results found' : 'Start typing to search'}
                </div>
              ) : (
                results.map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    onMouseDown={(e) => { e.preventDefault(); handleSelect(opt); }}
                    className="w-full text-left px-3 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors text-gray-700 dark:text-gray-300"
                  >
                    <span className="font-medium">{opt.label}</span>
                    {opt.sublabel && (
                      <span className="text-gray-500 dark:text-gray-400 text-xs ml-2">{opt.sublabel}</span>
                    )}
                  </button>
                ))
              )}
            </div>,
            document.body
          )}
        </div>
      )}
    </div>
  );
};
