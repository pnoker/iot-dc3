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

import type {
  AgingBacklog,
  AlertStatsSummary,
  DailyGrowthSummary,
  SilentSource,
  StatsTimeBucket,
  StatsTodaySummary,
  TopDimension,
  TopologyNode,
  TopologyNodeType,
  TopologyResponse,
} from '@/config/types/dashboard';

import {devices, drivers, points, profiles} from './entities';

/** Mirrors the AlertRow shape declared locally in AlertList.vue / EventTable.vue. */
interface AlertRow {
  id: string;
  source: 'point' | 'device' | 'driver';
  sourceId: string;
  pointId: string;
  eventTypeFlag: number;
  alarmLevelFlag?: number;
  confirmFlag: string;
  createTime: string;
  message?: string;
}

/** Deterministic wavy series so chart cards look alive without randomness. */
const wave = (base: number, amp: number, n: number): number[] =>
  Array.from({length: n}, (_, i) => Math.max(0, Math.round(base + Math.sin(i / 2) * amp)));

const pad = (value: number): string => String(value).padStart(2, '0');

/** Match the backend LocalDateTime JSON shape while keeping the browser's local timezone. */
const formatDateTime = (value: Date): string =>
  `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ` +
  `${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`;

const shiftedTime = (now: Date, millisecondsAgo: number): string =>
  formatDateTime(new Date(now.getTime() - millisecondsAgo));

export const statsToday: StatsTodaySummary = {today: 128, percentChange: 12.5, total: 4823};

export const alertStats: AlertStatsSummary = {
  total: 56,
  unconfirmed: 8,
  deviceAlerts: 40,
  driverAlerts: 16,
  deviceUnconfirmed: 6,
  driverUnconfirmed: 2,
  todayDeviceAlarms: 12,
  todayDriverAlarms: 4,
  todayDeviceUnconfirmed: 3,
  todayDriverUnconfirmed: 1,
  sparkline24h: wave(3, 5, 24),
};

export const dailyGrowth: DailyGrowthSummary = {
  driverDailyCounts: [6, 7, 7, 8, 8, 8, drivers.length],
  deviceDailyCounts: [12, 14, 15, 15, 16, 17, devices.length],
  pointDailyCounts: [30, 33, 35, 36, 38, 40, points.length],
  profileDailyCounts: [5, 5, 6, 6, 6, 6, profiles.length],
};

/**
 * Build complete bucket/count rows for the requested window. The old mock only
 * returned count, collapsing every G2 point onto an undefined x-axis bucket.
 */
export const statsTimeseries = (
  rangeKey = '24h',
  granularity: 'hour' | 'day' = rangeKey === '7d' || rangeKey === '30d' ? 'day' : 'hour',
  now = new Date(),
): StatsTimeBucket[] => {
  const isDaily = granularity === 'day';
  const size = rangeKey === '30d' ? 30 : rangeKey === '7d' ? 7 : rangeKey === 'today' ? now.getHours() + 1 : 24;
  const lastBucket = new Date(now);
  if (isDaily) {
    lastBucket.setHours(0, 0, 0, 0);
  } else {
    lastBucket.setMinutes(0, 0, 0);
  }
  const counts = wave(isDaily ? 420 : 18, isDaily ? 180 : 12, size);
  return counts.map((count, index) => {
    const bucket = new Date(lastBucket);
    const stepsAgo = size - index - 1;
    if (isDaily) {
      bucket.setDate(bucket.getDate() - stepsAgo);
    } else {
      bucket.setHours(bucket.getHours() - stepsAgo);
    }
    return {bucket: formatDateTime(bucket), count};
  });
};

export const systemHealth = {
  center: {auth: 'up', data: 'up', manager: 'up'},
  infra: {database: 'up', mq: 'up', gateway: 'up'},
  drivers: {total: drivers.length, online: Math.max(0, drivers.length - 1)},
  devices: {total: devices.length, online: Math.max(0, devices.length - 3)},
};

