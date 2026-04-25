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
import React, { useState } from 'react';
import '../styles/Search.css';

interface SearchBarProps {
  onSearch: (query: string) => void;
  placeholder?: string;
  debounceMs?: number;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  onSearch,
  placeholder = 'Search...',
  debounceMs = 300,
}) => {
  const [query, setQuery] = useState('');
  const [timeoutId, setTimeoutId] = useState<ReturnType<typeof setTimeout> | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);

    // Clear existing timeout
    if (timeoutId) {
      clearTimeout(timeoutId);
    }

    // Set new timeout for debounced search
    const newTimeoutId = setTimeout(() => {
      onSearch(value);
    }, debounceMs);

    setTimeoutId(newTimeoutId);
  };

  const handleClear = () => {
    setQuery('');
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    onSearch('');
  };

  return (
    <div className="search-bar dark:bg-gray-700 dark:border-gray-600">
      <input
        type="text"
        value={query}
        onChange={handleChange}
        placeholder={placeholder}
        className="search-input dark:bg-gray-700 dark:text-white dark:placeholder-gray-400"
      />
      {query && (
        <button onClick={handleClear} className="search-clear dark:text-gray-400 dark:hover:text-gray-200" title="Clear">
          ✕
        </button>
      )}
      <span className="search-icon dark:text-gray-400">🔍</span>
    </div>
  );
};
