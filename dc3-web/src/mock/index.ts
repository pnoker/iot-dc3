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

import request from '@/config/axios';
import {AUTH_HEADERS} from '@/config/constant/common';
import {getStorage, setStorage} from '@/utils/storageUtil';

import {createMockAdapter} from './adapter';
import {setFallback} from './dispatch';
import {registerAuthHandlers} from './handlers/auth';
import {registerMenuHandlers} from './handlers/menu';
import {fallbackHandler} from './handlers/fallback';

let installed = false;

/**
 * Install the mock axios adapter and seed local auth so the static demo runs
 * with zero backend. Called from main.ts only in mock builds; the dynamic
 * import + build-time `import.meta.env.MODE` literal lets production builds
 * drop this module entirely (verified via grep for the mock-token sentinel).
 *
 * Must run before `app.mount` so the adapter is in place ahead of the first
 * router guard's menuStore.fetchTree() call.
 */
export function setupMock(): void {
  if (installed) return;
  installed = true;

  setFallback(fallbackHandler);
  registerAuthHandlers();
  registerMenuHandlers();

  request.defaults.adapter = createMockAdapter();

  // Pre-seed auth so the first navigation lands on /home without a login click.
  // The real login flow still works (registerAuthHandlers mocks the token
  // endpoints), so logout → login → home is demoable too.
  if (getStorage(AUTH_HEADERS.TENANT) == null) {
    setStorage(AUTH_HEADERS.TENANT, 'default');
    setStorage(AUTH_HEADERS.LOGIN, 'dc3');
    setStorage(AUTH_HEADERS.TOKEN, {salt: 'mock-salt', token: 'mock-token'});
  }
}
