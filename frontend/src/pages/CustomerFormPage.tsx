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
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle';
import { Layout } from '../components/Layout';
import { CustomFieldFormSection } from '../components/CustomFieldFormSection';
import { Timeline } from '../components/Timeline';
import { apiClient } from '../api/apiClient';
import { useCrmStore } from '../store/crmStore';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { useCustomFields } from '../hooks/useCustomFields';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { PolicyInfoButton } from '../components/PolicyInfoButton';

export const CustomerFormPage = () => {
  usePageTitle('Customer');
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { customers, updateCustomer, addCustomer } = useCrmStore();
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Customer');
  const { evaluateRules } = usePolicyRulesStore();
  const label = useEntityLabel('Customer');
  // Related contact shape may be { id, name, ... } or { id, firstName, lastName }
  const [relatedContacts, setRelatedContacts] = useState<Array<{ id: number; name: string }>>([]);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    status: 'active',
  });
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, any>>({});
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

  const normalizePaged = (data: any) => {
    if (Array.isArray(data)) {
      return { content: data };
    }
    return data;
  };

  useEffect(() => {
    if (isEdit && id) {
      const customer = customers.find((c) => c.id === parseInt(id));
      if (customer) {
        setFormData({
          name: customer.name,
          status: customer.status,
        });
        if (customer.customFields) {
          setCustomFieldValues(customer.customFields);
        }
      } else {
        // If not found in store, fetch from API
        const fetchCustomer = async () => {
          setIsLoading(true);
          try {
            const resp = await apiClient.getCustomer(parseInt(id));
            const c = resp.data;
            setFormData({ name: c.name || '', status: c.status || 'active' });
            if (c.customFields) setCustomFieldValues(c.customFields);
            // add to store for future use
            addCustomer(c);
          } catch (e: any) {
            setError(e.response?.data?.message || 'Failed to load customer');
          } finally {
            setIsLoading(false);
          }
        };
        fetchCustomer();
      }
    }
  }, [isEdit, id, customers, updateCustomer]);

  useEffect(() => {
    if (isEdit && id) {
      const fetchContacts = async () => {
        setRelatedLoading(true);
        try {
          const response = await apiClient.getContacts({ customerId: parseInt(id), page: 0, size: 200 });
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
    setPolicyWarnings([]);

    try {
      const payload = {
        ...formData,
        customFields: customFieldValues,
      };

      // Evaluate policy rules
      const operation = isEdit ? 'UPDATE' : 'CREATE';
      const previousData = isEdit && id ? {
        name: customers.find((c) => c.id === parseInt(id))?.name,
        status: customers.find((c) => c.id === parseInt(id))?.status,
      } : undefined;

      const policyResult = await evaluateRules('Customer', operation, payload, previousData);

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
      setError(err.response?.data?.message || 'Failed to save customer');
      setIsLoading(false);
    }
  };

  const executeSubmit = async (payload: any) => {
    setIsLoading(true);
    try {
      if (isEdit && id) {
        const response = await apiClient.updateCustomer(parseInt(id), payload);
        updateCustomer(parseInt(id), response.data);
      } else {
        const response = await apiClient.createCustomer(payload);
        addCustomer(response.data);
      }
      navigate('/customers');
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
          <PolicyInfoButton entityType="Customer" />
        </div>

        <div className="form-container">
          <form ref={formRef} onSubmit={handleSubmit} className="form">
            {error && <div ref={errorRef} className="error-banner">{error}</div>}

            <div className="form-group">
              <label htmlFor="name">Customer Name *</label>
              <input
                id="name"
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="Company name"
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
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
                <option value="prospect">Prospect</option>
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

            {isEdit && (
              <div className="form-group">
                <label>Related Contacts</label>
                {relatedLoading ? (
                  <div className="loading">Loading contacts...</div>
                ) : relatedContacts.length === 0 ? (
                  <div className="empty-state">No related contacts</div>
                ) : (
                  <div className="list-container">
                    <table className="list-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {relatedContacts.map((contact) => (
                          <tr key={contact.id}>
                            <td>{contact.name}</td>
                            <td style={{ display: 'flex', gap: '8px' }}>
                              <Link to={`/contacts/${contact.id}/edit`} className="btn btn-sm btn-secondary">
                                Edit
                              </Link>
                              <button
                                type="button"
                                className="btn btn-sm btn-danger"
                                onClick={async () => {
                                  if (!confirm('Remove this contact from the customer?')) return;
                                  try {
                                    // Optimistic UI update
                                    setRelatedContacts((prev) => prev.filter((r) => r.id !== contact.id));
                                    await apiClient.disassociateContact(contact.id);
                                  } catch (e: any) {
                                    // Revert UI if API fails
                                    setRelatedLoading(true);
                                    try {
                                      const response = await apiClient.getContacts({ customerId: parseInt(id!), page: 0, size: 200 });
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

            {isEdit && id && <Timeline entityType="Customer" entityId={parseInt(id)} />}

            <div className="form-actions">
              <button type="submit" disabled={isLoading} className="btn btn-primary">
                {isLoading ? 'Saving...' : 'Save Customer'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/customers')}
                className="btn btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
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
