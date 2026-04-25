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
import { CustomFieldFormSection } from '../CustomFieldFormSection';
import { apiClient } from '../../api/apiClient';
import { useCustomFields } from '../../hooks/useCustomFields';
import { useEntityFieldOptions } from '../../hooks/useEntityFieldOptions';

interface CustomRecordFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export const CustomRecordForm: React.FC<CustomRecordFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('CustomRecord');
  const statusOptions = useEntityFieldOptions('CustomRecord', 'status');
  const [customers, setCustomers] = useState<Array<{ id: number; name: string }>>([]);
  const [formData, setFormData] = useState({
    name: '',
    type: '',
    serialNumber: '',
    status: 'active',
    customerId: '',
    notes: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const response = await apiClient.getCustomers({ page: 0, size: 500 });
        const data = Array.isArray(response.data) ? { content: response.data } : response.data;
        setCustomers(data.content || []);
      } catch {
        setCustomers([]);
      }
    };
    fetchCustomers();
  }, []);

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getCustomRecord(id);
          const resp = response.data || {};
          setFormData({
            name: resp.name || '',
            type: resp.type || '',
            serialNumber: resp.serialNumber || '',
            status: resp.status || 'active',
            customerId: resp.customerId ? String(resp.customerId) : '',
            notes: resp.notes || '',
          });
          if (resp.customFields) setCustomFieldValues(resp.customFields);
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch customRecord');
        }
      };
      fetchData();
    } else {
      setFormData({ name: '', type: '', serialNumber: '', status: 'active', customerId: '', notes: '' });
      setCustomFieldValues({});
    }
  }, [isEdit, id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      const payload = {
        name: formData.name,
        type: formData.type || undefined,
        serialNumber: formData.serialNumber || undefined,
        status: formData.status,
        customerId: formData.customerId ? parseInt(formData.customerId) : undefined,
        notes: formData.notes || undefined,
        customFields: customFieldValues,
      };
      if (isEdit && id) {
        await apiClient.updateCustomRecord(id, payload);
      } else {
        await apiClient.createCustomRecord(payload);
      }
      onSuccess();
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save customRecord';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        <div className="form-group mb-3">
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            CustomRecord Name *
          </label>
          <input
            id="name"
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="CustomRecord Name"
          />
        </div>

        <div className="grid gap-4 mb-3" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label htmlFor="customerId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Customer
            </label>
            <select
              id="customerId"
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">— No Customer —</option>
              {customers.map((c) => (
                <option key={c.id} value={String(c.id)}>{c.name}</option>
              ))}
            </select>
          </div>

          <div className="form-group mb-0">
            <label htmlFor="status" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Status *
            </label>
            <select
              id="status"
              name="status"
              value={formData.status}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {statusOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="form-group mb-0">
            <label htmlFor="type" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Type
            </label>
            <input
              id="type"
              type="text"
              name="type"
              value={formData.type}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. Hardware, Vehicle, Software"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="serialNumber" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Serial Number
            </label>
            <input
              id="serialNumber"
              type="text"
              name="serialNumber"
              value={formData.serialNumber}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Serial / CustomRecord Tag"
            />
          </div>
        </div>

        <div className="form-group mb-3">
          <label htmlFor="notes" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Notes
          </label>
          <textarea
            id="notes"
            name="notes"
            value={formData.notes}
            onChange={handleChange}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Additional notes..."
          />
        </div>

        <CustomFieldFormSection
          fields={customFields}
          values={customFieldValues}
          onChange={(fieldName, value) =>
            setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))
          }
          isLoading={customFieldsLoading}
          entityType="CustomRecord"
          entityId={isEdit ? id : undefined}
        />

        <div className="sticky bottom-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3 pt-4 pb-2 -mx-6 px-6 mt-6">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 font-medium"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50"
          >
            {isLoading ? 'Saving...' : 'Save CustomRecord'}
          </button>
        </div>
      </form>
    </div>
  );
};
