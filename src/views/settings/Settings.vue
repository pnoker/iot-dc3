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
    </el-main>
  </el-container>
</template>

<script lang="ts" setup>
  import { computed, onMounted } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';

  import { useMenuStore } from '@/store';
  import { resolveMenuTitle } from '@/utils/MenuUtil';

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
  const fallback: SidebarItem[] = [
    { name: 'settingsUser', title: t('nav.settingsUser'), icon: 'User' },
    { name: 'settingsRole', title: t('nav.settingsRole'), icon: 'UserFilled' },
    { name: 'settingsResource', title: t('nav.settingsResource'), icon: 'Key' },
    { name: 'settingsApi', title: t('nav.settingsApi'), icon: 'Link' },
    { name: 'settingsMenu', title: t('nav.settingsMenu'), icon: 'Menu' },
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
    return children
      .slice()
      .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
      .map(mapMenuNode);
  });

  const activeMenu = computed(() => String(route.name || 'settingsUser'));

  // Map API-tree menuCodes that don't match a vue-router route name.
  const ROUTE_ALIAS: Record<string, string> = {
    settingsEventOverview: 'settingsEvent',
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
    margin-top: 1px;

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
  }
</style>
