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

import {on} from '../dispatch';
import {ok, okPage, responseOf} from '../response';
import {menuTree} from '../seed/menuTree';
import type {MenuNode} from '@/store/modules/menu';

/** Flatten the tree into a flat list for the Menu settings table. */
const flattenMenu = (nodes: MenuNode[]): MenuNode[] => {
  const out: MenuNode[] = [];
  const walk = (list: MenuNode[]) => {
    for (const node of list) {
      out.push(node);
      if (node.children?.length) walk(node.children);
    }
  };
  walk(nodes);
  return out;
};

export function registerMenuHandlers(): void {
  // Drives the router guard + Layout top nav. menu store takes res.data when
  // it is an array, so we wrap the tree in an R envelope.
  on('post', 'api/v3/auth/menu/list_tree', (ctx) => responseOf(ctx.config, ok(menuTree)));

  // Menu management table (settingsMenu route).
  on('post', 'api/v3/auth/menu/list', (ctx) =>
    responseOf(ctx.config, okPage(flattenMenu(menuTree))),
  );
}
