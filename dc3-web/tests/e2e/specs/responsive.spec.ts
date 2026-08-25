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

// Three-terminal gate (docs/design/frontend-three-terminal-ux.md):
// runs under chromium-desktop / chromium-tablet / chromium-mobile projects.
// Gates the A2 acceptance criterion (no page-level horizontal scroll from
// 360px to 2560px), the A3 shell adaptation (hamburger vs menu strip,
// aside vs drawer), and an A7 smoke probe (interactive controls expose
// accessible names). Run: pnpm exec playwright test responsive.spec.ts

import {expect, test, type Page} from '@playwright/test';

import {login, waitForAppSettled} from '../fixtures/app';

/** Page-level overflow must not exceed the viewport (A2 acceptance line). */
const expectNoHorizontalOverflow = async (page: Page, label: string) => {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth
  );
  expect(overflow, label + ': page-level horizontal overflow').toBeLessThanOrEqual(0);
};

const isMobileViewport = (page: Page) => (page.viewportSize()?.width ?? 1440) < 768;

test.describe('three-terminal gate', () => {
  test('login, home, and settings keep zero page-level overflow', async ({page}) => {
    await expectNoHorizontalOverflow(page, 'login');
    await login(page);
    await expectNoHorizontalOverflow(page, 'home');

    await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await expectNoHorizontalOverflow(page, 'settings');
  });

  test('shell adapts to the viewport (menu strip vs drawer, aside vs drawer)', async ({page}) => {
    await login(page);
    const mobile = isMobileViewport(page);

    if (mobile) {
      // Hamburger replaces the horizontal menu strip.
      await expect(page.locator('.header_menu_toggle')).toBeVisible();
      await expect(page.locator('.header_menu_wrap')).toHaveCount(0);

      // Drawer opens and carries the navigation tree.
      await page.locator('.header_menu_toggle').click();
      await expect(page.locator('.nav-drawer')).toBeVisible();
      await expect(page.locator('.nav-drawer')).toContainText('Home');
      await page.keyboard.press('Escape');

      // Settings: floating toggle + drawer replace the fixed aside.
      await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
      await expect(page.locator('.settings-aside')).toHaveCount(0);
      await expect(page.locator('.settings-aside-toggle')).toBeVisible();
      await page.locator('.settings-aside-toggle').click();
      await expect(page.locator('.settings-drawer')).toBeVisible();
    } else {
      // Desktop/tablet keep the horizontal menu strip (ellipsis mode).
      await expect(page.locator('.header_menu_toggle')).toBeHidden();
      await expect(page.locator('.header_menu_wrap')).toBeVisible();

      await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
      await expect(page.locator('.settings-aside')).toBeVisible();
    }
  });

  test('a11y smoke: visible interactive controls expose accessible names', async ({page}) => {
    await login(page);
    await page.goto('/#/settings/user', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    const unlabeled = await page.evaluate(() => {
      const out: Array<{ cls: string; text: string }> = [];
      document.querySelectorAll('button, [role="button"]').forEach((el) => {
        const node = el as HTMLElement;
        if (node.offsetParent === null) return; // hidden
        const name = node.getAttribute('aria-label') || node.getAttribute('title') || node.textContent?.trim() || '';
        if (!name) {
          out.push({cls: String(node.className).slice(0, 80), text: (node.textContent || '').slice(0, 60)});
        }
      });
      return out;
    });

    expect(unlabeled, 'visible controls without an accessible name').toEqual([]);
  });
});
