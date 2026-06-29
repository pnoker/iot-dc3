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

export const statsTimeseries = (params: TimeRangeParams & {granularity?: Granularity} = {}) =>
  httpGet<R<StatsTimeBucket[]>>(`${API_DATA_BASE}/dashboard/stats/timeseries`, {params: timeRangeParams(params)});

export const statsTop = (params: TimeRangeParams & {dimension?: TopDimension; limit?: number} = {}) =>
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
