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

import {flushPromises, mount} from '@vue/test-utils';
import {createMemoryHistory, createRouter} from 'vue-router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';

import i18n from '@/config/i18n';
import {layoutStubs} from '../setup/stubs/element-plus';
import {sampleMenuTree} from '../fixtures/menu';

// Layout reaches through several stores and the router push singleton.
// Mock at the boundaries — store, router default export, agentic child.
const layoutMocks = vi.hoisted(() => ({
  routerPush: vi.fn(() => Promise.resolve()),
  logout: vi.fn(() => Promise.resolve()),
}));

vi.mock('@/config/router', () => ({
  default: {push: layoutMocks.routerPush, currentRoute: {value: {path: '/home'}}},
}));

vi.mock('@/components/agentic/AgenticAssistant.vue', () => ({
  default: {name: 'AgenticAssistant', template: '<div class="agentic-stub" />'},
}));

vi.mock('@/store', async () => {
  const actual = await vi.importActual<typeof import('@/store')>('@/store');
  return {
    ...actual,
    useAuthStore: () => ({logout: layoutMocks.logout}),
  };
});

async function mountLayout() {
  setActivePinia(createPinia());
  const {useMenuStore} = await import('@/store');
  const menuStore = useMenuStore();
  // Seed the store directly — bootstrap() would otherwise hit the menu API.
  menuStore.tree = sampleMenuTree;
  menuStore.loaded = true;

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {path: '/', redirect: '/home'},
      {path: '/home', name: 'home', component: {template: '<div class="home-route" />'}},
      {path: '/login', name: 'login', component: {template: '<div />'}},
    ],
  });
  await router.push('/home');
  await router.isReady();

  const Layout = (await import('@/components/layout/Layout.vue')).default;

  return mount(Layout, {
    global: {
      plugins: [i18n, router],
      stubs: {
        ...layoutStubs,
        ElBacktop: {template: '<div class="el-backtop-stub" />'},
        RouterView: {template: '<div class="router-view-stub" />'},
      },
    },
  });
}

describe('Layout', () => {
  beforeEach(() => {
    layoutMocks.routerPush.mockClear();
    layoutMocks.logout.mockClear();
  });

  it('renders the top nav with the home menu and the menu store tree', async () => {
    const wrapper = await mountLayout();
    await flushPromises();

    // Home item is always present.
    expect(wrapper.text()).toContain('Home');
    // First top-level node from sampleMenuTree renders via resolveMenuTitle.
    expect(wrapper.text()).toContain(sampleMenuTree[0].menuName);

    // Router-view is mounted into the body shell.
    expect(wrapper.find('.router-view-stub').exists()).toBe(true);
  });
});
