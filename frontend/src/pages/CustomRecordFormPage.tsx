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
import { useNavigate, useParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { AccessControlPanel } from '../components/AccessControlPanel';
import { apiClient } from '../api/apiClient';
import { usePageTitle } from '../hooks/usePageTitle';

export const CustomRecordFormPage = () => {
  usePageTitle('CustomRecord');
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    name: '',
    type: '',
    serialNumber: '',
    status: 'active',
    customerId: '',
    notes: '',
  });
  const [ownerId, setOwnerId] = useState<number | undefined>(undefined);

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getCustomRecord(parseInt(id));
          const d = response.data;
          setFormData({
            name: d.name || '',
            type: d.type || '',
            serialNumber: d.serialNumber || '',
            status: d.status || 'active',
            customerId: d.customerId != null ? String(d.customerId) : '',
            notes: d.notes || '',
          });
          if (d.ownerId != null) setOwnerId(d.ownerId);
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch customRecord');
        }
      };
      fetchData();
    }
  }, [isEdit, id]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
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
      };
      if (isEdit && id) {
        await apiClient.updateCustomRecord(parseInt(id), payload);
      } else {
        await apiClient.createCustomRecord(payload);
      }
      navigate('/custom-records');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save customRecord');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <div className="form-page">
        <div className="form-header">
          <button className="btn btn-secondary" onClick={() => navigate('/custom-records')}>
            &larr; Back
          </button>
          <h1 className="page-title">{isEdit ? 'Edit CustomRecord' : 'New CustomRecord'}</h1>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="form-card">
          <div className="form-group">
            <label htmlFor="name">Name *</label>
            <input
              id="name"
              name="name"
              type="text"
              required
              value={formData.name}
              onChange={handleChange}
              placeholder="CustomRecord name"
            />
          </div>

          <div className="form-group">
            <label htmlFor="type">Type</label>
            <input
              id="type"
              name="type"
              type="text"
              value={formData.type}
              onChange={handleChange}
              placeholder="e.g. Hardware, Software, Vehicle"
            />
          </div>

          <div className="form-group">
            <label htmlFor="serialNumber">Serial Number</label>
            <input
              id="serialNumber"
              name="serialNumber"
              type="text"
              value={formData.serialNumber}
              onChange={handleChange}
              placeholder="Serial or customRecord number"
            />
          </div>

          <div className="form-group">
            <label htmlFor="status">Status</label>
            <select id="status" name="status" value={formData.status} onChange={handleChange}>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="retired">Retired</option>
              <option value="maintenance">Maintenance</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="customerId">Customer ID</label>
            <input
              id="customerId"
              name="customerId"
              type="number"
              value={formData.customerId}
              onChange={handleChange}
              placeholder="Optional customer ID"
            />
          </div>

          <div className="form-group">
            <label htmlFor="notes">Notes</label>
            <textarea
              id="notes"
              name="notes"
              rows={3}
              value={formData.notes}
              onChange={handleChange}
              placeholder="Optional notes"
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/custom-records')}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              {isLoading ? 'Saving...' : isEdit ? 'Save Changes' : 'Create CustomRecord'}
            </button>
          </div>
        </form>

        {isEdit && id && (
          <AccessControlPanel entityType="CustomRecord" entityId={parseInt(id)} ownerId={ownerId} />
        )}
      </div>
    </Layout>
  );
};
