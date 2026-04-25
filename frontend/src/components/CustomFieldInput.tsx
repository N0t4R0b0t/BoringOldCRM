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
import React, { useEffect, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import type { CustomField } from '../store/customFieldsStore';
import { DocumentFieldExplorer } from './DocumentFieldExplorer';
import { CustomRecordFieldPicker } from './CustomRecordFieldPicker';
import { ContactFieldPicker } from './ContactFieldPicker';
import { WorkflowField } from './WorkflowField';

interface CustomFieldInputProps {
  field: CustomField;
  value: any;
  onChange: (value: any) => void;
  error?: string;
  disabled?: boolean;
  entityType?: string;
  entityId?: number;
}

const ToolbarButton = ({ onClick, active, title, children }: {
  onClick: () => void; active?: boolean; title: string; children: React.ReactNode;
}) => (
  <button
    type="button"
    onMouseDown={(e) => { e.preventDefault(); onClick(); }}
    title={title}
    className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
      active
        ? 'bg-blue-100 dark:bg-blue-800 text-blue-700 dark:text-blue-200'
        : 'text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-500'
    }`}
  >
    {children}
  </button>
);

const RichTextEditor = ({ fieldId, value, onChange, disabled, error, label, required }: {
  fieldId: string; value: any; onChange: (v: any) => void;
  disabled?: boolean; error?: string; label: string; required?: boolean;
}) => {
  const isFocused = useRef(false);
  const editor = useEditor({
    extensions: [StarterKit],
    content: value || '',
    editable: !disabled,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
  });

  useEffect(() => {
    if (editor && !isFocused.current && value !== editor.getHTML()) {
      editor.commands.setContent(value || '');
    }
  }, [value, editor]);

  const labelClass = "block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1";
  const errorClass = "mt-1 text-xs text-red-600 dark:text-red-400";

  if (disabled) {
    return (
      <div className="mb-1">
        <label htmlFor={fieldId} className={labelClass}>
          {label}{required && <span className="text-red-500 ml-1">*</span>}
        </label>
        <div
          className="prose prose-sm dark:prose-invert max-w-none text-sm text-gray-900 dark:text-gray-100"
          dangerouslySetInnerHTML={{ __html: value || '' }}
        />
        {error && <span className={errorClass}>{error}</span>}
      </div>
    );
  }

  return (
    <div className="mb-1">
      <label htmlFor={fieldId} className={labelClass}>
        {label}{required && <span className="text-red-500 ml-1">*</span>}
      </label>
      <div
        className={`border rounded-lg overflow-hidden bg-white dark:bg-gray-700 ${error ? 'border-red-300' : 'border-gray-300 dark:border-gray-600'}`}
        onFocus={() => { isFocused.current = true; }}
        onBlur={() => { isFocused.current = false; }}
      >
        {editor && (
          <div className="flex items-center gap-0.5 px-2 py-1 border-b border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-600 flex-wrap">
            <ToolbarButton onClick={() => editor.chain().focus().toggleBold().run()} active={editor.isActive('bold')} title="Bold">
              <strong>B</strong>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleItalic().run()} active={editor.isActive('italic')} title="Italic">
              <em>I</em>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleStrike().run()} active={editor.isActive('strike')} title="Strikethrough">
              <s>S</s>
            </ToolbarButton>
            <div className="w-px h-4 bg-gray-300 dark:bg-gray-500 mx-0.5" />
            <ToolbarButton onClick={() => editor.chain().focus().toggleBulletList().run()} active={editor.isActive('bulletList')} title="Bullet list">
              ≡
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleOrderedList().run()} active={editor.isActive('orderedList')} title="Numbered list">
              1.
            </ToolbarButton>
            <div className="w-px h-4 bg-gray-300 dark:bg-gray-500 mx-0.5" />
            <ToolbarButton onClick={() => editor.chain().focus().toggleBlockquote().run()} active={editor.isActive('blockquote')} title="Quote">
              "
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().setHorizontalRule().run()} active={false} title="Horizontal rule">
              —
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().undo().run()} active={false} title="Undo">
              ↩
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().redo().run()} active={false} title="Redo">
              ↪
            </ToolbarButton>
          </div>
        )}
        <EditorContent
          editor={editor}
          className="min-h-[120px] px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none"
        />
      </div>
      {error && <span className={errorClass}>{error}</span>}
    </div>
  );
};

export const CustomFieldInput = ({
  field,
  value,
  onChange,
  error,
  disabled = false,
  entityType,
  entityId,
}: CustomFieldInputProps) => {
  const fieldId = `custom-field-${field.id}`;

  const handleChange = (newValue: any) => {
    onChange(newValue);
  };

  const labelClass = "block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1";
  const inputClass = `w-full px-3 py-2 bg-white dark:bg-gray-700 dark:text-white dark:placeholder-gray-400 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow ${error ? 'border-red-300 focus:ring-red-200' : ''} ${disabled ? 'opacity-60 cursor-not-allowed bg-gray-50 dark:bg-gray-800' : ''}`;
  const errorClass = "mt-1 text-xs text-red-600 dark:text-red-400";

  switch (field.type) {
    case 'text':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="text"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            placeholder={`Enter ${field.label.toLowerCase()}`}
            className={inputClass}
            required={field.required}
          />
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'number':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="number"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value === '' ? null : parseFloat(e.target.value))}
            disabled={disabled}
            placeholder={`Enter ${field.label.toLowerCase()}`}
            className={inputClass}
            required={field.required}
          />
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'boolean':
    case 'checkbox':
      return (
        <div className="mb-1 flex items-center h-full pt-6">
          <label htmlFor={fieldId} className="flex items-center gap-2 cursor-pointer group">
            <div className="relative flex items-center">
              <input
                id={fieldId}
                type="checkbox"
                checked={value || false}
                onChange={(e) => handleChange(e.target.checked)}
                disabled={disabled}
                className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 dark:border-gray-600 dark:bg-gray-700 transition-all checked:border-blue-600 checked:bg-blue-600 hover:border-blue-400"
              />
              <svg className="pointer-events-none absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 text-white opacity-0 peer-checked:opacity-100 transition-opacity" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" width="12" height="12">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </div>
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300 group-hover:text-gray-900 dark:group-hover:text-gray-100 transition-colors">{field.label}</span>
          </label>
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'select':
    case 'dropdown':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="relative">
            <select
              id={fieldId}
              value={value || ''}
              onChange={(e) => handleChange(e.target.value)}
              disabled={disabled}
              className={`${inputClass} appearance-none`}
              required={field.required}
            >
              <option value="">Select {field.label.toLowerCase()}</option>
              {field.config?.options?.map((option: string) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-gray-500 dark:text-gray-400">
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          </div>
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'multiselect': {
      const selected: string[] = Array.isArray(value) ? value : [];
      return (
        <div className="mb-1">
          <label className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="space-y-1 border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 bg-white dark:bg-gray-700">
            {field.config?.options?.map((opt: string) => (
              <label key={opt} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={selected.includes(opt)}
                  disabled={disabled}
                  onChange={(e) => {
                    const next = e.target.checked
                      ? [...selected, opt]
                      : selected.filter((s) => s !== opt);
                    handleChange(next);
                  }}
                  className="h-4 w-4 rounded border-gray-300 dark:border-gray-600 text-blue-600"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">{opt}</span>
              </label>
            ))}
          </div>
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );
    }

    case 'date':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="date"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            className={inputClass}
            required={field.required}
          />
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'textarea':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <textarea
            id={fieldId}
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            placeholder={`Enter ${field.label.toLowerCase()}`}
            className={inputClass}
            required={field.required}
            rows={3}
          />
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'url':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="url"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            placeholder="https://"
            className={inputClass}
            required={field.required}
          />
          {value && (
            <a
              href={value}
              target="_blank"
              rel="noreferrer"
              className="mt-1 text-xs text-blue-500 hover:underline block"
            >
              Open link ↗
            </a>
          )}
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'email':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="email"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            placeholder="email@example.com"
            className={inputClass}
            required={field.required}
          />
          {value && (
            <a
              href={`mailto:${value}`}
              className="mt-1 text-xs text-blue-500 hover:underline block"
            >
              Send email ↗
            </a>
          )}
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'phone':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <input
            id={fieldId}
            type="tel"
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
            disabled={disabled}
            placeholder="+1 (555) 000-0000"
            className={inputClass}
            required={field.required}
          />
          {value && (
            <a
              href={`tel:${value}`}
              className="mt-1 text-xs text-blue-500 hover:underline block"
            >
              Call ↗
            </a>
          )}
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'currency':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 dark:text-gray-400 text-sm">
              {field.config?.symbol || '$'}
            </span>
            <input
              id={fieldId}
              type="number"
              step="0.01"
              value={value || ''}
              onChange={(e) => handleChange(e.target.value === '' ? null : parseFloat(e.target.value))}
              disabled={disabled}
              placeholder="0.00"
              className={`${inputClass} pl-8`}
              required={field.required}
            />
          </div>
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'percentage':
      return (
        <div className="mb-1">
          <label htmlFor={fieldId} className={labelClass}>
            {field.label}
            {field.required && <span className="text-red-500 ml-1">*</span>}
          </label>
          <div className="relative">
            <input
              id={fieldId}
              type="number"
              min="0"
              max="100"
              step="0.1"
              value={value || ''}
              onChange={(e) => handleChange(e.target.value === '' ? null : parseFloat(e.target.value))}
              disabled={disabled}
              placeholder="0"
              className={`${inputClass} pr-8`}
              required={field.required}
            />
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 dark:text-gray-400 text-sm">
              %
            </span>
          </div>
          {error && <span className={errorClass}>{error}</span>}
        </div>
      );

    case 'richtext':
      return (
        <RichTextEditor
          fieldId={fieldId}
          value={value}
          onChange={handleChange}
          disabled={disabled}
          error={error}
          label={field.label}
          required={field.required}
        />
      );

    case 'document':
      return (
        <DocumentFieldExplorer
          entityType={entityType ?? ''}
          entityId={entityId}
          fieldKey={field.key}
          multiple={false}
          label={field.label}
        />
      );

    case 'document_multi':
      return (
        <DocumentFieldExplorer
          entityType={entityType ?? ''}
          entityId={entityId}
          fieldKey={field.key}
          multiple={true}
          label={field.label}
        />
      );

    case 'custom_record':
      return (
        <CustomRecordFieldPicker
          value={value}
          onChange={handleChange}
          multiple={false}
          label={field.label}
          required={field.required}
          error={error}
        />
      );

    case 'custom_record_multi':
      return (
        <CustomRecordFieldPicker
          value={value}
          onChange={handleChange}
          multiple={true}
          label={field.label}
          required={field.required}
          error={error}
        />
      );

    case 'contact':
      return (
        <ContactFieldPicker
          value={value}
          onChange={handleChange}
          multiple={false}
          label={field.label}
          required={field.required}
          error={error}
          disabled={disabled}
        />
      );

    case 'contact_multi':
      return (
        <ContactFieldPicker
          value={value}
          onChange={handleChange}
          multiple={true}
          label={field.label}
          required={field.required}
          error={error}
          disabled={disabled}
        />
      );

    case 'workflow':
      return (
        <WorkflowField
          field={field}
          value={value}
          onChange={handleChange}
          disabled={disabled}
          error={error}
        />
      );

    default:
      return null;
  }
};
