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

import {afterEach, describe, expect, it, vi} from 'vitest';
import {nextTick, ref} from 'vue';

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

  it('clamps the page cursor when setAllData shrinks the source', () => {
    const {state, setAllData, currentChange} = usePagedList<Row>();
    setAllData(sampleRows);
    currentChange(3); // 25 rows / size 12 → page 3 holds the 1-row tail

    setAllData(sampleRows.slice(0, 10));

    expect(state.page.current).toBe(1);
    expect(state.listData).toHaveLength(10);
  });

  it('applies the optional filter on every refresh', () => {
    const {state, setAllData, search} = usePagedList<Row, { keyword?: string }>({
      filter: (rows, query) => (query.keyword ? rows.filter((r) => r.name.includes(query.keyword!)) : rows),
    });
    setAllData(sampleRows);

    search({keyword: 'Row 0'}); // matches Row 01..09
    expect(state.listData.every((r) => r.name.startsWith('Row 0'))).toBe(true);
    expect(state.page.total).toBe(9);
    expect(state.page.current).toBe(1);
  });

  it('reset clears the query and resets the page cursor', () => {
    const {state, setAllData, search, reset} = usePagedList<Row, { keyword?: string }>({
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
    expect(state.sortAsc).toBe(true);
    expect(state.page.orders).toEqual([{column: 'name', asc: true}]);
    // ascending — first listData item is Row 01.
    expect(state.listData[0].name).toBe('Row 01');

    sort();
    expect(state.sortAsc).toBe(false);
    // descending — first item is Row 25.
    expect(state.listData[0].name).toBe('Row 25');
  });

  it('uses default sortColumn=createTime when not provided', () => {
    const {state, setAllData, sort} = usePagedList<Row>();
    setAllData(sampleRows);

    sort();
    expect(state.page.orders).toEqual([{column: 'createTime', asc: true}]);
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
    expect(state.status).toBe('error');
    expect(state.error).toBeInstanceOf(Error);

    await withLoading(async () => undefined);
    expect(state.status).toBe('success');
    expect(state.error).toBeNull();
    expect(state.lastUpdated).not.toBeNull();
  });

  it('keeps the newest withLoading operation authoritative', async () => {
    let resolveFirst!: () => void;
    let resolveSecond!: () => void;
    const first = new Promise<void>((resolve) => (resolveFirst = resolve));
    const second = new Promise<void>((resolve) => (resolveSecond = resolve));
    const {state, withLoading} = usePagedList<Row>();

    const firstRun = withLoading(() => first);
    const secondRun = withLoading(() => second);
    resolveFirst();
    await firstRun;
    expect(state.loading).toBe(true);
    resolveSecond();
    await secondRun;
    expect(state.loading).toBe(false);
    expect(state.status).toBe('success');
  });

  it('loads server-paginated rows when a request handler is provided', async () => {
    const request = vi.fn(() =>
      Promise.resolve({items: sampleRows.slice(0, 2), offset: 0, limit: 2, total: sampleRows.length, hasNext: true})
    );
    const {state, load, search, sort, sizeChange, currentChange} = usePagedList<Row, { keyword?: string }>({
      request,
    });

    await load();
    expect(state.listData).toEqual(sampleRows.slice(0, 2));
    expect(state.page.total).toBe(sampleRows.length);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 0, limit: 12, sort: []}));

    search({keyword: 'Row 02'});
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({keyword: 'Row 02'}));

    sort();
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({sort: [{field: 'createTime', direction: 'ASC'}]}));

    sizeChange(24);
    await Promise.resolve();
    expect(state.page.current).toBe(1);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 0, limit: 24}));

    currentChange(2);
    await Promise.resolve();
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 24, limit: 24}));
  });

  it('keeps the newest server response when requests resolve out of order', async () => {
    let resolveFirst!: (value: {items: Row[]; offset: number; limit: number; total: number; hasNext: boolean}) => void;
    let resolveSecond!: (value: {items: Row[]; offset: number; limit: number; total: number; hasNext: boolean}) => void;
    const request = vi
      .fn()
      .mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));
    const {state, load} = usePagedList<Row>({request});

    const first = load();
    const second = load();
    resolveFirst({items: [sampleRows[0]], offset: 0, limit: 12, total: 1, hasNext: false});
    resolveSecond({items: [sampleRows[1]], offset: 0, limit: 12, total: 1, hasNext: false});
    await Promise.all([first, second]);

    expect(state.listData).toEqual([sampleRows[1]]);
    expect(state.loading).toBe(false);
  });

  it('loads client data with visible error state and retry support', async () => {
    const request = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(sampleRows.slice(0, 2));
    const {state, loadClientData, setAllData} = usePagedList<Row>();

    await loadClientData(request, setAllData);
    expect(state.status).toBe('error');
    expect(state.error).toBeInstanceOf(Error);
    expect(state.loading).toBe(false);

    await loadClientData(request, setAllData);
    expect(state.status).toBe('success');
    expect(state.listData).toEqual(sampleRows.slice(0, 2));
    expect(state.lastUpdated).not.toBeNull();
  });

  it('keeps the newest client response when requests resolve out of order', async () => {
    let resolveFirst!: (value: Row[]) => void;
    let resolveSecond!: (value: Row[]) => void;
    const firstRequest = () => new Promise<Row[]>((resolve) => (resolveFirst = resolve));
    const secondRequest = () => new Promise<Row[]>((resolve) => (resolveSecond = resolve));
    const {state, loadClientData, setAllData} = usePagedList<Row>();

    const first = loadClientData(firstRequest, setAllData);
    const second = loadClientData(secondRequest, setAllData);
    resolveSecond([sampleRows[1]]);
    resolveFirst([sampleRows[0]]);
    await Promise.all([first, second]);

    expect(state.listData).toEqual([sampleRows[1]]);
    expect(state.loading).toBe(false);
  });
});

