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

/**
 * Dashboard / event-overview payload shapes. Keeping these here (not in
 * api/dashboard.ts alongside the fetch functions) follows the project-wide
 * convention described in CLAUDE.md: "config/entity/" is the interface-only
 * module, always imported with `import type` under verbatimModuleSyntax.
 *
 * <p>Shared primitives (AlertSource, RangeKey) live here too so every
 * card / API wrapper points at the same union instead of re-declaring
 * `'device' | 'driver'` inline.</p>
 */

/** Two canonical event sources — device-level events vs driver-level events. */
export type AlertSource = 'device' | 'driver';

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

// ---- Alert list / pagination -----------------------------------------

export interface AlertPageQuery {
  source?: AlertSource | null;
  eventTypeFlag?: number | null;
  confirmFlag?: number | null;
  rangeHours?: number | null;
  /**
   * Preferred time-range selector. Backend TimeRangeUtil turns this into a
   * concrete `from` timestamp (TODAY maps to local-midnight; 24h/7d/30d are
   * rolling windows). When both rangeKey and rangeHours are set, rangeKey wins.
   */
  rangeKey?: string | null;
  current?: number;
  size?: number;
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
  sourceId: number | string;
  eventTypeFlag: number;
  count: number;
}

export interface CorrelationPair {
  aSource: AlertSource;
  aSourceId: number | string;
  aEventType: number;
  bSource: AlertSource;
  bSourceId: number | string;
  bEventType: number;
  coCount: number;
}

export interface PeerDeviation {
  profileId: number | string;
  deviceId: number | string;
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
  entityId: number | string;
  operateTime: string;
}

export interface SilentSource {
  deviceId: number | string;
  pointId: number | string;
  lastSeen: string;
  silentSeconds: number;
}

export interface CoverageGapItem {
  pointId: number | string;
  profileId: number | string;
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
  driver: number[];
  device: number[];
  point: number[];
}
