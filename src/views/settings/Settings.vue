<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <el-container class="settings-container">
    <el-aside class="settings-aside" width="220px">
      <el-card class="settings-aside-card" shadow="never">
        <el-scrollbar>
          <el-menu :default-active="activeMenu" :default-openeds="defaultOpeneds" @select="onSelect">
            <template v-for="item in sidebarItems" :key="item.name">
              <el-sub-menu v-if="item.children?.length" :index="item.name">
                <template #title>
                  <el-icon v-if="item.icon">
                    <component :is="item.icon" />
                  </el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.name" :index="child.name">
                  <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                  <span>{{ child.title }}</span>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="item.name">
                <el-icon v-if="item.icon">
                  <component :is="item.icon" />
                </el-icon>
                <span>{{ item.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-card>
    </el-aside>
    <el-main class="settings-main">
      <el-scrollbar>
        <router-view />
      </el-scrollbar>
    </el-main>
  </el-container>
</template>

<script lang="ts" setup>
  import { computed, onMounted } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';

  import {
    getSettingsActiveName,
    getSettingsDefaultOpeneds,
    getSettingsRouteName,
    SETTINGS_ALARM_CHILDREN,
    SETTINGS_COMMAND_CHILDREN,
    SETTINGS_EVENT_CHILDREN,
    SETTINGS_FALLBACK_ICON,
    SETTINGS_FALLBACK_SIDEBAR,
    SETTINGS_MODEL_CHILDREN,
    SETTINGS_TITLE_KEYS,
    type SettingsNavNode,
  } from '@/config/settingsNav';
  import { useMenuStore } from '@/store';
  import { resolveMenuTitle } from '@/utils/menuUtil';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();
  const menuStore = useMenuStore();

  onMounted(() => {
    // Force a refetch so the sidebar always reflects the latest menu tree
    // when entering Settings (the Layout-level fetch is cached once loaded).
    menuStore.fetchTree(true);
  });

  interface SidebarItem {
    name: string;
    title: string;
    icon?: string;
    children?: SidebarItem[];
  }

  const toSidebarItem = (node: SettingsNavNode): SidebarItem => ({
    name: node.name,
    title: t(node.titleKey),
    icon: node.icon,
    children: node.children?.map(toSidebarItem),
  });

  const fallbackItems = (): SidebarItem[] => SETTINGS_FALLBACK_SIDEBAR.map(toSidebarItem);

  // Static fallback shown when the menu API is unreachable or still loading.
  // `icon` holds the globally-registered element-plus icon component name.
  const modelGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsModel',
    title: t('nav.settingsModel'),
    icon: SETTINGS_FALLBACK_ICON.settingsModel,
    children: children?.length ? children : SETTINGS_MODEL_CHILDREN.map(toSidebarItem),
  });

  const alarmGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsAlarm',
    title: t('nav.settingsAlarm'),
    icon: SETTINGS_FALLBACK_ICON.settingsAlarm,
    children: children?.length ? children : SETTINGS_ALARM_CHILDREN.map(toSidebarItem),
  });

  const eventGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsEvent',
    title: t('nav.settingsEvent'),
    icon: SETTINGS_FALLBACK_ICON.settingsEvent,
    children: children?.length ? children : SETTINGS_EVENT_CHILDREN.map(toSidebarItem),
  });

  const commandGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsCommand',
    title: t('nav.settingsCommand'),
    icon: SETTINGS_FALLBACK_ICON.settingsCommand,
    children: children?.length ? children : SETTINGS_COMMAND_CHILDREN.map(toSidebarItem),
  });

  const menuTitle = (node: any) => {
    const titleKey = node.menuCode === 'settingsEvent' ? 'nav.settingsEvent' : SETTINGS_TITLE_KEYS[node.menuCode];
    return titleKey ? t(titleKey) : resolveMenuTitle(node);
  };

  const mapMenuNode = (node: any): SidebarItem => ({
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

  const sidebarItems = computed<SidebarItem[]>(() => {
    const settings = menuStore.findByCode('settings');
    const children = settings?.children || [];
    if (!children.length) return menuStore.loaded ? [] : fallbackItems();
    const items = children
      .slice()
      .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
      .map(mapMenuNode);
    if (menuStore.loaded) {
      return items;
    }
    ensureDirectItem(items, { name: 'settingsGroup', title: t('nav.settingsGroup'), icon: 'Grid' }, [
      'settingsLabel',
      'settingsModel',
      'settingsEvent',
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsPrincipal', title: t('nav.settingsPrincipal'), icon: 'Avatar' }, [
      'settingsTenantMembership',
      'settingsRole',
      'settingsRolePrincipalBind',
      'settingsResource',
      'settingsApi',
      'settingsMenu',
      'settingsAbout',
    ]);
    ensureDirectItem(
      items,
      { name: 'settingsTenantMembership', title: t('nav.settingsTenantMembership'), icon: 'OfficeBuilding' },
      [
        'settingsLocalCredential',
        'settingsRole',
        'settingsRolePrincipalBind',
        'settingsResource',
        'settingsApi',
        'settingsAbout',
      ]
    );
    ensureDirectItem(
      items,
      { name: 'settingsLocalCredential', title: t('nav.settingsLocalCredential'), icon: 'Lock' },
      [
        'settingsIdentityAudit',
        'settingsRole',
        'settingsRolePrincipalBind',
        'settingsResource',
        'settingsApi',
        'settingsAbout',
      ]
    );
    ensureDirectItem(
      items,
      { name: 'settingsIdentityAudit', title: t('nav.settingsIdentityAudit'), icon: 'DocumentChecked' },
      ['settingsRole', 'settingsRolePrincipalBind', 'settingsResource', 'settingsApi', 'settingsAbout']
    );
    ensureDirectItem(
      items,
      { name: 'settingsRolePrincipalBind', title: t('nav.settingsRolePrincipalBind'), icon: 'Link' },
      ['settingsResource', 'settingsApi', 'settingsMenu', 'settingsAbout']
    );
    ensureDirectItem(items, { name: 'settingsLabel', title: t('nav.settingsLabel'), icon: 'CollectionTag' }, [
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsServiceAccount', title: t('nav.settingsServiceAccount'), icon: 'Key' }, [
      'settingsMcpServer',
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsMcpServer', title: t('nav.settingsMcpServer'), icon: 'Connection' }, [
      'settingsMcpAudit',
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsMcpAudit', title: t('nav.settingsMcpAudit'), icon: 'Document' }, [
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsAbout', title: t('nav.settingsAbout'), icon: 'InfoFilled' }, []);
    ensureAlarmGroup(items);
    ensureModelGroup(items);
    ensureCommandGroup(items);
    ensureEventGroup(items);
    return items;
  });

  const hasItem = (items: SidebarItem[], name: string): boolean => {
    return items.some((item) => item.name === name || hasItem(item.children || [], name));
  };

  const insertBefore = (items: SidebarItem[], item: SidebarItem, beforeNames: string[]) => {
    const index = items.findIndex((candidate) => beforeNames.includes(candidate.name));
    if (index >= 0) {
      items.splice(index, 0, item);
    } else {
      items.push(item);
    }
  };

  const ensureDirectItem = (items: SidebarItem[], item: SidebarItem, beforeNames: string[]) => {
    if (hasItem(items, item.name)) return;
    insertBefore(items, item, beforeNames);
  };

  const ensureModelGroup = (items: SidebarItem[]) => {
    const extracted: SidebarItem[] = [];
    for (const name of SETTINGS_MODEL_CHILDREN.map((item) => item.name)) {
      const index = items.findIndex((item) => item.name === name);
      if (index >= 0) {
        const removed = items.splice(index, 1)[0];
        if (removed) extracted.push(removed);
      }
    }

    const model = items.find((item) => item.name === 'settingsModel');
    if (model) {
      model.children = model.children || [];
      if (!hasItem(model.children, 'settingsModelConfig')) {
        model.children.push(
          extracted.find((item) => item.name === 'settingsModelConfig') || {
            name: 'settingsModelConfig',
            title: t('nav.settingsModelConfig'),
            icon: SETTINGS_FALLBACK_ICON.settingsModelConfig,
          }
        );
      }
      if (!hasItem(model.children, 'settingsModelProvider')) {
        model.children.push(
          extracted.find((item) => item.name === 'settingsModelProvider') || {
            name: 'settingsModelProvider',
            title: t('nav.settingsModelProvider'),
            icon: SETTINGS_FALLBACK_ICON.settingsModelProvider,
          }
        );
      }
      return;
    }

    insertBefore(items, modelGroup(extracted), ['settingsEvent', 'settingsAbout']);
  };

  const ensureAlarmGroup = (items: SidebarItem[]) => {
    const alarmChildNames = SETTINGS_ALARM_CHILDREN.map((item) => item.name);
    const extracted: SidebarItem[] = [];
    for (const name of alarmChildNames) {
      const index = items.findIndex((item) => item.name === name);
      if (index >= 0) {
        const removed = items.splice(index, 1)[0];
        if (removed) extracted.push(removed);
      }
    }

    const alarm = items.find((item) => item.name === 'settingsAlarm');
    if (alarm) {
      alarm.children = alarm.children?.length ? alarm.children : alarmGroup(extracted).children;
      return;
    }

    insertBefore(items, alarmGroup(extracted), ['settingsModel', 'settingsEvent', 'settingsAbout']);
  };

  const ensureCommandGroup = (items: SidebarItem[]) => {
    const commandChildNames = SETTINGS_COMMAND_CHILDREN.map((item) => item.name);
    const extracted: SidebarItem[] = [];
    for (const name of commandChildNames) {
      const index = items.findIndex((item) => item.name === name);
      if (index >= 0) {
        const removed = items.splice(index, 1)[0];
        if (removed) extracted.push(removed);
      }
    }

    const command = items.find((item) => item.name === 'settingsCommand');
    if (command) {
      command.children = command.children?.length ? command.children : commandGroup(extracted).children;
      return;
    }

    insertBefore(items, commandGroup(extracted), ['settingsAbout']);
  };

  const ensureEventGroup = (items: SidebarItem[]) => {
    const eventChildNames = SETTINGS_EVENT_CHILDREN.map((item) => item.name);
    const extracted: SidebarItem[] = [];
    for (const name of eventChildNames) {
      const index = items.findIndex((item) => item.name === name);
      if (index >= 0) {
        const removed = items.splice(index, 1)[0];
        if (removed) extracted.push(removed);
      }
    }

    const event = items.find((item) => item.name === 'settingsEvent');
    if (event) {
      event.children = event.children?.length ? event.children : eventGroup(extracted).children;
      return;
    }

    insertBefore(items, eventGroup(extracted), ['settingsAbout']);
  };

  const activeMenu = computed(() => {
    const name = String(route.name || 'settingsUser');
    return getSettingsActiveName(name);
  });

  const defaultOpeneds = computed(() => {
    return getSettingsDefaultOpeneds(activeMenu.value);
  });

  const onSelect = (name: string) => {
    router.push({ name: getSettingsRouteName(name) });
  };
</script>

<style lang="scss" scoped>
  .settings-container {
    align-items: stretch;
    gap: 4px;
    height: 100%;
    min-width: 0;
  }

  .settings-aside {
    .settings-aside-card {
      height: 100%;
      border: 0;
      display: flex;
      flex-direction: column;

      :deep(.el-card__body) {
        flex: 1;
        padding: 0;
        min-height: 0;
        overflow: hidden;
      }

      :deep(.el-scrollbar) {
        height: 100%;
      }
    }

    :deep(.el-menu) {
      border-right: none;
    }

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .settings-main {
    padding: 0;
    min-width: 0;
    overflow: hidden;

    > .el-scrollbar {
      height: 100%;
    }
  }
</style>
