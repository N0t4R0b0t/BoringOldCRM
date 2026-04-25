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
import { useParams, useNavigate } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';

export const InvoiceFormPage = () => {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  usePageTitle(id ? `Edit Invoice ${id}` : 'New Invoice');

  const [formData, setFormData] = useState({
    customerId: '',
    orderId: '',
    status: 'DRAFT',
    currency: 'USD',
    subtotal: '',
    taxAmount: '',
    totalAmount: '',
    dueDate: '',
    paymentTerms: '',
    notes: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (id) {
      setIsLoading(true);
      apiClient
        .getInvoice(parseInt(id))
        .then((response) => {
          const invoice = response.data;
          setFormData({
            customerId: invoice.customerId || '',
            orderId: invoice.orderId || '',
            status: invoice.status || 'DRAFT',
            currency: invoice.currency || 'USD',
            subtotal: invoice.subtotal || '',
            taxAmount: invoice.taxAmount || '',
            totalAmount: invoice.totalAmount || '',
            dueDate: invoice.dueDate || '',
            paymentTerms: invoice.paymentTerms || '',
            notes: invoice.notes || '',
          });
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to load invoice');
        })
        .finally(() => setIsLoading(false));
    }
  }, [id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      if (id) {
        await apiClient.updateInvoice(parseInt(id), formData);
        setSuccess('Invoice updated successfully');
      } else {
        await apiClient.createInvoice(formData);
        setSuccess('Invoice created successfully');
        setTimeout(() => navigate('/invoices'), 1500);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save invoice');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <div className="form-page">
        <h1>{id ? `Edit Invoice ${id}` : 'New Invoice'}</h1>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        <form onSubmit={handleSubmit} className="form">
          <div className="form-group">
            <label>Customer ID *</label>
            <input
              type="number"
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              required
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label>Order ID</label>
            <input
              type="number"
              name="orderId"
              value={formData.orderId}
              onChange={handleChange}
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label>Status</label>
            <select
              name="status"
              value={formData.status}
              onChange={handleChange}
              disabled={isLoading}
            >
              <option value="DRAFT">Draft</option>
              <option value="SENT">Sent</option>
              <option value="PAID">Paid</option>
              <option value="OVERDUE">Overdue</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          <div className="form-group">
            <label>Currency</label>
            <input
              type="text"
              name="currency"
              value={formData.currency}
              onChange={handleChange}
              disabled={isLoading}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Subtotal</label>
              <input
                type="number"
                step="0.01"
                name="subtotal"
                value={formData.subtotal}
                onChange={handleChange}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label>Tax Amount</label>
              <input
                type="number"
                step="0.01"
                name="taxAmount"
                value={formData.taxAmount}
                onChange={handleChange}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label>Total Amount</label>
              <input
                type="number"
                step="0.01"
                name="totalAmount"
                value={formData.totalAmount}
                onChange={handleChange}
                disabled={isLoading}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Due Date</label>
              <input
                type="date"
                name="dueDate"
                value={formData.dueDate}
                onChange={handleChange}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label>Payment Terms</label>
              <input
                type="text"
                name="paymentTerms"
                value={formData.paymentTerms}
                onChange={handleChange}
                placeholder="e.g., NET-30"
                disabled={isLoading}
              />
            </div>
          </div>

          <div className="form-group">
            <label>Notes</label>
            <textarea
              name="notes"
              value={formData.notes}
              onChange={handleChange}
              disabled={isLoading}
              rows={4}
            />
          </div>

          <div className="form-actions">
            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary"
            >
              {isLoading ? 'Saving...' : 'Save Invoice'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/invoices')}
              disabled={isLoading}
              className="btn-secondary"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </Layout>
  );
};
