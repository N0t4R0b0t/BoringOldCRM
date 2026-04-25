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
import React from 'react';
import type { CustomField } from '../store/customFieldsStore';

export interface WorkflowValue {
  currentIndex: number | null;
}

interface WorkflowFieldProps {
  field: CustomField;
  value: WorkflowValue | null;
  onChange: (value: WorkflowValue) => void;
  disabled?: boolean;
  error?: string;
}

export const WorkflowField: React.FC<WorkflowFieldProps> = ({
  field,
  value,
  onChange,
  disabled = false,
  error,
}) => {
  const milestones = field.config?.milestones || [];
  const currentIndex = value?.currentIndex ?? null;

  if (!milestones || milestones.length === 0) {
    return (
      <div className="mb-1">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          {field.label}{field.required && <span className="text-red-500 ml-1">*</span>}
        </label>
        <div className="text-xs text-gray-400 dark:text-gray-500 italic">No milestones configured</div>
      </div>
    );
  }

  const getStepState = (idx: number): 'completed' | 'active' | 'upcoming' => {
    if (currentIndex === null) return 'upcoming';
    if (idx < currentIndex) return 'completed';
    if (idx === currentIndex) return 'active';
    return 'upcoming';
  };

  const isLineActive = (lineBeforeIdx: number): boolean =>
    currentIndex !== null && currentIndex > lineBeforeIdx;

  const handleClick = (idx: number) => {
    if (disabled) return;
    onChange({ currentIndex: currentIndex === idx ? null : idx });
  };

  const completedCount = currentIndex !== null ? currentIndex : 0;

  return (
    <div className="mb-1">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          {field.label}{field.required && <span className="text-red-500 ml-1">*</span>}
        </label>
        <div className="flex items-center gap-2">
          {currentIndex !== null ? (
            <>
              <span className="text-xs text-gray-500 dark:text-gray-400">
                {completedCount}/{milestones.length - 1} completed
              </span>
              <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-700 dark:text-blue-300 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 px-2.5 py-1 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse flex-shrink-0" />
                {milestones[currentIndex]}
              </span>
            </>
          ) : (
            <span className="text-xs text-gray-400 dark:text-gray-500 italic">Not started</span>
          )}
        </div>
      </div>

      {/* Stepper card */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 shadow-sm overflow-x-auto">
        <div className="min-w-max">
          {/* Circles + connector lines */}
          <div className="flex items-center">
            {milestones.map((m: string, i: number) => {
              const state = getStepState(i);
              return [
                i > 0 ? (
                  <div
                    key={`line-${i}`}
                    className={`flex-1 min-w-[28px] h-[3px] mx-1 rounded-full transition-all duration-500 ${
                      isLineActive(i - 1)
                        ? 'bg-gradient-to-r from-emerald-400 to-emerald-500'
                        : 'bg-gray-200 dark:bg-gray-600'
                    }`}
                  />
                ) : null,
                <button
                  key={`step-${i}`}
                  type="button"
                  onClick={() => handleClick(i)}
                  disabled={disabled}
                  title={
                    disabled
                      ? m
                      : currentIndex === i
                      ? `Reset "${m}"`
                      : `Activate "${m}"`
                  }
                  className={[
                    'relative flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center',
                    'border-2 transition-all duration-300 select-none outline-none',
                    'focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-blue-500',
                    state === 'completed'
                      ? 'bg-emerald-500 border-emerald-500 text-white shadow-md shadow-emerald-200 dark:shadow-emerald-900/40'
                      : state === 'active'
                      ? 'bg-blue-600 border-blue-500 text-white shadow-xl shadow-blue-300 dark:shadow-blue-900/70 scale-110'
                      : 'bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-500 text-gray-400 dark:text-gray-400',
                    !disabled
                      ? 'cursor-pointer hover:scale-105'
                      : 'cursor-default',
                    !disabled && state === 'upcoming'
                      ? 'hover:border-blue-400 hover:text-blue-500 dark:hover:border-blue-500 dark:hover:text-blue-400'
                      : '',
                    !disabled && state === 'completed'
                      ? 'hover:brightness-110'
                      : '',
                  ].join(' ')}
                >
                  {state === 'active' && (
                    <span className="absolute inset-0 rounded-full animate-ping bg-blue-400 opacity-25" />
                  )}
                  {state === 'completed' ? (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : state === 'active' ? (
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                      <circle cx="12" cy="12" r="6" />
                    </svg>
                  ) : (
                    <span className="text-xs font-bold">{i + 1}</span>
                  )}
                </button>,
              ];
            })}
          </div>

          {/* Labels row */}
          <div className="flex items-start mt-2.5">
            {milestones.map((m: string, i: number) => {
              const state = getStepState(i);
              return [
                i > 0 ? (
                  <div key={`gap-${i}`} className="flex-1 min-w-[28px] mx-1" />
                ) : null,
                <div
                  key={`label-${i}`}
                  className={[
                    'flex-shrink-0 w-10 text-center text-[10px] leading-tight font-medium transition-colors duration-300',
                    state === 'active'
                      ? 'text-blue-600 dark:text-blue-400'
                      : state === 'completed'
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : 'text-gray-400 dark:text-gray-500',
                  ].join(' ')}
                  style={{ wordBreak: 'break-word' }}
                  title={m}
                >
                  {m}
                </div>,
              ];
            })}
          </div>
        </div>
      </div>

      {!disabled && (
        <p className="mt-1.5 text-xs text-gray-400 dark:text-gray-500">
          Click a milestone to activate · Click active to reset
        </p>
      )}
      {error && <span className="mt-1 text-xs text-red-600 dark:text-red-400">{error}</span>}
    </div>
  );
};
