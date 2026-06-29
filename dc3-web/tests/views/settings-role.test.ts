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

const roleMocks = vi.hoisted(() => ({
  addRole: vi.fn(() => Promise.resolve({data: true})),
  deleteRole: vi.fn(() => Promise.resolve({data: true})),
  listRole: vi.fn(() => Promise.resolve({data: {records: [{id: 'r-1', roleName: 'Admin'}], total: 1}})),
  listRoleTree: vi.fn(() => Promise.resolve({data: []})),
  updateRole: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/role', () => roleMocks);
vi.mock('@/api/roleResourceBind', () => ({
  addRoleResourceBind: vi.fn(() => Promise.resolve({data: true})),
  deleteRoleResourceBind: vi.fn(() => Promise.resolve({data: true})),
}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsRole view', () => {
  it('loads the role list on mount', async () => {
    const Role = (await import('@/views/settings/role/Role.vue')).default;
    await mountListPage({
      component: Role,
      stubs: {
        roleTool: {template: '<div />'},
        roleAddForm: {template: '<div />'},
        roleEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    // Role page can call either listRole or listRoleTree on mount depending
    // on the active view; assert that at least one of them fired.
    expect(roleMocks.listRole.mock.calls.length + roleMocks.listRoleTree.mock.calls.length).toBeGreaterThan(0);
  });
});
