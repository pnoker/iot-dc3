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
