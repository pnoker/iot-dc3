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
 * Button Functions e2e spec.
 *
 * Tests toolbar and wizard button functions in detail:
 *   - List pages: search, sort, reset, refresh, pagination
 *   - Device edit tabs: reset and tab navigation
 *   - Profile edit tabs: reset and tab navigation
 *   - Detail pages: tab switching, edit button navigation
 *   - Enable/disable toggle buttons
 */

test.describe('list page toolbar buttons', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('device list: search, reset, sort, refresh all work without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);

    try {
      await page.goto('/#/device', {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      // Search
      const searchInput = page.getByPlaceholder(/device name|设备名称/i).first();
      if (await searchInput.isVisible().catch(() => false)) {
        await searchInput.fill('test_search');
        const mark1 = markHealth(health);
        const searchBtn = page.getByRole('button', {name: /Search|搜索/}).first();
        if (await searchBtn.isVisible().catch(() => false)) {
          await searchBtn.click();
          await waitForAppSettled(page);
          expectHealthy(health, mark1);
        }
      }

      // Reset
      const mark2 = markHealth(health);
      const resetBtn = page.getByRole('button', {name: /Reset|重置/}).first();
      if (await resetBtn.isVisible().catch(() => false)) {
        await resetBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark2);
      }

      // Sort
      const mark3 = markHealth(health);
      const sortBtn = page.getByRole('button', {name: /Sort|排序/}).first();
      if (await sortBtn.isVisible().catch(() => false)) {
        await sortBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark3);
      }

      // Refresh
      const mark4 = markHealth(health);
      const refreshBtn = page.getByRole('button', {name: /Refresh|刷新/}).first();
      if (await refreshBtn.isVisible().catch(() => false)) {
        await refreshBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark4);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('profile list: search, reset, sort, refresh all work without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);

    try {
      await page.goto('/#/profile', {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      // Search
      const searchInput = page.getByPlaceholder(/profile name|模板名称/i).first();
      if (await searchInput.isVisible().catch(() => false)) {
        await searchInput.fill('test');
        const mark1 = markHealth(health);
        const searchBtn = page.getByRole('button', {name: /Search|搜索/}).first();
        if (await searchBtn.isVisible().catch(() => false)) {
          await searchBtn.click();
          await waitForAppSettled(page);
          expectHealthy(health, mark1);
        }
      }

      // Reset
      const mark2 = markHealth(health);
      const resetBtn = page.getByRole('button', {name: /Reset|重置/}).first();
      if (await resetBtn.isVisible().catch(() => false)) {
        await resetBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark2);
      }

      // Sort
      const mark3 = markHealth(health);
      const sortBtn = page.getByRole('button', {name: /Sort|排序/}).first();
      if (await sortBtn.isVisible().catch(() => false)) {
        await sortBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark3);
      }

      // Refresh
      const mark4 = markHealth(health);
      const refreshBtn = page.getByRole('button', {name: /Refresh|刷新/}).first();
      if (await refreshBtn.isVisible().catch(() => false)) {
        await refreshBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark4);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('settings user list: search, reset, pagination work', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);

    try {
      await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      // Search
      const searchInput = page.getByPlaceholder(/user name|用户名/i).first();
      if (await searchInput.isVisible().catch(() => false)) {
        await searchInput.fill('dc3');
        const mark1 = markHealth(health);
        const searchBtn = page.getByRole('button', {name: /Search|搜索/}).first();
        if (await searchBtn.isVisible().catch(() => false)) {
          await searchBtn.click();
          await waitForAppSettled(page);
          expectHealthy(health, mark1);
        }
      }

      // Reset
      const mark2 = markHealth(health);
      const resetBtn = page.getByRole('button', {name: /Reset|重置/}).first();
      if (await resetBtn.isVisible().catch(() => false)) {
        await resetBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark2);
      }
    } finally {
      await e2eData.cleanup();
    }
  });
});

test.describe('edit page navigation buttons', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('device edit wizard: reset button restores original values', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      await page.goto(`/#/device/edit?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      // Find device name input and change it
      const nameInput = page.getByPlaceholder(/device name|设备名称/i).first();
      if (await nameInput.isVisible().catch(() => false)) {
        const originalName = await nameInput.inputValue();
        await nameInput.fill('Changed By Test');

        // Click Reset
        const resetBtn = page.getByRole('button', {name: /Reset|重置/});
        if (await resetBtn.isVisible().catch(() => false)) {
          const mark = markHealth(health);
          await resetBtn.click();
          await waitForAppSettled(page);
          expectHealthy(health, mark);

          // Verify original value restored
          const restored = await nameInput.inputValue();
          expect(restored).toBe(originalName);
        }
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('device edit tabs switch without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      await page.goto(`/#/device/edit?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      for (const tab of [
        /Driver Attributes|驱动属性/,
        /Point Attributes|位号属性/,
        /Related Commands|Device Commands|指令/,
        /Related Events|Device Events|事件/,
        /Device Info|设备信息/,
      ]) {
        const mark = markHealth(health);
        await expect(clickTab(page, tab), `${tab} should be reachable`).resolves.toBe(true);
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('profile edit wizard: reset button restores original values', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/edit?id=${profileId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      // Wait for profile data to load
      const nameInput = page.getByPlaceholder(/profile name|模板名称/i).first();
      await expect(nameInput).not.toHaveValue('', {timeout: 10_000});

      const originalName = await nameInput.inputValue();
      await nameInput.fill('Changed By Test');

      const resetBtn = page.getByRole('button', {name: /Reset|重置/});
      if (await resetBtn.isVisible().catch(() => false)) {
        const mark = markHealth(health);
        await resetBtn.click();
        await waitForAppSettled(page);
        expectHealthy(health, mark);

        const restored = await nameInput.inputValue();
        expect(restored).toBe(originalName);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('profile edit tabs switch without errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/edit?id=${profileId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      for (const tab of [
        /Related Points|Profile Points|模板位号|关联位号/,
        /Related Commands|Profile Commands|模板指令|关联指令/,
        /Related Events|Profile Events|模板事件|关联事件/,
        /Profile Info|模板信息/,
      ]) {
        const mark = markHealth(health);
        await expect(clickTab(page, tab), `${tab} should be reachable`).resolves.toBe(true);
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });
});

test.describe('detail page edit button navigation', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('device detail: edit button navigates to edit wizard', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      await page.goto(`/#/device/detail?id=${deviceId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      const editBtn = page.getByRole('button', {name: /Edit|编辑/}).first();
      if (await editBtn.isVisible().catch(() => false)) {
        const mark = markHealth(health);
        await editBtn.click();
        await waitForAppSettled(page);

        await expect(page.locator('.el-tabs__item').filter({hasText: /Device Info|设备信息/})).toBeVisible();
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('profile detail: edit button navigates to edit wizard', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/detail?id=${profileId}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      const editBtn = page.getByRole('button', {name: /Edit|编辑/}).first();
      if (await editBtn.isVisible().catch(() => false)) {
        const mark = markHealth(health);
        await editBtn.click();
        await waitForAppSettled(page);

        await expect(page.locator('.el-tabs__item').filter({hasText: /Profile Info|模板信息/})).toBeVisible();
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });
});
