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

import {getCurrentInstance, onUnmounted, reactive, watch} from 'vue';

import type {Order, PageQuery, PageResult, SortSpec} from '@/config/types';
import {CARD_WALL_PAGE_SIZE_MAX, CARD_WALL_TARGET_PAGE_SIZE} from '@/config/constant/cardWall';
import {useCardWallAuto} from '@/composables/useCardWallPageSize';

/**
 * PagedListPage data contract.
 */
export interface PagedListPage {
  total: number;
  size: number;
  current: number;
  orders: Order[];
  /**
   * Wall-managed paging flag for ToolCard. Present only when the list
   * opts into `autoPageSize`: true renders the adaptive size select,
   * false an explicit size; absent keeps the classic built-in sizes.
   */
  sizeAuto?: boolean;
}

/**
 * Paged list state.
 */
export interface PagedListState<T, Q extends Record<string, any> = Record<string, any>> {
  loading: boolean;
  /** Last list request error. Kept separate from the global toast so pages can offer retry. */
  error: unknown | null;
  /** Request lifecycle state used to distinguish an empty result from a failed request. */
  status: 'idle' | 'loading' | 'success' | 'error';
  /** Timestamp of the last successful response. */
  lastUpdated: number | null;
  listData: T[];
  allData: T[];
  query: Partial<Q>;
  sortAsc: boolean;
  page: PagedListPage;
}

/**
 * Use paged list options.
 */
export interface UsePagedListOptions<T, Q extends Record<string, any> = Record<string, any>> {
  pageSize?: number;
  /**
   * Opt into wall-driven page sizing: the page size snaps to a whole-row
   * multiple of the card wall's measured column count. `true` targets
   * CARD_WALL_TARGET_PAGE_SIZE, a number overrides the target. Only card
   * wall pages opt in — table pages keep the fixed `pageSize`.
   */
  autoPageSize?: boolean | number;
  sortColumn?: string;
  request?: (query: PageQuery & Partial<Q>) => Promise<PageResult<T>>;
  filter?: (rows: T[], query: Partial<Q>) => T[];
  sortValue?: (row: T) => string | number | null | undefined;
}

/**
 * Client-side paged list state: filtering, sorting, and server-backed loading
 * through the configured request.
 *
 * With `autoPageSize` the page size follows the card wall's measured column
 * count (see useCardWallPageSize): the first load waits for the wall's first
 * measurement, resize band changes re-page with position preservation, and
 * `bindWall` wires the observation. Plain callers are unaffected.
 *
 * @param options paging, sorting, request, and filter hooks
 * @returns the composable handle
 */
