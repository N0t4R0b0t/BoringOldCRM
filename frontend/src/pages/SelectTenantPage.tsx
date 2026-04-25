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
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

export const SelectTenantPage = () => {
  usePageTitle('Select Workspace');
  const navigate = useNavigate();
  const { requiresTenantSelection, availableTenants, switchTenant, isLoading } = useAuthStore();

  useEffect(() => {
    // Redirect to login if not in tenant selection mode
    if (!requiresTenantSelection) {
      navigate('/login');
    }
  }, [requiresTenantSelection, navigate]);

  if (!requiresTenantSelection || availableTenants.length === 0) {
    return null;
  }

  const handleTenantSelect = async (tenantId: number) => {
    await switchTenant(tenantId);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f3f4f6' }}>
      <div style={{ textAlign: 'center', backgroundColor: 'white', padding: '48px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', maxWidth: '400px' }}>
        <h1 style={{ marginBottom: '12px', fontSize: '28px', fontWeight: 'bold', color: '#111827' }}>Select a Workspace</h1>
        <p style={{ marginBottom: '32px', color: '#6b7280', fontSize: '14px' }}>
          You belong to multiple workspaces. Please select which one you'd like to access.
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {availableTenants.map((tenant) => (
            <button
              key={tenant.id}
              onClick={() => handleTenantSelect(tenant.id)}
              disabled={isLoading}
              style={{
                padding: '12px 16px',
                backgroundColor: '#3b82f6',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontSize: '14px',
                fontWeight: '500',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                opacity: isLoading ? 0.6 : 1,
                transition: 'background-color 0.2s',
              }}
              onMouseEnter={(e) => {
                if (!isLoading) {
                  (e.target as HTMLButtonElement).style.backgroundColor = '#2563eb';
                }
              }}
              onMouseLeave={(e) => {
                if (!isLoading) {
                  (e.target as HTMLButtonElement).style.backgroundColor = '#3b82f6';
                }
              }}
            >
              <div style={{ fontWeight: '600', marginBottom: '4px' }}>{tenant.name}</div>
              <div style={{ fontSize: '12px', opacity: 0.8 }}>Role: {tenant.role}</div>
            </button>
          ))}
        </div>

        {isLoading && (
          <p style={{ marginTop: '20px', color: '#6b7280', fontSize: '14px' }}>
            Loading...
          </p>
        )}
      </div>
    </div>
  );
};
