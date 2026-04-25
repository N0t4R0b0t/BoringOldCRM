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
import React, { useEffect, useRef, useState } from 'react';
import { CustomFieldFormSection } from '../CustomFieldFormSection';
import { apiClient } from '../../api/apiClient';
import { useCustomFields } from '../../hooks/useCustomFields';

interface ContactFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export const ContactForm: React.FC<ContactFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Contact');
  const [customers, setCustomers] = useState<Array<{ id: number; name: string }>>([]);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    customerId: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});

  const normalizePaged = (data: any) => {
    if (Array.isArray(data)) {
      return { content: data };
    }
    return data;
  };

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getContact(id);
          const respData = response.data || {};
          let first = respData.firstName || '';
          let last = respData.lastName || '';

          if ((!first && !last) && respData.name) {
            const parts = String(respData.name).trim().split(/\s+/);
            if (parts.length === 1) {
              first = parts[0];
              last = '';
            } else if (parts.length > 1) {
              first = parts[0];
              last = parts.slice(1).join(' ');
            }
          }

          setFormData({
            firstName: first,
            lastName: last,
            email: respData.email || '',
            phone: respData.phone || '',
            customerId: respData.customerId ? String(respData.customerId) : '',
          });

          if (respData.customFields) {
            setCustomFieldValues(respData.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch contact');
        }
      };
      fetchData();
    } else {
      setFormData({ firstName: '', lastName: '', email: '', phone: '', customerId: '' });
      setCustomFieldValues({});
    }
  }, [isEdit, id]);

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const response = await apiClient.getCustomers({ page: 0, size: 500 });
        const data = normalizePaged(response.data);
        setCustomers(data.content || []);
      } catch {
        setCustomers([]);
      }
    };
    fetchCustomers();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const payload = {
        name: `${formData.firstName} ${formData.lastName}`.trim(),
        email: formData.email,
        phone: formData.phone,
        customerId: formData.customerId ? parseInt(formData.customerId) : null,
        customFields: customFieldValues,
      };

      if (isEdit && id) {
        await apiClient.updateContact(id, payload);
      } else {
        await apiClient.createContact(payload);
      }
      onSuccess();
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save contact';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        {error && <div ref={errorRef} className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">{error}</div>}

        <div className="grid gap-4 mb-3" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">First Name *</label>
            <input
              id="firstName"
              type="text"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="First name"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Last Name *</label>
            <input
              id="lastName"
              type="text"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Last name"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
            <input
              id="email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="email@example.com"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="phone" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Phone</label>
            <input
              id="phone"
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="+1 (555) 000-0000"
            />
          </div>
        </div>

        <div className="form-group mb-3">
          <label htmlFor="customerId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Customer</label>
          <select
            id="customerId"
            name="customerId"
            value={formData.customerId}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">No linked customer</option>
            {customers.map((customer) => (
              <option key={customer.id} value={String(customer.id)}>
                {customer.name}
              </option>
            ))}
          </select>
        </div>

        <CustomFieldFormSection
          fields={customFields}
          values={customFieldValues}
          onChange={(fieldName, value) =>
            setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))
          }
          isLoading={customFieldsLoading}
          entityType="Contact"
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
          <button type="submit" disabled={isLoading} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50">
            {isLoading ? 'Saving...' : 'Save Contact'}
          </button>
        </div>
      </form>
    </div>
  );
};
