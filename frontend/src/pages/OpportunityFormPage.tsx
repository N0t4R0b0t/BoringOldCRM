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
import { Layout } from '../components/Layout';
import { CustomFieldFormSection } from '../components/CustomFieldFormSection';
import { Timeline } from '../components/Timeline';
import { AccessControlPanel } from '../components/AccessControlPanel';
import { ContactFieldPicker } from '../components/ContactFieldPicker';
import { apiClient } from '../api/apiClient';
import { useCustomFields } from '../hooks/useCustomFields';
import { useCalculatedFields } from '../hooks/useCalculatedFields';
import { usePageTitle } from '../hooks/usePageTitle';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { PolicyInfoButton } from '../components/PolicyInfoButton';

export const OpportunityFormPage = () => {
  usePageTitle('Opportunity');
  const navigate = useNavigate();
  const { id, typeSlug } = useParams<{ id?: string; typeSlug?: string }>();
  const isEdit = !!id;
  const effectiveEntityType = typeSlug ? `Opportunity:${typeSlug}` : 'Opportunity';
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields(effectiveEntityType);
  const { fields: calcDefinitions } = useCalculatedFields(effectiveEntityType);
  const [calcValues, setCalcValues] = useState<Record<string, any>>({});
  const [formData, setFormData] = useState({
    accountName: '',
    status: 'open',
    value: '',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
  const { evaluateRules } = usePolicyRulesStore();
  const label = useEntityLabel('Opportunity');
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
  const [previousEntityData, setPreviousEntityData] = useState<Record<string, any> | undefined>(undefined);
  const [contacts, setContacts] = useState<any[]>([]);
  const [ownerId, setOwnerId] = useState<number | undefined>(undefined);
  const [teamMembers, setTeamMembers] = useState<{ userId: number; userEmail: string; userName?: string }[]>([]);

  useEffect(() => {
    if (isEdit && id) {
      const fetchData = async () => {
        try {
          const response = await apiClient.getOpportunity(parseInt(id));
          // Map backend DTO to form fields
          const resp = response.data || {};
          setFormData({
            accountName: resp.name || '',
            status: resp.stage || 'open',
            value: resp.value != null ? String(resp.value) : '',
          });
          if (resp.ownerId != null) setOwnerId(resp.ownerId);
          setPreviousEntityData({
            name: resp.name || null,
            stage: resp.stage || null,
            value: resp.value != null ? parseFloat(String(resp.value)) : null,
            customerId: resp.customerId || null,
          });
          if (resp.customFields) {
            setCustomFieldValues(resp.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch opportunity');
        }
      };
      fetchData();
    }
  }, [isEdit, id]);

  useEffect(() => {
    if (isEdit && id) {
      apiClient.listOpportunityContacts(parseInt(id))
        .then((r) => setContacts(r.data?.content || []))
        .catch(() => {});
    }
  }, [isEdit, id]);

  useEffect(() => {
    apiClient.getTenantMembers()
      .then((r) => setTeamMembers(r.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (isEdit && id && calcDefinitions.length > 0) {
      apiClient.evaluateCalculatedFields('Opportunity', parseInt(id))
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setPolicyWarnings([]);

    try {
      const payload = {
        name: formData.accountName,
        stage: formData.status,
        value: formData.value !== '' ? parseFloat(formData.value) : undefined,
        customFields: customFieldValues,
        ...(typeSlug ? { opportunityTypeSlug: typeSlug } : {}),
        ...(ownerId !== undefined ? { ownerId } : {}),
      };

      // Evaluate policy rules
      const operation = isEdit ? 'UPDATE' : 'CREATE';
      const previousData = isEdit ? previousEntityData : undefined;

      const policyResult = await evaluateRules('Opportunity', operation, payload, previousData);

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
      setError(err.response?.data?.message || 'Failed to save opportunity');
      setIsLoading(false);
    }
  };

  const executeSubmit = async (payload: any) => {
    setIsLoading(true);
    try {
      if (isEdit && id) {
        await apiClient.updateOpportunity(parseInt(id), payload);
      } else {
        await apiClient.createOpportunity(payload);
      }
      navigate(typeSlug ? `/opportunities/type/${typeSlug}` : '/opportunities');
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save opportunity';
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

  const handleAddContact = async (contact: any) => {
    if (!isEdit || !id) return;
    try {
      await apiClient.addContactToOpportunity(parseInt(id), contact.id);
      setContacts((prev) => {
        const exists = prev.some((c) => c.id === contact.id);
        return exists ? prev : [...prev, contact];
      });
    } catch (err) {
      console.error('Failed to add contact', err);
    }
  };

  const handleRemoveContact = async (contactId: number) => {
    if (!isEdit || !id) return;
    try {
      await apiClient.removeContactFromOpportunity(parseInt(id), contactId);
      setContacts((prev) => prev.filter((c) => c.id !== contactId));
    } catch (err) {
      console.error('Failed to remove contact', err);
    }
  };

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>{isEdit ? `Edit ${label}` : `New ${label}`}</h1>
          <PolicyInfoButton entityType="Opportunity" />
        </div>

        <div className="form-container">
          <form ref={formRef} onSubmit={handleSubmit} className="form">
            {error && <div ref={errorRef} className="error-banner">{error}</div>}

            <div className="form-group">
              <label htmlFor="accountName">Account Name *</label>
              <input
                id="accountName"
                type="text"
                name="accountName"
                value={formData.accountName}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="Account name"
              />
            </div>

            <div className="form-group">
              <label htmlFor="status">Status *</label>
              <select
                id="status"
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="form-input"
              >
                <option value="open">Open</option>
                <option value="closed-won">Closed Won</option>
                <option value="closed-lost">Closed Lost</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="value">Value</label>
              <input
                id="value"
                type="number"
                name="value"
                value={formData.value}
                onChange={handleChange}
                className="form-input"
                placeholder="0.00"
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

            {isEdit && id && (
              <div className="form-group">
                <label>Associated Contacts</label>
                <div className="mb-2">
                  <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
                    Involved stakeholders. Used to filter Notify Team recipients.
                  </p>
                  {contacts.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mb-3">
                      {contacts.map((c) => (
                        <span
                          key={c.id}
                          className="inline-flex items-center gap-1 pl-2.5 pr-1 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300"
                        >
                          👤 {c.name}
                          <button
                            type="button"
                            onClick={() => handleRemoveContact(c.id)}
                            className="ml-0.5 rounded-full w-4 h-4 flex items-center justify-center hover:bg-blue-200 dark:hover:bg-blue-800 transition-colors text-blue-600 dark:text-blue-400 font-bold text-xs"
                          >
                            &times;
                          </button>
                        </span>
                      ))}
                    </div>
                  )}
                  <ContactFieldPicker
                    value={null}
                    onChange={handleAddContact}
                    multiple={true}
                    label="Add Contacts"
                    required={false}
                    error=""
                  />
                </div>
              </div>
            )}

            <CustomFieldFormSection
              fields={customFields}
              values={customFieldValues}
              onChange={(fieldName, value) =>
                setCustomFieldValues((prev) => ({ ...prev, [fieldName]: value }))
              }
              isLoading={customFieldsLoading}
            />

            {isEdit && calcDefinitions.filter((d) => d.enabled).length > 0 && (
              <div className="form-group">
                <label>Insights</label>
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

            {isEdit && id && <Timeline entityType="Opportunity" entityId={parseInt(id)} />}

            <div className="form-actions">
              <button type="submit" disabled={isLoading} className="btn btn-primary">
                {isLoading ? 'Saving...' : 'Save Opportunity'}
              </button>
              <button
                type="button"
                onClick={() => navigate(typeSlug ? `/opportunities/type/${typeSlug}` : '/opportunities')}
                className="btn btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>

          {isEdit && id && (
            <AccessControlPanel entityType="Opportunity" entityId={parseInt(id)} ownerId={ownerId} />
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