const tNode = (type: TopologyNodeType, id: unknown, name: unknown, layer: 1 | 2 | 3 | 4): TopologyNode => ({
  id: `${type}:${id}`,
  name: String(name ?? id),
  layer,
  type,
});

export const topology: TopologyResponse = (() => {
  // Mirror the backend's cardinality Top-N safeguards so the public demo does
  // not attempt to label every entity in a single Sankey viewport.
  const keptDrivers = drivers.slice(0, 10);
  const keptDriverIds = new Set(keptDrivers.map((driver) => String(driver.id)));
  const candidateDevices = devices.filter((device) => keptDriverIds.has(String(device.driverId)));
  const keptDevices = candidateDevices.slice(0, 20);
  const keptDeviceIds = new Set(keptDevices.map((device) => String(device.id)));
  const keptProfileIds = new Set(keptDevices.map((device) => String(device.profileId)));
  const keptProfiles = profiles.filter((profile) => keptProfileIds.has(String(profile.id)));
  const keptPoints = points.filter((point) => keptProfileIds.has(String(point.profileId)));

  const nodes: TopologyNode[] = [
    ...keptDrivers.map((d) => tNode('driver', d.id, d.driverName, 1)),
    ...keptDevices.map((d) => tNode('device', d.id, d.deviceName, 2)),
    ...keptProfiles.map((p) => tNode('profile', p.id, p.profileName, 3)),
    ...keptPoints.map((p) => tNode('point', p.id, p.pointName, 4)),
  ];

  const links: TopologyResponse['links'] = [
    ...keptDevices.map((d) => ({source: `driver:${d.driverId}`, target: `device:${d.id}`, value: 1})),
    ...keptDevices.map((d) => ({source: `device:${d.id}`, target: `profile:${d.profileId}`, value: 1})),
    ...keptPoints.map((p) => ({source: `profile:${p.profileId}`, target: `point:${p.id}`, value: 1})),
  ];

  for (const driver of keptDrivers) {
    const hidden = candidateDevices.filter(
      (device) => String(device.driverId) === String(driver.id) && !keptDeviceIds.has(String(device.id)),
    );
    if (hidden.length === 0) continue;
    const id = `others:device:${driver.id}`;
    nodes.push({
      id,
      name: `Others (${hidden.length})`,
      layer: 2,
      type: 'others',
      hiddenChildren: hidden.map((device) => ({
        id: `device:${device.id}`,
        name: String(device.deviceName),
        type: 'device'
      })),
    });
    links.push({source: `driver:${driver.id}`, target: id, value: hidden.length});
  }

  return {
    nodes,
    links,
    stats: {
      driverCount: drivers.length,
      deviceCount: devices.length,
      profileCount: profiles.length,
      pointCount: points.length,
    },
  };
})();

export const alertRows = (now = new Date()): AlertRow[] => [
  {
    id: '9001',
    source: 'device',
    sourceId: String(devices[0]?.id ?? 1),
    pointId: '',
    eventTypeFlag: 1,
    alarmLevelFlag: 2,
    confirmFlag: 'UNCONFIRMED',
    createTime: shiftedTime(now, 12 * 60_000),
    message: '设备离线超过 5 分钟',
  },
  {
    id: '9002',
    source: 'driver',
    sourceId: String(drivers[5]?.id ?? 6),
    pointId: '',
    eventTypeFlag: 2,
    alarmLevelFlag: 3,
    confirmFlag: 'UNCONFIRMED',
    createTime: shiftedTime(now, 47 * 60_000),
    message: '驱动连接异常',
  },
  {
    id: '9003',
    source: 'point',
    sourceId: String(points[0]?.id ?? 1),
    pointId: String(points[0]?.id ?? 1),
    eventTypeFlag: 3,
    alarmLevelFlag: 1,
    confirmFlag: 'CONFIRMED',
    createTime: shiftedTime(now, 2 * 3_600_000),
    message: '温度超过阈值上限',
  },
  {
    id: '9004',
    source: 'device',
    sourceId: String(devices[4]?.id ?? 5),
    pointId: '',
    eventTypeFlag: 1,
    alarmLevelFlag: 2,
    confirmFlag: 'CONFIRMED',
    createTime: shiftedTime(now, 4 * 3_600_000),
    message: '设备已恢复在线',
  },
  {
    id: '9005',
    source: 'point',
    sourceId: String(points[3]?.id ?? 4),
    pointId: String(points[3]?.id ?? 4),
    eventTypeFlag: 4,
    alarmLevelFlag: 1,
    confirmFlag: 'UNCONFIRMED',
    createTime: shiftedTime(now, 7 * 3_600_000),
    message: '电压低于安全范围',
  },
];

