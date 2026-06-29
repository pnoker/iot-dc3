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

import {
  apiPost,
  clickTab,
  ensureE2eData,
  expectHealthy,
  fillFirstEditableInput,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';

const PROFILE_TABS = [
  {active: 'profileConfig', label: /Profile Info|模板信息/},
  {active: 'pointConfig', label: /Related Points|Profile Points|模板位号|关联位号/},
  {active: 'commandConfig', label: /Related Commands|Profile Commands|模板指令|关联指令/},
  {active: 'eventConfig', label: /Related Events|Profile Events|模板事件|关联事件/},
] as const;

function uniqueName(prefix: string) {
  return `e2e_pw_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

function profileNameInput(page: Page) {
  return page.getByPlaceholder(/profile name|模板名称/i).first();
}

async function gotoProfileEdit(page: Page, profileId: string, active = 'profileConfig') {
  await page.goto(`/#/profile/edit?id=${profileId}&active=${active}`, {waitUntil: 'domcontentloaded'});
  await waitForAppSettled(page);
  await expect(page.locator('.el-tabs__item')).toHaveCount(PROFILE_TABS.length, {timeout: 10_000});
}

async function findCreatedId(page: Page, listUrl: string, nameField: string, name: string) {
  const response = await apiPost<{records?: Array<{id?: unknown}>}>(page, listUrl, {
    page: {current: 1, size: 1},
    [nameField]: name,
  });
  if (!response.data?.ok) return undefined;
  const id = response.data.data?.records?.[0]?.id;
  return id == null ? undefined : String(id);
}

test.describe('profile edit tabs', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('loads profile info and resets edited values', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await gotoProfileEdit(page, profileId!);

      const nameInput = profileNameInput(page);
      await expect(nameInput).not.toHaveValue('', {timeout: 10_000});
      const originalName = await nameInput.inputValue();
      await nameInput.fill(uniqueName('edited'));

      const mark = markHealth(health);
      await page.getByRole('button', {name: /Reset|重置/}).click();
      await waitForAppSettled(page);

      await expect(nameInput).toHaveValue(originalName);
      expectHealthy(health, mark);
    } finally {
      await e2eData.cleanup();
    }
  });

  test('switches through all tabs without browser or API errors', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      await gotoProfileEdit(page, profileId!);

      for (const tab of PROFILE_TABS.slice(1)) {
        const mark = markHealth(health);
        await expect(clickTab(page, tab.label), `${tab.active} tab should be reachable`).resolves.toBe(true);
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('opens every tab from the active query parameter', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();

    try {
      for (const tab of PROFILE_TABS) {
        const mark = markHealth(health);
        await gotoProfileEdit(page, profileId!, tab.active);
        await expect(page.locator('.el-tabs__item.is-active').filter({hasText: tab.label})).toBeVisible();
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('adds a command without params and does not duplicate-submit', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();
    const commandName = uniqueName('cmd');
    let commandId: string | undefined;

    try {
      await gotoProfileEdit(page, profileId!, 'commandConfig');

      const add = page.getByRole('button', {name: /^(Add|新增)$/}).first();
      await expect(add).toBeVisible({timeout: 10_000});
      await add.click();
      await waitForAppSettled(page);

      const dialog = page.locator('.el-dialog:visible').last();
      await expect(dialog).toBeVisible();
      await fillFirstEditableInput(dialog, commandName);

      const mark = markHealth(health);
      await dialog.getByRole('button', {name: /Confirm|确定/}).click();
      await waitForAppSettled(page);
      expectHealthy(health, mark);

      commandId = await findCreatedId(page, '/api/v3/manager/command/list', 'commandName', commandName);
      expect(commandId, 'created command id').toBeDefined();
    } finally {
      if (commandId) await apiPost(page, '/api/v3/manager/command/delete', {}, {id: commandId}).catch(() => {});
      await e2eData.cleanup();
    }
  });

  test('adds command params and event definitions from their tabs', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const profileId = e2eData.routeIds.profileId;
    expect(profileId, 'need a seeded profile').toBeDefined();
    const commandName = uniqueName('cmdp');
    const eventName = uniqueName('evt');
    let commandId: string | undefined;
    let eventId: string | undefined;

    try {
      await gotoProfileEdit(page, profileId!, 'commandConfig');
      await page
        .getByRole('button', {name: /^(Add|新增)$/})
        .first()
        .click();
      await waitForAppSettled(page);
      let dialog = page.locator('.el-dialog:visible').last();
      await fillFirstEditableInput(dialog, commandName);
      await dialog
        .getByRole('button', {name: /^(Add|新增)$/})
        .last()
        .click();
      const paramInputs = dialog.locator('table tbody tr').last().locator('input:not([readonly])');
      await expect(paramInputs.nth(0)).toBeVisible({timeout: 10_000});
      await paramInputs.nth(0).fill(`${commandName}_param`);
      await paramInputs.nth(1).fill(`${commandName}_code`);

      let mark = markHealth(health);
      await dialog.getByRole('button', {name: /Confirm|确定/}).click();
      await waitForAppSettled(page);
      expectHealthy(health, mark);
      commandId = await findCreatedId(page, '/api/v3/manager/command/list', 'commandName', commandName);
      expect(commandId, 'created command with params id').toBeDefined();

      await expect(clickTab(page, /Related Events|Profile Events|模板事件|关联事件/)).resolves.toBe(true);
      await page
        .getByRole('button', {name: /^(Add|新增)$/})
        .first()
        .click();
      await waitForAppSettled(page);
      dialog = page.locator('.el-dialog:visible').last();
      await fillFirstEditableInput(dialog, eventName);

      mark = markHealth(health);
      await dialog.getByRole('button', {name: /Confirm|确定/}).click();
      await waitForAppSettled(page);
      expectHealthy(health, mark);
      eventId = await findCreatedId(page, '/api/v3/manager/event/list', 'eventName', eventName);
      expect(eventId, 'created event id').toBeDefined();
    } finally {
      if (commandId) await apiPost(page, '/api/v3/manager/command/delete', {}, {id: commandId}).catch(() => {});
      if (eventId) await apiPost(page, '/api/v3/manager/event/delete', {}, {id: eventId}).catch(() => {});
      await e2eData.cleanup();
    }
  });
});
