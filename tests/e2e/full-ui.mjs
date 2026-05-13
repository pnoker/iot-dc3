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

/* global process, setTimeout, console, localStorage, atob, fetch, URL, window */

import { chromium } from 'playwright';

const BASE = process.env.E2E_BASE_URL || 'http://localhost:8080';
const CHROME = process.env.E2E_CHROME_PATH || '';
const HEADLESS = process.env.E2E_HEADLESS !== 'false';

const routeIds = {};

const baseProtectedRoutes = [
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

const protectedRouteProbes = [
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

function entityRoutes() {
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

const interactionPages = [
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

const destructiveDeleteCases = [
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
      profileIds: [routeIds.profileId],
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

function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function isBusinessApi(url) {
  return url.includes('/api/v3/');
}

function shortText(text, size = 260) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, size);
}

async function waitPage(page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {});
  await page.waitForLoadState('networkidle', { timeout: 6000 }).catch(() => {});
  await delay(500);
}

async function login(page) {
  await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
  await waitPage(page);
  const loginButton = page.getByRole('button', { name: 'Login' });
  if (await loginButton.count()) {
    await loginButton.click();
    await page.waitForURL((url) => !url.hash.includes('/login'), { timeout: 15000 }).catch(() => {});
  }
  await waitPage(page);
  if (page.url().includes('/login')) {
    throw new Error('Login failed, still on login page');
  }
}

function createWatch(page) {
  const state = {
    pageErrors: [],
    consoleErrors: [],
    badResponses: [],
    requestBodies: [],
  };

  page.on('pageerror', (err) => state.pageErrors.push(err.message));
  page.on('console', (msg) => {
    if (msg.type() === 'error') state.consoleErrors.push(msg.text());
  });
  page.on('request', (req) => {
    if (!isBusinessApi(req.url())) return;
    const body = req.postData();
    if (!body) return;
    let parsed;
    try {
      parsed = JSON.parse(body);
    } catch {
      parsed = body;
    }
    state.requestBodies.push({ method: req.method(), url: req.url(), body: parsed });
  });
  page.on('response', async (res) => {
    if (!isBusinessApi(res.url()) || res.status() < 400) return;
    let body;
    try {
      body = await res.text();
    } catch {
      body = '';
    }
    state.badResponses.push({ status: res.status(), url: res.url(), body: shortText(body) });
  });
  return state;
}

function markWatch(watch) {
  return {
    pageErrors: watch.pageErrors.length,
    consoleErrors: watch.consoleErrors.length,
    badResponses: watch.badResponses.length,
    requestBodies: watch.requestBodies.length,
  };
}

function snapshotWatch(watch, from) {
  return {
    pageErrors: watch.pageErrors.slice(from.pageErrors),
    consoleErrors: watch.consoleErrors.slice(from.consoleErrors),
    badResponses: watch.badResponses.slice(from.badResponses),
    requestBodies: watch.requestBodies.slice(from.requestBodies),
  };
}

function hasEmptySearchEnum(requests) {
  return requests
    .filter((item) => item.method === 'POST')
    .filter((item) =>
      /\/(driver|profile|device|point|point_value|api|resource|menu|role|user|group|label|event|model\/config|provider)\/list/.test(
        item.url
      )
    )
    .filter((item) => item.body && typeof item.body === 'object')
    .filter(
      (item) =>
        item.body.enableFlag === '' || item.body.rangeKey === '' || item.body.type === '' || item.body.scope === ''
    );
}

async function assertClean(step, watch, mark) {
  const diff = snapshotWatch(watch, mark);
  const emptyEnumBodies = hasEmptySearchEnum(diff.requestBodies);
  if (diff.pageErrors.length || diff.consoleErrors.length || diff.badResponses.length || emptyEnumBodies.length) {
    throw new Error(
      `${step} failed: pageErrors=${JSON.stringify(diff.pageErrors)}, consoleErrors=${JSON.stringify(
        diff.consoleErrors
      )}, badResponses=${JSON.stringify(diff.badResponses)}, emptyEnumBodies=${JSON.stringify(emptyEnumBodies)}`
    );
  }
}

async function closeModal(page) {
  const modal = page
    .locator('.el-dialog:visible, .el-drawer:visible, .el-popover:visible, .el-message-box:visible')
    .last();
  if (await modal.count()) {
    const cancel = modal.getByRole('button', { name: /Cancel|Close|No|取消|关闭|否/ }).last();
    if (await cancel.count()) {
      await cancel.click().catch(() => {});
      await delay(300);
      return;
    }
  }
  await page.keyboard.press('Escape').catch(() => {});
  await delay(300);
}

async function clickButtonIfPresent(page, name, options = {}) {
  const locator = page.getByRole('button', { name }).filter({ hasNot: page.locator('.is-disabled') });
  const count = await locator.count();
  if (!count) return false;
  const btn = locator.nth(options.index || 0);
  if (!(await btn.isVisible().catch(() => false)) || !(await btn.isEnabled().catch(() => false))) return false;
  await btn.click();
  await waitPage(page);
  return true;
}

async function apiPost(page, url, body = {}, params = {}) {
  return page.evaluate(
    async ({ requestUrl, requestBody, requestParams }) => {
      const decodeStorage = (key) => {
        const raw = localStorage.getItem(key);
        if (!raw) return undefined;
        return JSON.parse(atob(raw)).content;
      };
      const target = new URL(requestUrl, window.location.origin);
      Object.entries(requestParams).forEach(([key, value]) => {
        if (value !== undefined) target.searchParams.set(key, String(value));
      });
      const targetUrl =
        target.origin === window.location.origin ? `${target.pathname}${target.search}` : target.toString();
      const headers = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-Auth-Tenant': decodeStorage('X-Auth-Tenant'),
        'X-Auth-Login': decodeStorage('X-Auth-Login'),
        'X-Auth-Token': JSON.stringify(decodeStorage('X-Auth-Token')),
      };
      const res = await fetch(targetUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
      });
      const text = await res.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
      return { status: res.status, data, text };
    },
    { requestUrl: url, requestBody: body, requestParams: params }
  );
}

