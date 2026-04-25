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
import { useState, useEffect } from 'react';
import logoIconUrl from '../assets/logo-icon.svg';
import logoFullUrl from '../assets/logo.svg';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useUiStore } from '../store/uiStore';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { useNotificationsStore } from '../store/notificationsStore';
import { useEntityLabels } from '../hooks/useEntityLabel';
import { useCalculationWebSocket } from '../hooks/useCalculationWebSocket';
import { AssistantBar } from './AssistantBar';
import { TenantSwitcher } from './TenantSwitcher';
import { NotificationsDrawer } from './NotificationsDrawer';
import { apiClient } from '../api/apiClient';
import {
  LayoutDashboard, TrendingUp, Building2, UserCircle,
  Briefcase, CalendarCheck, Settings, FileText, Package, Bell, ShoppingCart, Receipt, Wand2, type LucideIcon,
} from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
}

function darkenHex(hex: string, amount = 20): string {
  const r = Math.max(0, parseInt(hex.slice(1, 3), 16) - amount);
  const g = Math.max(0, parseInt(hex.slice(3, 5), 16) - amount);
  const b = Math.max(0, parseInt(hex.slice(5, 7), 16) - amount);
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function lightenHex(hex: string, amount = 60): string {
  const r = Math.min(255, parseInt(hex.slice(1, 3), 16) + amount);
  const g = Math.min(255, parseInt(hex.slice(3, 5), 16) + amount);
  const b = Math.min(255, parseInt(hex.slice(5, 7), 16) + amount);
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

/** WCAG relative luminance (0 = black, 1 = white). */
function getLuminance(hex: string): number {
  const toLinear = (c: number) => c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  const r = toLinear(parseInt(hex.slice(1, 3), 16) / 255);
  const g = toLinear(parseInt(hex.slice(3, 5), 16) / 255);
  const b = toLinear(parseInt(hex.slice(5, 7), 16) / 255);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/**
 * Returns a version of the color that is readable on the target background.
 * On dark backgrounds, very dark colors are lightened.
 * On light backgrounds, very light colors are darkened.
 */
function adaptColor(hex: string, forDarkBackground: boolean): string {
  const lum = getLuminance(hex);
  if (forDarkBackground && lum < 0.2) return lightenHex(hex, Math.round((0.2 - lum) * 400));
  if (!forDarkBackground && lum > 0.8) return darkenHex(hex, Math.round((lum - 0.8) * 400));
  return hex;
}

export const Layout = ({ children }: LayoutProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, currentRole, logout, updateUserPreferences, tenantSettings, setTenantSettings, isAuthenticated, currentTenant } = useAuthStore();
  const { headerActions } = useUiStore();
  useCalculationWebSocket();
  const { types: opportunityTypes, fetchTypes: fetchOpportunityTypes } = useOpportunityTypesStore();
  const [sidebarOpen, setSidebarOpen] = useState(() => {
    if (typeof window !== 'undefined' && window.innerWidth < 768) return false;
    const stored = localStorage.getItem('sidebarOpen');
    return stored !== null ? JSON.parse(stored) : true;
  });
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);

  const primaryColor = tenantSettings?.primaryColor as string | undefined;
  const logoUrl = tenantSettings?.logoUrl as string | undefined;

  const parsePreferences = (preferences: unknown): Record<string, unknown> => {
    if (!preferences) return {};
    if (typeof preferences === 'string') {
      try {
        return JSON.parse(preferences) ?? {};
      } catch (e) {
        console.error('Failed to parse user preferences', e);
        return {};
      }
    }
    if (typeof preferences === 'object') {
      return preferences as Record<string, unknown>;
    }
    return {};
  };

  useEffect(() => {
    localStorage.setItem('sidebarOpen', JSON.stringify(sidebarOpen));
  }, [sidebarOpen]);

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) setSidebarOpen(false);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    const preferences = parsePreferences(user?.preferences);
    setIsDarkMode(preferences.darkMode === true);
  }, [user?.preferences]);

  // Fetch fresh tenant settings and opportunity types on mount for all authenticated users
  useEffect(() => {
    if (!isAuthenticated) return;
    apiClient.getCurrentTenant()
      .then(res => {
        const settings = res.data?.settings;
        if (settings && typeof settings === 'object') {
          setTenantSettings(settings);
        }
      })
      .catch(() => {/* non-critical, ignore */});
    fetchOpportunityTypes();
  }, [isAuthenticated, currentTenant?.id]);

  // Load unread notification count
  useEffect(() => {
    if (!isAuthenticated) return;
    apiClient.getUnreadNotificationCount()
      .then(res => setUnreadNotificationCount(res.data?.unreadCount || 0))
      .catch(() => {/* non-critical, ignore */});
  }, [isAuthenticated]);

  // Apply primary color as CSS custom properties whenever it changes.
  // Two sets: --primary for light mode, --primary-on-dark for dark mode
  // (very dark colors are auto-lightened so they remain visible on dark backgrounds).
  useEffect(() => {
    if (primaryColor && /^#[0-9A-Fa-f]{6}$/.test(primaryColor)) {
      const onDark = adaptColor(primaryColor, true);
      const onLight = adaptColor(primaryColor, false);
      document.documentElement.style.setProperty('--primary', onLight);
      document.documentElement.style.setProperty('--primary-hover', darkenHex(onLight));
      document.documentElement.style.setProperty('--primary-on-dark', onDark);
      document.documentElement.style.setProperty('--primary-on-dark-hover', darkenHex(onDark));
    } else {
      document.documentElement.style.removeProperty('--primary');
      document.documentElement.style.removeProperty('--primary-hover');
      document.documentElement.style.removeProperty('--primary-on-dark');
      document.documentElement.style.removeProperty('--primary-on-dark-hover');
    }
  }, [primaryColor]);

  // Apply dark mode class to HTML element
  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  const handleLogout = async () => {
    await logout();

    const authority = import.meta.env.VITE_OIDC_AUTHORITY as string | undefined;
    const clientId = import.meta.env.VITE_OIDC_CLIENT_ID as string | undefined;
    const logoutEndpoint =
      (import.meta.env.VITE_OIDC_LOGOUT_ENDPOINT as string | undefined) ||
      (authority ? `${String(authority).replace(/\/$/, '')}/v2/logout` : undefined);

    let returnTo = (import.meta.env.VITE_OIDC_LOGOUT_RETURN_TO as string | undefined) || '';
    if (!returnTo) {
      try {
        const res = await fetch(`${import.meta.env.VITE_API_URL || '/api'}/auth/public-config`);
        if (res.ok) {
          const cfg = await res.json();
          returnTo = cfg.oidcLogoutReturnTo || '';
        }
      } catch {
        // non-critical, fall through to default
      }
    }
    if (!returnTo) {
      returnTo = `${window.location.origin}/login`;
    }
    const federated = String(import.meta.env.VITE_OIDC_LOGOUT_FEDERATED || 'false').toLowerCase() === 'true';

    if (logoutEndpoint && clientId) {
      const params = new URLSearchParams({
        client_id: clientId,
        returnTo,
      });
      if (federated) {
        params.set('federated', '');
      }
      const logoutUrl = `${logoutEndpoint}?${params.toString()}`;
      console.log('[LOGOUT] Redirecting to Auth0:', logoutUrl);

      // Small delay to ensure localStorage is fully cleared before Auth0 redirect
      setTimeout(() => {
        window.location.href = logoutUrl;
      }, 100);
      return;
    }

    console.log('[LOGOUT] No Auth0 config, local logout only');
    navigate('/login');
  };

  const toggleDarkMode = () => {
    const preferences = parsePreferences(user?.preferences);
    const newPreferences = { ...preferences, darkMode: !isDarkMode };
    updateUserPreferences(JSON.stringify(newPreferences));
  };

  const isAdmin = currentRole === 'admin' || currentRole === 'system_admin';
  const isActive = (path: string, exact = false) =>
    exact ? location.pathname === path : location.pathname.startsWith(path);
  const userLabel = user?.displayName || user?.email || '';

  const entityLabels = useEntityLabels();
  const hiddenModules = new Set((tenantSettings?.hiddenModules as string[]) || []);

  const opportunityNavItems: { path: string; label: string; Icon: LucideIcon; exact?: boolean }[] =
    opportunityTypes.length === 0
      ? !hiddenModules.has('Opportunity') ? [{ path: '/opportunities', label: `${entityLabels.Opportunity}s`, Icon: Briefcase }] : []
      : !hiddenModules.has('Opportunity') ? [
          { path: '/opportunities', label: `All ${entityLabels.Opportunity}s`, Icon: Briefcase, exact: true },
          ...opportunityTypes.map(t => ({ path: `/opportunities/type/${t.slug}`, label: t.name, Icon: Briefcase })),
        ] : [];

  const reportBuilderEnabled = tenantSettings?.reportBuilderEnabled === true;

  const navItems: { path: string; label: string; Icon: LucideIcon; exact?: boolean; beta?: boolean }[] = [
    { path: '/dashboard', label: 'Dashboard',                        Icon: LayoutDashboard },
    { path: '/reports',   label: 'Reports',                          Icon: TrendingUp },
    { path: '/customers', label: `${entityLabels.Customer}s`,        Icon: Building2 },
    { path: '/contacts',  label: `${entityLabels.Contact}s`,         Icon: UserCircle },
    ...opportunityNavItems,
    ...(!hiddenModules.has('Activity') ? [{ path: '/activities', label: `${entityLabels.Activity}s`, Icon: CalendarCheck }] : []),
    ...(!hiddenModules.has('Order') ? [{ path: '/orders', label: 'Orders', Icon: ShoppingCart }] : []),
    ...(!hiddenModules.has('Invoice') ? [{ path: '/invoices', label: 'Invoices', Icon: Receipt }] : []),
    ...(!hiddenModules.has('CustomRecord') ? [{ path: '/custom-records', label: `${entityLabels.CustomRecord}s`, Icon: Package }] : []),
    { path: '/documents',  label: 'Documents',                       Icon: FileText },
    ...(reportBuilderEnabled ? [{ path: '/report-builder', label: 'Report Builder', Icon: Wand2, beta: true }] : []),
  ];

  const adminItems: { path: string; label: string; Icon: LucideIcon }[] = [
    { path: '/admin', label: 'Admin', Icon: Settings },
  ];

  const currentNavItem = navItems.find(i => isActive(i.path, i.exact)) || adminItems.find(i => isActive(i.path));
  const pageTitle = currentNavItem?.label || 'BOCRM';

  // Adapt primary color for the current mode so it stays readable
  const effectivePrimary = primaryColor
    ? adaptColor(primaryColor, isDarkMode)
    : (isDarkMode ? '#60A5FA' : '#3B82F6');

  const activeNavStyle = { backgroundColor: effectivePrimary, color: '#fff' };

  return (
    <div className="flex h-screen bg-gray-100 dark:bg-gray-900">
      {/* Mobile backdrop — closes sidebar on tap */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Nav Sidebar — overlay on mobile, inline on desktop */}
      <aside className={`fixed md:relative inset-y-0 left-0 z-50 bg-white dark:bg-gray-800 shadow-lg transition-all duration-300 flex flex-col shrink-0 ${sidebarOpen ? 'w-64 translate-x-0' : '-translate-x-full md:translate-x-0 w-64 md:w-20'}`}>
        {/* Sidebar Header */}
        <div className={`h-16 flex items-center px-4 ${sidebarOpen ? 'justify-between' : 'justify-center'}`}
          style={{ borderBottom: `2px solid ${effectivePrimary}` }}
        >
          <div className="flex items-center overflow-hidden">
            {logoUrl ? (
              <img
                src={logoUrl}
                alt="Logo"
                className="w-14 h-14 rounded object-contain shrink-0"
              />
            ) : sidebarOpen ? (
              <img src={logoFullUrl} alt="Boring Old CRM" className="h-9 w-auto shrink-0" />
            ) : (
              <img src={logoIconUrl} alt="B" className="w-10 h-10 shrink-0" />
            )}
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-6">
          <div>
            {sidebarOpen && <div className="px-3 mb-2 text-xs font-semibold text-gray-400 uppercase tracking-wider">Main</div>}
            <div className="space-y-1">
              {navItems.map(({ path, label, Icon, exact, beta }) => {
                const active = isActive(path, exact);
                return (
                  <Link
                    key={path}
                    to={path}
                    title={!sidebarOpen ? label : ''}
                    onClick={() => { if (window.innerWidth < 768) setSidebarOpen(false); }}
                    className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                      active
                        ? primaryColor ? '' : 'bg-blue-50 dark:bg-blue-900 text-blue-600 dark:text-blue-300'
                        : 'text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white'
                    }`}
                    style={active ? activeNavStyle : undefined}
                  >
                    <Icon size={20} className="shrink-0" style={!active ? { color: effectivePrimary } : undefined} />
                    {sidebarOpen && (
                      <span className="flex items-center gap-1.5 min-w-0">
                        <span className="font-medium truncate">{label}</span>
                        {beta && <span className="shrink-0 text-[9px] font-bold uppercase tracking-wide px-1 py-0.5 rounded bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-300">Beta</span>}
                      </span>
                    )}
                  </Link>
                );
              })}
            </div>
          </div>

          {isAdmin && (
            <div>
              {sidebarOpen && <div className="px-3 mb-2 text-xs font-semibold text-gray-400 uppercase tracking-wider">Administration</div>}
              <div className="space-y-1">
                {adminItems.map(({ path, label, Icon }) => {
                  const active = isActive(path);
                  return (
                    <Link
                      key={path}
                      to={path}
                      title={!sidebarOpen ? label : ''}
                      onClick={() => { if (window.innerWidth < 768) setSidebarOpen(false); }}
                      className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                        active
                          ? primaryColor ? '' : 'bg-blue-50 dark:bg-blue-900 text-blue-600 dark:text-blue-300'
                          : 'text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white'
                      }`}
                      style={active ? activeNavStyle : undefined}
                    >
                      <Icon size={20} className="shrink-0" style={!active ? { color: effectivePrimary } : undefined} />
                      {sidebarOpen && <span className="font-medium truncate">{label}</span>}
                    </Link>
                  );
                })}
              </div>
            </div>
          )}
        </nav>

        {/* User Profile & Tenant Switcher */}
        <div className="border-t border-gray-200 dark:border-gray-700 p-4 bg-gray-50 dark:bg-gray-900">
          {/* Tenant Switcher */}
          <div className="mb-4">
            <TenantSwitcher compact={!sidebarOpen} />
          </div>

          {/* User Info */}
          <div className={`flex items-center gap-3 ${!sidebarOpen ? 'justify-center' : ''}`}>
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-bold shrink-0">
              {userLabel.charAt(0).toUpperCase()}
            </div>
            {sidebarOpen && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{userLabel}</p>
                <button onClick={handleLogout} className="text-xs text-red-600 hover:text-red-700 font-medium">
                  Sign out
                </button>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content Area + Assistant */}
      <div className="flex-1 flex flex-col overflow-hidden min-w-0">
        <header className="h-16 bg-white dark:bg-gray-800 flex items-center justify-between px-4 shadow-sm z-10 shrink-0 gap-2 overflow-hidden"
          style={{ borderBottom: `2px solid ${effectivePrimary}` }}
        >
          <div className="flex items-center gap-2 min-w-0">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors shrink-0"
              title="Toggle sidebar"
            >
              {sidebarOpen ? '◀' : '▶'}
            </button>
            {currentNavItem?.Icon && (
              <currentNavItem.Icon size={22} className="sm:hidden shrink-0" style={{ color: effectivePrimary }} />
            )}
            <h1 className={`text-xl font-semibold truncate min-w-0${currentNavItem?.Icon ? ' hidden sm:block' : ''}`} style={{ color: effectivePrimary }}>{pageTitle}</h1>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => useNotificationsStore.setState({ isOpen: true })}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors relative"
              title="Notifications"
            >
              <Bell size={20} />
              {unreadNotificationCount > 0 && (
                <span className="absolute top-0 right-0 bg-red-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {unreadNotificationCount > 9 ? '9+' : unreadNotificationCount}
                </span>
              )}
            </button>
            <button
              onClick={toggleDarkMode}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors"
              title="Toggle dark mode"
            >
              {isDarkMode ? '☀️' : '🌙'}
            </button>
            {headerActions}
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6 min-h-0 flex flex-col">
          {children}
        </main>

        {/* Assistant bar — bottom of the content column, not spanning the sidebar */}
        <AssistantBar />

        {/* Footer */}
        <footer className="shrink-0 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 px-6 py-2 flex items-center justify-between text-xs text-gray-600 dark:text-gray-400">
          <span>
            © {new Date().getFullYear()} Boring Old CRM
            <span className="hidden sm:inline"> · {__APP_VERSION__} · </span>
            <a
              href="https://github.com/N0t4R0b0t/BoringOldCRM"
              target="_blank"
              rel="noopener noreferrer"
              className="hidden sm:inline underline hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
              title="Source code — AGPL-3.0-or-later"
            >
              AGPL-3.0
            </a>
          </span>
          <span className="inline-block bg-yellow-300 dark:bg-yellow-600 text-yellow-900 dark:text-yellow-100 px-3 py-1 rounded-full font-medium">
            <span className="sm:hidden">⚠️ Test only</span>
            <span className="hidden sm:inline">⚠️ For test purposes only ⚠️</span>
          </span>
          <a href="mailto:bocrm@rsalvador.dev" className="hidden sm:block hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
            bocrm@rsalvador.dev
          </a>
        </footer>
      </div>

      {/* Notifications Drawer */}
      <NotificationsDrawer />
    </div>
  );
};
