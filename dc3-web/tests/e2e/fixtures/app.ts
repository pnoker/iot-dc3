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

import {expect, type Locator, type Page} from '@playwright/test';
import JSONBigInt from 'json-bigint';
import type {RouteIds} from './routes';

export interface PageHealth {
  pageErrors: string[];
  consoleErrors: string[];
  badResponses: Array<{status: number; url: string; body: string}>;
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

interface MenuSeed {
  code: string;
  name: string;
  parentCode: string;
  icon: string;
  url: string;
  level: 'C2' | 'C3';
  index: number;
  type?: 'TITLE' | 'COMMON';
}

export interface E2eDataContext {
  routeIds: RouteIds;
  cleanup: () => Promise<void>;
}

const JSONBigIntStr = JSONBigInt({storeAsString: true});
const E2E_CREDENTIALS = {
  tenant: process.env.E2E_TENANT || 'default',
  name: process.env.E2E_USERNAME || 'dc3',
  password: process.env.E2E_PASSWORD || 'dc3dc3dc3',
};

export function isBusinessApi(url: string) {
  return url.includes('/api/v3/');
}

/**
 * Wait for the app shell to be visible before driving interactions. Replaces
 * the old `networkidle + 300ms hard wait` recipe — networkidle never fires
 * cleanly in this app (NProgress, SSE, polling), and arbitrary waits
 * compound across hundreds of navigations. Web-first assertion is bounded
 * by the Playwright `expect.timeout` and waits no longer than necessary.
 *
 * The `<router-view>` host (`#app > main`, fallback to `body > div`) appears
 * once Vue has mounted, even on the login page. After that the caller is
 * responsible for asserting page-specific elements.
 */
export async function waitForAppSettled(page: Page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {});
  // Wait until Vue's mount target has rendered something — that's the
  // earliest deterministic signal that Element Plus / vue-router have
  // wired up. Falls back gracefully if the app shell is unusually slow.
  await expect(page.locator('#app *').first()).toBeVisible({timeout: 10_000});
}

export async function clickTab(page: Page, pattern: RegExp | string) {
  for (let attempt = 0; attempt < 3; attempt++) {
    const tab = page.locator('.el-tabs__item').filter({hasText: pattern}).first();
    try {
      await tab.waitFor({state: 'visible', timeout: 5_000});
      await tab.click();
      await waitForAppSettled(page);
      return true;
    } catch {
      await page.waitForTimeout(250);
    }
  }
  return false;
}

export async function fillFirstEditableInput(root: Locator, value: string) {
  const input = root.locator('input:not([readonly]):not([disabled]):not([type="hidden"])').first();
  await expect(input).toBeVisible({timeout: 10_000});
  await input.fill(value);
}

export async function login(page: Page) {
  await page.goto('/#/login', {waitUntil: 'domcontentloaded'});
  await waitForAppSettled(page);

  const loginButton = page.getByRole('button', {name: 'Login'});
  const loginFormVisible = await loginButton
    .waitFor({state: 'visible', timeout: 10_000})
    .then(() => true)
    .catch(() => false);
  if (loginFormVisible) {
    await page.getByPlaceholder('Please enter tenant name').fill(E2E_CREDENTIALS.tenant);
    await page.getByPlaceholder('Please enter username').fill(E2E_CREDENTIALS.name);
    await page.locator('.login-form input[type="password"]').fill(E2E_CREDENTIALS.password);
    await loginButton.click();
    // Web-first: assert URL leaves /login. Bounded by expect.timeout in
    // playwright.config.ts (10s). Failure here is a real auth regression.
    await expect(page).not.toHaveURL(/\/login/);
  } else {
    await expect(page).not.toHaveURL(/\/login/);
  }

  await waitForAppSettled(page);
}

/**
 * Clears the browser session without calling the logout API. E2E specs use
 * this as a deterministic local auth reset; exercising the server-side token
 * cancellation belongs in an API/unit contract, not in every route teardown.
 */
export async function logout(page: Page) {
  await page.evaluate(() => {
    localStorage.removeItem('X-Auth-Tenant');
    localStorage.removeItem('X-Auth-Login');
    localStorage.removeItem('X-Auth-Token');
  });
}