async function listCount(page, url, nameField, name) {
  const res = await apiPost(page, url, { page: { size: 10, current: 1 }, [nameField]: name });
  if (!res.data?.ok) {
    throw new Error(`List failed for ${name}: ${JSON.stringify(res.data)}`);
  }
  return Number(res.data?.data?.total ?? 0);
}

function idOf(record) {
  if (!record || typeof record !== 'object' || record.id == null) return undefined;
  return String(record.id);
}

async function firstRecord(page, url) {
  const res = await apiPost(page, url, { page: { size: 1, current: 1 } });
  if (!res.data?.ok) return undefined;
  return res.data?.data?.records?.[0];
}

async function listByName(page, url, nameField, name) {
  const res = await apiPost(page, url, { page: { size: 1, current: 1 }, [nameField]: name });
  if (!res.data?.ok) return undefined;
  return res.data?.data?.records?.[0];
}

function uniqueName(prefix) {
  return `e2e_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

async function createEntity(page, cleanupStack, seed) {
  const name = String(seed.body[seed.nameField]);
  const existingId = idOf(await listByName(page, seed.listUrl, seed.nameField, name));
  if (existingId) return existingId;

  const add = await apiPost(page, seed.addUrl, seed.body);
  if (!add.data?.ok) {
    throw new Error(`Failed to seed ${seed.addUrl}: ${JSON.stringify(add.data)}`);
  }

  const id = idOf(add.data?.data) || idOf(await listByName(page, seed.listUrl, seed.nameField, name));
  if (!id) {
    throw new Error(`Seeded ${seed.addUrl} but could not resolve created id for ${name}`);
  }

  cleanupStack.push(async () => {
    await apiPost(page, seed.deleteUrl, {}, { id }).catch(() => {});
  });
  return id;
}

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

async function ensureE2eData(page) {
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
        serviceName: `dc3-e2e-${suffix}`,
        serviceHost: '127.0.0.1',
        driverTypeFlag: 'CUSTOM',
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
        profileIds: [profileId],
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
        rwFlag: 'RW',
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
        apiExt: { content: { url: '/e2e/fixture', title: suffix, remark: 'created by e2e route fixture' } },
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
        menuExt: { content: { titles: { zh: suffix, en: suffix }, icon: 'Menu', url: '/e2e-fixture' } },
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
        enableFlag: 0,
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

async function testSearch(page, watch, pageDef, result) {
  const mark = markWatch(watch);
  if (pageDef.placeholder) {
    const input = page.getByPlaceholder(pageDef.placeholder).first();
    if (await input.count()) {
      await input.fill(pageDef.value || '');
      result.actions.push(`fill ${pageDef.placeholder}`);
    }
  }
  await clickButtonIfPresent(page, 'Search');
  result.actions.push('search');
  await assertClean(`${pageDef.name} search`, watch, mark);

  const resetMark = markWatch(watch);
  await clickButtonIfPresent(page, 'Reset');
  result.actions.push('reset');
  await assertClean(`${pageDef.name} reset`, watch, resetMark);
}

async function testFooterButtons(page, watch, pageDef, result) {
  const footerButtons = page.locator('.tool-card-footer-page button.el-button.is-circle:visible');
  const count = await footerButtons.count();
  for (let i = 0; i < Math.min(count, 2); i += 1) {
    const mark = markWatch(watch);
    await footerButtons.nth(i).click();
    await waitPage(page);
    result.actions.push(i === 0 ? 'refresh' : 'sort');
    await assertClean(`${pageDef.name} footer circle ${i}`, watch, mark);
  }
}

async function testPagination(page, watch, pageDef, result) {
  const next = page.locator('.el-pagination button.btn-next:visible').first();
  if ((await next.count()) && (await next.isEnabled().catch(() => false))) {
    const mark = markWatch(watch);
    await next.click();
    await waitPage(page);
    result.actions.push('next page');
    await assertClean(`${pageDef.name} next page`, watch, mark);
  }

  const prev = page.locator('.el-pagination button.btn-prev:visible').first();
  if ((await prev.count()) && (await prev.isEnabled().catch(() => false))) {
    const mark = markWatch(watch);
    await prev.click();
    await waitPage(page);
    result.actions.push('previous page');
    await assertClean(`${pageDef.name} previous page`, watch, mark);
  }

  const sizeSelect = page.locator('.el-pagination .el-select:visible').first();
  if (await sizeSelect.count()) {
    const mark = markWatch(watch);
    await sizeSelect.click();
    await delay(300);
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '24/page' }).last();
    if (await option.count()) {
      await option.click();
      await waitPage(page);
      result.actions.push('page size 24');
      await assertClean(`${pageDef.name} page size`, watch, mark);
    } else {
      await page.keyboard.press('Escape').catch(() => {});
    }
  }
}

async function testAddImportForms(page, watch, pageDef, result) {
  if (pageDef.addDisabled) {
    const add = page.getByRole('button', { name: 'Add' }).first();
    if ((await add.count()) && !(await add.isEnabled().catch(() => true))) {
      result.actions.push('disabled add checked');
    }
  }

  if (pageDef.add) {
    const mark = markWatch(watch);
    const clicked = await clickButtonIfPresent(page, 'Add');
    if (clicked) {
      result.actions.push('open add form');
      const dialog = page.locator('.el-dialog:visible').last();
      if (!(await dialog.count())) throw new Error(`${pageDef.name} add did not open dialog`);
      const confirm = dialog.getByRole('button', { name: 'Confirm' }).last();
      if (await confirm.count()) {
        await confirm.click();
        await delay(400);
        result.actions.push('add form validation');
      }
      const reset = dialog.getByRole('button', { name: 'Reset' }).last();
      if (await reset.count()) {
        await reset.click();
        await delay(200);
        result.actions.push('add form reset');
      }
      await closeModal(page);
      await assertClean(`${pageDef.name} add form`, watch, mark);
    }
  }

  if (pageDef.importButton) {
    const mark = markWatch(watch);
    const clicked = await clickButtonIfPresent(page, 'Import');
    if (clicked) {
      result.actions.push('open import form');
      const dialog = page.locator('.el-dialog:visible').last();
      if (!(await dialog.count())) throw new Error(`${pageDef.name} import did not open dialog`);
      await closeModal(page);
      await assertClean(`${pageDef.name} import form`, watch, mark);
    }
  }
}

async function testRowActions(page, watch, pageDef, result) {
  const actions = [
    ['Detail', pageDef.detail],
    ['Edit', pageDef.edit],
    [pageDef.assign, Boolean(pageDef.assign)],
  ].filter(([name, enabled]) => name && enabled);

  for (const [name] of actions) {
    await page.goto(`${BASE}/#${pageDef.route}`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);
    const mark = markWatch(watch);
    const clicked = await clickButtonIfPresent(page, name);
    if (!clicked) continue;
    result.actions.push(String(name).toLowerCase());
    await waitPage(page);
    await closeModal(page);
    await assertClean(`${pageDef.name} row ${name}`, watch, mark);
  }

  if (pageDef.deleteClick) {
    await page.goto(`${BASE}/#${pageDef.route}`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);
    const mark = markWatch(watch);
    const deleted = await clickButtonIfPresent(page, 'Delete');
    if (deleted) {
      result.actions.push('delete popconfirm opened');
      await closeModal(page);
      await assertClean(`${pageDef.name} delete click`, watch, mark);
    }
  }
}

