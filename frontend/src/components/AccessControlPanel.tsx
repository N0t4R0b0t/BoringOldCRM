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
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';

interface AccessGrant {
  id: number;
  granteeType: string;
  granteeId: number;
  granteeName: string;
  permission: string;
}

interface AccessSummary {
  entityType: string;
  entityId: number;
  accessMode: string | null;
  ownerId: number | null;
  ownerEmail: string | null;
  grants: AccessGrant[];
  canManage: boolean;
}

interface AccessControlPanelProps {
  entityType: string;
  entityId: number;
  ownerId?: number;
}

export const AccessControlPanel = ({ entityType, entityId, ownerId }: AccessControlPanelProps) => {
  const { currentRole } = useAuthStore();
  const [summary, setSummary] = useState<AccessSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [collapsed, setCollapsed] = useState(true);
  const [groups, setGroups] = useState<{ id: number; name: string }[]>([]);
  const [teamMembers, setTeamMembers] = useState<{ userId: number; userEmail: string; userName?: string }[]>([]);
  const [granteeType, setGranteeType] = useState<'USER' | 'GROUP'>('USER');
  const [granteeId, setGranteeId] = useState('');
  const [permission, setPermission] = useState('READ');
  const [adding, setAdding] = useState(false);

  const isManagerOrAdmin = currentRole === 'manager' || currentRole === 'admin';

  useEffect(() => {
    fetchSummary();
    apiClient.getTenantMembers().then((r) => setTeamMembers(r.data || [])).catch(() => {});
    if (isManagerOrAdmin) {
      apiClient.getGroups().then((r) => setGroups(r.data || [])).catch(() => {});
    }
  }, [entityType, entityId, ownerId]);

  const fetchSummary = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.getAccessSummary(entityType, entityId, ownerId);
      setSummary(res.data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load access info');
    } finally {
      setLoading(false);
    }
  };

  const handleSetPolicy = async (mode: string) => {
    try {
      if (mode === 'OPEN') {
        await apiClient.removeAccessPolicy(entityType, entityId);
      } else {
        await apiClient.setAccessPolicy(entityType, entityId, mode);
      }
      fetchSummary();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to update policy');
    }
  };

  const handleAddGrant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!granteeId) return;
    setAdding(true);
    try {
      await apiClient.addAccessGrant(entityType, entityId, {
        granteeType,
        granteeId: parseInt(granteeId),
        permission,
      });
      setGranteeId('');
      fetchSummary();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to add grant');
    } finally {
      setAdding(false);
    }
  };

  const handleRemoveGrant = async (grantId: number) => {
    try {
      await apiClient.removeAccessGrant(entityType, entityId, grantId);
      fetchSummary();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to remove grant');
    }
  };

  if (!summary && loading) return null;

  if (!summary) return null;

  if (!summary.canManage && (!summary.accessMode || summary.accessMode === 'OPEN')) {
    return null;
  }

  const accessModeLabel = (mode: string | null) => {
    if (!mode || mode === 'OPEN') return { label: 'Open', cls: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300' };
    if (mode === 'READ_ONLY') return { label: 'Read-only', cls: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300' };
    if (mode === 'HIDDEN') return { label: 'Hidden', cls: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300' };
    return { label: mode, cls: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300' };
  };

  const { label, cls } = accessModeLabel(summary.accessMode);
  const ownerName = teamMembers.find((m) => m.userId === summary.ownerId)?.userName
    ?? teamMembers.find((m) => m.userId === summary.ownerId)?.userEmail
    ?? summary.ownerEmail;

  return (
    <div className="mt-4 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
      {/* Header — always visible, click to toggle */}
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full flex items-center justify-between px-4 py-3 bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-left"
      >
        <div className="flex items-center gap-3">
          <span className="text-sm font-semibold text-gray-700 dark:text-gray-200">Access Control</span>
          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>{label}</span>
          {ownerName && (
            <span className="text-xs text-gray-500 dark:text-gray-400">Owner: {ownerName}</span>
          )}
        </div>
        <svg
          className={`w-4 h-4 text-gray-400 transition-transform ${collapsed ? '' : 'rotate-180'}`}
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {!collapsed && (
        <div className="px-4 py-4 space-y-4 bg-white dark:bg-gray-900">
          {error && <div className="error-banner">{error}</div>}

          {/* Visibility policy buttons */}
          {summary.canManage && (
            <div>
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">Visibility</p>
              <div className="flex gap-2">
                {(['OPEN', 'READ_ONLY', 'HIDDEN'] as const).map((mode) => {
                  const active = mode === 'OPEN'
                    ? (!summary.accessMode || summary.accessMode === 'OPEN')
                    : summary.accessMode === mode;
                  const modeLabel = mode === 'READ_ONLY' ? 'Read-only' : mode.charAt(0) + mode.slice(1).toLowerCase();
                  return (
                    <button
                      key={mode}
                      type="button"
                      onClick={() => handleSetPolicy(mode)}
                      className={`px-3 py-1.5 rounded-md text-xs font-medium border transition-colors ${
                        active
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
                      }`}
                    >
                      {modeLabel}
                    </button>
                  );
                })}
              </div>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1.5">
                Read-only: others can view but not edit. Hidden: invisible unless granted access.
              </p>
            </div>
          )}

          {/* Grants table */}
          {summary.grants && summary.grants.length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">Access Grants</p>
              <div className="rounded-md border border-gray-200 dark:border-gray-700 overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 dark:bg-gray-800">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400">Grantee</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400">Type</th>
                      <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400">Permission</th>
                      {summary.canManage && <th className="px-3 py-2" />}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                    {summary.grants.map((g) => (
                      <tr key={g.id} className="bg-white dark:bg-gray-900">
                        <td className="px-3 py-2 text-gray-900 dark:text-gray-100">{g.granteeName}</td>
                        <td className="px-3 py-2 text-gray-500 dark:text-gray-400 capitalize">{g.granteeType.toLowerCase()}</td>
                        <td className="px-3 py-2">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                            g.permission === 'WRITE'
                              ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
                              : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                          }`}>{g.permission}</span>
                        </td>
                        {summary.canManage && (
                          <td className="px-3 py-2 text-right">
                            <button
                              type="button"
                              onClick={() => handleRemoveGrant(g.id)}
                              className="text-xs text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300 font-medium"
                            >
                              Remove
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Add grant form */}
          {summary.canManage && summary.accessMode && summary.accessMode !== 'OPEN' && (
            <div>
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">Add Grant</p>
              <form onSubmit={handleAddGrant} className="grid grid-cols-[auto_1fr_auto_auto] gap-2 items-end">
                <div>
                  <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">Type</label>
                  <select
                    value={granteeType}
                    onChange={(e) => { setGranteeType(e.target.value as 'USER' | 'GROUP'); setGranteeId(''); }}
                    className="px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="USER">User</option>
                    <option value="GROUP">Group</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">
                    {granteeType === 'USER' ? 'User' : 'Group'}
                  </label>
                  {granteeType === 'GROUP' ? (
                    <select
                      value={granteeId}
                      onChange={(e) => setGranteeId(e.target.value)}
                      required
                      className="w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">Select group…</option>
                      {groups.map((g) => (
                        <option key={g.id} value={g.id}>{g.name}</option>
                      ))}
                    </select>
                  ) : (
                    <select
                      value={granteeId}
                      onChange={(e) => setGranteeId(e.target.value)}
                      required
                      className="w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">Select user…</option>
                      {teamMembers.map((m) => (
                        <option key={m.userId} value={m.userId}>{m.userName ?? m.userEmail}</option>
                      ))}
                    </select>
                  )}
                </div>
                <div>
                  <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">Permission</label>
                  <select
                    value={permission}
                    onChange={(e) => setPermission(e.target.value)}
                    className="px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="READ">Read</option>
                    <option value="WRITE">Write</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">&nbsp;</label>
                  <button
                    type="submit"
                    disabled={adding || !granteeId}
                    className="px-3 py-1.5 text-sm font-medium bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors"
                  >
                    {adding ? 'Adding…' : 'Add'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
