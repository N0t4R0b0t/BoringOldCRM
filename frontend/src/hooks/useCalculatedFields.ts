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
 * @file Hook that fetches and subscribes to calculated field values for a single entity.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import { useEffect } from 'react';
import { useCalculatedFieldsStore } from '../store/calculatedFieldsStore';

export const useCalculatedFields = (entityType: string) => {
  const { fetchCalculatedFields, getFieldsByEntityType } = useCalculatedFieldsStore();

  useEffect(() => {
    fetchCalculatedFields(entityType);
  }, [entityType]);

  const fields = getFieldsByEntityType(entityType);
  const isLoading = useCalculatedFieldsStore((state) => state.isLoading[entityType] ?? false);
  const error = useCalculatedFieldsStore((state) => state.error[entityType] ?? null);

  return { fields, isLoading, error };
};