async function testOverviewButtons(page, watch, result) {
  for (const groupIndex of [0, 1]) {
    for (const name of ['All', 'Unconfirmed', 'Today', 'Last 7 days', 'Last 30 days']) {
      await page.goto(`${BASE}/#/settings/event`, { waitUntil: 'domcontentloaded' });
      await waitPage(page);
      const group = page.locator('.event-overview__quick-actions').nth(groupIndex);
      const button = group.getByRole('button', { name }).first();
      if (!(await button.count())) continue;
      const mark = markWatch(watch);
      await button.click();
      await waitPage(page);
      result.actions.push(`overview ${groupIndex === 0 ? 'device' : 'driver'} ${name}`);
      await assertClean(`Event overview ${name}`, watch, mark);
    }
  }
}

async function testDashboardTabsAndButtons(page, watch) {
  const result = { name: 'Dashboard widgets', ok: true, actions: [], error: '' };
  try {
    await page.goto(`${BASE}/#/home`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);

    const statRefreshButtons = page.locator('.home__stats .stat-card__refresh:visible');
    const statRefreshCount = await statRefreshButtons.count();
    for (let i = 0; i < statRefreshCount; i += 1) {
      const mark = markWatch(watch);
      await statRefreshButtons.nth(i).click();
      await waitPage(page);
      result.actions.push(`home stat refresh ${i + 1}`);
      await assertClean(`Home stat refresh ${i + 1}`, watch, mark);
    }

    const analyticsGroups = page.locator('.analytics-tabs:visible');
    const analyticsGroupCount = await analyticsGroups.count();
    for (let groupIndex = 0; groupIndex < analyticsGroupCount; groupIndex += 1) {
      const group = analyticsGroups.nth(groupIndex);
      const tabs = group.locator('.el-tabs__item:visible');
      const tabCount = await tabs.count();
      for (let tabIndex = 0; tabIndex < tabCount; tabIndex += 1) {
        const tab = tabs.nth(tabIndex);
        const label = shortText(await tab.innerText(), 80) || `${groupIndex + 1}.${tabIndex + 1}`;
        const mark = markWatch(watch);
        await tab.click();
        await waitPage(page);
        result.actions.push(`home analytics tab ${label}`);
        await assertClean(`Home analytics tab ${label}`, watch, mark);
      }

      const rangeButtons = group.locator('.el-segmented__item:visible');
      const rangeCount = await rangeButtons.count();
      for (let rangeIndex = 0; rangeIndex < rangeCount; rangeIndex += 1) {
        const button = rangeButtons.nth(rangeIndex);
        const label = shortText(await button.innerText(), 80) || `${groupIndex + 1}.${rangeIndex + 1}`;
        const mark = markWatch(watch);
        await button.click();
        await waitPage(page);
        result.actions.push(`home analytics range ${label}`);
        await assertClean(`Home analytics range ${label}`, watch, mark);
      }
    }

    const dashboardRefreshButtons = page.locator('.home .dashboard-card__tools button.el-button:visible');
    const dashboardRefreshCount = await dashboardRefreshButtons.count();
    for (let i = 0; i < Math.min(dashboardRefreshCount, 8); i += 1) {
      const mark = markWatch(watch);
      await dashboardRefreshButtons.nth(i).click();
      await waitPage(page);
      result.actions.push(`home dashboard refresh ${i + 1}`);
      await assertClean(`Home dashboard refresh ${i + 1}`, watch, mark);
    }

    for (let i = 0; i < 6; i += 1) {
      await page.goto(`${BASE}/#/home`, { waitUntil: 'domcontentloaded' });
      await waitPage(page);
      const card = page.locator('.home__stats .stat-card:visible').nth(i);
      if (!(await card.count())) continue;
      const mark = markWatch(watch);
      await card.click();
      await waitPage(page);
      result.actions.push(`home stat card ${i + 1}`);
      await assertClean(`Home stat card ${i + 1}`, watch, mark);
    }

    await page.goto(`${BASE}/#/settings/event`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);
    for (const tab of ['Situation', 'Noise', 'Availability', 'SLA']) {
      const locator = page.locator('.event-overview__tabs .el-tabs__item:visible', { hasText: tab }).first();
      if (!(await locator.count())) continue;
      const mark = markWatch(watch);
      await locator.click();
      await waitPage(page);
      result.actions.push(`event tab ${tab}`);
      await assertClean(`Event overview tab ${tab}`, watch, mark);
    }

    const eventRefreshButtons = page.locator(
      '.event-overview .stat-card__refresh:visible, .event-overview .dashboard-card__tools button.el-button:visible'
    );
    const eventRefreshCount = await eventRefreshButtons.count();
    for (let i = 0; i < Math.min(eventRefreshCount, 8); i += 1) {
      const mark = markWatch(watch);
      await eventRefreshButtons.nth(i).click();
      await waitPage(page);
      result.actions.push(`event refresh ${i + 1}`);
      await assertClean(`Event refresh ${i + 1}`, watch, mark);
    }
  } catch (err) {
    result.ok = false;
    result.error = err.message;
  }
  return result;
}

