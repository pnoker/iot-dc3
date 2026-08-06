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

import {on} from '../dispatch';
import {ok, okPage, responseOf} from '../response';
import {
  alertRows,
  alertStats,
  dailyGrowth,
  statsTimeseries,
  statsToday,
  systemHealth,
  topology,
} from '../seed/dashboard';

/**
 * Home page hard dependencies. The remaining insight-card endpoints
 * (latency, activity, stream, top, alert/trend, flapping, …) are not
 * registered here on purpose — every card wraps its call in try/catch, so the
 * generic fallback (empty object/array) keeps them blank without errors.
 */
export function registerDashboardHandlers(): void {
  on('get', 'api/v3/data/dashboard/stats/today', (ctx) => responseOf(ctx.config, ok(statsToday)));
  on('get', 'api/v3/data/dashboard/stats/timeseries', (ctx) =>
    responseOf(ctx.config, ok(statsTimeseries)),
  );
  on('get', 'api/v3/data/dashboard/alert/stats', (ctx) => responseOf(ctx.config, ok(alertStats)));
  on('get', 'api/v3/data/dashboard/alert/latest', (ctx) => responseOf(ctx.config, ok(alertRows)));
  on('get', 'api/v3/data/dashboard/system/health', (ctx) => responseOf(ctx.config, ok(systemHealth)));
  on('get', 'api/v3/manager/dashboard/growth', (ctx) => responseOf(ctx.config, ok(dailyGrowth)));
  on('get', 'api/v3/manager/dashboard/topology', (ctx) => responseOf(ctx.config, ok(topology)));
  on('post', 'api/v3/data/dashboard/alert/page', (ctx) => responseOf(ctx.config, okPage(alertRows)));
}
