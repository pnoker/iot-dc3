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

import {flushPromises} from '@vue/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const apiMocks = vi.hoisted(() => ({
  listApi: vi.fn(() =>
    Promise.resolve({
      data: {records: [{id: 'api-1', apiName: '/api/v3/manager/device/list', enableFlag: 'ENABLE'}], total: 1},
    })
  ),
}));

vi.mock('@/api/api', () => apiMocks);

vi.mock('@/utils/notificationUtil', () => ({
  failMessage: vi.fn(),
  successMessage: vi.fn(),
}));

describe('SettingsApi view', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists APIs on mount', async () => {
    const SettingsApi = (await import('@/views/settings/api/Api.vue')).default;
    await mountListPage({
      component: SettingsApi,
      stubs: {
        apiTool: {template: '<section class="api-tool-stub" />'},
      },
      extraRoutes: [{name: 'settingsApiDetail', path: '/settings/api/detail', component: {template: '<div />'}}],
    });
    await flushPromises();

    expect(apiMocks.listApi).toHaveBeenCalledTimes(1);
    expect(apiMocks.listApi).toHaveBeenCalledWith(
      expect.objectContaining({page: expect.objectContaining({current: 1, size: 12})})
    );
  });
});
