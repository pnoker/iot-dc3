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

import type {Dictionary} from '@/config/types';
import type {DeviceRecord, DriverRecord, PointRecord, ProfileRecord} from '@/config/types/manager';

/** Stable timestamps so the demo does not regenerate on every load. */
const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';

interface DriverDef {
  name: string;
  code: string;
  service: string;
  host: string;
  type?: string;
  enabled?: boolean;
}

const driverDefs: DriverDef[] = [
  {name: 'Modbus-TCP Driver', code: 'modbus-tcp', service: 'dc3-driver-modbus', host: '127.0.0.1:8201'},
  {name: 'OPC-UA Driver', code: 'opc-ua', service: 'dc3-driver-opc-ua', host: '127.0.0.1:8202'},
  {name: 'MQTT Driver', code: 'mqtt', service: 'dc3-driver-mqtt', host: '127.0.0.1:8203'},
  {name: 'BACnet Driver', code: 'bacnet', service: 'dc3-driver-bacnet', host: '127.0.0.1:8204'},
  {name: 'S7 PLC Driver', code: 's7', service: 'dc3-driver-s7', host: '127.0.0.1:8205'},
  {name: 'CANopen Driver', code: 'canopen', service: 'dc3-driver-canopen', host: '127.0.0.1:8206', enabled: false},
  {name: 'IEC104 Driver', code: 'iec104', service: 'dc3-driver-iec104', host: '127.0.0.1:8207'},
  {name: 'OPC-DA Driver', code: 'opcda', service: 'dc3-driver-opcda', host: '127.0.0.1:8208'},
];

interface PointDef {
  name: string;
  type: string;
  rw: string;
  unit: string;
}

interface ProfileDef {
  name: string;
  code: string;
  points: PointDef[];
}

const profileDefs: ProfileDef[] = [
  {
    name: '温湿度传感器',
    code: 'TH-SENSOR',
    points: [
      {name: '温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '湿度', type: 'FLOAT', rw: 'READ_ONLY', unit: '%RH'},
      {name: '电池电量', type: 'INT', rw: 'READ_ONLY', unit: '%'},
    ],
  },
  {
    name: '电力监测仪表',
    code: 'POWER-METER',
    points: [
      {name: '电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '电流', type: 'FLOAT', rw: 'READ_ONLY', unit: 'A'},
      {name: '有功功率', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kW'},
      {name: '功率因数', type: 'FLOAT', rw: 'READ_ONLY', unit: ''},
    ],
  },
  {
    name: '智能水表',
    code: 'WATER-METER',
    points: [
      {name: '累计流量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'm³'},
      {name: '瞬时流量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'L/h'},
    ],
  },
  {
    name: '边缘网关',
    code: 'EDGE-GATEWAY',
    points: [
      {name: 'CPU使用率', type: 'FLOAT', rw: 'READ_ONLY', unit: '%'},
      {name: '内存使用率', type: 'FLOAT', rw: 'READ_ONLY', unit: '%'},
      {name: '活跃连接数', type: 'INT', rw: 'READ_ONLY', unit: ''},
    ],
  },
  {
    name: 'PLC控制器',
    code: 'PLC-CTRL',
    points: [
      {name: '运行状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
      {name: '启动指令', type: 'BOOLEAN', rw: 'WRITE_ONLY', unit: ''},
      {name: '运行速度', type: 'INT', rw: 'READ_WRITE', unit: 'rpm'},
    ],
  },
  {
    name: '中央空调',
    code: 'HVAC-UNIT',
    points: [
      {name: '设定温度', type: 'FLOAT', rw: 'READ_WRITE', unit: '℃'},
      {name: '回风温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '风机转速', type: 'INT', rw: 'READ_WRITE', unit: 'rpm'},
    ],
  },
];

export const drivers: DriverRecord[] = driverDefs.map((d, i) => ({
  id: String(1001 + i),
  driverName: d.name,
  driverCode: d.code,
  serviceName: d.service,
  serviceHost: d.host,
  driverTypeFlag: 'DRIVER_CLIENT',
  enableFlag: d.enabled === false ? 'DISABLE' : 'ENABLE',
  createTime: CREATED,
  operateTime: UPDATED,
}));

export const profiles: ProfileRecord[] = profileDefs.map((p, i) => ({
  id: String(2001 + i),
  profileName: p.name,
  profileCode: p.code,
  profileShareFlag: 'TENANT',
  profileTypeFlag: 'USER',
  enableFlag: i === 4 ? 'DISABLE' : 'ENABLE',
  createTime: CREATED,
  operateTime: UPDATED,
}));

export const points: PointRecord[] = profileDefs.flatMap((def, pi) =>
  def.points.map((pt, qi) => ({
    id: String(5001 + pi * 10 + qi),
    pointName: pt.name,
    pointCode: `${def.code}-${pt.name}`,
    pointTypeFlag: pt.type,
    rwFlag: pt.rw,
    unit: pt.unit,
    baseValue: 0,
    multiple: 1,
    valueDecimal: 2,
    profileId: profiles[pi]!.id,
    enableFlag: 'ENABLE',
    createTime: CREATED,
    operateTime: UPDATED,
  })),
);

// 2–3 devices per profile, each linked to a driver + its profile.
export const devices: DeviceRecord[] = profiles.flatMap((profile, pi) => {
  const count = 2 + (pi % 2);
  return Array.from({length: count}, (_, k) => {
    const index = pi * 3 + k;
    const driver = drivers[(pi + k) % drivers.length];
    return {
      id: String(3001 + index),
      deviceName: `${profile.profileName}-${String(k + 1).padStart(2, '0')}`,
      deviceCode: `DEV-${String(index + 1).padStart(3, '0')}`,
      driverId: driver!.id,
      profileId: profile.id,
      enableFlag: index % 4 === 0 ? 'DISABLE' : 'ENABLE',
      createTime: CREATED,
      operateTime: UPDATED,
    } as DeviceRecord;
  });
});

const toDictionary = (type: string, label: string, value: string): Dictionary => ({
  type,
  label,
  value,
  disabled: false,
  expand: false,
  children: [],
});

export const driverDictionary: Dictionary[] = drivers.map((d) =>
  toDictionary('driver', d.driverName || d.id, d.id),
);
export const profileDictionary: Dictionary[] = profiles.map((p) =>
  toDictionary('profile', p.profileName || p.id, p.id),
);
export const deviceDictionary: Dictionary[] = devices.map((d) =>
  toDictionary('device', d.deviceName || d.id, d.id),
);
// Flat point list; the dictionary handler narrows by parentId (deviceId→profileId).
export const pointDictionary: Dictionary[] = points.map((p) =>
  toDictionary('point', p.pointName || p.id, p.id),
);
