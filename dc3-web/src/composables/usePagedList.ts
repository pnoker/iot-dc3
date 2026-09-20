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

import {getCurrentInstance, onUnmounted, reactive} from 'vue';

import type {Order, PageQuery, PageResult, SortSpec} from '@/config/types';

/**
 * PagedListPage data contract.
 */
export interface PagedListPage {
  total: number;
  size: number;
  current: number;
  orders: Order[];
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
  sortColumn?: string;
  request?: (query: PageQuery & Partial<Q>) => Promise<PageResult<T>>;
  filter?: (rows: T[], query: Partial<Q>) => T[];
  sortValue?: (row: T) => string | number | null | undefined;
}

/**
 * Client-side paged list state: filtering, sorting, and server-backed loading
 * through the configured request.
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
      size: options.pageSize ?? 12,
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

  const sizeChange = (size: number) => {
    state.page.size = size;
    state.page.current = 1;
    if (options.request) {
      void load();
      return;
    }
    applyFilters();
  };

  const currentChange = (current: number) => {
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
  };
};
