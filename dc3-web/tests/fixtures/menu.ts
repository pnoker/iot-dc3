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

import type {MenuNode} from '@/store';

/**
 * Three-level menu tree mirroring the production payload shape
 * (home + settings → users → user-detail). Used by menu-store tests,
 * resolveMenuTitle tests, and any future component that consumes
 * `findByCode` / `findById`.
 */
export const sampleMenuTree: MenuNode[] = [
  {
    id: 1,
    parentMenuId: 0,
    menuName: 'Home',
    menuCode: 'home',
    children: [],
  },
  {
    id: 2,
    parentMenuId: 0,
    menuName: 'Settings',
    menuCode: 'settings',
    children: [
      {
        id: 21,
        parentMenuId: 2,
        menuName: 'Users',
        menuCode: 'settings.user',
        children: [
          {
            id: 211,
            parentMenuId: 21,
            menuName: 'User detail',
            menuCode: 'settings.user.detail',
          },
        ],
      },
    ],
  },
];
