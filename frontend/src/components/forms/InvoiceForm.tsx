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

interface InvoiceFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export const InvoiceForm: React.FC<InvoiceFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Invoice');
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
  const [selectedCustomer, setSelectedCustomer] = useState<EntityOption | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<EntityOption | null>(null);

  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);

  const [formData, setFormData] = useState({
    status: 'DRAFT',
    currency: 'USD',
    subtotal: '',
    taxAmount: '',
    totalAmount: '',
    dueDate: '',
    paymentTerms: '',
    notes: '',
  });

  useEffect(() => {
    if (isEdit && id) {
      setIsLoading(true);
      apiClient
        .getInvoice(id)
        .then((response) => {
          const invoice = response.data;
          setFormData({
            status: invoice.status || 'DRAFT',
            currency: invoice.currency || 'USD',
            subtotal: invoice.subtotal || '',
            taxAmount: invoice.taxAmount || '',
            totalAmount: invoice.totalAmount || '',
            dueDate: invoice.dueDate || '',
            paymentTerms: invoice.paymentTerms || '',
            notes: invoice.notes || '',
          });
          if (invoice.customFields) setCustomFieldValues(invoice.customFields);
          if (invoice.customerId) {
            apiClient.getCustomer(invoice.customerId)
              .then((r) => setSelectedCustomer({ id: r.data.id, label: r.data.name }))
              .catch(() => setSelectedCustomer({ id: invoice.customerId, label: `Customer #${invoice.customerId}` }));
          }
          if (invoice.orderId) {
            apiClient.getOrder(invoice.orderId)
              .then((r) => setSelectedOrder({ id: r.data.id, label: r.data.name || `Order #${r.data.id}` }))
              .catch(() => setSelectedOrder({ id: invoice.orderId, label: `Order #${invoice.orderId}` }));
          }
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to load invoice');
        })
        .finally(() => setIsLoading(false));
    }
  }, [id, isEdit]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
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
        customerId: selectedCustomer?.id ?? null,
        orderId: selectedOrder?.id ?? null,
        customFields: customFieldValues,
      };
      if (isEdit && id) {
        await apiClient.updateInvoice(id, payload);
      } else {
        await apiClient.createInvoice(payload);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save invoice');
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
            <EntityPicker
              label="Linked Order"
              value={selectedOrder}
              onChange={setSelectedOrder}
              placeholder="Search orders (optional)..."
              disabled={isLoading}
              onSearch={async (term) => {
                const res = await apiClient.searchOrders({ term, size: 20 });
                return (res.data.content || []).map((o: any) => ({
                  id: o.id,
                  label: o.name || `Order #${o.id}`,
                  sublabel: o.status,
                }));
              }}
            />
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Status</label>
            <select name="status" value={formData.status} onChange={handleChange} disabled={isLoading} className={inputClass}>
              <option value="DRAFT">Draft</option>
              <option value="SENT">Sent</option>
              <option value="PAID">Paid</option>
              <option value="OVERDUE">Overdue</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Currency</label>
            <input type="text" name="currency" value={formData.currency} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="USD" />
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Subtotal</label>
            <input type="number" step="0.01" name="subtotal" value={formData.subtotal} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="0.00" />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Tax Amount</label>
            <input type="number" step="0.01" name="taxAmount" value={formData.taxAmount} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="0.00" />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Total Amount</label>
            <input type="number" step="0.01" name="totalAmount" value={formData.totalAmount} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="0.00" />
          </div>
        </div>

        <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label className={labelClass}>Due Date</label>
            <input type="date" name="dueDate" value={formData.dueDate} onChange={handleChange} disabled={isLoading} className={inputClass} />
          </div>
          <div className="form-group mb-0">
            <label className={labelClass}>Payment Terms</label>
            <input type="text" name="paymentTerms" value={formData.paymentTerms} onChange={handleChange} disabled={isLoading} className={inputClass} placeholder="e.g., NET-30" />
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
          entityType="Invoice"
          entityId={isEdit ? id : undefined}
        />

        <div className="sticky bottom-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3 pt-4 pb-2 -mx-6 px-6 mt-6">
          <button type="button" onClick={onCancel} disabled={isLoading} className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 font-medium">
            Cancel
          </button>
          <button type="submit" disabled={isLoading} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50">
            {isLoading ? 'Saving...' : isEdit ? 'Update Invoice' : 'Create Invoice'}
          </button>
        </div>
      </form>
    </div>
  );
};
