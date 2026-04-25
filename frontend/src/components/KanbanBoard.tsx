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
import React, { useMemo, useState } from 'react';

export interface KanbanColumn {
  key: string;
  label: string;
  color: string;
}

interface KanbanBoardProps<T> {
  items: T[];
  groupByKey: string;
  columns: KanbanColumn[];
  onCardClick?: (item: T) => void;
  onCardMove?: (itemId: number, newColumnKey: string) => void;
  nameKey: string;
  valueKey?: string;
  badgeKey?: string;
}

const COLUMN_COLOR_MAP: Record<string, string> = {
  blue: 'bg-blue-500',
  green: 'bg-green-500',
  yellow: 'bg-yellow-400',
  red: 'bg-red-500',
  gray: 'bg-gray-400',
  purple: 'bg-purple-500',
  orange: 'bg-orange-500',
};

const KanbanCard = React.forwardRef<
  HTMLDivElement,
  {
    item: any;
    nameKey: string;
    valueKey?: string;
    badgeKey?: string;
    colorDot: string;
    onCardClick?: (item: any) => void;
    isDragging?: boolean;
  }
>(({ item, nameKey, valueKey, badgeKey, colorDot, onCardClick, isDragging }, ref) => {
  const name = item[nameKey] || '—';
  const value = valueKey ? item[valueKey] : undefined;
  const badge = badgeKey ? item[badgeKey] : undefined;

  return (
    <div
      ref={ref}
      draggable
      onClick={() => onCardClick?.(item)}
      className={`p-3 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-sm cursor-move hover:shadow-md transition-shadow ${
        isDragging ? 'opacity-50' : ''
      }`}
    >
      <div className="flex items-start gap-2 mb-2">
        <span className={`${colorDot} w-2 h-2 rounded-full shrink-0 mt-1.5`} />
        <span className="font-medium text-sm text-gray-900 dark:text-white flex-1 break-words">{name}</span>
      </div>
      {value && (
        <div className="text-xs text-gray-500 dark:text-gray-400 mb-2">
          {typeof value === 'number' ? value.toLocaleString() : value}
        </div>
      )}
      {badge && (
        <div className="text-xs font-semibold text-blue-600 dark:text-blue-300">
          {badge}
        </div>
      )}
    </div>
  );
});

KanbanCard.displayName = 'KanbanCard';

export const KanbanBoard = React.forwardRef<HTMLDivElement, KanbanBoardProps<any>>(
  ({ items, groupByKey, columns, onCardClick, onCardMove, nameKey, valueKey, badgeKey }, ref) => {
    const [draggedItemId, setDraggedItemId] = useState<number | null>(null);
    const [dragOverColumn, setDragOverColumn] = useState<string | null>(null);

    const groupedItems = useMemo(() => {
      const map = new Map<string, any[]>();
      columns.forEach((col) => map.set(col.key, []));
      items.forEach((item) => {
        const columnKey = item[groupByKey];
        const existing = map.get(columnKey) || [];
        map.set(columnKey, [...existing, item]);
      });
      return map;
    }, [items, groupByKey, columns]);

    const handleDragStart = (e: React.DragEvent<HTMLDivElement>, itemId: number) => {
      setDraggedItemId(itemId);
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('itemId', String(itemId));
    };

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
    };

    const handleDropColumn = (columnKey: string, e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      const itemIdStr = e.dataTransfer.getData('itemId');
      if (itemIdStr && onCardMove) {
        const itemId = parseInt(itemIdStr, 10);
        onCardMove(itemId, columnKey);
        setDraggedItemId(null);
      }
    };

    const handleDragLeave = () => {
      setDragOverColumn(null);
    };

    return (
      <div ref={ref} className="flex gap-4 overflow-x-auto pb-4">
        {columns.map((column) => {
          const columnItems = groupedItems.get(column.key) || [];
          const colorClass = COLUMN_COLOR_MAP[column.color] || COLUMN_COLOR_MAP.gray;

          return (
            <div
              key={column.key}
              className="flex-shrink-0 w-72 flex flex-col"
            >
              <div className="flex items-center gap-2 mb-3">
                <span className={`${colorClass} w-3 h-3 rounded-full`} />
                <h3 className="font-semibold text-gray-900 dark:text-white">{column.label}</h3>
                <span className="ml-auto text-xs font-medium text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">
                  {columnItems.length}
                </span>
              </div>
              <div
                onDragOver={handleDragOver}
                onDrop={(e) => handleDropColumn(column.key, e)}
                onDragLeave={handleDragLeave}
                className={`flex-1 overflow-y-auto p-2 space-y-2 bg-gray-50 dark:bg-gray-800/50 rounded-lg border-2 border-dashed transition-colors ${
                  dragOverColumn === column.key
                    ? 'border-blue-400 dark:border-blue-500 bg-blue-50 dark:bg-blue-900/10'
                    : 'border-gray-200 dark:border-gray-700'
                }`}
              >
                {columnItems.length === 0 ? (
                  <div className="text-center py-8 text-xs text-gray-400 dark:text-gray-500">
                    No items
                  </div>
                ) : (
                  columnItems.map((item) => (
                    <div
                      key={item.id}
                      onDragStart={(e) => handleDragStart(e, item.id)}
                      onDragEnd={() => setDraggedItemId(null)}
                    >
                      <KanbanCard
                        item={item}
                        nameKey={nameKey}
                        valueKey={valueKey}
                        badgeKey={badgeKey}
                        colorDot={colorClass}
                        onCardClick={onCardClick}
                        isDragging={draggedItemId === item.id}
                      />
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  }
);

KanbanBoard.displayName = 'KanbanBoard';
