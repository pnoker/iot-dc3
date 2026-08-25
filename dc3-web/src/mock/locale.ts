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

import type {TopologyResponse} from '@/config/types/dashboard';
import i18n from '@/config/i18n';

import {devices, drivers, points, profiles} from './seed/entities';

export type MockEntityKind = 'driver' | 'device' | 'profile' | 'point';

type MockRow = Record<string, any>;

const profileNames: Record<string, string> = {
  'TH-SENSOR': 'Temperature & Humidity Sensor',
  'POWER-METER': 'Power Meter',
  'WATER-METER': 'Smart Water Meter',
  'EDGE-GATEWAY': 'Edge Gateway',
  'PLC-CTRL': 'PLC Controller',
  'HVAC-UNIT': 'HVAC Unit',
  'SMART-ELECTRIC-METER': 'Smart Electricity Meter',
  'GAS-METER': 'Smart Gas Meter',
  'PV-INVERTER': 'PV Inverter',
  'UPS-POWER': 'UPS Power Supply',
  'PV-STRING': 'PV String',
  'CHARGING-PILE': 'AC Charging Station',
  'AIR-COMPRESSOR': 'Screw Air Compressor',
  'COLD-STORAGE': 'Cold Storage Unit',
};

const pointNames: Record<string, string> = {
  '温度': 'Temperature',
  '湿度': 'Humidity',
  '电池电量': 'Battery Level',
  '电压': 'Voltage',
  '电流': 'Current',
  '有功功率': 'Active Power',
  '功率因数': 'Power Factor',
  '累计流量': 'Total Flow',
  '瞬时流量': 'Instantaneous Flow',
  'CPU使用率': 'CPU Usage',
  '内存使用率': 'Memory Usage',
  '活跃连接数': 'Active Connections',
  '运行状态': 'Operating Status',
  '启动指令': 'Start Command',
  '运行速度': 'Operating Speed',
  '设定温度': 'Set Temperature',
  '回风温度': 'Return Air Temperature',
  '风机转速': 'Fan Speed',
  '组合有功电能': 'Combined Active Energy',
  '正向有功电能': 'Forward Active Energy',
  '频率': 'Frequency',
  '工况流量': 'Operating Flow',
  '标况流量': 'Standard Flow',
  '压力': 'Pressure',
  '日发电量': 'Daily Energy',
  '累计发电量': 'Total Energy',
  '输出功率': 'Output Power',
  '输入电压': 'Input Voltage',
  '机内温度': 'Internal Temperature',
  '输出电压': 'Output Voltage',
  '负载率': 'Load Rate',
  '电池容量': 'Battery Capacity',
  '旁路开关': 'Bypass Switch',
  '组串电压': 'String Voltage',
  '组串电流': 'String Current',
  '组串功率': 'String Power',
  '充电状态': 'Charging Status',
  '充电功率': 'Charging Power',
  '累计充电量': 'Total Charged Energy',
  '输出电流': 'Output Current',
  '启停指令': 'Start/Stop Command',
  '排气压力': 'Discharge Pressure',
  '排气温度': 'Discharge Temperature',
  '累计运行时间': 'Total Runtime',
  '库内温度': 'Storage Temperature',
  '蒸发温度': 'Evaporation Temperature',
  '冷凝温度': 'Condensation Temperature',
  '压缩机状态': 'Compressor Status',
};

const alertMessages: Record<string, string> = {
  '设备离线超过 5 分钟': 'Device has been offline for more than 5 minutes',
  '驱动连接异常': 'Driver connection error',
  '温度超过阈值上限': 'Temperature exceeded the upper threshold',
  '设备已恢复在线': 'Device is back online',
  '电压低于安全范围': 'Voltage is below the safe range',
};

const byId = (rows: MockRow[]): Map<string, MockRow> =>
  new Map(rows.map((row) => [String(row.id), row]));

