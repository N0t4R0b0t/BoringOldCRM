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
import { useEffect, useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

const DEFAULT_PRIMARY = '#3B82F6';
const MAX_LOGO_BYTES = 10 * 1024 * 1024; // 10 MB

export const CurrentTenantSettingsPage = () => {
  usePageTitle('Settings');
  const navigate = useNavigate();
  const { currentTenant, currentRole, logout, setTenantSettings } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [tenantName, setTenantName] = useState('');
  const [useSimple, setUseSimple] = useState(true);
  const [rawJson, setRawJson] = useState('{}');

  // Branding settings
  const [primaryColor, setPrimaryColor] = useState(DEFAULT_PRIMARY);
  const [logoUrl, setLogoUrl] = useState<string>('');
  const [logoPreview, setLogoPreview] = useState<string>('');
  const logoInputRef = useRef<HTMLInputElement>(null);

  // Other simple settings
  const [language, setLanguage] = useState('en');
  const [orgBio, setOrgBio] = useState('');
  const [entityLabels, setEntityLabels] = useState<Record<string, string>>({
    Customer: '', Contact: '', Opportunity: '', Activity: '', CustomRecord: '', Order: '', Invoice: '',
  });
  const [hiddenModules, setHiddenModules] = useState<Set<string>>(new Set(['Order', 'Invoice']));
  const [reportBuilderEnabled, setReportBuilderEnabled] = useState(false);

  useEffect(() => {
    if (!currentTenant) return;
    setLoading(true);
    apiClient.getTenant(currentTenant.id)
      .then(res => {
        const data = res.data;
        const settings = data.settings || {};
        setTenantName(data.name || '');
        setRawJson(JSON.stringify(settings, null, 2));
        if (settings.language) setLanguage(settings.language);
        if (settings.orgBio) setOrgBio(settings.orgBio);
        if (settings.entityLabels) setEntityLabels(prev => ({ ...prev, ...settings.entityLabels }));
        if (settings.hiddenModules && Array.isArray(settings.hiddenModules)) {
          setHiddenModules(new Set(settings.hiddenModules));
        }
        setReportBuilderEnabled(settings.reportBuilderEnabled === true);
        if (settings.primaryColor) setPrimaryColor(settings.primaryColor);
        if (settings.logoUrl) {
          setLogoUrl(settings.logoUrl);
          setLogoPreview(settings.logoUrl);
        }
      })
      .catch(e => setError(e.response?.data?.message || 'Failed to load tenant settings'))
      .finally(() => setLoading(false));
  }, [currentTenant]);

  const handleLogoFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > MAX_LOGO_BYTES) {
      setError(`Logo file must be smaller than ${MAX_LOGO_BYTES / 1024 / 1024} MB`);
      return;
    }
    try {
      setLoading(true);
      setError('');
      const res = await apiClient.uploadLogo(file);
      const dataUrl = res.data.logoUrl;
      setLogoUrl(dataUrl);
      setLogoPreview(dataUrl);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upload logo');
    } finally {
      setLoading(false);
    }
  };

  const removeLogo = () => {
    setLogoUrl('');
    setLogoPreview('');
    if (logoInputRef.current) logoInputRef.current.value = '';
  };

  const handleResetOnboarding = async () => {
    if (!currentTenant) return;
    if (!window.confirm('Reset the onboarding wizard? It will appear again on the next dashboard load.')) {
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const settings = JSON.parse(rawJson);
      settings.onboardingCompleted = false;
      await apiClient.updateTenantSettings(currentTenant.id, settings);
      setSuccess('Onboarding wizard has been reset and will appear on your next visit to the dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reset onboarding wizard');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentTenant) return;

    setLoading(true);
    setError('');
    setSuccess('');

    let settings: Record<string, any>;
    if (useSimple) {
      const filteredLabels = Object.fromEntries(
        Object.entries(entityLabels).filter(([, v]) => v.trim() !== '')
      );
      settings = {
        language, primaryColor, logoUrl: logoUrl || undefined, orgBio: orgBio || undefined,
        entityLabels: Object.keys(filteredLabels).length > 0 ? filteredLabels : undefined,
        hiddenModules: Array.from(hiddenModules),
        reportBuilderEnabled,
      };
    } else {
      try {
        settings = JSON.parse(rawJson);
      } catch {
        setError('Settings JSON is invalid');
        setLoading(false);
        return;
      }
    }

    try {
      await apiClient.updateTenant(currentTenant.id, { name: tenantName, settings });
      setTenantSettings(settings);
      setSuccess('Settings saved successfully');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save settings');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTenant = async () => {
    if (!currentTenant || deleteConfirm !== currentTenant.name) return;
    setDeleting(true);
    setError('');
    try {
      await apiClient.deleteTenant(currentTenant.id);
      logout();
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete tenant');
      setDeleting(false);
    }
  };

  const role = currentRole?.toLowerCase();
  const canDelete = role === 'admin' || role === 'system_admin';

  if (!currentTenant) {
    return (
      <Layout>
        <div className="page">
          <div className="error-banner">No active tenant selected.</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>Tenant Settings</h1>
          <Link to="/admin" className="btn btn-secondary">Back to Admin</Link>
        </div>

        {error && <div className="error-banner">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        <div className="form-container">
          <form onSubmit={handleSubmit} className="card">
            <div className="form-group">
              <label htmlFor="tenantName">Tenant Name *</label>
              <input
                id="tenantName"
                value={tenantName}
                onChange={e => setTenantName(e.target.value)}
                className="form-input"
                required
              />
            </div>

            <div className="form-group">
              <label>Settings Editor</label>
              <div className="flex gap-3 mb-2">
                <label><input type="radio" checked={useSimple} onChange={() => setUseSimple(true)} /> Simple</label>
                <label><input type="radio" checked={!useSimple} onChange={() => setUseSimple(false)} /> JSON</label>
              </div>

              {useSimple ? (
                <div className="grid gap-5">

                  {/* Branding section */}
                  <div className="border border-gray-200 rounded-lg p-4 dark:border-gray-700">
                    <h3 className="text-sm font-semibold mb-4 text-gray-700 dark:text-gray-300">
                      Branding
                    </h3>

                    {/* Primary color */}
                    <div className="mb-4">
                      <label className="form-label">Primary Color</label>
                      <div className="flex items-center gap-3">
                        <input
                          type="color"
                          value={primaryColor}
                          onChange={e => setPrimaryColor(e.target.value)}
                          className="w-12 h-10 rounded-md border border-gray-300 cursor-pointer p-0.5"
                        />
                        <input
                          type="text"
                          value={primaryColor}
                          onChange={e => {
                            const v = e.target.value;
                            if (/^#[0-9A-Fa-f]{0,6}$/.test(v)) setPrimaryColor(v);
                          }}
                          className="form-input w-28"
                          placeholder="#3B82F6"
                        />
                        <button
                          type="button"
                          onClick={() => setPrimaryColor(DEFAULT_PRIMARY)}
                          className="btn btn-secondary btn-sm"
                        >
                          Reset
                        </button>
                        <div
                          className="w-20 h-9 rounded-md flex items-center justify-center text-white text-xs font-semibold"
                          style={{ backgroundColor: primaryColor }}
                        >
                          Preview
                        </div>
                      </div>
                      <p className="text-xs text-gray-500 mt-1.5">
                        Used for the sidebar icon, nav highlights, and primary buttons.
                      </p>
                    </div>

                    {/* Logo upload */}
                    <div>
                      <label className="form-label">Company Logo</label>
                      <div className="flex items-center gap-4 flex-wrap">
                        {logoPreview && (
                          <div className="relative inline-block">
                            <img
                              src={logoPreview}
                              alt="Logo preview"
                              className="w-16 h-16 object-contain rounded-lg border border-gray-200 bg-gray-50"
                            />
                            <button
                              type="button"
                              onClick={removeLogo}
                              className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-red-500 text-white border-none cursor-pointer text-[0.7rem] flex items-center justify-center leading-none"
                              title="Remove logo"
                            >
                              ✕
                            </button>
                          </div>
                        )}
                        <div>
                          <input
                            ref={logoInputRef}
                            type="file"
                            accept="image/png,image/jpeg,image/gif,image/webp,image/svg+xml"
                            onChange={handleLogoFile}
                            className="hidden"
                            id="logoUpload"
                          />
                          <label htmlFor="logoUpload" className="btn btn-secondary cursor-pointer">
                            {logoPreview ? 'Change logo' : 'Upload logo'}
                          </label>
                          <p className="text-xs text-gray-500 mt-1">
                            PNG, JPEG, SVG or WebP · Max 512 KB · Appears in the sidebar header.
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Other settings */}
                  <div>
                    <label className="form-label">Language</label>
                    <select value={language} onChange={(e) => setLanguage(e.target.value)} className="form-input">
                      <option value="en">English</option>
                      <option value="fr">Français</option>
                      <option value="es">Español</option>
                    </select>
                  </div>

                  {/* Entity Labels */}
                  <div className="border border-gray-200 rounded-lg p-4 dark:border-gray-700">
                    <h3 className="text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">
                      Entity Labels
                    </h3>
                    <p className="text-xs text-gray-500 mb-3">
                      Rename entity types for your organization. Leave blank to use the default name. The UI will show these labels throughout the app.
                    </p>
                    <div className="grid grid-cols-2 gap-3">
                      {(['Customer', 'Contact', 'Opportunity', 'Activity', 'CustomRecord', 'Order', 'Invoice'] as const).map((key) => (
                        <div key={key}>
                          <label className="block text-xs font-medium mb-1 text-gray-500">
                            {key}
                          </label>
                          <input
                            type="text"
                            value={entityLabels[key] ?? ''}
                            onChange={(e) => setEntityLabels(prev => ({ ...prev, [key]: e.target.value }))}
                            className="form-input"
                            placeholder={key}
                          />
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Hidden Modules */}
                  <div className="border border-gray-200 rounded-lg p-4 dark:border-gray-700">
                    <h3 className="text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">
                      Visibility
                    </h3>
                    <p className="text-xs text-gray-500 mb-3">
                      Hide modules you don't use. Hidden modules won't appear in the sidebar navigation.
                    </p>
                    <div className="space-y-2">
                      {(['Opportunity', 'Activity', 'CustomRecord', 'Order', 'Invoice'] as const).map((module) => (
                        <label key={module} className="flex items-center gap-2">
                          <input
                            type="checkbox"
                            checked={hiddenModules.has(module)}
                            onChange={(e) => {
                              const newHidden = new Set(hiddenModules);
                              if (e.target.checked) {
                                newHidden.add(module);
                              } else {
                                newHidden.delete(module);
                              }
                              setHiddenModules(newHidden);
                            }}
                            className="w-4 h-4 rounded"
                          />
                          <span className="text-sm text-gray-700 dark:text-gray-300">
                            Hide {module}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>

                  {/* Beta features */}
                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-2">
                      Beta Features
                      <span className="text-[9px] font-bold uppercase tracking-wide px-1.5 py-0.5 rounded bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-300">Beta</span>
                    </h3>
                    <p className="text-xs text-gray-500 mb-3">
                      These features are experimental. They may change or be removed in future versions.
                    </p>
                    <label className="flex items-start gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={reportBuilderEnabled}
                        onChange={e => setReportBuilderEnabled(e.target.checked)}
                        className="w-4 h-4 rounded mt-0.5"
                      />
                      <div>
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Report Builder</span>
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                          AI-assisted report and slide deck generator with live preview and style customization. Adds a "Report Builder" item to the sidebar.
                        </p>
                      </div>
                    </label>
                  </div>

                  {/* Organization bio */}
                  <div>
                    <label className="form-label">Organization Bio</label>
                    <textarea
                      value={orgBio}
                      onChange={(e) => setOrgBio(e.target.value)}
                      className="form-input"
                      rows={5}
                      placeholder="Describe your organization, industry, and key business context. The AI assistant will use this to give more relevant answers."
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Used by the AI assistant to contextualize all queries for your organization.
                    </p>
                  </div>
                </div>
              ) : (
                <textarea
                  value={rawJson}
                  onChange={e => setRawJson(e.target.value)}
                  className="form-input"
                  rows={10}
                />
              )}
            </div>

            <div className="form-actions">
              <button type="submit" disabled={loading} className="btn btn-primary">
                {loading ? 'Saving...' : 'Save Settings'}
              </button>
              <button type="button" onClick={() => navigate('/admin')} className="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <div className="form-container mt-8 border border-blue-200 rounded-lg p-5 bg-blue-50 dark:bg-blue-950/20 dark:border-blue-900">
          <h2 className="text-blue-600 mt-0 mb-2 text-lg font-semibold dark:text-blue-400">Setup Wizard</h2>
          <p className="text-gray-600 text-sm mb-4 dark:text-gray-400">
            Re-enable the onboarding wizard to guide you through setting up custom fields, policies, and sample data again.
          </p>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleResetOnboarding}
            disabled={loading}
          >
            {loading ? 'Resetting...' : 'Reset Onboarding Wizard'}
          </button>
        </div>

        {canDelete && (
          <div className="form-container mt-8 border border-red-200 rounded-lg p-5 bg-red-50 dark:bg-red-950/20 dark:border-red-900">
            <h2 className="text-red-600 mt-0 mb-2 text-lg font-semibold dark:text-red-400">Danger Zone</h2>
            <p className="text-gray-600 text-sm mb-4 dark:text-gray-400">
              Permanently delete this tenant and all its data. This will drop the entire tenant schema including all customers, contacts, opportunities, activities, and custom fields. <strong>This cannot be undone.</strong>
            </p>
            <div className="form-group">
              <label htmlFor="deleteConfirm" className="form-label">
                Type <strong>{currentTenant.name}</strong> to confirm deletion:
              </label>
              <input
                id="deleteConfirm"
                value={deleteConfirm}
                onChange={(e) => setDeleteConfirm(e.target.value)}
                className="form-input mt-1.5"
                placeholder={currentTenant.name}
              />
            </div>
            <button
              type="button"
              className="btn btn-danger"
              onClick={handleDeleteTenant}
              disabled={deleting || deleteConfirm !== currentTenant.name}
            >
              {deleting ? 'Deleting...' : 'Delete this tenant'}
            </button>
          </div>
        )}
      </div>
    </Layout>
  );
};
