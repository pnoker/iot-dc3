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

import {beforeEach, describe, expect, it, vi} from 'vitest';

const {requestMock} = vi.hoisted(() => ({requestMock: vi.fn()}));

vi.mock('@/config/axios', () => ({default: requestMock}));

import {crudAdd, crudDelete, crudGetById, crudList, crudUpdate, httpGet, httpPost} from '@/api/common';

describe('shared API transport helpers', () => {
  beforeEach(() => {
    requestMock.mockReset();
    requestMock.mockResolvedValue({ok: true});
  });

  it('maps HTTP and CRUD helpers to the expected Axios request shape', async () => {
    await httpGet('/health', {params: {verbose: true}});
    await httpPost('/command', {value: 42}, {timeout: 1000});
    await crudAdd('/device', {name: 'pump'});
    await crudUpdate('/device', {id: '7', name: 'pump-2'});
    await crudDelete('/device', '7');
    await crudGetById('/device', '7');
    await crudList('/device', {page: {current: 1, size: 20}});

    expect(requestMock.mock.calls).toEqual([
      [{params: {verbose: true}, url: '/health', method: 'get'}],
      [{timeout: 1000, url: '/command', method: 'post', data: {value: 42}}],
      [{url: '/device/add', method: 'post', data: {name: 'pump'}}],
      [{url: '/device/update', method: 'post', data: {id: '7', name: 'pump-2'}}],
      [{params: {id: '7'}, url: '/device/delete', method: 'post', data: undefined}],
      [{params: {id: '7'}, url: '/device/get_by_id', method: 'get'}],
      [{url: '/device/list', method: 'post', data: {page: {current: 1, size: 20}}}],
    ]);
  });
});
