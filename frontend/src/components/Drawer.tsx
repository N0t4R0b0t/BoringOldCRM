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
import React, { useEffect, useRef } from 'react';
import { PolicyInfoButton } from './PolicyInfoButton';

interface DrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  title: string;
  children: React.ReactNode;
  width?: string;
  mode?: 'view' | 'edit';
  entityType?: string;
}

export const Drawer: React.FC<DrawerProps> = ({
  isOpen,
  onClose,
  onEdit,
  onDelete,
  title,
  children,
  width = '1020px',
  mode = 'view',
  entityType,
}) => {
  const drawerRef = useRef<HTMLDivElement>(null);
  const isEditMode = mode === 'edit';

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const zIndex = isEditMode ? 'z-[60]' : 'z-50';
  const headerBg = isEditMode ? 'bg-blue-50 dark:bg-blue-950' : 'bg-gray-50 dark:bg-gray-900';
  const overlayOpacity = isEditMode ? 'bg-black/40' : 'bg-black/30';

  return (
    <div className={`fixed inset-0 ${zIndex} overflow-hidden`}>
      <div
        className={`absolute inset-0 ${overlayOpacity} backdrop-blur-sm transition-opacity duration-300 ease-out animate-in fade-in`}
        onClick={onClose}
      />

      <div className="absolute inset-y-0 right-0 flex max-w-full pl-10 pointer-events-none">
        <div
          ref={drawerRef}
          className="pointer-events-auto w-screen transform transition-all duration-300 ease-out translate-x-0 animate-in slide-in-from-right"
          style={{ width: `min(${width}, 100vw)`, minWidth: 0 }}
        >
          <div className="flex h-full flex-col overflow-hidden bg-white dark:bg-gray-800 shadow-2xl">
            {/* Header */}
            <div className={`h-16 flex items-center justify-between px-6 border-b border-gray-200 dark:border-gray-700 ${headerBg}`}>
              <div className="flex items-center gap-3">
                {isEditMode && (
                  <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center text-white font-bold">
                    ✏️
                  </div>
                )}
                <div>
                  <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h2>
                  {isEditMode && <p className="text-xs text-blue-600 dark:text-blue-400 font-medium">Editing</p>}
                </div>
              </div>

              <div className="flex items-center gap-2">
                {isEditMode && entityType && (
                  <PolicyInfoButton entityType={entityType} />
                )}
                {!isEditMode && onEdit && (
                  <button
                    type="button"
                    title="Edit"
                    onClick={onEdit}
                    className="inline-flex items-center justify-center w-9 h-9 rounded-md bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
                  >
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                    </svg>
                  </button>
                )}
                {!isEditMode && onDelete && (
                  <button
                    type="button"
                    title="Delete"
                    onClick={onDelete}
                    className="inline-flex items-center justify-center w-9 h-9 rounded-md bg-red-600 text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 transition-colors"
                  >
                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                )}
                <button
                  type="button"
                  title="Close"
                  onClick={onClose}
                  className="inline-flex items-center justify-center w-9 h-9 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:text-gray-500 dark:hover:text-gray-300 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
                >
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto px-6 py-4">
              {children}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
