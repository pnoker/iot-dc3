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

export interface RouteIds {
  driverId?: string;
  profileId?: string;
  deviceId?: string;
  pointId?: string;
  pointProfileId?: string;
  apiId?: string;
  resourceId?: string;
  menuId?: string;
  groupId?: string;
  labelId?: string;
  alarmRuleId?: string;
  alarmNotifyId?: string;
  alarmMessageId?: string;
  alarmChannelId?: string;
  alarmBindId?: string;
  alarmStateId?: string;
  alarmHistoryId?: string;
  agenticModelConfigId?: string;
  agenticProviderId?: string;
  userId?: string;
  roleId?: string;
}

export const protectedRoutes = [
  '/home',
  '/driver',
  '/profile',
  '/device',
  '/point_value',
  '/settings/user',
  '/settings/role',
  '/settings/resource',
  '/settings/api',
  '/settings/menu',
  '/settings/group',
  '/settings/label',
  '/settings/alarm/rule',
  '/settings/alarm/notify',
  '/settings/alarm/message',
  '/settings/alarm/channel',
  '/settings/alarm/bind',
  '/settings/alarm/state',
  '/settings/alarm/history',
  '/settings/model/config',
  '/settings/model/provider',
  '/settings/alarm/overview',
  '/settings/alarm/device',
  '/settings/alarm/driver',
  '/settings/alarm/point',
  '/settings/event/history',
  '/settings/command/history',
  '/settings/about',
];

export const protectedRouteProbes = [
  '/driver/detail?id=e2e-auth-probe',
  '/device/detail?id=e2e-auth-probe',
  '/device/edit?id=e2e-auth-probe',
  '/profile/detail?id=e2e-auth-probe',
  '/profile/edit?id=e2e-auth-probe',
  '/point/detail?id=e2e-auth-probe',
  '/settings/api/detail?id=e2e-auth-probe',
  '/settings/resource/detail?id=e2e-auth-probe',
  '/settings/menu/detail?id=e2e-auth-probe',
  '/settings/user/detail?id=e2e-auth-probe',
  '/settings/role/detail?id=e2e-auth-probe',
  '/settings/group/detail?id=e2e-auth-probe',
  '/settings/label/detail?id=e2e-auth-probe',
  '/settings/alarm/rule/detail?id=e2e-auth-probe',
  '/settings/alarm/notify/detail?id=e2e-auth-probe',
  '/settings/alarm/message/detail?id=e2e-auth-probe',
  '/settings/alarm/channel/detail?id=e2e-auth-probe',
  '/settings/alarm/bind/detail?id=e2e-auth-probe',
  '/settings/alarm/state/detail?id=e2e-auth-probe',
  '/settings/alarm/history/detail?id=e2e-auth-probe',
  '/settings/model/config/detail?id=e2e-auth-probe',
  '/settings/model/provider/detail?id=e2e-auth-probe',
];

export const publicRoutes = ['/login', '/403', '/404', '/500'];

export function buildEntityRoutes(routeIds: RouteIds) {
  const routes: string[] = [];
  if (routeIds.driverId) routes.push(`/driver/detail?id=${routeIds.driverId}`);
  if (routeIds.deviceId) {
    routes.push(`/device/detail?id=${routeIds.deviceId}`);
    routes.push(`/device/edit?id=${routeIds.deviceId}`);
  }
  if (routeIds.profileId) {
    routes.push(`/profile/detail?id=${routeIds.profileId}`);
    routes.push(`/profile/edit?id=${routeIds.profileId}`);
  }
  if (routeIds.pointId) routes.push(`/point/detail?id=${routeIds.pointId}`);
  if (routeIds.apiId) routes.push(`/settings/api/detail?id=${routeIds.apiId}`);
  if (routeIds.resourceId) routes.push(`/settings/resource/detail?id=${routeIds.resourceId}`);
  if (routeIds.menuId) routes.push(`/settings/menu/detail?id=${routeIds.menuId}`);
  if (routeIds.groupId) routes.push(`/settings/group/detail?id=${routeIds.groupId}`);
  if (routeIds.labelId) routes.push(`/settings/label/detail?id=${routeIds.labelId}`);
  if (routeIds.alarmRuleId) routes.push(`/settings/alarm/rule/detail?id=${routeIds.alarmRuleId}`);
  if (routeIds.alarmNotifyId) routes.push(`/settings/alarm/notify/detail?id=${routeIds.alarmNotifyId}`);
  if (routeIds.alarmMessageId) routes.push(`/settings/alarm/message/detail?id=${routeIds.alarmMessageId}`);
  if (routeIds.alarmChannelId) routes.push(`/settings/alarm/channel/detail?id=${routeIds.alarmChannelId}`);
  if (routeIds.alarmBindId) routes.push(`/settings/alarm/bind/detail?id=${routeIds.alarmBindId}`);
  if (routeIds.alarmStateId) routes.push(`/settings/alarm/state/detail?id=${routeIds.alarmStateId}`);
  if (routeIds.alarmHistoryId) routes.push(`/settings/alarm/history/detail?id=${routeIds.alarmHistoryId}`);
  if (routeIds.agenticModelConfigId) {
    routes.push(`/settings/model/config/detail?id=${routeIds.agenticModelConfigId}`);
  }
  if (routeIds.agenticProviderId) {
    routes.push(`/settings/model/provider/detail?id=${routeIds.agenticProviderId}`);
  }
  if (routeIds.userId) routes.push(`/settings/user/detail?id=${routeIds.userId}`);
  if (routeIds.roleId) routes.push(`/settings/role/detail?id=${routeIds.roleId}`);
  return routes;
}

