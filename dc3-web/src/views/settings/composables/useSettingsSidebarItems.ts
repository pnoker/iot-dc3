/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Shared builder for the settings sidebar tree: the sidebar menu and the
// toolbar search consume the SAME items, so the fallback data, the menu-API
// mapping and the title resolution stay in one place. The menu API is
// authoritative once loaded; the static SETTINGS_FALLBACK_SIDEBAR covers
// offline/loading states.

import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

import {
  SETTINGS_FALLBACK_SIDEBAR,
  SETTINGS_TITLE_KEYS,
  type SettingsNavNode,
} from '@/config/settingsNav';
import {useMenuStore} from '@/store';
import {resolveMenuTitle} from '@/utils/menuUtil';

export interface SettingsSidebarItem {
  name: string;
  title: string;
  icon?: string;
  children?: SettingsSidebarItem[];
}

export const useSettingsSidebarItems = () => {
  const {t} = useI18n();
  const menuStore = useMenuStore();

  const toSidebarItem = (node: SettingsNavNode): SettingsSidebarItem => ({
    name: node.name,
    title: t(node.titleKey),
    icon: node.icon,
    children: node.children?.map(toSidebarItem),
  });

  // Static fallback shown when the menu API is unreachable or still loading.
  // `icon` holds the globally-registered element-plus icon component name.
  const fallbackItems = (): SettingsSidebarItem[] => SETTINGS_FALLBACK_SIDEBAR.map(toSidebarItem);

  const menuTitle = (node: any) => {
    const titleKey = SETTINGS_TITLE_KEYS[node.menuCode];
    return titleKey ? t(titleKey) : resolveMenuTitle(node);
  };

  const mapMenuNode = (node: any): SettingsSidebarItem => ({
    name: node.menuCode,
    title: menuTitle(node),
    icon: node.menuExt?.content?.icon,
    children: node.children?.length
      ? node.children
        .slice()
        .sort((a: any, b: any) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
        .map(mapMenuNode)
      : undefined,
  });

  const sidebarItems = computed<SettingsSidebarItem[]>(() => {
    const settings = menuStore.findByCode('settings');
    const children = settings?.children || [];
    if (menuStore.loaded) {
      // The menu API is authoritative once loaded; the DB already encodes the group tree.
      return children.length
        ? children
          .slice()
          .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
          .map(mapMenuNode)
        : [];
    }
    return fallbackItems();
  });

  return {sidebarItems};
};
