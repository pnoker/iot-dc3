/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
