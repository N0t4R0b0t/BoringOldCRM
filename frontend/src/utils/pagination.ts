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
 * Pagination and data table utilities
 */

import type { PaginationParams, PagedResponse } from '../types/pagination';

export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * Normalizes API response data into a consistent PagedResponse format
 * @param data - Either an array or a paged response object
 * @returns Normalized PagedResponse
 */
export const normalizePaged = <T>(data: any): PagedResponse<T> => {
  if (Array.isArray(data)) {
    return {
      content: data,
      totalElements: data.length,
      totalPages: 1,
      currentPage: 1,
      pageSize: data.length,
      hasNext: false,
      hasPrev: false,
    };
  }
  return data;
};

// The following exports are intentionally exported for use in other modules.
// eslint-disable-next-line
export const createPaginationParams = (
  page: number = 1,
  size: number = DEFAULT_PAGE_SIZE,
  sortBy?: string,
  sortOrder?: 'asc' | 'desc',
  search?: string,
  filters?: Record<string, any>
): PaginationParams => {
  return {
    page: Math.max(1, page),
    size: Math.max(1, size),
    ...(sortBy && { sortBy }),
    ...(sortOrder && { sortOrder }),
    ...(search && { search }),
    ...(filters && Object.keys(filters).length > 0 && { filters }),
  };
};

// eslint-disable-next-line
export const getPageRange = (
  currentPage: number,
  totalPages: number,
  maxVisible: number = 5
): number[] => {
  const pages: number[] = [];
  let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages, start + maxVisible - 1);

  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
};

// eslint-disable-next-line
export const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

// eslint-disable-next-line
export const buildQueryString = (params: PaginationParams): string => {
  const queryParams = new URLSearchParams();
  queryParams.set('page', (params.page - 1).toString()); // Backend might use 0-based indexing
  queryParams.set('size', params.size.toString());

  if (params.sortBy) {
    queryParams.set('sortBy', params.sortBy);
  }
  if (params.sortOrder) {
    queryParams.set('sortOrder', params.sortOrder);
  }
  if (params.search) {
    queryParams.set('search', params.search);
  }
  if (params.filters) {
    Object.entries(params.filters).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        queryParams.set(`filter_${key}`, JSON.stringify(value));
      }
    });
  }

  return queryParams.toString();
};
