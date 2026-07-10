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

import {
  apiPost,
  clickButtonIfPresent,
  ensureE2eData,
  expectHealthy,
  login,
  markHealth,
  waitForAppSettled,
  watchPageHealth,
} from '../fixtures/app';

/**
 * Destructive delete regression — previously only covered by the standalone
 * browser-sweep runner. The flow is the same on every list page:
 *
 *   seed an entity via API → search by its unique name → click the row's
 *   Delete button → confirm in the popconfirm → verify the entity no longer
 *   appears in the list API.
 *
 * Each case carries its own seed payload; the per-page list/add URLs are
 * inlined here so the spec reads top-to-bottom without bouncing through
 * fixtures.
 */

interface DeleteCase {
  name: string;
  route: string;
  placeholder: RegExp;
  listUrl: string;
  addUrl: string;
  nameField: string;
  // Seed payload — needs the dependency ids that ensureE2eData(page) seeds
  // for us (driver / profile / api). We pass that bag in at runtime.
  seed: (uniqueName: string, deps: { driverId: string; profileId: string; apiId: string }) => Record<string, unknown>;
}

const deleteCases: DeleteCase[] = [
  {
    name: 'Profile',
    route: '/profile',
    placeholder: /profile name|模板名称/i,
    listUrl: '/api/v3/manager/profile/list',
    addUrl: '/api/v3/manager/profile/add',
    nameField: 'profileName',
    seed: (n) => ({profileName: n, profileCode: n, enableFlag: 'ENABLE', remark: 'e2e destructive'}),
  },
  {
    name: 'Device',
    route: '/device',
    placeholder: /device name|设备名称/i,
    listUrl: '/api/v3/manager/device/list',
    addUrl: '/api/v3/manager/device/add',
    nameField: 'deviceName',
    seed: (n, deps) => ({
      deviceName: n,
      deviceCode: n,
      driverId: deps.driverId,
      profileId: deps.profileId,
      enableFlag: 'ENABLE',
      remark: 'e2e destructive',
    }),
  },
  {
    name: 'Settings Role',
    route: '/settings/role',
    placeholder: /role name|角色名称/i,
    listUrl: '/api/v3/auth/role/list',
    addUrl: '/api/v3/auth/role/add',
    nameField: 'roleName',
    seed: (n) => ({
      parentRoleId: 0,
      roleName: n,
      // Role code must be ALL_CAPS by backend convention.
      roleCode: n.toUpperCase().replace(/-/g, '_'),
      enableFlag: 'ENABLE',
      remark: 'e2e destructive',
    }),
  },
  {
    name: 'Settings Resource',
    route: '/settings/resource',
    placeholder: /resource name|资源名称/i,
    listUrl: '/api/v3/auth/resource/list',
    addUrl: '/api/v3/auth/resource/add',
    nameField: 'resourceName',
    seed: (n, deps) => ({
      parentResourceId: 0,
      resourceName: n,
      resourceCode: n,
      resourceTypeFlag: 'API',
      resourceScopeFlag: 'LIST',
      entityId: deps.apiId,
      enableFlag: 'ENABLE',
      remark: 'e2e destructive',
    }),
  },
  {
    name: 'Settings Group',
    route: '/settings/group',
    placeholder: /group name|分组名称/i,
    listUrl: '/api/v3/manager/group/list',
    addUrl: '/api/v3/manager/group/add',
    nameField: 'groupName',
    seed: (n) => ({
      parentGroupId: 0,
      groupName: n,
      groupCode: n,
      groupTypeFlag: 'DEVICE',
      enableFlag: 'ENABLE',
      remark: 'e2e destructive',
    }),
  },
  {
    name: 'Settings Label',
    route: '/settings/label',
    placeholder: /label name|标签名称/i,
    listUrl: '/api/v3/manager/label/list',
    addUrl: '/api/v3/manager/label/add',
    nameField: 'labelName',
    seed: (n) => ({
      entityTypeFlag: 'DEVICE',
      labelName: n,
      labelCode: n,
      labelColor: '#F4F4F5',
      enableFlag: 'ENABLE',
      remark: 'e2e destructive',
    }),
  },
];

function uniqueName() {
  return `e2e_del_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`;
}

async function listCount(page: Page, listUrl: string, nameField: string, name: string) {
  const response = await apiPost<{ records?: unknown[]; total?: number }>(page, listUrl, {
    page: {current: 1, size: 1},
    [nameField]: name,
  });
  if (!response.data?.ok) return -1;
  return response.data.data?.records?.length ?? 0;
}

test.describe('destructive UI delete', () => {
  test.beforeEach(async ({page}) => {
    await login(page);
  });

  for (const testCase of deleteCases) {
    test(`${testCase.name}: search, delete via UI, and verify removal`, async ({page}) => {
      const e2eData = await ensureE2eData(page);
      const health = watchPageHealth(page);

      const deps = {
        driverId: e2eData.routeIds.driverId ?? '',
        profileId: e2eData.routeIds.profileId ?? '',
        apiId: e2eData.routeIds.apiId ?? '',
      };
      // The cases that need a dependency assert it exists — better to fail
      // here with a clear message than to send a half-empty add payload.
      if (testCase.name === 'Device') {
        expect(deps.driverId, 'driverId from ensureE2eData').not.toBe('');
        expect(deps.profileId, 'profileId from ensureE2eData').not.toBe('');
      }
      if (testCase.name === 'Settings Resource') {
        expect(deps.apiId, 'apiId from ensureE2eData').not.toBe('');
      }

      try {
        const name = uniqueName();
        const seed = await apiPost(page, testCase.addUrl, testCase.seed(name, deps));
        expect(seed.data?.ok, `${testCase.name} seed`).toBe(true);

        // Confirm the seed actually shows up in the list — protects
        // against silent backend acceptance that doesn't materialise.
        await expect
          .poll(() => listCount(page, testCase.listUrl, testCase.nameField, name), {
            message: `${testCase.name} should be findable after seed`,
            timeout: 10_000,
          })
          .toBe(1);

        await page.goto(`/#${testCase.route}`, {waitUntil: 'domcontentloaded'});
        await waitForAppSettled(page);

        const searchInput = page.getByPlaceholder(testCase.placeholder).first();
        await expect(searchInput, `${testCase.name} search input should be visible`).toBeVisible({timeout: 10_000});
        await searchInput.fill(name);

        const searchMark = markHealth(health);
        await clickButtonIfPresent(page, 'Search');
        expectHealthy(health, searchMark);

        // The seeded row must be visible — exact-text match avoids
        // catching unrelated rows that merely contain a substring.
        await expect(page.getByText(name, {exact: true}).first()).toBeVisible();

        const deleteMark = markHealth(health);
        await page.getByRole('button', {name: 'Delete'}).first().click();
        // Element Plus popconfirm renders Yes/Confirm in the active
        // locale — match both English and Chinese variants.
        await page
          .getByRole('button', {name: /^(Yes|Confirm|确定|确认)$/})
          .last()
          .click();
        await waitForAppSettled(page);
        expectHealthy(health, deleteMark);

        // Final source-of-truth check — list API confirms the row is gone.
        await expect
          .poll(() => listCount(page, testCase.listUrl, testCase.nameField, name), {
            message: `${testCase.name} must be absent after UI delete`,
            timeout: 10_000,
          })
          .toBe(0);
      } finally {
        await e2eData.cleanup();
      }
    });
  }
});
