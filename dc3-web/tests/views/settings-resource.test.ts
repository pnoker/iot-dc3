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
import {describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const resourceMocks = vi.hoisted(() => ({
  addResource: vi.fn(() => Promise.resolve({data: true})),
  deleteResource: vi.fn(() => Promise.resolve({data: true})),
  listResourceTree: vi.fn(() => Promise.resolve({data: []})),
  updateResource: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/resource', () => resourceMocks);
vi.mock('@/api/api', () => ({listApi: vi.fn(() => Promise.resolve({data: {records: [], total: 0}}))}));
vi.mock('@/api/driver', () => ({listDriverByIds: vi.fn(() => Promise.resolve({data: {}}))}));
vi.mock('@/api/device', () => ({listDeviceByIds: vi.fn(() => Promise.resolve({data: {}}))}));
vi.mock('@/api/point', () => ({listPointByIds: vi.fn(() => Promise.resolve({data: {}}))}));
vi.mock('@/api/profile', () => ({listProfileByIds: vi.fn(() => Promise.resolve({data: {}}))}));
// Resource page awaits menuStore.fetchTree() before listResourceTree(); stub the
// menu store so the load chain doesn't stall on a real listMenuTree call.
vi.mock('@/api/menu', () => ({listMenuTree: vi.fn(() => Promise.resolve({data: []}))}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsResource view', () => {
  it('loads the resource tree on mount', async () => {
    const Resource = (await import('@/views/settings/resource/Resource.vue')).default;
    await mountListPage({
      component: Resource,
      stubs: {
        resourceTool: {template: '<div />'},
        resourceAddForm: {template: '<div />'},
        resourceEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(resourceMocks.listResourceTree).toHaveBeenCalledTimes(1);
  });
});
