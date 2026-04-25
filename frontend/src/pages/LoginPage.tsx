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
import logoFullUrl from '../assets/logo.svg';
import '../styles/Login.css';

export const LoginPage = () => {
  usePageTitle('Login');
  const navigate = useNavigate();
  const { isLoading, error, isAuthenticated } = useAuthStore();
  const [localError, setLocalError] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (!isAuthenticated && !isLoading) {
      const authority = import.meta.env.VITE_OIDC_AUTHORITY;
      const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
      // Auto-redirect to SSO if configured
      if (authority && clientId) {
        handleExternalLogin();
      }
    }
  }, [isAuthenticated, isLoading]);

  const toBase64Url = (input: Uint8Array): string =>
    btoa(String.fromCharCode(...input))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');

  const randomString = (length = 32): string => {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return toBase64Url(bytes);
  };

  const createCodeChallenge = async (verifier: string): Promise<{ challenge: string; method: string }> => {
    if (crypto.subtle) {
      const data = new TextEncoder().encode(verifier);
      const digest = await crypto.subtle.digest('SHA-256', data);
      return { challenge: toBase64Url(new Uint8Array(digest)), method: 'S256' };
    }
    return { challenge: verifier, method: 'plain' };
  };

  const handleExternalLogin = async () => {
    setLocalError('');
    sessionStorage.removeItem('oidc_used_code');
    sessionStorage.removeItem('oidc_last_error');
    const authority = import.meta.env.VITE_OIDC_AUTHORITY;
    const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
    const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI || `${window.location.origin}/auth/callback`;
    const authorizationEndpoint = import.meta.env.VITE_OIDC_AUTHORIZATION_ENDPOINT || `${String(authority).replace(/\/$/, '')}/authorize`;
    const scope = import.meta.env.VITE_OIDC_SCOPE || 'openid profile email';
    const organization = import.meta.env.VITE_OIDC_ORGANIZATION;

    if (!authority || !clientId) {
      setLocalError('SSO is not configured (missing OIDC authority/client id)');
      return;
    }

    const state = randomString(32);
    const nonce = randomString(32);
    const codeVerifier = randomString(64);
    const { challenge: codeChallenge, method: codeChallengeMethod } = await createCodeChallenge(codeVerifier);

    sessionStorage.setItem('oidc_state', state);
    sessionStorage.setItem('oidc_nonce', nonce);
    sessionStorage.setItem('oidc_code_verifier', codeVerifier);

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      redirect_uri: redirectUri,
      scope,
      state,
      nonce,
      code_challenge: codeChallenge,
      code_challenge_method: codeChallengeMethod,
    });

    if (organization) {
      params.set('organization', organization);
    }

    window.location.href = `${authorizationEndpoint}?${params.toString()}`;
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-box">
          <div className="flex justify-center mb-6">
            <img src={logoFullUrl} alt="Boring Old CRM" className="h-12 w-auto" />
          </div>

          <div className="login-form">
            <p className="text-gray-700" style={{ marginBottom: '12px' }}>
              Sign in using your organization SSO provider.
            </p>

            {(error || localError) && (
              <div className="error-message">
                {error || localError}
              </div>
            )}

            <button
              type="button"
              disabled={isLoading}
              onClick={handleExternalLogin}
              className="submit-btn"
            >
              {isLoading ? 'Redirecting...' : 'Continue with SSO'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
