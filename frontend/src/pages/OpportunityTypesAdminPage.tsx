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
import { Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { usePageTitle } from '../hooks/usePageTitle';

export const OpportunityTypesAdminPage = () => {
  usePageTitle('Opportunity Types');
  const { types, isLoading, fetchTypes, createType, updateType, deleteType } = useOpportunityTypesStore();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '', displayOrder: 0 });
  const [formLoading, setFormLoading] = useState(false);

  useEffect(() => {
    fetchTypes();
  }, []);

  const resetForm = () => {
    setShowForm(false);
    setEditId(null);
    setFormData({ name: '', description: '', displayOrder: 0 });
    setError('');
  };

  const handleEdit = (type: typeof types[0]) => {
    setEditId(type.id);
    setFormData({ name: type.name, description: type.description || '', displayOrder: type.displayOrder });
    setShowForm(true);
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormLoading(true);
    setError('');
    try {
      if (editId !== null) {
        await updateType(editId, { name: formData.name, description: formData.description || undefined, displayOrder: formData.displayOrder });
      } else {
        await createType({ name: formData.name, description: formData.description || undefined });
      }
      setSuccess(editId !== null ? 'Type updated.' : 'Type created.');
      resetForm();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save type');
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete opportunity type "${name}"? This cannot be undone.`)) return;
    setError('');
    try {
      await deleteType(id);
      setSuccess('Type deleted.');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete type. It may still be in use.');
    }
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Opportunity Types</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Define named opportunity categories. Each type gets its own nav entry and custom fields.
            </p>
          </div>
          {!showForm && (
            <button
              onClick={() => { setShowForm(true); setEditId(null); setFormData({ name: '', description: '', displayOrder: 0 }); }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-sm"
            >
              + Add Type
            </button>
          )}
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-300 px-4 py-3 rounded-lg">
            {success}
          </div>
        )}

        {showForm && (
          <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-5 shadow-sm">
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4">
              {editId !== null ? 'Edit Type' : 'New Opportunity Type'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData(p => ({ ...p, name: e.target.value }))}
                  required
                  minLength={2}
                  maxLength={100}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g. Outgoing Opportunities"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={e => setFormData(p => ({ ...p, description: e.target.value }))}
                  maxLength={255}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Optional description"
                />
              </div>
              {editId !== null && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Display Order</label>
                  <input
                    type="number"
                    value={formData.displayOrder}
                    onChange={e => setFormData(p => ({ ...p, displayOrder: parseInt(e.target.value) || 0 }))}
                    className="w-32 px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              )}
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={formLoading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-sm disabled:opacity-50"
                >
                  {formLoading ? 'Saving...' : editId !== null ? 'Update' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 font-medium text-sm"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">Loading...</div>
          ) : types.length === 0 ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">
              No opportunity types defined yet. Create one to segment your pipeline.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-400 uppercase text-xs tracking-wider">
                <tr>
                  <th className="px-4 py-3 text-left">Name</th>
                  <th className="px-4 py-3 text-left">Slug</th>
                  <th className="px-4 py-3 text-left">Description</th>
                  <th className="px-4 py-3 text-left">Order</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {types.map(type => (
                  <tr key={type.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                    <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">{type.name}</td>
                    <td className="px-4 py-3 font-mono text-gray-500 dark:text-gray-400 text-xs">{type.slug}</td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{type.description || '—'}</td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{type.displayOrder}</td>
                    <td className="px-4 py-3 text-right space-x-3">
                      <Link
                        to={`/admin/custom-fields?entityType=Opportunity:${type.slug}`}
                        className="text-purple-600 hover:text-purple-800 dark:text-purple-400 dark:hover:text-purple-200 font-medium"
                      >
                        Fields
                      </Link>
                      <button
                        onClick={() => handleEdit(type)}
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-200 font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(type.id, type.name)}
                        className="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-200 font-medium"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </Layout>
  );
};
