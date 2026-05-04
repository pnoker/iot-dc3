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

// ==== Phase-2 insights — 9 new cards ===============================

export interface FlappingSource {
  source: 'device' | 'driver';
  sourceId: number | string;
  eventTypeFlag: number;
  count: number;
}
export const alertFlapping = (hours = 6, minCount = 5, limit = 20) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/flapping`, { params: { hours, minCount, limit } });

export interface CorrelationPair {
  aSource: 'device' | 'driver';
  aSourceId: number | string;
  aEventType: number;
  bSource: 'device' | 'driver';
  bSourceId: number | string;
  bEventType: number;
  coCount: number;
}
export const alertCorrelation = (hours = 24, windowSec = 30, limit = 15) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/correlation`, { params: { hours, windowSec, limit } });

export interface PeerDeviation {
  profileId: number | string;
  deviceId: number | string;
  alarmCount: number;
  peerMedian: number;
  ratio: number;
}
export const alertPeerDeviation = (days = 7) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/peer-deviation`, { params: { days } });

export interface AgingBacklog {
  under1h: number;
  h1to6: number;
  h6to24: number;
  over24h: number;
  total: number;
}
export const alertAging = () => httpGet(`${API_DATA_BASE}/dashboard/alert/aging`);

export interface MttaTrend {
  date: string;
  p50Ms: number;
  p95Ms: number;
  confirmedCount: number;
}
export const alertMtta = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/mtta`, { params: { days } });

export interface ProtocolHealth {
  serviceName: string;
  driverCount: number;
  enabledCount: number;
  deviceCount: number;
  sampleVolume: number;
}
export const protocolHealth = () => httpGet(`${API_DATA_BASE}/dashboard/protocol/health`);

export interface ChangeImpact {
  kind: 'driver' | 'device' | 'profile';
  entityId: number | string;
  operateTime: string;
}
export const alertChangeImpact = (days = 30, limit = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/change-impact`, { params: { days, limit } });

export interface SilentSource {
  deviceId: number | string;
  pointId: number | string;
  lastSeen: string;
  silentSeconds: number;
}
export const silentSources = (baselineDays = 7, silentMinutes = 15, limit = 50) =>
  httpGet(`${API_DATA_BASE}/dashboard/silent/sources`, {
    params: { baselineDays, silentMinutes, limit },
  });

export interface CoverageGap {
  totalPoints: number;
  missingPoints: number;
  items: Array<{ pointId: number | string; profileId: number | string }>;
}
export const coverageGap = (limit = 100) => httpGet(`${API_DATA_BASE}/dashboard/coverage/gap`, { params: { limit } });
