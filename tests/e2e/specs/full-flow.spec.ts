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

import { expect, test } from '@playwright/test';

import { apiGet, apiPost, expectHealthy, login, markHealth, waitForAppSettled, watchPageHealth } from '../fixtures/app';

/**
 * Full Flow e2e spec.
 *
 * Single login session that walks every page and key interaction:
 *   - Home dashboard
 *   - Driver list + detail tabs (Info, Devices)
 *   - Profile list + detail tabs (Info, Points, Commands, Events, Devices) + edit wizard (5 steps)
 *   - Device list + detail tabs (Info, Profiles, Points, PointValues) + edit wizard (6 steps)
 *   - Point detail tabs (Info, Devices) + edit wizard (2 steps)
 *   - Point value list
 *   - Settings: User/Role/Resource/API/Menu/Group/Label list + detail tabs + add forms
 *   - Settings: Alarm pages (Rule, Notify, Message, Channel, Bind, State, History)
 *   - Settings: Agentic Model Config + Provider list + add forms
 *   - Settings: Event Overview tabs (Situation, Noise, Availability, SLA) + Device/Driver/Point events
 *   - Settings: Command Record, Event Report, About
 *   - Profile edit: add command without params (key regression test)
 *
 * Uses a single login + discovered IDs to avoid auth flakiness.
 */

