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
  alertAging,
  alertRows,
  alertStats,
  dailyGrowth,
  deviceStats,
  driverStats,
  silentSources,
  statsActivity,
  statsLatency,
  statsTimeseries,
  statsToday,
  statsTop,
  streamLatest,
  systemHealth,
  topology,
} from '../seed/dashboard';

/**
 * Home page hard dependencies plus the insight-card endpoints that populate
 * the five secondary cards (live feed, analytics tabs, SLA badge, latency,
 * activity heatmap). The remaining event-overview-only endpoints
 * (alert/trend, flapping, correlation, …) are not registered here — every card
 * wraps its call in try/catch, so the generic fallback keeps them blank.
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

  on('get', 'api/v3/data/dashboard/stream', (ctx) => {
    const size = Number(ctx.params.size) || streamLatest.length;
    return responseOf(ctx.config, ok(streamLatest.slice(0, size)));
  });
  on('get', 'api/v3/manager/dashboard/device/stats', (ctx) => responseOf(ctx.config, ok(deviceStats)));
  on('get', 'api/v3/manager/dashboard/driver/stats', (ctx) => responseOf(ctx.config, ok(driverStats)));
  on('get', 'api/v3/data/dashboard/top', (ctx) => {
    const dim = (ctx.params.dimension as keyof typeof statsTop) || 'device';
    const rows = statsTop[dim] ?? statsTop.device;
    const limit = Number(ctx.params.limit) || 10;
    return responseOf(ctx.config, ok(rows.slice(0, limit)));
  });
  on('get', 'api/v3/data/dashboard/alert/aging', (ctx) => responseOf(ctx.config, ok(alertAging)));
  on('get', 'api/v3/data/dashboard/silent/sources', (ctx) =>
    responseOf(ctx.config, ok(silentSources)),
  );
  on('get', 'api/v3/data/dashboard/stats/latency', (ctx) => responseOf(ctx.config, ok(statsLatency)));
  on('get', 'api/v3/data/dashboard/stats/activity', (ctx) =>
    responseOf(ctx.config, ok(statsActivity)),
  );
}
