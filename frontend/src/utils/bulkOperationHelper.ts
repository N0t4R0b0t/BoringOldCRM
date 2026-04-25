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
 * Helper utilities for bulk operations to prevent timeouts on very large requests.
 * Recommends chunking and provides safe batch sizes.
 */

export interface BulkRecord {
  [key: string]: any;
}

export const BULK_OPERATION_LIMITS = {
  recommendedChunkSize: 100,      // Process in chunks of 100 for optimal performance
  warningThreshold: 200,          // Warn user if sending more than this
  absoluteMax: 1000,              // Hard limit per request
};

/**
 * Check if a bulk operation is too large and should be chunked.
 * Returns { shouldChunk: boolean, recommendedSize: number, message: string }
 */
export function validateBulkSize(
  records: BulkRecord[],
  operationType: 'customers' | 'contacts' | 'opportunities' | 'activities' | 'customRecords'
) {
  const count = records.length;

  if (count > BULK_OPERATION_LIMITS.absoluteMax) {
    return {
      shouldChunk: true,
      recommendedSize: BULK_OPERATION_LIMITS.recommendedChunkSize,
      message: `⚠️ Cannot send more than ${BULK_OPERATION_LIMITS.absoluteMax} ${operationType} at once. Would you like me to split this into ${Math.ceil(count / BULK_OPERATION_LIMITS.recommendedChunkSize)} batches of ~${BULK_OPERATION_LIMITS.recommendedChunkSize} each?`,
      allowed: false,
    };
  }

  if (count > BULK_OPERATION_LIMITS.warningThreshold) {
    return {
      shouldChunk: true,
      recommendedSize: BULK_OPERATION_LIMITS.recommendedChunkSize,
      message: `⚠️ You're sending ${count} ${operationType}. This might take a while. I'll split this into ${Math.ceil(count / BULK_OPERATION_LIMITS.recommendedChunkSize)} batches for faster processing.`,
      allowed: true,
    };
  }

  return {
    shouldChunk: false,
    recommendedSize: BULK_OPERATION_LIMITS.recommendedChunkSize,
    message: '',
    allowed: true,
  };
}

/**
 * Split a large array of records into chunks for processing.
 */
export function chunkRecords(
  records: BulkRecord[],
  chunkSize: number = BULK_OPERATION_LIMITS.recommendedChunkSize
): BulkRecord[][] {
  const chunks: BulkRecord[][] = [];
  for (let i = 0; i < records.length; i += chunkSize) {
    chunks.push(records.slice(i, i + chunkSize));
  }
  return chunks;
}

/**
 * Estimate request time based on record count.
 * Used to communicate to user why large requests take longer.
 */
export function estimateProcessingTime(recordCount: number): string {
  // Very rough estimate: ~0.5-2 seconds per 100 records depending on complexity
  const chunksNeeded = Math.ceil(recordCount / BULK_OPERATION_LIMITS.recommendedChunkSize);
  const secondsPerChunk = recordCount > 500 ? 5 : recordCount > 200 ? 3 : 2;
  const totalSeconds = chunksNeeded * secondsPerChunk;

  if (totalSeconds < 60) {
    return `~${totalSeconds} seconds`;
  }
  const minutes = Math.ceil(totalSeconds / 60);
  return `~${minutes} minute${minutes > 1 ? 's' : ''}`;
}
