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

import { expect, type Page } from '@playwright/test';
import JSONBigInt from 'json-bigint';
import type { RouteIds } from './routes';

export interface PageHealth {
  pageErrors: string[];
  consoleErrors: string[];
  badResponses: Array<{ status: number; url: string; body: string }>;
  businessApiRequests: string[];
}

interface ApiResult<T = unknown> {
  status: number;
  data: {
    ok?: boolean;
    code?: number;
    message?: string;
    data?: T;
  };
}

interface EntitySeed {
  listUrl: string;
  addUrl: string;
  deleteUrl: string;
  nameField: string;
  body: Record<string, unknown>;
}

export interface E2eDataContext {
  routeIds: RouteIds;
  cleanup: () => Promise<void>;
}

const JSONBigIntStr = JSONBigInt({ storeAsString: true });

export function isBusinessApi(url: string) {
  return url.includes('/api/v3/');
}

export async function waitForAppSettled(page: Page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {});
  await page.waitForLoadState('networkidle', { timeout: 6000 }).catch(() => {});
  await page.waitForTimeout(300);
}

export async function login(page: Page) {
  await page.goto('/#/login', { waitUntil: 'domcontentloaded' });
  await waitForAppSettled(page);

  const loginButton = page.getByRole('button', { name: 'Login' });
  if (await loginButton.count()) {
    await loginButton.click();
    await page.waitForURL((url) => !url.hash.includes('/login'), { timeout: 15_000 }).catch(() => {});
  }

  await waitForAppSettled(page);
  await expect(page).not.toHaveURL(/\/login/);
}

export async function apiPost<T = unknown>(
  page: Page,
  url: string,
  body: Record<string, unknown> = {},
  params: Record<string, string | number | boolean | undefined> = {}
) {
  const response = await page.evaluate(
    async ({ requestUrl, requestBody, requestParams }) => {
      const decodeStorage = (key: string) => {
        const raw = localStorage.getItem(key);
        if (!raw) return undefined;
        return JSON.parse(atob(raw)).content;
      };
      const target = new URL(requestUrl, window.location.origin);
      Object.entries(requestParams).forEach(([key, value]) => {
        if (value !== undefined) {
          target.searchParams.set(key, String(value));
        }
      });
      const targetUrl =
        target.origin === window.location.origin ? `${target.pathname}${target.search}` : target.toString();
      const headers: Record<string, string> = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      };
      const tenant = decodeStorage('X-Auth-Tenant');
      const login = decodeStorage('X-Auth-Login');
      const token = decodeStorage('X-Auth-Token');
      if (tenant) headers['X-Auth-Tenant'] = tenant;
      if (login) headers['X-Auth-Login'] = login;
      if (token) headers['X-Auth-Token'] = JSON.stringify(token);

      const response = await fetch(targetUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
      });
      const text = await response.text();
      return { status: response.status, text };
    },
    { requestUrl: url, requestBody: body, requestParams: params }
  );

  let data: unknown;
  try {
    data = JSONBigIntStr.parse(response.text);
  } catch {
    data = response.text;
  }

  return { status: response.status, data } as ApiResult<T>;
}

export async function apiGet<T = unknown>(
  page: Page,
  url: string,
  params: Record<string, string | number | boolean | undefined> = {}
) {
  const response = await page.evaluate(
    async ({ requestUrl, requestParams }) => {
      const decodeStorage = (key: string) => {
        const raw = localStorage.getItem(key);
        if (!raw) return undefined;
        return JSON.parse(atob(raw)).content;
      };
      const target = new URL(requestUrl, window.location.origin);
      Object.entries(requestParams).forEach(([key, value]) => {
        if (value !== undefined) {
          target.searchParams.set(key, String(value));
        }
      });
      const targetUrl =
        target.origin === window.location.origin ? `${target.pathname}${target.search}` : target.toString();
      const headers: Record<string, string> = {
        Accept: 'application/json',
      };
      const tenant = decodeStorage('X-Auth-Tenant');
      const login = decodeStorage('X-Auth-Login');
      const token = decodeStorage('X-Auth-Token');
      if (tenant) headers['X-Auth-Tenant'] = tenant;
      if (login) headers['X-Auth-Login'] = login;
      if (token) headers['X-Auth-Token'] = JSON.stringify(token);

      const response = await fetch(targetUrl, {
        method: 'GET',
        headers,
      });
      const text = await response.text();
      return { status: response.status, text };
    },
    { requestUrl: url, requestParams: params }
  );

  let data: unknown;
  try {
    data = JSONBigIntStr.parse(response.text);
  } catch {
    data = response.text;
  }

  return { status: response.status, data } as ApiResult<T>;
}

