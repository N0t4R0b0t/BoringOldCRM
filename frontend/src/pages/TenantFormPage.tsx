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
import { useNavigate, useParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { TenantUsersList } from '../components/TenantUsersList';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

export const TenantFormPage = () => {
  usePageTitle('Tenant');
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', settings: '{}' });
  const [useSimple, setUseSimple] = useState(true);
  const [theme, setTheme] = useState('light');
  const [language, setLanguage] = useState('en');
  const [activeTab, setActiveTab] = useState('details');
  const { switchTenant } = useAuthStore();

  useEffect(() => {
    if (isEdit && id) {
      const fetchTenant = async () => {
        setLoading(true);
        try {
          const res = await apiClient.getTenant(parseInt(id));
          const data = res.data;
          const settings = data.settings || {};
          setForm({ name: data.name || '', settings: JSON.stringify(settings || {}, null, 2) });
          // populate simple fields if available
          if (settings.theme) setTheme(settings.theme);
          if (settings.language) setLanguage(settings.language);
        } catch (e: any) {
          setError(e.response?.data?.message || 'Failed to load tenant');
        } finally {
          setLoading(false);
        }
      };
      fetchTenant();
    }
  }, [isEdit, id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target as any;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const payload: any = { name: form.name };
      if (useSimple) {
        payload.settings = { theme, language };
      } else {
        try {
          payload.settings = JSON.parse(form.settings);
        } catch (err) {
          setError('Settings JSON is invalid');
          setLoading(false);
          return;
        }
      }

      if (isEdit && id) {
        await apiClient.updateTenant(parseInt(id), payload);
      } else {
        await apiClient.createTenant({ name: form.name });
      }
      navigate('/admin/tenants');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save tenant');
    } finally {
      setLoading(false);
    }
  };

  const handleSwitchToTenant = async () => {
    if (id) {
      if (window.confirm('This will switch your active session to this tenant. Continue?')) {
        await switchTenant(parseInt(id));
        navigate('/admin/custom-fields');
      }
    }
  };

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>{isEdit ? 'Edit Tenant' : 'New Tenant'}</h1>
        </div>

        {error && <div className="error-banner">{error}</div>}

        {isEdit && (
          <div className="tabs mb-4">
            <button 
              className={`tab ${activeTab === 'details' ? 'active' : ''}`}
              onClick={() => setActiveTab('details')}
            >
              Details
            </button>
            <button 
              className={`tab ${activeTab === 'users' ? 'active' : ''}`}
              onClick={() => setActiveTab('users')}
            >
              Users
            </button>
            <button 
              className={`tab ${activeTab === 'fields' ? 'active' : ''}`}
              onClick={() => setActiveTab('fields')}
            >
              Custom Fields
            </button>
          </div>
        )}

        {activeTab === 'details' && (
          <div className="form-container">
            <form onSubmit={handleSubmit} className="form">
              <div className="form-group">
                <label htmlFor="name">Name *</label>
                <input id="name" name="name" value={form.name} onChange={handleChange} className="form-input" required />
              </div>

              <div className="form-group">
                <label>Settings Editor</label>
                <div style={{ display: 'flex', gap: '12px', marginBottom: '8px' }}>
                  <label><input type="radio" checked={useSimple} onChange={() => setUseSimple(true)} /> Simple</label>
                  <label><input type="radio" checked={!useSimple} onChange={() => setUseSimple(false)} /> JSON</label>
                </div>

                {useSimple ? (
                  <div style={{ display: 'grid', gap: '8px' }}>
                    <div>
                      <label>Theme</label>
                      <select value={theme} onChange={(e) => setTheme(e.target.value)} className="form-input">
                        <option value="light">Light</option>
                        <option value="dark">Dark</option>
                      </select>
                    </div>
                    <div>
                      <label>Language</label>
                      <select value={language} onChange={(e) => setLanguage(e.target.value)} className="form-input">
                        <option value="en">English</option>
                        <option value="fr">Français</option>
                        <option value="es">Español</option>
                      </select>
                    </div>
                  </div>
                ) : (
                  <textarea id="settings" name="settings" value={form.settings} onChange={handleChange} className="form-input" rows={8} />
                )}
              </div>

              <div className="form-actions">
                <button type="submit" disabled={loading} className="btn btn-primary">{loading ? 'Saving...' : 'Save'}</button>
                <button type="button" onClick={() => navigate('/admin/tenants')} className="btn btn-secondary">Cancel</button>
              </div>
            </form>
          </div>
        )}

        {activeTab === 'users' && id && (
          <TenantUsersList tenantId={parseInt(id)} />
        )}

        {activeTab === 'fields' && (
          <div className="content-card">
            <h3>Manage Custom Fields</h3>
            <p className="mb-4">
              Custom fields are managed within the context of the tenant. 
              To manage fields for this tenant, you need to switch your active session to this tenant.
            </p>
            <button onClick={handleSwitchToTenant} className="btn btn-primary">
              Switch to Tenant & Manage Fields
            </button>
          </div>
        )}
      </div>
    </Layout>
  );
};
