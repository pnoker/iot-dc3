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

import type {AxiosAdapter, InternalAxiosRequestConfig} from 'axios';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import request from '@/config/axios';
import {AUTH_HEADERS} from '@/config/constant/common';
import {getStorage, setStorage} from '@/utils/storageUtil';

const notificationSpies = vi.hoisted(() => ({
  failMessage: vi.fn(),
  warnMessage: vi.fn(),
}));

vi.mock('@/utils/notificationUtil', () => notificationSpies);

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(() => Promise.resolve()),
}));

vi.mock('@/config/router', () => ({
  default: {push: routerMocks.push},
}));

const responseOf = (config: InternalAxiosRequestConfig, status: number, data: unknown) => ({
  data,
  status,
  statusText: String(status),
  headers: {},
  config,
  request: {},
});

describe('axios request instance', () => {
  beforeEach(() => {
    notificationSpies.failMessage.mockClear();
    notificationSpies.warnMessage.mockClear();
  });

  it('injects auth headers and parses large integer JSON responses as strings', async () => {
    setStorage(AUTH_HEADERS.TENANT, 'default');
    setStorage(AUTH_HEADERS.LOGIN, 'dc3');
    setStorage(AUTH_HEADERS.TOKEN, {salt: 'salt', token: 'token'});

    const adapter: AxiosAdapter = vi.fn(async (config) => {
      expect(config.headers.get(AUTH_HEADERS.TENANT)).toBe('default');
      expect(config.headers.get(AUTH_HEADERS.LOGIN)).toBe('dc3');
      expect(config.headers.get(AUTH_HEADERS.TOKEN)).toBe(JSON.stringify({salt: 'salt', token: 'token'}));

      return responseOf(config, 200, '{"ok":true,"code":0,"message":"success","data":{"id":9007199254740993}}');
    });

    const response = await request({
      url: 'api/v3/manager/device/select_by_id',
      method: 'get',
      params: {id: 1},
      adapter,
    });

    expect(response).toEqual({
      ok: true,
      code: 0,
      message: 'success',
      data: {id: '9007199254740993'},
    });
  });

  it('removes auth keys and routes to login on unauthorized responses', async () => {
    setStorage(AUTH_HEADERS.TENANT, 'default');
    setStorage(AUTH_HEADERS.LOGIN, 'dc3');
    setStorage(AUTH_HEADERS.TOKEN, {salt: 'salt', token: 'token'});

    const adapter: AxiosAdapter = async (config) => responseOf(config, 401, {ok: false, code: 401});

    await expect(request({url: 'api/v3/auth/token/check', method: 'post', adapter})).rejects.toEqual({
      ok: false,
      code: 401,
    });

    expect(notificationSpies.warnMessage).toHaveBeenCalledTimes(1);
    // Only auth keys are removed — not the entire localStorage
    expect(getStorage(AUTH_HEADERS.TENANT)).toBeUndefined();
    expect(getStorage(AUTH_HEADERS.LOGIN)).toBeUndefined();
    expect(getStorage(AUTH_HEADERS.TOKEN)).toBeUndefined();
    // Routes via router.push instead of raw hash manipulation
    expect(routerMocks.push).toHaveBeenCalledWith({name: 'login'});
  });

  it('rejects non-ok business responses and surfaces the server payload', async () => {
    const payload = {ok: false, code: 50001, message: 'business failed'};
    // Use status 400 — non-ok, non-401, non-5xx hits the failMessage branch
    const adapter: AxiosAdapter = async (config) => responseOf(config, 400, payload);

    await expect(request({url: 'api/v3/data/dashboard/stats/today', method: 'get', adapter})).rejects.toBe(payload);

    expect(notificationSpies.failMessage).toHaveBeenCalledWith(
      'API request error. Please contact the system administrator.',
      50001,
      payload
    );
  });
});
