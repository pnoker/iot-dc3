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

export const statsToday = () => httpGet(`${API_DATA_BASE}/dashboard/stats/today`);

export const statsTimeseries = (params: { granularity?: 'hour' | 'day'; rangeHours?: number } = {}) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/timeseries`, { params });

export const statsTop = (
  params: { dimension?: 'device' | 'point' | 'driver'; rangeHours?: number; limit?: number } = {}
) => httpGet(`${API_DATA_BASE}/dashboard/top`, { params });

export const streamLatest = (size = 20) => httpGet(`${API_DATA_BASE}/dashboard/stream`, { params: { size } });

export const alertStats = () => httpGet(`${API_DATA_BASE}/dashboard/alert/stats`);

export const alertLatest = (size = 10) => httpGet(`${API_DATA_BASE}/dashboard/alert/latest`, { params: { size } });

export const statsLatency = (rangeHours = 24) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/latency`, { params: { rangeHours } });

export const statsActivity = (rangeHours = 168) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/activity`, { params: { rangeHours } });

export const systemHealth = () => httpGet(`${API_DATA_BASE}/dashboard/system/health`);

export const dailyGrowth = (days = 7) => httpGet(`${API_MANAGER_BASE}/dashboard/growth`, { params: { days } });

export interface AlertPageQuery {
  source?: 'device' | 'driver' | null;
  eventTypeFlag?: number | null;
  confirmFlag?: number | null;
  current?: number;
  size?: number;
}

export const alertPage = (body: AlertPageQuery = {}) => httpPost(`${API_DATA_BASE}/dashboard/alert/page`, body);

export const alertConfirm = (source: 'device' | 'driver', id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/confirm/${source}/${id}`);

export const alertUnconfirm = (source: 'device' | 'driver', id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/unconfirm/${source}/${id}`);

export const alertBulkConfirm = (
  items: Array<{ source: 'device' | 'driver'; id: string | number }>,
  confirm: boolean
) => httpPost(`${API_DATA_BASE}/dashboard/alert/bulk-confirm`, { items, confirm });

export const driverStats = () => httpGet(`${API_MANAGER_BASE}/dashboard/driver/stats`);

export const deviceStats = (topN = 10) => httpGet(`${API_MANAGER_BASE}/dashboard/device/stats`, { params: { topN } });
