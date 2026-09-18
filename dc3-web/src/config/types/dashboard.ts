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

/**
 * Dashboard / event-overview payload shapes. Kept here (not inside the API
 * wrappers) so every card and `api/dashboard/*` module points at one source
 * of truth, always imported with `import type` under verbatimModuleSyntax.
 */

/** Three canonical alarm sources — point-level, device-level, driver-level. */
export type AlertSource = 'point' | 'device' | 'driver';

/** Top-level entity dimensions for top-N queries. */
export type TopDimension = 'device' | 'point' | 'driver';

/** Preset ranges supported by TimeRangeUtil on the server. */
export type RangeKey = '' | 'today' | '24h' | '7d' | '30d';

/** Time-bucket granularity for the timeseries endpoint. */
export type Granularity = 'hour' | 'day';

/**
 * Shared query shape for any endpoint that takes a rolling time window.
 * Backend reads {@code rangeKey} in preference when both are present; the
 * legacy {@code rangeHours} integer is the older callers' escape hatch.
 */
export interface TimeRangeParams {
  rangeHours?: number;
  rangeKey?: RangeKey | string;
}

/** Config-change subjects used by the Change Impact card. */
export type ChangeKind = 'driver' | 'device' | 'profile';

export interface SystemHealth {
  center: Record<string, 'up' | 'down'>;
  infra: Record<string, 'up' | 'down'>;
  drivers: {total: number; online: number};
  devices: {total: number; online: number};
}

// ---- Alert list / pagination -----------------------------------------

export interface AlertPageQuery {
  source?: AlertSource | null;
  alarmTypeFlag?: number | null;
  confirmFlag?: number | null;
  /**
   * Preset time-range selector. Backend TimeRangeUtil turns this into a
   * concrete `from` timestamp (TODAY maps to local-midnight; 24h/7d/30d are rolling windows).
   */
  rangeKey?: string | null;
  offset?: number;
  limit?: number;
  sort?: Array<{field: string; direction: 'ASC' | 'DESC'}>;
}

// ---- Topology Sankey -------------------------------------------------

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

// ---- Phase-2 insight cards ------------------------------------------

export interface FlappingSource {
  source: AlertSource;
  sourceId: string;
  eventTypeFlag: number;
  count: number;
}

export interface CorrelationPair {
  aSource: AlertSource;
  aSourceId: string;
  aEventType: number;
  bSource: AlertSource;
  bSourceId: string;
  bEventType: number;
  coCount: number;
}

export interface PeerDeviation {
  profileId: string;
  deviceId: string;
  alarmCount: number;
  peerMedian: number;
  ratio: number;
}

export interface AgingBacklog {
  under1h: number;
  h1to6: number;
  h6to24: number;
  over24h: number;
  total: number;
}

export interface MttaTrend {
  date: string;
  p50Ms: number;
  p95Ms: number;
  confirmedCount: number;
}

export interface ProtocolHealth {
  serviceName: string;
  driverCount: number;
  enabledCount: number;
  deviceCount: number;
}

export interface ChangeImpact {
  kind: ChangeKind;
  entityId: string;
  operateTime: string;
}

export interface SilentSource {
  deviceId: string;
  pointId: string;
  lastSeen: string;
  silentSeconds: number;
}

export interface CoverageGapItem {
  pointId: string;
  profileId: string;
}

export interface CoverageGap {
  totalPoints: number;
  missingPoints: number;
  items: CoverageGapItem[];
}

// ---- Home dashboard summaries ---------------------------------------

export interface StatsTodaySummary {
  today: number;
  percentChange: number;
  total: number;
}

export interface StatsTimeBucket {
  /** Start of the bucket, matching backend TimeseriesPointVO.bucket. */
  bucket: string;
  count: number;
}

export interface AlertStatsSummary {
  total: number;
  unconfirmed: number;
  deviceAlerts: number;
  driverAlerts: number;
  deviceUnconfirmed: number;
  driverUnconfirmed: number;
  todayDeviceAlarms: number;
  todayDriverAlarms: number;
  todayDeviceUnconfirmed: number;
  todayDriverUnconfirmed: number;
  sparkline24h: number[];
}

export interface DailyGrowthSummary {
  driverDailyCounts: number[];
  deviceDailyCounts: number[];
  pointDailyCounts: number[];
  profileDailyCounts: number[];
}

// ---- Alert overview cards (previously declared inline in components) ----

export interface AlertEventRow {
  id: string;
  source: AlertSource;
  sourceId: string;
  pointId?: string;
  alarmTypeFlag: number;
  confirmFlag: string;
  createTime: string;
  message?: string;
}

export interface AlertStormRow {
  source: AlertSource;
  sourceId: string;
  count: number;
}

export interface AlertTypeRow {
  type: string;
  count: number;
}

export interface AlertActivityRow {
  /** 0..6 = Sun..Sat, matching Postgres EXTRACT(DOW). */
  dow: number;
  hour: number;
  count: number;
}

export interface AlertTrendRow {
  date: string;
  source: string;
  count: number;
}

export interface AlertTopSourceRow {
  name: string;
  count: number;
}

/** Bucket shape shared by statsTop / latency / enable-breakdown endpoints. */
export interface StatsCountBucket {
  entityId?: number;
  key?: string;
  bin?: number;
  count: number;
}

export interface StreamRow {
  deviceId: string;
  pointId: string;
  driverId?: string;
  // driverName / deviceName / pointName are populated server-side via metadata
  // facades, so the feed renders the full tuple without extra lookups.
  driverName?: string;
  deviceName?: string;
  pointName?: string;
}

export interface DriverStats {
  byEnable: { key: string; count: number }[];
  byType: { key: string; count: number }[];
  byService: { key: string; count: number }[];
}

export interface DeviceStats {
  byEnable: { key: string; count: number }[];
  byProfile: { key: string; count: number }[];
  byDriver: { key: string; count: number }[];
}