export const usePagedList = <T, Q extends Record<string, any> = Record<string, any>>(
  options: UsePagedListOptions<T, Q> = {}
) => {
  const state = reactive({
    loading: false,
    error: null as unknown | null,
    status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
    lastUpdated: null as number | null,
    listData: [] as T[],
    allData: [] as T[],
    query: {} as Partial<Q>,
    sortAsc: false,
    page: {
      total: 0,
      size: options.pageSize ?? CARD_WALL_TARGET_PAGE_SIZE,
      current: 1,
      orders: [] as Order[],
    },
  }) as PagedListState<T, Q>;
  let latestLoadId = 0;
  let latestOperationId = 0;
  // `setAllData` is also used as the apply callback of `loadClientData`.
  // Keep track of that synchronous hand-off so applying the newest response
  // does not invalidate its own request token and leave `loading` stuck.
  let applyingLoadId: number | null = null;

  // Wall-driven sizing (card wall pages only). The manager gates the first
  // load until the wall settles a size and mirrors its auto/manual mode
  // onto the page contract ToolCard renders from.
  const autoActive = Boolean(options.autoPageSize);
  const wall = useCardWallAuto({
    target: typeof options.autoPageSize === 'number' ? options.autoPageSize : undefined,
    gateLoads: autoActive,
    getCurrentSize: () => state.page.size,
    applySize: (size, ctx) => setSize(size, ctx),
    requestLoad: () => load(),
    listLength: () => state.listData.length,
  });
  if (autoActive) {
    watch(
      wall.sizeAuto,
      (value) => {
        state.page.sizeAuto = value;
      },
      {immediate: true},
    );
  }

  const applyFilters = () => {
    const filtered = options.filter ? options.filter([...state.allData], state.query) : [...state.allData];
    state.page.total = filtered.length;
    // Clamp so a shrunken source (e.g. setAllData while parked on a high page)
    // never leaves the slice past the last page showing an empty list.
    const maxPage = Math.max(1, Math.ceil(filtered.length / state.page.size));
    if (state.page.current > maxPage) state.page.current = maxPage;
    const start = (state.page.current - 1) * state.page.size;
    state.listData = filtered.slice(start, start + state.page.size);
  };

  const setAllData = (rows: T[]) => {
    if (applyingLoadId === null) {
      latestLoadId += 1;
    }
    state.allData = [...rows];
    state.error = null;
    state.status = 'success';
    state.lastUpdated = Date.now();
    state.loading = false;
    applyFilters();
  };

  const loadClientData = async <R>(request: () => Promise<R>, apply: (result: R) => void) => {
    const loadId = ++latestLoadId;
    state.loading = true;
    state.status = 'loading';
    state.error = null;
    try {
      const result = await request();
      if (loadId !== latestLoadId) return;
      applyingLoadId = loadId;
      try {
        apply(result);
      } finally {
        applyingLoadId = null;
      }
      state.error = null;
      state.status = 'success';
      state.lastUpdated = Date.now();
    } catch (error) {
      if (loadId !== latestLoadId) return;
      state.error = error;
      state.status = 'error';
    } finally {
      if (loadId === latestLoadId) {
        state.loading = false;
      }
    }
  };

  const load = async () => {
    // The setup-time call of a wall-managed list is a no-op until the wall
    // settles a page size — the manager's first resolution then issues the
    // single correctly-sized request (no 12-then-reflow double fetch).
    // armBackstop keeps the promise: a wall that never appears resolves at
    // the fallback size instead of hanging the list empty.
    if (wall.pending.value) {
      wall.armBackstop();
      return;
    }

    if (!options.request) {
      applyFilters();
      state.status = 'success';
      state.lastUpdated = Date.now();
      return;
    }

    const loadId = ++latestLoadId;
    state.loading = true;
    state.status = 'loading';
    state.error = null;
    try {
      const sort: SortSpec[] = state.page.orders.map((order) => ({
        field: order.column,
        direction: order.asc ? 'ASC' : 'DESC',
      }));
      const data = await options.request({
        offset: (state.page.current - 1) * state.page.size,
        limit: state.page.size,
        sort,
        ...state.query,
      } as PageQuery & Partial<Q>);
      if (loadId !== latestLoadId) return;
      state.listData = data.items ?? [];
      state.page.total = data.total ?? state.listData.length;
      state.status = 'success';
      state.lastUpdated = Date.now();
      // A stale window (deleted last record, remapped past the end) can
      // land on an empty page while data still exists — fall back to the
      // true last page exactly once instead of stranding the user.
      if (state.listData.length === 0 && state.page.total > 0 && state.page.current > 1) {
        const clamped = Math.max(1, Math.ceil(state.page.total / state.page.size));
        if (clamped !== state.page.current) {
          state.page.current = clamped;
          await load();
        }
      }
    } catch (error) {
      if (loadId !== latestLoadId) return;
      state.error = error;
      state.status = 'error';
    } finally {
      if (loadId === latestLoadId) {
        state.loading = false;
        if (state.status === 'loading') state.status = 'success';
      }
    }
  };

  const search = (params?: Partial<Q>) => {
    state.query = params || {};
    state.page.current = 1;
    if (options.request) {
      void load();
      return;
    }
    applyFilters();
  };

  const reset = () => {
    state.query = {};
    state.page.current = 1;
    if (options.request) {
      void load();
      return;
    }
    applyFilters();
  };

  const sort = () => {
    state.sortAsc = !state.sortAsc;
    state.page.orders = [{column: options.sortColumn ?? 'createTime', asc: state.sortAsc}];

    if (options.request) {
      void load();
      return;
    }

    if (options.sortValue) {
      const asc = state.sortAsc;
      state.allData = [...state.allData].sort((a, b) => {
        const aValue = options.sortValue?.(a);
        const bValue = options.sortValue?.(b);
        const result = String(aValue ?? '').localeCompare(String(bValue ?? ''));
        return asc ? result : -result;
      });
    }

    applyFilters();
  };

  /**
   * Change the page size and reload.
   *
   * `preservePosition` remaps the current page so the first record of the
   * page in view stays on screen (a resize-driven auto adjustment);
   * otherwise the list restarts at page 1 (an explicit size pick).
   *
   * @param size the new page size (clamped to 1..CARD_WALL_PAGE_SIZE_MAX)
   * @param opts remap behaviour
   * @param opts.preservePosition keep the first record of the current page in view
   */
  const setSize = async (size: number, opts: {preservePosition?: boolean} = {}) => {
    const {preservePosition = true} = opts;
    const next = Math.min(CARD_WALL_PAGE_SIZE_MAX, Math.max(1, Math.round(size)));
    const oldSize = state.page.size;
    if (next === oldSize) return;
    const absFirst = (state.page.current - 1) * oldSize;
    // Atomic write with a pre-clamped cursor: Element Plus reacts to a
    // programmatic page-size change by re-emitting current-change, and a
    // stale cursor past the new page count would cascade a second load.
    state.page.size = next;
    if (preservePosition && state.page.total > 0) {
      const maxPage = Math.max(1, Math.ceil(state.page.total / next));
      state.page.current = Math.min(maxPage, Math.floor(absFirst / next) + 1);
    } else {
      state.page.current = 1;
    }
    if (options.request) {
      await load();
      return;
    }
    applyFilters();
  };

  const sizeChange = (size: number) => {
    if (autoActive) {
      // Classic size-change cannot be a wall adjustment — the user left
      // auto mode by picking a number.
      wall.sizeAuto.value = false;
    }
    void setSize(size, {preservePosition: false});
  };

  const currentChange = (current: number) => {
    // Element Plus re-emits current-change on programmatic page-size and
    // total changes; a same-value call must not reload.
    if (current === state.page.current) return;
    state.page.current = current;
    if (options.request) {
      void load();
      return;
    }
    applyFilters();
  };

  const withLoading = async (handler: () => Promise<void>) => {
    const operationId = ++latestOperationId;
    state.loading = true;
    state.status = 'loading';
    state.error = null;
    try {
      await handler();
      if (operationId !== latestOperationId) return;
      state.error = null;
      state.status = 'success';
      state.lastUpdated = Date.now();
    } catch (error) {
      if (operationId === latestOperationId) {
        state.error = error;
        state.status = 'error';
      }
      throw error;
    } finally {
      if (operationId === latestOperationId) state.loading = false;
    }
  };

  if (getCurrentInstance()) {
    onUnmounted(() => {
      latestLoadId += 1;
      latestOperationId += 1;
    });
  }

  return {
    state,
    setAllData,
    loadClientData,
    applyFilters,
    load,
    search,
    reset,
    sort,
    sizeChange,
    currentChange,
    withLoading,
    setSize,
    bindWall: wall.bindWall,
    enableAuto: wall.enableAuto,
    setManual: wall.setManual,
    ghostCount: wall.ghostCount,
  };
};
