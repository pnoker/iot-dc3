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

const userMocks = vi.hoisted(() => ({
  addUser: vi.fn(() => Promise.resolve({data: true})),
  deleteUser: vi.fn(() => Promise.resolve({data: true})),
  listUser: vi.fn(() => Promise.resolve({data: {records: [{id: 'u-1', nickName: 'Alice'}], total: 1}})),
  updateUser: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/user', () => userMocks);
vi.mock('@/api/rolePrincipalBind', () => ({
  addRolePrincipalBind: vi.fn(() => Promise.resolve({data: true})),
  deleteRolePrincipalBind: vi.fn(() => Promise.resolve({data: true})),
}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsUser view', () => {
  it('lists users on mount', async () => {
    const User = (await import('@/views/settings/user/User.vue')).default;
    await mountListPage({
      component: User,
      stubs: {
        userTool: {template: '<div />'},
        userAddForm: {template: '<div />'},
        userEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(userMocks.listUser).toHaveBeenCalledTimes(1);
  });
});
