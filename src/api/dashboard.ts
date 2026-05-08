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

import { httpGet, httpPost } from '@/api/common';
import { API_DATA_BASE, API_MANAGER_BASE } from '@/config/constant/api';
import type {
  AlertPageQuery,
  AlertStatsSummary,
  AlertSource,
  DailyGrowthSummary,
  Granularity,
  StatsTimeBucket,
  StatsTodaySummary,
  TimeRangeParams,
  TopDimension,
  TopologyMode,
} from '@/config/entity/dashboard';

export const statsToday = () => httpGet<R<StatsTodaySummary>>(`${API_DATA_BASE}/dashboard/stats/today`);

export const statsTimeseries = (params: TimeRangeParams & { granularity?: Granularity } = {}) =>
  httpGet<R<StatsTimeBucket[]>>(`${API_DATA_BASE}/dashboard/stats/timeseries`, { params });

export const statsTop = (params: TimeRangeParams & { dimension?: TopDimension; limit?: number } = {}) =>
  httpGet(`${API_DATA_BASE}/dashboard/top`, { params });

export const streamLatest = (size = 20) => httpGet(`${API_DATA_BASE}/dashboard/stream`, { params: { size } });

export const alertStats = () => httpGet<R<AlertStatsSummary>>(`${API_DATA_BASE}/dashboard/alert/stats`);

export const alertLatest = (size = 10) => httpGet(`${API_DATA_BASE}/dashboard/alert/latest`, { params: { size } });

export const statsLatency = (params: TimeRangeParams = { rangeKey: '24h' }) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/latency`, { params });

export const statsActivity = (params: TimeRangeParams = { rangeKey: '7d' }) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/activity`, { params });

export const systemHealth = () => httpGet(`${API_DATA_BASE}/dashboard/system/health`);

export const dailyGrowth = (days = 7) =>
  httpGet<R<DailyGrowthSummary>>(`${API_MANAGER_BASE}/dashboard/growth`, { params: { days } });

export const alertPage = (body: AlertPageQuery = {}) => httpPost(`${API_DATA_BASE}/dashboard/alert/page`, body);

export const alertConfirm = (source: AlertSource, id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/confirm/${source}/${id}`);

export const alertUnconfirm = (source: AlertSource, id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/unconfirm/${source}/${id}`);

export const alertBulkConfirm = (items: Array<{ source: AlertSource; id: string | number }>, confirm: boolean) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/bulk-confirm`, { items, confirm });

export const driverStats = () => httpGet(`${API_MANAGER_BASE}/dashboard/driver/stats`);

export const deviceStats = (topN = 10) => httpGet(`${API_MANAGER_BASE}/dashboard/device/stats`, { params: { topN } });

export const alertTrend = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/trend`, { params: { days } });

export const alertTopSources = (days = 30, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/top-sources`, { params: { days, limit } });

export const alertActivity = (days = 7) => httpGet(`${API_DATA_BASE}/dashboard/alert/activity`, { params: { days } });

export const alertTypeDistribution = (days = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/type-distribution`, { params: { days } });

export const alertStormSources = (hours = 1, minCount = 10, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/storm-sources`, { params: { hours, minCount, limit } });

export const topology = (params: { mode?: TopologyMode; rangeKey?: string } = {}) =>
  httpGet(`${API_MANAGER_BASE}/dashboard/topology`, { params });

export const alertFlapping = (hours = 6, minCount = 5, limit = 20) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/flapping`, { params: { hours, minCount, limit } });

export const alertCorrelation = (hours = 24, windowSec = 30, limit = 15) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/correlation`, { params: { hours, windowSec, limit } });

export const alertPeerDeviation = (days = 7) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/peer-deviation`, { params: { days } });

export const alertAging = () => httpGet(`${API_DATA_BASE}/dashboard/alert/aging`);

export const alertMtta = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/mtta`, { params: { days } });

export const protocolHealth = () => httpGet(`${API_DATA_BASE}/dashboard/protocol/health`);

export const alertChangeImpact = (days = 30, limit = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/change-impact`, { params: { days, limit } });

export const silentSources = (baselineDays = 7, silentMinutes = 15, limit = 50) =>
  httpGet(`${API_DATA_BASE}/dashboard/silent/sources`, {
    params: { baselineDays, silentMinutes, limit },
  });

export const coverageGap = (limit = 100) => httpGet(`${API_DATA_BASE}/dashboard/coverage/gap`, { params: { limit } });
