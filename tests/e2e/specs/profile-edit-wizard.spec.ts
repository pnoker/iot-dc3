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

import { expect, test } from '@playwright/test';

import { ensureE2eData, expectHealthy, login, markHealth, waitForAppSettled, watchPageHealth } from '../fixtures/app';

/**
 * Profile Edit Wizard e2e spec.
 *
 * Covers the 5-step profile configuration wizard:
 *   Step 0 — Profile info (name, enable, remark)
 *   Step 1 — Point config (add/edit/delete points)
 *   Step 2 — Command config (add/edit/delete commands with params)
 *   Step 3 — Event config (add/edit/delete events with params)
 *   Step 4 — Completion screen
 *
 * Key regression: adding a command without params must NOT trigger
 * the "Command has been duplicated" R500 error (double-submit race).
 */

function uniqueName(prefix: string) {
  return `e2e_pw_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

/** Wait for the profile name input to be populated (API data loaded) */
async function waitForProfileData(page: import('@playwright/test').Page) {
  const nameInput = page.locator('.edit-card-body input').first();
  await expect(nameInput).not.toHaveValue('', { timeout: 10_000 });
}

async function clickNext(page: import('@playwright/test').Page) {
  const nextButtons = page.getByRole('button', { name: /Next|下一步/ });
  const count = await nextButtons.count();
  if (count > 0) {
    await nextButtons.last().click();
    await waitForAppSettled(page);
  }
}

async function clickPrevious(page: import('@playwright/test').Page) {
  const prevButtons = page.getByRole('button', { name: /Previous|上一步/ });
  const count = await prevButtons.count();
  if (count > 0) {
    await prevButtons.last().click();
    await waitForAppSettled(page);
  }
}

/** Navigate directly to a wizard step using the ?active=N query param */
async function gotoStep(page: import('@playwright/test').Page, profileId: string, step: number) {
  await page.goto(`/#/profile/edit?id=${profileId}&active=${step}`, { waitUntil: 'domcontentloaded' });
  await waitForAppSettled(page);
}

test.describe('profile edit wizard', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('loads step 0 and shows profile form fields', async ({ page }) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/edit?id=${profileId}`, { waitUntil: 'domcontentloaded' });
      await waitForAppSettled(page);

      await expect(page.locator('.el-steps')).toBeVisible();

      const mark = markHealth(health);
      expectHealthy(health, mark);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('navigates through all wizard steps without errors', async ({ page }) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/edit?id=${profileId}`, { waitUntil: 'domcontentloaded' });
      await waitForAppSettled(page);

      // Wait for profile data to load before clicking Next on step 0
      // (step 0 Next triggers profileUpdate which requires a valid form)
      await waitForProfileData(page);

      // Step 0 -> Step 1
      const s0 = markHealth(health);
      await clickNext(page);
      expectHealthy(health, s0);

      // Step 1 -> Step 2
      const s1 = markHealth(health);
      await clickNext(page);
      expectHealthy(health, s1);

      // Step 2 -> Step 3
      const s2 = markHealth(health);
      await clickNext(page);
      expectHealthy(health, s2);

      // Step 3 -> Step 4 (completion)
      const s3 = markHealth(health);
      await clickNext(page);
      expectHealthy(health, s3);

      await expect(page.locator('.el-result')).toBeVisible();

      await page.getByRole('button', { name: /Return|返回/ }).click();
      await waitForAppSettled(page);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('Previous button navigates back through steps', async ({ page }) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await page.goto(`/#/profile/edit?id=${profileId}`, { waitUntil: 'domcontentloaded' });
      await waitForAppSettled(page);
      await waitForProfileData(page);

      // Step 0 -> Step 1
      await clickNext(page);

      // Step 1 -> Step 0
      const mark = markHealth(health);
      await clickPrevious(page);
      expectHealthy(health, mark);

      await expect(page.locator('.el-steps')).toBeVisible();
    } finally {
      await e2eData.cleanup();
    }
  });

  test('add command without params on step 2 does not trigger duplicate error', async ({ page }) => {
    const health = watchPageHealth(page);
    const e2eData = await ensureE2eData(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      // Jump directly to step 2 (Command Config) to avoid step 0 validation
      await gotoStep(page, profileId, 2);

      // Click the Add button on the command config step
      const addBtn = page.getByRole('button', { name: /Add|新增/ }).first();
      await expect(addBtn).toBeVisible({ timeout: 10_000 });
      await addBtn.click();
      await waitForAppSettled(page);

      // Fill command name (required)
      const dialog = page.locator('.el-dialog:visible').last();
      await expect(dialog).toBeVisible();
      const nameInput = dialog.locator('input').first();
      await nameInput.fill(uniqueName('cmd'));

      // Submit without adding any params
      const confirmBtn = dialog.getByRole('button', { name: /Confirm|确定/ });
      const saveMark = markHealth(health);
      await confirmBtn.click();
      await waitForAppSettled(page);

      // The key assertion: no bad API responses (no R500 duplicate error)
      expectHealthy(health, saveMark);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('add command with params on step 2 saves successfully', async ({ page }) => {
    const health = watchPageHealth(page);
    const e2eData = await ensureE2eData(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await gotoStep(page, profileId, 2);

      await page
        .getByRole('button', { name: /Add|新增/ })
        .first()
        .click();
      await waitForAppSettled(page);

      const dialog = page.locator('.el-dialog:visible').last();
      await expect(dialog).toBeVisible();
      const cmdName = uniqueName('cmdp');

      // Fill command name
      await dialog.locator('input').first().fill(cmdName);

      // Add a param row
      const addParamBtn = dialog.getByRole('button', { name: /Add|新增/ });
      if (await addParamBtn.count()) {
        await addParamBtn.last().click();
        await waitForAppSettled(page);

        // Fill param fields in the table
        const paramRow = dialog.locator('table tbody tr').last();
        const paramInputs = paramRow.locator('input');

        // paramName
        if ((await paramInputs.count()) > 0) await paramInputs.nth(0).fill('test_param');
        // paramCode
        if ((await paramInputs.count()) > 1) await paramInputs.nth(1).fill('test_code');
      }

      const saveMark = markHealth(health);
      await dialog.getByRole('button', { name: /Confirm|确定/ }).click();
      await waitForAppSettled(page);

      expectHealthy(health, saveMark);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('add event on step 3 saves without errors', async ({ page }) => {
    const health = watchPageHealth(page);
    const e2eData = await ensureE2eData(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await gotoStep(page, profileId, 3);

      await page
        .getByRole('button', { name: /Add|新增/ })
        .first()
        .click();
      await waitForAppSettled(page);

      const dialog = page.locator('.el-dialog:visible').last();
      await expect(dialog).toBeVisible();
      await dialog.locator('input').first().fill(uniqueName('evt'));

      const saveMark = markHealth(health);
      await dialog.getByRole('button', { name: /Confirm|确定/ }).click();
      await waitForAppSettled(page);

      expectHealthy(health, saveMark);
    } finally {
      await e2eData.cleanup();
    }
  });
});
