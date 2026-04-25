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
import { useSearchParams, useParams } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { NewEntityButton } from '../components/NewEntityButton';
import { DataTable } from '../components/DataTable';
import { KanbanBoard, type KanbanColumn } from '../components/KanbanBoard';
import { AdvancedSearch } from '../components/AdvancedSearch';
import { Drawer } from '../components/Drawer';
import { OpportunityForm } from '../components/forms/OpportunityForm';
import { OpportunityView } from '../components/views/OpportunityView';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useCalculatedFields } from '../hooks/useCalculatedFields';
import { useDrawerState } from '../hooks/useDrawerState';
import { useListPageState } from '../hooks/useListPageState';
import { useUiStore } from '../store/uiStore';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { useEntityFieldOptions, getOptionBadgeColor, getOptionLabel } from '../hooks/useEntityFieldOptions';
import { normalizePaged } from '../utils/pagination';
import type { ColumnDefinition } from '../types/pagination';
import '../styles/List.css';

interface Opportunity {
  id: number;
  name?: string;
  accountName?: string;
  stage?: string;
  status?: string;
  value?: number;
  createdAt: string;
  calculatedFields?: any[];
  customFields?: Record<string, any>;
  [key: string]: any;
}

export const OpportunitiesPage = () => {
  usePageTitle('Opportunities');
  const [searchParams] = useSearchParams();
  const { typeSlug } = useParams<{ typeSlug?: string }>();
  const effectiveEntityType = typeSlug ? `Opportunity:${typeSlug}` : 'Opportunity';
  const drawer = useDrawerState();
  const listState = useListPageState();
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [localError, setLocalError] = useState('');
  const [viewMode, setViewMode] = useState<'list' | 'board'>(() => {
    const saved = localStorage.getItem('kanban:view:opportunities');
    return (saved as 'list' | 'board') || 'list';
  });
  const { fields: customFields = [] } = useCustomFields(effectiveEntityType);
  const { fields: calculatedFields = [] } = useCalculatedFields(effectiveEntityType);
  const stageOptions = useEntityFieldOptions('Opportunity', 'stage');
  const { setHeaderActions, dataRefreshToken } = useUiStore();
  const label = useEntityLabel('Opportunity');
  useOpportunityTypesStore();

  const fetchOpportunities = async () => {
    listState.setIsLoading(true);

    try {
      const response = await apiClient.getOpportunities({
        page: listState.page - 1,
        size: listState.pageSize,
        sortBy: listState.sortBy,
        sortOrder: listState.sortOrder,
        search: listState.search,
        ...(typeSlug ? { typeSlug } : {}),
        ...listState.filters,
      });

      const data = normalizePaged<Opportunity>(response.data);
      setOpportunities(data.content);
      listState.setTotalPages(data.totalPages || 1);
      listState.setTotalElements(data.totalElements || data.content.length);
      listState.setError('');
    } catch (err: any) {
      listState.setError(err.response?.data?.message || 'Failed to fetch opportunities');
      setOpportunities([]);
    } finally {
      listState.setIsLoading(false);
    }
  };


  const handleFormSuccess = () => {
    drawer.setEditDrawerOpen(false);
    drawer.setViewDrawerOpen(false);
    drawer.setSelectedId(undefined);
    fetchOpportunities();
  };

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this opportunity?')) {
      try {
        await apiClient.deleteOpportunity(id);
        setOpportunities(opportunities.filter((o) => o.id !== id));
        listState.setTotalElements(Math.max(0, listState.totalElements - 1));
        drawer.handleCloseViewDrawer();
      } catch (err: any) {
        setLocalError(err.response?.data?.message || 'Failed to delete');
      }
    }
  };

  useEffect(() => {
    setHeaderActions(
      <div className="flex items-center gap-2">
        <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
          <button
            onClick={() => {
              setViewMode('list');
              localStorage.setItem('kanban:view:opportunities', 'list');
            }}
            title="List view"
            className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
              viewMode === 'list'
                ? 'bg-white dark:bg-gray-800 text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            }`}
          >
            📋<span className="hidden sm:inline"> List</span>
          </button>
          <button
            onClick={() => {
              setViewMode('board');
              localStorage.setItem('kanban:view:opportunities', 'board');
            }}
            title="Board view"
            className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
              viewMode === 'board'
                ? 'bg-white dark:bg-gray-800 text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
            }`}
          >
            📊<span className="hidden sm:inline"> Board</span>
          </button>
        </div>
        <NewEntityButton label={label} onClick={drawer.handleNew} />
      </div>
    );
    return () => setHeaderActions(null);
  }, [viewMode]);

  useEffect(() => {
    const viewId = searchParams.get('view');
    if (viewId) drawer.handleView(parseInt(viewId, 10));
  }, [searchParams]);

  useEffect(() => {
    fetchOpportunities();
  }, [listState.page, listState.pageSize, listState.sortBy, listState.sortOrder, listState.search, listState.filters, dataRefreshToken, typeSlug, viewMode]);

  const { pendingFilters, setPendingFilters } = useUiStore();
  useEffect(() => {
    if (pendingFilters?.entityType === 'Opportunity') {
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

  // Build kanban columns: if stageOptions are loaded, use them; otherwise extract unique stages from opportunities
  const kanbanColumns: KanbanColumn[] = stageOptions.length > 0
    ? stageOptions.map((opt, idx) => ({
        key: opt.value,
        label: opt.label,
        color: (['blue', 'green', 'yellow', 'red', 'purple', 'orange', 'teal', 'gray'] as const)[idx % 8],
      }))
    : (() => {
        const uniqueStages = Array.from(new Set(
          opportunities.map((o) => o.stage).filter((s): s is string => Boolean(s))
        ));
        return uniqueStages.map((stage, idx) => ({
          key: stage,
          label: stage,
          color: (['blue', 'green', 'yellow', 'red', 'purple', 'orange', 'teal', 'gray'] as const)[idx % 8],
        }));
      })();

  const handleKanbanCardMove = async (itemId: number, newStage: string) => {
    try {
      await apiClient.updateOpportunity(itemId, { stage: newStage });
      setOpportunities((prev) =>
        prev.map((opp) => (opp.id === itemId ? { ...opp, stage: newStage } : opp))
      );
    } catch (err: any) {
      setLocalError(err.response?.data?.message || 'Failed to update stage');
    }
  };

  const baseColumns: ColumnDefinition[] = [
    {
      key: 'name',
      label: 'Opportunity',
      sortable: true,
      filterType: 'text',
      render: (value: any, row: Opportunity) => {
        const name = value || row.accountName || '—';
        return <span className="text-blue-600 font-medium">{name}</span>;
      },
    },
    {
      key: 'stage',
      label: 'Stage',
      sortable: true,
      filterType: 'select',
      filterOptions: stageOptions.map(o => ({ value: o.value, label: o.label })),
      render: (_value: any, row: Opportunity) => {
        // Always use the direct stage property, not the column value which may be a custom field
        // This prevents workflow custom fields from overwriting the actual stage
        const stage = row.stage;

        // Ensure value is a string (not a workflow object or other complex type)
        if (typeof stage !== 'string' && typeof stage !== 'number') {
          return '—';
        }

        const stageStr = String(stage || row.status || '—');
        const color = getOptionBadgeColor(stageStr, stageOptions);
        const colorClass = badgeColorMap[color] || badgeColorMap.gray;
        return (
          <span className={`px-2 py-1 rounded-full text-xs font-medium ${colorClass}`}>
            {getOptionLabel(stageStr, stageOptions)}
          </span>
        );
      },
    },
    {
      key: 'value',
      label: 'Value',
      sortable: true,
      filterType: 'number',
      render: (value: any) =>
        value ? `$${Number(value).toLocaleString()}` : '—',
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
      render: (_value: any, row: Opportunity) => {
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
      render: (_value: any, row: Opportunity) => {
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
              placeholder="Search by opportunity name or account..."
            />
          </div>
        </div>

        {viewMode === 'list' ? (
          <DataTable
            tableId="opportunities"
            data={opportunities}
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
            emptyMessage="No opportunities found. Create your first opportunity to get started."
            onBulkDelete={async (ids) => { await apiClient.bulkDeleteOpportunities(ids); fetchOpportunities(); }}
          />
        ) : (
          <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm p-4 overflow-hidden">
            <KanbanBoard
              items={opportunities}
              groupByKey="stage"
              columns={kanbanColumns}
              onCardClick={(item) => drawer.handleView(item.id)}
              onCardMove={handleKanbanCardMove}
              nameKey="name"
              valueKey="value"
            />
          </div>
        )}

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
            <OpportunityView
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
          entityType="Opportunity"
        >
          <OpportunityForm
            id={drawer.selectedId}
            typeSlug={typeSlug}
            onSuccess={handleFormSuccess}
            onCancel={drawer.handleCloseEditDrawer}
          />
        </Drawer>
      </div>
    </Layout>
  );
};
