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

import {defineStore} from 'pinia';

import {listMenuTree} from '@/api/menu';

export interface MenuNode {
  id: number | string;
  parentMenuId: number | string;
  menuName: string;
  menuCode: string;
  menuTypeFlag?: string;
  menuLevel?: string;
  menuIndex?: number;
  menuExt?: {
    content?: {
      // Authoritative locale → display-name map. Backed by
      // MenuExt.Content.titles on the server; populated via the menu CRUD
      // form (Chinese Title / English Title inputs).
      titles?: Record<string, string>;
      // Legacy i18n key (e.g. "nav.home") retained for backwards
      // compatibility with rows that pre-date the multilingual-titles
      // migration. New UI should treat this as a final fallback only.
      title?: string;
      icon?: string;
      url?: string;
      remark?: string;
    };
  };
  enableFlag?: string;
  children?: MenuNode[];
}

interface MenuState {
  tree: MenuNode[];
  loaded: boolean;
  loading: boolean;
}

export const useMenuStore = defineStore('menu', {
  state: (): MenuState => ({
    tree: [],
    loaded: false,
    loading: false,
  }),
  getters: {
    /**
     * Locate a menu subtree by menuCode. Used by settings sidebar which only
     * wants the children of the "settings" branch.
     */
    findByCode:
      (state) =>
      (code: string): MenuNode | undefined => {
        const walk = (nodes: MenuNode[]): MenuNode | undefined => {
          for (const n of nodes) {
            if (n.menuCode === code) return n;
            if (n.children && n.children.length) {
              const hit = walk(n.children);
              if (hit) return hit;
            }
          }
          return undefined;
        };
        return walk(state.tree);
      },

    /**
     * Locate a menu by numeric id. Used by Resource list to resolve
     * entity-id→name for MENU-typed resources, and by MenuDetail to render
     * the parent-menu name instead of a raw id.
     */
    findById:
      (state) =>
      (id: number | string): MenuNode | undefined => {
        const key = String(id);
        const walk = (nodes: MenuNode[]): MenuNode | undefined => {
          for (const n of nodes) {
            if (String(n.id) === key) return n;
            if (n.children && n.children.length) {
              const hit = walk(n.children);
              if (hit) return hit;
            }
          }
          return undefined;
        };
        return walk(state.tree);
      },
  },
  actions: {
    async fetchTree(force = false) {
      if (this.loaded && !force) return;
      if (this.loading) return;
      this.loading = true;
      try {
        const res: any = await listMenuTree({});
        this.tree = Array.isArray(res?.data) ? res.data : [];
        this.loaded = true;
      } catch {
        this.tree = [];
        // Keep failed loads retryable. The router will still deny non-public
        // routes against the empty tree for this navigation, but the next
        // navigation can recover if the backend/network comes back.
        this.loaded = false;
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.tree = [];
      this.loaded = false;
    },
  },
});
