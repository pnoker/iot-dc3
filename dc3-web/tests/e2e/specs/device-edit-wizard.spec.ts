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
  apiGet,
  apiPost,
  clickTab,
  ensureE2eData,
  expectHealthy,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';

const DEVICE_TABS = [
  {active: 'deviceConfig', label: /Device Info|设备信息/},
  {active: 'driverConfig', label: /Driver Attributes|驱动属性/},
  {active: 'pointConfig', label: /Point Attributes|位号属性/},
  {active: 'commandConfig', label: /Related Commands|Device Commands|指令/},
  {active: 'eventConfig', label: /Related Events|Device Events|事件/},
] as const;

function uniqueName(prefix: string) {
  return `e2e_dev_${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

function deviceNameInput(page: Page) {
  return page.getByPlaceholder(/device name|设备名称/i).first();
}

async function gotoDeviceEdit(page: Page, deviceId: string, active = 'deviceConfig') {
  await page.goto(`/#/device/edit?id=${deviceId}&active=${active}`, {waitUntil: 'domcontentloaded'});
  await waitForAppSettled(page);
  await expect(page.locator('.el-tabs__item')).toHaveCount(DEVICE_TABS.length, {timeout: 10_000});
}

async function findRecordId(page: Page, listUrl: string, nameField: string, name: string) {
  const response = await apiPost<{records?: Array<{id?: unknown}>}>(page, listUrl, {
    page: {current: 1, size: 1},
    [nameField]: name,
  });
  if (!response.data?.ok) return undefined;
  const id = response.data.data?.records?.[0]?.id;
  return id == null ? undefined : String(id);
}

async function createDriverAttribute(page: Page, driverId: string) {
  const name = uniqueName('attr');
  const add = await apiPost(page, '/api/v3/manager/driver_attribute/add', {
    attributeName: name,
    attributeCode: name,
    attributeTypeFlag: 'STRING',
    defaultValue: '',
    driverId,
    enableFlag: 'ENABLE',
    remark: 'created by e2e device edit test',
  });
  expect(add.data?.ok, 'driver attribute add').toBe(true);

  const id = await findRecordId(page, '/api/v3/manager/driver_attribute/list', 'attributeName', name);
  expect(id, 'created driver attribute id').toBeDefined();
  return {id: id!, name};
}

async function createDevice(page: Page, driverId: string, profileId: string) {
  const name = uniqueName('device');
  const add = await apiPost(page, '/api/v3/manager/device/add', {
    deviceName: name,
    deviceCode: name,
    driverId,
    profileId,
    enableFlag: 'ENABLE',
    remark: 'created by e2e device edit test',
  });
  expect(add.data?.ok, 'device add').toBe(true);

  const id = await findRecordId(page, '/api/v3/manager/device/list', 'deviceName', name);
  expect(id, 'created device id').toBeDefined();
  return {id: id!, name};
}

