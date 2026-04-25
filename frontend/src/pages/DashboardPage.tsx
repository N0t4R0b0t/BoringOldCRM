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
import { OnboardingWizardModal } from '../components/OnboardingWizardModal';
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { usePageTitle } from '../hooks/usePageTitle';

interface DashboardStats {
  customerCount: number;
  opportunityCount: number;
  activityCount: number;
  contactCount?: number;
}

export const DashboardPage = () => {
  usePageTitle('Dashboard');
  const { types: opportunityTypes } = useOpportunityTypesStore();
  const { currentTenant, currentRole } = useAuthStore();
  const [stats, setStats] = useState<DashboardStats>({
    customerCount: 0,
    opportunityCount: 0,
    activityCount: 0,
    contactCount: 0,
  });
  const [typeStats, setTypeStats] = useState<Record<string, number>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [insight, setInsight] = useState<{ text: string; generatedAt: string } | null>(null);
  const [insightLoading, setInsightLoading] = useState(false);
  const [showOnboarding, setShowOnboarding] = useState(false);
  const [tenantSettings, setTenantSettings] = useState<any>(null);

  useEffect(() => {
    const fetchStats = async () => {
      setIsLoading(true);
      try {
        const [customersRes, opportunitiesRes, activitiesRes, contactsRes] = await Promise.all([
          apiClient.getCustomers({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } })),
          apiClient.getOpportunities({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } })),
          apiClient.getActivities({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } })),
          apiClient.getContacts({ page: 0, size: 1 }).catch(() => ({ data: { totalElements: 0 } })),
        ]);

        setStats({
          customerCount: customersRes.data?.totalElements || 0,
          opportunityCount: opportunitiesRes.data?.totalElements || 0,
          activityCount: activitiesRes.data?.totalElements || 0,
          contactCount: contactsRes.data?.totalElements || 0,
        });

        if (opportunityTypes.length > 0) {
          const typeResults = await Promise.all(
            opportunityTypes.map(t =>
              apiClient.getOpportunities({ page: 0, size: 1, typeSlug: t.slug })
                .then(r => ({ slug: t.slug, count: r.data?.totalElements || 0 }))
                .catch(() => ({ slug: t.slug, count: 0 }))
            )
          );
          setTypeStats(Object.fromEntries(typeResults.map(r => [r.slug, r.count])));
        }

        setError('');
      } catch (err: any) {
        console.error('Failed to fetch dashboard stats:', err);
        setError('Failed to load dashboard stats');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, [opportunityTypes.length]);

  const loadInsight = async (forceRefresh = false) => {
    setInsightLoading(true);
    try {
      const res = forceRefresh
        ? await apiClient.refreshDashboardInsight()
        : await apiClient.getDashboardInsight();
      setInsight(res.data);
    } catch {
      // insight is optional — fail silently
    } finally {
      setInsightLoading(false);
    }
  };

  useEffect(() => { loadInsight(); }, []);

  useEffect(() => {
    if (currentRole === 'admin' && currentTenant) {
      // Fetch tenant settings to check onboarding status
      apiClient.getTenantSettings(currentTenant.id)
        .then(res => {
          setTenantSettings(res.data);
          if (res.data?.onboardingCompleted !== true) {
            setShowOnboarding(true);
          }
        })
        .catch(() => {
          // If fetch fails, don't show onboarding
        });
    }
  }, [currentRole, currentTenant]);

  const handleOnboardingComplete = () => {
    setShowOnboarding(false);
    // Update local tenant settings state
    setTenantSettings({
      ...tenantSettings,
      onboardingCompleted: true,
    });
  };

  return (
    <Layout>
      <div className="space-y-6">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg dark:bg-red-900 dark:text-red-200 dark:border-red-800">
            {error}
          </div>
        )}

        {/* Data Overview */}
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
            <span>📊</span> Data Overview
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-blue-900 dark:border-blue-800">
              <div className="text-3xl font-bold text-blue-600 dark:text-blue-300">
                {isLoading ? '—' : stats.customerCount}
              </div>
              <div className="text-sm font-medium text-blue-600 dark:text-blue-300 mt-2">👥 Customers</div>
            </div>
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-green-900 dark:border-green-800">
              <div className="text-3xl font-bold text-green-700 dark:text-green-300">
                {isLoading ? '—' : stats.contactCount}
              </div>
              <div className="text-sm font-medium text-green-700 dark:text-green-300 mt-2">📇 Contacts</div>
            </div>
            {opportunityTypes.length === 0 ? (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-yellow-900 dark:border-yellow-800">
                <div className="text-3xl font-bold text-yellow-800 dark:text-yellow-300">
                  {isLoading ? '—' : stats.opportunityCount}
                </div>
                <div className="text-sm font-medium text-yellow-800 dark:text-yellow-300 mt-2">🎯 Opportunities</div>
              </div>
            ) : (
              <>
                {opportunityTypes.map(t => (
                  <div key={t.slug} className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-yellow-900 dark:border-yellow-800">
                    <div className="text-3xl font-bold text-yellow-800 dark:text-yellow-300">
                      {isLoading ? '—' : (typeStats[t.slug] ?? '—')}
                    </div>
                    <div className="text-sm font-medium text-yellow-800 dark:text-yellow-300 mt-2">🎯 {t.name}</div>
                  </div>
                ))}
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-yellow-900 dark:border-yellow-800 opacity-75">
                  <div className="text-3xl font-bold text-yellow-800 dark:text-yellow-300">
                    {isLoading ? '—' : stats.opportunityCount}
                  </div>
                  <div className="text-sm font-medium text-yellow-800 dark:text-yellow-300 mt-2">🎯 Total Opportunities</div>
                </div>
              </>
            )}
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center hover:shadow-md transition-shadow dark:bg-red-900 dark:border-red-800">
              <div className="text-3xl font-bold text-red-800 dark:text-red-300">
                {isLoading ? '—' : stats.activityCount}
              </div>
              <div className="text-sm font-medium text-red-800 dark:text-red-300 mt-2">✅ Activities</div>
            </div>
          </div>
        </div>

        {/* Assistant Insight Card */}
        {(insight || insightLoading) && (
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-4 dark:from-blue-950 dark:to-indigo-950 dark:border-blue-800">
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-start gap-3 flex-1">
                <span className="text-2xl flex-shrink-0">🤖</span>
                <div>
                  <div className="text-xs font-semibold text-blue-600 dark:text-blue-400 mb-1">Assistant Insight</div>
                  {insightLoading ? (
                    <div className="h-4 bg-blue-200 dark:bg-blue-800 rounded animate-pulse w-64" />
                  ) : (
                    <p className="text-sm text-gray-700 dark:text-gray-300">{insight?.text}</p>
                  )}
                </div>
              </div>
              <button
                onClick={() => loadInsight(true)}
                disabled={insightLoading}
                className="text-xs text-blue-500 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 flex-shrink-0 disabled:opacity-40"
                title="Refresh insight"
              >
                ↻ Refresh
              </button>
            </div>
          </div>
        )}

        {/* Getting Started & Quick Links */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Getting Started Card */}
          <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden dark:bg-gray-800 dark:border-gray-700">
            <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 dark:bg-gray-700 dark:border-gray-600">
              <h2 className="text-lg font-semibold text-gray-800 dark:text-white flex items-center gap-2">
                <span>🚀</span> Getting Started
              </h2>
            </div>
            <div className="p-6">
              <ul className="space-y-3">
                <li>
                  <Link to="/customers/new" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors group">
                    <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-bold group-hover:bg-blue-600 group-hover:text-white transition-colors">＋</span>
                    Create a customer
                  </Link>
                </li>
                <li>
                  <Link to="/opportunities/new" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors group">
                    <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-bold group-hover:bg-blue-600 group-hover:text-white transition-colors">＋</span>
                    Add an opportunity
                  </Link>
                </li>
                <li>
                  <Link to="/contacts/new" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors group">
                    <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-bold group-hover:bg-blue-600 group-hover:text-white transition-colors">＋</span>
                    Add a contact
                  </Link>
                </li>
                <li>
                  <Link to="/activities/new" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors group">
                    <span className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-bold group-hover:bg-blue-600 group-hover:text-white transition-colors">＋</span>
                    Log an activity
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          {/* Quick Links Card */}
          <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden dark:bg-gray-800 dark:border-gray-700">
            <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 dark:bg-gray-700 dark:border-gray-600">
              <h2 className="text-lg font-semibold text-gray-800 dark:text-white flex items-center gap-2">
                <span>⚡</span> Quick Links
              </h2>
            </div>
            <div className="p-6">
              <ul className="space-y-3">
                <li>
                  <Link to="/customers" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    <span>👥</span> View All Customers
                  </Link>
                </li>
                <li>
                  <Link to="/contacts" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    <span>📇</span> View All Contacts
                  </Link>
                </li>
                <li>
                  <Link to="/opportunities" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    <span>🎯</span> View All Opportunities
                  </Link>
                </li>
                <li>
                  <Link to="/activities" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    <span>✅</span> View All Activities
                  </Link>
                </li>
                <li>
                  <Link to="/reports" className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    <span>📈</span> View Reports
                  </Link>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {showOnboarding && currentTenant && (
        <OnboardingWizardModal
          tenantId={currentTenant.id}
          tenantName={currentTenant.name}
          onComplete={handleOnboardingComplete}
        />
      )}
    </Layout>
  );
};
