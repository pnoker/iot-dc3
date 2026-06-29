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

import {expect, type Page, test} from '@playwright/test';

import {waitForAppSettled} from '../fixtures/app';

/**
 * Login form must reject obviously bad input client-side and bad
 * credentials server-side without ever leaving /login. These are the
 * happy-path's evil twin — they were the biggest gap in the e2e suite
 * before this spec.
 *
 * The selectors target stable structural surfaces (`.login-form`,
 * `.login-submit`, prop-bound `el-form-item`) rather than i18n strings,
 * so the same spec works against zh and en builds.
 */

const passwordInput = (page: Page) => page.locator('.login-form input[type="password"]');

const submitButton = (page: Page) => page.locator('.login-submit');

test.describe('login form validation', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/#/login', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);
  });

  test('blocks submission and surfaces an error when the password is empty', async ({page}) => {
    // The form pre-fills tenant/name/password defaults — clear password
    // to drive the "required" rule. We don't rely on i18n text; instead
    // we look at the structural `is-error` state Element Plus toggles
    // on the FormItem.
    await passwordInput(page).fill('');
    await submitButton(page).click();

    // First-party assertion: the page never leaves /login.
    await expect(page).toHaveURL(/\/login/);

    // Element Plus exposes the validation state via classes. Any
    // `is-error` form item proves the rule fired.
    await expect(page.locator('.login-form .el-form-item.is-error')).toHaveCount(1);
  });

  test('rejects passwords shorter than the 6-character minimum rule', async ({page}) => {
    await passwordInput(page).fill('12345');
    await submitButton(page).click();

    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('.login-form .el-form-item.is-error')).toHaveCount(1);
  });

  test('keeps the user on /login when the backend rejects the credentials', async ({page}) => {
    // Stub the auth API at the network layer so this test stays
    // deterministic regardless of backend health. We intercept *before*
    // navigating to /login below so the route handlers are armed by the
    // time the user clicks Login.
    await page.route('**/api/v3/auth/token/salt', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ok: true, code: 200, data: 'forced-salt'}),
      })
    );
    await page.route('**/api/v3/auth/token/generate', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ok: false, code: 401, message: 'Invalid credentials', data: null}),
      })
    );

    await page.goto('/#/login', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    await passwordInput(page).fill('dc3dc3dc3');

    // Arm the response waiter before clicking. The route is fulfilled
    // synchronously by Playwright, so waiting after the click can miss
    // the already completed 401 response.
    const rejectedLogin = page.waitForResponse(
      (response) => response.url().includes('/api/v3/auth/token/generate') && response.status() === 401
    );
    await submitButton(page).click();
    await rejectedLogin;

    // The 401 must NOT eject the user from /login (we're already there).
    // The axios interceptor redirects 401 callers from protected routes
    // back to /login; from /login itself, it stays put.
    await expect(page).toHaveURL(/\/login/);

    // No auth headers should have been persisted on a failed login.
    expect(await page.evaluate(() => localStorage.getItem('X-Auth-Token'))).toBeNull();
  });
});