// ---- Phase-2 insight-card payloads ----------------------------------

/** Mirrors the Row shape declared locally in LiveDataFeed.vue. */
interface StreamRow {
  deviceId: string;
  pointId: string;
  driverId: string;
  driverName: string;
  deviceName: string;
  pointName: string;
  rawValue: string;
  calValue: string;
  valueType: string;
  createTime: string;
}

const driverNameOf = (id: unknown): string =>
  drivers.find((d) => String(d.id) === String(id))?.driverName ?? '';

/** Deterministic per-unit telemetry value so the live feed looks like real
 *  sensor readings rather than flat placeholders. */
const streamValue = (unit: string, i: number): { valueType: string; rawValue: string } => {
  const r = (base: number, amp: number) => (base + Math.sin(i / 1.7) * amp + (i % 5) * 0.08).toFixed(2);
  switch (unit) {
    case '℃':
      return {valueType: 'FLOAT', rawValue: r(23.4, 1.3)};
    case '%RH':
      return {valueType: 'FLOAT', rawValue: r(56.2, 3.1)};
    case 'V':
      return {valueType: 'FLOAT', rawValue: r(220.3, 1.4)};
    case 'A':
      return {valueType: 'FLOAT', rawValue: r(12.46, 0.5)};
    case 'kW':
      return {valueType: 'FLOAT', rawValue: r(3.12, 0.35)};
    case '%':
      return {valueType: 'INT', rawValue: String(72 + ((i * 7) % 27))};
    case 'm³/h':
      return {valueType: 'FLOAT', rawValue: r(18.6, 2.2)};
    case 'bar':
      return {valueType: 'FLOAT', rawValue: r(4.05, 0.18)};
    default:
      return {valueType: 'DOUBLE', rawValue: r(0.95, 0.04)};
  }
};

// Flatten every (device, point) pair sharing a profile so each feed row is a
// coherent device/point tuple the server would plausibly emit.
const devicePointPairs = devices.flatMap((device) =>
  points
    .filter((p) => String(p.profileId) === String(device.profileId))
    .map((point) => ({device, point})),
);

export const streamLatest = (now = new Date()): StreamRow[] => devicePointPairs.slice(0, 20).map(({
                                                                                                    device,
                                                                                                    point
                                                                                                  }, i) => {
  const {valueType, rawValue} = streamValue(point.unit ?? '', i);
  return {
    deviceId: String(device.id),
    pointId: String(point.id),
    driverId: String(device.driverId ?? ''),
    driverName: driverNameOf(device.driverId),
    deviceName: device.deviceName ?? '',
    pointName: point.pointName ?? '',
    rawValue,
    calValue: rawValue,
    valueType,
    createTime: shiftedTime(now, (i * 37 + 5) * 1000),
  };
});

const countBy = <T>(items: T[], keyOf: (item: T) => string): { key: string; count: number }[] => {
  const map = new Map<string, number>();
  for (const item of items) {
    const key = keyOf(item);
    map.set(key, (map.get(key) ?? 0) + 1);
  }
  return [...map.entries()].map(([key, count]) => ({key, count}));
};

export const deviceStats = {
  byEnable: countBy(devices, (d) => (d.enableFlag === 'ENABLE' ? 'ENABLED' : 'DISABLED')),
  byProfile: countBy(devices, (d) => String(d.profileId)),
  byDriver: countBy(devices, (d) => String(d.driverId)),
};

