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
import { ValidationError } from '../core/errors.js';

type ManagerResource = Record<string, unknown>;

/**
 * Plain decimal integer lexeme. Numeric coercion alone (Number) accepts '',
 * ' 5', '0x10', '5.0', and '1e2' — the lexical check must come first so a
 * mistyped empty value can never silently become 0 (report F035).
 */
const DECIMAL_INTEGER_PATTERN = /^\d+$/u;

/**
 * Parse a CLI option as a non-negative integer written in plain decimal
 * digits; hex, exponent, decimal-point, padded, and empty forms are rejected
 * before the safe-integer range check.
 * @param value - raw CLI option lexeme to validate as plain decimal digits
 * @returns the parsed non-negative safe integer
 */
export function parseNonNegativeInteger(value: string): number {
  if (!DECIMAL_INTEGER_PATTERN.test(value)) {
    throw new InvalidArgumentError('must be a non-negative integer in plain decimal digits (e.g. 0, 5, 100)');
  }
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0) {
    throw new InvalidArgumentError('must be a non-negative safe integer');
  }
  return parsed;
}

/**
 * Parse a CLI option as a positive integer written in plain decimal digits;
 * hex, exponent, decimal-point, padded, and empty forms are rejected before
 * the safe-integer range check.
 * @param value - raw CLI option lexeme to validate as plain decimal digits
 * @returns the parsed positive safe integer
 */
export function parsePositiveInteger(value: string): number {
  if (!DECIMAL_INTEGER_PATTERN.test(value)) {
    throw new InvalidArgumentError('must be a positive integer in plain decimal digits (e.g. 1, 5, 100)');
  }
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    throw new InvalidArgumentError('must be a positive safe integer');
  }
  return parsed;
}

/**
 * Reject empty or whitespace resource ids before any request is built — an
 * empty id would otherwise travel to the wire as `?id=` (report F053).
 * Exported so id-keyed command paths that do not go through the manager
 * helpers can enforce the same contract. Thrown as a business validation
 * (kind "validation", exit 1), not a commander argv-shape error.
 * @param basePath - manager resource base path, for the error message
 * @param id - resource id to validate
 * @returns the unchanged id
 */
export function requireResourceId(basePath: string, id: string): string {
  if (typeof id !== 'string' || id.trim() === '') {
    throw new ValidationError(
      `resource id for ${basePath} must be a non-empty value, got ${JSON.stringify(id)}`,
    );
  }
  return id;
}

/**
 * Fetch a manager resource by id.
 * @param basePath - manager resource base path (e.g. device)
 * @param id - resource id to fetch
 * @returns the decoded gateway response body for the fetched record
 */
export async function getManagerResource<T = unknown>(basePath: string, id: string): Promise<T> {
  requireResourceId(basePath, id);
  return dc3Client.get<T>(`${basePath}/get_by_id?id=${encodeURIComponent(id)}`);
}

/**
 * Update a manager resource and report the result.
 * @param basePath - manager resource base path (e.g. device)
 * @param id - resource id to update
 * @param expectedVersion - version for optimistic locking
 * @param changes - fields to merge over the current record
 * @returns the decoded gateway response body of the update call
 */
export async function updateManagerResource<T = unknown>(
  basePath: string,
  id: string,
  expectedVersion: number,
  changes: ManagerResource,
): Promise<T> {
  requireResourceId(basePath, id);
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
 * Delete a manager resource. Non-empty 2xx response bodies are returned so
 * the caller can print them (the gateway may attach warnings or audit info);
 * 204 and empty bodies resolve to undefined and stay silent (report F038).
 * @param basePath - manager resource base path (e.g. device)
 * @param id - resource id to delete
 * @param expectedVersion - version for optimistic locking
 * @returns the decoded delete response body, or undefined when empty
 */
export async function deleteManagerResource<T = unknown>(
  basePath: string,
  id: string,
  expectedVersion: number,
): Promise<T> {
  requireResourceId(basePath, id);
  return dc3Client.del<T>(
    `${basePath}/delete?id=${encodeURIComponent(id)}&version=${expectedVersion}`,
  );
}