async function testActualDelete(page, watch, testCase) {
  const suffix = Date.now().toString(36).slice(-8);
  const name = `codex_${testCase.name.toLowerCase().replace(/[^a-z]+/g, '_')}_${suffix}`;
  const add = await apiPost(page, testCase.addUrl, testCase.seed(name, suffix));
  if (!add.data?.ok) throw new Error(`${testCase.name} seed failed: ${JSON.stringify(add.data)}`);
  const before = await listCount(page, testCase.listUrl, testCase.nameField, name);
  if (before < 1) throw new Error(`${testCase.name} seed not found after add`);

  await page.goto(`${BASE}/#${testCase.route}`, { waitUntil: 'domcontentloaded' });
  await waitPage(page);
  const input = page.getByPlaceholder(testCase.placeholder).first();
  await input.fill(name);
  const searchMark = markWatch(watch);
  await clickButtonIfPresent(page, 'Search');
  await assertClean(`${testCase.name} search before delete`, watch, searchMark);
  await page.getByText(name).first().waitFor({ state: 'visible', timeout: 10000 });

  const deleteMark = markWatch(watch);
  await page.getByRole('button', { name: 'Delete' }).first().click();
  const confirmButton = page.getByRole('button', { name: /^(Yes|Confirm|确定|确认)$/ }).last();
  await confirmButton.click();
  await waitPage(page);
  await assertClean(`${testCase.name} confirmed delete`, watch, deleteMark);

  const after = await listCount(page, testCase.listUrl, testCase.nameField, name);
  if (after !== 0) throw new Error(`${testCase.name} still exists after UI delete, count=${after}`);
  return `${testCase.name}: ${name}`;
}

