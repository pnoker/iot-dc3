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

interface PageInput {
  offset: number;
  limit: number;
}

/** Read pagination from a PageQuery body, defaulting to the usePagedList norms. */
export const readPage = (body: any): PageInput => ({
  offset: Math.max(0, Number(body?.offset ?? 0)),
  limit: Math.min(200, Math.max(1, Number(body?.limit ?? 12))),
});

/** Case-insensitive substring match, the common driver/device/profile/point search. */
export const matches = (value: unknown, term: unknown): boolean => {
  if (term === undefined || term === null || term === '') return true;
  return String(value ?? '')
    .toLowerCase()
    .includes(String(term).toLowerCase());
};

/**
 * Mimic server-side pagination + filtering. Returns the PageResult shape
 * ({items, offset, limit, total, hasNext}) that usePagedList reads from response.
 */
export const paginate = <T extends Record<string, any>>(
  rows: T[],
  body: any,
  filter?: (row: T, body: any) => boolean,
): { items: T[]; offset: number; limit: number; total: number; hasNext: boolean } => {
  const {offset, limit} = readPage(body);
  const filtered = filter ? rows.filter((row) => filter(row, body)) : rows;
  const items = filtered.slice(offset, offset + limit);
  return {items, offset, limit, total: filtered.length, hasNext: offset + items.length < filtered.length};
};
