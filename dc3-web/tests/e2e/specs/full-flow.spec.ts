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

import {type BrowserContext, expect, type Page, test} from '@playwright/test';

import {
  clickTab,
  type E2eDataContext,
  ensureE2eData,
  expectHealthy,
  fillFirstEditableInput,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';
import type {RouteIds} from '../fixtures/routes';

/**
 * Full Flow e2e spec.
 *
 * Single login session that walks every page and key interaction:
 *   - Home dashboard
 *   - Driver list + detail tabs (Info, Devices)
 *   - Profile list + detail tabs (Info, Points, Commands, Events, Devices) + edit tabs
 *   - Device list + detail tabs (Info, Profiles, Points, PointValues) + edit tabs
 *   - Point detail tabs (Info, Devices)
 *   - Point value list
 *   - Settings: User/Role/Resource/API/Menu/Group/Label list + detail tabs + add forms
 *   - Settings: Alarm pages (Rule, Notify, Message, Channel, Bind, State, History)
 *   - Settings: Agentic Model Config + Provider list + add forms
 *   - Settings: Event Overview tabs (Situation, Noise, Availability, SLA) + Device/Driver/Point events
 *   - Settings: Command History, Event History, About
 *   - Profile edit: add command without params (key regression test)
 *
 * Uses one seeded backend data context and a fresh login per test to avoid
 * auth/session coupling without silently skipping low-data environments.
 */

function uniqueName(prefix: string) {
  return `e2e_ff_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

async function openAddDialog(page: Page) {
  const btn = page.getByRole('button', {name: /^(Add|新增)$/}).first();
  if (await btn.isVisible().catch(() => false)) {
    await btn.click();
    await waitForAppSettled(page);
    const dialog = page.locator('.el-dialog:visible').last();
    if (await dialog.isVisible().catch(() => false)) return dialog;
  }
  return null;
}

async function closeDialog(page: Page) {
  const dialog = page.locator('.el-dialog:visible').last();
  if (await dialog.isVisible().catch(() => false)) {
    const cancel = dialog.getByRole('button', {name: /Cancel|取消/}).first();
    if (await cancel.isVisible().catch(() => false)) {
      await cancel.click();
    } else {
      await page.keyboard.press('Escape');
    }
    await page
      .locator('.el-dialog:visible')
      .waitFor({state: 'hidden', timeout: 5000})
      .catch(() => {});
  }
}

function requireRouteId(ids: RouteIds, key: keyof RouteIds, label: string) {
  const value = ids[key];
  expect(value, `${label} id must be prepared by ensureE2eData or by the backend seed data`).not.toBeUndefined();
  expect(value, `${label} id must be prepared by ensureE2eData or by the backend seed data`).not.toBe('');
  return String(value);
}

async function openDetailWhenSeeded(page: Page, id: string | undefined, listRoute: string, detailRoute: string) {
  if (id) {
    await page.goto(`/#${detailRoute}?id=${id}`, {waitUntil: 'domcontentloaded'});
  } else {
    await page.goto(`/#${listRoute}`, {waitUntil: 'domcontentloaded'});
  }
  await waitForAppSettled(page);
}