async function destructiveDeleteTest(page, watch) {
  const result = { name: 'Actual delete buttons', ok: true, actions: [], error: '' };
  try {
    for (const testCase of destructiveDeleteCases) {
      const action = await testActualDelete(page, watch, testCase);
      result.actions.push(action);
    }
  } catch (err) {
    result.ok = false;
    result.error = err.message;
  }
  return result;
}

async function securityTest(browser) {
  const context = await browser.newContext();
  const page = await context.newPage();
  const results = [];
  const businessApiRequests = [];
  page.on('request', (req) => {
    if (isBusinessApi(req.url())) businessApiRequests.push(req.url());
  });

  for (const route of [...baseProtectedRoutes, ...protectedRouteProbes]) {
    await page.goto(`${BASE}/#${route}`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);
    results.push({ route, ok: page.url().includes('/login'), url: page.url() });
  }

  await context.close();
  return {
    total: results.length,
    passed: results.filter((item) => item.ok).length,
    failed: results.filter((item) => !item.ok),
    businessApiRequests,
  };
}

async function routeOpenTest(page, watch) {
  const results = [];
  const openRoutes = ['/login', '/403', '/404', '/500', ...baseProtectedRoutes, ...entityRoutes()];
  for (const route of openRoutes) {
    const mark = markWatch(watch);
    await page.goto(`${BASE}/#${route}`, { waitUntil: 'domcontentloaded' });
    await waitPage(page);
    const body = shortText(
      await page
        .locator('body')
        .innerText()
        .catch(() => '')
    );
    let ok = true;
    let reason = '';
    try {
      await assertClean(`route ${route}`, watch, mark);
    } catch (err) {
      ok = false;
      reason = err.message;
    }
    if (!['/login', '/403', '/404', '/500'].includes(route) && page.url().includes('/login')) {
      ok = false;
      reason = 'unexpected redirect to login';
    }
    results.push({ route, ok, url: page.url(), body, reason });
  }
  return {
    total: results.length,
    passed: results.filter((item) => item.ok).length,
    failed: results.filter((item) => !item.ok),
  };
}

