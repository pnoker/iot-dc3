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
  AlertActivityRow,
  DailyGrowthSummary,
  DeviceStats,
  DriverStats,
  Granularity,
  StatsCountBucket,
  StatsTimeBucket,
  StatsTodaySummary,
  StreamRow,
  TimeRangeParams,
  TopDimension,
} from '@/config/types/dashboard';

/**
 * Fetch today's stats summary.
 * @returns the stats today summary response
 */
export const statsToday = () => httpGet<StatsTodaySummary>(`${API_DATA_BASE}/dashboard/stats/today`);

/**
 * Fetch the stats timeseries.
 * @param params - query parameters for the request
 * @returns the stats time bucket response
 */
export const statsTimeseries = (params: TimeRangeParams & { granularity?: Granularity } = {}) =>
  httpGet<StatsTimeBucket[]>(`${API_DATA_BASE}/dashboard/stats/timeseries`, {params: timeRangeParams(params)});

/**
 * Fetch the stats top.
 * @param params - query parameters for the request
 * @returns the stats count bucket response
 */
export const statsTop = (params: TimeRangeParams & { dimension?: TopDimension; limit?: number } = {}) =>
  httpGet<StatsCountBucket[]>(`${API_DATA_BASE}/dashboard/top`, {params: timeRangeParams(params)});

/**
 * Stream latest.
 * @param limit - maximum number of entries to return or generate
 * @returns the streamed response
 */
export const streamLatest = (limit = 20) =>
  httpGet<StreamRow[]>(`${API_DATA_BASE}/dashboard/stream`, {params: {limit}});

/**
 * Fetch the stats latency.
 * @param params - query parameters for the request
 * @returns the stats count bucket response
 */
export const statsLatency = (params: TimeRangeParams = {rangeKey: '24h'}) =>
  httpGet<StatsCountBucket[]>(`${API_DATA_BASE}/dashboard/stats/latency`, {params: timeRangeParams(params)});

/**
 * Fetch the stats activity.
 * @param params - query parameters for the request
 * @returns the alert activity row response
 */
export const statsActivity = (params: TimeRangeParams = {rangeKey: '7d'}) =>
  httpGet<AlertActivityRow[]>(`${API_DATA_BASE}/dashboard/stats/activity`, {params: timeRangeParams(params)});

/**
 * Fetch the daily growth series.
 * @param days - days entries
 * @returns the daily growth summary response
 */
export const dailyGrowth = (days = 7) =>
  httpGet<DailyGrowthSummary>(`${API_MANAGER_BASE}/dashboard/growth`, {params: {days}});

/**
 * Fetch the driver stats.
 * @returns the driver stats response
 */
export const driverStats = () => httpGet<DriverStats>(`${API_MANAGER_BASE}/dashboard/driver/stats`);

/**
 * Fetch the device stats.
 * @param topN - number of top entries to keep
 * @returns the device stats response
 */
export const deviceStats = (topN = 10) =>
  httpGet<DeviceStats>(`${API_MANAGER_BASE}/dashboard/device/stats`, {params: {top_n: topN}});

const timeRangeParams = <T extends TimeRangeParams>(params: T) => {
  const {rangeKey, rangeHours, ...rest} = params;
  return {
    ...rest,
    ...(rangeKey !== undefined ? {range_key: rangeKey} : {}),
    ...(rangeHours !== undefined ? {range_hours: rangeHours} : {}),
  };
};
