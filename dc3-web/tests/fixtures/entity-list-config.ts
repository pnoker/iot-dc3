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

import {vi} from 'vitest';

import type {EntityListConfig} from '@/config/types/entityList';

/**
 * Fixed rows returned by the stub list function. Two records with deterministic
 * values so assertions can be written against concrete expectations.
 */
export const ENTITY_LIST_ROWS = [
  {
    id: 'row-1',
    name: 'Alpha',
    status: 'SUCCESS',
    createTime: '2026-01-15T10:30:00.000Z',
    extra: {metadata: '{"key":"value"}'},
  },
  {
    id: 'row-2',
    name: null,
    status: 'FAILED',
    createTime: null,
    extra: {metadata: null},
  },
];

/**
 * Minimal EntityListConfig fixture for unit-testing useEntityListPage.
 * Covers: one search field (input), two columns (time + tag), one json field.
 *
 * The `list` function is a vi.fn() so tests can assert call counts and override
 * the return value per-test with mockResolvedValueOnce.
 */
export function makeEntityListConfig(): EntityListConfig {
  return {
    name: 'TestEntity',
    mode: 'page',
    editable: true,

    searchFields: [
      {
        prop: 'keyword',
        label: 'Keyword',
        kind: 'input',
        placeholder: 'Search…',
      },
    ],

    columns: [
      {
        prop: 'createTime',
        label: 'Created',
        kind: 'time',
      },
      {
        prop: 'status',
        label: 'Status',
        kind: 'tag',
        options: [
          {label: 'Success', value: 'SUCCESS'},
          {label: 'Failed', value: 'FAILED'},
        ],
      },
    ],

    fields: [
      {
        prop: 'name',
        label: 'Name',
        kind: 'input',
        required: true,
      },
      {
        prop: 'extra',
        label: 'Extra',
        kind: 'json',
      },
    ],

    defaultForm: () => ({
      name: '',
      extra: '',
    }),

    list: vi.fn(() =>
      Promise.resolve({
        ok: true,
        code: 'ok',
        message: 'ok',
        data: {records: ENTITY_LIST_ROWS, total: ENTITY_LIST_ROWS.length},
      })
    ),

    add: vi.fn(() => Promise.resolve({ok: true, code: 'ok', message: 'ok', data: null})),
    update: vi.fn(() => Promise.resolve({ok: true, code: 'ok', message: 'ok', data: null})),
    remove: vi.fn(() => Promise.resolve({ok: true, code: 'ok', message: 'ok', data: null})),

    emptyText: 'No records',
    confirmDeleteText: 'Confirm delete?',
  };
}
