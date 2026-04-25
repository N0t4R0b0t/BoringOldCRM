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
/**
 * @file Zustand store for the AI-assisted report builder state.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { create } from 'zustand';

interface ReportBuilderState {
  entityType: string;
  entityId: number | null;
  entityLabel: string;
  reportType: 'slide_deck' | 'one_pager';
  templateId: number | null;
  title: string;

  layout: 'dark' | 'light' | 'corporate' | 'minimal';
  accentColor: string;
  logoPlacement: 'none' | 'title' | 'header' | 'footer';
  includeFields: string[];
  excludeFields: string[];
  // Free-form style keys the assistant can set (backgroundColor, slideBackground,
  // textColor, h1Color, h2Color, fontFamily, customCss, etc.). Merged into styleJson
  // after the explicit fields above. Cleared when the user picks a new layout preset.
  styleOverrides: Record<string, unknown>;

  previewContent: string | null;
  previewMimeType: string | null;
  previewLoading: boolean;
  previewError: string | null;

  saveTemplateModalOpen: boolean;
  saveTemplateName: string;
  saveTemplateDescription: string;

  setEntityType: (v: string) => void;
  setEntityId: (id: number | null, label: string) => void;
  setReportType: (v: 'slide_deck' | 'one_pager') => void;
  setTemplateId: (v: number | null) => void;
  setTitle: (v: string) => void;
  setLayout: (v: 'dark' | 'light' | 'corporate' | 'minimal') => void;
  setAccentColor: (v: string) => void;
  setLogoPlacement: (v: 'none' | 'title' | 'header' | 'footer') => void;
  setIncludeFields: (v: string[]) => void;
  setExcludeFields: (v: string[]) => void;
  setStyleOverrides: (v: Record<string, unknown>) => void;
  mergeStyleOverrides: (v: Record<string, unknown>) => void;
  setPreview: (content: string, mimeType: string) => void;
  setPreviewLoading: (v: boolean) => void;
  setPreviewError: (v: string | null) => void;
  setSaveTemplateModalOpen: (v: boolean) => void;
  setSaveTemplateName: (v: string) => void;
  setSaveTemplateDescription: (v: string) => void;
  buildStyleJson: () => string;
}

export const useReportBuilderStore = create<ReportBuilderState>((set, get) => ({
  entityType: 'Customer',
  entityId: null,
  entityLabel: '',
  reportType: 'slide_deck',
  templateId: null,
  title: '',
  layout: 'dark',
  accentColor: '#533483',
  logoPlacement: 'title',
  includeFields: [],
  excludeFields: [],
  styleOverrides: {},
  previewContent: null,
  previewMimeType: null,
  previewLoading: false,
  previewError: null,
  saveTemplateModalOpen: false,
  saveTemplateName: '',
  saveTemplateDescription: '',

  setEntityType: (v) => set({ entityType: v, entityId: null, entityLabel: '' }),
  setEntityId: (id, label) => set({ entityId: id, entityLabel: label }),
  setReportType: (v) => set({ reportType: v }),
  setTemplateId: (v) => set({ templateId: v }),
  setTitle: (v) => set({ title: v }),
  // Picking a preset resets free-form overrides so the theme colors take effect cleanly
  setLayout: (v) => set({ layout: v, styleOverrides: {} }),
  setAccentColor: (v) => set({ accentColor: v }),
  setLogoPlacement: (v) => set({ logoPlacement: v }),
  setIncludeFields: (v) => set({ includeFields: v }),
  setExcludeFields: (v) => set({ excludeFields: v }),
  setStyleOverrides: (v) => set({ styleOverrides: v }),
  mergeStyleOverrides: (v) => set(state => ({ styleOverrides: { ...state.styleOverrides, ...v } })),
  // setPreview always clears the loading state so the spinner never gets stuck
  setPreview: (content, mimeType) => set({ previewContent: content, previewMimeType: mimeType, previewError: null, previewLoading: false }),
  setPreviewLoading: (v) => set({ previewLoading: v }),
  setPreviewError: (v) => set({ previewError: v, previewLoading: false }),
  setSaveTemplateModalOpen: (v) => set({ saveTemplateModalOpen: v }),
  setSaveTemplateName: (v) => set({ saveTemplateName: v }),
  setSaveTemplateDescription: (v) => set({ saveTemplateDescription: v }),

  buildStyleJson: () => {
    const { layout, accentColor, logoPlacement, includeFields, excludeFields, styleOverrides } = get();
    // Explicit UI fields first, then free-form overrides (which can override them).
    const obj: Record<string, unknown> = { layout, accentColor, logoPlacement, ...styleOverrides };
    if (includeFields.length > 0) obj.includeFields = includeFields;
    if (excludeFields.length > 0) obj.excludeFields = excludeFields;
    return JSON.stringify(obj);
  },
}));
