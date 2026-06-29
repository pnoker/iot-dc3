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
