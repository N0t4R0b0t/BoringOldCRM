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
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

const decodeJwtPayload = (jwt: string): Record<string, any> => {
  const parts = jwt.split('.');
  if (parts.length < 2) return {};
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const normalized = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  return JSON.parse(atob(normalized));
};

export const OidcCallbackPage = () => {
  usePageTitle('Signing in...');
  const navigate = useNavigate();
  const { loginWithExternalToken } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const hasStarted = useRef(false);

  const clearSessionAndLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('email');
    localStorage.removeItem('currentTenant');
    localStorage.removeItem('availableTenants');
    localStorage.removeItem('preferences');
    sessionStorage.removeItem('oidc_state');
    sessionStorage.removeItem('oidc_nonce');
    sessionStorage.removeItem('oidc_code_verifier');
    sessionStorage.removeItem('oidc_used_code');
    sessionStorage.removeItem('oidc_last_error');

    const authority = import.meta.env.VITE_OIDC_AUTHORITY as string | undefined;
    const clientId = import.meta.env.VITE_OIDC_CLIENT_ID as string | undefined;
    const logoutEndpoint =
      (import.meta.env.VITE_OIDC_LOGOUT_ENDPOINT as string | undefined) ||
      (authority ? `${String(authority).replace(/\/$/, '')}/v2/logout` : undefined);
    const returnTo =
      (import.meta.env.VITE_OIDC_LOGOUT_RETURN_TO as string | undefined) ||
      `${window.location.origin}/login`;
    const federated = String(import.meta.env.VITE_OIDC_LOGOUT_FEDERATED || 'false').toLowerCase() === 'true';

    if (logoutEndpoint && clientId) {
      const params = new URLSearchParams({
        client_id: clientId,
        returnTo,
      });
      if (federated) {
        params.set('federated', '');
      }
      window.location.href = `${logoutEndpoint}?${params.toString()}`;
      return;
    }

    navigate('/login');
  };

  const getTroubleshootingHints = (message: string): string[] => {
    const normalized = message.toLowerCase();
    if (normalized.includes('no organization membership')) {
      return [
        'Make sure your Auth0 user is assigned to an organization.',
        'Set VITE_OIDC_ORGANIZATION in frontend/.env.local for dev testing.',
        'Ensure your Auth0 Action adds org_id (and optionally org_name) to the ID token.',
      ];
    }
    if (normalized.includes('multiple organizations')) {
      return [
        'Sign in with a specific organization instead of a generic login.',
        'Set VITE_OIDC_ORGANIZATION in frontend/.env.local to force one org.',
      ];
    }
    if (normalized.includes('email is not verified')) {
      return [
        'Verify the user email in Auth0 or disable strict verification for local dev.',
        'For local dev only, set APP_EXTERNAL_AUTH_REQUIRE_EMAIL_VERIFIED=false.',
      ];
    }
    if (normalized.includes('audience is invalid')) {
      return [
        'Set APP_EXTERNAL_AUTH_AUDIENCE to the expected aud claim in backend config.',
        'Confirm your Auth0 app/API audience configuration.',
      ];
    }
    if (normalized.includes('invalid or expired')) {
      return [
        'Retry login from /login and avoid refreshing /auth/callback directly.',
        'Use Logout/Clear Session before retrying if needed.',
      ];
    }
    if (normalized.includes('step:')) {
      return ['Backend identified the failing processing step. Check backend logs for that step name.'];
    }
    return ['Use Logout/Clear Session and retry SSO from /login.'];
  };

  useEffect(() => {
    if (hasStarted.current) return;
    hasStarted.current = true;

    const run = async () => {
      try {
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        const state = params.get('state');
        const oauthError = params.get('error');
        const oauthErrorDescription = params.get('error_description');

        if (oauthError) {
          throw new Error(oauthErrorDescription || oauthError);
        }
        if (!code || !state) {
          const previousError = sessionStorage.getItem('oidc_last_error');
          if (previousError) {
            throw new Error(previousError);
          }
          // In React StrictMode (dev), effects run twice via remount.
          // The first pass may already consume and clear the auth code.
          if (sessionStorage.getItem('oidc_used_code')) {
            // Give the first pass a moment to finish writing either token or error.
            await new Promise((resolve) => setTimeout(resolve, 1500));
            const delayedError = sessionStorage.getItem('oidc_last_error');
            if (delayedError) {
              throw new Error(delayedError);
            }
            if (localStorage.getItem('accessToken')) {
              navigate('/dashboard');
              return;
            }
            throw new Error('SSO login did not complete. Please retry from the login page.');
          }
          if (!code && !state) {
            throw new Error('Missing authorization code/state');
          }
          throw new Error('Missing authorization code/state');
        }

        if (sessionStorage.getItem('oidc_used_code') && !code) {
          if (localStorage.getItem('accessToken')) {
            navigate('/dashboard');
            return;
          }
          throw new Error('SSO callback expired. Please start login again.');
        }

        const previouslyUsedCode = sessionStorage.getItem('oidc_used_code');
        if (previouslyUsedCode && previouslyUsedCode === code) {
          throw new Error('Authorization code already used. Please start login again.');
        }

        const storedState = sessionStorage.getItem('oidc_state');
        const storedNonce = sessionStorage.getItem('oidc_nonce');
        const codeVerifier = sessionStorage.getItem('oidc_code_verifier');

        if (!storedState || state !== storedState) {
          throw new Error('Invalid OIDC state');
        }
        if (!codeVerifier) {
          throw new Error('Missing PKCE verifier');
        }

        // Remove query params early so browser refresh can't accidentally reuse the same code.
        window.history.replaceState({}, document.title, window.location.pathname);
        sessionStorage.setItem('oidc_used_code', code);

        const authority = import.meta.env.VITE_OIDC_AUTHORITY;
        const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
        const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI || `${window.location.origin}/auth/callback`;
        const tokenEndpoint = import.meta.env.VITE_OIDC_TOKEN_ENDPOINT || `${String(authority).replace(/\/$/, '')}/oauth/token`;

        if (!authority || !clientId) {
          throw new Error('SSO is not configured (missing OIDC authority/client id)');
        }

        const body = new URLSearchParams({
          grant_type: 'authorization_code',
          client_id: clientId,
          code,
          redirect_uri: redirectUri,
          code_verifier: codeVerifier,
        });

        const tokenResponse = await fetch(tokenEndpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body,
        });

        const rawTokenResponse = await tokenResponse.text();
        let tokenData: any = {};
        try {
          tokenData = rawTokenResponse ? JSON.parse(rawTokenResponse) : {};
        } catch {
          tokenData = {};
        }
        if (!tokenResponse.ok) {
          const details = tokenData.error_description || tokenData.error || rawTokenResponse || `HTTP ${tokenResponse.status}`;
          throw new Error(`Failed to exchange authorization code: ${details}`);
        }

        const idToken = tokenData.id_token as string | undefined;
        if (!idToken) {
          throw new Error('OIDC provider did not return an id_token');
        }

        if (storedNonce) {
          const payload = decodeJwtPayload(idToken);
          if (!payload?.nonce || payload.nonce !== storedNonce) {
            throw new Error('Invalid nonce in id_token');
          }
        }

        await loginWithExternalToken(idToken);
        sessionStorage.removeItem('oidc_used_code');
        sessionStorage.removeItem('oidc_last_error');

        // Check if tenant selection is required
        const { requiresTenantSelection } = useAuthStore.getState();
        if (requiresTenantSelection) {
          navigate('/select-tenant');
        } else {
          navigate('/dashboard');
        }
      } catch (e: any) {
        const backendMessage = e?.response?.data?.message;
        const resolvedError = backendMessage || e?.message || 'External login failed';
        sessionStorage.setItem('oidc_last_error', resolvedError);
        setError(resolvedError);
      } finally {
        sessionStorage.removeItem('oidc_state');
        sessionStorage.removeItem('oidc_nonce');
        sessionStorage.removeItem('oidc_code_verifier');
      }
    };

    run();
  }, [loginWithExternalToken, navigate]);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ textAlign: 'center' }}>
        {!error && <p>Completing SSO login...</p>}
        {error && (
          <>
            <p style={{ marginBottom: '12px', color: '#b91c1c' }}>{error}</p>
            <div style={{ marginBottom: '12px', textAlign: 'left', maxWidth: '600px' }}>
              {getTroubleshootingHints(error).map((hint, idx) => (
                <p key={idx} style={{ margin: '4px 0', color: '#374151', fontSize: '14px' }}>
                  - {hint}
                </p>
              ))}
            </div>
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
              <button type="button" onClick={() => navigate('/login')}>Back to login</button>
              <button type="button" onClick={clearSessionAndLogout}>Logout/Clear Session</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
