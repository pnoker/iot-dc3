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
        <el-menu :default-active="activeMenu" @select="onSelect">
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
      </el-card>
    </el-aside>
    <el-main class="settings-main">
      <router-view />
      <!-- No backtop here: .settings-main is no longer a scroll container
           (see overflow override below), so the Layout-level <el-backtop>
           on .body .el-scrollbar__wrap handles all settings sub-pages. -->
    </el-main>
  </el-container>
</template>

<script lang="ts" setup>
  import { computed, onMounted } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';

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

  // Static fallback shown when the menu API is unreachable or still loading.
  // `icon` holds the globally-registered element-plus icon component name.
  const modelGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsModel',
    title: t('nav.settingsModel'),
    icon: 'Cpu',
    children: children?.length
      ? children
      : [
          { name: 'settingsAgentic', title: t('nav.settingsAgentic'), icon: 'ChatDotRound' },
          { name: 'settingsAgenticProvider', title: t('nav.settingsAgenticProvider'), icon: 'ChatLineSquare' },
        ],
  });

  const alarmGroup = (children?: SidebarItem[]): SidebarItem => ({
    name: 'settingsAlarm',
    title: t('nav.settingsAlarm'),
    icon: 'AlarmClock',
    children: children?.length
      ? children
      : [
          { name: 'settingsAlarmRule', title: t('nav.settingsAlarmRule'), icon: 'SetUp' },
          { name: 'settingsAlarmNotify', title: t('nav.settingsAlarmNotify'), icon: 'Bell' },
          { name: 'settingsAlarmMessage', title: t('nav.settingsAlarmMessage'), icon: 'Message' },
          { name: 'settingsAlarmChannel', title: t('nav.settingsAlarmChannel'), icon: 'Connection' },
          { name: 'settingsAlarmBind', title: t('nav.settingsAlarmBind'), icon: 'Link' },
          { name: 'settingsAlarmState', title: t('nav.settingsAlarmState'), icon: 'Monitor' },
          { name: 'settingsAlarmRecord', title: t('nav.settingsAlarmRecord'), icon: 'DocumentChecked' },
        ],
  });

  const fallback: SidebarItem[] = [
    { name: 'settingsUser', title: t('nav.settingsUser'), icon: 'User' },
    { name: 'settingsRole', title: t('nav.settingsRole'), icon: 'UserFilled' },
    { name: 'settingsResource', title: t('nav.settingsResource'), icon: 'Key' },
    { name: 'settingsApi', title: t('nav.settingsApi'), icon: 'Link' },
    { name: 'settingsMenu', title: t('nav.settingsMenu'), icon: 'Menu' },
    { name: 'settingsGroup', title: t('nav.settingsGroup'), icon: 'Grid' },
    { name: 'settingsLabel', title: t('nav.settingsLabel'), icon: 'CollectionTag' },
    alarmGroup(),
    modelGroup(),
    {
      name: 'settingsEvent',
      title: t('nav.settingsEvent'),
      icon: 'Bell',
      children: [
        { name: 'settingsEvent', title: t('nav.settingsEventOverview'), icon: 'DataLine' },
        { name: 'settingsDriverEvent', title: t('nav.settingsDriverEvent'), icon: 'Promotion' },
        { name: 'settingsDeviceEvent', title: t('nav.settingsDeviceEvent'), icon: 'Management' },
      ],
    },
  ];

  const mapMenuNode = (node: any): SidebarItem => ({
    name: node.menuCode,
    title: resolveMenuTitle(node),
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
    if (!children.length) return fallback;
    const items = children
      .slice()
      .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
      .map(mapMenuNode);
    ensureDirectItem(items, { name: 'settingsGroup', title: t('nav.settingsGroup'), icon: 'Grid' }, [
      'settingsLabel',
      'settingsModel',
      'settingsEvent',
      'settingsAbout',
    ]);
    ensureDirectItem(items, { name: 'settingsLabel', title: t('nav.settingsLabel'), icon: 'CollectionTag' }, [
      'settingsAlarm',
      'settingsModel',
      'settingsEvent',
      'settingsAbout',
    ]);
    ensureAlarmGroup(items);
    ensureModelGroup(items);
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
    for (const name of ['settingsAgentic', 'settingsAgenticProvider']) {
      const index = items.findIndex((item) => item.name === name);
      if (index >= 0) {
        const removed = items.splice(index, 1)[0];
        if (removed) extracted.push(removed);
      }
    }

    const model = items.find((item) => item.name === 'settingsModel');
    if (model) {
      model.children = model.children || [];
      if (!hasItem(model.children, 'settingsAgentic')) {
        model.children.push(
          extracted.find((item) => item.name === 'settingsAgentic') || {
            name: 'settingsAgentic',
            title: t('nav.settingsAgentic'),
            icon: 'ChatDotRound',
          }
        );
      }
      if (!hasItem(model.children, 'settingsAgenticProvider')) {
        model.children.push(
          extracted.find((item) => item.name === 'settingsAgenticProvider') || {
            name: 'settingsAgenticProvider',
            title: t('nav.settingsAgenticProvider'),
            icon: 'ChatLineSquare',
          }
        );
      }
      return;
    }

    insertBefore(items, modelGroup(extracted), ['settingsEvent', 'settingsAbout']);
  };

  const ensureAlarmGroup = (items: SidebarItem[]) => {
    const alarmChildNames = [
      'settingsAlarmRule',
      'settingsAlarmNotify',
      'settingsAlarmMessage',
      'settingsAlarmChannel',
      'settingsAlarmBind',
      'settingsAlarmState',
      'settingsAlarmRecord',
    ];
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

  const activeMenu = computed(() => String(route.name || 'settingsUser'));

  // Map API-tree menuCodes that don't match a vue-router route name.
  const ROUTE_ALIAS: Record<string, string> = {
    settingsEventOverview: 'settingsEvent',
    settingsAlarm: 'settingsAlarmRule',
  };

  const onSelect = (name: string) => {
    router.push({ name: ROUTE_ALIAS[name] || name });
  };
</script>

<style lang="scss" scoped>
  .settings-container {
    align-items: stretch;
    gap: 4px;
    min-height: calc(100vh - 120px);
  }

  .settings-aside {
    // No margin-top. The previous 1px was meant to line up with the
    // tool-card's old 1px top margin, but that margin is gone now; any
    // non-zero value here leaves the aside sitting lower than the main
    // content so the breadcrumb→aside gap reads as "bigger" than the
    // breadcrumb→content gap on every settings sub-page (including
    // About, which has no tool-card at all).
    .settings-aside-card {
      height: 100%;
      border: 0;
      display: flex;
      flex-direction: column;

      :deep(.el-card__body) {
        flex: 1;
        padding: 0;
      }
    }

    :deep(.el-menu) {
      height: 100%;
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
    // el-main ships with overflow:auto by design (aside-doesn't-scroll,
    // main-scrolls layout). We don't want that here — the Layout-level
    // el-scrollbar already owns page scrolling, and an additional auto
    // overflow on main gave flex-row a 1px rounding window where a ghost
    // scrollbar would appear, consuming wheel events whenever the cursor
    // hovered any settings card. Reset to visible so wheel always reaches
    // the Layout scrollbar above.
    overflow: visible;
  }
</style>
