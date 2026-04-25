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
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { usePageTitle } from '../hooks/usePageTitle';

export const TenantsListPage = () => {
  usePageTitle('Tenants');
  const [tenants, setTenants] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchTenants = async () => {
    setLoading(true);
    try {
      const res = await apiClient.getTenants({ page: 0, size: 100 });
      setTenants(res.data || []);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load tenants');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTenants();
  }, []);

  const handleDelete = async (tenant: any) => {
    if (!window.confirm(`Delete tenant "${tenant.name}"? This will permanently drop the tenant schema and all its data. This cannot be undone.`)) return;
    setDeletingId(tenant.id);
    try {
      await apiClient.deleteTenant(tenant.id);
      await fetchTenants();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete tenant');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>Tenants</h1>
          <Link to="/admin/tenants/new" className="btn btn-primary">+ New Tenant</Link>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={4}>Loading...</td></tr>
            ) : tenants.length === 0 ? (
              <tr><td colSpan={4}>No tenants found</td></tr>
            ) : (
              tenants.map((t: any) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td>{t.name}</td>
                  <td>{t.status}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <Link to={`/admin/tenants/${t.id}/edit`} className="btn btn-sm btn-secondary">Edit</Link>
                      <Link to={`/admin/tenants/${t.id}/users`} className="btn btn-sm btn-info">Users</Link>
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => handleDelete(t)}
                        disabled={deletingId === t.id}
                      >
                        {deletingId === t.id ? 'Deleting...' : 'Delete'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};