// Controllable ResizeObserver — the shared test-setup mock never fires, so
// wall-gated tests drive the first measurement themselves.
class ControllableResizeObserver {
  static instances: ControllableResizeObserver[] = [];
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();

  constructor(private readonly callback: () => void) {
    ControllableResizeObserver.instances.push(this);
  }

  trigger() {
    this.callback();
  }

  static reset() {
    ControllableResizeObserver.instances = [];
  }

  static get last() {
    return ControllableResizeObserver.instances[ControllableResizeObserver.instances.length - 1];
  }
}

const stubWall = (tracks: string, clientWidth = 1200): HTMLElement => {
  vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
  vi.stubGlobal('getComputedStyle', () => ({gridTemplateColumns: tracks}));
  const el = document.createElement('div');
  Object.defineProperty(el, 'clientWidth', {configurable: true, get: () => clientWidth});
  return el;
};

describe('usePagedList wall-managed paging (autoPageSize)', () => {
  const pageServer = (rows: Row[]) =>
    vi.fn(async (query: {offset: number; limit: number}) => ({
      items: rows.slice(query.offset, query.offset + query.limit),
      offset: query.offset,
      limit: query.limit,
      total: rows.length,
      hasNext: query.offset + query.limit < rows.length,
    }));

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('gates the setup load until the wall settles, then fires one sized request', async () => {
    ControllableResizeObserver.reset();
    const request = pageServer(pagedRows());
    const {state, load, bindWall} = usePagedList<Row>({autoPageSize: true, request});

    void load(); // setup-time call of the pages — must not request yet
    expect(request).not.toHaveBeenCalled();

    const wallRef = ref<HTMLElement | null>(null);
    bindWall(wallRef);
    wallRef.value = stubWall('1px 1px 1px 1px 1px'); // 5 cols → derived 10
    await nextTick();

    expect(request).toHaveBeenCalledTimes(1);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 0, limit: 10}));
    expect(state.page.size).toBe(10);
    expect(state.page.sizeAuto).toBe(true);
  });

  it('preserves the first record of the current page across a size remap', async () => {
    ControllableResizeObserver.reset();
    const rows = pagedRows();
    const request = pageServer(rows);
    const {state, currentChange, setSize, load} = usePagedList<Row>({request});

    await load();
    currentChange(2);
    await Promise.resolve();
    expect(state.page.current).toBe(2);

    // Page 2 of size 12 starts at absolute row 13; size 10 keeps it on
    // page 2 (rows 11-20) instead of kicking back to page 1.
    await setSize(10, {preservePosition: true});
    expect(state.page.size).toBe(10);
    expect(state.page.current).toBe(2);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 10, limit: 10}));
  });

  it('restarts at page 1 for an explicit size pick', async () => {
    ControllableResizeObserver.reset();
    const request = pageServer(pagedRows());
    const {state, currentChange, sizeChange, load} = usePagedList<Row>({request});

    await load();
    currentChange(2);
    await Promise.resolve();
    sizeChange(24);
    await Promise.resolve();
    expect(state.page.current).toBe(1);
    expect(state.page.size).toBe(24);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 0, limit: 24}));
  });

  it('marks managed lists manual when the user leaves auto mode', async () => {
    ControllableResizeObserver.reset();
    const request = pageServer(pagedRows(1));
    const {state, setManual, enableAuto} = usePagedList<Row>({autoPageSize: true, request});

    expect(state.page.sizeAuto).toBe(true);
    setManual(24);
    await Promise.resolve();
    expect(state.page.sizeAuto).toBe(false);
    expect(state.page.size).toBe(24);

    enableAuto();
    await Promise.resolve();
    expect(state.page.sizeAuto).toBe(true);
  });

  it('clamps a stale empty page back to the true last page exactly once', async () => {
    ControllableResizeObserver.reset();
    const rows = pagedRows();
    const shrunk = rows.slice(0, 20); // post-delete dataset
    // Delete-last-record window: page 3 (offset 24) suddenly comes back
    // empty with a shrunken total — the list must land on the real last
    // page instead of stranding on a blank wall.
    const request = vi
      .fn()
      .mockImplementationOnce(async (query: {offset: number; limit: number}) => ({
        items: rows.slice(query.offset, query.offset + query.limit),
        offset: query.offset,
        limit: query.limit,
        total: rows.length,
        hasNext: false,
      }))
      .mockImplementationOnce(async () => ({items: [], offset: 24, limit: 12, total: 20, hasNext: false}))
      .mockImplementation(async (query: {offset: number; limit: number}) => ({
        items: shrunk.slice(query.offset, query.offset + query.limit),
        offset: query.offset,
        limit: query.limit,
        total: shrunk.length,
        hasNext: false,
      }));
    const {state, currentChange, load} = usePagedList<Row>({request});

    await load();
    currentChange(3);
    // Nested clamp reload — wait for the landed state, not a timer
    // (ai-guardrails forbids hard-coded setTimeout waits).
    await vi.waitFor(() => {
      expect(state.page.current).toBe(2);
      expect(state.listData).toHaveLength(8);
    });

    // Clamped to ceil(20/12) = 2 — one corrective reload, never a loop.
    expect(request.mock.calls.length).toBe(3);
    expect(request).toHaveBeenLastCalledWith(expect.objectContaining({offset: 12, limit: 12}));
  });

  it('never reloads for a same-value currentChange', async () => {
    ControllableResizeObserver.reset();
    const request = vi.fn(async () => ({items: [], offset: 0, limit: 12, total: 0, hasNext: false}));
    const {state, load, currentChange} = usePagedList<Row>({request});

    await load();
    const calls = request.mock.calls.length;
    currentChange(state.page.current);
    await Promise.resolve();
    expect(request.mock.calls.length).toBe(calls);
  });

  it('keeps the wire shape: offset/limit/sort/query only, limit capped', async () => {
    ControllableResizeObserver.reset();
    const request = vi.fn(async (query: Record<string, unknown>) => ({
      items: [],
      offset: Number(query.offset ?? 0),
      limit: Number(query.limit ?? 0),
      total: 0,
      hasNext: false,
    }));
    // Plain list — the wire contract is identical for wall-managed pages;
    // gating is covered above, here only the request body shape matters.
    const {setSize, search} = usePagedList<Row, {keyword?: string}>({request});

    search({keyword: 'Row'});
    await Promise.resolve();
    const body = request.mock.calls[request.mock.calls.length - 1][0] as Record<string, unknown>;
    expect(Object.keys(body).sort()).toEqual(['keyword', 'limit', 'offset', 'sort']);
    expect(Number(body.limit)).toBeLessThanOrEqual(200);

    await setSize(500, {preservePosition: false});
    const capped = request.mock.calls[request.mock.calls.length - 1][0] as Record<string, unknown>;
    expect(capped.limit).toBe(200);
  });
});