export async function apiPost<T = unknown>(
  page: Page,
  url: string,
  body: Record<string, unknown> = {},
  params: Record<string, string | number | boolean | undefined> = {}
) {
  const response = await page.evaluate(
    async ({requestUrl, requestBody, requestParams}) => {
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
      return {status: response.status, text};
    },
    {requestUrl: url, requestBody: body, requestParams: params}
  );

  let data: unknown;
  try {
    data = JSONBigIntStr.parse(response.text);
  } catch {
    data = response.text;
  }

  return {status: response.status, data} as ApiResult<T>;
}

export async function apiGet<T = unknown>(
  page: Page,
  url: string,
  params: Record<string, string | number | boolean | undefined> = {}
) {
  const response = await page.evaluate(
    async ({requestUrl, requestParams}) => {
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
      return {status: response.status, text};
    },
    {requestUrl: url, requestParams: params}
  );

  let data: unknown;
  try {
    data = JSONBigIntStr.parse(response.text);
  } catch {
    data = response.text;
  }

  return {status: response.status, data} as ApiResult<T>;
}

function idOf(record: unknown) {
  if (!record || typeof record !== 'object' || !('id' in record)) return undefined;
  const id = (record as {id?: unknown}).id;
  return id == null ? undefined : String(id);
}

async function firstRecord(page: Page, url: string) {
  const response = await apiPost(page, url, {page: {current: 1, size: 1}});
  const payload = response.data as {ok?: boolean; data?: {records?: unknown[]}};
  if (!payload?.ok) return undefined;
  return payload.data?.records?.[0];
}

async function firstArrayRecord(page: Page, url: string) {
  const response = await apiGet<unknown[]>(page, url);
  const payload = response.data as {ok?: boolean; data?: unknown[]};
  if (!payload?.ok) return undefined;
  return payload.data?.[0];
}

async function listByName(page: Page, url: string, nameField: string, name: string) {
  const response = await apiPost<{records?: unknown[]}>(page, url, {
    page: {current: 1, size: 1},
    [nameField]: name,
  });
  if (!response.data?.ok) return undefined;
  return response.data.data?.records?.[0];
}

async function arrayRecordByName(page: Page, url: string, nameField: string, name: string) {
  const response = await apiGet<unknown[]>(page, url);
  const payload = response.data as {ok?: boolean; data?: unknown[]};
  if (!payload?.ok) return undefined;
  return payload.data?.find((record) => {
    if (!record || typeof record !== 'object') return false;
    return String((record as Record<string, unknown>)[nameField] ?? '') === name;
  });
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
    await apiPost(page, seed.deleteUrl, {}, {id: createdId}).catch(() => undefined);
  });

  return createdId;
}

async function createArrayEntity(page: Page, seed: EntitySeed, cleanupStack: Array<() => Promise<void>>) {
  const name = String(seed.body[seed.nameField]);
  const existing = await arrayRecordByName(page, seed.listUrl, seed.nameField, name);
  if (idOf(existing)) return idOf(existing);

  const add = await apiPost(page, seed.addUrl, seed.body);
  if (!add.data?.ok) {
    throw new Error(`Failed to seed ${seed.addUrl}: ${JSON.stringify(add.data)}`);
  }

  const createdId = idOf(add.data.data) || idOf(await arrayRecordByName(page, seed.listUrl, seed.nameField, name));
  if (!createdId) {
    throw new Error(`Seeded ${seed.addUrl} but could not resolve created id for ${name}`);
  }

  cleanupStack.push(async () => {
    await apiPost(page, seed.deleteUrl, {}, {id: createdId}).catch(() => undefined);
  });

  return createdId;
}

function structuredExt(type: string, content: Record<string, unknown>, remark = '') {
  return {type, version: 1, remark, content};
}