test.describe('device edit tabs', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  test('loads device info and resets edited values', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      await gotoDeviceEdit(page, deviceId!);

      const nameInput = deviceNameInput(page);
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
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      await gotoDeviceEdit(page, deviceId!);

      for (const tab of DEVICE_TABS.slice(1)) {
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
    const deviceId = e2eData.routeIds.deviceId;
    expect(deviceId, 'need a seeded device').toBeDefined();

    try {
      for (const tab of DEVICE_TABS) {
        const mark = markHealth(health);
        await gotoDeviceEdit(page, deviceId!, tab.active);
        await expect(page.locator('.el-tabs__item.is-active').filter({hasText: tab.label})).toBeVisible();
        expectHealthy(health, mark);
      }
    } finally {
      await e2eData.cleanup();
    }
  });

  test('saves driver attribute config with add first and update on re-save', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const health = watchPageHealth(page);
    const driverId = e2eData.routeIds.driverId;
    const profileId = e2eData.routeIds.profileId;
    expect(driverId, 'need a seeded driver').toBeDefined();
    expect(profileId, 'need a seeded profile').toBeDefined();

    let attributeId: string | undefined;
    let deviceId: string | undefined;

    try {
      const attribute = await createDriverAttribute(page, driverId!);
      attributeId = attribute.id;
      const device = await createDevice(page, driverId!, profileId!);
      deviceId = device.id;

      await gotoDeviceEdit(page, deviceId, 'driverConfig');
      const row = page.locator('tr').filter({hasText: attribute.name}).first();
      await expect(row).toBeVisible({timeout: 10_000});
      await row.locator('input:not([readonly]):not([disabled])').first().fill('first_value');

      let mark = markHealth(health);
      await page.getByRole('button', {name: /Save All|保存全部/}).click();
      await waitForAppSettled(page);
      expectHealthy(health, mark);

      const configsAfterAdd = await apiGet<unknown[]>(
        page,
        '/api/v3/manager/driver_attribute_config/list_by_device_id',
        {device_id: deviceId}
      );
      const savedConfig = (
        (configsAfterAdd.data as {data?: Array<{attributeId?: unknown; id?: unknown}>}).data || []
      ).find((item) => String(item.attributeId) === attributeId);
      expect(savedConfig?.id, 'driver attribute config should be created').toBeDefined();

      await gotoDeviceEdit(page, deviceId, 'driverConfig');
      await expect(row).toBeVisible({timeout: 10_000});
      await row.locator('input:not([readonly]):not([disabled])').first().fill('updated_value');

      mark = markHealth(health);
      await page.getByRole('button', {name: /Save All|保存全部/}).click();
      await waitForAppSettled(page);
      expectHealthy(health, mark);

      const configsAfterUpdate = await apiGet<unknown[]>(
        page,
        '/api/v3/manager/driver_attribute_config/list_by_device_id',
        {device_id: deviceId}
      );
      const matchingConfigs = (
        (
          configsAfterUpdate.data as {
            data?: Array<{attributeId?: unknown}>;
          }
        ).data || []
      ).filter((item) => String(item.attributeId) === attributeId);
      expect(matchingConfigs).toHaveLength(1);
    } finally {
      if (deviceId) await apiPost(page, '/api/v3/manager/device/delete', {}, {id: deviceId}).catch(() => {});
      if (attributeId)
        await apiPost(page, '/api/v3/manager/driver_attribute/delete', {}, {id: attributeId}).catch(() => {});
      await e2eData.cleanup();
    }
  });

  test('reopens driver attribute tab with existing config values', async ({page}) => {
    const e2eData = await ensureE2eData(page);
    const driverId = e2eData.routeIds.driverId;
    const profileId = e2eData.routeIds.profileId;
    expect(driverId, 'need a seeded driver').toBeDefined();
    expect(profileId, 'need a seeded profile').toBeDefined();

    let attributeId: string | undefined;
    let deviceId: string | undefined;

    try {
      const attribute = await createDriverAttribute(page, driverId!);
      attributeId = attribute.id;
      const device = await createDevice(page, driverId!, profileId!);
      deviceId = device.id;

      const addConfig = await apiPost(page, '/api/v3/manager/driver_attribute_config/add', {
        attributeId,
        deviceId,
        configValue: 'persisted_value',
        enableFlag: 'ENABLE',
      });
      expect(addConfig.data?.ok, 'driver attribute config add').toBe(true);

      await gotoDeviceEdit(page, deviceId, 'driverConfig');
      const row = page.locator('tr').filter({hasText: attribute.name}).first();
      await expect(row.locator('input:not([readonly]):not([disabled])').first()).toHaveValue('persisted_value');
    } finally {
      if (deviceId) await apiPost(page, '/api/v3/manager/device/delete', {}, {id: deviceId}).catch(() => {});
      if (attributeId)
        await apiPost(page, '/api/v3/manager/driver_attribute/delete', {}, {id: attributeId}).catch(() => {});
      await e2eData.cleanup();
    }
  });
});
