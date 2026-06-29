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

import {reactive} from 'vue';

import type {Order, PageQuery, PageResult} from '@/config/types';

export interface PagedListPage {
  total: number;
  size: number;
  current: number;
  orders: Order[];
}

export interface PagedListState<T, Q extends Record<string, any> = Record<string, any>> {
  loading: boolean;
  listData: T[];
  allData: T[];
  query: Partial<Q>;
  order: boolean;
  page: PagedListPage;
}

export interface UsePagedListOptions<T, Q extends Record<string, any> = Record<string, any>> {
  pageSize?: number;
  sortColumn?: string;
  request?: (query: PageQuery & Partial<Q>) => Promise<R<PageResult<T>>>;
  filter?: (rows: T[], query: Partial<Q>) => T[];
  sortValue?: (row: T) => string | number | null | undefined;
}

export const usePagedList = <T, Q extends Record<string, any> = Record<string, any>>(
  options: UsePagedListOptions<T, Q> = {}
) => {
  const state = reactive({
    loading: false,
    listData: [] as T[],
    allData: [] as T[],
    query: {} as Partial<Q>,
    order: false,
    page: {
      total: 0,
      size: options.pageSize ?? 12,
      current: 1,
      orders: [] as Order[],
    },
  }) as PagedListState<T, Q>;

  const applyFilters = () => {
    const filtered = options.filter ? options.filter([...state.allData], state.query) : [...state.allData];
    state.page.total = filtered.length;
    const start = (state.page.current - 1) * state.page.size;
    state.listData = filtered.slice(start, start + state.page.size);
  };

  const setAllData = (rows: T[]) => {
    state.allData = [...rows];
    applyFilters();
  };

  const load = async () => {
    if (!options.request) {
      applyFilters();
      return;
    }

    state.loading = true;
    try {
      const response = await options.request({page: state.page, ...state.query} as PageQuery & Partial<Q>);
      const data = response.data || ({records: [], total: 0} as PageResult<T>);
      state.listData = data.records || [];
      state.page.total = data.total || 0;
    } catch {
      // handled globally
    } finally {
      state.loading = false;
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
    state.order = !state.order;
    state.page.orders = [{column: options.sortColumn ?? 'create_time', asc: state.order}];

    if (options.request) {
      void load();
      return;
    }

    if (options.sortValue) {
      const asc = state.order;
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
    state.loading = true;
    try {
      await handler();
    } finally {
      state.loading = false;
    }
  };

  return {
    state,
    setAllData,
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