const sourceRows: Record<MockEntityKind, Map<string, MockRow>> = {
  driver: byId(drivers),
  device: byId(devices),
  profile: byId(profiles),
  point: byId(points),
};

export const currentMockLocale = (): 'en' | 'zh' =>
  String(i18n.global.locale.value).toLowerCase().startsWith('zh') ? 'zh' : 'en';

const localizeDriver = (row: MockRow, locale: 'en' | 'zh'): MockRow => {
  if (locale === 'en') return {...row};
  const name = String(row.driverName ?? '');
  return {...row, driverName: name.replace(/ Driver$/, ' 驱动')};
};

const localizeProfile = (row: MockRow, locale: 'en' | 'zh'): MockRow => {
  if (locale === 'zh') return {...row};
  const name = profileNames[String(row.profileCode ?? '')];
  return name ? {...row, profileName: name} : {...row};
};

const localizePoint = (row: MockRow, locale: 'en' | 'zh'): MockRow => {
  if (locale === 'zh') return {...row};
  const name = pointNames[String(row.pointName ?? '')];
  return name ? {...row, pointName: name} : {...row};
};

const localizeDevice = (row: MockRow, locale: 'en' | 'zh'): MockRow => {
  if (locale === 'zh') return {...row};
  const profile = sourceRows.profile.get(String(row.profileId ?? ''));
  const profileName = profile && profileNames[String(profile.profileCode ?? '')];
  if (!profileName) return {...row};
  const suffix = String(row.deviceName ?? '').match(/-(\d{2})$/)?.[1];
  return {...row, deviceName: suffix ? `${profileName}-${suffix}` : profileName};
};

export const localizeEntity = <T extends MockRow | undefined>(kind: MockEntityKind, row: T): T => {
  if (!row) return row;
  const locale = currentMockLocale();
  const localized =
    kind === 'driver'
      ? localizeDriver(row, locale)
      : kind === 'device'
        ? localizeDevice(row, locale)
        : kind === 'profile'
          ? localizeProfile(row, locale)
          : localizePoint(row, locale);
  return localized as T;
};

export const localizeEntities = <T extends MockRow>(kind: MockEntityKind, rows: T[]): T[] =>
  rows.map((row) => localizeEntity(kind, row));

const localizedName = (kind: MockEntityKind, id: string, fallback: string): string => {
  const row = sourceRows[kind].get(id);
  if (!row) return fallback;
  const localized = localizeEntity(kind, row);
  return String(localized[`${kind}Name`] ?? fallback);
};

export const localizeAlertRows = <T extends { message?: string }>(rows: T[]): T[] => {
  if (currentMockLocale() === 'zh') return rows.map((row) => ({...row}));
  return rows.map((row) => ({
    ...row,
    message: row.message ? (alertMessages[row.message] ?? row.message) : row.message,
  }));
};

export const localizeStreamRows = <T extends MockRow>(rows: T[]): T[] =>
  rows.map((row) => ({
    ...row,
    driverName: localizedName('driver', String(row.driverId), String(row.driverName ?? '')),
    deviceName: localizedName('device', String(row.deviceId), String(row.deviceName ?? '')),
    pointName: localizedName('point', String(row.pointId), String(row.pointName ?? '')),
  }));

export const localizeTopology = (payload: TopologyResponse): TopologyResponse => {
  const locale = currentMockLocale();
  return {
    ...payload,
    nodes: payload.nodes.map((node) => {
      if (node.type === 'others') {
        return {
          ...node,
          name: `${locale === 'zh' ? '其他' : 'Others'} (${node.hiddenChildren?.length ?? 0})`,
          hiddenChildren: node.hiddenChildren?.map((child) => ({
            ...child,
            name: localizedName(child.type, child.id.split(':').at(-1) ?? '', child.name),
          })),
        };
      }
      return {
        ...node,
        name: localizedName(node.type, node.id.split(':').at(-1) ?? '', node.name),
      };
    }),
  };
};
