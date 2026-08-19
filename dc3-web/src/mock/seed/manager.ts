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

import type {ApiRecord, ResourceRecord} from '@/config/types/auth';
import type {Attribute} from '@/config/types';
import type {CommandParamRecord, CommandRecord} from '@/config/types/command';
import type {EventParamRecord, EventRecord} from '@/config/types/event';
import type {GroupRecord, LabelRecord} from '@/config/types/manager';

import {devices, drivers, points, profiles} from './entities';

/** Stable timestamps so the demo does not regenerate on every load. */
const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';

/** Common {content:{keep:''}} envelope shared by every `*Ext` field. */
const KEEP = {content: {keep: ''}};

// ─── Group ───────────────────────────────────────────────────────────
// Two-layer tree: a root per entity type with child folders underneath.
// parentGroupId === 0 marks a root; child rows reference the root id.
export const groups: GroupRecord[] = [
  {
    id: '1',
    parentGroupId: '0',
    groupTypeFlag: 'DRIVER',
    groupName: '驱动分组',
    groupCode: 'DRIVER-GROUP',
    groupLevel: 1,
    groupIndex: 1,
    enableFlag: 'ENABLE',
    remark: '驱动根分组',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '2',
    parentGroupId: '1',
    groupTypeFlag: 'DRIVER',
    groupName: 'Modbus 驱动组',
    groupCode: 'DRIVER-MODBUS',
    groupLevel: 2,
    groupIndex: 1,
    enableFlag: 'ENABLE',
    remark: 'Modbus 系列驱动',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '3',
    parentGroupId: '0',
    groupTypeFlag: 'DEVICE',
    groupName: '设备分组',
    groupCode: 'DEVICE-GROUP',
    groupLevel: 1,
    groupIndex: 2,
    enableFlag: 'ENABLE',
    remark: '设备根分组',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '4',
    parentGroupId: '3',
    groupTypeFlag: 'DEVICE',
    groupName: '温湿度设备组',
    groupCode: 'DEVICE-TH',
    groupLevel: 2,
    groupIndex: 1,
    enableFlag: 'ENABLE',
    remark: '温湿度传感器设备',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5',
    parentGroupId: '3',
    groupTypeFlag: 'DEVICE',
    groupName: '电力设备组',
    groupCode: 'DEVICE-POWER',
    groupLevel: 2,
    groupIndex: 2,
    enableFlag: 'DISABLE',
    remark: '电力监测仪表设备',
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Label ───────────────────────────────────────────────────────────
// Status colors: online #67C23A, offline #909399, alarm #F56C6C.
export const labels: LabelRecord[] = [
  {
    id: '1',
    labelName: '在线',
    labelCode: 'ONLINE',
    labelColor: '#67C23A',
    entityTypeFlag: 'DEVICE',
    enableFlag: 'ENABLE',
    remark: '设备在线状态',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '2',
    labelName: '离线',
    labelCode: 'OFFLINE',
    labelColor: '#909399',
    entityTypeFlag: 'DEVICE',
    enableFlag: 'ENABLE',
    remark: '设备离线状态',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '3',
    labelName: '告警',
    labelCode: 'ALARM',
    labelColor: '#F56C6C',
    entityTypeFlag: 'POINT',
    enableFlag: 'ENABLE',
    remark: '位号越限告警',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '4',
    labelName: '已关注',
    labelCode: 'STARRED',
    labelColor: '#67C23A',
    entityTypeFlag: 'PROFILE',
    enableFlag: 'ENABLE',
    remark: '关注模板',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5',
    labelName: '维护中',
    labelCode: 'MAINTAIN',
    labelColor: '#909399',
    entityTypeFlag: 'DRIVER',
    enableFlag: 'DISABLE',
    remark: '驱动维护状态',
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Api ─────────────────────────────────────────────────────────────
// Covers GET/POST/PUT/DELETE across dc3-manager / dc3-auth / dc3-data and
// the device/driver/point/profile api groups.
export const apis: ApiRecord[] = [
  {
    id: '1',
    apiName: '驱动分页查询',
    apiCode: 'driver_list',
    serviceName: 'dc3-manager',
    apiTypeFlag: 'GET',
    apiGroup: 'driver',
    enableFlag: 'ENABLE',
    remark: '分页查询驱动列表',
    apiExt: {content: {url: '/driver/list', title: '驱动列表', remark: '分页查询驱动'}},
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '2',
    apiName: '新增设备',
    apiCode: 'device_add',
    serviceName: 'dc3-manager',
    apiTypeFlag: 'POST',
    apiGroup: 'device',
    enableFlag: 'ENABLE',
    remark: '新增设备',
    apiExt: {content: {url: '/device/add', title: '新增设备', remark: '创建一台新设备'}},
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '3',
    apiName: '更新位号',
    apiCode: 'point_update',
    serviceName: 'dc3-data',
    apiTypeFlag: 'PUT',
    apiGroup: 'point',
    enableFlag: 'ENABLE',
    remark: '更新位号配置',
    apiExt: {content: {url: '/point/update', title: '更新位号', remark: '修改位号基础配置'}},
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '4',
    apiName: '删除模板',
    apiCode: 'profile_delete',
    serviceName: 'dc3-manager',
    apiTypeFlag: 'DELETE',
    apiGroup: 'profile',
    enableFlag: 'ENABLE',
    remark: '删除物模型模板',
    apiExt: {content: {url: '/profile/delete', title: '删除模板', remark: '按 id 删除模板'}},
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5',
    apiName: '令牌校验',
    apiCode: 'token_check',
    serviceName: 'dc3-auth',
    apiTypeFlag: 'POST',
    apiGroup: 'auth',
    enableFlag: 'ENABLE',
    remark: '校验访问令牌',
    apiExt: {content: {url: '/auth/token/check', title: '令牌校验', remark: '校验并解析访问令牌'}},
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Resource ────────────────────────────────────────────────────────
// Root node (parentResourceId=0, entityId='0') plus one entity resource per
// type/scope. Ids start at 5001 to align with the auth seed roleResourceBind.
export const resources: ResourceRecord[] = [
  {
    id: '5001',
    parentResourceId: '0',
    resourceName: '根资源',
    resourceCode: 'ROOT',
    serviceName: 'dc3-manager',
    resourceTypeFlag: 'MENU',
    resourceScopeFlag: 'LIST',
    entityId: '0',
    resourceExt: {},
    enableFlag: 'ENABLE',
    remark: '资源树根节点',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5002',
    parentResourceId: '5001',
    resourceName: '驱动新增',
    resourceCode: 'driver_add',
    serviceName: 'dc3-manager',
    resourceTypeFlag: 'DRIVER',
    resourceScopeFlag: 'ADD',
    entityId: drivers[0]!.id, // 1001 Modbus-TCP
    resourceExt: {},
    enableFlag: 'ENABLE',
    remark: '新增驱动资源',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5003',
    parentResourceId: '5001',
    resourceName: '设备查询',
    resourceCode: 'device_list',
    serviceName: 'dc3-manager',
    resourceTypeFlag: 'DEVICE',
    resourceScopeFlag: 'LIST',
    entityId: devices[0]!.id, // 3001
    resourceExt: {},
    enableFlag: 'ENABLE',
    remark: '查询设备资源',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5004',
    parentResourceId: '5001',
    resourceName: '位号更新',
    resourceCode: 'point_update',
    serviceName: 'dc3-data',
    resourceTypeFlag: 'POINT',
    resourceScopeFlag: 'UPDATE',
    entityId: points[0]!.id, // 5001 Temperature
    resourceExt: {},
    enableFlag: 'ENABLE',
    remark: '更新位号资源',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '5005',
    parentResourceId: '5001',
    resourceName: '模板删除',
    resourceCode: 'profile_delete',
    serviceName: 'dc3-manager',
    resourceTypeFlag: 'PROFILE',
    resourceScopeFlag: 'DELETE',
    entityId: profiles[0]!.id, // 2001 Temperature and humidity sensor
    resourceExt: {},
    enableFlag: 'ENABLE',
    remark: '删除模板资源',
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Command ─────────────────────────────────────────────────────────
// Ids start at 9001. commandTypeFlag mirrors CommandTypeEnum
// (CUSTOM/CONFIG/ACTION); callTypeFlag mirrors CallTypeEnum (SYNC/ASYNC).
export const commands: CommandRecord[] = [
  {
    id: '9001',
    commandName: '重启网关',
    commandCode: 'gateway_reboot',
    commandTypeFlag: 'ACTION',
    callTypeFlag: 'ASYNC',
    timeout: 30,
    commandExt: KEEP,
    profileId: profiles[3]!.id, // 2004 Edge gateway
    enableFlag: 'ENABLE',
    remark: '远程重启边缘网关',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9002',
    commandName: '启动PLC',
    commandCode: 'plc_start',
    commandTypeFlag: 'ACTION',
    callTypeFlag: 'SYNC',
    timeout: 10,
    commandExt: KEEP,
    profileId: profiles[4]!.id, // 2005 PLC controller
    enableFlag: 'ENABLE',
    remark: '启动 PLC 运行',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9003',
    commandName: '设定运行速度',
    commandCode: 'plc_set_speed',
    commandTypeFlag: 'CONFIG',
    callTypeFlag: 'SYNC',
    timeout: 10,
    commandExt: KEEP,
    profileId: profiles[4]!.id, // 2005 PLC controller
    enableFlag: 'ENABLE',
    remark: '下发 PLC 运行速度',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9004',
    commandName: '设定温度',
    commandCode: 'hvac_set_temp',
    commandTypeFlag: 'CONFIG',
    callTypeFlag: 'SYNC',
    timeout: 15,
    commandExt: KEEP,
    profileId: profiles[5]!.id, // 2006 Central air conditioner
    enableFlag: 'ENABLE',
    remark: '下发空调设定温度',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9005',
    commandName: '远程抄表',
    commandCode: 'meter_read',
    commandTypeFlag: 'CUSTOM',
    callTypeFlag: 'ASYNC',
    timeout: 60,
    commandExt: KEEP,
    profileId: profiles[2]!.id, // 2003 Smart water meter
    enableFlag: 'ENABLE',
    remark: '远程读取水表累计流量',
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Command Param ───────────────────────────────────────────────────
// Ids start at 9101. 1–2 params per command; paramDirectionFlag mirrors
// ParamDirectionTypeEnum (INPUT/OUTPUT).
export const commandParams: CommandParamRecord[] = [
  {
    id: '9101',
    paramName: '延迟秒数',
    paramCode: 'delay',
    paramDirectionFlag: 'INPUT',
    paramTypeFlag: 'INT',
    requiredFlag: false,
    defaultValue: '5',
    paramExt: KEEP,
    commandId: '9001',
    enableFlag: 'ENABLE',
  },
  {
    id: '9102',
    paramName: '执行结果',
    paramCode: 'result',
    paramDirectionFlag: 'OUTPUT',
    paramTypeFlag: 'STRING',
    requiredFlag: false,
    defaultValue: '',
    paramExt: KEEP,
    commandId: '9001',
    enableFlag: 'ENABLE',
  },
  {
    id: '9103',
    paramName: '启动模式',
    paramCode: 'mode',
    paramDirectionFlag: 'INPUT',
    paramTypeFlag: 'BOOLEAN',
    requiredFlag: true,
    defaultValue: 'true',
    paramExt: KEEP,
    commandId: '9002',
    enableFlag: 'ENABLE',
  },
  {
    id: '9104',
    paramName: '目标速度',
    paramCode: 'speed',
    paramDirectionFlag: 'INPUT',
    paramTypeFlag: 'INT',
    requiredFlag: true,
    defaultValue: '0',
    paramExt: KEEP,
    commandId: '9003',
    enableFlag: 'ENABLE',
  },
  {
    id: '9105',
    paramName: '目标温度',
    paramCode: 'temperature',
    paramDirectionFlag: 'INPUT',
    paramTypeFlag: 'FLOAT',
    requiredFlag: true,
    defaultValue: '25',
    paramExt: KEEP,
    commandId: '9004',
    enableFlag: 'ENABLE',
  },
  {
    id: '9106',
    paramName: '累计读数',
    paramCode: 'reading',
    paramDirectionFlag: 'OUTPUT',
    paramTypeFlag: 'FLOAT',
    requiredFlag: false,
    defaultValue: '',
    paramExt: KEEP,
    commandId: '9005',
    enableFlag: 'ENABLE',
  },
];

// ─── Event ───────────────────────────────────────────────────────────
// Ids start at 9501. eventTypeFlag mirrors EventTypeFlagEnum subset
// (ALERT/INFO); eventLevelFlag mirrors EventLevelEnum (LOW/MEDIUM/HIGH).
export const events: EventRecord[] = [
  {
    id: '9501',
    eventName: '温度超限告警',
    eventCode: 'temp_over',
    eventTypeFlag: 'ALERT',
    eventLevelFlag: 'HIGH',
    eventExt: KEEP,
    profileId: profiles[0]!.id, // 2001 Temperature and humidity sensor
    enableFlag: 'ENABLE',
    remark: '温度超过阈值上限',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9502',
    eventName: '电池电量低',
    eventCode: 'battery_low',
    eventTypeFlag: 'ALERT',
    eventLevelFlag: 'MEDIUM',
    eventExt: KEEP,
    profileId: profiles[0]!.id, // 2001 Temperature and humidity sensor
    enableFlag: 'ENABLE',
    remark: '电池电量低于告警阈值',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9503',
    eventName: '通信中断',
    eventCode: 'comm_lost',
    eventTypeFlag: 'ALERT',
    eventLevelFlag: 'HIGH',
    eventExt: KEEP,
    profileId: profiles[3]!.id, // 2004 Edge gateway
    enableFlag: 'ENABLE',
    remark: '网关通信链路中断',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9504',
    eventName: '登录通知',
    eventCode: 'login_info',
    eventTypeFlag: 'INFO',
    eventLevelFlag: 'LOW',
    eventExt: KEEP,
    profileId: profiles[3]!.id, // 2004 Edge gateway
    enableFlag: 'ENABLE',
    remark: '网关登录上线通知',
    createTime: CREATED,
    operateTime: UPDATED,
  },
  {
    id: '9505',
    eventName: '过流告警',
    eventCode: 'overcurrent',
    eventTypeFlag: 'ALERT',
    eventLevelFlag: 'HIGH',
    eventExt: KEEP,
    profileId: profiles[1]!.id, // 2002 Power monitoring meter
    enableFlag: 'ENABLE',
    remark: '电流超过安全范围',
    createTime: CREATED,
    operateTime: UPDATED,
  },
];

// ─── Event Param ─────────────────────────────────────────────────────
// Ids start at 9601. Each event carries the value/threshold it reports.
export const eventParams: EventParamRecord[] = [
  {
    id: '9601',
    paramName: '温度值',
    paramCode: 'temperature',
    paramTypeFlag: 'FLOAT',
    paramExt: KEEP,
    eventId: '9501',
    enableFlag: 'ENABLE',
  },
  {
    id: '9602',
    paramName: '阈值',
    paramCode: 'threshold',
    paramTypeFlag: 'FLOAT',
    paramExt: KEEP,
    eventId: '9501',
    enableFlag: 'ENABLE',
  },
  {
    id: '9603',
    paramName: '电量',
    paramCode: 'battery',
    paramTypeFlag: 'INT',
    paramExt: KEEP,
    eventId: '9502',
    enableFlag: 'ENABLE',
  },
  {
    id: '9604',
    paramName: '电流值',
    paramCode: 'current',
    paramTypeFlag: 'FLOAT',
    paramExt: KEEP,
    eventId: '9505',
    enableFlag: 'ENABLE',
  },
];

// ─── Attribute ───────────────────────────────────────────────────────
// Ids start at 8001. Modbus-flavour driver/point attributes.
// `name` is the technical key, `attributeName` the Chinese display name,
// `attributeCode` the form identifier consumed by device edit.
export const attributes: Attribute[] = [
  {
    id: '8001',
    name: 'host',
    attributeName: '主机地址',
    attributeCode: 'host',
    attributeTypeFlag: 'STRING',
    defaultValue: '127.0.0.1',
    remark: 'Modbus TCP 主机地址',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
  {
    id: '8002',
    name: 'port',
    attributeName: '端口号',
    attributeCode: 'port',
    attributeTypeFlag: 'INT',
    defaultValue: '502',
    remark: 'Modbus TCP 端口',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
  {
    id: '8003',
    name: 'unitId',
    attributeName: '从站地址',
    attributeCode: 'unitId',
    attributeTypeFlag: 'BYTE',
    defaultValue: '1',
    remark: 'Modbus 从站 ID',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
  {
    id: '8004',
    name: 'address',
    attributeName: '寄存器地址',
    attributeCode: 'address',
    attributeTypeFlag: 'INT',
    defaultValue: '0',
    remark: 'Modbus 寄存器起始地址',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
  {
    id: '8005',
    name: 'enabled',
    attributeName: '使能开关',
    attributeCode: 'enabled',
    attributeTypeFlag: 'BOOLEAN',
    defaultValue: 'true',
    remark: '采集使能',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
  {
    id: '8006',
    name: 'factor',
    attributeName: '系数倍率',
    attributeCode: 'factor',
    attributeTypeFlag: 'FLOAT',
    defaultValue: '1',
    remark: '原始值缩放系数',
    attributeExt: KEEP,
    enableFlag: 'ENABLE',
  },
];

// ─── Attribute Config ────────────────────────────────────────────────
// Ids start at 30001. Unifies the four `*_attribute_config` tables:
//   - driver: deviceId only
//   - point:  deviceId + pointId
//   - command: deviceId + commandId
//   - event:  deviceId + eventId
// Device ids reference the entities seed so profile bindings stay consistent
// (e.g. devices[10]=3013 PLC/profile 2005, devices[12]=3016 HVAC/profile 2006).
interface AttributeConfigRecord {
  id: string;
  deviceId: string;
  attributeId: string;
  configValue: string;
  pointId?: string;
  commandId?: string;
  eventId?: string;

  [key: string]: unknown;
}

export const attributeConfigs: AttributeConfigRecord[] = [
  // driver_attribute_config (device-level driver attributes)
  {
    id: '30001',
    deviceId: devices[0]!.id, // 3001
    attributeId: '8001', // host
    configValue: '192.168.1.100',
  },
  {
    id: '30002',
    deviceId: devices[0]!.id, // 3001
    attributeId: '8002', // port
    configValue: '502',
  },
  {
    id: '30003',
    deviceId: devices[1]!.id, // 3002
    attributeId: '8003', // unitId
    configValue: '2',
  },
  // point_attribute_config (point-level attributes)
  {
    id: '30004',
    deviceId: devices[0]!.id, // 3001
    pointId: points[0]!.id, // 5001 Temperature
    attributeId: '8004', // address
    configValue: '0',
  },
  {
    id: '30005',
    deviceId: devices[0]!.id, // 3001
    pointId: points[0]!.id, // 5001 Temperature
    attributeId: '8006', // factor
    configValue: '0.1',
  },
  {
    id: '30006',
    deviceId: devices[0]!.id, // 3001
    pointId: points[1]!.id, // 5002 Humidity
    attributeId: '8004', // address
    configValue: '1',
  },
  // command_attribute_config (command-level attributes)
  {
    id: '30007',
    deviceId: devices[10]!.id, // 3013 PLC (profile 2005)
    commandId: '9003', // plc_set_speed
    attributeId: '8004', // address
    configValue: '10',
  },
  {
    id: '30008',
    deviceId: devices[12]!.id, // 3016 HVAC (profile 2006)
    commandId: '9004', // hvac_set_temp
    attributeId: '8004', // address
    configValue: '20',
  },
  // event_attribute_config (event-level attributes)
  {
    id: '30009',
    deviceId: devices[0]!.id, // 3001 (profile 2001)
    eventId: '9501', // temp_over
    attributeId: '8004', // address
    configValue: '0',
  },
  {
    id: '30010',
    deviceId: devices[2]!.id, // 3004 (profile 2002)
    eventId: '9505', // overcurrent
    attributeId: '8004', // address
    configValue: '5',
  },
];
