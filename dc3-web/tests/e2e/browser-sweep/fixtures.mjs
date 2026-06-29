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

import {createEntity, firstRecord, idOf, uniqueName} from './support.mjs';

async function discoverRouteIds(page) {
  const [driver, profile, device, point, api, resource, menu, user, role] = await Promise.all([
    firstRecord(page, '/api/v3/manager/driver/list'),
    firstRecord(page, '/api/v3/manager/profile/list'),
    firstRecord(page, '/api/v3/manager/device/list'),
    firstRecord(page, '/api/v3/manager/point/list'),
    firstRecord(page, '/api/v3/auth/api/list'),
    firstRecord(page, '/api/v3/auth/resource/list'),
    firstRecord(page, '/api/v3/auth/menu/list'),
    firstRecord(page, '/api/v3/auth/user_profile/list'),
    firstRecord(page, '/api/v3/auth/role/list'),
  ]);

  return {
    driverId: idOf(driver),
    profileId: idOf(profile),
    deviceId: idOf(device),
    pointId: idOf(point),
    pointProfileId: point?.profileId ? String(point.profileId) : undefined,
    apiId: idOf(api),
    resourceId: idOf(resource),
    menuId: idOf(menu),
    userId: idOf(user),
    roleId: idOf(role),
  };
}

export async function ensureE2eData(page, routeIds) {
  const cleanupStack = [];
  const discovered = await discoverRouteIds(page);
  const suffix = uniqueName('route');

  const driverId =
    discovered.driverId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/manager/driver/list',
      addUrl: '/api/v3/manager/driver/add',
      deleteUrl: '/api/v3/manager/driver/delete',
      nameField: 'driverName',
      body: {
        driverName: suffix,
        driverCode: suffix,
        serviceName: suffix,
        serviceHost: '127.0.0.1',
        driverTypeFlag: 'DRIVER_CLIENT',
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  const profileId =
    discovered.profileId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/manager/profile/list',
      addUrl: '/api/v3/manager/profile/add',
      deleteUrl: '/api/v3/manager/profile/delete',
      nameField: 'profileName',
      body: {
        profileName: suffix,
        profileCode: suffix,
        profileShareFlag: 'TENANT',
        profileTypeFlag: 'USER',
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  const deviceId =
    discovered.deviceId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/manager/device/list',
      addUrl: '/api/v3/manager/device/add',
      deleteUrl: '/api/v3/manager/device/delete',
      nameField: 'deviceName',
      body: {
        deviceName: suffix,
        deviceCode: suffix,
        driverId,
        profileId,
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  const pointId =
    discovered.pointId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/manager/point/list',
      addUrl: '/api/v3/manager/point/add',
      deleteUrl: '/api/v3/manager/point/delete',
      nameField: 'pointName',
      body: {
        pointName: suffix,
        pointCode: suffix,
        pointTypeFlag: 'STRING',
        rwFlag: 'READ_WRITE',
        baseValue: 0,
        multiple: 1,
        valueDecimal: 0,
        profileId,
        unit: '',
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  const apiId =
    discovered.apiId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/auth/api/list',
      addUrl: '/api/v3/auth/api/add',
      deleteUrl: '/api/v3/auth/api/delete',
      nameField: 'apiName',
      body: {
        apiName: suffix,
        apiCode: `E2E_${suffix}`.toUpperCase(),
        serviceName: 'dc3-e2e',
        apiTypeFlag: 'POST',
        apiGroup: 'e2e',
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
        apiExt: {content: {url: '/e2e/fixture', title: suffix, remark: 'created by e2e route fixture'}},
      },
    }));

  const resourceId =
    discovered.resourceId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/auth/resource/list',
      addUrl: '/api/v3/auth/resource/add',
      deleteUrl: '/api/v3/auth/resource/delete',
      nameField: 'resourceName',
      body: {
        parentResourceId: 0,
        resourceName: suffix,
        resourceCode: suffix,
        resourceTypeFlag: 'API',
        resourceScopeFlag: 'LIST',
        entityId: apiId,
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  const menuId =
    discovered.menuId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/auth/menu/list',
      addUrl: '/api/v3/auth/menu/add',
      deleteUrl: '/api/v3/auth/menu/delete',
      nameField: 'menuName',
      body: {
        parentMenuId: 0,
        menuName: suffix,
        menuCode: suffix,
        menuTypeFlag: 'COMMON',
        menuLevel: 'C1',
        menuIndex: 999,
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
        menuExt: {content: {titles: {zh: suffix, en: suffix}, icon: 'Menu', url: '/e2e-fixture'}},
      },
    }));

  const userId =
    discovered.userId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/auth/user_profile/list',
      addUrl: '/api/v3/auth/user_profile/add',
      deleteUrl: '/api/v3/auth/user_profile/delete',
      nameField: 'userName',
      body: {
        userName: suffix,
        nickName: suffix,
        phone: `139${String(Date.now()).slice(-8)}`,
        email: `${suffix}@example.com`,
        enableFlag: 'ENABLE',
      },
    }));

  const roleId =
    discovered.roleId ||
    (await createEntity(page, cleanupStack, {
      listUrl: '/api/v3/auth/role/list',
      addUrl: '/api/v3/auth/role/add',
      deleteUrl: '/api/v3/auth/role/delete',
      nameField: 'roleName',
      body: {
        parentRoleId: 0,
        roleName: suffix,
        roleCode: `E2E_${suffix}`.toUpperCase(),
        enableFlag: 'ENABLE',
        remark: 'created by e2e route fixture',
      },
    }));

  Object.assign(routeIds, {
    driverId,
    profileId,
    deviceId,
    pointId,
    pointProfileId: discovered.pointProfileId || profileId,
    apiId,
    resourceId,
    menuId,
    userId,
    roleId,
  });

  return async () => {
    for (const clean of cleanupStack.reverse()) {
      await clean();
    }
  };
}
