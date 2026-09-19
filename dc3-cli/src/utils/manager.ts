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

export function parseNonNegativeInteger(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0) {
    throw new InvalidArgumentError('must be a non-negative safe integer');
  }
  return parsed;
}

export function parsePositiveInteger(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    throw new InvalidArgumentError('must be a positive safe integer');
  }
  return parsed;
}

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

export async function deleteManagerResource(
  basePath: string,
  id: string,
  expectedVersion: number,
): Promise<void> {
  await dc3Client.del<void>(
    `${basePath}/delete?id=${encodeURIComponent(id)}&version=${expectedVersion}`,
  );
}
