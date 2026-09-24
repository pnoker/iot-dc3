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
 * Backend reads `rangeKey` in preference when both are present; the
 * legacy `rangeHours` integer is the older callers' escape hatch.
 */
export interface TimeRangeParams {
  rangeHours?: number;
  rangeKey?: RangeKey | string;
}

/** Config-change subjects used by the Change Impact card. */
export type ChangeKind = 'driver' | 'device' | 'profile';

/**
 * SystemHealth data contract.
 */
export interface SystemHealth {
  center: Record<string, 'up' | 'down'>;
  infra: Record<string, 'up' | 'down'>;
  drivers: {total: number; online: number};
  devices: {total: number; online: number};
}

// ---- Alert list / pagination -----------------------------------------

/**
 * AlertPageQuery data contract.
 */
export interface AlertPageQuery {
  source?: AlertSource | null;
  /** Device id when `source` is 'device' (device-scoped alert listing). */
  sourceId?: string | null;
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

/**
 * Topology mode union.
 */
export type TopologyMode = 'cardinality' | 'volume';
/**
 * Topology node type union.
 */
export type TopologyNodeType = 'driver' | 'device' | 'profile' | 'point' | 'others';

/**
 * TopologyHiddenChild data contract.
 */
export interface TopologyHiddenChild {
  id: string;
  name: string;
  type: 'driver' | 'device' | 'point';
}

/**
 * Topology node.
 */
export interface TopologyNode {
  id: string;
  name: string;
  layer: 1 | 2 | 3 | 4;
  type: TopologyNodeType;
  hiddenChildren?: TopologyHiddenChild[];
}

/**
 * TopologyLink data contract.
 */
export interface TopologyLink {
  source: string;
  target: string;
  value: number;
}

/**
 * TopologyStats data contract.
 */
export interface TopologyStats {
  driverCount: number;
  deviceCount: number;
  profileCount: number;
  pointCount: number;
  /** Human range label ("24h" / "7d" / …) — present only in volume mode. */
  rangeLabel?: string | null;
}

/**
 * TopologyResponse data contract.
 */
export interface TopologyResponse {
  nodes: TopologyNode[];
  links: TopologyLink[];
  stats: TopologyStats;
}

// ---- Phase-2 insight cards ------------------------------------------

/**
 * FlappingSource data contract.
 */
export interface FlappingSource {
  source: AlertSource;
  sourceId: string;
  eventTypeFlag: number;
  count: number;
}

/**
 * CorrelationPair data contract.
 */
export interface CorrelationPair {
  aSource: AlertSource;
  aSourceId: string;
  aEventType: number;
  bSource: AlertSource;
  bSourceId: string;
  bEventType: number;
  coCount: number;
}

/**
 * PeerDeviation data contract.
 */
export interface PeerDeviation {
  profileId: string;
  deviceId: string;
  alarmCount: number;
  peerMedian: number;
  ratio: number;
}

/**
 * AgingBacklog data contract.
 */
export interface AgingBacklog {
  under1h: number;
  h1to6: number;
  h6to24: number;
  over24h: number;
  total: number;
}

/**
 * MttaTrend data contract.
 */
export interface MttaTrend {
  date: string;
  p50Ms: number;
  p95Ms: number;
  confirmedCount: number;
}

/**
 * ProtocolHealth data contract.
 */
export interface ProtocolHealth {
  serviceName: string;
  driverCount: number;
  enabledCount: number;
  deviceCount: number;
}

/**
 * ChangeImpact data contract.
 */
export interface ChangeImpact {
  kind: ChangeKind;
  entityId: string;
  operateTime: string;
}

/**
 * SilentSource data contract.
 */
export interface SilentSource {
  deviceId: string;
  pointId: string;
  lastSeen: string;
  silentSeconds: number;
}

/**
 * CoverageGapItem data contract.
 */
export interface CoverageGapItem {
  pointId: string;
  profileId: string;
}

/**
 * CoverageGap data contract.
 */
export interface CoverageGap {
  totalPoints: number;
  missingPoints: number;
  items: CoverageGapItem[];
}

// ---- Home dashboard summaries ---------------------------------------

/**
 * StatsTodaySummary data contract.
 */
export interface StatsTodaySummary {
  today: number;
  percentChange: number;
  total: number;
}

/**
 * StatsTimeBucket data contract.
 */
export interface StatsTimeBucket {
  /** Start of the bucket, matching backend TimeseriesPointVO.bucket. */
  bucket: string;
  count: number;
}

/**
 * AlertStatsSummary data contract.
 */
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

/**
 * DailyGrowthSummary data contract.
 */
export interface DailyGrowthSummary {
  driverDailyCounts: number[];
  deviceDailyCounts: number[];
  pointDailyCounts: number[];
  profileDailyCounts: number[];
}

// ---- Alert overview cards (previously declared inline in components) ----

/**
 * AlertEventRow data contract.
 */
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

/**
 * AlertStormRow data contract.
 */
export interface AlertStormRow {
  source: AlertSource;
  sourceId: string;
  count: number;
}

/**
 * AlertTypeRow data contract.
 */
export interface AlertTypeRow {
  type: string;
  count: number;
}

/**
 * AlertActivityRow data contract.
 */
export interface AlertActivityRow {
  /** 0..6 = Sun..Sat, matching Postgres EXTRACT(DOW). */
  dow: number;
  hour: number;
  count: number;
}

/**
 * AlertTrendRow data contract.
 */
export interface AlertTrendRow {
  date: string;
  source: string;
  count: number;
}

/**
 * AlertTopSourceRow data contract.
 */
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

/**
 * StreamRow data contract.
 */
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

/**
 * DriverStats data contract.
 */
export interface DriverStats {
  byEnable: { key: string; count: number }[];
  byType: { key: string; count: number }[];
  byService: { key: string; count: number }[];
}

/**
 * DeviceStats data contract.
 */
export interface DeviceStats {
  byEnable: { key: string; count: number }[];
  byProfile: { key: string; count: number }[];
  byDriver: { key: string; count: number }[];
}

// ---- Device-scoped dashboard ------------------------------------------

/**
 * DeviceTimeseriesPoint data contract.
 * `bucket` is a server-rendered LocalDateTime string.
 */
export interface DeviceTimeseriesPoint {
  bucket: string;
  count: number;
}

/**
 * DeviceLatencyBucket data contract.
 * One of six fixed latency bins (0..5).
 */
export interface DeviceLatencyBucket {
  bin: number;
  count: number;
}

/**
 * DeviceActivityCell data contract.
 * One of 168 weekly cells; `dow` 0 = Sunday.
 */
export interface DeviceActivityCell {
  dow: number;
  hour: number;
  count: number;
}

/**
 * DeviceQuality data contract.
 */
export interface DeviceQuality {
  totalSamples: number;
  numericSamples: number;
  nonNumericSamples: number;
  numericRatio: number;
  latestSeen: string | null;
}

/**
 * DeviceStreamRow data contract.
 */
export interface DeviceStreamRow {
  deviceId: string;
  pointId: string;
  driverId: string;
  deviceName: string;
  pointName: string;
  driverName: string;
  rawValue: string;
  calValue: string;
  valueType: string;
  createTime: string;
}

/**
 * DeviceTopPoint data contract.
 */
export interface DeviceTopPoint {
  entityId: string;
  count: number;
}

/**
 * DeviceCoverageGap data contract.
 */
export interface DeviceCoverageGap {
  totalPoints: number;
  missingPoints: number;
  items: CoverageGapItem[];
}

/**
 * DeviceSilentSource data contract.
 */
export interface DeviceSilentSource {
  deviceId: string;
  pointId: string;
  lastSeen: string;
  silentSeconds: number;
}

/**
 * DeviceAlertTrendPoint data contract.
 */
export interface DeviceAlertTrendPoint {
  date: string;
  deviceCount: number;
  driverCount: number;
}

/**
 * DeviceStatusDetail data contract.
 */
export interface DeviceStatusDetail {
  deviceId: string;
  status: string;
  lastHeartbeatTime: string | null;
  timeoutSeconds: number | null;
  expireTime: string | null;
}

// ---- Point-scoped dashboard (位号看板) ---------------------------------
// Payload shapes of the point data dashboard, mirroring the backend
// PointValueDashboardVO / PointAlertProfileVO contracts.

/**
 * PointDashboardTrendBucket data contract.
 * One bucket of the value trend band; `from` is an ISO instant string.
 */
export interface PointDashboardTrendBucket {
  from: string;
  min: number | null;
  max: number | null;
  avg: number | null;
  sampleCount: number;
}

/**
 * PointDashboardHourVolume data contract.
 * Sample volume of one hour; `hourStart` is an ISO instant string.
 */
export interface PointDashboardHourVolume {
  hourStart: string;
  count: number;
}

/**
 * PointDashboardValueBin data contract.
 * One bin of the numeric value histogram; `to` is null for the open-ended top bin.
 */
export interface PointDashboardValueBin {
  from: number | null;
  to: number | null;
  count: number;
}

/**
 * PointDashboardIntervalBin data contract.
 * One bin of the sampling-interval histogram; `toMs` is null for the open-ended top bin.
 */
export interface PointDashboardIntervalBin {
  fromMs: number;
  toMs: number | null;
  count: number;
}

/**
 * PointDashboardGap data contract.
 * One collection gap between consecutive samples; `from`/`to` are ISO instant strings.
 */
export interface PointDashboardGap {
  from: string;
  to: string;
  durationMs: number;
}

/**
 * PointDashboardHourAverage data contract.
 * One hour of the typical day curve (hourOfDay 0 through 23).
 */
export interface PointDashboardHourAverage {
  hourOfDay: number;
  avg: number | null;
}

/**
 * PointDashboardStats data contract.
 * Window-level stats of the raw sample walk.
 */
export interface PointDashboardStats {
  sampleCount: number;
  min: number | null;
  max: number | null;
  avg: number | null;
  medianIntervalMs: number | null;
  /** True when the walk hit the raw-cap before exhausting the window. */
  truncated: boolean;
}

/**
 * PointValueDashboard data contract.
 * Everything the point detail dashboard renders in one payload.
 */
export interface PointValueDashboard {
  trend: PointDashboardTrendBucket[];
  hourlyVolume: PointDashboardHourVolume[];
  valueHistogram: PointDashboardValueBin[];
  intervalHistogram: PointDashboardIntervalBin[];
  gaps: PointDashboardGap[];
  typicalDay: PointDashboardHourAverage[];
  stats: PointDashboardStats | null;
}

/**
 * PointAlertDaily data contract.
 * One day of the point-scoped alarm trend.
 */
export interface PointAlertDaily {
  date: string;
  count: number;
}

/**
 * PointAlertProfile data contract.
 * Alarm profile of a single point: type distribution and daily counts.
 */
export interface PointAlertProfile {
  typeDistribution: AlertTypeRow[];
  dailyTrend: PointAlertDaily[];
}
