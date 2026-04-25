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
import { useNavigate, useParams } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { CustomFieldFormSection } from '../components/CustomFieldFormSection';
import { AccessControlPanel } from '../components/AccessControlPanel';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { PolicyInfoButton } from '../components/PolicyInfoButton';

export const ActivityFormPage = () => {
  usePageTitle('Activity');
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Activity');
  const label = useEntityLabel('Activity');
  const [formData, setFormData] = useState({
    title: '',
    type: 'call',
    description: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
  const { evaluateRules } = usePolicyRulesStore();
  const [policyWarnings, setPolicyWarnings] = useState<any[]>([]);
  const errorRef = useRef<HTMLDivElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [error]);
  useEffect(() => {
    if (policyWarnings.length > 0 && formRef.current) {
      formRef.current.closest('.overflow-y-auto')?.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }, [policyWarnings]);
  const [showWarningsModal, setShowWarningsModal] = useState(false);
  const [pendingSubmit, setPendingSubmit] = useState<{ payload: any; operation: 'create' | 'update' } | null>(null);
  const [ownerId, setOwnerId] = useState<number | undefined>(undefined);
  const [teamMembers, setTeamMembers] = useState<{ userId: number; userEmail: string; userName?: string }[]>([]);

  useEffect(() => {
    apiClient.getTenantMembers()
      .then((r) => setTeamMembers(r.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getActivity(parseInt(id));
          setFormData(response.data);
          if (response.data.ownerId != null) setOwnerId(response.data.ownerId);
          if (response.data.customFields) {
            setCustomFieldValues(response.data.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch activity');
        }
      };
      fetchData();
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
    setPolicyWarnings([]);

    try {
      const payload = {
        ...formData,
        customFields: customFieldValues,
        ...(ownerId !== undefined ? { ownerId } : {}),
      };

      // Evaluate policy rules
      const operation = isEdit ? 'UPDATE' : 'CREATE';
      const previousData = isEdit && id ? {
        title: '',
        type: 'call',
        description: '',
      } : undefined;

      const policyResult = await evaluateRules('Activity', operation, payload, previousData);

      // If there are DENY violations, show error
      if (policyResult.blocked) {
        setError(`Operation blocked by policy: ${policyResult.violations.map((v: any) => v.message).join(', ')}`);
        setIsLoading(false);
        return;
      }

      // If there are WARN violations, show confirmation modal
      if (policyResult.warnings.length > 0) {
        setPolicyWarnings(policyResult.warnings);
        setPendingSubmit({ payload, operation: isEdit ? 'update' : 'create' });
        setShowWarningsModal(true);
        setIsLoading(false);
        return;
      }

      // No violations, proceed with API call
      await executeSubmit(payload);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save activity');
      setIsLoading(false);
    }
  };

  const executeSubmit = async (payload: any) => {
    setIsLoading(true);
    try {
      if (isEdit && id) {
        await apiClient.updateActivity(parseInt(id), payload);
      } else {
        await apiClient.createActivity(payload);
      }
      navigate('/activities');
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

  const handleConfirmWarnings = async () => {
    setShowWarningsModal(false);
    if (pendingSubmit) {
      await executeSubmit(pendingSubmit.payload);
      setPendingSubmit(null);
    }
  };

  const handleCancelWarnings = () => {
    setShowWarningsModal(false);
    setPendingSubmit(null);
    setPolicyWarnings([]);
  };

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>{isEdit ? `Edit ${label}` : `New ${label}`}</h1>
          <PolicyInfoButton entityType="Activity" />
        </div>

        <div className="form-container">
          <form ref={formRef} onSubmit={handleSubmit} className="form">
            {error && <div ref={errorRef} className="error-banner">{error}</div>}

            <div className="form-group">
              <label htmlFor="title">Title *</label>
              <input
                id="title"
                type="text"
                name="title"
                value={formData.title}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="Activity title"
              />
            </div>

            <div className="form-group">
              <label htmlFor="type">Type *</label>
              <select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleChange}
                className="form-input"
              >
                <option value="call">Call</option>
                <option value="email">Email</option>
                <option value="meeting">Meeting</option>
                <option value="note">Note</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                value={(formData as any).description}
                onChange={handleChange}
                className="form-input"
                placeholder="Activity details"
                rows={4}
              />
            </div>

            <div className="form-group">
              <label htmlFor="ownerId">Owner</label>
              <select
                id="ownerId"
                value={ownerId ?? ''}
                onChange={(e) => setOwnerId(e.target.value ? parseInt(e.target.value) : undefined)}
                className="form-input"
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
            />

            <div className="form-actions">
              <button type="submit" disabled={isLoading} className="btn btn-primary">
                {isLoading ? 'Saving...' : 'Save Activity'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/activities')}
                className="btn btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>

          {isEdit && id && (
            <AccessControlPanel entityType="Activity" entityId={parseInt(id)} ownerId={ownerId} />
          )}
        </div>

        {/* Policy Warnings Modal */}
        {showWarningsModal && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
            justifyContent: 'center', alignItems: 'center', zIndex: 1000
          }}>
            <div style={{
              backgroundColor: 'white', borderRadius: '8px', padding: '24px',
              maxWidth: '500px', maxHeight: '80vh', overflow: 'auto',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
            }} className="dark:bg-gray-800">
              <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-gray-100">Policy Warnings</h3>
              <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
                The following policy rules generated warnings. Do you want to proceed anyway?
              </p>
              <div style={{ marginBottom: '16px' }}>
                {policyWarnings.map((w: any) => (
                  <div key={w.ruleId} style={{
                    padding: '12px', marginBottom: '8px',
                    backgroundColor: '#fef3c7', border: '1px solid #f59e0b',
                    borderRadius: '4px', color: '#92400e'
                  }} className="dark:bg-amber-900 dark:border-amber-700 dark:text-amber-200">
                    <strong>{w.ruleName}:</strong> {w.message}
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={handleCancelWarnings}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleConfirmWarnings}
                  className="btn btn-primary"
                >
                  Proceed Anyway
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};
