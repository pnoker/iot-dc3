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

import * as ElementIcons from '@element-plus/icons-vue';
import type {Component} from 'vue';

/**
 * Full name → component lookup for every icon shipped by
 * `@element-plus/icons-vue` (~280 icons). Used by the menu edit picker and
 * anywhere a menu references an icon by string.
 *
 * We pull in the whole namespace because this is an internal admin UI where
 * bundle size is less important than letting operators pick any icon without
 * having to touch code. Tree-shakers still drop unused icons outside this
 * file; inside it, the lookup is O(1) by name.
 *
 * Entries are filtered to capitalized names referring to actual components,
 * so stray module-level exports (e.g. `__esModule`) don't leak through.
 */
export const iconMap: Record<string, Component> = Object.fromEntries(
  Object.entries(ElementIcons).filter(([name, comp]) => /^[A-Z]/.test(name) && comp != null && typeof comp === 'object')
) as Record<string, Component>;

/**
 * Alphabetically sorted list of icon names for UI pickers (stable order).
 */
export const iconNames: string[] = Object.keys(iconMap).sort((a, b) => a.localeCompare(b));

export const resolveIcon = (name?: string): Component | undefined => {
  if (!name) return undefined;
  return iconMap[name];
};
