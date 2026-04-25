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
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotificationsStore } from '../store/notificationsStore';
import { usePageTitle } from '../hooks/usePageTitle';

export const NotificationsPage = () => {
  usePageTitle('Notifications');
  const navigate = useNavigate();
  const { openNotifications } = useNotificationsStore();

  useEffect(() => {
    // Open the notifications drawer and redirect back to dashboard
    openNotifications();
    navigate('/', { replace: true });
  }, [openNotifications, navigate]);

  return null; // This page is just a redirect handler
};
