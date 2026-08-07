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

import {createMockAdapter} from './adapter';
import {setFallback} from './dispatch';
import {registerAuthHandlers} from './handlers/auth';
import {registerCoreHandlers} from './handlers/core';
import {registerDashboardHandlers} from './handlers/dashboard';
import {registerDictionaryHandlers} from './handlers/dictionary';
import {registerMenuHandlers} from './handlers/menu';
import {registerAgenticHandlers} from './handlers/agentic';
import {registerBusinessHandlers} from './handlers/business';
import {registerSettingsHandlers} from './handlers/settings';
import {registerTimeseriesHandlers} from './handlers/timeseries';
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
  registerCoreHandlers();
  registerDashboardHandlers();
  registerDictionaryHandlers();
  registerMenuHandlers();
  registerTimeseriesHandlers();
  registerSettingsHandlers();
  registerBusinessHandlers();
  registerAgenticHandlers();

  request.defaults.adapter = createMockAdapter();

  // No pre-seeded auth: the demo lands on /login so visitors experience the
  // full sign-in flow. registerAuthHandlers mocks the token endpoints, so any
  // tenant/name + a 6+ char password logs in and reaches /home.
}
