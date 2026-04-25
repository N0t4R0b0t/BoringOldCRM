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
import { apiClient } from '../api/apiClient';
import '../styles/BulkActions.css';

interface BulkActionsProps {
  entityType: string;
  selectedIds: number[];
  onComplete: () => void;
  onError: (error: string) => void;
}

export const BulkActions: React.FC<BulkActionsProps> = ({
  entityType,
  selectedIds,
  onComplete,
  onError,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleAction = async (action: string) => {
    if (selectedIds.length === 0) {
      onError('No items selected');
      return;
    }

    setIsLoading(true);
    try {
      const updates = {};
      if (action === 'updateStatus') {
        const status = prompt('Enter new status:');
        if (!status) {
          setIsLoading(false);
          return;
        }
        Object.assign(updates, { status });
      }

      await apiClient.executeBulkOperation({
        entityType,
        action,
        entityIds: selectedIds,
        updates,
      });

      onComplete();
      setIsOpen(false);
    } catch (err: any) {
      onError(err.response?.data?.message || 'Bulk operation failed');
    } finally {
      setIsLoading(false);
    }
  };

  if (selectedIds.length === 0) return null;

  return (
    <div className="bulk-actions-container">
      <div className="bulk-actions-info">
        <span className="selection-count">{selectedIds.length} selected</span>
        <div className="bulk-actions-menu">
          <button
            className="bulk-actions-toggle"
            onClick={() => setIsOpen(!isOpen)}
            disabled={isLoading}
          >
            Actions ▼
          </button>

          {isOpen && (
            <div className="bulk-actions-dropdown">
              <button
                className="action-item"
                onClick={() => handleAction('updateStatus')}
                disabled={isLoading}
              >
                📝 Update Status
              </button>
              <button
                className="action-item"
                onClick={() => handleAction('export')}
                disabled={isLoading}
              >
                📥 Export
              </button>
              <button
                className="action-item action-danger"
                onClick={() => {
                  if (
                    window.confirm(
                      `Are you sure you want to delete ${selectedIds.length} items?`
                    )
                  ) {
                    handleAction('delete');
                  }
                }}
                disabled={isLoading}
              >
                🗑️ Delete
              </button>
            </div>
          )}
        </div>
      </div>

      {isLoading && (
        <div className="bulk-actions-progress">
          <div className="spinner"></div>
          <span>Processing...</span>
        </div>
      )}
    </div>
  );
};
