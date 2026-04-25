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
 * @file Hook encapsulating pagination, sorting, search, and filter state shared by all list pages.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useState } from 'react';

/**
 * Custom hook for managing list page state
 * Handles pagination, sorting, searching, and filtering
 */
export const useListPageState = () => {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [sortBy, setSortBy] = useState('name');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<Record<string, any>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const resetPagination = () => {
    setPage(1);
    setTotalPages(1);
    setTotalElements(0);
  };

  const resetFilters = () => {
    setFilters({});
    resetPagination();
  };

  const updateSort = (newSortBy: string, newSortOrder: 'asc' | 'desc') => {
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    resetPagination();
  };

  const updateSearch = (newSearch: string) => {
    setSearch(newSearch);
    resetPagination();
  };

  return {
    page,
    setPage,
    pageSize,
    setPageSize,
    totalPages,
    setTotalPages,
    totalElements,
    setTotalElements,
    sortBy,
    setSortBy,
    sortOrder,
    setSortOrder,
    search,
    setSearch,
    filters,
    setFilters,
    isLoading,
    setIsLoading,
    error,
    setError,
    resetPagination,
    resetFilters,
    updateSort,
    updateSearch,
  };
};

