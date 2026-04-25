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
import React, { useState, useRef, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';

interface TenantSwitcherProps {
  compact?: boolean;
}

export const TenantSwitcher: React.FC<TenantSwitcherProps> = ({ compact = false }) => {
  const { currentTenant, availableTenants, switchTenant, isLoading, refreshMemberships } = useAuthStore();
  const [isOpen, setIsOpen] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [dropdownPos, setDropdownPos] = useState<{ top: number; left: number } | null>(null);

  // Refresh memberships whenever the dropdown opens so tenants created via the
  // assistant (or any other out-of-band path) appear immediately.
  useEffect(() => {
    if (isOpen) refreshMemberships();
  }, [isOpen]);

  // Poll memberships on mount so a tenant created by the assistant shows up
  // without requiring a page reload.
  useEffect(() => {
    refreshMemberships();
  }, []);

  if (!currentTenant) return null;

  if (availableTenants.length <= 1) {
    return (
      <div className={`flex items-center gap-2 px-3 py-2 text-sm text-gray-600 dark:text-gray-300 ${compact ? 'justify-center' : ''}`}>
        <span className="text-lg">🏢</span>
        {!compact && <span className="font-medium truncate">{currentTenant.name}</span>}
      </div>
    );
  }

  const handleSwitch = async (tenantId: number) => {
    if (tenantId === currentTenant.id) return;
    setLastError(null);
    try {
      await switchTenant(tenantId);
      setIsOpen(false);
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to switch tenant';
      setLastError(errorMsg);
      console.error('Failed to switch tenant:', error);
    }
  };

  const getRoleBadge = (role: string) => {
    const normalizedRole = role?.toUpperCase();
    if (normalizedRole === 'SYSTEM_ADMIN') {
      return (
        <span title="System Admin" className="text-lg">
          👑
        </span>
      );
    }
    if (normalizedRole === 'ADMIN') {
      return (
        <span title="Admin" className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-200 text-xs font-semibold">
          ⚙️
        </span>
      );
    }
    return null;
  };

  // Calculate dropdown position when it opens
  useEffect(() => {
    if (isOpen && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      const dropdownWidth = 256; // w-64 = 16rem = 256px
      const dropdownHeight = 150; // approximate height

      // Vertical positioning: below or above button
      const spaceBelow = window.innerHeight - rect.bottom;
      const spaceAbove = rect.top;

      let top: number;
      if (spaceBelow > dropdownHeight) {
        // Room below
        top = rect.bottom + 8;
      } else if (spaceAbove > dropdownHeight) {
        // Room above
        top = rect.top - dropdownHeight - 8;
      } else {
        // Not enough space either way, position at bottom with scrolling
        top = rect.bottom + 8;
      }

      // Horizontal positioning: align with button start, but keep on screen
      let left = rect.left;
      if (left + dropdownWidth > window.innerWidth) {
        // Would go off right edge, align to right edge instead
        left = window.innerWidth - dropdownWidth - 8;
      }
      left = Math.max(8, left); // Don't go off left edge

      setDropdownPos({ top, left });
    }
  }, [isOpen]);

  return (
    <div className="relative">
      <button
        ref={buttonRef}
        className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-white dark:hover:bg-gray-700 hover:shadow-sm transition-all text-left border border-transparent hover:border-gray-200 dark:hover:border-gray-600 ${
          compact ? 'justify-center' : ''
        }`}
        onClick={() => setIsOpen(!isOpen)}
        disabled={isLoading}
        title={compact ? currentTenant.name : ''}
      >
        <span className="text-lg shrink-0">🏢</span>
        {!compact && (
          <>
            <div className="flex-1 min-w-0">
              <div className="text-xs text-gray-500 dark:text-gray-400 font-medium uppercase tracking-wider">Tenant</div>
              <div className="text-sm font-semibold text-gray-900 dark:text-white truncate">{currentTenant.name}</div>
            </div>
            <span className="text-xs text-gray-400">▼</span>
          </>
        )}
      </button>

      {isOpen && dropdownPos && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
          <div
            ref={dropdownRef}
            className="fixed w-64 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden"
            style={{
              top: `${dropdownPos.top}px`,
              left: `${dropdownPos.left}px`,
            }}
          >
            <div className="px-4 py-2 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600 text-xs font-semibold text-gray-500 dark:text-gray-300 uppercase tracking-wider">
              Switch Tenant
            </div>
            {lastError && (
              <div className="px-4 py-2 bg-red-50 dark:bg-red-900 border-b border-red-200 dark:border-red-800">
                <p className="text-xs text-red-700 dark:text-red-200">{lastError}</p>
              </div>
            )}
            <ul className="max-h-64 overflow-y-auto py-1">
              {availableTenants.map((tenant) => (
                <li key={tenant.id}>
                  <button
                    className={`w-full flex items-center justify-between gap-2 px-4 py-2 text-sm text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors ${
                      tenant.id === currentTenant.id ? 'bg-blue-50 dark:bg-blue-900 text-blue-700 dark:text-blue-300 font-medium' : 'text-gray-700 dark:text-gray-300'
                    }`}
                    onClick={() => handleSwitch(tenant.id)}
                    disabled={isLoading}
                  >
                    <span className="truncate flex-1">{tenant.name}</span>
                    <div className="flex items-center gap-1 shrink-0">
                      {getRoleBadge(tenant.role)}
                      {tenant.id === currentTenant.id && <span className="text-blue-600 dark:text-blue-400">✓</span>}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  );
};
