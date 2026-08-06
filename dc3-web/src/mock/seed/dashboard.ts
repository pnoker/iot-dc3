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
  AlertStatsSummary,
  DailyGrowthSummary,
  StatsTimeBucket,
  StatsTodaySummary,
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

export const statsTimeseries: StatsTimeBucket[] = wave(18, 12, 24).map((count) => ({count}));

export const systemHealth = {
  center: {auth: 'UP', data: 'UP', manager: 'UP'},
  infra: {database: 'UP', mq: 'UP', gateway: 'UP'},
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
  const nodes: TopologyNode[] = [
    ...drivers.map((d) => tNode('driver', d.id, d.driverName, 1)),
    ...devices.map((d) => tNode('device', d.id, d.deviceName, 2)),
    ...profiles.map((p) => tNode('profile', p.id, p.profileName, 3)),
    ...points.map((p) => tNode('point', p.id, p.pointName, 4)),
  ];
  const links = [
    ...devices.map((d) => ({source: `driver:${d.driverId}`, target: `device:${d.id}`, value: 1})),
    ...devices.map((d) => ({source: `device:${d.id}`, target: `profile:${d.profileId}`, value: 1})),
    ...points.map((p) => ({source: `profile:${p.profileId}`, target: `point:${p.id}`, value: 1})),
  ];
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

export const alertRows: AlertRow[] = [
  {
    id: '9001',
    source: 'device',
    sourceId: String(devices[0]?.id ?? 1),
    pointId: '',
    eventTypeFlag: 1,
    alarmLevelFlag: 2,
    confirmFlag: 'UNCONFIRMED',
    createTime: '2026-08-01T13:50:00',
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
    createTime: '2026-08-01T13:12:00',
    message: '驱动连接异常',
  },
  {
    id: '9003',
    source: 'point',
    sourceId: String(devices[1]?.id ?? 2),
    pointId: String(points[0]?.id ?? 1),
    eventTypeFlag: 3,
    alarmLevelFlag: 1,
    confirmFlag: 'CONFIRMED',
    createTime: '2026-08-01T11:40:00',
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
    createTime: '2026-08-01T09:20:00',
    message: '设备已恢复在线',
  },
  {
    id: '9005',
    source: 'point',
    sourceId: String(devices[2]?.id ?? 3),
    pointId: String(points[4]?.id ?? 5),
    eventTypeFlag: 4,
    alarmLevelFlag: 1,
    confirmFlag: 'UNCONFIRMED',
    createTime: '2026-08-01T08:05:00',
    message: '电压低于安全范围',
  },
];
