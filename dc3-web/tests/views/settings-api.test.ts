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
