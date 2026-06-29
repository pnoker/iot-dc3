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