function idOf(record: unknown) {
  if (!record || typeof record !== 'object' || !('id' in record)) return undefined;
  const id = (record as { id?: unknown }).id;
  return id == null ? undefined : String(id);
}

async function firstRecord(page: Page, url: string) {
  const response = await apiPost(page, url, { page: { current: 1, size: 1 } });
  const payload = response.data as { ok?: boolean; data?: { records?: unknown[] } };
  if (!payload?.ok) return undefined;
  return payload.data?.records?.[0];
}

async function firstArrayRecord(page: Page, url: string) {
  const response = await apiGet<unknown[]>(page, url);
  const payload = response.data as { ok?: boolean; data?: unknown[] };
  if (!payload?.ok) return undefined;
  return payload.data?.[0];
}

async function listByName(page: Page, url: string, nameField: string, name: string) {
  const response = await apiPost<{ records?: unknown[] }>(page, url, {
    page: { current: 1, size: 1 },
    [nameField]: name,
  });
  if (!response.data?.ok) return undefined;
  return response.data.data?.records?.[0];
}

function uniqueName(prefix: string) {
  return `e2e_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

async function createEntity(page: Page, seed: EntitySeed, cleanupStack: Array<() => Promise<void>>) {
  const name = String(seed.body[seed.nameField]);
  const existing = await listByName(page, seed.listUrl, seed.nameField, name);
  if (idOf(existing)) return idOf(existing);

  const add = await apiPost(page, seed.addUrl, seed.body);
  if (!add.data?.ok) {
    throw new Error(`Failed to seed ${seed.addUrl}: ${JSON.stringify(add.data)}`);
  }

  const createdId = idOf(add.data.data) || idOf(await listByName(page, seed.listUrl, seed.nameField, name));
  if (!createdId) {
    throw new Error(`Seeded ${seed.addUrl} but could not resolve created id for ${name}`);
  }

  cleanupStack.push(async () => {
    await apiPost(page, seed.deleteUrl, {}, { id: createdId }).catch(() => undefined);
  });

  return createdId;
}

async function discoverRouteIds(page: Page): Promise<RouteIds> {
  const [
    driver,
    profile,
    device,
    point,
    api,
    resource,
    menu,
    group,
    label,
    alarmRule,
    alarmNotify,
    alarmMessage,
    alarmChannel,
    alarmBind,
    alarmState,
    alarmRecord,
    agenticModelConfig,
    agenticProvider,
    user,
    role,
  ] = await Promise.all([
    firstRecord(page, '/api/v3/manager/driver/list'),
    firstRecord(page, '/api/v3/manager/profile/list'),
    firstRecord(page, '/api/v3/manager/device/list'),
    firstRecord(page, '/api/v3/manager/point/list'),
    firstRecord(page, '/api/v3/auth/api/list'),
    firstRecord(page, '/api/v3/auth/resource/list'),
    firstRecord(page, '/api/v3/auth/menu/list'),
    firstRecord(page, '/api/v3/manager/group/list'),
    firstRecord(page, '/api/v3/manager/label/list'),
    firstRecord(page, '/api/v3/data/rule/list'),
    firstRecord(page, '/api/v3/data/notify/list'),
    firstRecord(page, '/api/v3/data/message/list'),
    firstRecord(page, '/api/v3/data/notify/channel/list'),
    firstRecord(page, '/api/v3/data/notify/channel/bind/list'),
    firstRecord(page, '/api/v3/data/rule/state/list'),
    firstRecord(page, '/api/v3/data/notify/record/list'),
    firstArrayRecord(page, '/api/v3/agentic/model/config/list'),
    firstArrayRecord(page, '/api/v3/agentic/provider/list'),
    firstRecord(page, '/api/v3/auth/user_profile/list'),
    firstRecord(page, '/api/v3/auth/role/list'),
  ]);

  const pointProfileId =
    point && typeof point === 'object' && 'profileId' in point
      ? String((point as { profileId?: unknown }).profileId || '')
      : undefined;

  return {
    driverId: idOf(driver),
    profileId: idOf(profile),
    deviceId: idOf(device),
    pointId: idOf(point),
    pointProfileId: pointProfileId || idOf(profile),
    apiId: idOf(api),
    resourceId: idOf(resource),
    menuId: idOf(menu),
    groupId: idOf(group),
    labelId: idOf(label),
    alarmRuleId: idOf(alarmRule),
    alarmNotifyId: idOf(alarmNotify),
    alarmMessageId: idOf(alarmMessage),
    alarmChannelId: idOf(alarmChannel),
    alarmBindId: idOf(alarmBind),
    alarmStateId: idOf(alarmState),
    alarmRecordId: idOf(alarmRecord),
    agenticModelConfigId: idOf(agenticModelConfig),
    agenticProviderId: idOf(agenticProvider),
    userId: idOf(user),
    roleId: idOf(role),
  };
}

export async function ensureE2eData(page: Page): Promise<E2eDataContext> {
  const cleanupStack: Array<() => Promise<void>> = [];
  const discovered = await discoverRouteIds(page);
  const suffix = uniqueName('route');

  const driverId =
    discovered.driverId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/driver/list',
        addUrl: '/api/v3/manager/driver/add',
        deleteUrl: '/api/v3/manager/driver/delete',
        nameField: 'driverName',
        body: {
          driverName: suffix,
          driverCode: suffix,
          serviceName: `dc3-e2e-${suffix}`,
          serviceHost: '127.0.0.1',
          driverTypeFlag: 'CUSTOM',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const profileId =
    discovered.profileId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/profile/list',
        addUrl: '/api/v3/manager/profile/add',
        deleteUrl: '/api/v3/manager/profile/delete',
        nameField: 'profileName',
        body: {
          profileName: suffix,
          profileCode: suffix,
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const deviceId =
    discovered.deviceId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/device/list',
        addUrl: '/api/v3/manager/device/add',
        deleteUrl: '/api/v3/manager/device/delete',
        nameField: 'deviceName',
        body: {
          deviceName: suffix,
          deviceCode: suffix,
          driverId,
          profileIds: [profileId],
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const pointId =
    discovered.pointId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/point/list',
        addUrl: '/api/v3/manager/point/add',
        deleteUrl: '/api/v3/manager/point/delete',
        nameField: 'pointName',
        body: {
          pointName: suffix,
          pointCode: suffix,
          pointTypeFlag: 'STRING',
          rwFlag: 'RW',
          profileId,
          unit: '',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const apiId =
    discovered.apiId ||
    (await createEntity(
      page,
      {
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
          apiExt: { content: { url: '/e2e/fixture', title: suffix, remark: 'created by e2e route fixture' } },
        },
      },
      cleanupStack
    ));

  const resourceId =
    discovered.resourceId ||
    (await createEntity(
      page,
      {
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
      },
      cleanupStack
    ));

  const menuId =
    discovered.menuId ||
    (await createEntity(
      page,
      {
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
          menuExt: { content: { titles: { zh: suffix, en: suffix }, icon: 'Menu', url: '/e2e-fixture' } },
        },
      },
      cleanupStack
    ));

  const userId =
    discovered.userId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/auth/user_profile/list',
        addUrl: '/api/v3/auth/user_profile/add',
        deleteUrl: '/api/v3/auth/user_profile/delete',
        nameField: 'userName',
        body: {
          userName: suffix,
          nickName: suffix,
          phone: `139${String(Date.now()).slice(-8)}`,
          email: `${suffix}@example.com`,
          enableFlag: 0,
        },
      },
      cleanupStack
    ));

  const roleId =
    discovered.roleId ||
    (await createEntity(
      page,
      {
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
      },
      cleanupStack
    ));

  return {
    routeIds: {
      driverId,
      profileId,
      deviceId,
      pointId,
      pointProfileId: discovered.pointProfileId || profileId,
      apiId,
      resourceId,
      menuId,
      groupId: discovered.groupId,
      labelId: discovered.labelId,
      alarmRuleId: discovered.alarmRuleId,
      alarmNotifyId: discovered.alarmNotifyId,
      alarmMessageId: discovered.alarmMessageId,
      alarmChannelId: discovered.alarmChannelId,
      alarmBindId: discovered.alarmBindId,
      alarmStateId: discovered.alarmStateId,
      alarmRecordId: discovered.alarmRecordId,
      agenticModelConfigId: discovered.agenticModelConfigId,
      agenticProviderId: discovered.agenticProviderId,
      userId,
      roleId,
    },
    cleanup: async () => {
      for (const clean of cleanupStack.reverse()) {
        await clean();
      }
    },
  };
}

export function watchPageHealth(page: Page): PageHealth {
  const health: PageHealth = {
    pageErrors: [],
    consoleErrors: [],
    badResponses: [],
    businessApiRequests: [],
  };

  page.on('pageerror', (error) => health.pageErrors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') {
      health.consoleErrors.push(message.text());
    }
  });
  page.on('request', (request) => {
    if (isBusinessApi(request.url())) {
      health.businessApiRequests.push(request.url());
    }
  });
  page.on('response', async (response) => {
    if (!isBusinessApi(response.url()) || response.status() < 400) return;
    let body: string;
    try {
      body = (await response.text()).replace(/\s+/g, ' ').trim().slice(0, 260);
    } catch {
      body = '';
    }
    health.badResponses.push({ status: response.status(), url: response.url(), body });
  });

  return health;
}

export function markHealth(health: PageHealth) {
  return {
    pageErrors: health.pageErrors.length,
    consoleErrors: health.consoleErrors.length,
    badResponses: health.badResponses.length,
    businessApiRequests: health.businessApiRequests.length,
  };
}

export function expectHealthy(health: PageHealth, mark = markHealth(health)) {
  expect(health.pageErrors.slice(mark.pageErrors), 'page errors').toEqual([]);
  expect(health.consoleErrors.slice(mark.consoleErrors), 'console errors').toEqual([]);
  expect(health.badResponses.slice(mark.badResponses), 'bad API responses').toEqual([]);
}

export async function closeOverlay(page: Page) {
  const modal = page
    .locator('.el-dialog:visible, .el-drawer:visible, .el-popover:visible, .el-message-box:visible')
    .last();
  if (await modal.count()) {
    const cancel = modal.getByRole('button', { name: /Cancel|Close|No|取消|关闭|否/ }).last();
    if (await cancel.count()) {
      await cancel.click().catch(() => {});
      await page.waitForTimeout(300);
      return;
    }
  }
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(300);
}

export async function clickButtonIfPresent(page: Page, name: string | RegExp) {
  const button = page
    .getByRole('button', { name })
    .filter({ hasNot: page.locator('.is-disabled') })
    .first();
  if (!(await button.count())) return false;
  if (!(await button.isVisible().catch(() => false)) || !(await button.isEnabled().catch(() => false))) return false;

  await button.click();
  await waitForAppSettled(page);
  return true;
}