const E2E_SETTINGS_MENUS: MenuSeed[] = [
  {
    code: 'settingsModel',
    name: 'Model',
    parentCode: 'settings',
    icon: 'Cpu',
    url: '',
    level: 'C2',
    index: 20,
    type: 'TITLE',
  },
  {
    code: 'settingsModelConfig',
    name: 'ModelConfig',
    parentCode: 'settingsModel',
    icon: 'ChatDotRound',
    url: '/settings/model/config',
    level: 'C3',
    index: 1,
  },
  {
    code: 'settingsModelProvider',
    name: 'ModelProvider',
    parentCode: 'settingsModel',
    icon: 'ChatLineSquare',
    url: '/settings/model/provider',
    level: 'C3',
    index: 2,
  },
  {
    code: 'settingsCommand',
    name: 'Command',
    parentCode: 'settings',
    icon: 'Operation',
    url: '',
    level: 'C2',
    index: 30,
    type: 'TITLE',
  },
  {
    code: 'settingsCommandHistory',
    name: 'CommandHistory',
    parentCode: 'settingsCommand',
    icon: 'Document',
    url: '/settings/command/history',
    level: 'C3',
    index: 1,
  },
  {
    code: 'settingsEvent',
    name: 'Event',
    parentCode: 'settings',
    icon: 'Bell',
    url: '',
    level: 'C2',
    index: 35,
    type: 'TITLE',
  },
  {
    code: 'settingsEventHistory',
    name: 'EventHistory',
    parentCode: 'settingsEvent',
    icon: 'Document',
    url: '/settings/event/history',
    level: 'C3',
    index: 1,
  },
  {
    code: 'settingsAlarm',
    name: 'Alarm',
    parentCode: 'settings',
    icon: 'AlarmClock',
    url: '',
    level: 'C2',
    index: 40,
    type: 'TITLE',
  },
  {
    code: 'settingsAlarmOverview',
    name: 'AlarmOverview',
    parentCode: 'settingsAlarm',
    icon: 'DataLine',
    url: '/settings/alarm/overview',
    level: 'C3',
    index: 1,
  },
  {
    code: 'settingsAlarmRule',
    name: 'AlarmRule',
    parentCode: 'settingsAlarm',
    icon: 'SetUp',
    url: '/settings/alarm/rule',
    level: 'C3',
    index: 2,
  },
  {
    code: 'settingsAlarmNotify',
    name: 'AlarmNotify',
    parentCode: 'settingsAlarm',
    icon: 'Bell',
    url: '/settings/alarm/notify',
    level: 'C3',
    index: 3,
  },
  {
    code: 'settingsAlarmMessage',
    name: 'AlarmMessage',
    parentCode: 'settingsAlarm',
    icon: 'Message',
    url: '/settings/alarm/message',
    level: 'C3',
    index: 4,
  },
  {
    code: 'settingsAlarmChannel',
    name: 'AlarmChannel',
    parentCode: 'settingsAlarm',
    icon: 'Connection',
    url: '/settings/alarm/channel',
    level: 'C3',
    index: 5,
  },
  {
    code: 'settingsAlarmBind',
    name: 'AlarmBind',
    parentCode: 'settingsAlarm',
    icon: 'Link',
    url: '/settings/alarm/bind',
    level: 'C3',
    index: 6,
  },
  {
    code: 'settingsAlarmState',
    name: 'AlarmState',
    parentCode: 'settingsAlarm',
    icon: 'Monitor',
    url: '/settings/alarm/state',
    level: 'C3',
    index: 7,
  },
  {
    code: 'settingsAlarmHistory',
    name: 'AlarmHistory',
    parentCode: 'settingsAlarm',
    icon: 'DocumentChecked',
    url: '/settings/alarm/history',
    level: 'C3',
    index: 8,
  },
  {
    code: 'settingsDeviceAlarm',
    name: 'DeviceAlarm',
    parentCode: 'settingsAlarm',
    icon: 'Management',
    url: '/settings/alarm/device',
    level: 'C3',
    index: 9,
  },
  {
    code: 'settingsDriverAlarm',
    name: 'DriverAlarm',
    parentCode: 'settingsAlarm',
    icon: 'Promotion',
    url: '/settings/alarm/driver',
    level: 'C3',
    index: 10,
  },
  {
    code: 'settingsPointAlarm',
    name: 'PointAlarm',
    parentCode: 'settingsAlarm',
    icon: 'TrendCharts',
    url: '/settings/alarm/point',
    level: 'C3',
    index: 11,
  },
  {
    code: 'settingsGroup',
    name: 'Group',
    parentCode: 'settings',
    icon: 'Grid',
    url: '/settings/group',
    level: 'C2',
    index: 70,
  },
  {
    code: 'settingsLabel',
    name: 'Label',
    parentCode: 'settings',
    icon: 'CollectionTag',
    url: '/settings/label',
    level: 'C2',
    index: 80,
  },
];

