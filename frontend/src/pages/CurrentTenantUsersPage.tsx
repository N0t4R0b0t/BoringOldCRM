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
import { Fragment, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { TenantUsersList } from '../components/TenantUsersList';
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { usePageTitle } from '../hooks/usePageTitle';

interface GroupMember {
  userId: number;
  userEmail: string;
  displayName: string;
}

interface Group {
  id: number;
  name: string;
  description: string;
  memberCount: number;
}

export const CurrentTenantUsersPage = () => {
  usePageTitle('Users');
  const { currentTenant } = useAuthStore();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'groups' ? 'groups' : 'users';

  // Groups state
  const [groups, setGroups] = useState<Group[]>([]);
  const [loadingGroups, setLoadingGroups] = useState(false);
  const [groupsError, setGroupsError] = useState('');
  const [expandedGroupId, setExpandedGroupId] = useState<number | null>(null);
  const [groupMembers, setGroupMembers] = useState<Record<number, GroupMember[]>>({});
  const [showAddUser, setShowAddUser] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [creating, setCreating] = useState(false);
  const [tenantUsers, setTenantUsers] = useState<any[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<Set<number>>(new Set());
  const [userSearch, setUserSearch] = useState('');
  const [addingMember, setAddingMember] = useState(false);

  useEffect(() => {
    if (activeTab === 'groups') {
      fetchGroups();
    }
  }, [activeTab]);

  const fetchGroups = async () => {
    setLoadingGroups(true);
    setGroupsError('');
    try {
      const res = await apiClient.getGroups();
      setGroups(res.data || []);
    } catch (e: any) {
      setGroupsError(e.response?.data?.message || 'Failed to load groups');
    } finally {
      setLoadingGroups(false);
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName) return;
    setCreating(true);
    try {
      await apiClient.createGroup({ name: newName, description: newDesc || undefined });
      setNewName('');
      setNewDesc('');
      setShowCreate(false);
      fetchGroups();
    } catch (e: any) {
      setGroupsError(e.response?.data?.message || 'Failed to create group');
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteGroup = async (id: number, name: string) => {
    if (!window.confirm(`Delete group "${name}"?`)) return;
    try {
      await apiClient.deleteGroup(id);
      fetchGroups();
    } catch (e: any) {
      setGroupsError(e.response?.data?.message || 'Failed to delete group');
    }
  };

  const handleExpand = async (groupId: number) => {
    if (expandedGroupId === groupId) {
      setExpandedGroupId(null);
      setSelectedUserIds(new Set());
      setUserSearch('');
      return;
    }
    setExpandedGroupId(groupId);
    setSelectedUserIds(new Set());
    setUserSearch('');
    const promises: Promise<any>[] = [];
    if (!groupMembers[groupId]) {
      promises.push(
        apiClient.getGroupMembers(groupId)
          .then((res) => setGroupMembers((prev) => ({ ...prev, [groupId]: res.data || [] })))
          .catch(() => setGroupMembers((prev) => ({ ...prev, [groupId]: [] })))
      );
    }
    if (tenantUsers.length === 0 && currentTenant?.id) {
      promises.push(
        apiClient.getTenantUsers(currentTenant.id)
          .then((res) => setTenantUsers(res.data || []))
          .catch(() => {})
      );
    }
    await Promise.all(promises);
  };

  const handleToggleUser = (userId: number) => {
    setSelectedUserIds((prev) => {
      const next = new Set(prev);
      next.has(userId) ? next.delete(userId) : next.add(userId);
      return next;
    });
  };

  const handleAddMembers = async (groupId: number) => {
    if (selectedUserIds.size === 0) return;
    setAddingMember(true);
    try {
      await Promise.all([...selectedUserIds].map((uid) => apiClient.addGroupMember(groupId, uid)));
      setSelectedUserIds(new Set());
      setUserSearch('');
      const res = await apiClient.getGroupMembers(groupId);
      setGroupMembers((prev) => ({ ...prev, [groupId]: res.data || [] }));
      fetchGroups();
    } catch (e: any) {
      setGroupsError(e.response?.data?.message || 'Failed to add members');
    } finally {
      setAddingMember(false);
    }
  };

  const handleRemoveMember = async (groupId: number, userId: number) => {
    try {
      await apiClient.removeGroupMember(groupId, userId);
      const res = await apiClient.getGroupMembers(groupId);
      setGroupMembers((prev) => ({ ...prev, [groupId]: res.data || [] }));
      fetchGroups();
    } catch (e: any) {
      setGroupsError(e.response?.data?.message || 'Failed to remove member');
    }
  };

  const switchTab = (tab: 'users' | 'groups') => {
    setSearchParams(tab === 'users' ? {} : { tab });
  };

  if (!currentTenant) {
    return (
      <Layout>
        <div className="page">
          <div className="error-banner">No active tenant selected.</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>User Management</h1>
          <Link to="/admin" className="btn btn-secondary">Back to Admin</Link>
        </div>
        <div className="mb-4">
          <h3 className="text-muted">Tenant: {currentTenant.name}</h3>
        </div>

        <div className="tabs mb-4">
          <button
            className={`tab${activeTab === 'users' ? ' active' : ''}`}
            onClick={() => switchTab('users')}
          >
            Users
          </button>
          <button
            className={`tab${activeTab === 'groups' ? ' active' : ''}`}
            onClick={() => switchTab('groups')}
          >
            Groups
          </button>
        </div>

        {activeTab === 'users' && (
          <>
            <div className="page-header mb-4">
              <div />
              <button className="btn btn-primary" onClick={() => setShowAddUser(!showAddUser)}>
                {showAddUser ? 'Cancel' : '+ New User'}
              </button>
            </div>
            <TenantUsersList tenantId={currentTenant.id} showAddForm={showAddUser} />
          </>
        )}

        {activeTab === 'groups' && (
          <>
            <div className="page-header mb-4">
              <div />
              <button className="btn btn-primary" onClick={() => setShowCreate(!showCreate)}>
                {showCreate ? 'Cancel' : '+ New Group'}
              </button>
            </div>

            {groupsError && <div className="error-banner">{groupsError}</div>}

            {showCreate && (
              <div className="content-card mb-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">
                  Create Group
                </h3>
                <form onSubmit={handleCreateGroup}>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4">
                    <div>
                      <label htmlFor="newName" className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                        Name *
                      </label>
                      <input
                        id="newName"
                        type="text"
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        placeholder="Group name"
                        className="form-input"
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="newDesc" className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                        Description
                      </label>
                      <input
                        id="newDesc"
                        type="text"
                        value={newDesc}
                        onChange={(e) => setNewDesc(e.target.value)}
                        placeholder="Optional description"
                        className="form-input"
                      />
                    </div>
                  </div>
                  <div className="form-actions mt-3">
                    <button type="button" className="btn btn-secondary" onClick={() => setShowCreate(false)}>
                      Cancel
                    </button>
                    <button type="submit" className="btn btn-primary" disabled={creating}>
                      {creating ? 'Creating...' : 'Create Group'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {loadingGroups ? (
              <div className="content-card">
                <p className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">Loading...</p>
              </div>
            ) : groups.length === 0 ? (
              <div className="content-card">
                <p className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">
                  No groups yet. Create one to manage team access.
                </p>
              </div>
            ) : (
              <div className="content-card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Description</th>
                      <th>Members</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((g) => (
                      <Fragment key={g.id}>
                        <tr className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                          <td className="font-medium text-gray-900 dark:text-gray-100">{g.name}</td>
                          <td className="text-gray-500 dark:text-gray-400 text-sm">{g.description || '—'}</td>
                          <td>
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300">
                              {g.memberCount} {g.memberCount === 1 ? 'member' : 'members'}
                            </span>
                          </td>
                          <td className="text-right">
                            <div className="flex items-center justify-end gap-4">
                              <button
                                type="button"
                                className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium text-sm"
                                onClick={() => handleExpand(g.id)}
                              >
                                {expandedGroupId === g.id ? 'Collapse' : 'Manage Members'}
                              </button>
                              <button
                                type="button"
                                className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium text-sm"
                                onClick={() => handleDeleteGroup(g.id, g.name)}
                              >
                                Delete
                              </button>
                            </div>
                          </td>
                        </tr>
                        {expandedGroupId === g.id && (
                          <tr key={`${g.id}-members`}>
                            <td colSpan={4} className="p-0">
                              <div className="px-4 py-4 bg-gray-50 dark:bg-gray-800/60 border-t border-b border-gray-200 dark:border-gray-700">
                                {(groupMembers[g.id] || []).length > 0 && (
                                  <table className="data-table text-sm mb-4">
                                    <thead>
                                      <tr>
                                        <th>Name</th>
                                        <th>Email</th>
                                        <th></th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      {(groupMembers[g.id] || []).map((m) => (
                                        <tr key={m.userId} className="hover:bg-gray-100 dark:hover:bg-gray-700/50">
                                          <td className="text-gray-700 dark:text-gray-300">{m.displayName || '—'}</td>
                                          <td className="text-gray-500 dark:text-gray-400">{m.userEmail}</td>
                                          <td className="text-right">
                                            <button
                                              type="button"
                                              className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium text-xs"
                                              onClick={() => handleRemoveMember(g.id, m.userId)}
                                            >
                                              Remove
                                            </button>
                                          </td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                )}
                                {(() => {
                                  const currentMembers = groupMembers[g.id] || [];
                                  const memberIds = new Set(currentMembers.map((m) => m.userId));
                                  const available = tenantUsers.filter(
                                    (u) => !memberIds.has(u.userId) &&
                                      (userSearch === '' ||
                                        u.userEmail?.toLowerCase().includes(userSearch.toLowerCase()) ||
                                        u.displayName?.toLowerCase().includes(userSearch.toLowerCase()))
                                  );
                                  return (
                                    <div className={currentMembers.length > 0 ? 'border-t border-gray-200 dark:border-gray-700 pt-3' : ''}>
                                      <div className="flex items-center justify-between mb-2">
                                        <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                                          Add Members
                                        </span>
                                        {selectedUserIds.size > 0 && (
                                          <button
                                            type="button"
                                            className="btn btn-primary btn-sm"
                                            onClick={() => handleAddMembers(g.id)}
                                            disabled={addingMember}
                                          >
                                            {addingMember ? 'Adding...' : `Add ${selectedUserIds.size} user${selectedUserIds.size !== 1 ? 's' : ''}`}
                                          </button>
                                        )}
                                      </div>
                                      <input
                                        type="text"
                                        placeholder="Search users..."
                                        value={userSearch}
                                        onChange={(e) => setUserSearch(e.target.value)}
                                        className="form-input mb-2 text-sm"
                                      />
                                      {available.length === 0 ? (
                                        <p className="text-sm text-gray-500 dark:text-gray-400 m-0">
                                          {tenantUsers.length === 0 ? 'Loading users...' : 'No users available to add.'}
                                        </p>
                                      ) : (
                                        <div className="max-h-44 overflow-y-auto rounded-md border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800">
                                          {available.map((u) => (
                                            <label
                                              key={u.userId}
                                              className="flex items-center gap-2.5 px-3 py-2 cursor-pointer text-sm border-b border-gray-100 dark:border-gray-700 last:border-b-0 hover:bg-gray-50 dark:hover:bg-gray-700"
                                            >
                                              <input
                                                type="checkbox"
                                                checked={selectedUserIds.has(u.userId)}
                                                onChange={() => handleToggleUser(u.userId)}
                                              />
                                              <span className="flex-1 text-gray-800 dark:text-gray-200">{u.userEmail}</span>
                                              {u.role && (
                                                <span className="text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">
                                                  {u.role}
                                                </span>
                                              )}
                                            </label>
                                          ))}
                                        </div>
                                      )}
                                    </div>
                                  );
                                })()}
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  );
};
