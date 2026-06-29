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

const menuMocks = vi.hoisted(() => ({
  addMenu: vi.fn(() => Promise.resolve({data: true})),
  deleteMenu: vi.fn(() => Promise.resolve({data: true})),
  listMenuTree: vi.fn(() => Promise.resolve({data: [{id: 'm-1', menuName: 'Home', children: []}]})),
  updateMenu: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/menu', () => menuMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsMenu view', () => {
  it('lists the menu tree on mount', async () => {
    const Menu = (await import('@/views/settings/menu/Menu.vue')).default;
    await mountListPage({
      component: Menu,
      stubs: {
        menuTool: {template: '<div />'},
        menuAddForm: {template: '<div />'},
        menuEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(menuMocks.listMenuTree).toHaveBeenCalledTimes(1);
  });
});
