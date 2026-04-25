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
import React, { useEffect, useMemo, useState } from 'react';
import { formatDistanceToNow } from 'date-fns';
import { SlidersHorizontal, Filter, Download, Trash2, ChevronDown } from 'lucide-react';
import type { ColumnDefinition, SortConfig } from '../types/pagination';
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS, getPageRange } from '../utils/pagination';

interface DataTableProps<T> {
  tableId: string;
  data: T[];
  columns: ColumnDefinition[];
  isLoading?: boolean;
  error?: string;
  onSort?: (key: string, order: 'asc' | 'desc') => void;
  onPageChange?: (page: number) => void;
  onPageSizeChange?: (size: number) => void;
  currentPage?: number;
  totalPages?: number;
  totalElements?: number;
  pageSize?: number;
  sortConfig?: SortConfig;
  onRowClick?: (row: T) => void;
  emptyMessage?: string;
  onBulkDelete?: (ids: number[]) => Promise<void>;
  rowId?: (row: T) => number;
}

type PersistedConfig = {
  columnOrder: string[];
  hiddenColumns: string[];
};

const storageKey = (tableId: string) => `datatable:${tableId}:config`;

const getNestedValue = (row: any, key: string): any => {
  if (!key.includes('.')) {
    return row?.[key];
  }
  return key.split('.').reduce((acc, part) => (acc == null ? undefined : acc[part]), row);
};

const toStringSafe = (value: unknown): string => {
  if (value == null) return '';
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  if (value instanceof Date) return value.toISOString();
  return JSON.stringify(value);
};

