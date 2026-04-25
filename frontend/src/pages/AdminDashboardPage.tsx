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
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

interface AdminStats {
  customerCount?: number;
  contactCount?: number;
  opportunityCount?: number;
  activityCount?: number;
  tenantCount?: number;
  userCount?: number;
}

export const AdminDashboardPage = () => {
  usePageTitle('Admin Dashboard');
  const { currentTenant, currentRole, user } = useAuthStore();
  const isSystemAdmin = currentRole === 'system_admin';

  const [_stats, setStats] = useState<AdminStats>({});
  const [_isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchStats = async () => {
      setIsLoading(true);
      try {
        const results: AdminStats = {};

        // Fetch entity counts
        const customersRes = await apiClient.getCustomers({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } }));
        const contactsRes = await apiClient.getContacts({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } }));
        const opportunitiesRes = await apiClient.getOpportunities({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } }));
        const activitiesRes = await apiClient.getActivities({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } }));

        results.customerCount = customersRes.data?.totalElements || 0;
        results.contactCount = contactsRes.data?.totalElements || 0;
        results.opportunityCount = opportunitiesRes.data?.totalElements || 0;
        results.activityCount = activitiesRes.data?.totalElements || 0;

        setStats(results);
        setError('');
      } catch (err: any) {
        console.error('Failed to fetch admin stats:', err);
        setError('Failed to load admin statistics');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, []);

  return (
    <Layout>
      <div className="space-y-6">
        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {/* System Info */}
        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 shadow-sm">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium text-sm">User:</span>
              <div className="text-gray-900 dark:text-gray-100 mt-1 font-medium">{user?.email}</div>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium text-sm">Role:</span>
              <div className="text-gray-900 dark:text-gray-100 mt-1 font-medium capitalize flex items-center gap-1">
                {currentRole} {isSystemAdmin && '👑'}
              </div>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 font-medium text-sm">Current Tenant:</span>
              <div className="text-gray-900 dark:text-gray-100 mt-1 font-medium">{currentTenant?.name}</div>
            </div>
          </div>
        </div>

        {/* Admin Features Grid */}
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <span>🛠️</span> Tenant Administration
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Custom Fields Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">🎨</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Data Model
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Custom fields, calculated values, policy rules, and core field option lists
              </p>
              <Link to="/admin/custom-fields" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Fields →
              </Link>
            </div>

            {/* User Management Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">👤</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                User Management
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Add, remove, or manage users and groups in this tenant
              </p>
              <Link to="/admin/users" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Users →
              </Link>
            </div>

            {/* Tenant Settings Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">⚙️</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Tenant Settings
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Configure tenant name, theme, and settings
              </p>
              <Link to="/admin/settings" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Edit Settings →
              </Link>
            </div>

            {/* Opportunity Types Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">📂</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Opportunity Types
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Define custom opportunity categories with their own fields
              </p>
              <Link to="/admin/opportunity-types" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Types →
              </Link>
            </div>

            {/* Templates Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">📋</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Templates
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Manage document and notification templates
              </p>
              <Link to="/admin/document-templates" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Templates →
              </Link>
            </div>

            {/* Integrations Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">⚡</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Integrations
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Connect external services (Slack, Webhooks, HubSpot, Zapier) to receive CRM events
              </p>
              <Link to="/admin/integrations" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Integrations →
              </Link>
            </div>

            {/* AI Assistant Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">🤖</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                AI Assistant
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Manage subscription tier, view token usage, and configure the AI assistant
              </p>
              <Link to="/admin/assistant" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Manage Assistant →
              </Link>
            </div>

            {/* Tenant Backup Card */}
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="text-3xl mb-3">💾</div>
              <h4 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">
                Tenant Backup
              </h4>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Create tenant backups (settings + optional data) and restore from JSON
              </p>
              <Link to="/admin/backup" className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
                Open Backup →
              </Link>
            </div>
          </div>
        </div>

        {/* System Admin Section */}
        {isSystemAdmin && (
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <span>👑</span> System Administration
            </h3>
            <div className="bg-gradient-to-br from-purple-500 to-indigo-600 rounded-lg p-6 shadow-md text-white max-w-md">
              <div className="text-4xl mb-3">🏢</div>
              <h4 className="text-lg font-semibold mb-2">
                System Admin Console
              </h4>
              <p className="text-sm text-white/90 mb-5">
                Manage all tenants, users, and view system-wide statistics from one centralized dashboard
              </p>
              <Link to="/admin/system" className="inline-flex items-center px-4 py-2 bg-white/20 hover:bg-white/30 text-white text-sm font-medium rounded-lg transition-colors border border-white/30 backdrop-blur-sm">
                Open Console →
              </Link>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};
