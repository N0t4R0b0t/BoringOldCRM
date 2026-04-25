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
import { useEntityFieldOptions } from '../../hooks/useEntityFieldOptions';

interface ActivityFormProps {
  id?: number;
  onSuccess: () => void;
  onCancel: () => void;
}

export const ActivityForm: React.FC<ActivityFormProps> = ({ id, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Activity');
  const typeOptions = useEntityFieldOptions('Activity', 'type');
  const [formData, setFormData] = useState({
    title: '',
    type: 'call',
    description: '',
    relatedEntityType: 'Customer',
    relatedEntityId: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({})
  const [relatedEntities, setRelatedEntities] = useState<any[]>([]);

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getActivity(id);
          setFormData({
            ...response.data,
            title: response.data.subject || response.data.title || '',
            relatedEntityType: response.data.relatedEntityType || 'Customer',
            relatedEntityId: response.data.relatedEntityId || ''
          });
          if (response.data.customFields) {
            setCustomFieldValues(response.data.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch activity');
        }
      };
      fetchData();
    } else {
      setFormData({ title: '', type: 'call', description: '', relatedEntityType: 'Customer', relatedEntityId: '' });
      setCustomFieldValues({});
    }
  }, [isEdit, id]);

  useEffect(() => {
    const fetchRelatedEntities = async () => {
      try {
        let response;
        if (formData.relatedEntityType === 'Customer') {
          response = await apiClient.getCustomers({ size: 100 });
        } else if (formData.relatedEntityType === 'Contact') {
          response = await apiClient.getContacts({ size: 100 });
        } else if (formData.relatedEntityType === 'Opportunity') {
          response = await apiClient.getOpportunities({ size: 100 });
        }
        
        if (response) {
            const data = response.data.content || response.data;
            setRelatedEntities(data);
        }
      } catch (err) {
        console.error('Failed to fetch related entities', err);
        setRelatedEntities([]);
      }
    };

    fetchRelatedEntities();
  }, [formData.relatedEntityType]);

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
        subject: formData.title,
        type: formData.type,
        description: formData.description,
        relatedType: formData.relatedEntityType,
        relatedId: formData.relatedEntityId ? Number(formData.relatedEntityId) : undefined,
        customFields: customFieldValues,
      };

      if (isEdit && id) {
        await apiClient.updateActivity(id, payload);
      } else {
        await apiClient.createActivity(payload);
      }
      onSuccess();
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save activity';
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
            <label htmlFor="title" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Title *</label>
            <input
              id="title"
              type="text"
              name="title"
              value={formData.title}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Activity title"
            />
          </div>

          <div className="form-group mb-0">
            <label htmlFor="type" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type *</label>
            <select
              id="type"
              name="type"
              value={formData.type}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {typeOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 mb-3">
            <div className="form-group">
              <label htmlFor="relatedEntityType" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Related To *</label>
              <select
                id="relatedEntityType"
                name="relatedEntityType"
                value={formData.relatedEntityType}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="Customer">Customer</option>
                <option value="Contact">Contact</option>
                <option value="Opportunity">Opportunity</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="relatedEntityId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Select Entity *</label>
              <select
                id="relatedEntityId"
                name="relatedEntityId"
                value={formData.relatedEntityId}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select...</option>
                {relatedEntities.map((entity: any) => (
                  <option key={entity.id} value={entity.id}>
                    {entity.name || (entity.firstName ? `${entity.firstName} ${entity.lastName}` : entity.title || entity.subject || `ID: ${entity.id}`)}
                  </option>
                ))}
              </select>
            </div>
        </div>

        <div className="form-group mb-3">
          <label htmlFor="description" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
          <textarea
            id="description"
            name="description"
            value={(formData as any).description}
            onChange={handleChange}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Activity details"
            rows={4}
          />
        </div>

        <CustomFieldFormSection
          fields={customFields}
          values={customFieldValues}
          onChange={(fieldName, value) =>
            setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))
          }
          isLoading={customFieldsLoading}
          entityType="Activity"
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
            {isLoading ? 'Saving...' : 'Save Activity'}
          </button>
        </div>
      </form>
    </div>
  );
};
