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
