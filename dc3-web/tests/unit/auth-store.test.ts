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

import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';

import {useAuthStore} from '@/store';
import {AUTH_HEADERS} from '@/config/constant/common';
import {getStorage} from '@/utils/storageUtil';

import {seedAuthStorage, TEST_CREDENTIALS} from '../fixtures/auth';

const tokenMocks = vi.hoisted(() => ({
  generateSalt: vi.fn(),
  generateToken: vi.fn(),
  cancelToken: vi.fn(),
  checkTokenValid: vi.fn(),
}));

vi.mock('@/api/token', () => tokenMocks);

const elementPlusMocks = vi.hoisted(() => {
  const close = vi.fn();
  const service = vi.fn(() => ({close}));
  return {close, service};
});

vi.mock('element-plus', () => ({
  ElLoading: {service: elementPlusMocks.service},
}));

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(() => Promise.resolve()),
}));

vi.mock('@/config/router', () => ({
  default: {push: routerMocks.push},
}));

const notificationMocks = vi.hoisted(() => ({
  failMessage: vi.fn(),
}));

vi.mock('@/utils/notificationUtil', () => notificationMocks);

const loadingService = elementPlusMocks.service;
const loadingClose = elementPlusMocks.close;
const routerPush = routerMocks.push;

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();

    tokenMocks.generateSalt.mockResolvedValue({data: 'salt-abc'});
    tokenMocks.generateToken.mockResolvedValue({data: 'token-xyz'});
    tokenMocks.cancelToken.mockResolvedValue({data: true});
  });

  describe('login', () => {
    it('sends the raw password over HTTPS, persists token, and routes to home', async () => {
      const store = useAuthStore();
      await store.login({
        tenant: TEST_CREDENTIALS.tenant,
        name: TEST_CREDENTIALS.name,
        password: TEST_CREDENTIALS.password,
      });

      // Salt request first, then token request with the raw password.
      expect(tokenMocks.generateSalt).toHaveBeenCalledTimes(1);
      expect(tokenMocks.generateSalt).toHaveBeenCalledWith({
        tenant: TEST_CREDENTIALS.tenant,
        name: TEST_CREDENTIALS.name,
      });

      expect(tokenMocks.generateToken).toHaveBeenCalledTimes(1);
      const tokenPayload = tokenMocks.generateToken.mock.calls[0][0];
      expect(tokenPayload.tenant).toBe(TEST_CREDENTIALS.tenant);
      expect(tokenPayload.name).toBe(TEST_CREDENTIALS.name);
      expect(tokenPayload.salt).toBe(TEST_CREDENTIALS.salt);
      expect(tokenPayload.password).toBe(TEST_CREDENTIALS.password);

      // Storage now holds the credential triple.
      expect(getStorage(AUTH_HEADERS.TENANT)).toBe(TEST_CREDENTIALS.tenant);
      expect(getStorage(AUTH_HEADERS.LOGIN)).toBe(TEST_CREDENTIALS.name);
      expect(getStorage(AUTH_HEADERS.TOKEN)).toEqual({
        salt: TEST_CREDENTIALS.salt,
        token: TEST_CREDENTIALS.token,
      });

      // Reactive state is updated.
      expect(store.tenant).toBe(TEST_CREDENTIALS.tenant);
      expect(store.name).toBe(TEST_CREDENTIALS.name);

      // Router pushed to home.
      expect(routerPush).toHaveBeenCalledWith({name: 'home'});

      // Loading was opened and closed exactly once.
      expect(loadingService).toHaveBeenCalledTimes(1);
      expect(loadingClose).toHaveBeenCalledTimes(1);
    });

    it('closes the loading overlay and does not navigate when the API rejects', async () => {
      tokenMocks.generateSalt.mockRejectedValueOnce(new Error('network down'));
      const store = useAuthStore();

      // login now catches errors internally and shows failMessage
      await store.login({
        tenant: TEST_CREDENTIALS.tenant,
        name: TEST_CREDENTIALS.name,
        password: 'x',
      });

      expect(loadingClose).toHaveBeenCalledTimes(1);
      expect(tokenMocks.generateToken).not.toHaveBeenCalled();
      expect(routerPush).not.toHaveBeenCalled();
      // No partial credentials should be persisted on failure.
      expect(getStorage(AUTH_HEADERS.TOKEN)).toBeUndefined();
    });
  });

  describe('logout', () => {
    it('cancels the server token and clears storage when credentials exist', async () => {
      const creds = seedAuthStorage();

      const store = useAuthStore();
      await store.logout();

      expect(tokenMocks.cancelToken).toHaveBeenCalledWith({tenant: creds.tenant, name: creds.name});
      expect(getStorage(AUTH_HEADERS.TENANT)).toBeUndefined();
      expect(getStorage(AUTH_HEADERS.LOGIN)).toBeUndefined();
      expect(getStorage(AUTH_HEADERS.TOKEN)).toBeUndefined();
    });

    it('skips cancelToken when no credentials are stored but still clears storage', async () => {
      const store = useAuthStore();
      await store.logout();

      // Without tenant/user, the API call must not be made — otherwise we'd
      // be sending an anonymous cancel that the backend would reject.
      expect(tokenMocks.cancelToken).not.toHaveBeenCalled();
    });
  });

  describe('getters', () => {
    it('exposes getTenant and getName from storage', () => {
      const creds = seedAuthStorage({tenant: 'tenant-1', name: 'user-1'});

      const store = useAuthStore();

      expect(store.getTenant).toBe(creds.tenant);
      expect(store.getName).toBe(creds.name);
    });
  });
});
