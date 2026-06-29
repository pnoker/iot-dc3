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

import {describe, expect, it, vi} from 'vitest';

import {usePagedList} from '@/composables/usePagedList';

import {type PagedRow as Row, pagedRows} from '../fixtures/rows';

// 25 produces exactly two full pages of 12 plus a 1-row tail — exercises
// start/middle/end paging math without ceremony.
const TOTAL_ROWS = 25;
const sampleRows = pagedRows(TOTAL_ROWS);

describe('usePagedList', () => {
  it('uses the default page size of 12 and slices allData accordingly', () => {
    const {state, setAllData} = usePagedList<Row>();
    setAllData(sampleRows);

    expect(state.page.size).toBe(12);
    expect(state.page.total).toBe(25);
    expect(state.listData).toHaveLength(12);
    expect(state.listData[0]).toEqual(sampleRows[0]);
    expect(state.listData[11]).toEqual(sampleRows[11]);
  });

  it('honors the configured pageSize option', () => {
    const {state, setAllData} = usePagedList<Row>({pageSize: 5});
    setAllData(sampleRows);

    expect(state.page.size).toBe(5);
    expect(state.listData).toHaveLength(5);
  });

  it('applies the optional filter on every refresh', () => {
    const {state, setAllData, search} = usePagedList<Row, {keyword?: string}>({
      filter: (rows, query) => (query.keyword ? rows.filter((r) => r.name.includes(query.keyword!)) : rows),
    });
    setAllData(sampleRows);

    search({keyword: 'Row 0'}); // matches Row 01..09
    expect(state.listData.every((r) => r.name.startsWith('Row 0'))).toBe(true);
    expect(state.page.total).toBe(9);
    expect(state.page.current).toBe(1);
  });

  it('reset clears the query and resets the page cursor', () => {
    const {state, setAllData, search, reset} = usePagedList<Row, {keyword?: string}>({
      filter: (rows, query) => (query.keyword ? rows.filter((r) => r.name.includes(query.keyword!)) : rows),
    });
    setAllData(sampleRows);

    search({keyword: 'Row 11'});
    expect(state.page.total).toBe(1);

    reset();
    expect(state.query).toEqual({});
    expect(state.page.total).toBe(25);
    expect(state.page.current).toBe(1);
  });

  it('toggles sort order and applies sortValue', () => {
    const {state, setAllData, sort} = usePagedList<Row>({
      sortColumn: 'name',
      sortValue: (r) => r.name,
    });
    setAllData(sampleRows);

    sort();
    expect(state.order).toBe(true);
    expect(state.page.orders).toEqual([{column: 'name', asc: true}]);
    // ascending — first listData item is Row 01.
    expect(state.listData[0].name).toBe('Row 01');

    sort();
    expect(state.order).toBe(false);
    // descending — first item is Row 25.
    expect(state.listData[0].name).toBe('Row 25');
  });

  it('uses default sortColumn=create_time when not provided', () => {
    const {state, setAllData, sort} = usePagedList<Row>();
    setAllData(sampleRows);

    sort();
    expect(state.page.orders).toEqual([{column: 'create_time', asc: true}]);
  });

  it('sizeChange resets page to 1 and re-slices', () => {
    const {state, setAllData, currentChange, sizeChange} = usePagedList<Row>();
    setAllData(sampleRows);

    currentChange(2);
    expect(state.page.current).toBe(2);
    // page 2 of size 12 → rows 13..24
    expect(state.listData[0]).toEqual(sampleRows[12]);

    sizeChange(5);
    // size change forces page back to 1 to avoid showing an empty slice.
    expect(state.page.current).toBe(1);
    expect(state.page.size).toBe(5);
    expect(state.listData).toHaveLength(5);
    expect(state.listData[0]).toEqual(sampleRows[0]);
  });

  it('currentChange jumps to the requested page', () => {
    const {state, setAllData, currentChange} = usePagedList<Row>({pageSize: 10});
    setAllData(sampleRows);

    currentChange(3);
    // page 3 of size 10 → only 5 remaining rows (21..25)
    expect(state.page.current).toBe(3);
    expect(state.listData).toHaveLength(5);
    expect(state.listData[0]).toEqual(sampleRows[20]);
  });

  it('withLoading toggles loading and propagates errors', async () => {
    const {state, withLoading} = usePagedList<Row>();

    const loadingDuringHandler: boolean[] = [];
    await withLoading(async () => {
      loadingDuringHandler.push(state.loading);
    });

    expect(loadingDuringHandler).toEqual([true]);
    expect(state.loading).toBe(false);

    // Errors must not leave loading stuck on true.
    await expect(
      withLoading(async () => {
        throw new Error('boom');
      })
    ).rejects.toThrow('boom');
    expect(state.loading).toBe(false);
  });

  it('loads server-paginated rows when a request handler is provided', async () => {
    const request = vi.fn(() =>
      Promise.resolve({
        ok: true,
        code: 'ok',
        message: 'ok',
        data: {records: sampleRows.slice(0, 2), total: sampleRows.length},
      })
    );
    const {state, load, search, sort, sizeChange, currentChange} = usePagedList<Row, {keyword?: string}>({
      request,
    });

    await load();
    expect(state.listData).toEqual(sampleRows.slice(0, 2));
    expect(state.page.total).toBe(sampleRows.length);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({page: state.page}));

    search({keyword: 'Row 02'});
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({keyword: 'Row 02'}));

    sort();
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(
      expect.objectContaining({page: expect.objectContaining({orders: [{column: 'create_time', asc: true}]})})
    );

    sizeChange(24);
    await Promise.resolve();
    expect(state.page.current).toBe(1);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({page: expect.objectContaining({size: 24})}));

    currentChange(2);
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({page: expect.objectContaining({current: 2})}));
  });
});
