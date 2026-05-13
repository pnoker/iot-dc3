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
  userId?: string;
  roleId?: string;
}

export const protectedRoutes = [
  '/home',
  '/home/application',
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
  '/settings/agentic',
  '/settings/agentic/provider',
  '/settings/event',
  '/settings/event/device',
  '/settings/event/driver',
  '/settings/about',
];

export const protectedRouteProbes = [
  '/driver/detail?id=e2e-auth-probe',
  '/device/detail?id=e2e-auth-probe',
  '/device/edit?id=e2e-auth-probe',
  '/profile/detail?id=e2e-auth-probe',
  '/profile/edit?id=e2e-auth-probe',
  '/point/detail?id=e2e-auth-probe',
  '/point/edit?id=e2e-auth-probe&profileId=e2e-auth-probe',
  '/settings/api/detail?id=e2e-auth-probe',
  '/settings/resource/detail?id=e2e-auth-probe',
  '/settings/menu/detail?id=e2e-auth-probe',
  '/settings/user/detail?id=e2e-auth-probe',
  '/settings/role/detail?id=e2e-auth-probe',
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
  if (routeIds.pointId && routeIds.pointProfileId) {
    routes.push(`/point/edit?id=${routeIds.pointId}&profileId=${routeIds.pointProfileId}`);
  }
  if (routeIds.apiId) routes.push(`/settings/api/detail?id=${routeIds.apiId}`);
  if (routeIds.resourceId) routes.push(`/settings/resource/detail?id=${routeIds.resourceId}`);
  if (routeIds.menuId) routes.push(`/settings/menu/detail?id=${routeIds.menuId}`);
  if (routeIds.userId) routes.push(`/settings/user/detail?id=${routeIds.userId}`);
  if (routeIds.roleId) routes.push(`/settings/role/detail?id=${routeIds.roleId}`);
  return routes;
}

export const interactionPages = [
  { name: 'Driver', route: '/driver', placeholder: 'Enter driver name', value: 'Virtual', detail: true },
  { name: 'Profile', route: '/profile', placeholder: 'Enter profile name', value: 'Demo', add: true, detail: true },
  {
    name: 'Device',
    route: '/device',
    placeholder: 'Enter device name',
    value: 'V_Device',
    add: true,
    importButton: true,
  },
  { name: 'PointValue', route: '/point_value', addDisabled: true, paginate: true },
  { name: 'Settings User', route: '/settings/user', placeholder: 'Enter user name', value: 'dc3', add: true },
  { name: 'Settings Role', route: '/settings/role', placeholder: 'Enter role name', value: 'Admin', add: true },
  { name: 'Settings Menu', route: '/settings/menu', placeholder: 'Enter menu name', value: 'Home', add: true },
  {
    name: 'Settings Resource',
    route: '/settings/resource',
    placeholder: 'Enter resource name',
    value: 'Device',
    add: true,
  },
  { name: 'Settings API', route: '/settings/api', placeholder: 'Enter api name', value: 'Controller', paginate: true },
  { name: 'Settings Group', route: '/settings/group', placeholder: 'Enter group name', value: 'Group', add: true },
  { name: 'Settings Label', route: '/settings/label', placeholder: 'Enter label name', value: 'Label', add: true },
  { name: 'Agentic Model Config', route: '/settings/agentic', placeholder: 'gpt-4o-mini', value: 'gpt', add: true },
  {
    name: 'Agentic Provider',
    route: '/settings/agentic/provider',
    placeholder: 'Provider name',
    value: 'openai',
    add: true,
  },
  { name: 'Device Event', route: '/settings/event/device', paginate: true },
  { name: 'Driver Event', route: '/settings/event/driver', paginate: true },
];
