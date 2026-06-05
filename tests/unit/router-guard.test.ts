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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

import { AUTH_HEADERS } from '@/config/constant/common';
import { setStorage } from '@/utils/storageUtil';

import { seedAuthStorage } from '../fixtures/auth';

// Mock the menu store so the guard doesn't crash calling fetchTree
vi.mock('@/store/modules/menu', () => ({
  useMenuStore: vi.fn(() => ({
    tree: [],
    loaded: true,
    loading: false,
    fetchTree: vi.fn(() => Promise.resolve()),
    reset: vi.fn(),
    findByCode: vi.fn(() => undefined),
  })),
}));

// CLAUDE.md flags this guard as a recurrent regression hotspot — every branch
// of `beforeEach` must call next() (or return true / a redirect target).
// We replace the heavy real route configs with stub records so the router
// can be instantiated in jsdom without dragging in the entire view tree.

const tokenMocks = vi.hoisted(() => ({
  checkTokenValid: vi.fn(() => Promise.resolve({ data: true })),
}));

vi.mock('@/api/token', () => tokenMocks);

const routeStubs = vi.hoisted(() => {
  const stubComponent = { render: () => null };
  const common: RouteRecordRaw[] = [
    { name: 'login', path: '/login', meta: { title: 'Login' }, component: stubComponent },
    { name: '404', path: '/404', component: stubComponent },
    { path: '/:catchAll(.*)', redirect: '/404' },
  ];
  const views: RouteRecordRaw = {
    name: 'home',
    path: '/',
    meta: { title: 'IoT DC3' },
    component: stubComponent,
  };
  const settings: RouteRecordRaw = {
    name: 'settings',
    path: '/settings',
    meta: { title: 'Settings' },
    component: stubComponent,
  };
  return { common, views, settings, operate: [] as RouteRecordRaw[] };
});

const nprogressMock = vi.hoisted(() => ({
  configure: vi.fn(),
  start: vi.fn(),
  done: vi.fn(),
  remove: vi.fn(),
}));

vi.mock('nprogress', () => ({ default: nprogressMock }));
// Stylesheet import in router/index.ts has no effect in unit tests but happy-dom
// resolves it through Vite's CSS handling — stub out to keep the import quiet.
vi.mock('nprogress/nprogress.css', () => ({}));

vi.mock('@/config/router/common', () => ({ default: routeStubs.common }));
vi.mock('@/config/router/views', () => ({ default: routeStubs.views }));
vi.mock('@/config/router/settings', () => ({ default: routeStubs.settings }));
vi.mock('@/config/router/operate', () => ({ default: routeStubs.operate }));

async function loadRouter() {
  vi.resetModules();
  const { default: router } = await import('@/config/router');
  return router;
}

describe('router beforeEach guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    document.title = '';
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('always allows navigation to the login page even when unauthenticated', async () => {
    const router = await loadRouter();

    await router.push({ name: 'login' });

    expect(router.currentRoute.value.name).toBe('login');
  });

  it('redirects to login when any of tenant/user/token is missing', async () => {
    const router = await loadRouter();

    await router.push({ name: 'home' });

    // Without credentials the guard returns { name: 'login' }.
    expect(router.currentRoute.value.name).toBe('login');
  });

  it('allows authenticated navigation and updates document.title from route meta', async () => {
    seedAuthStorage();
    const router = await loadRouter();

    await router.push({ name: 'settings' });

    expect(router.currentRoute.value.name).toBe('settings');
    expect(document.title).toBe('Settings');
  });

  it('redirects to login when only the token is missing', async () => {
    setStorage(AUTH_HEADERS.TENANT, 'acme');
    setStorage(AUTH_HEADERS.LOGIN, 'alice');
    // Token deliberately not set.
    const router = await loadRouter();

    await router.push({ name: 'home' });

    expect(router.currentRoute.value.name).toBe('login');
  });

  it('keeps document.title untouched when the route has no meta.title', async () => {
    seedAuthStorage();
    document.title = 'previous';
    const router = await loadRouter();

    // The catchAll route has no meta — the guard short-circuits the title
    // update and leaves whatever was there before.
    await router.push('/');
    // Home route has meta.title → it WILL update. Use 404 redirect instead.
    document.title = 'previous';
    await router.push('/totally-unknown-path');
    // The redirect target /404 has no meta.title → title preserved.
    expect(document.title).toBe('previous');
  });
});
