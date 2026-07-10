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

export interface PagedRow {
  id: string;
  name: string;
  createTime: string;
}

/**
 * Generates a deterministic list of paged rows for usePagedList /
 * usePagedList-style table tests. The default 25 produces exactly two
 * full pages of 12 plus a 1-row tail — exercises start/middle/end
 * paging math without ceremony.
 */
export function pagedRows(count = 25): PagedRow[] {
  return Array.from({length: count}, (_, i) => ({
    id: `id-${i + 1}`,
    name: `Row ${String(i + 1).padStart(2, '0')}`,
    createTime: `2026-05-${String((i % 28) + 1).padStart(2, '0')}`,
  }));
}
