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
import '../styles/AdvancedFilters.css';

export interface AdvancedFilterConfig {
  clauses: FilterClause[];
  combineWith: 'AND' | 'OR';
}

export interface FilterClause {
  field: string;
  fieldType: 'text' | 'number' | 'date' | 'select';
  operator: string;
  value: any;
  valueEnd?: any;
}

interface AdvancedFiltersProps {
  availableFields: Array<{ name: string; type: string; label: string; options?: string[] }>;
  onApplyFilters: (config: AdvancedFilterConfig) => void;
  onClose: () => void;
}

export const AdvancedFilters: React.FC<AdvancedFiltersProps> = ({
  availableFields,
  onApplyFilters,
  onClose,
}) => {
  const [clauses, setClauses] = useState<FilterClause[]>([
    { field: '', fieldType: 'text', operator: 'contains', value: '' },
  ]);
  const [combineWith, setCombineWith] = useState<'AND' | 'OR'>('AND');

  const addClause = () => {
    setClauses([
      ...clauses,
      { field: '', fieldType: 'text', operator: 'contains', value: '' },
    ]);
  };

  const removeClause = (index: number) => {
    setClauses(clauses.filter((_, i) => i !== index));
  };

  const updateClause = (index: number, updates: Partial<FilterClause>) => {
    const newClauses = [...clauses];
    newClauses[index] = { ...newClauses[index], ...updates };
    setClauses(newClauses);
  };

  const getOperatorsForType = (fieldType: string) => {
    switch (fieldType) {
      case 'text':
        return ['contains', 'equals', 'starts_with', 'ends_with'];
      case 'number':
        return ['equals', 'gt', 'lt', 'gte', 'lte', 'between'];
      case 'date':
        return ['equals', 'after', 'before', 'between'];
      case 'select':
        return ['equals', 'in'];
      default:
        return ['equals'];
    }
  };

  const handleApply = () => {
    const validClauses = clauses.filter((c) => c.field && c.value !== '');
    if (validClauses.length > 0) {
      onApplyFilters({
        clauses: validClauses,
        combineWith,
      });
    }
  };

  return (
    <div className="advanced-filters-modal">
      <div className="advanced-filters-content">
        <div className="advanced-filters-header">
          <h3>Advanced Filters</h3>
          <button className="close-btn" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="advanced-filters-body">
          <div className="combine-with-selector">
            <label>Combine filters with:</label>
            <select
              value={combineWith}
              onChange={(e) => setCombineWith(e.target.value as 'AND' | 'OR')}
            >
              <option value="AND">AND (all filters must match)</option>
              <option value="OR">OR (any filter can match)</option>
            </select>
          </div>

          <div className="filter-clauses">
            {clauses.map((clause, index) => (
              <div key={index} className="filter-clause">
                <select
                  value={clause.field}
                  onChange={(e) => {
                    const field = availableFields.find((f) => f.name === e.target.value);
                    if (field) {
                      updateClause(index, {
                        field: e.target.value,
                        fieldType: field.type as any,
                        operator: getOperatorsForType(field.type)[0],
                      });
                    }
                  }}
                  className="field-select"
                >
                  <option value="">Select field...</option>
                  {availableFields.map((field) => (
                    <option key={field.name} value={field.name}>
                      {field.label}
                    </option>
                  ))}
                </select>

                {clause.field && (
                  <>
                    <select
                      value={clause.operator}
                      onChange={(e) => updateClause(index, { operator: e.target.value })}
                      className="operator-select"
                    >
                      {getOperatorsForType(clause.fieldType).map((op) => (
                        <option key={op} value={op}>
                          {op}
                        </option>
                      ))}
                    </select>

                    {clause.fieldType === 'text' && (
                      <input
                        type="text"
                        value={clause.value}
                        onChange={(e) => updateClause(index, { value: e.target.value })}
                        placeholder="Value..."
                        className="value-input"
                      />
                    )}

                    {clause.fieldType === 'number' && (
                      <>
                        <input
                          type="number"
                          value={clause.value}
                          onChange={(e) => updateClause(index, { value: e.target.value })}
                          placeholder="Value..."
                          className="value-input"
                        />
                        {(clause.operator === 'between' || clause.operator === 'gte-lte') && (
                          <input
                            type="number"
                            value={clause.valueEnd || ''}
                            onChange={(e) => updateClause(index, { valueEnd: e.target.value })}
                            placeholder="To..."
                            className="value-input"
                          />
                        )}
                      </>
                    )}

                    {clause.fieldType === 'date' && (
                      <>
                        <input
                          type="date"
                          value={clause.value}
                          onChange={(e) => updateClause(index, { value: e.target.value })}
                          className="value-input"
                        />
                        {clause.operator === 'between' && (
                          <input
                            type="date"
                            value={clause.valueEnd || ''}
                            onChange={(e) => updateClause(index, { valueEnd: e.target.value })}
                            className="value-input"
                          />
                        )}
                      </>
                    )}

                    {clause.fieldType === 'select' && (
                      <select
                        value={clause.value}
                        onChange={(e) => updateClause(index, { value: e.target.value })}
                        className="value-input"
                      >
                        <option value="">Select...</option>
                        {availableFields
                          .find((f) => f.name === clause.field)
                          ?.options?.map((opt) => (
                            <option key={opt} value={opt}>
                              {opt}
                            </option>
                          ))}
                      </select>
                    )}
                  </>
                )}

                {clauses.length > 1 && (
                  <button
                    className="remove-clause-btn"
                    onClick={() => removeClause(index)}
                    title="Remove filter"
                  >
                    ✕
                  </button>
                )}
              </div>
            ))}
          </div>

          <button className="add-clause-btn" onClick={addClause}>
            + Add Filter
          </button>
        </div>

        <div className="advanced-filters-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" onClick={handleApply}>
            Apply Filters
          </button>
        </div>
      </div>
    </div>
  );
};
