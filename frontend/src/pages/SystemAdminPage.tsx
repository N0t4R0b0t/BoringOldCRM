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
import { Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

type Tab = 'overview' | 'tenants' | 'users' | 'ai-models' | 'ai-tiers';

interface SystemStats {
  totalTenants: number;
  activeTenants: number;
  inactiveTenants: number;
  newTenantsLast30Days: number;
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  newUsersLast30Days: number;
  totalMemberships: number;
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

interface UserMembership {
  membershipId: number;
  tenantId: number;
  tenantName: string | null;
  role: string;
  status: string;
  createdAt: string;
}

interface PagedUsers {
  content: SystemUser[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrev: boolean;
}

// ---------- Overview Tab ----------

const OverviewTab = () => {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    apiClient.getSystemStats()
      .then(res => setStats(res.data))
      .catch(() => setError('Failed to load system stats'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="text-gray-500 dark:text-gray-400 py-8">Loading statistics...</div>;
  if (error) return <div className="error-banner">{error}</div>;
  if (!stats) return null;

  const cards = [
    { label: 'Total Tenants', value: stats.totalTenants, sub: `${stats.activeTenants} active · ${stats.inactiveTenants} inactive`, accent: 'from-purple-500 to-indigo-600', text: 'white' },
    { label: 'New Tenants (30d)', value: stats.newTenantsLast30Days, sub: 'created in the last 30 days', accent: 'from-indigo-400 to-blue-500', text: 'white' },
    { label: 'Total Users', value: stats.totalUsers, sub: `${stats.activeUsers} active · ${stats.inactiveUsers} inactive`, accent: 'from-emerald-500 to-teal-600', text: 'white' },
    { label: 'New Users (30d)', value: stats.newUsersLast30Days, sub: 'created in the last 30 days', accent: 'from-teal-400 to-cyan-500', text: 'white' },
    { label: 'Total Memberships', value: stats.totalMemberships, sub: 'across all tenants', accent: null, text: 'dark' },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {cards.map(card => (
        <div
          key={card.label}
          className={card.accent
            ? `bg-gradient-to-br ${card.accent} rounded-lg p-5 shadow-md text-white`
            : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm'}
        >
          <div className={`text-3xl font-bold mb-1 ${card.text === 'dark' ? 'text-gray-900 dark:text-gray-100' : ''}`}>
            {card.value.toLocaleString()}
          </div>
          <div className={`text-sm font-semibold mb-1 ${card.text === 'dark' ? 'text-gray-700 dark:text-gray-300' : 'text-white/90'}`}>
            {card.label}
          </div>
          <div className={`text-xs ${card.text === 'dark' ? 'text-gray-500 dark:text-gray-400' : 'text-white/75'}`}>
            {card.sub}
          </div>
        </div>
      ))}
    </div>
  );
};

// ---------- Create Tenant Modal ----------

interface CreateTenantModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTenantCreated: (tenant: any) => void;
}

const CreateTenantModal = ({ isOpen, onClose, onTenantCreated }: CreateTenantModalProps) => {
  const [step, setStep] = useState<'form' | 'users'>('form');
  const [tenantName, setTenantName] = useState('');
  const [orgBio, setOrgBio] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [newTenant, setNewTenant] = useState<any>(null);
  const [userIds, setUserIds] = useState<number[]>([]);
  const [userRoles, setUserRoles] = useState<Record<number, string>>({});
  const [addingUsers, setAddingUsers] = useState(false);
  const [platformUsers, setPlatformUsers] = useState<SystemUser[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);

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

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await apiClient.createTenant({
        name: tenantName.trim(),
      });
      setNewTenant(res.data);
      setStep('users');
      fetchPlatformUsers();
      useAuthStore.getState().refreshMemberships();
    } catch (e: any) {
      setError((e as any).response?.data?.message || 'Failed to create tenant');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddUser = (userId: number) => {
    if (!userIds.includes(userId)) {
      setUserIds([...userIds, userId]);
      setUserRoles({ ...userRoles, [userId]: 'user' });
      setSearchQuery('');
      setShowDropdown(false);
    }
  };

  const handleRemoveUser = (userId: number) => {
    setUserIds(userIds.filter(id => id !== userId));
    const newRoles = { ...userRoles };
    delete newRoles[userId];
    setUserRoles(newRoles);
  };

  const handleApplyUsers = async () => {
    if (userIds.length === 0) {
      onTenantCreated(newTenant);
      onClose();
      return;
    }
    setAddingUsers(true);
    setError('');
    try {
      for (const userId of userIds) {
        const role = userRoles[userId] || 'user';
        await apiClient.addUserToTenant(newTenant.id, userId, role);
      }
      onTenantCreated(newTenant);
      onClose();
    } catch (e: any) {
      setError((e as any).response?.data?.message || 'Failed to add users to tenant');
    } finally {
      setAddingUsers(false);
    }
  };

  const handleReset = () => {
    setStep('form');
    setTenantName('');
    setOrgBio('');
    setSubmitting(false);
    setError('');
    setNewTenant(null);
    setUserIds([]);
    setUserRoles({});
    setAddingUsers(false);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg max-w-md w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            {step === 'form' ? 'Create Tenant' : 'Assign Users'}
          </h2>
          <button
            onClick={() => {
              handleReset();
              onClose();
            }}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-xl leading-none"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-4">
          {error && <div className="error-banner mb-4 text-sm">{error}</div>}

          {step === 'form' ? (
            <form onSubmit={handleCreateTenant}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Organization Name *
                </label>
                <input
                  type="text"
                  value={tenantName}
                  onChange={e => setTenantName(e.target.value)}
                  placeholder="e.g. Acme Corp"
                  className="form-input w-full"
                  required
                  autoFocus
                />
              </div>
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Organization Bio
                </label>
                <textarea
                  value={orgBio}
                  onChange={e => setOrgBio(e.target.value)}
                  placeholder="Describe your organization (optional)"
                  className="form-input w-full"
                  rows={4}
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={submitting || !tenantName.trim()}
                  className="btn btn-primary flex-1"
                >
                  {submitting ? 'Creating...' : 'Create Tenant'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    handleReset();
                    onClose();
                  }}
                  className="btn btn-secondary flex-1"
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                Tenant <strong>{newTenant?.name}</strong> created successfully. You can optionally assign users now, or skip and do it later.
              </p>

              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Add Users
                </label>
                <div className="relative">
                  <input
                    type="text"
                    id="userSearchInput"
                    placeholder="Search users..."
                    value={searchQuery}
                    onChange={e => {
                      setSearchQuery(e.target.value);
                      setShowDropdown(true);
                    }}
                    onFocus={() => setShowDropdown(true)}
                    className="form-input w-full"
                  />
                  {showDropdown && searchQuery.length >= 3 && (
                    <div className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 max-h-48 overflow-y-auto">
                      {loadingUsers ? (
                        <div className="px-4 py-2 text-xs text-gray-500">Loading users...</div>
                      ) : (
                        platformUsers
                          .filter(u =>
                            !userIds.includes(u.id) &&
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
                                onClick={() => handleAddUser(user.id)}
                                className="w-full text-left px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors border-b border-gray-100 dark:border-gray-600 last:border-b-0"
                              >
                                <div className="flex items-center justify-between gap-2">
                                  <div>
                                    <div className="text-sm font-medium text-gray-900 dark:text-white">{user.email}</div>
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
                        !userIds.includes(u.id) &&
                        (u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         (u.displayName && u.displayName.toLowerCase().includes(searchQuery.toLowerCase())))
                      ).length === 0 && (
                        <div className="px-4 py-2 text-xs text-gray-500">No users found</div>
                      )}
                    </div>
                  )}
                  {showDropdown && searchQuery.length < 3 && searchQuery.length > 0 && (
                    <div className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 px-4 py-2">
                      <p className="text-xs text-gray-500">Type at least 3 characters to search</p>
                    </div>
                  )}
                </div>
              </div>

              {userIds.length > 0 && (
                <div className="mb-4">
                  <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Users to add ({userIds.length}):
                  </p>
                  <div className="space-y-2">
                    {userIds.map(userId => {
                      const user = platformUsers.find(u => u.id === userId);
                      return (
                        <div key={userId} className="flex items-center justify-between bg-gray-50 dark:bg-gray-700/40 p-2 rounded">
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{user?.email}</p>
                            {user?.displayName && (
                              <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user.displayName}</p>
                            )}
                          </div>
                          <div className="flex items-center gap-2 ml-2 flex-shrink-0">
                            <select
                              value={userRoles[userId] || 'user'}
                              onChange={e => setUserRoles({ ...userRoles, [userId]: e.target.value })}
                              className="form-input text-xs py-1"
                            >
                              <option value="user">User</option>
                              <option value="admin">Admin</option>
                            </select>
                            <button
                              type="button"
                              onClick={() => handleRemoveUser(userId)}
                              className="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300 text-sm font-medium whitespace-nowrap"
                            >
                              Remove
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleApplyUsers}
                  disabled={addingUsers}
                  className="btn btn-primary flex-1"
                >
                  {addingUsers ? 'Adding users...' : userIds.length > 0 ? 'Add Users & Close' : 'Done'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    handleReset();
                    onClose();
                  }}
                  className="btn btn-secondary flex-1"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ---------- Change Tenant Tier Modal ----------

interface ChangeTierModalProps {
  isOpen: boolean;
  tenant: any | null;
  availableTiers: any[];
  onClose: () => void;
  onChanged: () => void;
}

const ChangeTierModal = ({ isOpen, tenant, availableTiers, onClose, onChanged }: ChangeTierModalProps) => {
  const [currentTier, setCurrentTier] = useState<string | null>(null);
  const [selectedTier, setSelectedTier] = useState<string | null>(null);
  const [loadingCurrent, setLoadingCurrent] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !tenant) return;
    setError('');
    setSelectedTier(null);
    setLoadingCurrent(true);
    apiClient.getSystemTenantSubscription(tenant.id)
      .then(res => {
        setCurrentTier(res.data.tierName);
        setSelectedTier(res.data.tierName);
      })
      .catch(() => setError('Failed to load current tier'))
      .finally(() => setLoadingCurrent(false));
  }, [isOpen, tenant]);

  const handleChange = async () => {
    if (!selectedTier || !tenant) return;
    setSaving(true);
    setError('');
    try {
      await apiClient.changeSystemTenantTier(tenant.id, selectedTier);
      onChanged();
      onClose();
    } catch (e: any) {
      setError((e as any).response?.data?.message || 'Failed to change tier');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg max-w-md w-full">
        <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Change Tier</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-xl leading-none"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-4">
          {error && <div className="error-banner mb-4 text-sm">{error}</div>}

          {loadingCurrent ? (
            <div className="text-gray-500 dark:text-gray-400 py-4">Loading current tier...</div>
          ) : (
            <>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Tenant: <span className="font-semibold">{tenant?.name}</span>
                </label>
              </div>

              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Select Tier *
                </label>
                <select
                  value={selectedTier || ''}
                  onChange={e => setSelectedTier(e.target.value)}
                  className="form-input w-full"
                >
                  <option value="">-- Select a tier --</option>
                  {availableTiers.map(tier => (
                    <option key={tier.id} value={tier.name}>
                      {tier.displayName} {currentTier === tier.name ? '(Current)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={handleChange}
                  disabled={saving || !selectedTier || selectedTier === currentTier}
                  className="btn btn-primary flex-1"
                >
                  {saving ? 'Changing...' : 'Change Tier'}
                </button>
                <button
                  type="button"
                  onClick={onClose}
                  className="btn btn-secondary flex-1"
                >
                  Cancel
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

// ---------- Tenants Tab ----------

const TenantsTab = () => {
  const [tenants, setTenants] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [changeTierTenant, setChangeTierTenant] = useState<any | null>(null);
  const [availableTiers, setAvailableTiers] = useState<any[]>([]);
  const [tenantTiers, setTenantTiers] = useState<Record<number, string>>({});
  const [loadingTiers, setLoadingTiers] = useState(false);

  const fetchTenants = async () => {
    setLoading(true);
    try {
      const res = await apiClient.getTenants({ page: 0, size: 100 });
      setTenants(res.data || []);
      setError('');
    } catch (e: any) {
      setError((e as any).response?.data?.message || 'Failed to load tenants');
    } finally {
      setLoading(false);
    }
  };

  const fetchAvailableTiers = async () => {
    try {
      const res = await apiClient.getAiTiers();
      setAvailableTiers(res.data || []);
    } catch (e: any) {
      console.error('Failed to load tiers', e);
    }
  };

  const fetchTenantTiers = async (tenantList: any[]) => {
    setLoadingTiers(true);
    try {
      const tiers: Record<number, string> = {};
      for (const tenant of tenantList) {
        try {
          const res = await apiClient.getSystemTenantSubscription(tenant.id);
          tiers[tenant.id] = res.data.tierName;
        } catch {
          tiers[tenant.id] = 'Unknown';
        }
      }
      setTenantTiers(tiers);
    } finally {
      setLoadingTiers(false);
    }
  };

  useEffect(() => {
    fetchTenants();
    fetchAvailableTiers();
  }, []);

  useEffect(() => {
    if (tenants.length > 0) {
      fetchTenantTiers(tenants);
    }
  }, [tenants]);

  const handleDelete = async (tenant: any) => {
    if (!window.confirm(`Delete tenant "${tenant.name}"? This will permanently drop the tenant schema and all its data. This cannot be undone.`)) return;
    setDeletingId(tenant.id);
    try {
      await apiClient.deleteTenant(tenant.id);
      await fetchTenants();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete tenant');
    } finally {
      setDeletingId(null);
    }
  };

  const handleTenantCreated = (tenant: any) => {
    setTenants([tenant, ...tenants]);
    useAuthStore.getState().refreshMemberships();
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <span className="text-sm text-gray-500 dark:text-gray-400">{tenants.length} tenant{tenants.length !== 1 ? 's' : ''}</span>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-primary"
        >
          + New Tenant
        </button>
      </div>
      {error && <div className="error-banner mb-4">{error}</div>}
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Status</th>
            <th>AI Tier</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={5}>Loading...</td></tr>
          ) : tenants.length === 0 ? (
            <tr><td colSpan={5}>No tenants found</td></tr>
          ) : (
            tenants.map((t: any) => (
              <tr key={t.id}>
                <td>{t.id}</td>
                <td>{t.name}</td>
                <td>{t.status}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                      {loadingTiers ? '...' : tenantTiers[t.id] || 'Unknown'}
                    </span>
                    <button
                      onClick={() => setChangeTierTenant(t)}
                      className="btn btn-sm btn-secondary"
                      disabled={loadingTiers}
                    >
                      Change
                    </button>
                  </div>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Link to={`/admin/tenants/${t.id}/edit`} className="btn btn-sm btn-secondary">Edit</Link>
                    <Link to={`/admin/tenants/${t.id}/users`} className="btn btn-sm btn-info">Users</Link>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDelete(t)}
                      disabled={deletingId === t.id}
                    >
                      {deletingId === t.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <CreateTenantModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onTenantCreated={handleTenantCreated}
      />

      <ChangeTierModal
        isOpen={!!changeTierTenant}
        tenant={changeTierTenant}
        availableTiers={availableTiers}
        onClose={() => setChangeTierTenant(null)}
        onChanged={fetchTenants}
      />
    </div>
  );
};

// ---------- Invite User Modal ----------

interface InviteUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUserInvited: () => void;
}

const InviteUserModal = ({ isOpen, onClose, onUserInvited }: InviteUserModalProps) => {
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await apiClient.inviteUser(email, displayName || undefined);
      setEmail('');
      setDisplayName('');
      onUserInvited();
      onClose();
    } catch (e: any) {
      setError((e as any).response?.data?.message || 'Failed to invite user');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg max-w-md w-full">
        <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex justify-between items-center">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Invite User</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-xl leading-none"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-4">
          {error && <div className="error-banner mb-4 text-sm">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Email Address *
              </label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="user@example.com"
                className="form-input w-full"
                required
                autoFocus
              />
            </div>
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Display Name (optional)
              </label>
              <input
                type="text"
                value={displayName}
                onChange={e => setDisplayName(e.target.value)}
                placeholder="John Doe"
                className="form-input w-full"
              />
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-6">
              An invitation email will be sent to this address. The user can then log in via Auth0 or set a password.
            </p>
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={submitting || !email.trim()}
                className="btn btn-primary flex-1"
              >
                {submitting ? 'Inviting...' : 'Send Invite'}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-secondary flex-1"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// ---------- Users Tab ----------

const UsersTab = () => {
  const [paged, setPaged] = useState<PagedUsers | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [expandedUserId, setExpandedUserId] = useState<number | null>(null);
  const [memberships, setMemberships] = useState<Record<number, UserMembership[]>>({});
  const [membershipsLoading, setMembershipsLoading] = useState<Record<number, boolean>>({});
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchUsers = async (p: number, q: string) => {
    setLoading(true);
    try {
      const res = await apiClient.getSystemUsers({ page: p, size: 20, search: q || undefined });
      setPaged(res.data);
      setError('');
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUsers(page, search); }, [page]);

  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      setPage(0);
      fetchUsers(0, value);
    }, 300);
  };

  const handleToggleMemberships = async (userId: number) => {
    if (expandedUserId === userId) {
      setExpandedUserId(null);
      return;
    }
    setExpandedUserId(userId);
    if (memberships[userId]) return;
    setMembershipsLoading(prev => ({ ...prev, [userId]: true }));
    try {
      const res = await apiClient.getUserMemberships(userId);
      setMemberships(prev => ({ ...prev, [userId]: res.data }));
    } catch {
      setMemberships(prev => ({ ...prev, [userId]: [] }));
    } finally {
      setMembershipsLoading(prev => ({ ...prev, [userId]: false }));
    }
  };

  const handleToggleStatus = async (user: SystemUser) => {
    const newStatus = user.status === 'active' ? 'inactive' : 'active';
    if (!window.confirm(`Set user "${user.email}" status to "${newStatus}"?`)) return;
    setTogglingId(user.id);
    try {
      await apiClient.updateUserStatus(user.id, newStatus);
      fetchUsers(page, search);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to update user status');
    } finally {
      setTogglingId(null);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4 gap-4">
        <input
          type="text"
          placeholder="Search by email or name..."
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
          className="form-input"
          style={{ maxWidth: 320 }}
        />
        <button onClick={() => setShowInviteModal(true)} className="btn btn-primary">+ Invite User</button>
      </div>

      {error && <div className="error-banner mb-4">{error}</div>}

      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-4">
        <p className="text-sm text-blue-900 dark:text-blue-200">
          <strong>📌 Note:</strong> If you see duplicate users with the same email (one local, one OAuth), this is expected.
          OAuth logins create separate user records to prevent collisions. You can manage memberships for each user separately.
        </p>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>Email</th>
            <th>Display Name</th>
            <th>Auth Method</th>
            <th>Status</th>
            <th>Memberships</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={7}>Loading...</td></tr>
          ) : !paged || paged.content.length === 0 ? (
            <tr><td colSpan={7}>No users found</td></tr>
          ) : (
            paged.content.flatMap(user => {
              const getAuthBadge = () => {
                if (user.oauthProvider || user.oauthId) {
                  // Parse provider from oauthId if it contains a pipe (e.g., "google-oauth2|id" or "linkedin|id")
                  let displayProvider = user.oauthProvider;
                  if (user.oauthId && user.oauthId.includes('|')) {
                    const providerPrefix = user.oauthId.split('|')[0];
                    // Map Auth0 provider names to display names
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
                    <div className="flex flex-col gap-1">
                      <span className={`inline-block px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
                        {config.icon} {config.label}
                      </span>
                      <span className="text-xs text-gray-500 dark:text-gray-400 break-all">{user.oauthId}</span>
                    </div>
                  );
                }
                return (
                  <span className="inline-block px-2 py-1 rounded text-xs font-medium bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                    🔐 Local Login
                  </span>
                );
              };

              const rows = [(
                <tr key={user.id}>
                  <td>{user.email}</td>
                  <td>{user.displayName || <span className="text-gray-400">—</span>}</td>
                  <td>{getAuthBadge()}</td>
                  <td>
                    <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                      user.status === 'active'
                        ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                        : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                    }`}>
                      {user.status}
                    </span>
                  </td>
                  <td>{user.membershipCount}</td>
                  <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => handleToggleMemberships(user.id)}
                      >
                        {expandedUserId === user.id ? 'Hide' : 'Tenants'}
                      </button>
                      <button
                        className={`btn btn-sm ${user.status === 'active' ? 'btn-danger' : 'btn-success'}`}
                        onClick={() => handleToggleStatus(user)}
                        disabled={togglingId === user.id}
                      >
                        {togglingId === user.id ? '...' : user.status === 'active' ? 'Deactivate' : 'Activate'}
                      </button>
                    </div>
                  </td>
                </tr>
              )];

              if (expandedUserId === user.id) {
                rows.push(
                  <tr key={`${user.id}-memberships`} className="bg-gray-50 dark:bg-gray-900/40">
                    <td colSpan={7} className="px-6 py-3">
                      {membershipsLoading[user.id] ? (
                        <span className="text-sm text-gray-400">Loading memberships...</span>
                      ) : !memberships[user.id] || memberships[user.id].length === 0 ? (
                        <span className="text-sm text-gray-400">No tenant memberships</span>
                      ) : (
                        <table className="data-table" style={{ margin: 0 }}>
                          <thead>
                            <tr>
                              <th>Tenant</th>
                              <th>Role</th>
                              <th>Status</th>
                              <th>Joined</th>
                            </tr>
                          </thead>
                          <tbody>
                            {memberships[user.id].map(m => (
                              <tr key={m.membershipId}>
                                <td>{m.tenantName || m.tenantId}</td>
                                <td>{m.role}</td>
                                <td>{m.status}</td>
                                <td>{new Date(m.createdAt).toLocaleDateString()}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </td>
                  </tr>
                );
              }

              return rows;
            })
          )}
        </tbody>
      </table>

      {paged && paged.totalPages > 1 && (
        <div className="flex items-center gap-3 mt-4 text-sm text-gray-600 dark:text-gray-400">
          <button
            className="btn btn-sm btn-secondary"
            onClick={() => setPage(p => p - 1)}
            disabled={!paged.hasPrev}
          >
            Previous
          </button>
          <span>Page {paged.currentPage + 1} of {paged.totalPages} ({paged.totalElements.toLocaleString()} users)</span>
          <button
            className="btn btn-sm btn-secondary"
            onClick={() => setPage(p => p + 1)}
            disabled={!paged.hasNext}
          >
            Next
          </button>
        </div>
      )}

      <InviteUserModal
        isOpen={showInviteModal}
        onClose={() => setShowInviteModal(false)}
        onUserInvited={() => fetchUsers(page, search)}
      />
    </div>
  );
};

// ---------- AI Models Tab ----------

interface AiModel {
  id: number;
  provider: string;
  modelId: string;
  enabled: boolean;
}

const AiModelsTab = () => {
  const [models, setModels] = useState<AiModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [newProvider, setNewProvider] = useState('');
  const [newModelId, setNewModelId] = useState('');
  const [newEnabled, setNewEnabled] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [availableModels, setAvailableModels] = useState<{ modelId: string; hasQuota: boolean | null }[]>([]);
  const [loadingAvailable, setLoadingAvailable] = useState(false);
  const [availableLoadError, setAvailableLoadError] = useState(false);
  const [useDropdown, setUseDropdown] = useState(false);

  const loadModels = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.getAiModels();
      setModels(res.data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load AI models');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadModels();
  }, []);

  const handleAddModel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProvider.trim() || !newModelId.trim()) {
      setError('Provider and Model ID are required');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await apiClient.createAiModel({
        provider: newProvider.trim(),
        modelId: newModelId.trim(),
        enabled: newEnabled
      });
      loadModels();
      setNewProvider('');
      setNewModelId('');
      setNewEnabled(true);
      setShowAddModal(false);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to add model');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleEnabled = async (model: AiModel) => {
    setTogglingId(model.id);
    try {
      await apiClient.updateAiModel(model.id, { enabled: !model.enabled });
      loadModels();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to update model');
    } finally {
      setTogglingId(null);
    }
  };

  const handleDeleteModel = async (model: AiModel) => {
    if (!window.confirm(`Are you sure you want to delete ${model.provider}/${model.modelId}?`)) return;
    setDeletingId(model.id);
    try {
      await apiClient.deleteAiModel(model.id);
      loadModels();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete model');
    } finally {
      setDeletingId(null);
    }
  };

  const handleLoadFromApi = async () => {
    setLoadingAvailable(true);
    setAvailableLoadError(false);
    try {
      const res = await apiClient.getAvailableModelsFromProvider(newProvider);
      const models = res.data ?? [];
      if (models.length > 0) {
        setAvailableModels(models);
        setNewModelId(models[0].modelId);
        setUseDropdown(true);
      } else {
        setAvailableLoadError(true);
      }
    } catch {
      setAvailableLoadError(true);
    } finally {
      setLoadingAvailable(false);
    }
  };

  const resetModalState = () => {
    setAvailableModels([]);
    setLoadingAvailable(false);
    setAvailableLoadError(false);
    setUseDropdown(false);
  };

  // Group models by provider
  const modelsByProvider = models.reduce((acc, model) => {
    if (!acc[model.provider]) acc[model.provider] = [];
    acc[model.provider].push(model);
    return acc;
  }, {} as Record<string, AiModel[]>);

  return (
    <div>
      <div className="flex justify-between items-center mb-4 gap-4">
        <div className="text-sm text-gray-600 dark:text-gray-400">
          Total models: {models.length}
        </div>
        <button onClick={() => setShowAddModal(true)} className="btn btn-primary">+ Add Model</button>
      </div>

      {error && <div className="error-banner mb-4">{error}</div>}

      {loading ? (
        <div className="text-gray-500 dark:text-gray-400 py-8">Loading AI models...</div>
      ) : models.length === 0 ? (
        <div className="text-gray-500 dark:text-gray-400 py-8">No AI models configured</div>
      ) : (
        <div className="space-y-6">
          {Object.entries(modelsByProvider).map(([provider, providerModels]) => (
            <div key={provider} className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
              <div className="bg-gray-50 dark:bg-gray-800 px-4 py-3 font-semibold text-gray-900 dark:text-white">
                {provider.charAt(0).toUpperCase() + provider.slice(1)} ({providerModels.length})
              </div>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Model ID</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {providerModels.map(model => (
                    <tr key={model.id}>
                      <td>
                        <code className="bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded text-sm">{model.modelId}</code>
                      </td>
                      <td>
                        <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                          model.enabled
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                        }`}>
                          {model.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button
                            className={`btn btn-sm ${model.enabled ? 'btn-danger' : 'btn-success'}`}
                            onClick={() => handleToggleEnabled(model)}
                            disabled={togglingId === model.id}
                          >
                            {togglingId === model.id ? '...' : model.enabled ? 'Disable' : 'Enable'}
                          </button>
                          <button
                            className="btn btn-sm btn-danger"
                            onClick={() => handleDeleteModel(model)}
                            disabled={deletingId === model.id}
                          >
                            {deletingId === model.id ? '...' : 'Delete'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}

      {/* Add Model Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg max-w-md w-full">
            <div className="border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Add AI Model</h2>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setNewProvider('');
                  setNewModelId('');
                  setError('');
                  resetModalState();
                }}
                className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-xl"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleAddModel} className="px-6 py-4">
              {error && <div className="error-banner mb-4 text-sm">{error}</div>}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Provider *
                </label>
                <select
                  value={newProvider}
                  onChange={e => {
                    setNewProvider(e.target.value);
                    setNewModelId('');
                    setAvailableModels([]);
                    setUseDropdown(false);
                    setAvailableLoadError(false);
                  }}
                  className="form-input w-full"
                  required
                >
                  <option value="">Select provider...</option>
                  <option value="anthropic">Anthropic</option>
                  <option value="openai">OpenAI</option>
                  <option value="google">Google</option>
                  <option value="ollama">Ollama (self-hosted)</option>
                </select>
              </div>
              <div className="mb-4">
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Model ID *
                  </label>
                  {(newProvider === 'anthropic' || newProvider === 'google' || newProvider === 'openai' || newProvider === 'ollama') && (
                    <button
                      type="button"
                      onClick={handleLoadFromApi}
                      disabled={loadingAvailable}
                      className="text-xs text-blue-600 dark:text-blue-400 hover:underline disabled:opacity-50"
                    >
                      {loadingAvailable ? 'Loading...' : 'Load from API'}
                    </button>
                  )}
                </div>

                {useDropdown ? (
                  <select
                    value={newModelId}
                    onChange={e => setNewModelId(e.target.value)}
                    className="form-input w-full"
                    required
                  >
                    <option value="">Select a model...</option>
                    {availableModels.map(model => (
                      <option key={model.modelId} value={model.modelId}>
                        {model.modelId}
                        {model.hasQuota === true ? ' ✓' : model.hasQuota === false ? ' ✗' : ''}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="text"
                    value={newModelId}
                    onChange={e => setNewModelId(e.target.value)}
                    placeholder="e.g. gemini-2.5-flash-lite-preview"
                    className="form-input w-full"
                    required
                  />
                )}

                {availableLoadError && (
                  <p className="text-xs text-amber-600 dark:text-amber-400 mt-1">
                    Could not load models from provider API. Enter the model ID manually.
                  </p>
                )}
              </div>
              <div className="mb-6">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={newEnabled}
                    onChange={e => setNewEnabled(e.target.checked)}
                    className="w-4 h-4"
                  />
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Enabled by default</span>
                </label>
              </div>
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={submitting || !newProvider.trim() || !newModelId.trim()}
                  className="btn btn-primary flex-1"
                >
                  {submitting ? 'Adding...' : 'Add Model'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    setNewProvider('');
                    setNewModelId('');
                    setError('');
                    resetModalState();
                  }}
                  className="btn btn-secondary flex-1"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

// ---------- AI Tiers Tab ----------

interface AiTier {
  id: number;
  name: string;
  displayName: string;
  monthlyTokenLimit: number;
  modelId: string;
  provider: string;
  priceMonthly: number;
  enabled: boolean;
}

const AssistantTiersTab = () => {
  const [tiers, setTiers] = useState<AiTier[]>([]);
  const [models, setModels] = useState<AiModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editData, setEditData] = useState<Partial<AiTier>>({});
  const [applyingAllTierId, setApplyingAllTierId] = useState<number | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [tiersRes, modelsRes] = await Promise.all([
        apiClient.getAiTiers(),
        apiClient.getAiModels(),
      ]);
      setTiers(tiersRes.data);
      setModels(modelsRes.data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load AI configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (tier: AiTier) => {
    setEditingId(tier.id);
    setEditData({ ...tier });
  };

  const handleSave = async (tierId: number) => {
    try {
      setError('');
      await apiClient.updateAiTier(tierId, editData);
      setEditingId(null);
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update AI tier');
    }
  };

  const handleApplyToAllTenants = async (tierName: string) => {
    if (!window.confirm(`Apply "${tierName}" tier to ALL tenants? This will override current tier assignments.`)) {
      return;
    }
    setApplyingAllTierId(tiers.find(t => t.name === tierName)?.id || null);
    try {
      setError('');
      await apiClient.applyAiTiersToAllTenants(tierName);
      setTimeout(() => setApplyingAllTierId(null), 500);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to apply tier to all tenants');
      setApplyingAllTierId(null);
    }
  };

  const handleToggleEnabled = async (tier: AiTier) => {
    try {
      await apiClient.updateAiTier(tier.id, { enabled: !tier.enabled });
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update tier');
    }
  };

  if (loading) {
    return <div className="text-gray-500 dark:text-gray-400 py-8">Loading AI tiers...</div>;
  }

  return (
    <div className="space-y-4">
      {error && <div className="error-banner mb-4">{error}</div>}

      {tiers.length === 0 ? (
        <div className="text-gray-500 dark:text-gray-400 py-8">No AI tiers configured</div>
      ) : (
        <div className="space-y-4">
          {tiers.map((tier) => (
            <div key={tier.id} className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
              {editingId === tier.id ? (
                <div className="p-6 space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Display Name</label>
                      <input
                        type="text"
                        value={editData.displayName || ''}
                        onChange={(e) => setEditData({ ...editData, displayName: e.target.value })}
                        className="form-input w-full"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Provider</label>
                      <select
                        value={editData.provider || 'anthropic'}
                        onChange={(e) => {
                          const newProvider = e.target.value;
                          const availableModels = models.filter(m => m.provider === newProvider && m.enabled);
                          setEditData({
                            ...editData,
                            provider: newProvider,
                            modelId: availableModels.length > 0 ? availableModels[0].modelId : ''
                          });
                        }}
                        className="form-input w-full"
                      >
                        <option value="anthropic">Anthropic</option>
                        <option value="openai">OpenAI</option>
                        <option value="google">Google</option>
                        <option value="ollama">Ollama</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Model ID</label>
                      <select
                        value={editData.modelId || ''}
                        onChange={(e) => setEditData({ ...editData, modelId: e.target.value })}
                        className="form-input w-full"
                      >
                        <option value="">Select a model</option>
                        {models
                          .filter(m => m.provider === (editData.provider || 'anthropic') && m.enabled)
                          .map(m => (
                            <option key={m.id} value={m.modelId}>
                              {m.modelId}
                            </option>
                          ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Monthly Token Limit</label>
                      <input
                        type="number"
                        value={editData.monthlyTokenLimit || 0}
                        onChange={(e) => setEditData({ ...editData, monthlyTokenLimit: parseInt(e.target.value) })}
                        className="form-input w-full"
                        placeholder="-1 for unlimited"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Price (Monthly)</label>
                      <input
                        type="number"
                        step="0.01"
                        value={editData.priceMonthly || 0}
                        onChange={(e) => setEditData({ ...editData, priceMonthly: parseFloat(e.target.value) })}
                        className="form-input w-full"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Status</label>
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={editData.enabled !== false}
                          onChange={(e) => setEditData({ ...editData, enabled: e.target.checked })}
                          className="w-4 h-4"
                        />
                        <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                          editData.enabled !== false
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                        }`}>
                          {editData.enabled !== false ? '✓ Enabled' : '✗ Disabled'}
                        </span>
                      </label>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleSave(tier.id)}
                      className="btn btn-primary"
                    >
                      Save Changes
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="btn btn-secondary"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{tier.displayName}</h3>
                        <label className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={tier.enabled}
                            onChange={() => handleToggleEnabled(tier)}
                            className="w-4 h-4"
                          />
                          <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                            tier.enabled
                              ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                              : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                          }`}>
                            {tier.enabled ? '✓ Enabled' : '✗ Disabled'}
                          </span>
                        </label>
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        <span className="font-medium">Tier:</span> {tier.name}
                      </p>
                    </div>
                    <div className="text-right">
                      <div className="text-2xl font-bold text-gray-900 dark:text-white">${tier.priceMonthly}/mo</div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                    <div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 uppercase font-semibold">Model</p>
                      <p className="text-sm font-mono text-gray-900 dark:text-white break-all">{tier.modelId}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 uppercase font-semibold">Provider</p>
                      <p className="text-sm font-medium text-gray-900 dark:text-white capitalize">{tier.provider}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 uppercase font-semibold">Token Limit</p>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">
                        {tier.monthlyTokenLimit === -1 ? 'Unlimited' : tier.monthlyTokenLimit.toLocaleString()}
                      </p>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      onClick={() => handleEdit(tier)}
                      className="btn btn-sm btn-secondary"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleApplyToAllTenants(tier.name)}
                      disabled={applyingAllTierId === tier.id}
                      className="btn btn-sm btn-success"
                    >
                      {applyingAllTierId === tier.id ? 'Applying...' : 'Apply to All Tenants'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ---------- Main Page ----------

export const SystemAdminPage = () => {
  usePageTitle('System Admin');
  const { currentRole } = useAuthStore();
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  if (currentRole !== 'system_admin') {
    return (
      <Layout>
        <div className="page">
          <div className="error-banner">Access denied. This page requires system admin privileges.</div>
        </div>
      </Layout>
    );
  }

  const tabs: { id: Tab; label: string }[] = [
    { id: 'overview', label: '📊 Overview' },
    { id: 'tenants', label: '🏢 Tenants' },
    { id: 'users', label: '👤 Users' },
    { id: 'ai-models', label: '🤖 AI Models' },
    { id: 'ai-tiers', label: '💎 AI Tiers' },
  ];

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>System Administration</h1>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="flex space-x-1">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2 text-sm font-medium rounded-t-md transition-colors ${
                  activeTab === tab.id
                    ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400 dark:border-blue-400'
                    : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {activeTab === 'overview' && <OverviewTab />}
        {activeTab === 'tenants' && <TenantsTab />}
        {activeTab === 'users' && <UsersTab />}
        {activeTab === 'ai-models' && <AiModelsTab />}
        {activeTab === 'ai-tiers' && <AssistantTiersTab />}
      </div>
    </Layout>
  );
};
