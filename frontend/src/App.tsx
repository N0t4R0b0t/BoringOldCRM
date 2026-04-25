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
/**
 * @file Root router. Defines all client-side routes and wraps protected pages in ProtectedRoute.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ReportingDashboardPage } from './pages/ReportingDashboardPage';
import { ReportBuilderPage } from './pages/ReportBuilderPage';
import { CustomerFormPage } from './pages/CustomerFormPage';
import { ContactsPage } from './pages/ContactsPage';
import { ContactFormPage } from './pages/ContactFormPage';
import { OpportunitiesPage } from './pages/OpportunitiesPage';
import { OpportunityFormPage } from './pages/OpportunityFormPage';
import { ActivitiesPage } from './pages/ActivitiesPage';
import { ActivityFormPage } from './pages/ActivityFormPage';
import { CustomersPage } from './pages/CustomersPage';
import { TenantsListPage } from './pages/TenantsListPage';
import { TenantFormPage } from './pages/TenantFormPage';
import { TenantUsersPage } from './pages/TenantUsersPage';
import { CustomFieldsPage } from './pages/CustomFieldsPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';
import { CurrentTenantUsersPage } from './pages/CurrentTenantUsersPage';
import { CurrentTenantSettingsPage } from './pages/CurrentTenantSettingsPage';
import { OidcCallbackPage } from './pages/OidcCallbackPage';
import { OnboardingPage } from './pages/OnboardingPage';
import { AssistantSettingsPage } from './pages/AssistantSettingsPage';
import { OpportunityTypesAdminPage } from './pages/OpportunityTypesAdminPage';
import { DocumentTemplatesAdminPage } from './pages/DocumentTemplatesAdminPage';
import { TenantBackupPage } from './pages/TenantBackupPage';
import { CustomRecordsPage } from './pages/CustomRecordsPage';
import { DocumentsPage } from './pages/DocumentsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { SelectTenantPage } from './pages/SelectTenantPage';
import { SystemAdminPage } from './pages/SystemAdminPage';
import { OrdersPage } from './pages/OrdersPage';
import { OrderFormPage } from './pages/OrderFormPage';
import { InvoicesPage } from './pages/InvoicesPage';
import { InvoiceFormPage } from './pages/InvoiceFormPage';
import { IntegrationsAdminPage } from './pages/IntegrationsAdminPage';

function App() {
  const { initializeAuth, isLoading, user } = useAuthStore();
  const [authInitialized, setAuthInitialized] = useState(false);

  const getDarkModePreference = (preferences: unknown): boolean => {
    if (!preferences) return false;
    if (typeof preferences === 'string') {
      try {
        const parsed = JSON.parse(preferences);
        return parsed?.darkMode === true;
      } catch {
        return false;
      }
    }
    if (typeof preferences === 'object') {
      return (preferences as { darkMode?: boolean }).darkMode === true;
    }
    return false;
  };

  useEffect(() => {
    // Initialize auth from localStorage
    initializeAuth();
    // Mark auth as initialized immediately since initializeAuth is synchronous
    setAuthInitialized(true);
  }, [initializeAuth]);

  useEffect(() => {
    if (getDarkModePreference(user?.preferences)) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [user?.preferences]);

  // Show loading screen while auth is being initialized
  if (!authInitialized || isLoading) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        backgroundColor: 'rgb(249 250 251)',
      }}>
        <div style={{
          textAlign: 'center',
        }}>
          <div style={{
            fontSize: '48px',
            marginBottom: '16px',
          }}>📱</div>
          <p style={{
            color: 'rgb(107 114 128)',
            fontSize: '18px',
            fontWeight: '500',
          }}>BOCRM</p>
          <p style={{
            color: 'rgb(156 163 175)',
            fontSize: '14px',
            marginTop: '8px',
          }}>Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<OidcCallbackPage />} />
        <Route path="/onboard" element={<OnboardingPage />} />
        <Route path="/select-tenant" element={<SelectTenantPage />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports"
          element={
            <ProtectedRoute>
              <ReportingDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/report-builder"
          element={
            <ProtectedRoute>
              <ReportBuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customers"
          element={
            <ProtectedRoute>
              <CustomersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customers/new"
          element={
            <ProtectedRoute>
              <CustomerFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customers/:id/edit"
          element={
            <ProtectedRoute>
              <CustomerFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contacts"
          element={
            <ProtectedRoute>
              <ContactsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contacts/new"
          element={
            <ProtectedRoute>
              <ContactFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contacts/:id/edit"
          element={
            <ProtectedRoute>
              <ContactFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities"
          element={
            <ProtectedRoute>
              <OpportunitiesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities/new"
          element={
            <ProtectedRoute>
              <OpportunityFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities/:id/edit"
          element={
            <ProtectedRoute>
              <OpportunityFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities/type/:typeSlug"
          element={
            <ProtectedRoute>
              <OpportunitiesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities/type/:typeSlug/new"
          element={
            <ProtectedRoute>
              <OpportunityFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/opportunities/type/:typeSlug/:id/edit"
          element={
            <ProtectedRoute>
              <OpportunityFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/activities"
          element={
            <ProtectedRoute>
              <ActivitiesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/activities/new"
          element={
            <ProtectedRoute>
              <ActivityFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/activities/:id/edit"
          element={
            <ProtectedRoute>
              <ActivityFormPage />
            </ProtectedRoute>
          }
        />
        
        {/* CustomRecord Routes */}
        <Route
          path="/custom-records"
          element={
            <ProtectedRoute>
              <CustomRecordsPage />
            </ProtectedRoute>
          }
        />

        {/* Document Routes */}
        <Route
          path="/documents"
          element={
            <ProtectedRoute>
              <DocumentsPage />
            </ProtectedRoute>
          }
        />

        {/* Notifications Route */}
        <Route
          path="/notifications"
          element={
            <ProtectedRoute>
              <NotificationsPage />
            </ProtectedRoute>
          }
        />

        {/* Admin Routes */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <AdminDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/custom-fields"
          element={
            <ProtectedRoute>
              <CustomFieldsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenant-users"
          element={<Navigate to="/admin/users" replace />}
        />
        <Route
          path="/admin/tenant-settings"
          element={
            <ProtectedRoute>
              <CurrentTenantSettingsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/custom-fields"
          element={
            <ProtectedRoute>
              <CustomFieldsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute>
              <CurrentTenantUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/settings"
          element={
            <ProtectedRoute>
              <CurrentTenantSettingsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/assistant"
          element={
            <ProtectedRoute>
              <AssistantSettingsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/opportunity-types"
          element={
            <ProtectedRoute>
              <OpportunityTypesAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/document-templates"
          element={
            <ProtectedRoute>
              <DocumentTemplatesAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/integrations"
          element={
            <ProtectedRoute>
              <IntegrationsAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/backup"
          element={
            <ProtectedRoute>
              <TenantBackupPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/notification-templates"
          element={<Navigate to="/admin/document-templates?tab=notifications" replace />}
        />
        <Route
          path="/admin/groups"
          element={<Navigate to="/admin/users?tab=groups" replace />}
        />

        {/* System Admin Routes */}
        <Route
          path="/admin/tenants"
          element={
            <ProtectedRoute>
              <TenantsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenants/new"
          element={
            <ProtectedRoute>
              <TenantFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenants/:id/edit"
          element={
            <ProtectedRoute>
              <TenantFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/tenants/:id/users"
          element={
            <ProtectedRoute>
              <TenantUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/system"
          element={
            <ProtectedRoute>
              <SystemAdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders/new"
          element={
            <ProtectedRoute>
              <OrderFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders/:id/edit"
          element={
            <ProtectedRoute>
              <OrderFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices"
          element={
            <ProtectedRoute>
              <InvoicesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices/new"
          element={
            <ProtectedRoute>
              <InvoiceFormPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices/:id/edit"
          element={
            <ProtectedRoute>
              <InvoiceFormPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
