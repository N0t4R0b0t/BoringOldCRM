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
import { X, CheckCheck, Clock } from 'lucide-react';
import { apiClient } from '../api/apiClient';
import { useNotificationsStore } from '../store/notificationsStore';
import type { NotificationInboxItem } from '../types/notifications';

export const NotificationsDrawer = () => {
  const { isOpen, closeNotifications } = useNotificationsStore();
  const [notifications, setNotifications] = useState<NotificationInboxItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (isOpen) {
      loadNotifications();
      loadUnreadCount();
    }
  }, [page, isOpen]);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const res = await apiClient.getNotificationInbox({ page, size: 20 });
      setNotifications(res.data?.content || []);
      setTotalPages(res.data?.totalPages || 0);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const res = await apiClient.getUnreadNotificationCount();
      setUnreadCount(res.data?.unreadCount || 0);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

  const handleMarkRead = async (id: number) => {
    try {
      await apiClient.markNotificationRead(id);
      setNotifications(notifications.map(n => n.id === id ? { ...n, read: true } : n));
      loadUnreadCount();
    } catch (error) {
      console.error('Failed to mark notification read:', error);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await apiClient.markAllNotificationsRead();
      setNotifications(notifications.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
  };

  const getTypeColor = (type: string) => {
    return {
      'RECORD_MODIFIED': 'bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300',
      'OWNERSHIP_ASSIGNED': 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300',
      'ACCESS_GRANTED': 'bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300',
      'ACTIVITY_DUE_SOON': 'bg-yellow-50 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-300',
      'DAILY_INSIGHT': 'bg-indigo-50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-300',
    }[type] || 'bg-gray-50 dark:bg-gray-900/20 text-gray-700 dark:text-gray-300';
  };

  return (
    <>
      {/* Overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 transition-opacity"
          style={{ backgroundColor: 'rgba(0, 0, 0, 0.2)' }}
          onClick={closeNotifications}
        />
      )}

      {/* Drawer */}
      <div
        className={`fixed right-0 top-0 h-full w-full max-w-md bg-white dark:bg-gray-900 shadow-xl z-50 transform transition-transform duration-300 ease-in-out flex flex-col ${
          isOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Notifications</h2>
            {unreadCount > 0 && (
              <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">{unreadCount} unread</p>
            )}
          </div>
          <button
            onClick={closeNotifications}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 p-1"
          >
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {loading && (
            <div className="text-center py-8 text-gray-600 dark:text-gray-400">
              Loading notifications...
            </div>
          )}

          {!loading && notifications.length === 0 && (
            <div className="text-center py-8">
              <Clock size={32} className="mx-auto text-gray-400 mb-2" />
              <p className="text-gray-600 dark:text-gray-400">No notifications yet</p>
            </div>
          )}

          {!loading && notifications.length > 0 && (
            <>
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllRead}
                  className="w-full px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded-lg flex items-center justify-center gap-2 transition-colors mb-2"
                >
                  <CheckCheck size={16} />
                  Mark All as Read
                </button>
              )}

              {notifications.map(notif => (
                <div
                  key={notif.id}
                  className={`p-3 rounded-lg border transition-colors cursor-pointer ${
                    notif.read
                      ? 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700'
                      : 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-900'
                  } hover:shadow-md`}
                  onClick={() => !notif.read && handleMarkRead(notif.id)}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`px-2 py-0.5 text-xs font-semibold rounded ${getTypeColor(notif.notificationType)}`}>
                          {notif.notificationType.replace(/_/g, ' ')}
                        </span>
                        {!notif.read && <span className="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0"></span>}
                      </div>
                      <p className="text-sm text-gray-900 dark:text-white font-medium line-clamp-2">{notif.message}</p>
                      {notif.actorDisplayName && (
                        <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">From: {notif.actorDisplayName}</p>
                      )}
                      <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">{formatDate(notif.createdAt)}</p>
                    </div>
                    {!notif.read && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkRead(notif.id);
                        }}
                        className="px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white text-xs rounded transition-colors flex-shrink-0"
                      >
                        Read
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </>
          )}
        </div>

        {/* Footer - Pagination */}
        {!loading && totalPages > 1 && (
          <div className="flex justify-center gap-2 p-4 border-t border-gray-200 dark:border-gray-700">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-3 py-1 text-sm bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed rounded transition-colors"
            >
              Prev
            </button>
            <span className="px-3 py-1 text-sm text-gray-700 dark:text-gray-300">
              {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 text-sm bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed rounded transition-colors"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </>
  );
};
