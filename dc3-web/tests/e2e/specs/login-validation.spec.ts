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

import {waitForAppSettled} from '../fixtures/app';

/**
 * Login form must reject obviously bad input client-side and bad
 * credentials server-side without ever leaving /login. These are the
 * happy-path's evil twin — they were the biggest gap in the e2e suite
 * before this spec.
 *
 * `pnpm test:e2e` (and CI) serves the dev-mode build (`vite build --mode
 * dev`) through scripts/testing/e2e-server.mjs — there is no in-process
 * mock in this mode, so login requests are real network traffic that
 * specs can stub with `page.route`.
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

  test('persists language and theme preferences before authentication', async ({page}) => {
    await page.getByRole('button', {name: '中', exact: true}).click();
    await expect(page.getByRole('heading', {name: '欢迎回来'})).toBeVisible();

    await page.getByRole('button', {name: '深色主题'}).click();
    await expect(page.locator('html')).toHaveClass(/dark/);

    await page.reload();
    await waitForAppSettled(page);
    await expect(page.getByRole('heading', {name: '欢迎回来'})).toBeVisible();
    await expect(page.locator('html')).toHaveClass(/dark/);
    await expect(page.getByRole('button', {name: '深色主题'})).toHaveAttribute('aria-pressed', 'true');
  });

  test('keeps the user on /login when the backend rejects the credentials', async ({page}) => {
    // The dev build talks HTTP, so stub the auth API at the network layer to
    // keep this deterministic regardless of backend health. The stub bodies
    // mirror the production wire contract (see src/mock/response.ts): a raw
    // payload on success and RFC 9457 problem details on failure.
    await page.route('**/api/v3/auth/token/salt', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify('forced-salt'),
      })
    );
    await page.route('**/api/v3/auth/token/generate', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          type: 'about:blank',
          title: 'Invalid credentials',
          status: 401,
          code: 'R4010',
          detail: 'Invalid credentials',
        }),
      })
    );

    await page.goto('/#/login', {waitUntil: 'domcontentloaded'});
    await waitForAppSettled(page);

    // A well-formed password, so the rejection below can only come from the
    // backend response — not from client-side validation.
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

    // The rejection must surface to the user: the auth store reports the
    // failed login with an error notification (src/utils/notificationUtil.ts).
    await expect(page.locator('.el-notification .el-notification--error').first()).toBeVisible();

    // A failed login must not leave a frontend-visible authenticated session.
    expect(await page.evaluate(() => sessionStorage.getItem('dc3-authenticated'))).toBeNull();
  });
});
