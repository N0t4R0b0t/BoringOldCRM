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
 * @file Hook managing the open/close state and selected record ID for detail-view drawer panels.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useState } from 'react';

/**
 * Custom hook for managing dual drawer state (view and edit)
 * Handles opening/closing logic and selected item ID management
 */
export const useDrawerState = () => {
  const [viewDrawerOpen, setViewDrawerOpen] = useState(false);
  const [editDrawerOpen, setEditDrawerOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<number | undefined>(undefined);

  const handleView = (id: number) => {
    setSelectedId(id);
    setViewDrawerOpen(true);
  };

  const handleEditFromView = () => {
    setEditDrawerOpen(true);
  };

  const handleNew = () => {
    setSelectedId(undefined);
    setEditDrawerOpen(true);
  };

  const handleCloseViewDrawer = () => {
    setViewDrawerOpen(false);
    setSelectedId(undefined);
  };

  const handleCloseEditDrawer = () => {
    setEditDrawerOpen(false);
    if (selectedId) {
      setViewDrawerOpen(true);
    }
  };

  return {
    viewDrawerOpen,
    setViewDrawerOpen,
    editDrawerOpen,
    setEditDrawerOpen,
    selectedId,
    setSelectedId,
    handleView,
    handleEditFromView,
    handleNew,
    handleCloseViewDrawer,
    handleCloseEditDrawer,
  };
};

