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
import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';

const todayIso = () => new Date().toISOString().slice(0, 10);

const buildSuggestedName = (customerName: string, date: string) => {
  const datePart = date || todayIso();
  return customerName ? `${customerName} - ${datePart}` : '';
};

export const OrderFormPage = () => {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  usePageTitle(id ? `Edit Order ${id}` : 'New Order');

  const [formData, setFormData] = useState({
    customerId: '',
    name: '',
    status: 'DRAFT',
    currency: 'USD',
    subtotal: '',
    taxAmount: '',
    totalAmount: '',
    orderDate: id ? '' : todayIso(),
    expectedDeliveryDate: '',
    notes: '',
  });

  // Track whether the user has manually edited the name field
  const nameTouched = useRef(!!id);
  const [customerName, setCustomerName] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (id) {
      setIsLoading(true);
      apiClient
        .getOrder(parseInt(id))
        .then((response) => {
          const order = response.data;
          setFormData({
            customerId: order.customerId || '',
            name: order.name || '',
            status: order.status || 'DRAFT',
            currency: order.currency || 'USD',
            subtotal: order.subtotal || '',
            taxAmount: order.taxAmount || '',
            totalAmount: order.totalAmount || '',
            orderDate: order.orderDate || '',
            expectedDeliveryDate: order.expectedDeliveryDate || '',
            notes: order.notes || '',
          });
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to load order');
        })
        .finally(() => setIsLoading(false));
    }
  }, [id]);

  // Fetch customer name when customerId changes (new orders only)
  useEffect(() => {
    if (nameTouched.current) return;
    const parsed = parseInt(formData.customerId);
    if (!formData.customerId || isNaN(parsed)) {
      setCustomerName('');
      setFormData((prev) => ({ ...prev, name: '' }));
      return;
    }
    apiClient
      .getCustomer(parsed)
      .then((res) => {
        const name = res.data.name || '';
        setCustomerName(name);
        setFormData((prev) => ({
          ...prev,
          name: buildSuggestedName(name, prev.orderDate),
        }));
      })
      .catch(() => {
        setCustomerName('');
      });
  }, [formData.customerId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Keep suggestion in sync when orderDate changes (only if name not touched)
  useEffect(() => {
    if (nameTouched.current || !customerName) return;
    setFormData((prev) => ({
      ...prev,
      name: buildSuggestedName(customerName, prev.orderDate),
    }));
  }, [formData.orderDate, customerName]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    if (name === 'name') {
      nameTouched.current = true;
    }
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      if (id) {
        await apiClient.updateOrder(parseInt(id), formData);
        setSuccess('Order updated successfully');
      } else {
        await apiClient.createOrder(formData);
        setSuccess('Order created successfully');
        setTimeout(() => navigate('/orders'), 1500);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save order');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <div className="form-page">
        <h1>{id ? `Edit Order ${id}` : 'New Order'}</h1>

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
            <label>Order Name</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              disabled={isLoading}
              placeholder={customerName ? buildSuggestedName(customerName, formData.orderDate) : 'Enter order name'}
            />
            {!nameTouched.current && formData.name && (
              <span className="field-hint">Auto-suggested — edit to override</span>
            )}
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
              <option value="CONFIRMED">Confirmed</option>
              <option value="SHIPPED">Shipped</option>
              <option value="DELIVERED">Delivered</option>
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
              <label>Order Date</label>
              <input
                type="date"
                name="orderDate"
                value={formData.orderDate}
                onChange={handleChange}
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <label>Expected Delivery Date</label>
              <input
                type="date"
                name="expectedDeliveryDate"
                value={formData.expectedDeliveryDate}
                onChange={handleChange}
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
              {isLoading ? 'Saving...' : 'Save Order'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/orders')}
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
