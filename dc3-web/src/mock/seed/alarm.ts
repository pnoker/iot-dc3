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
  AlertSource,
  ChangeImpact,
  CorrelationPair,
  CoverageGap,
  FlappingSource,
  MttaTrend,
  PeerDeviation,
  ProtocolHealth,
} from '@/config/types/dashboard';

import {devices, drivers, points, profiles} from './entities';

export interface MockAlertRow {
  id: string;
  source: AlertSource;
  sourceId: string;
  pointId: string;
  eventTypeFlag: number;
  alarmLevelFlag: number;
  confirmFlag: 'CONFIRMED' | 'UNCONFIRMED';
  createTime: string;
  message: string;
}

export interface AlertTrendRow {
  date: string;
  deviceCount: number;
  driverCount: number;
  pointCount: number;
}

export interface AlertTopSourceRow {
  source: AlertSource;
  sourceId: string;
  count: number;
}

const pad = (value: number): string => String(value).padStart(2, '0');

const formatDate = (value: Date): string =>
  `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;

const formatDateTime = (value: Date): string =>
  `${formatDate(value)} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`;

const shifted = (now: Date, millisecondsAgo: number): string =>
  formatDateTime(new Date(now.getTime() - millisecondsAgo));

const sourceRows = (source: AlertSource): Array<Record<string, any> & { id: string }> => {
  if (source === 'driver') return drivers;
  if (source === 'point') {
    return points.filter((point) => /温度|电压/.test(String(point.pointName)));
  }
  return devices;
};

const messageFor = (source: AlertSource, index: number, entity: Record<string, any>): string => {
  if (source === 'driver') return '驱动连接异常';
  if (source === 'point') {
    return String(entity.pointName ?? '').includes('电压') ? '电压低于安全范围' : '温度超过阈值上限';
  }
  return index % 4 === 3 ? '设备已恢复在线' : '设备离线超过 5 分钟';
};

const buildSourceAlerts = (
  source: AlertSource,
  count: number,
  todayCount: number,
  unconfirmedCount: number,
  idOffset: number,
  now: Date,
): MockAlertRow[] => {
  const entities = sourceRows(source);
  return Array.from({length: count}, (_, index) => {
    const entity = entities[(index * 3 + idOffset) % entities.length]!;
    const millisecondsAgo =
      index < todayCount
        ? (index * 23 + idOffset + 5) * 60_000
        : (1 + ((index - todayCount) % 29)) * 86_400_000 + ((index * 47 + idOffset) % 20) * 3_600_000;
    return {
      id: String(10_000 + idOffset * 100 + index),
      source,
      sourceId: String(entity.id),
      pointId: source === 'point' ? String(entity.id) : '',
      eventTypeFlag: index % 5,
      alarmLevelFlag: index % 7 === 0 ? 3 : index % 3 === 0 ? 2 : 1,
      confirmFlag: index < unconfirmedCount ? 'UNCONFIRMED' : 'CONFIRMED',
      createTime: shifted(now, millisecondsAgo),
      message: messageFor(source, index, entity),
    };
  });
};

/**
 * One coherent alarm population backs totals, recent rows, filters and list
 * pages. Counts intentionally match alertStats in dashboard.ts:
 * 40 device / 16 driver, with 6 / 2 unconfirmed and 12 / 4 today.
 */
export const alertRecords = (now = new Date()): MockAlertRow[] =>
  [
    ...buildSourceAlerts('device', 40, 12, 6, 1, now),
    ...buildSourceAlerts('driver', 16, 4, 2, 2, now),
    ...buildSourceAlerts('point', 24, 8, 5, 3, now),
  ].sort((a, b) => b.createTime.localeCompare(a.createTime));

const waveCount = (index: number, base: number, amplitude: number, period: number): number =>
  Math.max(0, Math.round(base + Math.sin(index / period) * amplitude + (index % 4) * 0.7));

export const alertTrendRows = (days = 30, now = new Date()): AlertTrendRow[] => {
  const size = Math.max(1, Math.min(90, Math.round(days)));
  const end = new Date(now);
  end.setHours(0, 0, 0, 0);
  return Array.from({length: size}, (_, index) => {
    const date = new Date(end);
    date.setDate(end.getDate() - (size - index - 1));
    return {
      date: formatDate(date),
      deviceCount: waveCount(index, 15, 7, 2.3),
      driverCount: waveCount(index + 2, 6, 3, 2.8),
      pointCount: waveCount(index + 4, 10, 5, 2.1),
    };
  });
};

export const alertTopSourceRows = (limit = 10): AlertTopSourceRow[] => {
  const candidates: AlertTopSourceRow[] = [
    ...devices.slice(0, 5).map((row, index) => ({
      source: 'device' as const,
      sourceId: row.id,
      count: 186 - index * 19
    })),
    ...drivers.slice(0, 4).map((row, index) => ({
      source: 'driver' as const,
      sourceId: row.id,
      count: 142 - index * 17
    })),
    ...points.slice(0, 4).map((row, index) => ({source: 'point' as const, sourceId: row.id, count: 118 - index * 13})),
  ];
  return candidates.sort((a, b) => b.count - a.count).slice(0, Math.max(1, limit));
};

export const alertActivityRows = (days = 7): Array<{ dow: number; hour: number; count: number }> => {
  const factor = Math.max(0.4, Math.min(4, days / 7));
  return Array.from({length: 7}, (_, dow) =>
    Array.from({length: 24}, (_, hour) => {
      const weekday = dow >= 1 && dow <= 5;
      const shiftChange = weekday && ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19));
      const daytime = hour >= 6 && hour <= 22;
      const base = shiftChange ? 22 : daytime ? (weekday ? 10 : 7) : 2;
      return {dow, hour, count: Math.round((base + Math.abs(Math.sin((dow + hour) / 2)) * 6) * factor)};
    }),
  ).flat();
};

export const alertTypeRows = () => [
  {type: 'device-offline', count: 168},
  {type: 'device-online', count: 121},
  {type: 'driver-offline', count: 74},
  {type: 'driver-online', count: 63},
  {type: 'device-alarm', count: 52},
  {type: 'driver-state-flip', count: 31},
];

export const alertStormRows = (minCount = 10, limit = 10): AlertTopSourceRow[] =>
  alertTopSourceRows(limit).map((row, index) => ({
    ...row,
    count: Math.max(row.count, minCount + (limit - index) * 7),
  }));

export const alertFlappingRows = (minCount = 5, limit = 20): FlappingSource[] => {
  const rows: FlappingSource[] = [
    ...devices.slice(0, 7).map((row, index) => ({
      source: 'device' as const,
      sourceId: row.id,
      eventTypeFlag: index % 4,
      count: minCount + 18 - index,
    })),
    ...drivers.slice(0, 5).map((row, index) => ({
      source: 'driver' as const,
      sourceId: row.id,
      eventTypeFlag: (index + 1) % 4,
      count: minCount + 13 - index,
    })),
  ];
  return rows.sort((a, b) => b.count - a.count).slice(0, Math.max(1, limit));
};

export const alertCorrelationRows = (hours = 24, limit = 15): CorrelationPair[] => {
  const factor = Math.max(0.4, Math.min(4, hours / 24));
  const rows: CorrelationPair[] = Array.from({length: 15}, (_, index) => {
    const driver = drivers[index % drivers.length]!;
    const device = devices[(index * 2 + 1) % devices.length]!;
    const point = points[(index * 3 + 2) % points.length]!;
    return index % 3 === 0
      ? {
        aSource: 'driver',
        aSourceId: driver.id,
        aEventType: 2,
        bSource: 'device',
        bSourceId: device.id,
        bEventType: 1,
        coCount: Math.round((48 - index * 2) * factor),
      }
      : {
        aSource: 'device',
        aSourceId: device.id,
        aEventType: 1,
        bSource: 'point',
        bSourceId: point.id,
        bEventType: 3,
        coCount: Math.round((43 - index * 2) * factor),
      };
  });
  return rows.slice(0, Math.max(1, limit));
};

export const alertPeerDeviationRows = (days = 7): PeerDeviation[] =>
  devices.slice(0, 12).map((device, index) => {
    const factor = Math.max(0.5, Math.min(4, days / 7));
    const peerMedian = Math.max(1, Math.round((2 + (index % 4)) * factor));
    const alarmCount = peerMedian * (3 + (index % 4)) + (index % 3);
    return {
      profileId: String(device.profileId),
      deviceId: device.id,
      alarmCount,
      peerMedian,
      ratio: Number((alarmCount / peerMedian).toFixed(1)),
    };
  });

export const alertMttaRows = (days = 30, now = new Date()): MttaTrend[] => {
  const size = Math.max(7, Math.min(90, Math.round(days)));
  const end = new Date(now);
  end.setHours(0, 0, 0, 0);
  return Array.from({length: size}, (_, index) => {
    const date = new Date(end);
    date.setDate(end.getDate() - (size - index - 1));
    const p50Ms = Math.round(310_000 + Math.sin(index / 3) * 95_000 + (index % 5) * 8_000);
    return {
      date: formatDate(date),
      p50Ms,
      p95Ms: Math.round(p50Ms * (2.5 + (index % 3) * 0.25)),
      confirmedCount: 8 + (index % 11),
    };
  });
};

export const alertChangeImpactRows = (days = 30, limit = 30, now = new Date()): ChangeImpact[] => {
  const rows: ChangeImpact[] = Array.from({length: 18}, (_, index) => {
    const kind = (['driver', 'device', 'profile'] as const)[index % 3]!;
    const pool = kind === 'driver' ? drivers : kind === 'device' ? devices : profiles;
    const entity = pool[(index * 2) % pool.length]!;
    return {
      kind,
      entityId: entity.id,
      operateTime: shifted(now, (index * 4 + 1) * 3_600_000),
    };
  });
  const from = now.getTime() - Math.max(1, days) * 86_400_000;
  return rows
    .filter((row) => new Date(row.operateTime.replace(' ', 'T')).getTime() >= from)
    .slice(0, Math.max(1, limit));
};

export const protocolHealthRows = (): ProtocolHealth[] =>
  drivers.slice(0, 12).map((driver) => ({
    serviceName: String(driver.serviceName),
    driverCount: 1,
    enabledCount: driver.enableFlag === 'ENABLE' ? 1 : 0,
    deviceCount: devices.filter((device) => String(device.driverId) === String(driver.id)).length,
  }));

export const coverageGapReport = (limit = 100): CoverageGap => {
  const selected = [5, 8, 12, 17, 23, 31, 38, 44, 51, 57]
    .map((index) => points[index])
    .filter((point): point is NonNullable<typeof point> => Boolean(point))
    .slice(0, Math.max(1, limit));
  return {
    totalPoints: points.length,
    missingPoints: selected.length,
    items: selected.map((point) => ({pointId: point.id, profileId: String(point.profileId)})),
  };
};
