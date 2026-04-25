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
import type { CustomField } from '../store/customFieldsStore';
import { CustomFieldInput } from './CustomFieldInput';

interface CustomFieldFormSectionProps {
  fields: CustomField[];
  values: Record<string, any>;
  onChange: (fieldName: string, value: any) => void;
  errors?: Record<string, string>;
  isLoading?: boolean;
  entityType?: string;
  entityId?: number;
}

export const CustomFieldFormSection = ({
  fields,
  values,
  onChange,
  errors = {},
  isLoading = false,
  entityType,
  entityId,
}: CustomFieldFormSectionProps) => {

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-4 bg-gray-50 dark:bg-gray-900 rounded-lg border border-gray-100 dark:border-gray-700">
        <div className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-2">
          <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          Loading custom fields...
        </div>
      </div>
    );
  }

  if (fields.length === 0) {
    return null;
  }

  return (
    <div className="mt-6 bg-gray-50 dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gray-100/50 dark:bg-gray-800 flex items-center gap-2">
        <span className="text-lg">📋</span>
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200 uppercase tracking-wide">Additional Information</h3>
      </div>
      <div className="p-4 grid gap-4" style={{ gridTemplateColumns: fields.length > 2 ? 'repeat(2, minmax(0, 1fr))' : '1fr' }}>
        {fields.map((field) => (
          <div key={field.id} className={field.type === 'richtext' || field.type === 'textarea' || field.type === 'document' || field.type === 'document_multi' || field.type === 'workflow' ? 'col-span-2' : ''}>
            <CustomFieldInput
              field={field}
              value={values[field.key]}
              onChange={(value) => onChange(field.key, value)}
              error={errors[field.key]}
              entityType={entityType}
              entityId={entityId}
            />
          </div>
        ))}
      </div>
    </div>
  );
};
