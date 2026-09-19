/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { InvalidArgumentError } from 'commander';
import { dc3Client } from '../core/client.js';

type ManagerResource = Record<string, unknown>;

/**
 * Parse a CLI option as a non-negative integer.
 * @param value - value to set
 * @returns the transformed value
 */
export function parseNonNegativeInteger(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0) {
    throw new InvalidArgumentError('must be a non-negative safe integer');
  }
  return parsed;
}

/**
 * Parse a CLI option as a positive integer.
 * @param value - value to set
 * @returns the transformed value
 */
export function parsePositiveInteger(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    throw new InvalidArgumentError('must be a positive safe integer');
  }
  return parsed;
}

/**
 * Update a manager resource and report the result.
 * @param basePath - manager resource base path (e.g. device)
 * @param id - resource id to update
 * @param expectedVersion - version for optimistic locking
 * @param changes - fields to merge over the current record
 * @returns the operation result
 */
export async function updateManagerResource<T = unknown>(
  basePath: string,
  id: string,
  expectedVersion: number,
  changes: ManagerResource,
): Promise<T> {
  const current = await dc3Client.get<ManagerResource>(
    `${basePath}/get_by_id?id=${encodeURIComponent(id)}`,
  );
  return dc3Client.post<T>(`${basePath}/update`, {
    ...current,
    ...changes,
    id,
    version: expectedVersion,
  });
}

/**
 * Delete a manager resource and report the result.
 * @param basePath - manager resource base path (e.g. device)
 * @param id - resource id to delete
 * @param expectedVersion - version for optimistic locking
 */
export async function deleteManagerResource(
  basePath: string,
  id: string,
  expectedVersion: number,
): Promise<void> {
  await dc3Client.del<void>(
    `${basePath}/delete?id=${encodeURIComponent(id)}&version=${expectedVersion}`,
  );
}