async function interactionTest(page, watch) {
  const results = [];
  for (const pageDef of interactionPages) {
    const result = { name: pageDef.name, route: pageDef.route, ok: true, actions: [], error: '' };
    try {
      await page.goto(`${BASE}/#${pageDef.route}`, { waitUntil: 'domcontentloaded' });
      await waitPage(page);
      await testSearch(page, watch, pageDef, result);
      await testFooterButtons(page, watch, pageDef, result);
      await testPagination(page, watch, pageDef, result);
      await testAddImportForms(page, watch, pageDef, result);
      await testRowActions(page, watch, pageDef, result);
    } catch (err) {
      result.ok = false;
      result.error = err.message;
    }
    results.push(result);
  }

  const overview = { name: 'Settings Event Overview', route: '/settings/event', ok: true, actions: [], error: '' };
  try {
    await testOverviewButtons(page, watch, overview);
  } catch (err) {
    overview.ok = false;
    overview.error = err.message;
  }
  results.push(overview);
  results.push(await testDashboardTabsAndButtons(page, watch));
  results.push(await destructiveDeleteTest(page, watch));

  return {
    total: results.length,
    passed: results.filter((item) => item.ok).length,
    failed: results.filter((item) => !item.ok),
    results,
  };
}

const launchOptions = {
  headless: HEADLESS,
};

if (CHROME) {
  launchOptions.executablePath = CHROME;
}

const browser = await chromium.launch(launchOptions);

const security = await securityTest(browser);

const context = await browser.newContext();
const page = await context.newPage();
const watch = createWatch(page);
await login(page);

const cleanupE2eData = await ensureE2eData(page);
let routeOpen;
let interactions;

try {
  routeOpen = await routeOpenTest(page, watch);
  interactions = await interactionTest(page, watch);
} finally {
  await cleanupE2eData();
  await context.close();
  await browser.close();
}

const output = {
  security,
  routeOpen,
  interactions,
  totals: {
    pageErrors: watch.pageErrors.length,
    consoleErrors: watch.consoleErrors.length,
    badResponses: watch.badResponses.length,
    businessRequestsWithBodies: watch.requestBodies.length,
    emptySearchEnumBodies: hasEmptySearchEnum(watch.requestBodies),
  },
};

console.log(JSON.stringify(output, null, 2));

if (
  security.failed.length ||
  security.businessApiRequests.length ||
  routeOpen.failed.length ||
  interactions.failed.length ||
  output.totals.pageErrors ||
  output.totals.consoleErrors ||
  output.totals.badResponses ||
  output.totals.emptySearchEnumBodies.length
) {
  process.exit(1);
}
