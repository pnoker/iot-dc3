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
