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
import { AccessControlPanel } from '../AccessControlPanel';
import { apiClient } from '../../api/apiClient';
import { useCustomFields } from '../../hooks/useCustomFields';
import { useCalculatedFields } from '../../hooks/useCalculatedFields';
import { useOpportunityTypesStore } from '../../store/opportunityTypesStore';
import { usePolicyRulesStore } from '../../store/policyRulesStore';
import { useEntityFieldOptions } from '../../hooks/useEntityFieldOptions';

interface OpportunityFormProps {
  id?: number;
  typeSlug?: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export const OpportunityForm: React.FC<OpportunityFormProps> = ({ id, typeSlug, onSuccess, onCancel }) => {
  const isEdit = !!id;
  const { types: opportunityTypes } = useOpportunityTypesStore();
  const [selectedTypeSlug, setSelectedTypeSlug] = useState<string>(typeSlug || '');
  const effectiveEntityType = selectedTypeSlug ? `Opportunity:${selectedTypeSlug}` : 'Opportunity';
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const topRef = useRef<HTMLDivElement>(null);
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields(effectiveEntityType);
  const { fields: calcDefinitions } = useCalculatedFields(effectiveEntityType);
  const [calcValues, setCalcValues] = useState<Record<string, any>>({});
  const [customers, setCustomers] = useState<Array<{ id: number; name: string }>>([]);
  const [customersLoading, setCustomersLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    stage: 'open',
    value: '',
    customerId: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
  const [previousEntityData, setPreviousEntityData] = useState<Record<string, any> | undefined>(undefined);
  const [policyWarnings, setPolicyWarnings] = useState<any[]>([]);
  useEffect(() => {
    if (error && topRef.current) {
      topRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);
  useEffect(() => {
    if (policyWarnings.length > 0 && topRef.current) {
      topRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [policyWarnings]);
  const [showWarningsModal, setShowWarningsModal] = useState(false);
  const [pendingSubmit, setPendingSubmit] = useState<any>(null);
  const [ownerId, setOwnerId] = useState<number | undefined>(undefined);
  const [teamMembers, setTeamMembers] = useState<{ userId: number; userEmail: string; userName?: string }[]>([]);
  const { evaluateRules } = usePolicyRulesStore();
  const stageOptions = useEntityFieldOptions('Opportunity', 'stage');

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
          const response = await apiClient.getOpportunity(id);
          const resp = response.data || {};
          setFormData({
            name: resp.name || '',
            stage: resp.stage || 'open',
            value: resp.value != null ? String(resp.value) : '',
            customerId: resp.customerId ? String(resp.customerId) : '',
          });
          setPreviousEntityData({
            name: resp.name || null,
            stage: resp.stage || null,
            value: resp.value != null ? parseFloat(String(resp.value)) : null,
            customerId: resp.customerId || null,
          });
          if (resp.opportunityTypeSlug != null) {
            setSelectedTypeSlug(resp.opportunityTypeSlug);
          }
          if (resp.ownerId != null) setOwnerId(resp.ownerId);
          if (resp.customFields) {
            setCustomFieldValues(resp.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch opportunity');
        }
      };
      fetchData();
    } else {
      setFormData({ name: '', stage: 'open', value: '', customerId: '' });
      setCustomFieldValues({});
    }
  }, [isEdit, id]);

  useEffect(() => {
    const fetchCustomers = async () => {
      setCustomersLoading(true);
      try {
        const response = await apiClient.getCustomers({ page: 0, size: 500 });
        const data = normalizePaged(response.data);
        setCustomers(data.content || []);
      } catch {
        setCustomers([]);
      } finally {
        setCustomersLoading(false);
      }
    };
    fetchCustomers();
  }, []);

  useEffect(() => {
    apiClient.getTenantMembers()
      .then((r) => setTeamMembers(r.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (isEdit && id && calcDefinitions.length > 0) {
      apiClient.evaluateCalculatedFields('Opportunity', id)
        .then((r) => setCalcValues(r.data || {}))
        .catch(() => {});
    }
  }, [isEdit, id, calcDefinitions.length]);

  const renderCalcValue = (value: any, returnType: string) => {
    if (value === null || value === undefined) return '—';
    switch (returnType) {
      case 'number': return typeof value === 'number' ? value.toLocaleString() : String(value);
      case 'boolean': return value ? 'Yes' : 'No';
      case 'date': return new Date(value).toLocaleDateString();
      default: return String(value);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const executeSubmit = async (payload: any) => {
    setIsLoading(true);
    try {
      if (isEdit && id) {
        await apiClient.updateOpportunity(id, payload);
      } else {
        await apiClient.createOpportunity(payload);
      }
      onSuccess();
    } catch (err: any) {
      const data = err.response?.data;
      if (data?.errorCode === 'POLICY_VIOLATION' && Array.isArray(data?.violations) && data.violations.length > 0) {
        setError(`Operation blocked by policy: ${data.violations.map((v: any) => `${v.ruleName}: ${v.message}`).join(', ')}`);
      } else {
        setError(data?.message || 'Failed to save opportunity');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmWarnings = async () => {
    setShowWarningsModal(false);
    if (pendingSubmit) {
      await executeSubmit(pendingSubmit);
      setPendingSubmit(null);
    }
  };

  const handleCancelWarnings = () => {
    setShowWarningsModal(false);
    setPendingSubmit(null);
    setPolicyWarnings([]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setPolicyWarnings([]);

    try {
      const payload = {
        name: formData.name,
        stage: formData.stage,
        value: formData.value ? parseFloat(formData.value) : null,
        customerId: formData.customerId ? parseInt(formData.customerId) : null,
        customFields: customFieldValues,
        opportunityTypeSlug: selectedTypeSlug,
        ...(ownerId !== undefined ? { ownerId } : {}),
      };

      const operation = isEdit ? 'UPDATE' : 'CREATE';
      const previousData = isEdit ? previousEntityData : undefined;
      const policyResult = await evaluateRules('Opportunity', operation, payload, previousData);

      if (policyResult.blocked) {
        setError(`Operation blocked by policy: ${policyResult.violations.map((v: any) => `${v.ruleName}: ${v.message}`).join(', ')}`);
        setIsLoading(false);
        return;
      }

      if (policyResult.warnings.length > 0) {
        setPolicyWarnings(policyResult.warnings);
        setPendingSubmit(payload);
        setShowWarningsModal(true);
        setIsLoading(false);
        return;
      }

      await executeSubmit(payload);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save opportunity');
      setIsLoading(false);
    }
  };

  return (
    <div ref={topRef}>
      <form onSubmit={handleSubmit}>
        {error && <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">{error}</div>}

        {showWarningsModal && (
          <div className="mb-4 rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50 dark:bg-amber-900/30 p-4">
            <p className="text-sm font-semibold text-amber-800 dark:text-amber-200 mb-2">Policy Warnings — do you want to proceed?</p>
            <div className="space-y-2 mb-3">
              {policyWarnings.map((w: any) => (
                <div key={w.ruleId} className="text-sm text-amber-700 dark:text-amber-300">
                  <strong>{w.ruleName}:</strong> {w.message}
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={handleConfirmWarnings} className="px-3 py-1.5 text-sm font-medium bg-amber-600 text-white rounded-md hover:bg-amber-700 transition-colors">
                Proceed Anyway
              </button>
              <button type="button" onClick={handleCancelWarnings} className="px-3 py-1.5 text-sm font-medium bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                Cancel
              </button>
            </div>
          </div>
        )}

        <div className="form-group mb-3">
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Opportunity Name *</label>
          <input
            id="name"
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Opportunity Name"
          />
        </div>

        <div className="grid gap-4 mb-3" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div className="form-group mb-0">
            <label htmlFor="customerId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Customer *</label>
            <select
              id="customerId"
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={customersLoading}
            >
              <option value="">Select a Customer</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group mb-0">
            <label htmlFor="stage" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Stage *</label>
            <select
              id="stage"
              name="stage"
              value={formData.stage}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {stageOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          {opportunityTypes.length > 0 && (
            <div className="form-group mb-0">
              <label htmlFor="opportunityType" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
              <select
                id="opportunityType"
                value={selectedTypeSlug}
                onChange={e => setSelectedTypeSlug(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">— No Type —</option>
                {opportunityTypes.map(t => (
                  <option key={t.slug} value={t.slug}>{t.name}</option>
                ))}
              </select>
            </div>
          )}

          <div className="form-group mb-0">
            <label htmlFor="value" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Value</label>
            <input
              id="value"
              type="number"
              name="value"
              value={formData.value}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="0.00"
            />
          </div>
        </div>

        <div className="form-group mb-3">
          <label htmlFor="ownerId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Owner</label>
          <select
            id="ownerId"
            value={ownerId ?? ''}
            onChange={(e) => setOwnerId(e.target.value ? parseInt(e.target.value) : undefined)}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">— Unassigned —</option>
            {teamMembers.map((m) => (
              <option key={m.userId} value={m.userId}>{m.userName ?? m.userEmail}</option>
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
          entityType="Opportunity"
          entityId={isEdit ? id : undefined}
        />

        {isEdit && calcDefinitions.filter((d) => d.enabled).length > 0 && (
          <div className="mb-3">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Insights</label>
            <div className="grid grid-cols-2 gap-3">
              {calcDefinitions.filter((d) => d.enabled).map((def) => (
                <div key={def.key} className="px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700">
                  <div className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1">{def.label}</div>
                  <div className="text-sm text-gray-900 dark:text-gray-100">{renderCalcValue(calcValues[def.key], def.returnType)}</div>
                </div>
              ))}
            </div>
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
          <button type="submit" disabled={isLoading} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium disabled:opacity-50">
            {isLoading ? 'Saving...' : 'Save Opportunity'}
          </button>
        </div>
      </form>

      {isEdit && id && (
        <AccessControlPanel entityType="Opportunity" entityId={id} ownerId={ownerId} />
      )}
    </div>
  );
};
