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

import {AUTH_HEADERS} from '@/config/constant/common';
import {setStorage} from '@/utils/storageUtil';

/**
 * Canonical credentials used across auth-related tests. Centralised so
 * "what does a logged-in user look like" stays identical from auth-store
 * tests through router-guard tests through axios interceptor tests —
 * any change to the auth shape only needs editing here.
 */
export const TEST_CREDENTIALS = {
  tenant: 'acme',
  name: 'alice',
  password: 'plaintext-secret',
  salt: 'salt-abc',
  token: 'token-xyz',
} as const;

/**
 * Seeds localStorage with a logged-in user so router guards / axios
 * interceptors / auth getters see authenticated state. Returns the
 * credentials so tests can assert against the same values.
 */
export function seedAuthStorage(overrides: Partial<typeof TEST_CREDENTIALS> = {}) {
  const creds = {...TEST_CREDENTIALS, ...overrides};
  setStorage(AUTH_HEADERS.TENANT, creds.tenant);
  setStorage(AUTH_HEADERS.LOGIN, creds.name);
  setStorage(AUTH_HEADERS.TOKEN, {salt: creds.salt, token: creds.token});
  return creds;
}