const escapeCsv = (value: string): string => {
  const normalized = value.replace(/\r?\n/g, ' ');
  if (/[",]/.test(normalized)) {
    return `"${normalized.replace(/"/g, '""')}"`;
  }
  return normalized;
};

const exportFilename = (tableId: string, extension: string): string => {
  const date = new Date().toISOString().slice(0, 10);
  return `${tableId}-${date}.${extension}`;
};

const downloadBlob = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

const stringToHue = (s: string): number => {
  let hash = 5381;
  for (let i = 0; i < s.length; i++) {
    hash = ((hash << 5) + hash) + s.charCodeAt(i);
  }
  return Math.abs(hash) % 360;
};

const getInitials = (name: string): string => {
  if (!name) return '?';
  return name
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('');
};

export const DataTable = React.forwardRef<HTMLDivElement, DataTableProps<any>>(
  (
    {
      tableId,
      data,
      columns,
      isLoading = false,
      error = '',
      onSort,
      onPageChange,
      onPageSizeChange,
      currentPage = 1,
      totalPages = 1,
      totalElements = 0,
      pageSize = DEFAULT_PAGE_SIZE,
      sortConfig,
      onRowClick,
      emptyMessage = 'No data available',
      onBulkDelete,
      rowId,
    },
    ref
  ) => {
    const [showColumnConfig, setShowColumnConfig] = useState(false);
    const [showColumnFilters, setShowColumnFilters] = useState(false);
    const [columnOrder, setColumnOrder] = useState<string[]>(columns.map((c) => c.key));
    const [hiddenColumns, setHiddenColumns] = useState<Set<string>>(new Set());
    const [columnFilters, setColumnFilters] = useState<Record<string, string>>({});
    const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
    const [bulkDeleting, setBulkDeleting] = useState(false);
    const [bulkModeActive, setBulkModeActive] = useState(false);
    const [showExportMenu, setShowExportMenu] = useState(false);
    const exportMenuRef = React.useRef<HTMLDivElement>(null);

    useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (exportMenuRef.current && !exportMenuRef.current.contains(event.target as Node)) {
          setShowExportMenu(false);
        }
      };
      if (showExportMenu) {
        document.addEventListener('mousedown', handleClickOutside);
      }
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [showExportMenu]);

    const toggleBulkMode = () => {
      setBulkModeActive((v) => {
        if (v) setSelectedIds(new Set());
        return !v;
      });
    };

    const getRowId = (row: any): number => rowId ? rowId(row) : row.id;

    useEffect(() => {
      const parsed = columns.map((c) => c.key);
      setColumnOrder((prev) => {
        const filtered = prev.filter((key) => parsed.includes(key));
        const missing = parsed.filter((key) => !filtered.includes(key));
        return [...filtered, ...missing];
      });
      setHiddenColumns((prev) => new Set(Array.from(prev).filter((key) => parsed.includes(key))));
    }, [columns]);

    useEffect(() => {
      try {
        const raw = localStorage.getItem(storageKey(tableId));
        if (!raw) return;
        const parsed = JSON.parse(raw) as PersistedConfig;
        if (parsed.columnOrder?.length) {
          setColumnOrder(parsed.columnOrder);
        }
        if (parsed.hiddenColumns?.length) {
          setHiddenColumns(new Set(parsed.hiddenColumns));
        }
      } catch (err) {
        console.error('Failed to load table config', err);
      }
    }, [tableId]);

    useEffect(() => {
      const payload: PersistedConfig = {
        columnOrder,
        hiddenColumns: Array.from(hiddenColumns),
      };
      localStorage.setItem(storageKey(tableId), JSON.stringify(payload));
    }, [tableId, columnOrder, hiddenColumns]);

    const columnsByKey = useMemo(() => {
      const map = new Map<string, ColumnDefinition>();
      columns.forEach((c) => map.set(c.key, c));
      return map;
    }, [columns]);

    const orderedColumns = useMemo(
      () => columnOrder.map((key) => columnsByKey.get(key)).filter(Boolean) as ColumnDefinition[],
      [columnOrder, columnsByKey]
    );

    const visibleColumns = useMemo(
      () => orderedColumns.filter((c) => !hiddenColumns.has(c.key)),
      [orderedColumns, hiddenColumns]
    );

    const activeFilters = useMemo(
      () => Object.entries(columnFilters).filter(([, value]) => value.trim().length > 0),
      [columnFilters]
    );

    const filteredData = useMemo(() => {
      if (activeFilters.length === 0) return data;

      return data.filter((row) => {
        return activeFilters.every(([key, rawValue]) => {
          const column = columnsByKey.get(key);
          if (!column) return true;

          const filterValue = rawValue.toLowerCase();
          const cellRaw = getNestedValue(row, key);

          if (column.filterType === 'number') {
            const cellNum = Number(cellRaw);
            const filterNum = Number(rawValue);
            if (Number.isNaN(filterNum)) return true;
            return cellNum === filterNum;
          }

          if (column.filterType === 'date') {
            if (!cellRaw) return false;
            const cellDate = new Date(cellRaw).toISOString().slice(0, 10);
            return cellDate === rawValue;
          }

          if (column.filterType === 'select') {
            return toStringSafe(cellRaw).toLowerCase() === filterValue;
          }

          return toStringSafe(cellRaw).toLowerCase().includes(filterValue);
        });
      });
    }, [activeFilters, columnsByKey, data]);

    const allFilteredIds = filteredData.map(getRowId);
    const allSelected = allFilteredIds.length > 0 && allFilteredIds.every((id) => selectedIds.has(id));
    const someSelected = !allSelected && allFilteredIds.some((id) => selectedIds.has(id));

    const toggleAll = () => {
      if (allSelected) {
        setSelectedIds((prev) => {
          const next = new Set(prev);
          allFilteredIds.forEach((id) => next.delete(id));
          return next;
        });
      } else {
        setSelectedIds((prev) => new Set([...prev, ...allFilteredIds]));
      }
    };

    const toggleRow = (id: number) => {
      setSelectedIds((prev) => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });
    };

    const handleBulkDelete = async () => {
      if (!onBulkDelete || selectedIds.size === 0) return;
      if (!confirm(`Delete ${selectedIds.size} selected item${selectedIds.size > 1 ? 's' : ''}? This cannot be undone.`)) return;
      setBulkDeleting(true);
      try {
        await onBulkDelete(Array.from(selectedIds));
        setSelectedIds(new Set());
      } finally {
        setBulkDeleting(false);
      }
    };

    const pageNumbers = getPageRange(currentPage, totalPages);

    const handleColumnClick = (column: ColumnDefinition) => {
      if (column.sortable && onSort) {
        const newOrder =
          sortConfig?.key === column.key && sortConfig?.order === 'asc'
            ? 'desc'
            : 'asc';
        onSort(column.key, newOrder);
      }
    };

    const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newSize = parseInt(e.target.value);
      onPageSizeChange?.(newSize);
      onPageChange?.(1);
    };

    const moveColumn = (key: string, direction: 'up' | 'down') => {
      setColumnOrder((prev) => {
        const index = prev.indexOf(key);
        if (index < 0) return prev;
        const target = direction === 'up' ? index - 1 : index + 1;
        if (target < 0 || target >= prev.length) return prev;
        const next = [...prev];
        const [item] = next.splice(index, 1);
        next.splice(target, 0, item);
        return next;
      });
    };

    const toggleColumn = (key: string) => {
      setHiddenColumns((prev) => {
        const next = new Set(prev);
        if (next.has(key)) {
          next.delete(key);
        } else {
          next.add(key);
        }
        return next;
      });
    };

    const resetColumns = () => {
      setColumnOrder(columns.map((c) => c.key));
      setHiddenColumns(new Set());
    };

    const clearFilters = () => {
      setColumnFilters({});
    };

    const getExportCell = (row: any, column: ColumnDefinition): string => {
      if (column.exportValue) {
        return toStringSafe(column.exportValue(row));
      }
      return toStringSafe(getNestedValue(row, column.key));
    };

    const exportCsv = () => {
      const headers = visibleColumns.map((c) => escapeCsv(c.label)).join(',');
      const rows = filteredData
        .map((row) => visibleColumns.map((c) => escapeCsv(getExportCell(row, c))).join(','))
        .join('\n');
      const csv = `${headers}\n${rows}`;
      downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8;' }), exportFilename(tableId, 'csv'));
    };

    const exportExcel = () => {
      const headerHtml = visibleColumns.map((c) => `<th>${c.label}</th>`).join('');
      const rowsHtml = filteredData
        .map((row) => {
          const cells = visibleColumns.map((c) => `<td>${getExportCell(row, c)}</td>`).join('');
          return `<tr>${cells}</tr>`;
        })
        .join('');

      const html = `
        <html>
          <head><meta charset="UTF-8" /></head>
          <body>
            <table border="1">
              <thead><tr>${headerHtml}</tr></thead>
              <tbody>${rowsHtml}</tbody>
            </table>
          </body>
        </html>
      `;

      downloadBlob(
        new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' }),
        exportFilename(tableId, 'xls')
      );
    };

    const exportPdf = () => {
      const popup = window.open('', '_blank', 'width=1200,height=800');
      if (!popup) return;

      const headerHtml = visibleColumns.map((c) => `<th>${c.label}</th>`).join('');
      const rowsHtml = filteredData
        .map((row) => {
          const cells = visibleColumns.map((c) => `<td>${getExportCell(row, c)}</td>`).join('');
          return `<tr>${cells}</tr>`;
        })
        .join('');

      popup.document.write(`
        <html>
          <head>
            <title>${tableId} Export</title>
            <style>
              body { font-family: Arial, sans-serif; margin: 24px; }
              table { width: 100%; border-collapse: collapse; }
              th, td { border: 1px solid #ddd; padding: 8px; font-size: 12px; text-align: left; }
              th { background: #f5f5f5; }
            </style>
          </head>
          <body>
            <h2>${tableId} Export</h2>
            <table>
              <thead><tr>${headerHtml}</tr></thead>
              <tbody>${rowsHtml}</tbody>
            </table>
          </body>
        </html>
      `);
      popup.document.close();
      popup.focus();
      popup.print();
    };

    if (error) {
      return (
        <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-200 px-4 py-3 rounded-lg mb-4">
          {error}
        </div>
      );
    }

    const buttonBaseClass = 'inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors';

    return (
      <div ref={ref} className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 flex flex-wrap items-center gap-2">
          <button
            onClick={() => setShowColumnConfig((v) => !v)}
            className={buttonBaseClass}
            title="Columns"
          >
            <SlidersHorizontal size={15} />
            <span className="hidden sm:inline">Columns</span>
          </button>
          <button
            onClick={() => setShowColumnFilters((v) => !v)}
            className={`${buttonBaseClass} ${activeFilters.length > 0 ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-700' : ''}`}
            title="Filters"
          >
            <Filter size={15} />
            <span className="hidden sm:inline">Filters</span>
            {activeFilters.length > 0 && <span className="inline-block w-1.5 h-1.5 rounded-full bg-blue-600 dark:bg-blue-400" />}
          </button>
          <div className="relative" ref={exportMenuRef}>
            <button
              onClick={() => setShowExportMenu((v) => !v)}
              className={buttonBaseClass}
              title="Export"
            >
              <Download size={15} />
              <span className="hidden sm:inline">Export</span>
              <ChevronDown size={14} className="hidden sm:inline" />
            </button>
            {showExportMenu && (
              <div className="absolute top-full left-0 mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-md shadow-lg z-50 overflow-hidden">
                <button
                  onClick={() => { exportCsv(); setShowExportMenu(false); }}
                  className="w-full text-left px-3 py-2 text-xs hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors border-b border-gray-200 dark:border-gray-700 last:border-b-0 text-gray-700 dark:text-gray-300"
                >
                  CSV
                </button>
                <button
                  onClick={() => { exportExcel(); setShowExportMenu(false); }}
                  className="w-full text-left px-3 py-2 text-xs hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors border-b border-gray-200 dark:border-gray-700 last:border-b-0 text-gray-700 dark:text-gray-300"
                >
                  Excel
                </button>
                <button
                  onClick={() => { exportPdf(); setShowExportMenu(false); }}
                  className="w-full text-left px-3 py-2 text-xs hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors border-b border-gray-200 dark:border-gray-700 last:border-b-0 text-gray-700 dark:text-gray-300"
                >
                  PDF
                </button>
              </div>
            )}
          </div>
          {onBulkDelete && (
            <button
              onClick={toggleBulkMode}
              title="Delete"
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border transition-colors ${
                bulkModeActive
                  ? 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 border-red-300 dark:border-red-600 hover:bg-red-100 dark:hover:bg-red-900/50'
                  : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
              }`}
            >
              <Trash2 size={15} />
              <span className="hidden sm:inline">Delete</span>
            </button>
          )}
          {onBulkDelete && bulkModeActive && selectedIds.size > 0 && (
            <button
              onClick={handleBulkDelete}
              disabled={bulkDeleting}
              className="ml-auto px-3 py-1.5 text-xs font-medium rounded-md bg-red-600 hover:bg-red-700 text-white disabled:opacity-50 transition-colors"
            >
              {bulkDeleting ? 'Deleting…' : `Delete ${selectedIds.size}`}
            </button>
          )}
        </div>

        {showColumnConfig && (
          <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
            <div className="flex justify-between items-center mb-3">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Column Configuration</h3>
              <button
                onClick={resetColumns}
                className="text-xs text-blue-600 dark:text-blue-400 hover:underline"
              >
                Reset to default
              </button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              {orderedColumns.map((column, index) => (
                <div
                  key={column.key}
                  className="flex items-center justify-between px-3 py-2 rounded-md border border-gray-200 dark:border-gray-700"
                >
                  <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                    <input
                      type="checkbox"
                      checked={!hiddenColumns.has(column.key)}
                      onChange={() => toggleColumn(column.key)}
                    />
                    {column.label}
                  </label>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => moveColumn(column.key, 'up')}
                      disabled={index === 0}
                      className="px-2 py-0.5 text-xs border border-gray-300 dark:border-gray-600 rounded disabled:opacity-50"
                    >
                      ↑
                    </button>
                    <button
                      onClick={() => moveColumn(column.key, 'down')}
                      disabled={index === orderedColumns.length - 1}
                      className="px-2 py-0.5 text-xs border border-gray-300 dark:border-gray-600 rounded disabled:opacity-50"
                    >
                      ↓
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {activeFilters.length > 0 && (
          <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-blue-50 dark:bg-blue-950/30 flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold text-blue-700 dark:text-blue-300">Active filters:</span>
            {activeFilters.map(([key, value]) => {
              const label = columnsByKey.get(key)?.label || key;
              return (
                <span
                  key={key}
                  className="px-2 py-1 text-xs rounded-full bg-white dark:bg-gray-800 border border-blue-200 dark:border-blue-700 text-blue-800 dark:text-blue-200"
                >
                  {label}: {value}
                </span>
              );
            })}
            <button onClick={clearFilters} className="text-xs text-blue-700 dark:text-blue-300 hover:underline">
              Clear all
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600">
                  {visibleColumns.map((column) => (
                    <th
                      key={column.key}
                      style={{ width: column.width }}
                      className="px-4 py-2 text-xs font-semibold text-gray-500 dark:text-gray-300 uppercase tracking-wider"
                    >
                      {column.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {Array.from({ length: 5 }).map((_, rowIdx) => (
                  <tr key={rowIdx} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    {visibleColumns.map((column) => (
                      <td key={`${rowIdx}-${column.key}`} className="px-4 py-2.5 text-sm">
                        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : filteredData.length === 0 ? (
          <div className="p-12 text-center text-gray-500 dark:text-gray-400">
            <svg className="mx-auto mb-4 w-16 h-16 text-gray-300 dark:text-gray-600" fill="currentColor" viewBox="0 0 24 24">
              <path d="M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zm-1 6H5v-4h14v4zM21 9H3c-.55 0-1 .45-1 1s.45 1 1 1h18c.55 0 1-.45 1-1s-.45-1-1-1zM21 5H3c-.55 0-1 .45-1 1s.45 1 1 1h18c.55 0 1-.45 1-1s-.45-1-1-1z" />
            </svg>
            <p className="text-lg font-medium">{emptyMessage}</p>
            <p className="text-sm mt-2">Try adjusting your filters</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600">
                    {onBulkDelete && bulkModeActive && (
                      <th className="w-10 px-3 py-3">
                        <input
                          type="checkbox"
                          checked={allSelected}
                          ref={(el) => { if (el) el.indeterminate = someSelected; }}
                          onChange={toggleAll}
                          className="rounded"
                        />
                      </th>
                    )}
                    {visibleColumns.map((column) => (
                      <th
                        key={column.key}
                        style={{ width: column.width }}
                        onClick={() => handleColumnClick(column)}
                        className={`px-4 py-2 text-xs font-semibold text-gray-500 dark:text-gray-300 uppercase tracking-wider ${
                          column.sortable ? 'cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors' : ''
                        }`}
                      >
                        <div className="flex items-center gap-1">
                          <span>{column.label}</span>
                          {column.sortable && sortConfig?.key === column.key && (
                            <span className="text-blue-600 dark:text-blue-400">
                              {sortConfig?.order === 'asc' ? '↑' : '↓'}
                            </span>
                          )}
                        </div>
                      </th>
                    ))}
                  </tr>
                  {showColumnFilters && (
                    <tr className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
                      {onBulkDelete && bulkModeActive && <th className="w-10 px-3 py-2" />}
                      {visibleColumns.map((column) => (
                        <th key={`filter-${column.key}`} className="px-4 py-2">
                          {column.filterType === 'select' ? (
                            <select
                              value={columnFilters[column.key] || ''}
                              onChange={(e) => setColumnFilters((prev) => ({ ...prev, [column.key]: e.target.value }))}
                              className="w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-xs px-2 py-1"
                            >
                              <option value="">All</option>
                              {(column.filterOptions || []).map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                  {opt.label}
                                </option>
                              ))}
                            </select>
                          ) : (
                            <input
                              type={column.filterType === 'number' ? 'number' : column.filterType === 'date' ? 'date' : 'text'}
                              value={columnFilters[column.key] || ''}
                              onChange={(e) => setColumnFilters((prev) => ({ ...prev, [column.key]: e.target.value }))}
                              placeholder="Filter..."
                              className="w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-xs px-2 py-1"
                            />
                          )}
                        </th>
                      ))}
                    </tr>
                  )}
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                  {filteredData.map((row, index) => {
                    const rowIdVal = getRowId(row);
                    const isSelected = selectedIds.has(rowIdVal);
                    return (
                    <tr
                      key={index}
                      onClick={() => onRowClick?.(row)}
                      className={`transition-colors ${isSelected ? 'bg-blue-50 dark:bg-blue-900/20' : 'hover:bg-gray-50 dark:hover:bg-gray-700'} ${onRowClick ? 'cursor-pointer' : ''}`}
                    >
                      {onBulkDelete && bulkModeActive && (
                        <td className="w-10 px-3 py-4" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleRow(rowIdVal)}
                            className="rounded"
                          />
                        </td>
                      )}
                      {visibleColumns.map((column) => {
                        const cellValue = getNestedValue(row, column.key);
                        let renderedContent = column.render ? column.render(cellValue, row) : cellValue?.toString() || '—';

                        if (column.avatar && !column.render && cellValue) {
                          const hue = stringToHue(cellValue);
                          const initials = getInitials(cellValue);
                          renderedContent = (
                            <div className="flex items-center gap-2">
                              <span
                                style={{ background: `hsl(${hue}, 60%, 50%)` }}
                                className="w-7 h-7 rounded-full text-white text-xs font-bold flex items-center justify-center shrink-0"
                              >
                                {initials}
                              </span>
                              <span>{cellValue}</span>
                            </div>
                          );
                        } else if (column.relativeTime && !column.render && cellValue) {
                          renderedContent = (
                            <span title={new Date(cellValue).toLocaleString()}>
                              {formatDistanceToNow(new Date(cellValue), { addSuffix: true })}
                            </span>
                          );
                        }

                        return (
                          <td key={`${index}-${column.key}`} className="px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300">
                            {renderedContent}
                          </td>
                        );
                      })}
                    </tr>
                  );
                  })}
                </tbody>
              </table>
            </div>

            <div className="px-4 py-3 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-4 text-sm text-gray-600 dark:text-gray-300">
                <span>
                  Showing <span className="font-medium">{Math.max(1, (currentPage - 1) * pageSize + 1)}</span> to{' '}
                  <span className="font-medium">{Math.min(currentPage * pageSize, totalElements)}</span> of{' '}
                  <span className="font-medium">{totalElements}</span> items
                  {activeFilters.length > 0 && (
                    <span className="ml-2 text-blue-600 dark:text-blue-300">(filtered rows on current page: {filteredData.length})</span>
                  )}
                </span>
                <select
                  value={pageSize}
                  onChange={handlePageSizeChange}
                  className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-1.5"
                >
                  {PAGE_SIZE_OPTIONS.map((size) => (
                    <option key={size} value={size}>
                      {size} per page
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => onPageChange?.(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="px-3 py-1 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Previous
                </button>

                <div className="hidden sm:flex gap-1">
                  {currentPage > 1 && (
                    <>
                      <button
                        onClick={() => onPageChange?.(1)}
                        className="px-3 py-1 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
                      >
                        1
                      </button>
                      {pageNumbers[0] > 2 && <span className="px-2 py-1 text-gray-500 dark:text-gray-400">...</span>}
                    </>
                  )}

                  {pageNumbers.map((num) => (
                    <button
                      key={num}
                      onClick={() => onPageChange?.(num)}
                      className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
                        num === currentPage
                          ? 'bg-blue-600 text-white border border-blue-600 dark:bg-blue-500 dark:border-blue-500'
                          : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-600'
                      }`}
                    >
                      {num}
                    </button>
                  ))}

                  {currentPage < totalPages && (
                    <>
                      {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
                        <span className="px-2 py-1 text-gray-500 dark:text-gray-400">...</span>
                      )}
                      <button
                        onClick={() => onPageChange?.(totalPages)}
                        className="px-3 py-1 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
                      >
                        {totalPages}
                      </button>
                    </>
                  )}
                </div>

                <button
                  onClick={() => onPageChange?.(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    );
  }
);

DataTable.displayName = 'DataTable';
