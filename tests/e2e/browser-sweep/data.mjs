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

export const baseProtectedRoutes = [
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

export function buildEntityRoutes(routeIds) {
  const routes = [];
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
  {
    name: 'Profile',
    route: '/profile',
    placeholder: 'Enter profile name',
    value: 'Demo',
    add: true,
    detail: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Device',
    route: '/device',
    placeholder: 'Enter device name',
    value: 'V_Device',
    add: true,
    importButton: true,
    detail: true,
    edit: true,
    deleteClick: true,
    paginate: true,
  },
  {
    name: 'PointValue',
    route: '/point_value',
    addDisabled: true,
    detail: true,
    edit: true,
    deleteClick: true,
    paginate: true,
  },
  {
    name: 'Settings User',
    route: '/settings/user',
    placeholder: 'Enter user name',
    value: 'dc3',
    add: true,
    detail: true,
    edit: true,
    assign: 'Assign Roles',
    deleteClick: true,
  },
  {
    name: 'Settings Role',
    route: '/settings/role',
    placeholder: 'Enter role name',
    value: 'Admin',
    add: true,
    detail: true,
    edit: true,
    assign: 'Assign Resources',
    deleteClick: true,
  },
  {
    name: 'Settings Menu',
    route: '/settings/menu',
    placeholder: 'Enter menu name',
    value: 'Home',
    add: true,
    detail: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Settings Resource',
    route: '/settings/resource',
    placeholder: 'Enter resource name',
    value: 'Device',
    add: true,
    detail: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Settings API',
    route: '/settings/api',
    placeholder: 'Enter api name',
    value: 'Controller',
    detail: true,
    paginate: true,
  },
  {
    name: 'Settings Group',
    route: '/settings/group',
    placeholder: 'Enter group name',
    value: 'Group',
    add: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Settings Label',
    route: '/settings/label',
    placeholder: 'Enter label name',
    value: 'Label',
    add: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Agentic Model Config',
    route: '/settings/agentic',
    placeholder: 'gpt-4o-mini',
    value: 'gpt',
    add: true,
    edit: true,
    deleteClick: true,
  },
  {
    name: 'Agentic Provider',
    route: '/settings/agentic/provider',
    placeholder: 'Provider name',
    value: 'openai',
    add: true,
    edit: true,
    deleteClick: true,
  },
  { name: 'Device Event', route: '/settings/event/device', paginate: true },
  { name: 'Driver Event', route: '/settings/event/driver', paginate: true },
];

export function buildDestructiveDeleteCases(routeIds) {
  return [
    {
      name: 'Profile delete',
      route: '/profile',
      placeholder: 'Enter profile name',
      listUrl: '/api/v3/manager/profile/list',
      addUrl: '/api/v3/manager/profile/add',
      nameField: 'profileName',
      seed: (name) => ({ profileName: name, profileCode: name, enableFlag: 'ENABLE', remark: 'codex e2e delete' }),
    },
    {
      name: 'Device delete',
      route: '/device',
      placeholder: 'Enter device name',
      listUrl: '/api/v3/manager/device/list',
      addUrl: '/api/v3/manager/device/add',
      nameField: 'deviceName',
      seed: (name) => ({
        deviceName: name,
        deviceCode: name,
        driverId: routeIds.driverId,
        profileId: routeIds.profileId,
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
      }),
    },
    {
      name: 'User delete',
      route: '/settings/user',
      placeholder: 'Enter user name',
      listUrl: '/api/v3/auth/user_profile/list',
      addUrl: '/api/v3/auth/user_profile/add',
      nameField: 'userName',
      seed: (name, suffix) => ({
        userName: name,
        nickName: `codex_nick_${suffix}`,
        phone: `139${String(Date.now()).slice(-8)}`,
        email: `codex_${suffix}@test.com`,
        enableFlag: 0,
      }),
    },
    {
      name: 'Role delete',
      route: '/settings/role',
      placeholder: 'Enter role name',
      listUrl: '/api/v3/auth/role/list',
      addUrl: '/api/v3/auth/role/add',
      nameField: 'roleName',
      seed: (name, suffix) => ({
        parentRoleId: 0,
        roleName: name,
        roleCode: `CODEX_${suffix}`.toUpperCase(),
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
      }),
    },
    {
      name: 'Menu delete',
      route: '/settings/menu',
      placeholder: 'Enter menu name',
      listUrl: '/api/v3/auth/menu/list',
      addUrl: '/api/v3/auth/menu/add',
      nameField: 'menuName',
      seed: (name) => ({
        parentMenuId: 0,
        menuName: name,
        menuCode: name,
        menuTypeFlag: 'COMMON',
        menuLevel: 'C1',
        menuIndex: 999,
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
        menuExt: { content: { titles: { zh_CN: name, en_US: name }, icon: 'Menu', url: '/codex' } },
      }),
    },
    {
      name: 'Resource delete',
      route: '/settings/resource',
      placeholder: 'Enter resource name',
      listUrl: '/api/v3/auth/resource/list',
      addUrl: '/api/v3/auth/resource/add',
      nameField: 'resourceName',
      seed: (name) => ({
        parentResourceId: 0,
        resourceName: name,
        resourceCode: name,
        resourceTypeFlag: 'API',
        resourceScopeFlag: 'LIST',
        entityId: routeIds.apiId,
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
      }),
    },
    {
      name: 'Group delete',
      route: '/settings/group',
      placeholder: 'Enter group name',
      listUrl: '/api/v3/manager/group/list',
      addUrl: '/api/v3/manager/group/add',
      nameField: 'groupName',
      seed: (name) => ({
        parentGroupId: 0,
        groupName: name,
        groupCode: name,
        groupTypeFlag: 'DEVICE',
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
      }),
    },
    {
      name: 'Label delete',
      route: '/settings/label',
      placeholder: 'Enter label name',
      listUrl: '/api/v3/manager/label/list',
      addUrl: '/api/v3/manager/label/add',
      nameField: 'labelName',
      seed: (name) => ({
        labelName: name,
        labelCode: name,
        enableFlag: 'ENABLE',
        remark: 'codex e2e delete',
      }),
    },
  ];
}
