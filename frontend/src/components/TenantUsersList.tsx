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
import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';

interface TenantUsersListProps {
  tenantId: number;
  showAddForm?: boolean;
}

interface SystemUser {
  id: number;
  email: string;
  displayName: string | null;
  status: string;
  createdAt: string;
  membershipCount: number;
  oauthProvider: string | null;
  oauthId: string | null;
}

export const TenantUsersList: React.FC<TenantUsersListProps> = ({ tenantId, showAddForm = true }) => {
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [newUserId, setNewUserId] = useState<number | null>(null);
  const [newUserRole, setNewUserRole] = useState('user');
  const [addingUser, setAddingUser] = useState(false);
  const [platformUsers, setPlatformUsers] = useState<SystemUser[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    if (tenantId) {
      fetchUsers();
      fetchPlatformUsers();
    }
  }, [tenantId]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await apiClient.getTenantUsers(tenantId);
      setUsers(res.data || []);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const fetchPlatformUsers = async () => {
    setLoadingUsers(true);
    try {
      const res = await apiClient.getSystemUsers({ page: 0, size: 1000, search: undefined });
      setPlatformUsers(res.data?.content || []);
    } catch (e: any) {
      console.error('Failed to load platform users', e);
      setPlatformUsers([]);
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleAddUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUserId) return;

    setAddingUser(true);
    setError('');
    try {
      await apiClient.addUserToTenant(tenantId, newUserId, newUserRole);
      setNewUserId(null);
      setNewUserRole('user');
      setSearchQuery('');
      setShowDropdown(false);
      fetchUsers();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to add user');
    } finally {
      setAddingUser(false);
    }
  };

  const handleSelectUser = (userId: number) => {
    setNewUserId(userId);
    setSearchQuery('');
    setShowDropdown(false);
  };

  const handleRemoveUser = async (userId: number) => {
    if (!window.confirm('Are you sure you want to remove this user from the tenant?')) return;
    
    try {
      await apiClient.removeUserFromTenant(tenantId, userId);
      fetchUsers();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to remove user');
    }
  };

  const roleBadge = (role: string) => {
    const map: Record<string, string> = {
      admin: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300',
      manager: 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300',
      user: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
    };
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${map[role] ?? map.user}`}>
        {role}
      </span>
    );
  };

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      active: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300',
      inactive: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
      pending: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300',
    };
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${map[status] ?? map.inactive}`}>
        {status}
      </span>
    );
  };

  return (
    <div>
      {error && <div className="error-banner">{error}</div>}

      {showAddForm && <div className="content-card mb-4">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">
          Add User to Workspace
        </h3>
        <form onSubmit={handleAddUser}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 sm:gap-4 items-end">
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                User
              </label>
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search users..."
                  value={newUserId ? platformUsers.find(u => u.id === newUserId)?.email || '' : searchQuery}
                  onChange={(e) => {
                    const val = e.target.value;
                    if (!newUserId || platformUsers.find(u => u.id === newUserId)?.email !== val) {
                      setSearchQuery(val);
                      setNewUserId(null);
                      setShowDropdown(true);
                    }
                  }}
                  onFocus={() => setShowDropdown(true)}
                  className="form-input w-full"
                  required
                />
                {showDropdown && searchQuery.length >= 3 && (
                  <div className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 max-h-64 overflow-y-auto">
                    {loadingUsers ? (
                      <div className="px-3 py-2 text-xs text-gray-500">Loading users...</div>
                    ) : (
                      platformUsers
                        .filter(u =>
                          !users.find(tu => tu.userId === u.id) &&
                          (u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
                           (u.displayName && u.displayName.toLowerCase().includes(searchQuery.toLowerCase())))
                        )
                        .slice(0, 10)
                        .map(user => {
                          const getAuthBadge = () => {
                            if (user.oauthProvider || user.oauthId) {
                              // Parse provider from oauthId if it contains a pipe (e.g., "google-oauth2|id" or "linkedin|id")
                              let displayProvider = user.oauthProvider;
                              if (user.oauthId && user.oauthId.includes('|')) {
                                const providerPrefix = user.oauthId.split('|')[0];
                                const providerMap: Record<string, string> = {
                                  'google-oauth2': 'google',
                                  'linkedin': 'linkedin',
                                  'windowslive': 'microsoft',
                                  'auth0': 'auth0',
                                };
                                displayProvider = providerMap[providerPrefix] || providerPrefix;
                              }

                              const providerConfig: Record<string, { bg: string; text: string; icon: string; label: string }> = {
                                'google': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-800 dark:text-blue-400', icon: '🔵', label: 'Google' },
                                'google-oauth2': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-800 dark:text-blue-400', icon: '🔵', label: 'Google' },
                                'microsoft': { bg: 'bg-cyan-100 dark:bg-cyan-900/30', text: 'text-cyan-800 dark:text-cyan-400', icon: '🔷', label: 'Microsoft' },
                                'windowslive': { bg: 'bg-cyan-100 dark:bg-cyan-900/30', text: 'text-cyan-800 dark:text-cyan-400', icon: '🔷', label: 'Microsoft' },
                                'linkedin': { bg: 'bg-indigo-100 dark:bg-indigo-900/30', text: 'text-indigo-800 dark:text-indigo-400', icon: '📘', label: 'LinkedIn' },
                                'auth0': { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-800 dark:text-orange-400', icon: '🔐', label: 'Auth0' },
                              };
                              const config = providerConfig[(displayProvider || '').toLowerCase()] || { bg: 'bg-gray-100 dark:bg-gray-700', text: 'text-gray-800 dark:text-gray-300', icon: '🔐', label: displayProvider || user.oauthProvider };
                              return (
                                <span className={`inline-block px-1.5 py-0.5 rounded text-xs font-medium ${config.bg} ${config.text}`}>
                                  {config.icon} {config.label}
                                </span>
                              );
                            }
                            return (
                              <span className="inline-block px-1.5 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                                🔐 Local
                              </span>
                            );
                          };

                          return (
                            <button
                              key={user.id}
                              type="button"
                              onClick={() => handleSelectUser(user.id)}
                              className="w-full text-left px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors border-b border-gray-100 dark:border-gray-600 last:border-b-0"
                            >
                              <div className="flex items-center justify-between gap-2">
                                <div>
                                  <div className="text-xs font-medium text-gray-900 dark:text-white">{user.email}</div>
                                  {user.displayName && (
                                    <div className="text-xs text-gray-500 dark:text-gray-400">{user.displayName}</div>
                                  )}
                                </div>
                                <div>{getAuthBadge()}</div>
                              </div>
                            </button>
                          );
                        })
                    )}
                    {!loadingUsers && platformUsers.filter(u =>
                      !users.find(tu => tu.userId === u.id) &&
                      (u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
                       (u.displayName && u.displayName.toLowerCase().includes(searchQuery.toLowerCase())))
                    ).length === 0 && (
                      <div className="px-3 py-2 text-xs text-gray-500">No users found</div>
                    )}
                  </div>
                )}
                {showDropdown && searchQuery.length < 3 && searchQuery.length > 0 && (
                  <div className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 px-3 py-2">
                    <p className="text-xs text-gray-500">Type at least 3 characters to search</p>
                  </div>
                )}
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                Role
              </label>
              <select
                value={newUserRole}
                onChange={(e) => setNewUserRole(e.target.value)}
                className="form-input"
              >
                <option value="user">User</option>
                <option value="manager">Manager</option>
                <option value="admin">Admin</option>
              </select>
            </div>
            <div>
              <button type="submit" disabled={addingUser || !newUserId} className="btn btn-primary w-full">
                {addingUser ? 'Adding...' : 'Add User'}
              </button>
            </div>
          </div>
        </form>
      </div>}

      <div className="content-card">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">
          Members
        </h3>
        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">Loading...</p>
        ) : users.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">No users found.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Auth Method</th>
                <th>Role</th>
                <th>Status</th>
                <th>Joined</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u: any) => {
                const getAuthBadge = () => {
                  if (u.oauthProvider || u.oauthId) {
                    // Parse provider from oauthId if it contains a pipe (e.g., "google-oauth2|id" or "linkedin|id")
                    let displayProvider = u.oauthProvider;
                    if (u.oauthId && u.oauthId.includes('|')) {
                      const providerPrefix = u.oauthId.split('|')[0];
                      const providerMap: Record<string, string> = {
                        'google-oauth2': 'google',
                        'linkedin': 'linkedin',
                        'windowslive': 'microsoft',
                        'auth0': 'auth0',
                      };
                      displayProvider = providerMap[providerPrefix] || providerPrefix;
                    }

                    const providerConfig: Record<string, { bg: string; text: string; icon: string; label: string }> = {
                      'google': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-800 dark:text-blue-400', icon: '🔵', label: 'Google' },
                      'google-oauth2': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-800 dark:text-blue-400', icon: '🔵', label: 'Google' },
                      'microsoft': { bg: 'bg-cyan-100 dark:bg-cyan-900/30', text: 'text-cyan-800 dark:text-cyan-400', icon: '🔷', label: 'Microsoft' },
                      'windowslive': { bg: 'bg-cyan-100 dark:bg-cyan-900/30', text: 'text-cyan-800 dark:text-cyan-400', icon: '🔷', label: 'Microsoft' },
                      'linkedin': { bg: 'bg-indigo-100 dark:bg-indigo-900/30', text: 'text-indigo-800 dark:text-indigo-400', icon: '📘', label: 'LinkedIn' },
                      'auth0': { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-800 dark:text-orange-400', icon: '🔐', label: 'Auth0' },
                    };
                    const config = providerConfig[displayProvider?.toLowerCase()] || { bg: 'bg-gray-100 dark:bg-gray-700', text: 'text-gray-800 dark:text-gray-300', icon: '🔐', label: displayProvider || u.oauthProvider };
                    return (
                      <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${config.bg} ${config.text}`}>
                        {config.icon} {config.label}
                      </span>
                    );
                  }
                  return (
                    <span className="inline-block px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                      🔐 Local
                    </span>
                  );
                };

                return (
                <tr key={u.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="font-medium text-gray-900 dark:text-gray-100">{u.userEmail}</td>
                  <td>{getAuthBadge()}</td>
                  <td>{roleBadge(u.role)}</td>
                  <td>{statusBadge(u.status)}</td>
                  <td className="text-gray-500 dark:text-gray-400 text-sm">
                    {new Date(u.createdAt).toLocaleDateString()}
                  </td>
                  <td className="text-right">
                    <button
                      onClick={() => handleRemoveUser(u.userId)}
                      className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium text-sm"
                    >
                      Remove
                    </button>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
