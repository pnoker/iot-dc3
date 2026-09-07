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

import {expectHealthy, login, logout, markHealth, waitForAppSettled, watchPageHealth} from '../fixtures/app';

test.describe('logout', () => {
  test('clearing the auth session forces a redirect to /login on next protected nav', async ({page}) => {
    const health = watchPageHealth(page);

    await login(page);
    await page.goto('/#/home', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await expect(page, 'authenticated /home should not redirect').not.toHaveURL(/\/login/);

    const authenticatedBeforeLogout = await page.evaluate(() => sessionStorage.getItem('dc3-authenticated'));
    expect(authenticatedBeforeLogout, 'auth session marker should exist before logout').not.toBeNull();

    const logoutMark = markHealth(health);
    await logout(page);

    // Tenant/login are local identity hints; the session marker is the
    // frontend-visible authentication state. The access token itself is an
    // httpOnly cookie and must never be readable from JavaScript storage.
    expect(await page.evaluate(() => localStorage.getItem('X-Auth-Tenant'))).toBeNull();
    expect(await page.evaluate(() => localStorage.getItem('X-Auth-Login'))).toBeNull();
    expect(await page.evaluate(() => sessionStorage.getItem('dc3-authenticated'))).toBeNull();

    // Subsequent protected nav must redirect — proves the guard reads
    // storage on every navigation, not only on app boot.
    await page.goto('/#/home', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
    await expect(page, 'protected nav after logout must redirect to /login').toHaveURL(/\/login/);

    expectHealthy(health, logoutMark);
  });

  test('navigating to a protected route after logout never calls business APIs', async ({page}) => {
    const health = watchPageHealth(page);

    await login(page);
    await logout(page);

    // Reset the per-route business-API counter to zero before the
    // post-logout navigation so we can assert the guard short-circuits
    // the request before any /api/v3 call goes out. This is the safety
    // contract that keeps a logged-out browser from leaking stale
    // requests against the backend.
    const mark = markHealth(health);
    await page.goto('/#/device', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    await expect(page).toHaveURL(/\/login/);
    expect(health.businessApiRequests.slice(mark.businessApiRequests), '/device after logout').toEqual([]);
  });
});