async function ensureMenuSeed(page: Page, seed: MenuSeed, cleanupStack: Array<() => Promise<void>>) {
  const existing = await listByName(page, '/api/v3/auth/menu/list', 'menuCode', seed.code);
  const existingId = idOf(existing);
  if (existingId) {
    if (
      existing &&
      typeof existing === 'object' &&
      (existing as {remark?: unknown}).remark === 'created by e2e route fixture'
    ) {
      cleanupStack.push(async () => {
        await apiPost(page, '/api/v3/auth/menu/delete', {}, {id: existingId}).catch(() => undefined);
      });
    }
    return {id: existingId, created: false};
  }

  const parent = await listByName(page, '/api/v3/auth/menu/list', 'menuCode', seed.parentCode);
  const parentId = idOf(parent);
  if (!parentId) {
    throw new Error(`Cannot seed menu ${seed.code}: missing parent menu ${seed.parentCode}`);
  }

  const add = await apiPost(page, '/api/v3/auth/menu/add', {
    parentMenuId: parentId,
    menuName: seed.name,
    menuCode: seed.code,
    menuTypeFlag: seed.type || 'COMMON',
    menuLevel: seed.level,
    menuIndex: seed.index,
    enableFlag: 'ENABLE',
    remark: 'created by e2e route fixture',
    menuExt: {
      content: {
        titles: {zh: seed.name, en: seed.name},
        icon: seed.icon,
        url: seed.url,
      },
    },
  });
  if (!add.data?.ok) {
    throw new Error(`Failed to seed menu ${seed.code}: ${JSON.stringify(add.data)}`);
  }

  const createdId =
    idOf(add.data.data) || idOf(await listByName(page, '/api/v3/auth/menu/list', 'menuCode', seed.code));
  if (!createdId) {
    throw new Error(`Seeded menu ${seed.code} but could not resolve created id`);
  }

  cleanupStack.push(async () => {
    await apiPost(page, '/api/v3/auth/menu/delete', {}, {id: createdId}).catch(() => undefined);
  });

  return {id: createdId, created: true};
}

async function ensureSettingsMenus(page: Page, cleanupStack: Array<() => Promise<void>>) {
  let created = false;
  for (const seed of E2E_SETTINGS_MENUS) {
    const result = await ensureMenuSeed(page, seed, cleanupStack);
    created ||= result.created;
  }
  if (created) {
    await page.reload({waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
  }
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
    alarmHistory,
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
    firstRecord(page, '/api/v3/data/notify/history/list'),
    firstArrayRecord(page, '/api/v3/agentic/model/config/list'),
    firstArrayRecord(page, '/api/v3/agentic/provider/list'),
    firstRecord(page, '/api/v3/auth/user_profile/list'),
    firstRecord(page, '/api/v3/auth/role/list'),
  ]);

  const pointProfileId =
    point && typeof point === 'object' && 'profileId' in point
      ? String((point as {profileId?: unknown}).profileId || '')
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
    alarmHistoryId: idOf(alarmHistory),
    agenticModelConfigId: idOf(agenticModelConfig),
    agenticProviderId: idOf(agenticProvider),
    userId: idOf(user),
    roleId: idOf(role),
  };
}