export const interactionPages = [
  {name: 'Driver', route: '/driver', placeholder: 'Enter driver name', value: 'Virtual', detail: true},
  {name: 'Profile', route: '/profile', placeholder: 'Enter profile name', value: 'Demo', add: true, detail: true},
  {
    name: 'Device',
    route: '/device',
    placeholder: 'Enter device name',
    value: 'V_Device',
    add: true,
    importButton: true,
  },
  {name: 'PointValue', route: '/point_value', addDisabled: true, paginate: true},
  {
    name: 'Settings User',
    route: '/settings/user',
    placeholder: 'Enter user name',
    value: 'dc3',
    add: true,
    enableForm: true,
  },
  {
    name: 'Settings Role',
    route: '/settings/role',
    placeholder: 'Enter role name',
    value: 'Admin',
    add: true,
    enableForm: true,
  },
  {
    name: 'Settings Menu',
    route: '/settings/menu',
    placeholder: 'Enter menu name',
    value: 'Home',
    add: true,
    enableForm: true,
  },
  {
    name: 'Settings Resource',
    route: '/settings/resource',
    placeholder: 'Enter resource name',
    value: 'Device',
    add: true,
    enableForm: true,
  },
  {name: 'Settings API', route: '/settings/api', placeholder: 'Enter api name', value: 'Controller', paginate: true},
  {
    name: 'Settings Group',
    route: '/settings/group',
    placeholder: 'Enter group name',
    value: 'Group',
    add: true,
    enableForm: true,
  },
  {
    name: 'Settings Label',
    route: '/settings/label',
    placeholder: 'Enter label name',
    value: 'Label',
    add: true,
    enableForm: true,
  },
  {
    name: 'Alarm Rule',
    route: '/settings/alarm/rule',
    placeholder: 'Search rule name',
    value: 'Temperature',
    add: true,
    enableForm: true,
  },
  {
    name: 'Alarm Notify Policy',
    route: '/settings/alarm/notify',
    placeholder: 'Search policy name',
    value: 'Operations',
    add: true,
    enableForm: true,
  },
  {
    name: 'Alarm Message Template',
    route: '/settings/alarm/message',
    placeholder: 'Search template name',
    value: 'Critical',
    add: true,
    enableForm: true,
  },
  {
    name: 'Alarm Notify Channel',
    route: '/settings/alarm/channel',
    placeholder: 'Search channel name',
    value: 'Feishu',
    add: true,
    enableForm: true,
  },
  {
    name: 'Alarm Channel Binding',
    route: '/settings/alarm/bind',
    placeholder: 'Search notify ID',
    value: '1',
    add: true,
    enableForm: true,
  },
  {name: 'Alarm Runtime State', route: '/settings/alarm/state', placeholder: 'Search rule ID', value: '1'},
  {name: 'Alarm Delivery History', route: '/settings/alarm/history', placeholder: 'Search target', value: 'ops'},
  {
    name: 'Agentic Model Config',
    route: '/settings/model/config',
    placeholder: 'gpt-4o-mini',
    value: 'gpt',
    add: true,
    enableForm: true,
  },
  {
    name: 'Agentic Provider',
    route: '/settings/model/provider',
    placeholder: 'Provider name',
    value: 'openai',
    add: true,
    enableForm: true,
  },
  {name: 'Alarm Overview', route: '/settings/alarm/overview', paginate: true},
  {name: 'Device Alarm', route: '/settings/alarm/device', paginate: true},
  {name: 'Driver Alarm', route: '/settings/alarm/driver', paginate: true},
  {name: 'Point Alarm', route: '/settings/alarm/point', paginate: true},
  {name: 'Event History', route: '/settings/event/history', paginate: true},
  {name: 'Command History', route: '/settings/command/history', paginate: true},
];
