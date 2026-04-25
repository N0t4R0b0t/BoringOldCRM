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
import { apiClient } from '../api/apiClient';
import { useAuthStore } from '../store/authStore';
import { X, Send, AlertCircle } from 'lucide-react';

interface TenantUser {
  id: number;
  userEmail: string;
  userId: number;
  role: string;
}

interface NotificationComposerModalProps {
  entityType: string;
  entityId: number;
  entityName: string;
  onClose: () => void;
  onSuccess?: () => void;
  opportunityContacts?: { id: number; name: string; email: string }[];
}

export const NotificationComposerModal = ({
  entityType,
  entityId,
  entityName,
  onClose,
  onSuccess,
  opportunityContacts,
}: NotificationComposerModalProps) => {
  const { currentTenant } = useAuthStore();
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [selectedUserIds, setSelectedUserIds] = useState<Set<number>>(new Set());
  const [users, setUsers] = useState<TenantUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [showOtherTeamMembers, setShowOtherTeamMembers] = useState(false);

  useEffect(() => {
    loadTenantUsers();
  }, [currentTenant?.id]);

  useEffect(() => {
    if (!loading && opportunityContacts && users.length > 0) {
      // Pre-select users whose emails match opportunity contacts (case-insensitive)
      const contactEmails = new Set(opportunityContacts.map((c) => c.email.toLowerCase()));
      const matchedUserIds = new Set(
        users
          .filter((u) => contactEmails.has(u.userEmail.toLowerCase()))
          .map((u) => u.userId)
      );
      setSelectedUserIds(matchedUserIds);
    }
  }, [loading, opportunityContacts, users]);

  const loadTenantUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      if (!currentTenant?.id) return;
      const response = await apiClient.getTenantUsers(currentTenant.id);
      setUsers(response.data || []);
    } catch (err) {
      console.error('Failed to load tenant users:', err);
      setError('Failed to load team members');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleUser = (userId: number) => {
    const newSelected = new Set(selectedUserIds);
    if (newSelected.has(userId)) {
      newSelected.delete(userId);
    } else {
      newSelected.add(userId);
    }
    setSelectedUserIds(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedUserIds.size === users.length) {
      setSelectedUserIds(new Set());
    } else {
      setSelectedUserIds(new Set(users.map((u) => u.userId)));
    }
  };

  const handleSend = async () => {
    if (!subject.trim()) {
      setError('Subject is required');
      return;
    }
    if (!body.trim()) {
      setError('Message is required');
      return;
    }
    if (selectedUserIds.size === 0) {
      setError('Select at least one recipient');
      return;
    }

    try {
      setSending(true);
      setError(null);

      await apiClient.composeNotification({
        entityType,
        entityId,
        entityName,
        subject: subject.trim(),
        body: body.trim(),
        recipientUserIds: Array.from(selectedUserIds),
      });

      setSuccess(true);
      setTimeout(() => {
        onClose();
        onSuccess?.();
      }, 1500);
    } catch (err) {
      console.error('Failed to send notification:', err);
      setError('Failed to send notification');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center z-[70] pointer-events-none">
      <div className="pointer-events-auto bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            Send Message: {entityName}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            disabled={sending}
          >
            <X size={24} />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4">
          {success && (
            <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-900 rounded-lg">
              <p className="text-sm text-green-800 dark:text-green-300">
                Message sent successfully!
              </p>
            </div>
          )}

          {error && (
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-900 rounded-lg flex items-center gap-2">
              <AlertCircle size={16} className="text-red-600 dark:text-red-400" />
              <p className="text-sm text-red-800 dark:text-red-300">{error}</p>
            </div>
          )}

          {/* Subject */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Subject
            </label>
            <input
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="e.g., Urgent update needed"
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={sending}
            />
          </div>

          {/* Body */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Message
            </label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Write your message here..."
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={sending}
            />
          </div>

          {/* Recipients */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Recipients ({selectedUserIds.size} selected)
              </label>
              {users.length > 0 && !opportunityContacts && (
                <button
                  onClick={handleSelectAll}
                  className="text-xs text-blue-600 dark:text-blue-400 hover:underline"
                  disabled={sending || loading}
                >
                  {selectedUserIds.size === users.length ? 'Deselect All' : 'Select All'}
                </button>
              )}
            </div>

            {loading && (
              <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                Loading team members...
              </div>
            )}

            {!loading && users.length === 0 && (
              <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                No team members found
              </div>
            )}

            {!loading && users.length > 0 && (
              <div className="space-y-4">
                {/* Opportunity Contacts Section */}
                {opportunityContacts && opportunityContacts.length > 0 && (
                  <div className="border border-green-200 dark:border-green-900/50 rounded-lg bg-green-50 dark:bg-green-900/20">
                    <div className="px-4 py-2 border-b border-green-200 dark:border-green-900/50">
                      <p className="text-sm font-medium text-green-900 dark:text-green-300">
                        🎯 Opportunity Contacts (pre-selected)
                      </p>
                    </div>
                    <div className="max-h-40 overflow-y-auto">
                      {users
                        .filter((u) =>
                          opportunityContacts.some(
                            (c) => c.email.toLowerCase() === u.userEmail.toLowerCase()
                          )
                        )
                        .map((user) => (
                          <label
                            key={user.id}
                            className="flex items-center px-4 py-3 hover:bg-green-100 dark:hover:bg-green-900/30 border-b border-green-100 dark:border-green-900/30 last:border-b-0 cursor-pointer"
                          >
                            <input
                              type="checkbox"
                              checked={selectedUserIds.has(user.userId)}
                              onChange={() => handleToggleUser(user.userId)}
                              disabled={sending}
                              className="w-4 h-4 text-green-600 rounded focus:ring-2 focus:ring-green-500"
                            />
                            <span className="ml-3 text-sm text-gray-900 dark:text-white">
                              {user.userEmail}
                            </span>
                            {user.role && (
                              <span className="ml-auto text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                                {user.role}
                              </span>
                            )}
                          </label>
                        ))}
                    </div>
                  </div>
                )}

                {/* Other Team Members Section */}
                {opportunityContacts && opportunityContacts.length > 0 ? (
                  <div className="border border-gray-300 dark:border-gray-600 rounded-lg">
                    <button
                      type="button"
                      onClick={() => setShowOtherTeamMembers(!showOtherTeamMembers)}
                      className="w-full px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 text-left flex items-center justify-between"
                    >
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        Other Team Members
                      </span>
                      <span className="text-gray-500 dark:text-gray-400">
                        {showOtherTeamMembers ? '▼' : '▶'}
                      </span>
                    </button>
                    {showOtherTeamMembers && (
                      <div className="max-h-40 overflow-y-auto border-t border-gray-300 dark:border-gray-600">
                        {users
                          .filter(
                            (u) =>
                              !opportunityContacts.some(
                                (c) => c.email.toLowerCase() === u.userEmail.toLowerCase()
                              )
                          )
                          .map((user) => (
                            <label
                              key={user.id}
                              className="flex items-center px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 border-b border-gray-200 dark:border-gray-700 last:border-b-0 cursor-pointer"
                            >
                              <input
                                type="checkbox"
                                checked={selectedUserIds.has(user.userId)}
                                onChange={() => handleToggleUser(user.userId)}
                                disabled={sending}
                                className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                              />
                              <span className="ml-3 text-sm text-gray-900 dark:text-white">
                                {user.userEmail}
                              </span>
                              {user.role && (
                                <span className="ml-auto text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                                  {user.role}
                                </span>
                              )}
                            </label>
                          ))}
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="border border-gray-300 dark:border-gray-600 rounded-lg max-h-48 overflow-y-auto">
                    {users.map((user) => (
                      <label
                        key={user.id}
                        className="flex items-center px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 border-b border-gray-200 dark:border-gray-700 last:border-b-0 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={selectedUserIds.has(user.userId)}
                          onChange={() => handleToggleUser(user.userId)}
                          disabled={sending}
                          className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                        />
                        <span className="ml-3 text-sm text-gray-900 dark:text-white">
                          {user.userEmail}
                        </span>
                        {user.role && (
                          <span className="ml-auto text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">
                            {user.role}
                          </span>
                        )}
                      </label>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 flex items-center justify-end gap-3 p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={sending}
          >
            Cancel
          </button>
          <button
            onClick={handleSend}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            disabled={sending || !subject.trim() || !body.trim() || selectedUserIds.size === 0}
          >
            <Send size={16} />
            {sending ? 'Sending...' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  );
};
