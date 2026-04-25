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
import { useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { useUiStore } from '../store/uiStore';
import { useCustomFieldsStore } from '../store/customFieldsStore';
import { useCalculatedFieldsStore } from '../store/calculatedFieldsStore';
import { usePolicyRulesStore } from '../store/policyRulesStore';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { usePageTitle } from '../hooks/usePageTitle';
import { clearOptionCache } from '../hooks/useEntityFieldOptions';

const FIELD_TYPES = [
  { value: 'text', label: 'Text' },
  { value: 'number', label: 'Number' },
  { value: 'date', label: 'Date' },
  { value: 'boolean', label: 'Boolean (Checkbox)' },
  { value: 'select', label: 'Select (Dropdown)' },
  { value: 'textarea', label: 'Textarea (Multi-line)' },
  { value: 'multiselect', label: 'Multi-Select' },
  { value: 'url', label: 'URL' },
  { value: 'email', label: 'Email' },
  { value: 'phone', label: 'Phone' },
  { value: 'currency', label: 'Currency' },
  { value: 'percentage', label: 'Percentage' },
  { value: 'richtext', label: 'Rich Text' },
  { value: 'document', label: 'Document (single file)' },
  { value: 'document_multi', label: 'Document (multiple files)' },
  { value: 'custom_record', label: 'CustomRecord Link (single)' },
  { value: 'custom_record_multi', label: 'CustomRecord Link (multiple)' },
  { value: 'contact', label: 'Contact Link (single)' },
  { value: 'contact_multi', label: 'Contact Link (multiple)' },
  { value: 'workflow', label: 'Workflow (Milestones)' },
];

const RETURN_TYPES = [
  { value: 'text', label: 'Text' },
  { value: 'number', label: 'Number' },
  { value: 'boolean', label: 'Boolean' },
  { value: 'date', label: 'Date' },
];

const OPERATIONS = [
  { value: 'CREATE', label: 'Create' },
  { value: 'UPDATE', label: 'Update' },
  { value: 'DELETE', label: 'Delete' },
];

const SEVERITIES = [
  { value: 'DENY', label: 'Deny (blocks operation)' },
  { value: 'WARN', label: 'Warn (user confirms)' },
];

const ChevronIcon = ({ open }: { open: boolean }) => (
  <svg
    className={`h-5 w-5 text-gray-500 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
    fill="none" stroke="currentColor" viewBox="0 0 24 24"
  >
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
  </svg>
);

const DragHandleIcon = () => (
  <svg className="h-4 w-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
    <path d="M7 4a1 1 0 100-2 1 1 0 000 2zm6 0a1 1 0 100-2 1 1 0 000 2zM7 10a1 1 0 100-2 1 1 0 000 2zm6 0a1 1 0 100-2 1 1 0 000 2zM7 16a1 1 0 100-2 1 1 0 000 2zm6 0a1 1 0 100-2 1 1 0 000 2z" />
  </svg>
);

export const CustomFieldsPage = () => {
  usePageTitle('Data Model');
  const { types: opportunityTypes } = useOpportunityTypesStore();
  const [searchParams] = useSearchParams();

  const entityTypes: { label: string; value: string }[] = [
    { label: 'Customer', value: 'Customer' },
    { label: 'Contact', value: 'Contact' },
    { label: opportunityTypes.length > 0 ? 'Opportunity (General)' : 'Opportunity', value: 'Opportunity' },
    ...opportunityTypes.map(t => ({ label: t.name, value: `Opportunity:${t.slug}` })),
    { label: 'Activity', value: 'Activity' },
    { label: 'CustomRecord', value: 'CustomRecord' },
    { label: 'Order', value: 'Order' },
    { label: 'Invoice', value: 'Invoice' },
  ];

  const initialEntity = (() => {
    const param = searchParams.get('entityType');
    if (param && entityTypes.some(e => e.value === param)) return param;
    const saved = sessionStorage.getItem('customFieldsPage_selectedEntity');
    if (saved && entityTypes.some(e => e.value === saved)) return saved;
    return entityTypes[0].value;
  })();

  const [selectedEntity, setSelectedEntity] = useState(initialEntity);
  const [error, setError] = useState('');
  const { dataRefreshToken } = useUiStore();
  const { clearCache: clearCustomCache } = useCustomFieldsStore();
  const { clearCache: clearCalcCache } = useCalculatedFieldsStore();

  // ── Custom Fields ──
  const [customSectionOpen, setCustomSectionOpen] = useState(() => {
    const saved = sessionStorage.getItem('customFieldsPage_customSectionOpen');
    return saved !== null ? JSON.parse(saved) : true;
  });
  const [fields, setFields] = useState<any[]>([]);
  const [loadingCustom, setLoadingCustom] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [showCustomForm, setShowCustomForm] = useState(false);
  const [form, setForm] = useState({
    key: '', label: '', type: 'text', required: false,
    options: '', milestones: '', displayInTable: false,
  });
  const [customDragIdx, setCustomDragIdx] = useState<number | null>(null);
  const [customDragOverIdx, setCustomDragOverIdx] = useState<number | null>(null);

  // ── Calculated Fields ──
  const [calcSectionOpen, setCalcSectionOpen] = useState(() => {
    const saved = sessionStorage.getItem('customFieldsPage_calcSectionOpen');
    return saved !== null ? JSON.parse(saved) : true;
  });
  const [calcFields, setCalcFields] = useState<any[]>([]);
  const [loadingCalc, setLoadingCalc] = useState(false);
  const [isEditingCalc, setIsEditingCalc] = useState(false);
  const [editCalcId, setEditCalcId] = useState<number | null>(null);
  const [showCalcForm, setShowCalcForm] = useState(false);
  const [calcForm, setCalcForm] = useState({
    key: '', label: '', returnType: 'text', expression: '', enabled: true, displayInTable: false,
  });
  const [calcDragIdx, setCalcDragIdx] = useState<number | null>(null);
  const [calcDragOverIdx, setCalcDragOverIdx] = useState<number | null>(null);

  // ── Core Field Options ──
  const CORE_FIELD_CONFIG: Record<string, { fieldName: string; label: string }[]> = {
    Customer: [{ fieldName: 'status', label: 'Status' }],
    Opportunity: [{ fieldName: 'stage', label: 'Stage' }],
    Activity: [{ fieldName: 'type', label: 'Type' }],
    CustomRecord: [{ fieldName: 'status', label: 'Status' }],
  };
  const coreFieldsForEntity = CORE_FIELD_CONFIG[selectedEntity] ?? [];
  const hasCoreFields = coreFieldsForEntity.length > 0;

  const [coreOptionsSectionOpen, setCoreOptionsSectionOpen] = useState(() => {
    const saved = sessionStorage.getItem('customFieldsPage_coreOptionsSectionOpen');
    return saved !== null ? JSON.parse(saved) : true;
  });
  // Map of fieldName → ordered option list
  const [coreOptions, setCoreOptions] = useState<Record<string, { value: string; label: string }[]>>({});
  const [loadingCoreOptions, setLoadingCoreOptions] = useState(false);
  const [coreOptionsSaving, setCoreOptionsSaving] = useState<Record<string, boolean>>({});
  // Per-field "add new option" form state
  const [newOptionLabel, setNewOptionLabel] = useState<Record<string, string>>({});
  // Per-field drag state
  const [coreDragField, setCoreDragField] = useState<string | null>(null);
  const [coreDragIdx, setCoreDragIdx] = useState<number | null>(null);
  const [coreDragOverIdx, setCoreDragOverIdx] = useState<number | null>(null);

  // ── Policy Rules ──
  const [policySectionOpen, setPolicySectionOpen] = useState(() => {
    const saved = sessionStorage.getItem('customFieldsPage_policySectionOpen');
    return saved !== null ? JSON.parse(saved) : false;
  });
  const [policyRules, setPolicyRules] = useState<any[]>([]);
  const [loadingPolicy, setLoadingPolicy] = useState(false);
  const [isEditingPolicy, setIsEditingPolicy] = useState(false);
  const [editPolicyId, setEditPolicyId] = useState<number | null>(null);
  const [showPolicyForm, setShowPolicyForm] = useState(false);
  const [policyForm, setPolicyForm] = useState({
    name: '', description: '', operations: [] as string[], expression: '', severity: 'DENY', enabled: true,
  });
  const [policyDragIdx, setPolicyDragIdx] = useState<number | null>(null);
  const [policyDragOverIdx, setPolicyDragOverIdx] = useState<number | null>(null);
  const { clearCache: clearPolicyCache } = usePolicyRulesStore();

  // Save tab state to sessionStorage
  useEffect(() => {
    sessionStorage.setItem('customFieldsPage_selectedEntity', selectedEntity);
  }, [selectedEntity]);

  useEffect(() => {
    sessionStorage.setItem('customFieldsPage_customSectionOpen', JSON.stringify(customSectionOpen));
  }, [customSectionOpen]);

  useEffect(() => {
    sessionStorage.setItem('customFieldsPage_calcSectionOpen', JSON.stringify(calcSectionOpen));
  }, [calcSectionOpen]);

  useEffect(() => {
    sessionStorage.setItem('customFieldsPage_policySectionOpen', JSON.stringify(policySectionOpen));
  }, [policySectionOpen]);

  useEffect(() => {
    sessionStorage.setItem('customFieldsPage_coreOptionsSectionOpen', JSON.stringify(coreOptionsSectionOpen));
  }, [coreOptionsSectionOpen]);

  useEffect(() => {
    fetchFields();
    fetchCalcFields();
    fetchPolicyRules();
    if (hasCoreFields) fetchCoreOptions();
    handleCancel();
    handleCancelCalc();
    handleCancelPolicy();
    setNewOptionLabel({});
  }, [selectedEntity, dataRefreshToken]);

  // ── Core Field Options logic ──

  const fetchCoreOptions = async () => {
    setLoadingCoreOptions(true);
    try {
      const res = await apiClient.getEntityFieldOptions(selectedEntity);
      setCoreOptions(res.data || {});
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load field options');
    } finally {
      setLoadingCoreOptions(false);
    }
  };

  const saveCoreOptions = async (fieldName: string, options: { value: string; label: string }[]) => {
    setCoreOptionsSaving((s) => ({ ...s, [fieldName]: true }));
    try {
      await apiClient.updateEntityFieldOptions(selectedEntity, fieldName, options);
      setCoreOptions((prev) => ({ ...prev, [fieldName]: options }));
      // Clear the module-level cache so pages/views pick up the new options
      clearOptionCache(selectedEntity);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to save field options');
    } finally {
      setCoreOptionsSaving((s) => ({ ...s, [fieldName]: false }));
    }
  };

  const handleCoreOptionDelete = (fieldName: string, idx: number) => {
    const updated = (coreOptions[fieldName] || []).filter((_, i) => i !== idx);
    saveCoreOptions(fieldName, updated);
  };

  const handleCoreOptionLabelChange = (fieldName: string, idx: number, newLabel: string) => {
    const updated = (coreOptions[fieldName] || []).map((opt, i) =>
      i === idx ? { ...opt, label: newLabel } : opt
    );
    setCoreOptions((prev) => ({ ...prev, [fieldName]: updated }));
  };

  const handleCoreOptionLabelBlur = (fieldName: string) => {
    saveCoreOptions(fieldName, coreOptions[fieldName] || []);
  };

  const handleCoreOptionAdd = (fieldName: string) => {
    const raw = (newOptionLabel[fieldName] || '').trim();
    if (!raw) return;
    const value = raw.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');
    const opts = coreOptions[fieldName] || [];
    if (opts.some((o) => o.value === value)) {
      setError(`Option "${value}" already exists`);
      return;
    }
    const updated = [...opts, { value, label: raw }];
    setNewOptionLabel((prev) => ({ ...prev, [fieldName]: '' }));
    saveCoreOptions(fieldName, updated);
  };

  const handleCoreOptionDrop = (fieldName: string, toIdx: number) => {
    if (coreDragIdx === null || coreDragIdx === toIdx) return;
    const opts = [...(coreOptions[fieldName] || [])];
    const [moved] = opts.splice(coreDragIdx, 1);
    opts.splice(toIdx, 0, moved);
    saveCoreOptions(fieldName, opts);
    setCoreDragField(null);
    setCoreDragIdx(null);
    setCoreDragOverIdx(null);
  };

  // ── Custom Fields logic ──

  const fetchFields = async () => {
    setLoadingCustom(true);
    try {
      const res = await apiClient.getCustomFieldDefinitions(selectedEntity);
      const sorted = (res.data || []).sort((a: any, b: any) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      setFields(sorted);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load custom fields');
    } finally {
      setLoadingCustom(false);
    }
  };

  const handleEdit = (field: any) => {
    setIsEditing(true);
    setEditId(field.id);
    setShowCustomForm(true);
    const options = field.config?.options ? field.config.options.join(', ') : '';
    const milestones = field.config?.milestones ? field.config.milestones.join(', ') : '';
    setForm({ key: field.key, label: field.label, type: field.type,
      required: field.required, options, milestones, displayInTable: field.displayInTable ?? false });
  };

  const handleCancel = () => {
    setIsEditing(false);
    setEditId(null);
    setShowCustomForm(false);
    setForm({ key: '', label: '', type: 'text', required: false, options: '', milestones: '', displayInTable: false });
    setError('');
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure? This will delete the field definition.')) return;
    try {
      await apiClient.deleteCustomFieldDefinition(id);
      clearCustomCache(selectedEntity);
      fetchFields();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete field');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const config: any = {};
      if ((form.type === 'select' || form.type === 'multiselect') && form.options) {
        config.options = form.options.split(',').map((s) => s.trim()).filter((s) => s);
      }
      if (form.type === 'workflow' && form.milestones) {
        config.milestones = form.milestones.split(',').map((s) => s.trim()).filter((s) => s);
      }
      const payload = {
        entityType: selectedEntity, key: form.key, label: form.label,
        type: form.type, required: form.required, config, displayInTable: form.displayInTable,
      };
      if (isEditing && editId) {
        await apiClient.updateCustomFieldDefinition(editId, {
          label: form.label, required: form.required, config, displayInTable: form.displayInTable,
        });
      } else {
        await apiClient.createCustomFieldDefinition(payload);
      }
      handleCancel();
      clearCustomCache(selectedEntity);
      fetchFields();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to save field');
    }
  };

  const handleCustomDrop = async (targetIdx: number) => {
    if (customDragIdx === null || customDragIdx === targetIdx) {
      setCustomDragIdx(null);
      setCustomDragOverIdx(null);
      return;
    }
    const newFields = [...fields];
    const [removed] = newFields.splice(customDragIdx, 1);
    newFields.splice(targetIdx, 0, removed);
    setFields(newFields);
    setCustomDragIdx(null);
    setCustomDragOverIdx(null);
    try {
      await apiClient.reorderCustomFieldDefinitions(newFields.map((f) => f.id));
      clearCustomCache(selectedEntity);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to reorder fields');
      fetchFields();
    }
  };

  // ── Calculated Fields logic ──

  const fetchCalcFields = async () => {
    setLoadingCalc(true);
    try {
      const res = await apiClient.getCalculatedFieldDefinitions(selectedEntity);
      const sorted = (res.data || []).sort((a: any, b: any) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      setCalcFields(sorted);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load calculated fields');
    } finally {
      setLoadingCalc(false);
    }
  };

  const handleEditCalc = (field: any) => {
    setIsEditingCalc(true);
    setEditCalcId(field.id);
    setShowCalcForm(true);
    setCalcForm({
      key: field.key, label: field.label, returnType: field.returnType,
      expression: field.expression, enabled: field.enabled ?? true, displayInTable: field.displayInTable ?? false,
    });
  };

  const handleCancelCalc = () => {
    setIsEditingCalc(false);
    setEditCalcId(null);
    setShowCalcForm(false);
    setCalcForm({ key: '', label: '', returnType: 'text', expression: '', enabled: true, displayInTable: false });
    setError('');
  };

  const handleDeleteCalc = async (id: number) => {
    if (!window.confirm('Are you sure? This will delete the calculated field definition.')) return;
    try {
      await apiClient.deleteCalculatedFieldDefinition(id);
      clearCalcCache(selectedEntity);
      fetchCalcFields();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete calculated field');
    }
  };

  const handleSubmitCalc = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const payload = {
        entityType: selectedEntity, key: calcForm.key, label: calcForm.label,
        returnType: calcForm.returnType, expression: calcForm.expression, enabled: calcForm.enabled, displayInTable: calcForm.displayInTable,
      };
      if (isEditingCalc && editCalcId) {
        await apiClient.updateCalculatedFieldDefinition(editCalcId, {
          label: calcForm.label, expression: calcForm.expression, enabled: calcForm.enabled, displayInTable: calcForm.displayInTable,
        });
      } else {
        await apiClient.createCalculatedFieldDefinition(payload);
      }
      handleCancelCalc();
      clearCalcCache(selectedEntity);
      fetchCalcFields();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to save calculated field');
    }
  };

  const handleCalcDrop = async (targetIdx: number) => {
    if (calcDragIdx === null || calcDragIdx === targetIdx) {
      setCalcDragIdx(null);
      setCalcDragOverIdx(null);
      return;
    }
    const newFields = [...calcFields];
    const [removed] = newFields.splice(calcDragIdx, 1);
    newFields.splice(targetIdx, 0, removed);
    setCalcFields(newFields);
    setCalcDragIdx(null);
    setCalcDragOverIdx(null);
    try {
      await apiClient.reorderCalculatedFieldDefinitions(newFields.map((f) => f.id));
      clearCalcCache(selectedEntity);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to reorder fields');
      fetchCalcFields();
    }
  };

  // ── Policy Rules logic ──

  const fetchPolicyRules = async () => {
    setLoadingPolicy(true);
    try {
      const res = await apiClient.getPolicyRuleDefinitions(selectedEntity);
      const sorted = (res.data || []).sort((a: any, b: any) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
      setPolicyRules(sorted);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to load policy rules');
    } finally {
      setLoadingPolicy(false);
    }
  };

  const handleEditPolicy = (rule: any) => {
    setIsEditingPolicy(true);
    setEditPolicyId(rule.id);
    setShowPolicyForm(true);
    setPolicyForm({
      name: rule.name, description: rule.description || '', operations: Array.isArray(rule.operations) ? rule.operations : [rule.operations],
      expression: rule.expression, severity: rule.severity, enabled: rule.enabled ?? true,
    });
  };

  const handleCancelPolicy = () => {
    setIsEditingPolicy(false);
    setEditPolicyId(null);
    setShowPolicyForm(false);
    setPolicyForm({ name: '', description: '', operations: [], expression: '', severity: 'DENY', enabled: true });
    setError('');
  };

  const handleDeletePolicy = async (id: number) => {
    if (!window.confirm('Are you sure? This will delete the policy rule.')) return;
    try {
      await apiClient.deletePolicyRule(id);
      clearPolicyCache(selectedEntity);
      fetchPolicyRules();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to delete policy rule');
    }
  };

  const handleSubmitPolicy = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (policyForm.operations.length === 0) {
      setError('Please select at least one operation');
      return;
    }
    try {
      const payload = {
        entityType: selectedEntity, name: policyForm.name, description: policyForm.description,
        operations: policyForm.operations, expression: policyForm.expression,
        severity: policyForm.severity, enabled: policyForm.enabled,
      };
      if (isEditingPolicy && editPolicyId) {
        await apiClient.updatePolicyRule(editPolicyId, {
          name: policyForm.name, description: policyForm.description,
          operations: policyForm.operations, expression: policyForm.expression,
          severity: policyForm.severity, enabled: policyForm.enabled,
        });
      } else {
        await apiClient.createPolicyRule(payload);
      }
      handleCancelPolicy();
      clearPolicyCache(selectedEntity);
      fetchPolicyRules();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to save policy rule');
    }
  };

  const handlePolicyDrop = async (targetIdx: number) => {
    if (policyDragIdx === null || policyDragIdx === targetIdx) {
      setPolicyDragIdx(null);
      setPolicyDragOverIdx(null);
      return;
    }
    const newRules = [...policyRules];
    const [removed] = newRules.splice(policyDragIdx, 1);
    newRules.splice(targetIdx, 0, removed);
    setPolicyRules(newRules);
    setPolicyDragIdx(null);
    setPolicyDragOverIdx(null);
    try {
      await apiClient.reorderPolicyRules(newRules.map((r) => r.id));
      clearPolicyCache(selectedEntity);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to reorder policy rules');
      fetchPolicyRules();
    }
  };

  return (
    <Layout>
      <div className="page">
        <div className="page-header">
          <h1>Data Model</h1>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <div className="tabs">
          {entityTypes.map(({ label, value }) => (
            <button
              key={value}
              className={`tab ${selectedEntity === value ? 'active' : ''}`}
              onClick={() => setSelectedEntity(value)}
            >
              {label}
            </button>
          ))}
        </div>

        {/* ── Core Field Options Section ── */}
        {hasCoreFields && (
          <div className="content-card mt-4">
            <div
              className="flex items-center justify-between cursor-pointer select-none pb-2"
              onClick={() => setCoreOptionsSectionOpen((v: boolean) => !v)}
            >
              <h3 className="text-base font-semibold text-gray-800 dark:text-gray-100 m-0">
                Core Field Options
              </h3>
              <div className="flex items-center gap-3">
                {!coreOptionsSectionOpen && (
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {coreFieldsForEntity.map(f => f.label).join(', ')}
                  </span>
                )}
                <ChevronIcon open={coreOptionsSectionOpen} />
              </div>
            </div>

            {coreOptionsSectionOpen && (
              <>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                  Configure the selectable values for built-in dropdown fields on this entity.
                  Drag to reorder. Changes save automatically.
                </p>
                {loadingCoreOptions ? (
                  <p className="text-sm text-gray-400">Loading...</p>
                ) : (
                  coreFieldsForEntity.map(({ fieldName, label }) => {
                    const opts = coreOptions[fieldName] || [];
                    const saving = coreOptionsSaving[fieldName] ?? false;
                    return (
                      <div key={fieldName} className="mb-6">
                        <div className="flex items-center gap-2 mb-2">
                          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 m-0">{label}</h4>
                          {saving && <span className="text-xs text-gray-400">Saving…</span>}
                        </div>
                        <table className="data-table mb-2">
                          <thead>
                            <tr>
                              <th className="w-8"></th>
                              <th>Label</th>
                              <th>Value (stored in DB)</th>
                              <th>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {opts.length === 0 ? (
                              <tr><td colSpan={4} className="text-gray-400 text-sm">No options configured</td></tr>
                            ) : (
                              opts.map((opt, idx) => (
                                <tr
                                  key={opt.value}
                                  draggable
                                  onDragStart={() => { setCoreDragField(fieldName); setCoreDragIdx(idx); }}
                                  onDragOver={(e) => { e.preventDefault(); setCoreDragOverIdx(idx); }}
                                  onDrop={() => handleCoreOptionDrop(fieldName, idx)}
                                  onDragEnd={() => { setCoreDragField(null); setCoreDragIdx(null); setCoreDragOverIdx(null); }}
                                  className="cursor-grab"
                                  style={{
                                    opacity: coreDragField === fieldName && coreDragIdx === idx ? 0.4 : 1,
                                    outline: coreDragField === fieldName && coreDragOverIdx === idx && coreDragIdx !== idx
                                      ? '2px solid var(--color-primary, #3b82f6)' : undefined,
                                  }}
                                >
                                  <td className="text-center"><DragHandleIcon /></td>
                                  <td>
                                    <input
                                      className="form-input"
                                      value={opt.label}
                                      onChange={(e) => handleCoreOptionLabelChange(fieldName, idx, e.target.value)}
                                      onBlur={() => handleCoreOptionLabelBlur(fieldName)}
                                    />
                                  </td>
                                  <td>
                                    <code className="text-xs text-gray-500 dark:text-gray-400">{opt.value}</code>
                                  </td>
                                  <td>
                                    <button
                                      type="button"
                                      className="btn btn-sm btn-danger"
                                      disabled={opts.length <= 1}
                                      title={opts.length <= 1 ? 'Must keep at least one option' : 'Delete'}
                                      onClick={() => handleCoreOptionDelete(fieldName, idx)}
                                    >
                                      Delete
                                    </button>
                                  </td>
                                </tr>
                              ))
                            )}
                          </tbody>
                        </table>

                        {/* Add new option */}
                        <div className="flex gap-2 items-center">
                          <input
                            className="form-input"
                            style={{ maxWidth: '220px' }}
                            placeholder="New option label…"
                            value={newOptionLabel[fieldName] || ''}
                            onChange={(e) => setNewOptionLabel((prev) => ({ ...prev, [fieldName]: e.target.value }))}
                            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleCoreOptionAdd(fieldName); } }}
                          />
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => handleCoreOptionAdd(fieldName)}
                          >
                            + Add Option
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </>
            )}
          </div>
        )}

        {/* ── Custom Fields Section ── */}
        <div className="content-card mt-4">
          <div
            className="flex items-center justify-between cursor-pointer select-none pb-2"
            onClick={() => setCustomSectionOpen((v: boolean) => !v)}
          >
            <h3 className="text-base font-semibold text-gray-800 dark:text-gray-100 m-0">
              Custom Fields
            </h3>
            <div className="flex items-center gap-3">
              {!customSectionOpen && (
                <span className="text-xs text-gray-500 dark:text-gray-400">{fields.length} field{fields.length !== 1 ? 's' : ''}</span>
              )}
              <ChevronIcon open={customSectionOpen} />
            </div>
          </div>

          {customSectionOpen && (
            <>
              <div className="flex justify-end mb-3">
                {!showCustomForm && (
                  <button className="btn btn-primary btn-sm" onClick={() => setShowCustomForm(true)}>
                    + Add Field
                  </button>
                )}
              </div>

              {showCustomForm && (
                <form onSubmit={handleSubmit} className="form mb-4 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                  <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    {isEditing ? 'Edit Custom Field' : 'New Custom Field'}
                  </h4>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Key (internal name) *</label>
                      <input
                        value={form.key}
                        onChange={(e) => setForm({ ...form, key: e.target.value })}
                        className="form-input"
                        disabled={isEditing}
                        required
                        pattern="[a-zA-Z0-9_]+"
                        title="Alphanumeric and underscore only"
                      />
                    </div>
                    <div className="form-group">
                      <label>Label (display name) *</label>
                      <input
                        value={form.label}
                        onChange={(e) => setForm({ ...form, label: e.target.value })}
                        className="form-input"
                        required
                      />
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label>Type *</label>
                      <select
                        value={form.type}
                        onChange={(e) => setForm({ ...form, type: e.target.value })}
                        className="form-input"
                        disabled={isEditing}
                      >
                        {FIELD_TYPES.map((t) => (
                          <option key={t.value} value={t.value}>{t.label}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group checkbox-group">
                      <label>
                        <input type="checkbox" checked={form.required}
                          onChange={(e) => setForm({ ...form, required: e.target.checked })} />
                        Required
                      </label>
                    </div>
                    <div className="form-group checkbox-group">
                      <label>
                        <input type="checkbox" checked={form.displayInTable}
                          onChange={(e) => setForm({ ...form, displayInTable: e.target.checked })} />
                        Display in Table
                      </label>
                    </div>
                  </div>

                  {(form.type === 'select' || form.type === 'multiselect') && (
                    <div className="form-group">
                      <label>Options (comma separated)</label>
                      <input
                        value={form.options}
                        onChange={(e) => setForm({ ...form, options: e.target.value })}
                        className="form-input"
                        placeholder="Option 1, Option 2, Option 3"
                      />
                    </div>
                  )}

                  {form.type === 'workflow' && (
                    <div className="form-group">
                      <label>Milestones (comma separated) *</label>
                      <input
                        value={form.milestones}
                        onChange={(e) => setForm({ ...form, milestones: e.target.value })}
                        className="form-input"
                        placeholder="Lead, Prospect, Meeting, Proposal, Contract, Won"
                        required
                      />
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                        Enter milestone names in order. Users will progress through them sequentially.
                      </p>
                    </div>
                  )}

                  <div className="form-actions">
                    <button type="submit" className="btn btn-primary">{isEditing ? 'Update' : 'Create'}</button>
                    <button type="button" onClick={handleCancel} className="btn btn-secondary">Cancel</button>
                  </div>
                </form>
              )}

              <table className="data-table">
                <thead>
                  <tr>
                    <th className="w-8"></th>
                    <th>Key</th><th>Label</th><th>Type</th><th>Required</th><th>In Table</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingCustom ? (
                    <tr><td colSpan={7}>Loading...</td></tr>
                  ) : fields.length === 0 ? (
                    <tr><td colSpan={7}>No custom fields defined</td></tr>
                  ) : (
                    fields.map((f: any, index: number) => (
                      <tr
                        key={f.id}
                        draggable
                        onDragStart={() => setCustomDragIdx(index)}
                        onDragOver={(e) => { e.preventDefault(); setCustomDragOverIdx(index); }}
                        onDrop={() => handleCustomDrop(index)}
                        onDragEnd={() => { setCustomDragIdx(null); setCustomDragOverIdx(null); }}
                        className="cursor-grab"
                        style={{
                          opacity: customDragIdx === index ? 0.4 : 1,
                          outline: customDragOverIdx === index && customDragIdx !== index ? '2px solid var(--color-primary, #3b82f6)' : undefined,
                        }}
                      >
                        <td className="text-center">
                          <DragHandleIcon />
                        </td>
                        <td>{f.key}</td>
                        <td>{f.label}</td>
                        <td>{f.type}</td>
                        <td>{f.required ? 'Yes' : 'No'}</td>
                        <td>{f.displayInTable ? 'Yes' : 'No'}</td>
                        <td>
                          <div className="flex gap-2">
                            <button type="button" onClick={() => handleEdit(f)} className="btn btn-sm btn-secondary">Edit</button>
                            <button type="button" onClick={() => handleDelete(f.id)} className="btn btn-sm btn-danger">Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>

        {/* ── Calculated Fields Section ── */}
        <div className="content-card mt-4">
          <div
            className="flex items-center justify-between cursor-pointer select-none pb-2"
            onClick={() => setCalcSectionOpen((v: boolean) => !v)}
          >
            <h3 className="text-base font-semibold text-gray-800 dark:text-gray-100 m-0">
              Insights
            </h3>
            <div className="flex items-center gap-3">
              {!calcSectionOpen && (
                <span className="text-xs text-gray-500 dark:text-gray-400">{calcFields.length} field{calcFields.length !== 1 ? 's' : ''}</span>
              )}
              <ChevronIcon open={calcSectionOpen} />
            </div>
          </div>

          {calcSectionOpen && (
            <>
              <div className="flex justify-end mb-3">
                {!showCalcForm && (
                  <button className="btn btn-primary btn-sm" onClick={() => setShowCalcForm(true)}>
                    + Add Insight
                  </button>
                )}
              </div>

              {showCalcForm && (
                <form onSubmit={handleSubmitCalc} className="form mb-4 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                  <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    {isEditingCalc ? 'Edit Insight' : 'New Insight'}
                  </h4>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Key (internal name) *</label>
                      <input
                        value={calcForm.key}
                        onChange={(e) => setCalcForm({ ...calcForm, key: e.target.value })}
                        className="form-input"
                        disabled={isEditingCalc}
                        required
                        pattern="[a-zA-Z0-9_]+"
                        title="Alphanumeric and underscore only"
                      />
                    </div>
                    <div className="form-group">
                      <label>Label (display name) *</label>
                      <input
                        value={calcForm.label}
                        onChange={(e) => setCalcForm({ ...calcForm, label: e.target.value })}
                        className="form-input"
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Return Type *</label>
                      <select
                        value={calcForm.returnType}
                        onChange={(e) => setCalcForm({ ...calcForm, returnType: e.target.value })}
                        className="form-input"
                        disabled={isEditingCalc}
                      >
                        {RETURN_TYPES.map((t) => (
                          <option key={t.value} value={t.value}>{t.label}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Expression *</label>
                    <textarea
                      value={calcForm.expression}
                      onChange={(e) => setCalcForm({ ...calcForm, expression: e.target.value })}
                      className="form-input font-mono"
                      required
                      rows={3}
                      placeholder="e.g. value * probability / 100"
                    />
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Available variables: entity fields by name (e.g. <code>name</code>, <code>value</code>, <code>status</code>) and custom fields as <code>customField_key</code>. Both <code>fieldName</code> and <code>#fieldName</code> syntax work.
                    </p>
                  </div>

                  <div className="form-group checkbox-group">
                    <label>
                      <input
                        type="checkbox"
                        checked={calcForm.enabled}
                        onChange={(e) => setCalcForm({ ...calcForm, enabled: e.target.checked })}
                      />
                      Enabled
                    </label>
                  </div>

                  <div className="form-group checkbox-group">
                    <label>
                      <input type="checkbox" checked={calcForm.displayInTable}
                        onChange={(e) => setCalcForm({ ...calcForm, displayInTable: e.target.checked })} />
                      Display in Table
                    </label>
                  </div>

                  <div className="form-actions">
                    <button type="submit" className="btn btn-primary">{isEditingCalc ? 'Update' : 'Create'}</button>
                    <button type="button" onClick={handleCancelCalc} className="btn btn-secondary">Cancel</button>
                  </div>
                </form>
              )}

              <table className="data-table">
                <thead>
                  <tr>
                    <th className="w-8"></th>
                    <th>Key</th><th>Label</th><th>Return Type</th><th>Expression</th><th>Enabled</th><th>Display in Table</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingCalc ? (
                    <tr><td colSpan={8}>Loading...</td></tr>
                  ) : calcFields.length === 0 ? (
                    <tr><td colSpan={8}>No insights defined</td></tr>
                  ) : (
                    calcFields.map((f: any, index: number) => (
                      <tr
                        key={f.id}
                        draggable
                        onDragStart={() => setCalcDragIdx(index)}
                        onDragOver={(e) => { e.preventDefault(); setCalcDragOverIdx(index); }}
                        onDrop={() => handleCalcDrop(index)}
                        onDragEnd={() => { setCalcDragIdx(null); setCalcDragOverIdx(null); }}
                        className="cursor-grab"
                        style={{
                          opacity: calcDragIdx === index ? 0.4 : 1,
                          outline: calcDragOverIdx === index && calcDragIdx !== index ? '2px solid var(--color-primary, #3b82f6)' : undefined,
                        }}
                      >
                        <td className="text-center">
                          <DragHandleIcon />
                        </td>
                        <td>{f.key}</td>
                        <td>{f.label}</td>
                        <td>{f.returnType}</td>
                        <td>
                          <code className="text-xs bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded">
                            {f.expression.length > 40 ? f.expression.slice(0, 40) + '…' : f.expression}
                          </code>
                        </td>
                        <td>{f.enabled ? 'Yes' : 'No'}</td>
                        <td>{f.displayInTable ? 'Yes' : 'No'}</td>
                        <td>
                          <div className="flex gap-2">
                            <button type="button" onClick={() => handleEditCalc(f)} className="btn btn-sm btn-secondary">Edit</button>
                            <button type="button" onClick={() => handleDeleteCalc(f.id)} className="btn btn-sm btn-danger">Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>

        {/* ── Policy Rules Section ── */}
        <div className="content-card mt-4">
          <div
            className="flex items-center justify-between cursor-pointer select-none pb-2"
            onClick={() => setPolicySectionOpen((v: boolean) => !v)}
          >
            <h3 className="text-base font-semibold text-gray-800 dark:text-gray-100 m-0">
              Business Rules (Policies)
            </h3>
            <div className="flex items-center gap-3">
              {!policySectionOpen && (
                <span className="text-xs text-gray-500 dark:text-gray-400">{policyRules.length} rule{policyRules.length !== 1 ? 's' : ''}</span>
              )}
              <ChevronIcon open={policySectionOpen} />
            </div>
          </div>

          {policySectionOpen && (
            <>
              <div className="flex justify-end mb-3">
                {!showPolicyForm && (
                  <button className="btn btn-primary btn-sm" onClick={() => setShowPolicyForm(true)}>
                    + Add Rule
                  </button>
                )}
              </div>

              {showPolicyForm && (
                <form onSubmit={handleSubmitPolicy} className="form mb-4 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
                  <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    {isEditingPolicy ? 'Edit Policy Rule' : 'New Policy Rule'}
                  </h4>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Name *</label>
                      <input
                        value={policyForm.name}
                        onChange={(e) => setPolicyForm({ ...policyForm, name: e.target.value })}
                        className="form-input"
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Operations * (select one or more)</label>
                      <div className="flex gap-4 mt-2">
                        {OPERATIONS.map((o) => (
                          <label key={o.value} className="flex items-center gap-1.5 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={policyForm.operations.includes(o.value)}
                              onChange={(e) => {
                                if (e.target.checked) {
                                  setPolicyForm({ ...policyForm, operations: [...policyForm.operations, o.value] });
                                } else {
                                  setPolicyForm({ ...policyForm, operations: policyForm.operations.filter(op => op !== o.value) });
                                }
                              }}
                              disabled={isEditingPolicy}
                            />
                            {o.label}
                          </label>
                        ))}
                      </div>
                      {policyForm.operations.length === 0 && (
                        <p className="text-xs text-red-600 dark:text-red-400 mt-1">Select at least one operation</p>
                      )}
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label>Severity *</label>
                      <select
                        value={policyForm.severity}
                        onChange={(e) => setPolicyForm({ ...policyForm, severity: e.target.value })}
                        className="form-input"
                        required
                      >
                        {SEVERITIES.map((s) => (
                          <option key={s.value} value={s.value}>{s.label}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group checkbox-group">
                      <label>
                        <input
                          type="checkbox"
                          checked={policyForm.enabled}
                          onChange={(e) => setPolicyForm({ ...policyForm, enabled: e.target.checked })}
                        />
                        Enabled
                      </label>
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Description</label>
                    <textarea
                      value={policyForm.description}
                      onChange={(e) => setPolicyForm({ ...policyForm, description: e.target.value })}
                      className="form-input"
                      rows={2}
                      placeholder="Optional description of this rule"
                    />
                  </div>

                  <div className="form-group">
                    <label>Expression (Rego) *</label>
                    <textarea
                      value={policyForm.expression}
                      onChange={(e) => setPolicyForm({ ...policyForm, expression: e.target.value })}
                      className="form-input font-mono"
                      required
                      rows={3}
                      placeholder={'e.g. input.entity.status == "locked"'}
                    />
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Rego condition body that evaluates to <code>true</code> when the rule is triggered.
                      Available: <code>input.entity.*</code> (new/merged state), <code>input.previous.*</code> (old state, UPDATE only), <code>input.operation</code>.
                      Multiple conditions on separate lines are ANDed; create two rules for OR logic.
                    </p>
                  </div>

                  <div className="form-actions">
                    <button type="submit" className="btn btn-primary">{isEditingPolicy ? 'Update' : 'Create'}</button>
                    <button type="button" onClick={handleCancelPolicy} className="btn btn-secondary">Cancel</button>
                  </div>
                </form>
              )}

              <table className="data-table">
                <thead>
                  <tr>
                    <th className="w-8"></th>
                    <th>Name</th><th>Operations</th><th>Severity</th><th>Expression</th><th>Enabled</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingPolicy ? (
                    <tr><td colSpan={7}>Loading...</td></tr>
                  ) : policyRules.length === 0 ? (
                    <tr><td colSpan={7}>No policy rules defined</td></tr>
                  ) : (
                    policyRules.map((r: any, index: number) => (
                      <tr
                        key={r.id}
                        draggable
                        onDragStart={() => setPolicyDragIdx(index)}
                        onDragOver={(e) => { e.preventDefault(); setPolicyDragOverIdx(index); }}
                        onDrop={() => handlePolicyDrop(index)}
                        onDragEnd={() => { setPolicyDragIdx(null); setPolicyDragOverIdx(null); }}
                        className="cursor-grab"
                        style={{
                          opacity: policyDragIdx === index ? 0.4 : 1,
                          outline: policyDragOverIdx === index && policyDragIdx !== index ? '2px solid var(--color-primary, #3b82f6)' : undefined,
                        }}
                      >
                        <td className="text-center">
                          <DragHandleIcon />
                        </td>
                        <td>{r.name}</td>
                        <td>{Array.isArray(r.operations) ? r.operations.join(', ') : r.operations}</td>
                        <td>
                          <span className={r.severity === 'DENY' ? 'font-bold text-red-600 dark:text-red-400' : 'text-amber-500 dark:text-amber-400'}>
                            {r.severity}
                          </span>
                        </td>
                        <td>
                          <code className="text-xs bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded">
                            {r.expression.length > 40 ? r.expression.slice(0, 40) + '…' : r.expression}
                          </code>
                        </td>
                        <td>{r.enabled ? 'Yes' : 'No'}</td>
                        <td>
                          <div className="flex gap-2">
                            <button type="button" onClick={() => handleEditPolicy(r)} className="btn btn-sm btn-secondary">Edit</button>
                            <button type="button" onClick={() => handleDeletePolicy(r.id)} className="btn btn-sm btn-danger">Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>
    </Layout>
  );
};