export async function ensureE2eData(page: Page): Promise<E2eDataContext> {
  const cleanupStack: Array<() => Promise<void>> = [];
  await ensureSettingsMenus(page, cleanupStack);
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
          serviceName: suffix,
          serviceHost: '127.0.0.1',
          driverTypeFlag: 'DRIVER_CLIENT',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const driverAttributeResponse = await apiGet<unknown[]>(page, '/api/v3/manager/driver_attribute/list_by_driver_id', {
    driver_id: driverId,
  });
  const driverAttributes =
    (driverAttributeResponse.data as {ok?: boolean; data?: unknown[]})?.ok === true
      ? ((driverAttributeResponse.data as {data?: unknown[]}).data ?? [])
      : [];
  if (driverAttributes.length === 0) {
    const attributeName = `e2e_attr_${Date.now().toString(36).slice(-6)}`;
    await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/driver_attribute/list',
        addUrl: '/api/v3/manager/driver_attribute/add',
        deleteUrl: '/api/v3/manager/driver_attribute/delete',
        nameField: 'attributeName',
        body: {
          attributeName,
          attributeCode: attributeName,
          attributeTypeFlag: 'STRING',
          defaultValue: '',
          driverId,
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    );
  }

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
          profileShareFlag: 'TENANT',
          profileTypeFlag: 'USER',
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
          profileId,
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
          rwFlag: 'READ_WRITE',
          baseValue: 0,
          multiple: 1,
          valueDecimal: 0,
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
          apiExt: {content: {url: '/e2e/fixture', title: suffix, remark: 'created by e2e route fixture'}},
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
          menuExt: {content: {titles: {zh: suffix, en: suffix}, icon: 'Menu', url: '/e2e-fixture'}},
        },
      },
      cleanupStack
    ));

  const groupId =
    discovered.groupId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/group/list',
        addUrl: '/api/v3/manager/group/add',
        deleteUrl: '/api/v3/manager/group/delete',
        nameField: 'groupName',
        body: {
          parentGroupId: 0,
          groupName: suffix,
          groupCode: suffix,
          groupTypeFlag: 'DEVICE',
          groupIndex: 999,
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const labelId =
    discovered.labelId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/manager/label/list',
        addUrl: '/api/v3/manager/label/add',
        deleteUrl: '/api/v3/manager/label/delete',
        nameField: 'labelName',
        body: {
          entityTypeFlag: 'DEVICE',
          labelName: suffix,
          labelCode: suffix,
          labelColor: '#F4F4F5',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const alarmNotifyId =
    discovered.alarmNotifyId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/data/notify/list',
        addUrl: '/api/v3/data/notify/add',
        deleteUrl: '/api/v3/data/notify/delete',
        nameField: 'notifyName',
        body: {
          notifyName: suffix,
          notifyCode: suffix,
          autoConfirmFlag: 'MANUAL',
          notifyInterval: 300000,
          notifyExt: structuredExt('alarm-notify-policy', {
            dedup: {enabled: true, key: '${tenantId}:${ruleCode}:${entityId}'},
            rateLimit: {intervalMs: 300000, maxCount: 1},
            repeat: {enabled: false},
            recovery: {enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: false},
          }),
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const alarmMessageId =
    discovered.alarmMessageId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/data/message/list',
        addUrl: '/api/v3/data/message/add',
        deleteUrl: '/api/v3/data/message/delete',
        nameField: 'messageName',
        body: {
          messageName: suffix,
          messageCode: suffix,
          messageLevel: 'P2',
          messageExt: structuredExt('alarm-message-template', {
            variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
            templates: [
              {
                channelType: 'FEISHU_BOT',
                payloadType: 'CARD',
                template: {
                  title: '${severity} ${device} alarm',
                  summary: '${point} is ${value}${unit}, threshold ${threshold}${unit}.',
                },
              },
            ],
          }),
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const alarmChannelId =
    discovered.alarmChannelId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/data/notify/channel/list',
        addUrl: '/api/v3/data/notify/channel/add',
        deleteUrl: '/api/v3/data/notify/channel/delete',
        nameField: 'channelName',
        body: {
          channelName: suffix,
          channelCode: suffix,
          channelTypeFlag: 'FEISHU_BOT',
          credentialRef: 'secret:feishu:e2e',
          channelExt: structuredExt('notify-channel', {
            signEnabled: true,
            cardVersion: 'interactive-card-v1',
            atAllAllowed: false,
            testMessageEnabled: false,
            options: {locale: 'zh-CN'},
          }),
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const alarmBindId =
    discovered.alarmBindId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/data/notify/channel/bind/list',
        addUrl: '/api/v3/data/notify/channel/bind/add',
        deleteUrl: '/api/v3/data/notify/channel/bind/delete',
        nameField: 'notifyId',
        body: {
          notifyId: alarmNotifyId,
          channelId: alarmChannelId,
          bindExt: structuredExt('notify-channel-bind', {
            levels: ['P0', 'P1', 'P2'],
            sendRecovery: true,
            rateLimitOverrideMs: 300000,
          }),
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const alarmRuleId =
    discovered.alarmRuleId ||
    (await createEntity(
      page,
      {
        listUrl: '/api/v3/data/rule/list',
        addUrl: '/api/v3/data/rule/add',
        deleteUrl: '/api/v3/data/rule/delete',
        nameField: 'ruleName',
        body: {
          ruleName: suffix,
          ruleCode: suffix,
          alarmTargetTypeFlag: 'POINT',
          entityId: pointId,
          notifyId: alarmNotifyId,
          messageId: alarmMessageId,
          ruleExt: structuredExt('alarm-rule', {
            condition: {field: 'numValue', operator: '>', threshold: 80, unit: ''},
            window: {mode: 'LAST', minSamples: 1},
            recovery: {enabled: true, operator: '<=', threshold: 75, duration: 'PT5M'},
            severity: 'P2',
            eventType: 'ALARM',
            labels: [],
          }),
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const agenticProviderId =
    discovered.agenticProviderId ||
    (await createArrayEntity(
      page,
      {
        listUrl: '/api/v3/agentic/provider/list',
        addUrl: '/api/v3/agentic/provider/config/add',
        deleteUrl: '/api/v3/agentic/provider/config/delete',
        nameField: 'name',
        body: {
          name: suffix,
          providerType: 'openai-compatible',
          baseUrl: 'https://api.example.com/v1',
          defaultFlag: 'NOT_DEFAULT',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
        },
      },
      cleanupStack
    ));

  const agenticModelConfigId =
    discovered.agenticModelConfigId ||
    (await createArrayEntity(
      page,
      {
        listUrl: '/api/v3/agentic/model/config/list',
        addUrl: '/api/v3/agentic/model/config/add',
        deleteUrl: '/api/v3/agentic/model/config/delete',
        nameField: 'model',
        body: {
          providerId: agenticProviderId,
          model: suffix,
          label: suffix,
          stream: true,
          toolCall: true,
          vision: false,
          reasoning: false,
          temperature: 0.2,
          maxTokens: 1024,
          defaultFlag: 'NOT_DEFAULT',
          enableFlag: 'ENABLE',
          remark: 'created by e2e route fixture',
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
          enableFlag: 'ENABLE',
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
      groupId,
      labelId,
      alarmRuleId,
      alarmNotifyId,
      alarmMessageId,
      alarmChannelId,
      alarmBindId,
      alarmStateId: discovered.alarmStateId,
      alarmHistoryId: discovered.alarmHistoryId,
      agenticModelConfigId,
      agenticProviderId,
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
    health.badResponses.push({status: response.status(), url: response.url(), body});
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

/**
 * Dismisses the topmost open dialog/drawer/popover. Web-first: after the
 * Cancel click (or Escape fallback), waits until the overlay actually
 * detaches from the DOM, instead of a 300ms guess that can race transitions.
 */
export async function closeOverlay(page: Page) {
  const overlaySelector = '.el-dialog:visible, .el-drawer:visible, .el-popover:visible, .el-message-box:visible';
  const modal = page.locator(overlaySelector).last();

  if (await modal.count()) {
    const cancel = modal.getByRole('button', {name: /Cancel|Close|No|取消|关闭|否/}).last();
    if (await cancel.count()) {
      await cancel.click().catch(() => undefined);
    } else {
      await page.keyboard.press('Escape').catch(() => undefined);
    }
  } else {
    await page.keyboard.press('Escape').catch(() => undefined);
  }

  // The overlay must actually disappear; bounded by expect.timeout (10s).
  // A stuck modal here is a real bug, not a flake.
  await expect(page.locator(overlaySelector)).toHaveCount(0, {timeout: 5_000});
}

/**
 * Clicks a button by accessible name only when present, visible, and
 * enabled. The `[aria-disabled="true"]` and `[disabled]` filters cover
 * Element Plus's two disabled states (it toggles both depending on
 * component). Returns whether the click happened so callers can decide
 * what to assert.
 */
export async function clickButtonIfPresent(page: Page, name: string | RegExp) {
  const button = page
    .getByRole('button', {name})
    .filter({hasNot: page.locator('[aria-disabled="true"]')})
    .filter({hasNot: page.locator('[disabled]')})
    .first();

  if (!(await button.count())) return false;
  if (!(await button.isVisible().catch(() => false))) return false;
  if (!(await button.isEnabled().catch(() => false))) return false;

  await button.click();
  await waitForAppSettled(page);
  return true;
}
