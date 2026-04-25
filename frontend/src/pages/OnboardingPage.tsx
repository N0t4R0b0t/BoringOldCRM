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
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';
import '../styles/Login.css';

export const OnboardingPage = () => {
  usePageTitle('Welcome');
  const navigate = useNavigate();
  const { completeOnboarding, isLoading, error, logout, isAuthenticated, requiresOnboarding, currentTenant } = useAuthStore();
  const [tenantName, setTenantName] = useState('');
  const [localError, setLocalError] = useState('');

  // Redirect away if the user already has a tenant (onboarding complete or not needed)
  useEffect(() => {
    if (isAuthenticated && !requiresOnboarding && currentTenant) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, requiresOnboarding, currentTenant, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError('');

    const trimmed = tenantName.trim();
    if (trimmed.length < 2) {
      setLocalError('Organization name must be at least 2 characters.');
      return;
    }
    if (trimmed.length > 80) {
      setLocalError('Organization name must be 80 characters or less.');
      return;
    }

    try {
      await completeOnboarding(trimmed);
      navigate('/dashboard');
    } catch {
      // error already set in store
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-box">
          <h1 className="login-title">Welcome!</h1>
          <p className="login-subtitle">Let's set up your workspace</p>

          <form className="login-form" onSubmit={handleSubmit}>
            <p className="text-gray-700" style={{ marginBottom: '12px' }}>
              What's the name of your organization?
            </p>

            {(error || localError) && (
              <div className="error-message">
                {error || localError}
              </div>
            )}

            <input
              type="text"
              className="border border-gray-300 rounded-lg px-3 py-2 w-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
              placeholder="e.g. Acme Corp"
              value={tenantName}
              onChange={(e) => setTenantName(e.target.value)}
              maxLength={80}
              autoFocus
              style={{ marginBottom: '16px' }}
            />

            <button
              type="submit"
              disabled={isLoading || tenantName.trim().length < 2}
              className="submit-btn"
            >
              {isLoading ? 'Creating workspace...' : 'Get Started'}
            </button>

            <button
              type="button"
              onClick={() => { logout(); navigate('/login'); }}
              className="text-gray-600 hover:text-gray-800 transition-colors"
              style={{ marginTop: '12px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}
            >
              Sign out
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
