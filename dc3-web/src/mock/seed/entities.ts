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
  {name: 'Modbus-RTU Driver', code: 'modbus-rtu', service: 'dc3-driver-modbus-rtu', host: '127.0.0.1:8209'},
  {name: 'DLT645 Driver', code: 'dlt645', service: 'dc3-driver-dlt645', host: '127.0.0.1:8210'},
  {name: 'IEC61850 Driver', code: 'iec61850', service: 'dc3-driver-iec61850', host: '127.0.0.1:8211'},
  {
    name: 'BACnet/IP Driver',
    code: 'bacnet-ip',
    service: 'dc3-driver-bacnet-ip',
    host: '127.0.0.1:8212',
    enabled: false
  },
  {name: 'MQTT-SN Driver', code: 'mqtt-sn', service: 'dc3-driver-mqtt-sn', host: '127.0.0.1:8213'},
  {name: 'LoRaWAN Driver', code: 'lorawan', service: 'dc3-driver-lorawan', host: '127.0.0.1:8214'},
  {name: 'Zigbee Driver', code: 'zigbee', service: 'dc3-driver-zigbee', host: '127.0.0.1:8215'},
  {name: 'CoAP Driver', code: 'coap', service: 'dc3-driver-coap', host: '127.0.0.1:8216', enabled: false},
  {name: 'HJ212 Driver', code: 'hj212', service: 'dc3-driver-hj212', host: '127.0.0.1:8217'},
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
  {
    name: '智能电表',
    code: 'SMART-ELECTRIC-METER',
    points: [
      {name: '组合有功电能', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kWh'},
      {name: '正向有功电能', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kWh'},
      {name: '电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '电流', type: 'FLOAT', rw: 'READ_ONLY', unit: 'A'},
      {name: '频率', type: 'FLOAT', rw: 'READ_ONLY', unit: 'Hz'},
    ],
  },
  {
    name: '智能燃气表',
    code: 'GAS-METER',
    points: [
      {name: '累计流量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'm³'},
      {name: '工况流量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'm³/h'},
      {name: '标况流量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'm³/h'},
      {name: '压力', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kPa'},
      {name: '温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
    ],
  },
  {
    name: '光伏逆变器',
    code: 'PV-INVERTER',
    points: [
      {name: '日发电量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kWh'},
      {name: '累计发电量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kWh'},
      {name: '输出功率', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kW'},
      {name: '输入电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '机内温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '运行状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
    ],
  },
  {
    name: 'UPS电源',
    code: 'UPS-POWER',
    points: [
      {name: '输入电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '输出电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '负载率', type: 'FLOAT', rw: 'READ_ONLY', unit: '%'},
      {name: '电池容量', type: 'FLOAT', rw: 'READ_ONLY', unit: '%'},
      {name: '运行状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
      {name: '旁路开关', type: 'BOOLEAN', rw: 'READ_WRITE', unit: ''},
    ],
  },
  {
    name: '光伏组串',
    code: 'PV-STRING',
    points: [
      {name: '组串电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '组串电流', type: 'FLOAT', rw: 'READ_ONLY', unit: 'A'},
      {name: '组串功率', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kW'},
    ],
  },
  {
    name: '交流充电桩',
    code: 'CHARGING-PILE',
    points: [
      {name: '充电状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
      {name: '充电功率', type: 'FLOAT', rw: 'READ_WRITE', unit: 'kW'},
      {name: '累计充电量', type: 'FLOAT', rw: 'READ_ONLY', unit: 'kWh'},
      {name: '输出电压', type: 'FLOAT', rw: 'READ_ONLY', unit: 'V'},
      {name: '输出电流', type: 'FLOAT', rw: 'READ_ONLY', unit: 'A'},
      {name: '启停指令', type: 'BOOLEAN', rw: 'WRITE_ONLY', unit: ''},
    ],
  },
  {
    name: '螺杆式空压机',
    code: 'AIR-COMPRESSOR',
    points: [
      {name: '排气压力', type: 'FLOAT', rw: 'READ_ONLY', unit: 'MPa'},
      {name: '排气温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '运行状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
      {name: '累计运行时间', type: 'INT', rw: 'READ_ONLY', unit: 'h'},
      {name: '启停指令', type: 'BOOLEAN', rw: 'WRITE_ONLY', unit: ''},
    ],
  },
  {
    name: '冷库机组',
    code: 'COLD-STORAGE',
    points: [
      {name: '库内温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '设定温度', type: 'FLOAT', rw: 'READ_WRITE', unit: '℃'},
      {name: '蒸发温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '冷凝温度', type: 'FLOAT', rw: 'READ_ONLY', unit: '℃'},
      {name: '压缩机状态', type: 'BOOLEAN', rw: 'READ_ONLY', unit: ''},
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
  enableFlag: i === 4 || i === 10 ? 'DISABLE' : 'ENABLE',
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

// 3–6 devices per profile, each linked to a driver + its profile.
export const devices: DeviceRecord[] = profiles.flatMap((profile, pi) => {
  const count = 3 + (pi % 4);
  return Array.from({length: count}, (_, k) => {
    const index = pi * 6 + k;
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
