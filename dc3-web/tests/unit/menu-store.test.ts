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
