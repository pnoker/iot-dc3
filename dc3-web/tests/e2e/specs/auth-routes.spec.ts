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

import {ensureE2eData, expectHealthy, login, markHealth, waitForAppSettled, watchPageHealth} from '../fixtures/app';
import {buildEntityRoutes, protectedRouteProbes, protectedRoutes, publicRoutes} from '../fixtures/routes';

test.describe('auth and route smoke', () => {
  test('unauthenticated protected routes redirect before calling business APIs', async ({page}) => {
    const health = watchPageHealth(page);

    for (const route of [...protectedRoutes, ...protectedRouteProbes]) {
      const mark = markHealth(health);
      await page.goto(`/#${route}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);

      await expect(page, route).toHaveURL(/\/login/);
      expect(health.businessApiRequests.slice(mark.businessApiRequests), route).toEqual([]);
    }
  });

  test('public and authenticated routes open without browser or API errors', async ({page}) => {
    const health = watchPageHealth(page);

    for (const route of publicRoutes) {
      const mark = markHealth(health);
      await page.goto(`/#${route}`, {waitUntil: 'domcontentloaded'});
      await waitForAppSettled(page);
      expectHealthy(health, mark);
    }

    await login(page);
    const e2eData = await ensureE2eData(page);

    try {
      for (const route of [...protectedRoutes, ...buildEntityRoutes(e2eData.routeIds)]) {
        const mark = markHealth(health);
        await page.goto(`/#${route}`, {waitUntil: 'domcontentloaded'});
        await waitForAppSettled(page);

        await expect(page, route).not.toHaveURL(/\/login/);
        await expect(page.locator('body')).not.toHaveText('');
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });
});
