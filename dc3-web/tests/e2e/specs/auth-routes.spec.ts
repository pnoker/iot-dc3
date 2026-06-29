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
