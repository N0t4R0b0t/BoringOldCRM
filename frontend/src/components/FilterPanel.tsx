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
import '../styles/Filter.css';

interface FilterPanelProps {
  onApplyFilters: (filters: Record<string, any>) => void;
  filterOptions: Array<{
    field: string;
    label: string;
    type: 'text' | 'select' | 'date' | 'number';
    options?: Array<{ value: any; label: string }>;
  }>;
  isOpen?: boolean;
  onToggle?: () => void;
}

export const FilterPanel: React.FC<FilterPanelProps> = ({
  onApplyFilters,
  filterOptions,
  isOpen = false,
  onToggle,
}) => {
  const [filters, setFilters] = useState<Record<string, any>>({});

  const handleFilterChange = (field: string, value: any) => {
    const newFilters = { ...filters };
    if (value === '' || value === null) {
      delete newFilters[field];
    } else {
      newFilters[field] = value;
    }
    setFilters(newFilters);
  };

  const handleApply = () => {
    onApplyFilters(filters);
  };

  const handleReset = () => {
    setFilters({});
    onApplyFilters({});
  };

  if (!isOpen) {
    return (
      <button onClick={onToggle} className="btn btn-sm btn-secondary">
        🔧 Filters
      </button>
    );
  }

  return (
    <div className="filter-panel">
      <div className="filter-header">
        <h3>Filters</h3>
        <button onClick={onToggle} className="close-btn">
          ✕
        </button>
      </div>

      <div className="filter-fields">
        {filterOptions.map((option) => (
          <div key={option.field} className="filter-field">
            <label>{option.label}</label>
            {option.type === 'select' && (
              <select
                value={filters[option.field] || ''}
                onChange={(e) =>
                  handleFilterChange(option.field, e.target.value)
                }
                className="filter-select"
              >
                <option value="">All</option>
                {option.options?.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            )}
            {option.type === 'text' && (
              <input
                type="text"
                value={filters[option.field] || ''}
                onChange={(e) =>
                  handleFilterChange(option.field, e.target.value)
                }
                className="filter-input"
                placeholder="Enter text..."
              />
            )}
            {option.type === 'date' && (
              <input
                type="date"
                value={filters[option.field] || ''}
                onChange={(e) =>
                  handleFilterChange(option.field, e.target.value)
                }
                className="filter-input"
              />
            )}
            {option.type === 'number' && (
              <input
                type="number"
                value={filters[option.field] || ''}
                onChange={(e) =>
                  handleFilterChange(option.field, e.target.value)
                }
                className="filter-input"
                placeholder="Enter number..."
              />
            )}
          </div>
        ))}
      </div>

      <div className="filter-actions">
        <button onClick={handleReset} className="btn btn-sm btn-secondary">
          Reset
        </button>
        <button onClick={handleApply} className="btn btn-sm btn-primary">
          Apply Filters
        </button>
      </div>
    </div>
  );
};
