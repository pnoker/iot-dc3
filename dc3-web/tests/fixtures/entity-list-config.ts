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
