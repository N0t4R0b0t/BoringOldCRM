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
import { apiClient } from '../api/apiClient';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { useCustomFields } from '../hooks/useCustomFields';
import { useEntityLabel } from '../hooks/useEntityLabel';
import { usePageTitle } from '../hooks/usePageTitle';
import { PolicyInfoButton } from '../components/PolicyInfoButton';

export const ContactFormPage = () => {
  usePageTitle('Contact');
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { fields: customFields, isLoading: customFieldsLoading } = useCustomFields('Contact');
  const { evaluateRules } = usePolicyRulesStore();
  const label = useEntityLabel('Contact');
  const [customers, setCustomers] = useState<Array<{ id: number; name: string }>>([]);
  const [customersLoading, setCustomersLoading] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    customerId: '',
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
      const fetchData = async () => {
        try {
          const response = await apiClient.getContact(parseInt(id));

          // Backend currently returns a single `name` field (full name).
          // Populate firstName/lastName by splitting `name` if firstName/lastName are not provided.
          const respData = response.data || {};
          let first = respData.firstName || '';
          let last = respData.lastName || '';

          if ((!first && !last) && respData.name) {
            const parts = String(respData.name).trim().split(/\s+/);
            if (parts.length === 1) {
              first = parts[0];
              last = '';
            } else if (parts.length > 1) {
              first = parts[0];
              last = parts.slice(1).join(' ');
            }
          }

          setFormData({
            firstName: first,
            lastName: last,
            email: respData.email || '',
            phone: respData.phone || '',
            customerId: respData.customerId ? String(respData.customerId) : '',
          });

          if (respData.customFields) {
            setCustomFieldValues(respData.customFields);
          }
        } catch (err: any) {
          setError(err.response?.data?.message || 'Failed to fetch contact');
        }
      };
      fetchData();
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
        customerId: formData.customerId ? parseInt(formData.customerId) : null,
        customFields: customFieldValues,
      };

      // Evaluate policy rules
      const operation = isEdit ? 'UPDATE' : 'CREATE';
      const previousData = isEdit && id ? {
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        customerId: null,
      } : undefined;

      const policyResult = await evaluateRules('Contact', operation, payload, previousData);

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
      setError(err.response?.data?.message || 'Failed to save contact');
      setIsLoading(false);
    }
  };

  const executeSubmit = async (payload: any) => {
    setIsLoading(true);
    try {
      if (isEdit && id) {
        await apiClient.updateContact(parseInt(id), payload);
      } else {
        await apiClient.createContact(payload);
      }
      navigate('/contacts');
    } catch (err: any) {
      const errData = err.response?.data;
      const msg = errData?.errorCode === 'POLICY_VIOLATION' && errData?.violations?.length
        ? `Operation blocked by policy: ${errData.violations.map((v: any) => v.message).join('; ')}`
        : errData?.message || 'Failed to save contact';
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
          <PolicyInfoButton entityType="Contact" />
        </div>

        <div className="form-container">
          <form ref={formRef} onSubmit={handleSubmit} className="form">
            {error && <div ref={errorRef} className="error-banner">{error}</div>}

            <div className="form-group">
              <label htmlFor="firstName">First Name *</label>
              <input
                id="firstName"
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="First name"
              />
            </div>

            <div className="form-group">
              <label htmlFor="lastName">Last Name *</label>
              <input
                id="lastName"
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="Last name"
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="form-input"
                placeholder="email@example.com"
              />
            </div>

            <div className="form-group">
              <label htmlFor="phone">Phone</label>
              <input
                id="phone"
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                className="form-input"
                placeholder="+1 (555) 000-0000"
              />
            </div>

            <div className="form-group">
              <label htmlFor="customerId">Customer</label>
              <select
                id="customerId"
                name="customerId"
                value={formData.customerId}
                onChange={handleChange}
                className="form-input"
                disabled={customersLoading}
              >
                <option value="">No linked customer</option>
                {customers.map((customer) => (
                  <option key={customer.id} value={customer.id}>
                    {customer.name}
                  </option>
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

            {isEdit && id && <Timeline entityType="Contact" entityId={parseInt(id)} />}

            <div className="form-actions">
              <button type="submit" disabled={isLoading} className="btn btn-primary">
                {isLoading ? 'Saving...' : 'Save Contact'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/contacts')}
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
