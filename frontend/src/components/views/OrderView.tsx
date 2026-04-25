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
import React, { useEffect, useState } from 'react';
import { apiClient } from '../../api/apiClient';
import { Timeline } from '../Timeline';
import { useCustomFields } from '../../hooks/useCustomFields';
import { useCalculatedFields } from '../../hooks/useCalculatedFields';
import { useUiStore } from '../../store/uiStore';
import { DocumentFieldExplorer } from '../DocumentFieldExplorer';
import { CustomRecordFieldPicker } from '../CustomRecordFieldPicker';
import { ContactFieldPicker } from '../ContactFieldPicker';
import { WorkflowField } from '../WorkflowField';

interface OrderViewProps {
  id: number;
}

export const OrderView: React.FC<OrderViewProps> = ({ id }) => {
  const [order, setOrder] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [calcValues, setCalcValues] = useState<Record<string, any>>({});
  const [calcLoading, setCalcLoading] = useState(false);
  const { fields: fieldDefinitions } = useCustomFields('Order');
  const { fields: calcDefinitions } = useCalculatedFields('Order');
  const { dataRefreshToken } = useUiStore();

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await apiClient.getOrder(id);
        setOrder(res.data);
        setCalcLoading(true);
        apiClient.evaluateCalculatedFields('Order', id)
          .then((r) => setCalcValues(r.data || {}))
          .catch(() => {})
          .finally(() => setCalcLoading(false));
      } catch {
        setError('Failed to load order');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id, dataRefreshToken]);

  useEffect(() => {
    if (calcDefinitions.length > 0) {
      setCalcLoading(true);
      apiClient.evaluateCalculatedFields('Order', id)
        .then((r) => setCalcValues(r.data || {}))
        .catch(() => {})
        .finally(() => setCalcLoading(false));
    }
  }, [calcDefinitions.length, id]);

  if (loading) return <div className="flex items-center justify-center h-32 text-gray-500">Loading...</div>;
  if (error) return <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">{error}</div>;
  if (!order) return null;

  const statusColors: Record<string, string> = {
    draft:      'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
    confirmed:  'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300',
    processing: 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300',
    shipped:    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300',
    delivered:  'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300',
    cancelled:  'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300',
  };

  const statusKey = (order.status || '').toLowerCase();
  const statusClass = statusColors[statusKey] || 'bg-gray-100 text-gray-800';

  const field = (label: string, value: React.ReactNode) => (
    <div key={label}>
      <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">{label}</dt>
      <dd className="mt-1 text-sm text-gray-900 dark:text-white">{value || '—'}</dd>
    </div>
  );

  const renderFieldValue = (value: any) => {
    if (value === null || value === undefined) return '—';
    if (Array.isArray(value)) return value.join(', ');
    if (typeof value === 'object' && 'value' in value) return String(value.value);
    return String(value);
  };

  const renderCalcValue = (value: any, returnType: string) => {
    if (value === null || value === undefined) return <span className="text-gray-400">—</span>;
    switch (returnType) {
      case 'number':
        return <span>{typeof value === 'number' ? value.toLocaleString() : String(value)}</span>;
      case 'boolean':
        return (
          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
            value ? 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300'
                  : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
          }`}>
            {value ? 'Yes' : 'No'}
          </span>
        );
      case 'date':
        return <span>{new Date(value).toLocaleDateString()}</span>;
      default:
        return <span>{String(value)}</span>;
    }
  };

  return (
    <div className="space-y-6 p-1">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{order.name || `Order #${order.id}`}</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Order #{order.id}</p>
        </div>
        <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${statusClass}`}>
          {order.status}
        </span>
      </div>

      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">Order Details</h3>
        <dl className="grid grid-cols-2 gap-4">
          {field('Customer ID', order.customerId)}
          {field('Customer', order.customerName)}
          {field('Currency', order.currency)}
          {field('Order Date', order.orderDate ? new Date(order.orderDate).toLocaleDateString() : null)}
          {field('Expected Delivery', order.expectedDeliveryDate ? new Date(order.expectedDeliveryDate).toLocaleDateString() : null)}
          {field('Created', order.createdAt ? new Date(order.createdAt).toLocaleDateString() : null)}
        </dl>
      </div>

      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">Financials</h3>
        <dl className="grid grid-cols-2 gap-4">
          {field('Subtotal', order.subtotal != null ? `$${Number(order.subtotal).toFixed(2)}` : null)}
          {field('Tax Amount', order.taxAmount != null ? `$${Number(order.taxAmount).toFixed(2)}` : null)}
        </dl>
      </div>

      {order.notes && (
        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-2">Notes</h3>
          <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{order.notes}</p>
        </div>
      )}

      {fieldDefinitions.length > 0 && (
        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">Additional Information</h3>
          <div className="grid gap-3" style={{ gridTemplateColumns: fieldDefinitions.length > 2 ? 'repeat(2, minmax(0, 1fr))' : '1fr' }}>
            {fieldDefinitions.map((f) => {
              if (f.type === 'document' || f.type === 'document_multi') {
                return (
                  <div key={f.key} className="col-span-2">
                    <DocumentFieldExplorer
                      entityType="Order"
                      entityId={id}
                      fieldKey={f.key}
                      multiple={f.type === 'document_multi'}
                      label={f.label}
                    />
                  </div>
                );
              }
              if (f.type === 'custom_record' || f.type === 'custom_record_multi') {
                return (
                  <div key={f.key} className="col-span-2">
                    <CustomRecordFieldPicker
                      value={order.customFields?.[f.key]}
                      onChange={() => {}}
                      multiple={f.type === 'custom_record_multi'}
                      label={f.label}
                      disabled={true}
                    />
                  </div>
                );
              }
              if (f.type === 'contact' || f.type === 'contact_multi') {
                return (
                  <div key={f.key} className="col-span-2">
                    <ContactFieldPicker
                      value={order.customFields?.[f.key]}
                      onChange={() => {}}
                      multiple={f.type === 'contact_multi'}
                      label={f.label}
                      disabled={true}
                    />
                  </div>
                );
              }
              if (f.type === 'workflow') {
                return (
                  <div key={f.key} className="col-span-2">
                    <WorkflowField
                      field={f}
                      value={order.customFields?.[f.key]}
                      onChange={() => {}}
                      disabled={true}
                    />
                  </div>
                );
              }
              return (
                <div key={f.key} className={f.type === 'richtext' ? 'col-span-2' : ''}>
                  <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{f.label}</label>
                  {f.type === 'richtext' ? (
                    <div className="mt-1 prose prose-sm dark:prose-invert max-w-none text-sm text-gray-900 dark:text-gray-100" dangerouslySetInnerHTML={{ __html: order.customFields?.[f.key] || '' }} />
                  ) : (
                    <div className="mt-1 text-sm text-gray-900 dark:text-gray-100">{renderFieldValue(order.customFields?.[f.key])}</div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {calcDefinitions.filter((d) => d.enabled).length > 0 && (
        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wide mb-4">Insights</h3>
          {calcLoading ? (
            <div className="text-sm text-gray-500 dark:text-gray-400">Calculating...</div>
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {calcDefinitions.filter((d) => d.enabled).map((def) => (
                <div key={def.key}>
                  <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{def.label}</label>
                  <div className="mt-1 text-sm text-gray-900 dark:text-gray-100">
                    {renderCalcValue(calcValues[def.key], def.returnType)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <Timeline entityType="Order" entityId={id} />
    </div>
  );
};
