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
import { useSearchParams } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { NewEntityButton } from '../components/NewEntityButton';
import { DataTable } from '../components/DataTable';
import { AdvancedSearch } from '../components/AdvancedSearch';
import { Drawer } from '../components/Drawer';
import { CustomerForm } from '../components/forms/CustomerForm';
import { CustomerView } from '../components/views/CustomerView';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useCalculatedFields } from '../hooks/useCalculatedFields';
import { useDrawerState } from '../hooks/useDrawerState';
import { useListPageState } from '../hooks/useListPageState';
import { useUiStore } from '../store/uiStore';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { useEntityFieldOptions, getOptionBadgeColor, getOptionLabel } from '../hooks/useEntityFieldOptions';
import { normalizePaged } from '../utils/pagination';
import type { ColumnDefinition } from '../types/pagination';
import '../styles/List.css';

interface Customer {
  id: number;
  name: string;
  status: string;
  createdAt: string;
  customFields?: Record<string, any>;
  [key: string]: any;
}


export const CustomersPage = () => {
  usePageTitle('Customers');

  const [searchParams] = useSearchParams();
  const drawer = useDrawerState();
  const listState = useListPageState();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [localError, setLocalError] = useState('');
  const { fields: customFields = [] } = useCustomFields('Customer');
  const { fields: calculatedFields = [] } = useCalculatedFields('Customer');
  const { setHeaderActions, dataRefreshToken } = useUiStore();
  const label = useEntityLabel('Customer');
  const statusOptions = useEntityFieldOptions('Customer', 'status');

  const fetchCustomers = async () => {
    listState.setIsLoading(true);
    try {
      const response = await apiClient.getCustomers({
        page: listState.page - 1,
        size: listState.pageSize,
        sortBy: listState.sortBy,
        sortOrder: listState.sortOrder,
        search: listState.search,
        ...listState.filters,
      });

      const data = normalizePaged<Customer>(response.data);
      setCustomers(data.content);
      listState.setTotalPages(data.totalPages || 1);
      listState.setTotalElements(data.totalElements || data.content.length);
      listState.setError('');
    } catch (err: any) {
      listState.setError(err.response?.data?.message || 'Failed to fetch customers');
      setCustomers([]);
    } finally {
      listState.setIsLoading(false);
    }
  };

  const handleFormSuccess = () => {
    drawer.setEditDrawerOpen(false);
    drawer.setViewDrawerOpen(false);
    drawer.setSelectedId(undefined);
    fetchCustomers();
  };

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this customer?')) {
      try {
        await apiClient.deleteCustomer(id);
        setCustomers(customers.filter((c) => c.id !== id));
        listState.setTotalElements(Math.max(0, listState.totalElements - 1));
        drawer.handleCloseViewDrawer();
      } catch (err: any) {
        setLocalError(err.response?.data?.message || 'Failed to delete');
      }
    }
  };

  useEffect(() => {
    setHeaderActions(
      <NewEntityButton label={label} onClick={drawer.handleNew} />
    );
    return () => setHeaderActions(null);
  }, []);

  useEffect(() => {
    const viewId = searchParams.get('view');
    if (viewId) drawer.handleView(parseInt(viewId, 10));
  }, [searchParams]);

  useEffect(() => {
    fetchCustomers();
  }, [listState.page, listState.pageSize, listState.sortBy, listState.sortOrder, listState.search, listState.filters, dataRefreshToken]);

  const { pendingFilters, setPendingFilters } = useUiStore();
  useEffect(() => {
    if (pendingFilters?.entityType === 'Customer') {
      listState.setFilters(pendingFilters.filters);
      listState.resetPagination();
      setPendingFilters(null);
    }
  }, [pendingFilters]);

  const badgeColorMap: Record<string, string> = {
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-green-100 text-green-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    red: 'bg-red-100 text-red-800',
    purple: 'bg-purple-100 text-purple-800',
    orange: 'bg-orange-100 text-orange-800',
    teal: 'bg-teal-100 text-teal-800',
    gray: 'bg-gray-100 text-gray-800',
  };

  const baseColumns: ColumnDefinition[] = [
    {
      key: 'name',
      label: 'Name',
      sortable: true,
      filterType: 'text',
      avatar: true,
      render: (value: any) => (
        <span className="text-blue-600 font-medium">{value}</span>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      sortable: true,
      filterType: 'select',
      filterOptions: statusOptions.map(o => ({ value: o.value, label: o.label })),
      render: (value: any) => {
        const color = getOptionBadgeColor(value, statusOptions);
        const colorClass = badgeColorMap[color] || badgeColorMap.gray;
        return (
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${colorClass}`}>
            {getOptionLabel(value, statusOptions)}
          </span>
        );
      },
    },
    {
      key: 'createdAt',
      label: 'Created',
      sortable: true,
      filterType: 'date',
      render: (value: any) => new Date(value).toLocaleDateString(),
    },
  ];

  const renderCustomFieldValue = (value: any, field?: any): string => {
    if (value === undefined || value === null || value === '') return '—';

    // Handle workflow fields
    if (field?.type === 'workflow' && typeof value === 'object' && 'currentIndex' in value) {
      const currentIndex = value.currentIndex;
      if (currentIndex === null || currentIndex === undefined) return '—';
      const config = typeof field.config === 'string' ? JSON.parse(field.config) : field.config;
      const milestones = config?.milestones || [];
      return milestones[currentIndex] || '—';
    }

    if (Array.isArray(value)) {
      // For arrays of objects (contact/customRecord fields), extract names
      return value
        .map((item: any) => (typeof item === 'object' && item?.name ? item.name : String(item)))
        .join(', ')
        .substring(0, 50);
    }
    if (typeof value === 'object') {
      // For single objects (contact/customRecord fields), extract name
      return (value?.name || String(value)).substring(0, 50);
    }
    return String(value).substring(0, 50);
  };

  const customFieldColumns: ColumnDefinition[] = customFields
    .filter((field) => field.displayInTable)
    .map((field) => ({
      key: `customFields.${field.key}`,
      label: field.label,
      sortable: false,
      filterType: 'text',
      render: (_value: any, row: Customer) => {
        const customFieldValue = row.customFields?.[field.key];
        return renderCustomFieldValue(customFieldValue, field);
      },
    }));

  const calculatedFieldColumns: ColumnDefinition[] = calculatedFields
    .filter((field) => field.displayInTable)
    .map((field) => ({
      key: `calculatedFields.${field.key}`,
      label: field.label,
      sortable: false,
      filterType: 'text',
      render: (_value: any, row: Customer) => {
        const calcFieldValue = row.customFields?.[field.key];
        return renderCustomFieldValue(calcFieldValue, field);
      },
    }));

  const columns = [...baseColumns, ...customFieldColumns, ...calculatedFieldColumns];

  return (
    <Layout>
      <div className="space-y-6">
        {(listState.error || localError) && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
            {listState.error || localError}
          </div>
        )}

        <AdvancedSearch
          onSearch={(query) => {
            listState.updateSearch(query);
            listState.setPage(1);
          }}
          placeholder="Search by name, status, or any field..."
        />

        <DataTable
          tableId="customers"
          data={customers}
          columns={columns}
          isLoading={listState.isLoading}
          currentPage={listState.page}
          totalPages={listState.totalPages}
          totalElements={listState.totalElements}
          pageSize={listState.pageSize}
          onSort={(key, order) => {
            listState.updateSort(key, order);
            listState.setPage(1);
          }}
          onPageChange={listState.setPage}
          onPageSizeChange={listState.setPageSize}
          onRowClick={(row) => drawer.handleView(row.id)}
          emptyMessage="No customers found. Create your first customer to get started."
          onBulkDelete={async (ids) => { await apiClient.bulkDeleteCustomers(ids); fetchCustomers(); }}
        />

        {/* View Drawer (Read-only) */}
        <Drawer
          isOpen={drawer.viewDrawerOpen}
          onClose={drawer.handleCloseViewDrawer}
          onEdit={drawer.handleEditFromView}
          onDelete={() => {
            if (drawer.selectedId) {
              handleDelete(drawer.selectedId);
            }
          }}
          title={`${label} Details`}
          mode="view"
          width="1020px"
        >
          {drawer.selectedId && (
            <CustomerView
              id={drawer.selectedId}
            />
          )}
        </Drawer>

        {/* Edit Drawer (Editable) */}
        <Drawer
          isOpen={drawer.editDrawerOpen}
          onClose={drawer.handleCloseEditDrawer}
          title={drawer.selectedId ? `Edit ${label}` : `New ${label}`}
          mode="edit"
          width="1230px"
          entityType="Customer"
        >
          <CustomerForm
            id={drawer.selectedId}
            onSuccess={handleFormSuccess}
            onCancel={drawer.handleCloseEditDrawer}
          />
        </Drawer>
      </div>
    </Layout>
  );
};
