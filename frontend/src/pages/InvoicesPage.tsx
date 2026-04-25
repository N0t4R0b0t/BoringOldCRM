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
import { InvoiceForm } from '../components/forms/InvoiceForm';
import { InvoiceView } from '../components/views/InvoiceView';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useCalculatedFields } from '../hooks/useCalculatedFields';
import { useDrawerState } from '../hooks/useDrawerState';
import { useListPageState } from '../hooks/useListPageState';
import { useUiStore } from '../store/uiStore';
import { normalizePaged } from '../utils/pagination';
import type { ColumnDefinition } from '../types/pagination';
import '../styles/List.css';

interface Invoice {
  id: number;
  customerId: number;
  customerName?: string;
  status: string;
  currency: string;
  totalAmount: number;
  dueDate?: string;
  paymentTerms?: string;
  createdAt: string;
  customFields?: Record<string, any>;
  [key: string]: any;
}

export const InvoicesPage = () => {
  usePageTitle('Invoices');

  const [searchParams] = useSearchParams();
  const drawer = useDrawerState();
  const listState = useListPageState();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [localError, setLocalError] = useState('');
  const { fields: customFields = [] } = useCustomFields('Invoice');
  const { fields: calculatedFields = [] } = useCalculatedFields('Invoice');
  const { setHeaderActions, dataRefreshToken } = useUiStore();

  const fetchInvoices = async () => {
    listState.setIsLoading(true);
    try {
      const response = await apiClient.getInvoices({
        page: listState.page - 1,
        size: listState.pageSize,
        sortBy: listState.sortBy,
        sortOrder: listState.sortOrder,
        search: listState.search,
        ...listState.filters,
      });
      const data = normalizePaged<Invoice>(response.data);
      setInvoices(data.content);
      listState.setTotalPages(data.totalPages || 1);
      listState.setTotalElements(data.totalElements || data.content.length);
      listState.setError('');
    } catch (err: any) {
      listState.setError(err.response?.data?.message || 'Failed to fetch invoices');
      setInvoices([]);
    } finally {
      listState.setIsLoading(false);
    }
  };

  const handleFormSuccess = () => {
    drawer.setEditDrawerOpen(false);
    drawer.setViewDrawerOpen(false);
    drawer.setSelectedId(undefined);
    fetchInvoices();
  };

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this invoice?')) {
      try {
        await apiClient.deleteInvoice(id);
        setInvoices((prev) => prev.filter((i) => i.id !== id));
        listState.setTotalElements(Math.max(0, listState.totalElements - 1));
        drawer.handleCloseViewDrawer();
      } catch (err: any) {
        setLocalError(err.response?.data?.message || 'Failed to delete');
      }
    }
  };

  useEffect(() => {
    setHeaderActions(
      <NewEntityButton label="Invoice" onClick={drawer.handleNew} />
    );
    return () => setHeaderActions(null);
  }, []);

  useEffect(() => {
    const viewId = searchParams.get('view');
    if (viewId) drawer.handleView(parseInt(viewId, 10));
  }, [searchParams]);

  useEffect(() => {
    fetchInvoices();
  }, [listState.page, listState.pageSize, listState.sortBy, listState.sortOrder, listState.search, listState.filters, dataRefreshToken]);

  const { pendingFilters, setPendingFilters } = useUiStore();
  useEffect(() => {
    if (pendingFilters?.entityType === 'Invoice') {
      listState.setFilters(pendingFilters.filters);
      listState.resetPagination();
      setPendingFilters(null);
    }
  }, [pendingFilters]);

  const renderCustomFieldValue = (value: any, field?: any): string => {
    if (value === undefined || value === null || value === '') return '—';
    if (field?.type === 'workflow' && typeof value === 'object' && 'currentIndex' in value) {
      const currentIndex = value.currentIndex;
      if (currentIndex === null || currentIndex === undefined) return '—';
      const config = typeof field.config === 'string' ? JSON.parse(field.config) : field.config;
      const milestones = config?.milestones || [];
      return milestones[currentIndex] || '—';
    }
    if (Array.isArray(value)) {
      return value.map((item: any) => (typeof item === 'object' && item?.name ? item.name : String(item))).join(', ').substring(0, 50);
    }
    if (typeof value === 'object') {
      return (value?.name || String(value)).substring(0, 50);
    }
    return String(value).substring(0, 50);
  };

  const baseColumns: ColumnDefinition[] = [
    {
      key: 'id',
      label: 'ID',
      sortable: true,
      filterType: 'text',
      render: (value: any) => <span className="text-blue-600 font-medium">#{value}</span>,
    },
    {
      key: 'customerName',
      label: 'Customer',
      sortable: true,
      filterType: 'text',
    },
    {
      key: 'status',
      label: 'Status',
      sortable: true,
      filterType: 'select',
      filterOptions: [
        { value: 'draft', label: 'Draft' },
        { value: 'sent', label: 'Sent' },
        { value: 'paid', label: 'Paid' },
        { value: 'overdue', label: 'Overdue' },
        { value: 'cancelled', label: 'Cancelled' },
      ],
      render: (value: any) => (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
          value?.toLowerCase() === 'paid'      ? 'bg-green-100 text-green-800' :
          value?.toLowerCase() === 'sent'      ? 'bg-blue-100 text-blue-800' :
          value?.toLowerCase() === 'overdue'   ? 'bg-red-100 text-red-800' :
          value?.toLowerCase() === 'draft'     ? 'bg-gray-100 text-gray-800' :
                                                 'bg-yellow-100 text-yellow-800'
        }`}>
          {value}
        </span>
      ),
    },
    {
      key: 'currency',
      label: 'Currency',
      sortable: true,
      filterType: 'text',
    },
    {
      key: 'totalAmount',
      label: 'Total',
      sortable: true,
      filterType: 'text',
      render: (value: any) => `$${(value || 0).toFixed(2)}`,
    },
    {
      key: 'dueDate',
      label: 'Due Date',
      sortable: true,
      filterType: 'date',
      render: (value: any) => value ? new Date(value).toLocaleDateString() : '—',
    },
    {
      key: 'paymentTerms',
      label: 'Payment Terms',
      sortable: false,
      filterType: 'text',
    },
    {
      key: 'createdAt',
      label: 'Created',
      sortable: true,
      filterType: 'date',
      render: (value: any) => new Date(value).toLocaleDateString(),
    },
  ];

  const customFieldColumns: ColumnDefinition[] = customFields
    .filter((field) => field.displayInTable)
    .map((field) => ({
      key: `customFields.${field.key}`,
      label: field.label,
      sortable: false,
      filterType: 'text' as const,
      render: (_value: any, row: Invoice) => renderCustomFieldValue(row.customFields?.[field.key], field),
    }));

  const calculatedFieldColumns: ColumnDefinition[] = calculatedFields
    .filter((field) => field.displayInTable)
    .map((field) => ({
      key: `calculatedFields.${field.key}`,
      label: field.label,
      sortable: false,
      filterType: 'text' as const,
      render: (_value: any, row: Invoice) => renderCustomFieldValue(row.customFields?.[field.key], field),
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
          onSearch={(query) => { listState.updateSearch(query); listState.setPage(1); }}
          placeholder="Search by customer, status, or any field..."
        />

        <DataTable
          tableId="invoices"
          data={invoices}
          columns={columns}
          isLoading={listState.isLoading}
          currentPage={listState.page}
          totalPages={listState.totalPages}
          totalElements={listState.totalElements}
          pageSize={listState.pageSize}
          onSort={(key, order) => { listState.updateSort(key, order); listState.setPage(1); }}
          onPageChange={listState.setPage}
          onPageSizeChange={listState.setPageSize}
          onRowClick={(row) => drawer.handleView(row.id)}
          emptyMessage="No invoices found. Create your first invoice to get started."
          onBulkDelete={async (ids) => { await apiClient.bulkDeleteInvoices(ids); fetchInvoices(); }}
        />

        <Drawer
          isOpen={drawer.viewDrawerOpen}
          onClose={drawer.handleCloseViewDrawer}
          onEdit={drawer.handleEditFromView}
          onDelete={() => drawer.selectedId && handleDelete(drawer.selectedId)}
          title="Invoice Details"
          mode="view"
          width="1020px"
        >
          {drawer.selectedId && <InvoiceView id={drawer.selectedId} />}
        </Drawer>

        <Drawer
          isOpen={drawer.editDrawerOpen}
          onClose={drawer.handleCloseEditDrawer}
          title={drawer.selectedId ? 'Edit Invoice' : 'New Invoice'}
          mode="edit"
          width="900px"
          entityType="Invoice"
        >
          <InvoiceForm
            id={drawer.selectedId}
            onSuccess={handleFormSuccess}
            onCancel={drawer.handleCloseEditDrawer}
          />
        </Drawer>
      </div>
    </Layout>
  );
};
