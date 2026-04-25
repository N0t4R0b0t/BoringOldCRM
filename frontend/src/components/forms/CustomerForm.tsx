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
import { Link } from 'react-router-dom';
import { CustomFieldFormSection } from '../CustomFieldFormSection';
import { apiClient } from '../../api/apiClient';
import { useCrmStore } from '../../store/crmStore';
import { useCustomFields } from '../../hooks/useCustomFields';
import { useEntityFieldOptions } from '../../hooks/useEntityFieldOptions';

interface CustomerFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export const CustomerForm: React.FC<CustomerFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);
  const { customers, updateCustomer, addCustomer } = useCrmStore();
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Customer');
  const statusOptions = useEntityFieldOptions('Customer', 'status');
  const [relatedContacts, setRelatedContacts] = useState<Array<{ id: number; name: string }>>([]);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    status: 'active',
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
      const customer = customers.find((c) => c.id === id);
      if (customer) {
        setFormData({
          name: customer.name,
          status: customer.status,
        });
        if (customer.customFields) {
          setCustomFieldValues(customer.customFields);
        }
      } else {
        const fetchCustomer = async () => {
          setIsLoading(true);
          try {
            const resp = await apiClient.getCustomer(id);
            const c = resp.data;
            setFormData({ name: c.name || '', status: c.status || 'active' });
            if (c.customFields) setCustomFieldValues(c.customFields);
            addCustomer(c);
          } catch (e: any) {
            setError(e.response?.data?.message || 'Failed to load customer');
          } finally {
            setIsLoading(false);
          }
        };
        fetchCustomer();
      }
    } else {
      setFormData({ name: '', status: 'active' });
      setCustomFieldValues({});
    }
  }, [isEdit, id, customers, addCustomer]);

  useEffect(() => {
    if (isEdit && id) {
      const fetchContacts = async () => {
        setRelatedLoading(true);
        try {
          const response = await apiClient.getContacts({ customerId: id, page: 0, size: 200 });
          const data = normalizePaged(response.data);
          const items = (data.content || []).map((c: any) => {
            const rawName = (c.name && String(c.name).trim()) || `${(c.firstName || '')} ${(c.lastName || '')}`.trim();
            const fallback = rawName || c.email || c.phone || `#${c.id}`;
            return { id: c.id, name: fallback };
          });
          setRelatedContacts(items);
        } catch {
          setRelatedContacts([]);
        } finally {
          setRelatedLoading(false);
        }
      };
      fetchContacts();
    }
  }, [isEdit, id]);

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
        ...formData,
        customFields: customFieldValues,
      };

      if (isEdit && id) {
        const response = await apiClient.updateCustomer(id, payload);
        updateCustomer(id, response.data);
      } else {
        const response = await apiClient.createCustomer(payload);
        addCustomer(response.data);
      }
      onSuccess();
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save customer';
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
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Customer Name *</label>
            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Company name"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="status" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Status *</label>
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
        </div>

        <CustomFieldFormSection
          fields={customFields}
          values={customFieldValues}
          onChange={(fieldName, value) =>
            setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))
          }
          isLoading={customFieldsLoading}
          entityType="Customer"
          entityId={isEdit ? id : undefined}
        />

        {isEdit && (
          <div className="form-group mb-3">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Related Contacts</label>
            {relatedLoading ? (
              <div className="text-gray-500 text-sm">Loading contacts...</div>
            ) : relatedContacts.length === 0 ? (
              <div className="text-gray-500 text-sm">No related contacts</div>
            ) : (
              <div className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Name</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                    {relatedContacts.map((contact) => (
                      <tr key={contact.id}>
                        <td className="px-4 py-2 text-sm text-gray-900 dark:text-gray-200">{contact.name}</td>
                        <td className="px-4 py-2 text-sm flex gap-2">
                          <Link to={`/contacts/${contact.id}/edit`} className="text-blue-600 hover:text-blue-800">
                            Edit
                          </Link>
                          <button
                            type="button"
                            className="text-red-600 hover:text-red-800"
                            onClick={async () => {
                              if (!confirm('Remove this contact from the customer?')) return;
                              try {
                                setRelatedContacts((prev) => prev.filter((r) => r.id !== contact.id));
                                await apiClient.disassociateContact(contact.id);
                              } catch (e: any) {
                                setRelatedLoading(true);
                                try {
                                  const response = await apiClient.getContacts({ customerId: id!, page: 0, size: 200 });
                                  const data = normalizePaged(response.data);
                                  const items = (data.content || []).map((c: any) => {
                                    const rawName = (c.name && String(c.name).trim()) || `${(c.firstName || '')} ${(c.lastName || '')}`.trim();
                                    const fallback = rawName || c.email || c.phone || `#${c.id}`;
                                    return { id: c.id, name: fallback };
                                  });
                                  setRelatedContacts(items);
                                } catch {
                                  setRelatedContacts([]);
                                } finally {
                                  setRelatedLoading(false);
                                }
                                alert(e.response?.data?.message || 'Failed to remove contact');
                              }
                            }}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}


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
            {isLoading ? 'Saving...' : 'Save Customer'}
          </button>
        </div>
      </form>
    </div>
  );
};
