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

vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), warnMessage: vi.fn()}));
vi.mock('@/config/router', () => ({default: {push: vi.fn(() => Promise.resolve())}}));

import request from '@/config/axios';
import {setupMock} from '@/mock';

/**
 * Exercise the mock adapter end-to-end through the real axios instance + its
 * interceptors. Guards the demo's load-bearing shapes: menu tree (router
 * guard), core list pagination, login flow, and the generic fallbacks that
 * keep unregistered endpoints from surfacing errors.
 */
describe('mock adapter', () => {
  beforeEach(() => {
    localStorage.clear();
    setupMock();
  });

  const call = (config: Record<string, unknown>) => request(config as never) as Promise<any>;

  it('seeds local auth so the router guard treats the session as logged in', () => {
    expect(localStorage.getItem('X-Auth-Tenant')).not.toBeNull();
    expect(localStorage.getItem('X-Auth-Token')).not.toBeNull();
  });

  it('returns the full menu tree from list_tree', async () => {
    const res = await call({url: 'api/v3/auth/menu/list_tree', method: 'post', data: {}});
    expect(res.ok).toBe(true);
    expect(Array.isArray(res.data)).toBe(true);
    expect(res.data.some((n: any) => n.menuCode === 'driver')).toBe(true);
    expect(res.data.some((n: any) => n.menuCode === 'settings')).toBe(true);
  });

  it('paginates core lists into a PageResult', async () => {
    const res = await call({url: 'api/v3/manager/driver/list', method: 'post', data: {page: {current: 1, size: 12}}});
    expect(res.data.total).toBeGreaterThan(0);
    expect(Array.isArray(res.data.records)).toBe(true);
    expect(res.data.records.length).toBeGreaterThan(0);
  });

  it('resolves get_by_id by id', async () => {
    const res = await call({url: 'api/v3/manager/driver/get_by_id', method: 'get', params: {id: '1001'}});
    expect(res.data.id).toBe('1001');
  });

  it('returns a driver dictionary with label/value', async () => {
    const res = await call({url: 'api/v3/manager/dictionary/driver', method: 'post', data: {}});
    expect(res.data.records[0]).toHaveProperty('label');
    expect(res.data.records[0]).toHaveProperty('value');
  });

  it('mocks the token endpoints so login works end-to-end', async () => {
    const salt = await call({url: 'api/v3/auth/token/salt', method: 'post', data: {}});
    expect(salt.data).toBe('mock-salt');
    const token = await call({url: 'api/v3/auth/token/generate', method: 'post', data: {}});
    expect(token.data).toBe('mock-token');
  });

  it('stubs unregistered config-domain lists instead of erroring', async () => {
    const res = await call({url: 'api/v3/auth/role/list', method: 'post', data: {page: {current: 1, size: 12}}});
    expect(res.ok).toBe(true);
    expect(res.data.records.length).toBe(3);
  });

  it('returns [] for unregistered flat-array endpoints', async () => {
    const res = await call({url: 'api/v3/auth/role/list_tree', method: 'post', data: {}});
    expect(res.ok).toBe(true);
    expect(res.data).toEqual([]);
  });

  it('returns the dashboard topology with layered nodes', async () => {
    const res = await call({url: 'api/v3/manager/dashboard/topology', method: 'get'});
    expect(res.data.nodes.length).toBeGreaterThan(0);
    expect(res.data.stats.driverCount).toBeGreaterThan(0);
  });
});