// byService is keyed by driver serviceName (e.g. dc3-driver-modbus); the
// AnalyticsTabs protocol tab strips the `dc3-driver-` prefix before drawing.
export const driverStats = {
  byEnable: countBy(drivers, (d) => (d.enableFlag === 'ENABLE' ? 'ENABLED' : 'DISABLED')),
  byType: countBy(drivers, (d) => String(d.driverTypeFlag ?? 'DRIVER_CLIENT')),
  byService: countBy(devices, (d) => {
    const drv = drivers.find((x) => String(x.id) === String(d.driverId));
    return drv?.serviceName ?? 'unknown';
  }),
};

const topRows = (ids: string[], base: number, step: number, jitter: number) =>
  ids.map((id, i) => ({entityId: id, count: Math.max(1, base - i * step + (i % 3) * jitter)}));

export const statsTop: Record<TopDimension, { entityId: string; count: number }[]> = {
  device: topRows(devices.slice(0, 10).map((d) => String(d.id)), 342, 28, 6),
  point: topRows(points.slice(0, 10).map((p) => String(p.id)), 286, 23, 7),
  driver: topRows(drivers.slice(0, 8).map((d) => String(d.id)), 524, 58, 4),
};

export const alertAging: AgingBacklog = {under1h: 24, h1to6: 13, h6to24: 6, over24h: 3, total: 46};

const silentSource = (deviceIndex: number, pointIndex: number, silentSeconds: number, now: Date): SilentSource => {
  const device = devices[deviceIndex] ?? devices[0]!;
  const profilePoints = points.filter((point) => String(point.profileId) === String(device.profileId));
  const point = profilePoints[pointIndex % profilePoints.length] ?? points[0]!;
  return {
    deviceId: String(device.id),
    pointId: String(point.id),
    lastSeen: shiftedTime(now, silentSeconds * 1000),
    silentSeconds,
  };
};

export const silentSources = (now = new Date()): SilentSource[] => [
  silentSource(1, 0, 2074, now),
  silentSource(3, 0, 1536, now),
  silentSource(5, 1, 1102, now),
  silentSource(7, 1, 3640, now),
  silentSource(9, 0, 988, now),
  silentSource(2, 2, 2752, now),
];

// 6 latency buckets: <100ms / 100-500ms / 0.5-1s / 1-5s / 5-30s / >30s.
export const statsLatency = [
  {bin: 0, count: 8420},
  {bin: 1, count: 1284},
  {bin: 2, count: 312},
  {bin: 3, count: 87},
  {bin: 4, count: 19},
  {bin: 5, count: 4},
];

// 7 (Sun..Sat) x 24 (hour) activity grid — hot on weekday business hours,
// cool overnight and on weekends, matching real IoT traffic shape.
const activityCount = (dow: number, hour: number): number => {
  const weekend = dow === 0 || dow === 6;
  const peak = !weekend && hour >= 9 && hour <= 18;
  const day = !weekend && hour >= 7 && hour <= 21;
  const base = weekend ? 5 : peak ? 56 : day ? 24 : 3;
  return Math.max(0, Math.round(base + Math.sin((hour + dow) / 2) * (peak ? 16 : 5)));
};

export const statsActivity = (rangeKey = '7d', now = new Date()) => {
  const today = now.getDay();
  const previousDay = (today + 6) % 7;
  const currentHour = now.getHours();
  const factor = rangeKey === '30d' ? 4 : rangeKey === 'today' ? 0.2 : rangeKey === '24h' ? 0.25 : 1;

  return Array.from({length: 7}, (_, dow) =>
    Array.from({length: 24}, (_, hour) => {
      const inWindow =
        rangeKey === 'today'
          ? dow === today && hour <= currentHour
          : rangeKey === '24h'
            ? (dow === today && hour <= currentHour) || (dow === previousDay && hour > currentHour)
            : true;
      return {dow, hour, count: inWindow ? Math.round(activityCount(dow, hour) * factor) : 0};
    }),
  ).flat();
};