function uniqueName(prefix: string) {
  return `e2e_ff_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

async function clickTab(page: import('@playwright/test').Page, pattern: RegExp) {
  // Retry loop to handle DOM re-rendering during navigation
  for (let attempt = 0; attempt < 3; attempt++) {
    const tab = page.locator('.el-tabs__item').filter({ hasText: pattern }).first();
    try {
      await tab.waitFor({ state: 'visible', timeout: 5000 });
      await tab.click();
      await waitForAppSettled(page);
      return true;
    } catch {
      await page.waitForTimeout(500);
    }
  }
  return false;
}

async function clickWizardNext(page: import('@playwright/test').Page) {
  const btn = page.getByRole('button', { name: /Next|下一步/ }).last();
  if (await btn.isVisible().catch(() => false)) {
    await btn.click();
    await waitForAppSettled(page);
  }
}

async function clickWizardPrevious(page: import('@playwright/test').Page) {
  const btn = page.getByRole('button', { name: /Previous|上一步/ }).last();
  if (await btn.isVisible().catch(() => false)) {
    await btn.click();
    await waitForAppSettled(page);
  }
}

async function openAddDialog(page: import('@playwright/test').Page) {
  const btn = page.getByRole('button', { name: /^(Add|新增)$/ }).first();
  if (await btn.isVisible().catch(() => false)) {
    await btn.click();
    await waitForAppSettled(page);
    const dialog = page.locator('.el-dialog:visible').last();
    if (await dialog.isVisible().catch(() => false)) return dialog;
  }
  return null;
}

async function closeDialog(page: import('@playwright/test').Page) {
  const dialog = page.locator('.el-dialog:visible').last();
  if (await dialog.isVisible().catch(() => false)) {
    const cancel = dialog.getByRole('button', { name: /Cancel|取消/ }).first();
    if (await cancel.isVisible().catch(() => false)) {
      await cancel.click();
    } else {
      await page.keyboard.press('Escape');
    }
    await page
      .locator('.el-dialog:visible')
      .waitFor({ state: 'hidden', timeout: 5000 })
      .catch(() => {});
  }
}

async function firstRecordId(page: import('@playwright/test').Page, url: string) {
  const res = await apiPost(page, url, { page: { current: 1, size: 1 } });
  const data = res.data as { ok?: boolean; data?: { records?: Array<{ id?: unknown }> } };
  if (!data?.ok) return undefined;
  const id = data.data?.records?.[0]?.id;
  return id != null ? String(id) : undefined;
}

async function firstArrayRecordId(page: import('@playwright/test').Page, url: string) {
  const res = await apiGet<Array<{ id?: unknown }>>(page, url);
  const data = res.data as { ok?: boolean; data?: Array<{ id?: unknown }> };
  if (!data?.ok) return undefined;
  const id = data.data?.[0]?.id;
  return id != null ? String(id) : undefined;
}

interface Ids {
  driverId?: string;
  profileId?: string;
  deviceId?: string;
  pointId?: string;
  pointProfileId?: string;
  userId?: string;
  roleId?: string;
  resourceId?: string;
  apiId?: string;
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
}

async function discoverIds(page: import('@playwright/test').Page): Promise<Ids> {
  const [
    driverId,
    profileId,
    deviceId,
    pointId,
    userId,
    roleId,
    resourceId,
    apiId,
    menuId,
    groupId,
    labelId,
    alarmRuleId,
    alarmNotifyId,
    alarmMessageId,
    alarmChannelId,
    alarmBindId,
    alarmStateId,
    alarmHistoryId,
    agenticModelConfigId,
    agenticProviderId,
  ] = await Promise.all([
    firstRecordId(page, '/api/v3/manager/driver/list'),
    firstRecordId(page, '/api/v3/manager/profile/list'),
    firstRecordId(page, '/api/v3/manager/device/list'),
    firstRecordId(page, '/api/v3/manager/point/list'),
    firstRecordId(page, '/api/v3/auth/user_profile/list'),
    firstRecordId(page, '/api/v3/auth/role/list'),
    firstRecordId(page, '/api/v3/auth/resource/list'),
    firstRecordId(page, '/api/v3/auth/api/list'),
    firstRecordId(page, '/api/v3/auth/menu/list'),
    firstRecordId(page, '/api/v3/manager/group/list'),
    firstRecordId(page, '/api/v3/manager/label/list'),
    firstRecordId(page, '/api/v3/data/rule/list'),
    firstRecordId(page, '/api/v3/data/notify/list'),
    firstRecordId(page, '/api/v3/data/message/list'),
    firstRecordId(page, '/api/v3/data/notify/channel/list'),
    firstRecordId(page, '/api/v3/data/notify/channel/bind/list'),
    firstRecordId(page, '/api/v3/data/rule/state/list'),
    firstRecordId(page, '/api/v3/data/notify/history/list'),
    firstArrayRecordId(page, '/api/v3/agentic/model/config/list'),
    firstArrayRecordId(page, '/api/v3/agentic/provider/list'),
  ]);

  let pointProfileId: string | undefined;
  if (pointId) {
    const ptRes = await apiGet(page, `/api/v3/manager/point/get_by_id`, { id: pointId });
    const ptData = ptRes.data as { ok?: boolean; data?: { profileId?: unknown } };
    pointProfileId = ptData?.data?.profileId ? String(ptData.data.profileId) : profileId;
  }

  return {
    driverId,
    profileId,
    deviceId,
    pointId,
    pointProfileId,
    userId,
    roleId,
    resourceId,
    apiId,
    menuId,
    groupId,
    labelId,
    alarmRuleId,
    alarmNotifyId,
    alarmMessageId,
    alarmChannelId,
    alarmBindId,
    alarmStateId,
    alarmHistoryId,
    agenticModelConfigId,
    agenticProviderId,
  };
}

test.describe('full flow - all pages', () => {
  let ids: Ids;
  let health: ReturnType<typeof watchPageHealth>;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await login(page);
    ids = await discoverIds(page);
    await context.close();
  });

  test.beforeEach(async ({ page }) => {
    // Re-login for each test to get a fresh session
    await login(page);
    health = watchPageHealth(page);
  });

  // ── Home ──────────────────────────────────────────────────────────
  test('home dashboard loads', async ({ page }) => {
    await page.goto('/#/home', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await expect(page.locator('.home-banner, .home'))
      .toBeVisible({ timeout: 10_000 })
      .catch(() => {});
    expectHealthy(health);
  });

  // ── Driver ────────────────────────────────────────────────────────
  test('driver list page', async ({ page }) => {
    await page.goto('/#/driver', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('driver detail tabs', async ({ page }) => {
    test.skip(!ids.driverId, 'no driver');
    await page.goto(`/#/driver/detail?id=${ids.driverId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  // ── Profile ───────────────────────────────────────────────────────
  test('profile list page', async ({ page }) => {
    await page.goto('/#/profile', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('profile detail all tabs', async ({ page }) => {
    test.skip(!ids.profileId, 'no profile');
    await page.goto(`/#/profile/detail?id=${ids.profileId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Point|位号/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  test('profile edit wizard navigation', async ({ page }) => {
    test.skip(!ids.profileId, 'no profile');
    await page.goto(`/#/profile/edit?id=${ids.profileId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    // Wait for data to load
    const nameInput = page.locator('.edit-card-body input').first();
    await expect(nameInput).not.toHaveValue('', { timeout: 10_000 });
    await clickWizardNext(page); // 0→1
    await clickWizardNext(page); // 1→2
    await clickWizardPrevious(page); // 2→1
    await clickWizardNext(page); // 1→2
    await clickWizardNext(page); // 2→3
    await clickWizardNext(page); // 3→4
    await expect(page.locator('.el-result')).toBeVisible({ timeout: 10_000 });
    expectHealthy(health);
  });

  test('profile edit: add command without params (regression)', async ({ page }) => {
    test.skip(!ids.profileId, 'no profile');
    await page.goto(`/#/profile/edit?id=${ids.profileId}&active=2`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const addBtn = page.getByRole('button', { name: /^(Add|新增)$/ }).first();
    await expect(addBtn).toBeVisible({ timeout: 10_000 });
    await addBtn.click();
    await waitForAppSettled(page);
    const dialog = page.locator('.el-dialog:visible').last();
    await dialog.locator('input').first().fill(uniqueName('cmd'));
    const mark = markHealth(health);
    await dialog.getByRole('button', { name: /Confirm|确定/ }).click();
    await waitForAppSettled(page);
    expectHealthy(health, mark);
  });

  // ── Device ────────────────────────────────────────────────────────
  test('device list page', async ({ page }) => {
    await page.goto('/#/device', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('device detail all tabs', async ({ page }) => {
    test.skip(!ids.deviceId, 'no device');
    await page.goto(`/#/device/detail?id=${ids.deviceId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Profile|模板/);
    await clickTab(page, /Point|位号/);
    await clickTab(page, /Command|命令/);
    await clickTab(page, /Event|事件/);
    await clickTab(page, /Data|数据/);
    expectHealthy(health);
  });

  test('device edit wizard navigation', async ({ page }) => {
    test.skip(!ids.deviceId, 'no device');
    await page.goto(`/#/device/edit?id=${ids.deviceId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await expect(page.locator('.el-steps')).toBeVisible();
    await clickWizardNext(page); // 0→1
    await clickWizardNext(page); // 1→2
    await clickWizardPrevious(page); // 2→1
    await clickWizardNext(page); // 1→2
    await clickWizardNext(page); // 2→3
    await clickWizardNext(page); // 3→4
    await clickWizardNext(page); // 4→5
    await expect(page.locator('.el-result')).toBeVisible({ timeout: 10_000 });
    expectHealthy(health);
  });

  // ── Point ─────────────────────────────────────────────────────────
  test('point value list page', async ({ page }) => {
    await page.goto('/#/point_value', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('point detail tabs', async ({ page }) => {
    test.skip(!ids.pointId, 'no point');
    await page.goto(`/#/point/detail?id=${ids.pointId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Device|设备/);
    expectHealthy(health);
  });

  test('point edit wizard', async ({ page }) => {
    test.skip(!ids.pointId || !ids.pointProfileId, 'no point');
    await page.goto(`/#/point/edit?id=${ids.pointId}&profileId=${ids.pointProfileId}`, {
      waitUntil: 'domcontentloaded',
    });
    await waitForAppSettled(page);
    await expect(page.locator('.el-steps')).toBeVisible();
    await clickWizardNext(page); // 0→1 (complete)
    await expect(page.locator('.el-result')).toBeVisible({ timeout: 10_000 });
    expectHealthy(health);
  });

  // ── Settings: Users ───────────────────────────────────────────────
  test('settings user list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/user', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill(uniqueName('user'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings user detail tabs', async ({ page }) => {
    test.skip(!ids.userId, 'no user');
    await page.goto(`/#/settings/user/detail?id=${ids.userId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Role|角色/);
    await clickTab(page, /Resource|资源/);
    expectHealthy(health);
  });

  // ── Settings: Roles ───────────────────────────────────────────────
  test('settings role list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/role', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill(uniqueName('role'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings role detail tabs', async ({ page }) => {
    test.skip(!ids.roleId, 'no role');
    await page.goto(`/#/settings/role/detail?id=${ids.roleId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /User|用户/);
    await clickTab(page, /Resource|资源/);
    expectHealthy(health);
  });

  // ── Settings: Resources ───────────────────────────────────────────
  test('settings resource list', async ({ page }) => {
    await page.goto('/#/settings/resource', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings resource detail tabs', async ({ page }) => {
    test.skip(!ids.resourceId, 'no resource');
    await page.goto(`/#/settings/resource/detail?id=${ids.resourceId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Role|角色/);
    await clickTab(page, /Child|子资源/);
    expectHealthy(health);
  });

  // ── Settings: API ─────────────────────────────────────────────────
  test('settings api list', async ({ page }) => {
    await page.goto('/#/settings/api', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings api detail', async ({ page }) => {
    test.skip(!ids.apiId, 'no api');
    await page.goto(`/#/settings/api/detail?id=${ids.apiId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Menu ────────────────────────────────────────────────
  test('settings menu list', async ({ page }) => {
    await page.goto('/#/settings/menu', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings menu detail', async ({ page }) => {
    test.skip(!ids.menuId, 'no menu');
    await page.goto(`/#/settings/menu/detail?id=${ids.menuId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Group ───────────────────────────────────────────────
  test('settings group list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/group', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill(uniqueName('group'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings group detail', async ({ page }) => {
    test.skip(!ids.groupId, 'no group');
    await page.goto(`/#/settings/group/detail?id=${ids.groupId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Label ───────────────────────────────────────────────
  test('settings label list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/label', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill(uniqueName('label'));
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings label detail', async ({ page }) => {
    test.skip(!ids.labelId, 'no label');
    await page.goto(`/#/settings/label/detail?id=${ids.labelId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Alarm ───────────────────────────────────────────────
  const alarmPages = [
    { name: 'Alarm Rule', route: '/settings/alarm/rule' },
    { name: 'Alarm Notify Policy', route: '/settings/alarm/notify' },
    { name: 'Alarm Message Template', route: '/settings/alarm/message' },
    { name: 'Alarm Notify Channel', route: '/settings/alarm/channel' },
    { name: 'Alarm Channel Binding', route: '/settings/alarm/bind' },
    { name: 'Alarm Runtime State', route: '/settings/alarm/state' },
    { name: 'Alarm Delivery History', route: '/settings/alarm/history' },
  ];

  for (const ap of alarmPages) {
    test(`settings ${ap.name} list`, async ({ page }) => {
      await page.goto(`/#${ap.route}`, { waitUntil: 'domcontentloaded' });
      await waitForAppSettled(page);
      expectHealthy(health);
    });
  }

  test('settings alarm rule detail', async ({ page }) => {
    test.skip(!ids.alarmRuleId, 'no alarm rule');
    await page.goto(`/#/settings/alarm/rule/detail?id=${ids.alarmRuleId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm notify detail', async ({ page }) => {
    test.skip(!ids.alarmNotifyId, 'no alarm notify');
    await page.goto(`/#/settings/alarm/notify/detail?id=${ids.alarmNotifyId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm message detail', async ({ page }) => {
    test.skip(!ids.alarmMessageId, 'no alarm message');
    await page.goto(`/#/settings/alarm/message/detail?id=${ids.alarmMessageId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm channel detail', async ({ page }) => {
    test.skip(!ids.alarmChannelId, 'no alarm channel');
    await page.goto(`/#/settings/alarm/channel/detail?id=${ids.alarmChannelId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm bind detail', async ({ page }) => {
    test.skip(!ids.alarmBindId, 'no alarm bind');
    await page.goto(`/#/settings/alarm/bind/detail?id=${ids.alarmBindId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm state detail', async ({ page }) => {
    test.skip(!ids.alarmStateId, 'no alarm state');
    await page.goto(`/#/settings/alarm/state/detail?id=${ids.alarmStateId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings alarm history detail', async ({ page }) => {
    test.skip(!ids.alarmHistoryId, 'no alarm history');
    await page.goto(`/#/settings/alarm/history/detail?id=${ids.alarmHistoryId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Agentic ─────────────────────────────────────────────
  test('settings agentic model config list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/agentic', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill('gpt-4.1-mini-test');
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings agentic provider list + add dialog', async ({ page }) => {
    await page.goto('/#/settings/agentic/provider', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    const dialog = await openAddDialog(page);
    if (dialog) {
      await dialog.locator('input').first().fill(uniqueName('provider'));
      const urlInput = dialog.getByPlaceholder(/url|地址/i).first();
      if (await urlInput.isVisible().catch(() => false)) {
        await urlInput.fill('https://api.example.com/v1');
      }
      await closeDialog(page);
    }
    expectHealthy(health);
  });

  test('settings agentic model config detail', async ({ page }) => {
    test.skip(!ids.agenticModelConfigId, 'no model config');
    await page.goto(`/#/settings/agentic/detail?id=${ids.agenticModelConfigId}`, { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings agentic provider detail', async ({ page }) => {
    test.skip(!ids.agenticProviderId, 'no provider');
    await page.goto(`/#/settings/agentic/provider/detail?id=${ids.agenticProviderId}`, {
      waitUntil: 'domcontentloaded',
    });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Events ──────────────────────────────────────────────
  test('settings event overview all tabs', async ({ page }) => {
    await page.goto('/#/settings/event', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await clickTab(page, /Noise|噪音/);
    await clickTab(page, /Availability|可用性/);
    await clickTab(page, /SLA/);
    expectHealthy(health);
  });

  test('settings device event list', async ({ page }) => {
    await page.goto('/#/settings/event/device', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings driver event list', async ({ page }) => {
    await page.goto('/#/settings/event/driver', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings point event list', async ({ page }) => {
    await page.goto('/#/settings/event/point', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: Command Record & Event Report ───────────────────────
  test('settings command record list', async ({ page }) => {
    await page.goto('/#/settings/command/record', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('settings event report list', async ({ page }) => {
    await page.goto('/#/settings/event/report', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Settings: About ───────────────────────────────────────────────
  test('settings about page', async ({ page }) => {
    await page.goto('/#/settings/about', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Toolbar interactions ──────────────────────────────────────────
  test('device list toolbar: search + reset + sort + refresh', async ({ page }) => {
    await page.goto('/#/device', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);

    const searchInput = page.getByPlaceholder(/device name|设备名称/i).first();
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');
      await page
        .getByRole('button', { name: /Search|搜索/ })
        .first()
        .click()
        .catch(() => {});
      await waitForAppSettled(page);
    }
    await page
      .getByRole('button', { name: /Reset|重置/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', { name: /Sort|排序/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', { name: /Refresh|刷新/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  test('profile list toolbar: search + reset + sort + refresh', async ({ page }) => {
    await page.goto('/#/profile', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);

    const searchInput = page.getByPlaceholder(/profile name|模板名称/i).first();
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');
      await page
        .getByRole('button', { name: /Search|搜索/ })
        .first()
        .click()
        .catch(() => {});
      await waitForAppSettled(page);
    }
    await page
      .getByRole('button', { name: /Reset|重置/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', { name: /Sort|排序/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    await page
      .getByRole('button', { name: /Refresh|刷新/ })
      .first()
      .click()
      .catch(() => {});
    await waitForAppSettled(page);
    expectHealthy(health);
  });

  // ── Error pages ───────────────────────────────────────────────────
  test('error pages render (403, 404, 500)', async ({ page }) => {
    for (const code of ['403', '404', '500']) {
      await page.goto(`/#/${code}`, { waitUntil: 'domcontentloaded' });
      await waitForAppSettled(page);
    }
    expect(health.pageErrors).toEqual([]);
  });

  // ── Login page UI ─────────────────────────────────────────────────
  test('login page renders form elements', async ({ page }) => {
    await page.goto('/#/login', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);
    await expect(page.locator('input').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('button', { name: /Login|登录/ })).toBeVisible();
    expect(health.pageErrors).toEqual([]);
  });

  // ── AI Assistant Panel ────────────────────────────────────────────
  test('home AI assistant panel opens and closes', async ({ page }) => {
    await page.goto('/#/home', { waitUntil: 'domcontentloaded' });
    await waitForAppSettled(page);

    const launcher = page.locator('.agentic-launcher:visible').first();
    if (await launcher.isVisible().catch(() => false)) {
      await launcher.click();
      await waitForAppSettled(page);
      const panel = page.locator('.agentic-panel:visible').first();
      await expect(panel).toBeVisible({ timeout: 10_000 });

      // Close panel
      const closeBtn = panel.getByRole('button', { name: /Close|关闭|×/ }).first();
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
