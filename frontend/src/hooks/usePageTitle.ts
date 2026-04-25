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
/**
 * @file Hook that synchronises the browser tab title with the current page name.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useEffect } from 'react';
import { useAuthStore } from '../store/authStore';

export function usePageTitle(pageTitle: string) {
  const currentTenant = useAuthStore((s) => s.currentTenant);

  useEffect(() => {
    const tenantName = currentTenant?.name;
    document.title = tenantName ? `${pageTitle} | ${tenantName}` : pageTitle;
  }, [pageTitle, currentTenant]);
}
