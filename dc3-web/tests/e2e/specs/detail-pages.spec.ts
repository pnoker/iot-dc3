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
 * distributed under the License or distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {expect, test} from '@playwright/test';

import {
  clickTab,
  ensureE2eData,
  expectHealthy,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';

/**
 * Detail Pages e2e spec.
 *
 * Tests that entity detail pages load correctly and their tabs work:
 *   - Device detail: Info, Points, Commands, Events tabs; Data tab is
 *     asserted as present but kept lazy because it depends on data-service
 *     point-value storage rather than manager-service detail data.
 *   - Profile detail: Info, Points, Commands, Events, Devices tabs
 *   - Driver detail: Info, Devices tabs
 *   - Point detail: Info, Devices tabs
 */

test.describe('detail pages', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('device detail loads with all tabs', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      const initialMark = markHealth(health);
      await page.goto(`/#/device/detail?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
      expectHealthy(health, initialMark);

      await expect(page.locator('.el-tabs__item').filter({hasText: /Device Info|设备信息/})).toBeVisible();
      await expect(page.locator('.el-tabs__item').filter({hasText: /Related Points|关联位号/})).toBeVisible();
      await expect(page.locator('.el-tabs__item').filter({hasText: /Related Commands|关联指令/})).toBeVisible();
      await expect(page.locator('.el-tabs__item').filter({hasText: /Related Events|关联事件/})).toBeVisible();
      await expect(page.locator('.el-tabs__item').filter({hasText: /Device Data|设备数据/})).toBeVisible();

      for (const tab of [/Related Points|关联位号/, /Related Commands|关联指令/, /Related Events|关联事件/]) {
        const mark = markHealth(health);
        await expect(clickTab(page, tab), `device detail tab ${tab}`).resolves.toBe(true);
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('profile detail loads with all tabs', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/detail?id=${profileId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      const tabs = page.locator('.el-tabs__item');
      const tabCount = await tabs.count();
      expect(tabCount, 'profile detail should have tabs').toBeGreaterThan(0);

      for (let i = 0; i < tabCount; i++) {
        const tab = tabs.nth(i);
        if (await tab.isVisible()) {
          const mark = markHealth(health);
          await tab.click();
          await waitForAppSettled(page);
          expectHealthy(health, mark);
        }
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('driver detail loads without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const driverId = e2eData.routeIds.driverId;
    expect(driverId, 'need a seeded driver').toBeDefined();

    try {
      await page.goto(`/#/driver/detail?id=${driverId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      const mark = markHealth(health);

      // Click through tabs if present
      const tabs = page.locator('.el-tabs__item');
      const tabCount = await tabs.count();
      for (let i = 0; i < tabCount; i++) {
        const tab = tabs.nth(i);
        if (await tab.isVisible()) {
          await tab.click();
          await waitForAppSettled(page);
        }
      }

      expectHealthy(health, mark);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('point detail loads without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const pointId = e2eData.routeIds.pointId;
    expect(pointId, 'need a seeded point').toBeDefined();

    try {
      await page.goto(`/#/point/detail?id=${pointId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      const mark = markHealth(health);

      const tabs = page.locator('.el-tabs__item');
      const tabCount = await tabs.count();
      for (let i = 0; i < tabCount; i++) {
        const tab = tabs.nth(i);
        if (await tab.isVisible()) {
          await tab.click();
          await waitForAppSettled(page);
        }
      }

      expectHealthy(health, mark);
    } finally {
      await e2eData.cleanup();
    }
  });
});
