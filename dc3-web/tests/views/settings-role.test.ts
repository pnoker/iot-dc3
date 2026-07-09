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
