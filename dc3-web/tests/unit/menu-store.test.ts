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

import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';

import {useMenuStore} from '@/store';

import {sampleMenuTree} from '../fixtures/menu';

const menuMocks = vi.hoisted(() => ({
  listMenuTree: vi.fn(),
}));

vi.mock('@/api/menu', () => menuMocks);

describe('menu store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    menuMocks.listMenuTree.mockResolvedValue({data: sampleMenuTree});
  });

  describe('fetchTree', () => {
    it('loads the tree once and skips subsequent calls without force', async () => {
      const store = useMenuStore();

      await store.fetchTree();
      await store.fetchTree();
      await store.fetchTree();

      expect(menuMocks.listMenuTree).toHaveBeenCalledTimes(1);
      expect(store.tree).toEqual(sampleMenuTree);
      expect(store.loaded).toBe(true);
      expect(store.loading).toBe(false);
    });

    it('refetches when force=true', async () => {
      const store = useMenuStore();

      await store.fetchTree();
      await store.fetchTree(true);

      expect(menuMocks.listMenuTree).toHaveBeenCalledTimes(2);
    });

    it('leaves the tree empty when the API rejects', async () => {
      menuMocks.listMenuTree.mockRejectedValueOnce(new Error('boom'));
      const store = useMenuStore();

      await store.fetchTree();

      // Network failures must not throw — UI falls back to a static Home.
      expect(store.tree).toEqual([]);
      expect(store.loaded).toBe(false);
      expect(store.loading).toBe(false);
    });

    it('coerces non-array payloads to an empty tree', async () => {
      menuMocks.listMenuTree.mockResolvedValueOnce({data: {not: 'an array'}});
      const store = useMenuStore();

      await store.fetchTree();

      expect(store.tree).toEqual([]);
      expect(store.loaded).toBe(true);
    });
  });

  describe('getters', () => {
    it('finds a node by code at any depth', async () => {
      const store = useMenuStore();
      await store.fetchTree();

      expect(store.findByCode('home')?.id).toBe(1);
      expect(store.findByCode('settings.user')?.id).toBe(21);
      expect(store.findByCode('settings.user.detail')?.id).toBe(211);
      expect(store.findByCode('does-not-exist')).toBeUndefined();
    });

    it('finds a node by id (string or number) at any depth', async () => {
      const store = useMenuStore();
      await store.fetchTree();

      expect(store.findById(2)?.menuCode).toBe('settings');
      expect(store.findById('21')?.menuCode).toBe('settings.user');
      expect(store.findById(211)?.menuCode).toBe('settings.user.detail');
      expect(store.findById(999)).toBeUndefined();
    });
  });

  describe('reset', () => {
    it('drops the cached tree so the next fetch hits the API again', async () => {
      const store = useMenuStore();
      await store.fetchTree();
      expect(store.loaded).toBe(true);

      store.reset();

      expect(store.tree).toEqual([]);
      expect(store.loaded).toBe(false);

      await store.fetchTree();
      expect(menuMocks.listMenuTree).toHaveBeenCalledTimes(2);
    });
  });
});
