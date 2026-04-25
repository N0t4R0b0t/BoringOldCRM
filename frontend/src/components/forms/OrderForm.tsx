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
import { apiClient } from '../../api/apiClient';
import { CustomFieldFormSection } from '../CustomFieldFormSection';
import { useCustomFields } from '../../hooks/useCustomFields';
import { EntityPicker, type EntityOption } from '../EntityPicker';

interface OrderFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

const todayIso = () => new Date().toISOString().slice(0, 10);

const buildSuggestedName = (customerName: string, date: string) =>
  `${customerName} - ${date || todayIso()}`;

export const OrderForm: React.FC<OrderFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Order');
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
  const [selectedCustomer, setSelectedCustomer] = useState<EntityOption | null>(null);
  // true once the user manually edits the name field (stops auto-suggestion)
  const nameTouched = useRef(isEdit);

  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);

  const [formData, setFormData] = useState({
    name: '',
    status: 'DRAFT',
    currency: 'USD',
    subtotal: '',
    taxAmount: '',
    orderDate: isEdit ? '' : todayIso(),
    expectedDeliveryDate: '',
    notes: '',
  });

  useEffect(() => {
    if (isEdit && id) {
      setIsLoading(true);
      apiClient
        .getOrder(id)
        .then((response) => {
          const order = response.data;
          setFormData({
            name: order.name || '',
            status: order.status || 'DRAFT',
            currency: order.currency || 'USD',
            subtotal: order.subtotal || '',
            taxAmount: order.taxAmount || '',
            orderDate: order.orderDate || '',
            expectedDeliveryDate: order.expectedDeliveryDate || '',
            notes: order.notes || '',
          });
          if (order.customFields) setCustomFieldValues(order.customFields);
          if (order.customerId) {
            apiClient.getCustomer(order.customerId)
              .then((r) => setSelectedCustomer({ id: r.data.id, label: r.data.name }))
              .catch(() => setSelectedCustomer({ id: order.customerId, label: `Customer #${order.customerId}` }));
          }
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to load order');
        })
        .finally(() => setIsLoading(false));
    }
  }, [id, isEdit]);

  // Auto-suggest name when customer or date changes (only if user hasn't typed their own name)
  useEffect(() => {
    if (nameTouched.current || !selectedCustomer) return;
    setFormData((prev) => ({
      ...prev,
      name: buildSuggestedName(selectedCustomer.label, prev.orderDate),
    }));
  }, [selectedCustomer, formData.orderDate]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    if (name === 'name') nameTouched.current = true;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const payload = {
        ...formData,
        customerId: selectedCustomer?.id ?? null,
        customFields: customFieldValues,
      };
      if (isEdit && id) {
        await apiClient.updateOrder(id, payload);
      } else {
        await apiClient.createOrder(payload);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save order');
    } finally {
      setIsLoading(false);
    }
  };

  const inputClass = 'w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500';
  const labelClass = 'block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1';

  return (
    <div>
      <form onSubmit={handleSubmit}>
        {error && (
          <div ref={errorRef} className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <EntityPicker
              label="Customer *"
              value={selectedCustomer}
              onChange={setSelectedCustomer}
              placeholder="Search customers..."
              required
              disabled={isLoading}
              onSearch={async (term) => {
                const res = await apiClient.searchCustomers(term, 0, 20);
                return (res.data.content || []).map((c: any) => ({ id: c.id, label: c.name }));
              }}
            />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Order Name</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              disabled={isLoading}
              className={inputClass}
              placeholder={selectedCustomer ? buildSuggestedName(selectedCustomer.label, formData.orderDate) : 'Order name'}
            />
            {!nameTouched.current && formData.name && (
              <span className="text-xs text-gray-400 dark:text-gray-500 mt-1 italic block">
                Auto-suggested — edit to override
              </span>
            )}
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Status</label>
            <select name="status" value={formData.status} onChange={handleChange} disabled={isLoading} className={inputClass}>
              <option value="DRAFT">Draft</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="PROCESSING">Processing</option>
              <option value="SHIPPED">Shipped</option>
              <option value="DELIVERED">Delivered</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Currency</label>
            <input type="text" name="currency" value={formData.currency} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="USD" />
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Subtotal</label>
            <input type="number" step="0.01" name="subtotal" value={formData.subtotal} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="0.00" />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Tax Amount</label>
            <input type="number" step="0.01" name="taxAmount" value={formData.taxAmount} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="0.00" />
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Order Date</label>
            <input type="date" name="orderDate" value={formData.orderDate} onChange={handleChange} disabled={isLoading} className={inputClass} />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Expected Delivery Date</label>
            <input type="date" name="expectedDeliveryDate" value={formData.expectedDeliveryDate} onChange={handleChange} disabled={isLoading} className={inputClass} />
          </div>
        </div>

        <div className="form-group mb-4">
          <label className={labelClass}>Notes</label>
          <textarea name="notes" value={formData.notes} onChange={handleChange} disabled={isLoading} rows={3} className={inputClass} placeholder="Additional notes..." />
        </div>

        <CustomFieldFormSection
          fields={customFields}
          values={customFieldValues}
          onChange={(fieldName, value) => setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))}
          isLoading={customFieldsLoading}
          entityType="Order"
          entityId={isEdit ? id : undefined}
        />

        <div className="sticky bottom-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3 pt-4 pb-2 -mx-6 px-6 mt-6">
          <button type="button" onClick={onCancel} disabled={isLoading} className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 font-medium">
            Cancel
          </button>
          <button type="submit" disabled={isLoading} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50">
            {isLoading ? 'Saving...' : isEdit ? 'Update Order' : 'Create Order'}
          </button>
        </div>
      </form>
    </div>
  );
};