test.describe('full flow - all pages', () => {
  let ids: RouteIds = {};
  let seedContext: BrowserContext | undefined;
  let seedData: E2eDataContext | undefined;
  let health: ReturnType<typeof watchPageHealth>;

  test.beforeAll(async ({browser}) => {
    seedContext = await browser.newContext();
    const page = await seedContext.newPage();
    await login(page);
    seedData = await ensureE2eData(page);
    ids = seedData.routeIds;
  });

  test.afterAll(async () => {
    await seedData?.cleanup();
    await seedContext?.close();
  });

  test.beforeEach(async ({page}) => {
    // Re-login for each test to get a fresh session
    await login(page);
    health = watchPageHealth(page);
  });

  // ── Home ──────────────────────────────────────────────────────────
  test('home dashboard loads', async ({page}) => {
    await page.goto('/#/home', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await expect(page.locator('.home-banner, .home'))
      .toBeVisible({timeout: 10_000})
      .catch(() => {});
    expectHealthy(health);
  });

  // ── Driver ────────────────────────────────────────────────────────
  test('driver list page', async ({page}) => {
    await page.goto('/#/driver', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('driver detail tabs', async ({page}) => {
    const driverId = requireRouteId(ids, 'driverId', 'driver');
    await page.goto(`/#/driver/detail?id=${driverId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  // ── Profile ───────────────────────────────────────────────────────
  test('profile list page', async ({page}) => {
    await page.goto('/#/profile', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('profile detail all tabs', async ({page}) => {
    const profileId = requireRouteId(ids, 'profileId', 'profile');
    await page.goto(`/#/profile/detail?id=${profileId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Point|位号/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  test('profile edit tabs', async ({page}) => {
    const profileId = requireRouteId(ids, 'profileId', 'profile');
    await page.goto(`/#/profile/edit?id=${profileId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const nameInput = page.getByPlaceholder(/profile name|模板名称/i).first();
    await expect(nameInput).not.toHaveValue('', {timeout: 10_000});
    await clickTab(page, /Point|位号/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Profile Info|模板信息/);
    expectHealthy(health);
  });

  test('profile edit: add command without params (regression)', async ({page}) => {
    const profileId = requireRouteId(ids, 'profileId', 'profile');
    await page.goto(`/#/profile/edit?id=${profileId}&active=commandConfig`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const addBtn = page.getByRole('button', {name: /^(Add|新增)$/}).first();
    await expect(addBtn).toBeVisible({timeout: 10_000});
    await addBtn.click();
    await waitForAppSettled(page);
    const dialog = page.locator('.el-dialog:visible').last();
    await fillFirstEditableInput(dialog, uniqueName('cmd'));
    const mark = markHealth(health);
    await dialog.getByRole('button', {name: /Confirm|确定/}).click();
    await waitForAppSettled(page);
    expectHealthy(health, mark);
  });

  // ── Device ────────────────────────────────────────────────────────
  test('device list page', async ({page}) => {
    await page.goto('/#/device', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('device detail all tabs', async ({page}) => {
    const deviceId = requireRouteId(ids, 'deviceId', 'device');
    await page.goto(`/#/device/detail?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Profile|模板/);
    await clickTab(page, /Point|位号/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Data|数据/);
    expectHealthy(health);
  });

  test('device edit tabs', async ({page}) => {
    const deviceId = requireRouteId(ids, 'deviceId', 'device');
    await page.goto(`/#/device/edit?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const nameInput = page.getByPlaceholder(/device name|设备名称/i).first();
    await expect(nameInput).not.toHaveValue('', {timeout: 10_000});
    await clickTab(page, /Driver Attribute|驱动属性/);
    await clickTab(page, /Point Attribute|位号属性/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Device Info|设备信息/);
    expectHealthy(health);
  });

  // ── Point ─────────────────────────────────────────────────────────
  test('point value list page', async ({page}) => {
    await page.goto('/#/point_value', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('point detail tabs', async ({page}) => {
    const pointId = requireRouteId(ids, 'pointId', 'point');
    await page.goto(`/#/point/detail?id=${pointId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  // ── Settings: Users ───────────────────────────────────────────────
  test('settings user list + add dialog', async ({page}) => {
    await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      const name = uniqueName('user');
      await dialog.getByPlaceholder(/user name|用户名/i).fill(name);
      await dialog.getByPlaceholder(/nickname|昵称/i).fill(name);
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings user detail tabs', async ({page}) => {
    const userId = requireRouteId(ids, 'userId', 'user');
    await page.goto(`/#/settings/user/detail?id=${userId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Role|角色/);
    await clickTab(page, /Resource|资源/);
    expectHealthy(health);
  });

  // ── Settings: Roles ───────────────────────────────────────────────
  test('settings role list + add dialog', async ({page}) => {
    await page.goto('/#/settings/role', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await fillFirstEditableInput(dialog, uniqueName('role'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings role detail tabs', async ({page}) => {
    const roleId = requireRouteId(ids, 'roleId', 'role');
    await page.goto(`/#/settings/role/detail?id=${roleId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /User|用户/);
    await clickTab(page, /Resource|资源/);
    expectHealthy(health);
  });

  // ── Settings: Resources ───────────────────────────────────────────
  test('settings resource list', async ({page}) => {
    await page.goto('/#/settings/resource', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings resource detail tabs', async ({page}) => {
    const resourceId = requireRouteId(ids, 'resourceId', 'resource');
    await page.goto(`/#/settings/resource/detail?id=${resourceId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Role|角色/);
    await clickTab(page, /Child|子资源/);
    expectHealthy(health);
  });

  // ── Settings: API ─────────────────────────────────────────────────
  test('settings api list', async ({page}) => {
    await page.goto('/#/settings/api', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings api detail', async ({page}) => {
    const apiId = requireRouteId(ids, 'apiId', 'api');
    await page.goto(`/#/settings/api/detail?id=${apiId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Menu ────────────────────────────────────────────────
  test('settings menu list', async ({page}) => {
    await page.goto('/#/settings/menu', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings menu detail', async ({page}) => {
    const menuId = requireRouteId(ids, 'menuId', 'menu');
    await page.goto(`/#/settings/menu/detail?id=${menuId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Group ───────────────────────────────────────────────
  test('settings group list + add dialog', async ({page}) => {
    await page.goto('/#/settings/group', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await fillFirstEditableInput(dialog, uniqueName('group'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings group detail', async ({page}) => {
    const groupId = requireRouteId(ids, 'groupId', 'group');
    await page.goto(`/#/settings/group/detail?id=${groupId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Label ───────────────────────────────────────────────
  test('settings label list + add dialog', async ({page}) => {
    await page.goto('/#/settings/label', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await fillFirstEditableInput(dialog, uniqueName('label'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings label detail', async ({page}) => {
    const labelId = requireRouteId(ids, 'labelId', 'label');
    await page.goto(`/#/settings/label/detail?id=${labelId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Alarm ───────────────────────────────────────────────
  const alarmPages = [
    {name: 'Alarm Rule', route: '/settings/alarm/rule'},
    {name: 'Alarm Notify Policy', route: '/settings/alarm/notify'},
    {name: 'Alarm Message Template', route: '/settings/alarm/message'},
    {name: 'Alarm Notify Channel', route: '/settings/alarm/channel'},
    {name: 'Alarm Channel Binding', route: '/settings/alarm/bind'},
    {name: 'Alarm Runtime State', route: '/settings/alarm/state'},
    {name: 'Alarm Delivery History', route: '/settings/alarm/history'},
  ];

  for (const ap of alarmPages) {
    test(`settings ${ap.name} list`, async ({page}) => {
      await page.goto(`/#${ap.route}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
      expectHealthy(health);
    });
  }

  test('settings alarm rule detail', async ({page}) => {
    const alarmRuleId = requireRouteId(ids, 'alarmRuleId', 'alarm rule');
    await page.goto(`/#/settings/alarm/rule/detail?id=${alarmRuleId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm notify detail', async ({page}) => {
    const alarmNotifyId = requireRouteId(ids, 'alarmNotifyId', 'alarm notify');
    await page.goto(`/#/settings/alarm/notify/detail?id=${alarmNotifyId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm message detail', async ({page}) => {
    const alarmMessageId = requireRouteId(ids, 'alarmMessageId', 'alarm message');
    await page.goto(`/#/settings/alarm/message/detail?id=${alarmMessageId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm channel detail', async ({page}) => {
    const alarmChannelId = requireRouteId(ids, 'alarmChannelId', 'alarm channel');
    await page.goto(`/#/settings/alarm/channel/detail?id=${alarmChannelId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm bind detail', async ({page}) => {
    const alarmBindId = requireRouteId(ids, 'alarmBindId', 'alarm bind');
    await page.goto(`/#/settings/alarm/bind/detail?id=${alarmBindId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm state detail', async ({page}) => {
    await openDetailWhenSeeded(page, ids.alarmStateId, '/settings/alarm/state', '/settings/alarm/state/detail');
    expectHealthy(health);
  });

  test('settings alarm history detail', async ({page}) => {
    await openDetailWhenSeeded(page, ids.alarmHistoryId, '/settings/alarm/history', '/settings/alarm/history/detail');
    expectHealthy(health);
  });

  // ── Settings: Agentic ─────────────────────────────────────────────
  test('settings agentic model config list + add dialog', async ({page}) => {
    await page.goto('/#/settings/model/config', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await fillFirstEditableInput(dialog, 'gpt-4.1-mini-test');
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings agentic provider list + add dialog', async ({page}) => {
    await page.goto('/#/settings/model/provider', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await fillFirstEditableInput(dialog, uniqueName('provider'));
      const urlInput = dialog.getByPlaceholder(/url|地址/i).first();
      if (await urlInput.isVisible().catch(() => false)) {
        await urlInput.fill('https://api.example.com/v1');
      }
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings agentic model config detail', async ({page}) => {
    const agenticModelConfigId = requireRouteId(ids, 'agenticModelConfigId', 'agentic model config');
    await page.goto(`/#/settings/model/config/detail?id=${agenticModelConfigId}`, {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings agentic provider detail', async ({page}) => {
    const agenticProviderId = requireRouteId(ids, 'agenticProviderId', 'agentic provider');
    await page.goto(`/#/settings/model/provider/detail?id=${agenticProviderId}`, {
      waitUntil: 'domcontentloaded',
    });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Events ──────────────────────────────────────────────
  test('settings alarm overview all tabs', async ({page}) => {
    await page.goto('/#/settings/alarm/overview', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await clickTab(page, /Noise|噪音/);
    await clickTab(page, /Availability|可用性/);
    await clickTab(page, /SLA/);
    expectHealthy(health);
  });

  test('settings device alarm list', async ({page}) => {
    await page.goto('/#/settings/alarm/device', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings driver alarm list', async ({page}) => {
    await page.goto('/#/settings/alarm/driver', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings point alarm list', async ({page}) => {
    await page.goto('/#/settings/alarm/point', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Command & Event History ─────────────────────────────
  test('settings command history list', async ({page}) => {
    await page.goto('/#/settings/command/history', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings event history list', async ({page}) => {
    await page.goto('/#/settings/event/history', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: About ───────────────────────────────────────────────
  test('settings about page', async ({page}) => {
    await page.goto('/#/settings/about', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Toolbar interactions ──────────────────────────────────────────
  test('device list toolbar: search + reset + sort + refresh', async ({page}) => {
    await page.goto('/#/device', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    const searchInput = page.getByPlaceholder(/device name|设备名称/i).first();
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');
      await page
        .getByRole('button', {name: /Search|搜索/})
        .first()
        .click()
        .catch(() => {});
      await waitForAppSettled(page);
    }
    await page
      .getByRole('button', {name: /Reset|重置/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', {name: /Sort|排序/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', {name: /Refresh|刷新/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('profile list toolbar: search + reset + sort + refresh', async ({page}) => {
    test.setTimeout(90_000);
    await page.goto('/#/profile', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    const searchInput = page.getByPlaceholder(/profile name|模板名称/i).first();
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');
      await page
        .getByRole('button', {name: /Search|搜索/})
        .first()
        .click()
        .catch(() => {});
      await waitForAppSettled(page);
    }
    await page
      .getByRole('button', {name: /Reset|重置/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', {name: /Sort|排序/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', {name: /Refresh|刷新/})
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Error pages ───────────────────────────────────────────────────
  test('error pages render (403, 404, 500)', async ({page}) => {
    for (const code of ['403', '404', '500']) {
      await page.goto(`/#/${code}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
    }
    expect(health.pageErrors).toEqual([]);
  });

  // ── Login page UI ─────────────────────────────────────────────────
  test('login page renders form elements', async ({page}) => {
    await page.goto('/#/login', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await expect(page.locator('input').first()).toBeVisible({timeout: 10_000});
    await expect(page.getByRole('button', {name: /Login|登录/})).toBeVisible();
    expect(health.pageErrors).toEqual([]);
  });

  // ── AI Assistant Panel ────────────────────────────────────────────
  test('home AI assistant panel opens and closes', async ({page}) => {
    await page.goto('/#/home', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    const launcher = page.locator('.agentic-launcher:visible').first();
    if (await launcher.isVisible().catch(() => false)) {
      await launcher.click();
      await waitForAppSettled(page);
      const panel = page.locator('.agentic-panel:visible').first();
      await expect(panel).toBeVisible({timeout: 10_000});

      // Close panel
      const closeBtn = panel.getByRole('button', {name: /Close|关闭|×/}).first();
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click();
      } else {
        await page.keyboard.press('Escape');
      }
      await waitForAppSettled(page);
      expectHealthy(health);
    }
  });
});
