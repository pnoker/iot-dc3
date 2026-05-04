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

// rangeKey ("today" | "24h" | "7d" | "30d") wins over rangeHours server-side;
// the legacy rangeHours integer is kept for callers that haven't migrated yet.
export const statsTimeseries = (
  params: { granularity?: 'hour' | 'day'; rangeHours?: number; rangeKey?: string } = {}
) => httpGet(`${API_DATA_BASE}/dashboard/stats/timeseries`, { params });

export const statsTop = (
  params: { dimension?: 'device' | 'point' | 'driver'; rangeHours?: number; rangeKey?: string; limit?: number } = {}
) => httpGet(`${API_DATA_BASE}/dashboard/top`, { params });

export const streamLatest = (size = 20) => httpGet(`${API_DATA_BASE}/dashboard/stream`, { params: { size } });

export const alertStats = () => httpGet(`${API_DATA_BASE}/dashboard/alert/stats`);

export const alertLatest = (size = 10) => httpGet(`${API_DATA_BASE}/dashboard/alert/latest`, { params: { size } });

export const statsLatency = (params: { rangeHours?: number; rangeKey?: string } = { rangeHours: 24 }) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/latency`, { params });

export const statsActivity = (params: { rangeHours?: number; rangeKey?: string } = { rangeHours: 168 }) =>
  httpGet(`${API_DATA_BASE}/dashboard/stats/activity`, { params });

export const systemHealth = () => httpGet(`${API_DATA_BASE}/dashboard/system/health`);

export const dailyGrowth = (days = 7) => httpGet(`${API_MANAGER_BASE}/dashboard/growth`, { params: { days } });

export interface AlertPageQuery {
  source?: 'device' | 'driver' | null;
  eventTypeFlag?: number | null;
  confirmFlag?: number | null;
  rangeHours?: number | null;
  // Preferred time-range selector. Backend TimeRangeUtil turns this into a
  // concrete `from` timestamp (TODAY maps to local-midnight; 24h/7d/30d are
  // rolling windows). When both rangeKey and rangeHours are set, rangeKey wins.
  rangeKey?: string | null;
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

export const alertTrend = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/trend`, { params: { days } });

export const alertTopSources = (days = 30, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/top-sources`, { params: { days, limit } });

// ALARM hour × day-of-week heatmap. Service always returns 168 cells.
export const alertActivity = (days = 7) => httpGet(`${API_DATA_BASE}/dashboard/alert/activity`, { params: { days } });

// ALARM count by event_ext.type (driver-offline, driver-state-flip, ...).
export const alertTypeDistribution = (days = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/type-distribution`, { params: { days } });

// Sources that tripped the storm threshold in the last N hours.
export const alertStormSources = (hours = 1, minCount = 10, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/storm-sources`, { params: { hours, minCount, limit } });

// ---- Topology (Driver → Device → Profile → Point Sankey) ----
export type TopologyMode = 'cardinality' | 'volume';
export type TopologyNodeType = 'driver' | 'device' | 'profile' | 'point' | 'others';

export interface TopologyHiddenChild {
  id: string;
  name: string;
  type: 'driver' | 'device' | 'point';
}

export interface TopologyNode {
  id: string;
  name: string;
  layer: 1 | 2 | 3 | 4;
  type: TopologyNodeType;
  hiddenChildren?: TopologyHiddenChild[];
}

export interface TopologyLink {
  source: string;
  target: string;
  value: number;
}

export interface TopologyStats {
  driverCount: number;
  deviceCount: number;
  profileCount: number;
  pointCount: number;
  /** Human range label ("24h" / "7d" / …) — present only in volume mode. */
  rangeLabel?: string | null;
}

export interface TopologyResponse {
  nodes: TopologyNode[];
  links: TopologyLink[];
  stats: TopologyStats;
}

// Phase 0 only wires cardinality. rangeKey is reserved for Phase 2 volume mode.
export const topology = (params: { mode?: TopologyMode; rangeKey?: string } = {}) =>
  httpGet(`${API_MANAGER_BASE}/dashboard/topology`, { params });
