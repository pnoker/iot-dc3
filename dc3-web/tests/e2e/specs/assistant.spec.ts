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

import {expect, type Page, test} from '@playwright/test';

import {expectHealthy, login, waitForAppSettled, watchPageHealth} from '../fixtures/app';

/**
 * Agent assistant e2e spec.
 *
 * The black-box contract for the assistant, in one login session:
 *   - header entry opens the panel; Escape closes it (graded exit from workbench)
 *   - blank sends are no-ops; hostile payloads render inert (no script exec)
 *   - workbench mode records its state in the URL and survives a reload
 *   - the sessions rail item menu drives rename/archive
 */

const composer = (page: Page) => page.locator('.el-textarea__inner').first();

test.describe('agent assistant', () => {
  test('opens from the header and closes with Escape', async ({page}) => {
    const health = watchPageHealth(page);
    await login(page);
    await waitForAppSettled(page);

    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator('.agentic-panel')).toHaveCount(0);
    expectHealthy(health);
  });

  test('never submits blank drafts and renders hostile payloads inert', async ({page}) => {
    const health = watchPageHealth(page);
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    // let the conversation load settle before snapshotting the count
    await page.waitForTimeout(1500);
    const before = await page.locator('.agentic-message').count();
    await composer(page).fill('   ');
    await composer(page).press('Enter');
    await page.waitForTimeout(1200);
    await expect(page.locator('.agentic-message')).toHaveCount(before);

    const payload = '<img src=x onerror="window.__pwned=1"> hostile **markdown**';
    await composer(page).fill(payload);
    await composer(page).press('Enter');
    // the user bubble shows the raw text; no script executes
    await expect(page.locator('.agentic-message--user').last()).toContainText('hostile');
    await page.waitForTimeout(500);
    expect(await page.evaluate(() => (window as unknown as {__pwned?: number}).__pwned)).toBeUndefined();
    expectHealthy(health);
  });

  test('records workbench state in the URL and restores it after reload', async ({page}, testInfo) => {
    const health = watchPageHealth(page);
    const isMobile = testInfo.project.name.includes('mobile');
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    await expect(page.locator('.agentic-panel')).toBeVisible();

    // mobile hides the workbench entry by design — assert that contract too
    if (isMobile) {
      await expect(page.getByRole('button', {name: /全屏|full.?screen/i})).toHaveCount(0);
      expectHealthy(health);
      return;
    }
    // enter workbench mode
    const expand = page.getByRole('button', {name: /全屏|full.?screen/i}).first();
    await expand.click();
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible();

    // pick a session so the URL records the selection too
    const item = page.locator('.agentic-sessions__item-main').first();
    if (await item.count()) await item.click();
    await page.waitForTimeout(600);
    expect(page.url()).toMatch(/agentic=workbench/);
    expect(page.url()).toMatch(/session=/);

    await page.reload();
    await waitForAppSettled(page);
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible({timeout: 15000});
    expectHealthy(health);
  });

  test('renames a conversation through the rail item menu', async ({page}, testInfo) => {
    const health = watchPageHealth(page);
    const isMobile = testInfo.project.name.includes('mobile');
    await login(page);
    await waitForAppSettled(page);
    await page.locator('.agentic-launcher').click();
    if (isMobile) {
      // no workbench rail on mobile — the docked header keeps its own actions
      await expect(page.locator('.agentic-sessions')).toHaveCount(0);
      expectHealthy(health);
      return;
    }
    await page.getByRole('button', {name: /全屏|full.?screen/i}).first().click();
    await expect(page.locator('.agentic-panel--expanded')).toBeVisible();

    const item = page.locator('.agentic-sessions__item').first();
    await item.hover();
    await item.locator('.agentic-sessions__item-more').click();
    await page.getByRole('menuitem', {name: /重命名|Rename/}).first().click();
    const dialog = page.locator('.el-dialog:visible, .el-overlay:visible').last();
    await expect(dialog).toBeVisible();
    await dialog.locator('input').first().fill('e2e renamed');
    await dialog.getByRole('button', {name: /保存|[Ss]ave/}).first().click();
    await expect(page.locator('.agentic-sessions__item-main').first()).toContainText('e2e renamed');
    expectHealthy(health);
  });
});
