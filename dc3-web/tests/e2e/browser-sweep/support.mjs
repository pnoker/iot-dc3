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

import JSONBigInt from 'json-bigint';

const JSONBigIntStr = JSONBigInt({storeAsString: true});
const E2E_CREDENTIALS = {
  tenant: process.env.E2E_TENANT || 'default',
  name: process.env.E2E_USERNAME || 'dc3',
  password: process.env.E2E_PASSWORD || 'dc3dc3dc3',
};

export function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function isBusinessApi(url) {
  return url.includes('/api/v3/');
}

export function shortText(text, size = 260) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, size);
}

export async function waitPage(page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {
  });
  await page.waitForLoadState('networkidle', {timeout: 6000}).catch(() => {
  });
  await delay(500);
}

export async function login(page, base) {
  await page.goto(`${base}/#/login`, {waitUntil: 'domcontentloaded'});
  await waitPage(page);
  const loginButton = page.getByRole('button', {name: 'Login'});
  if (await loginButton.count()) {
    await page.getByPlaceholder('Please enter tenant name').fill(E2E_CREDENTIALS.tenant);
    await page.getByPlaceholder('Please enter username').fill(E2E_CREDENTIALS.name);
    await page.locator('.login-form input[type="password"]').fill(E2E_CREDENTIALS.password);
    await loginButton.click();
    await page.waitForURL((url) => !url.hash.includes('/login'), {timeout: 15000}).catch(() => {
    });
  }
  await waitPage(page);
  if (page.url().includes('/login')) {
    throw new Error('Login failed, still on login page');
  }
}

export function createWatch(page) {
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
    state.requestBodies.push({method: req.method(), url: req.url(), body: parsed});
  });
  page.on('response', async (res) => {
    if (!isBusinessApi(res.url()) || res.status() < 400) return;
    let body;
    try {
      body = await res.text();
    } catch {
      body = '';
    }
    state.badResponses.push({status: res.status(), url: res.url(), body: shortText(body)});
  });
  return state;
}

export function markWatch(watch) {
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

export function hasEmptySearchEnum(requests) {
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

export async function assertClean(step, watch, mark) {
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

export async function closeModal(page) {
  const modal = page
    .locator('.el-dialog:visible, .el-drawer:visible, .el-popover:visible, .el-message-box:visible')
    .last();
  if (await modal.count()) {
    const cancel = modal.getByRole('button', {name: /Cancel|Close|No|取消|关闭|否/}).last();
    if (await cancel.count()) {
      await cancel.click().catch(() => {
      });
      await delay(300);
      return;
    }
  }
  await page.keyboard.press('Escape').catch(() => {
  });
  await delay(300);
}

export async function clickButtonIfPresent(page, name, options = {}) {
  const locator = page.getByRole('button', {name}).filter({hasNot: page.locator('.is-disabled')});
  const count = await locator.count();
  if (!count) return false;
  const btn = locator.nth(options.index || 0);
  if (!(await btn.isVisible().catch(() => false)) || !(await btn.isEnabled().catch(() => false))) return false;
  await btn.click();
  await waitPage(page);
  return true;
}

export async function apiPost(page, url, body = {}, params = {}) {
  const response = await page.evaluate(
    async ({requestUrl, requestBody, requestParams}) => {
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
      return {status: res.status, text};
    },
    {requestUrl: url, requestBody: body, requestParams: params}
  );

  let data;
  try {
    data = JSONBigIntStr.parse(response.text);
  } catch {
    data = response.text;
  }

  return {status: response.status, data, text: response.text};
}

export async function listCount(page, url, nameField, name) {
  const res = await apiPost(page, url, {page: {size: 10, current: 1}, [nameField]: name});
  if (!res.data?.ok) {
    throw new Error(`Failed to list ${url}: ${JSON.stringify(res.data)}`);
  }
  return res.data.data?.records?.length || 0;
}

export function idOf(record) {
  if (!record || typeof record !== 'object') return undefined;
  const id = record.id;
  return id == null ? undefined : String(id);
}

export async function firstRecord(page, url) {
  const res = await apiPost(page, url, {page: {size: 1, current: 1}});
  if (!res.data?.ok) return undefined;
  return res.data.data?.records?.[0];
}

export async function listByName(page, url, nameField, name) {
  const res = await apiPost(page, url, {page: {size: 1, current: 1}, [nameField]: name});
  if (!res.data?.ok) return undefined;
  return res.data.data?.records?.[0];
}

export function uniqueName(prefix) {
  return `e2e_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

export async function createEntity(page, cleanupStack, seed) {
  const name = seed.body[seed.nameField];
  const existing = await listByName(page, seed.listUrl, seed.nameField, name);
  if (idOf(existing)) return idOf(existing);

  const add = await apiPost(page, seed.addUrl, seed.body);
  if (!add.data?.ok) {
    throw new Error(`Failed to seed ${seed.addUrl}: ${JSON.stringify(add.data)}`);
  }
  const created = add.data.data || (await listByName(page, seed.listUrl, seed.nameField, name));
  const id = idOf(created);
  if (!id) {
    throw new Error(`Seeded ${seed.addUrl} but could not resolve id`);
  }

  cleanupStack.push(async () => {
    await apiPost(page, seed.deleteUrl, {}, {id}).catch(() => {
    });
  });
  return id;
}
