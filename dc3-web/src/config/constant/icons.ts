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
