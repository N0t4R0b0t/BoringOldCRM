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
import { useEntityLabel } from '../../hooks/useEntityLabel';
import { useEntityFieldOptions, getOptionBadgeColor, getOptionLabel } from '../../hooks/useEntityFieldOptions';
import { DocumentFieldExplorer } from '../DocumentFieldExplorer';
import { CustomRecordFieldPicker } from '../CustomRecordFieldPicker';
import { ContactFieldPicker } from '../ContactFieldPicker';
import { WorkflowField } from '../WorkflowField';

interface CustomerViewProps {
  id: number;
  onEdit?: () => void;
  onDelete?: () => void;
  onClose?: () => void;
}

export const CustomerView: React.FC<CustomerViewProps> = ({ id }) => {
  const [customer, setCustomer] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [calcValues, setCalcValues] = useState<Record<string, any>>({});
  const [calcLoading, setCalcLoading] = useState(false);
  const { fields: fieldDefinitions } = useCustomFields('Customer');
  const label = useEntityLabel('Customer');
  const { fields: calcDefinitions } = useCalculatedFields('Customer');
  const { dataRefreshToken } = useUiStore();
  const statusOptions = useEntityFieldOptions('Customer', 'status');

  const badgeColorMap: Record<string, string> = {
    blue: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300',
    green: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300',
    yellow: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300',
    red: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300',
    purple: 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300',
    orange: 'bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300',
    teal: 'bg-teal-100 text-teal-800 dark:bg-teal-900/40 dark:text-teal-300',
    gray: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
  };

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await apiClient.getCustomer(id);
        setCustomer(res.data);
        setCalcLoading(true);
        apiClient.evaluateCalculatedFields('Customer', id)
          .then((r) => setCalcValues(r.data || {}))
          .catch(() => {})
          .finally(() => setCalcLoading(false));
      } catch (e: any) {
        setError('Failed to load customer');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id, dataRefreshToken]);

  useEffect(() => {
    if (calcDefinitions.length > 0) {
      setCalcLoading(true);
      apiClient.evaluateCalculatedFields('Customer', id)
        .then((r) => setCalcValues(r.data || {}))
        .catch(() => {})
        .finally(() => setCalcLoading(false));
    }
  }, [calcDefinitions.length, id]);

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

  if (loading) return <div className="p-4 text-center text-gray-500 dark:text-gray-400">Loading...</div>;
  if (error) return <div className="p-4 text-center text-red-500 dark:text-red-400">{error}</div>;
  if (!customer) return null;

  return (
    <div className="space-y-6">
      <div className="bg-white dark:bg-gray-900 rounded-lg border border-gray-200 dark:border-gray-700 p-4 shadow-sm">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">{label} Details</h3>
        <div className="grid grid-cols-1 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Name</label>
            <div className="mt-1 text-sm text-gray-900 dark:text-gray-100 font-medium">{customer.name}</div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Status</label>
            <div className="mt-1">
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${badgeColorMap[getOptionBadgeColor(customer.status, statusOptions)] || badgeColorMap.gray}`}>
                {getOptionLabel(customer.status, statusOptions)}
              </span>
            </div>
          </div>

          {fieldDefinitions.length > 0 && (
            <div className="pt-4 border-t border-gray-100 dark:border-gray-700 mt-4">
              <h4 className="text-sm font-medium text-gray-900 dark:text-gray-100 mb-3">Additional Information</h4>
              <div className="grid gap-3" style={{ gridTemplateColumns: fieldDefinitions.length > 2 ? 'repeat(2, minmax(0, 1fr))' : '1fr' }}>
                {fieldDefinitions.map((f) => {
                  if (f.type === 'document' || f.type === 'document_multi') {
                    return (
                      <div key={f.key} className="col-span-2">
                        <DocumentFieldExplorer
                          entityType="Customer"
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
                          value={customer.customFields?.[f.key]}
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
                          value={customer.customFields?.[f.key]}
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
                          value={customer.customFields?.[f.key]}
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
                        <div className="mt-1 prose prose-sm dark:prose-invert max-w-none text-sm text-gray-900 dark:text-gray-100" dangerouslySetInnerHTML={{ __html: customer.customFields?.[f.key] || '' }} />
                      ) : (
                        <div className="mt-1 text-sm text-gray-900 dark:text-gray-100">{renderFieldValue(customer.customFields?.[f.key])}</div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      {calcDefinitions.filter((d) => d.enabled).length > 0 && (
        <div className="bg-white dark:bg-gray-900 rounded-lg border border-gray-200 dark:border-gray-700 p-4 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Insights</h3>
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

      <Timeline entityType="Customer" entityId={id} />
    </div>
  );
};
