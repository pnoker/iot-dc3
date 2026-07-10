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

import {expect, type Locator, test} from '@playwright/test';

import {
  clickButtonIfPresent,
  closeOverlay,
  ensureE2eData,
  expectHealthy,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';
import {interactionPages} from '../fixtures/routes';

async function expectEnableSegmented(container: Locator) {
  await expect(container.locator('.enable-flag-segmented:visible').first()).toBeVisible();
  await expect(
    container
      .locator('.el-form-item')
      .filter({hasText: /Enable/})
      .locator('.el-switch')
  ).toHaveCount(0);
  await expect(
    container
      .locator('.el-form-item')
      .filter({hasText: /Enable/})
      .locator('.el-select')
  ).toHaveCount(0);
}

test.describe('authenticated UI interactions', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  for (const pageDef of interactionPages) {
    test(`${pageDef.name} toolbar and safe actions`, async ({page}) => {
      const e2eData = await ensureE2eData(page);
      const health = watchPageHealth(page);

      try {
        await page.goto(`/#${pageDef.route}`, {waitUntil: 'domcontentloaded'});
        await waitForAppSettled(page);

        if (pageDef.placeholder) {
          const input = page.getByPlaceholder(pageDef.placeholder).first();
          if (await input.count()) {
            await input.fill(pageDef.value || '');
          }
        }

        let mark = markHealth(health);
        await clickButtonIfPresent(page, 'Search');
        expectHealthy(health, mark);

        mark = markHealth(health);
        await clickButtonIfPresent(page, 'Reset');
        expectHealthy(health, mark);

        const refreshOrSort = page.locator('.tool-card-footer-page button.el-button.is-circle:visible');
        for (let i = 0; i < Math.min(await refreshOrSort.count(), 2); i += 1) {
          mark = markHealth(health);
          await refreshOrSort.nth(i).click();
          await waitForAppSettled(page);
          expectHealthy(health, mark);
        }

        if (pageDef.paginate) {
          const next = page.locator('.el-pagination button.btn-next:visible').first();
          if ((await next.count()) && (await next.isEnabled().catch(() => false))) {
            mark = markHealth(health);
            await next.click();
            await waitForAppSettled(page);
            expectHealthy(health, mark);
          }
        }

        if (pageDef.addDisabled) {
          const add = page.getByRole('button', {name: 'Add'}).first();
          if (await add.count()) {
            await expect(add).toBeDisabled();
          }
        }

        if (pageDef.add) {
          mark = markHealth(health);
          if (await clickButtonIfPresent(page, 'Add')) {
            const dialog = page.locator('.el-dialog:visible').last();
            await expect(dialog).toBeVisible();
            if (pageDef.enableForm) {
              await expectEnableSegmented(dialog);
            }
            await closeOverlay(page);
            expectHealthy(health, mark);
          }
        }

        if (pageDef.importButton) {
          mark = markHealth(health);
          if (await clickButtonIfPresent(page, 'Import')) {
            await expect(page.locator('.el-dialog:visible').last()).toBeVisible();
            await closeOverlay(page);
            expectHealthy(health, mark);
          }
        }

        for (const action of ['Detail', 'Edit', 'Assign Roles', 'Assign Resources']) {
          mark = markHealth(health);
          if (await clickButtonIfPresent(page, action)) {
            await closeOverlay(page);
            expectHealthy(health, mark);
          }
        }
      } finally {
        await e2eData.cleanup();
      }
    });
  }

  test('entity edit forms use the common Enable segmented control', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const editRoutes: string[] = [];

    if (e2eData.routeIds.deviceId) editRoutes.push(`/device/edit?id=${e2eData.routeIds.deviceId}`);
    if (e2eData.routeIds.profileId) editRoutes.push(`/profile/edit?id=${e2eData.routeIds.profileId}`);
    try {
      for (const route of editRoutes) {
        const mark = markHealth(health);
        await page.goto(`/#${route}`, {waitUntil: 'domcontentloaded'});
        await waitForAppSettled(page);
        await expectEnableSegmented(page.locator('body'));
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });
});
