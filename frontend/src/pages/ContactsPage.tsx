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
import { usePageTitle } from '../hooks/usePageTitle';
import { useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { NewEntityButton } from '../components/NewEntityButton';
import { DataTable } from '../components/DataTable';
import { AdvancedSearch } from '../components/AdvancedSearch';
import { Drawer } from '../components/Drawer';
import { ContactForm } from '../components/forms/ContactForm';
import { ContactView } from '../components/views/ContactView';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useCalculatedFields } from '../hooks/useCalculatedFields';
import { useDrawerState } from '../hooks/useDrawerState';
import { useListPageState } from '../hooks/useListPageState';
import { useUiStore } from '../store/uiStore';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { normalizePaged } from '../utils/pagination';
import type { ColumnDefinition } from '../types/pagination';
import '../styles/List.css';

interface Contact {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  customerId?: number;
  createdAt: string;
  customFields?: Record<string, any>;
  [key: string]: any;
}

export const ContactsPage = () => {
  usePageTitle('Contacts');
  const [searchParams] = useSearchParams();
  const drawer = useDrawerState();
  const listState = useListPageState();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [customerMap, setCustomerMap] = useState<Record<number, string>>({});
  const [localError, setLocalError] = useState('');
  const { fields: customFields = [] } = useCustomFields('Contact');
  const { fields: calculatedFields = [] } = useCalculatedFields('Contact');
  const { setHeaderActions, dataRefreshToken } = useUiStore();
  const label = useEntityLabel('Contact');

  const fetchContacts = async () => {
    listState.setIsLoading(true);
    try {
      const response = await apiClient.getContacts({
        page: listState.page - 1,
        size: listState.pageSize,
        sortBy: listState.sortBy,
        sortOrder: listState.sortOrder,
        search: listState.search,
        ...listState.filters,
      });

      const data = normalizePaged<Contact>(response.data);
      setContacts(data.content);
      listState.setTotalPages(data.totalPages || 1);
      listState.setTotalElements(data.totalElements || data.content.length);
      listState.setError('');
    } catch (err: any) {
      listState.setError(err.response?.data?.message || 'Failed to fetch contacts');
      setContacts([]);
    } finally {
      listState.setIsLoading(false);
    }
  };

  const fetchCustomers = async () => {
    try {
      const response = await apiClient.getCustomers({ page: 0, size: 500 });
      const data = normalizePaged(response.data);
      const map: Record<number, string> = {};
      data.content?.forEach((c: any) => {
        map[c.id] = c.name;
      });
      setCustomerMap(map);
    } catch {
      setCustomerMap({});
    }
  };

  const handleFormSuccess = () => {
    drawer.setEditDrawerOpen(false);
    drawer.setViewDrawerOpen(false);
    drawer.setSelectedId(undefined);
    fetchContacts();
  };

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this contact?')) {
      try {
        await apiClient.deleteContact(id);
        setContacts(contacts.filter((c) => c.id !== id));
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
    fetchContacts();
  }, [listState.page, listState.pageSize, listState.sortBy, listState.sortOrder, listState.search, listState.filters, dataRefreshToken]);

  const { pendingFilters, setPendingFilters } = useUiStore();
  useEffect(() => {
    if (pendingFilters?.entityType === 'Contact') {
      listState.setFilters(pendingFilters.filters);
      listState.resetPagination();
      setPendingFilters(null);
    }
  }, [pendingFilters]);

  const baseColumns: ColumnDefinition[] = [
    {
      key: 'name',
      label: 'Name',
      sortable: true,
      filterType: 'text',
      render: (value: any, row: Contact) => {
        const name = row.firstName || row.lastName
          ? `${row.firstName || ''} ${row.lastName || ''}`.trim()
          : value || '—';
        return <span className="text-blue-600 font-medium">{name}</span>;
      },
    },
    {
      key: 'email',
      label: 'Email',
      sortable: true,
      filterType: 'text',
      render: (value: any) => (
        <a href={`mailto:${value}`} className="text-blue-600 hover:underline">
          {value || '—'}
        </a>
      ),
    },
    {
      key: 'phone',
      label: 'Phone',
      sortable: false,
      filterType: 'text',
      render: (value: any) => value || '—',
    },
    {
      key: 'customerId',
      label: 'Customer',
      sortable: false,
      filterType: 'text',
      render: (value: any) => customerMap[value] || '—',
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
      render: (_value: any, row: Contact) => {
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
      render: (_value: any, row: Contact) => {
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

        <div className="space-y-4">
          <div className="flex-1">
            <AdvancedSearch
              onSearch={(query) => {
                listState.updateSearch(query);
                listState.setPage(1);
              }}
              placeholder="Search by name, email, or phone..."
            />
          </div>
        </div>

        <DataTable
          tableId="contacts"
          data={contacts}
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
          emptyMessage="No contacts found. Create your first contact to get started."
          onBulkDelete={async (ids) => { await apiClient.bulkDeleteContacts(ids); fetchContacts(); }}
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
            <ContactView
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
          entityType="Contact"
        >
          <ContactForm
            id={drawer.selectedId}
            onSuccess={handleFormSuccess}
            onCancel={drawer.handleCloseEditDrawer}
          />
        </Drawer>
      </div>
    </Layout>
  );
};

