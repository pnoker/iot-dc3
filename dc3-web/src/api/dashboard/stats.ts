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

import {httpGet} from '@/api/common';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {
  DailyGrowthSummary,
  Granularity,
  StatsTimeBucket,
  StatsTodaySummary,
  TimeRangeParams,
  TopDimension,
} from '@/config/types/dashboard';

export const statsToday = () => httpGet<R<StatsTodaySummary>>(`${API_DATA_BASE}/dashboard/stats/today`);

export const statsTimeseries = (params: TimeRangeParams & { granularity?: Granularity } = {}) =>
  httpGet<R<StatsTimeBucket[]>>(`${API_DATA_BASE}/dashboard/stats/timeseries`, {params: timeRangeParams(params)});

export const statsTop = (params: TimeRangeParams & { dimension?: TopDimension; limit?: number } = {}) =>
  httpGet(`${API_DATA_BASE}/dashboard/top`, {params: timeRangeParams(params)});

export const streamLatest = (size = 20) => httpGet(`${API_DATA_BASE}/dashboard/stream`, {params: {size}});

export const statsLatency = (params: TimeRangeParams = {rangeKey: '24h'}) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/latency`, {params: timeRangeParams(params)});

export const statsActivity = (params: TimeRangeParams = {rangeKey: '7d'}) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/activity`, {params: timeRangeParams(params)});

export const dailyGrowth = (days = 7) =>
  httpGet<R<DailyGrowthSummary>>(`${API_MANAGER_BASE}/dashboard/growth`, {params: {days}});

export const driverStats = () => httpGet(`${API_MANAGER_BASE}/dashboard/driver/stats`);

export const deviceStats = (topN = 10) =>
  httpGet(`${API_MANAGER_BASE}/dashboard/device/stats`, {params: {top_n: topN}});

const timeRangeParams = <T extends TimeRangeParams>(params: T) => {
  const {rangeKey, rangeHours, ...rest} = params;
  return {
    ...rest,
    ...(rangeKey !== undefined ? {range_key: rangeKey} : {}),
    ...(rangeHours !== undefined ? {range_hours: rangeHours} : {}),
  };
};
