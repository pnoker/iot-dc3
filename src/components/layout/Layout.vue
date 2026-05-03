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
  <div class="container">
    <div class="header">
      <el-col :span="4" class="header_item">
        <img class="header_logo" src="/images/logo/logo.png" />
      </el-col>
      <el-col :span="16" class="header_item">
        <el-menu :default-active="handleMenuEnter($route.path)" :router="true" class="header_menu" mode="horizontal">
          <el-menu-item index="/home">
            <el-icon>
              <HomeFilled />
            </el-icon>
            {{ t('nav.home') }}
          </el-menu-item>
          <template v-for="node in topLevelMenus" :key="`menu-${node.id}`">
            <el-sub-menu v-if="node.children && node.children.length > 0" :index="`node-${node.id}`">
              <template #title>
                <el-icon v-if="node.menuExt?.content?.icon">
                  <component :is="node.menuExt.content.icon" />
                </el-icon>
                {{ resolveMenuTitle(node) }}
              </template>
              <el-menu-item
                v-for="child in node.children"
                :key="`menu-child-${child.id}`"
                :index="child.menuExt?.content?.url || `/${child.menuCode}`"
              >
                {{ resolveMenuTitle(child) }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="node.menuExt?.content?.url || `/${node.menuCode}`">
              <el-icon v-if="node.menuExt?.content?.icon">
                <component :is="node.menuExt.content.icon" />
              </el-icon>
              {{ resolveMenuTitle(node) }}
            </el-menu-item>
          </template>
        </el-menu>
      </el-col>
      <el-col :span="4" class="header_item header_user">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user_avatar">
            <el-avatar>
              <img src="/images/common/avatar.png" />
            </el-avatar>
            <span class="user_name">{{ t('layout.admin') }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <!-- Inline language switch. @click.stop keeps the dropdown
                   open while the user toggles locales; the native
                   <el-dropdown-item> variants would close the menu on
                   every click. -->
              <li class="user_lang_row" @click.stop>
                <span class="user_lang_label">{{ t('layout.language') }}</span>
                <el-segmented v-model="langModel" :options="langOptions" size="small" />
              </li>
              <el-dropdown-item divided command="settings" :icon="Setting">
                {{ t('layout.settings') }}
              </el-dropdown-item>
              <el-dropdown-item command="help" :icon="QuestionFilled">{{ t('layout.about') }}</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton">{{ t('layout.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </div>
    <div class="body">
      <el-scrollbar ref="scrollbarRef">
        <div v-if="breadcrumbItems.length > 1" class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path" :to="item.path">
              <span class="breadcrumb__item">
                <el-icon v-if="item.icon" class="breadcrumb__icon">
                  <component :is="item.icon" />
                </el-icon>
                <span>{{ item.title }}</span>
              </span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <router-view />
      </el-scrollbar>
      <el-backtop :right="40" :bottom="40" target=".body .el-scrollbar__wrap" />
    </div>
  </div>
</template>

<script lang="ts" setup>
  import router from '@/config/router';
  import { HomeFilled, QuestionFilled, Setting, SwitchButton } from '@element-plus/icons-vue';
  import { computed, onMounted } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useAuthStore, useMenuStore } from '@/store';
  import { resolveMenuTitle } from '@/utils/MenuUtil';

  const { t, locale } = useI18n();
  const route = useRoute();
  const authStore = useAuthStore();
  const menuStore = useMenuStore();

  const langOptions = [
    { label: 'EN', value: 'en' },
    { label: '中', value: 'zh' },
  ];
  const langModel = computed({
    get: () => locale.value,
    set: (val: string) => {
      locale.value = val;
      localStorage.setItem('locale', val);
    },
  });

  onMounted(() => {
    menuStore.fetchTree();
  });

  const nameMap: Record<string, string> = {
    home: 'nav.home',
    driver: 'nav.driver',
    profile: 'nav.profile',
    device: 'nav.device',
    pointValue: 'nav.pointValue',
    driverDetail: 'nav.driverDetail',
    deviceDetail: 'nav.deviceDetail',
    deviceEdit: 'nav.deviceEdit',
    profileDetail: 'nav.profileDetail',
    profileEdit: 'nav.profileEdit',
    pointDetail: 'nav.pointDetail',
    pointEdit: 'nav.pointEdit',
    application: 'nav.application',
    settings: 'nav.settings',
    settingsUser: 'nav.settingsUser',
    settingsRole: 'nav.settingsRole',
    settingsResource: 'nav.settingsResource',
    settingsApi: 'nav.settingsApi',
    settingsMenu: 'nav.settingsMenu',
    settingsEvent: 'nav.settingsEvent',
    settingsDeviceEvent: 'nav.settingsDeviceEvent',
    settingsDriverEvent: 'nav.settingsDriverEvent',
    settingsAbout: 'nav.settingsAbout',
    settingsUserDetail: 'nav.settingsUserDetail',
    settingsRoleDetail: 'nav.settingsRoleDetail',
    settingsResourceDetail: 'nav.settingsResourceDetail',
    settingsApiDetail: 'nav.settingsApiDetail',
    settingsMenuDetail: 'nav.settingsMenuDetail',
  };

  // For a settings detail page we want the breadcrumb to read
  //   Home / Settings / <list> / <detail>
  // instead of stopping at Home / Settings. Map each detail route to the list
  // route it descends from.
  const settingsDetailParent: Record<string, { path: string; titleKey: string; code: string }> = {
    settingsUserDetail: { path: '/settings/user', titleKey: 'nav.settingsUser', code: 'settingsUser' },
    settingsRoleDetail: { path: '/settings/role', titleKey: 'nav.settingsRole', code: 'settingsRole' },
    settingsResourceDetail: { path: '/settings/resource', titleKey: 'nav.settingsResource', code: 'settingsResource' },
    settingsApiDetail: { path: '/settings/api', titleKey: 'nav.settingsApi', code: 'settingsApi' },
    settingsMenuDetail: { path: '/settings/menu', titleKey: 'nav.settingsMenu', code: 'settingsMenu' },
    settingsDeviceEvent: { path: '/settings/event', titleKey: 'nav.settingsEvent', code: 'settingsEvent' },
    settingsDriverEvent: { path: '/settings/event', titleKey: 'nav.settingsEvent', code: 'settingsEvent' },
  };

  // Static icon fallback for route names the backend menu tree does not yet
  // describe (detail pages, legacy routes). The top-level nav entries defer
  // to `menuStore.findByCode(...).menuExt.content.icon` first; this map only
  // kicks in for crumbs the backend has no row for.
  const FALLBACK_ICON: Record<string, string> = {
    home: 'HomeFilled',
    driver: 'Promotion',
    profile: 'List',
    device: 'Management',
    pointValue: 'TrendCharts',
    settings: 'Setting',
    settingsUser: 'User',
    settingsRole: 'UserFilled',
    settingsResource: 'Key',
    settingsApi: 'Link',
    settingsMenu: 'Menu',
    settingsEvent: 'Bell',
    settingsEventOverview: 'DataLine',
    settingsDeviceEvent: 'Management',
    settingsDriverEvent: 'Promotion',
    settingsAbout: 'InfoFilled',
    driverDetail: 'Promotion',
    deviceDetail: 'Management',
    deviceEdit: 'Management',
    profileDetail: 'List',
    profileEdit: 'List',
    pointDetail: 'TrendCharts',
    pointEdit: 'TrendCharts',
    settingsUserDetail: 'User',
    settingsRoleDetail: 'UserFilled',
    settingsResourceDetail: 'Key',
    settingsApiDetail: 'Link',
    settingsMenuDetail: 'Menu',
  };

  const iconForCode = (code: string): string | undefined => {
    const node = menuStore.findByCode(code);
    return node?.menuExt?.content?.icon || FALLBACK_ICON[code];
  };

  const breadcrumbItems = computed(() => {
    const items: { path: string; title: string; icon?: string }[] = [
      { path: '/home', title: t('nav.home'), icon: iconForCode('home') },
    ];
    const name = route.name as string;
    if (!name || name === 'home') return items;

    const title = nameMap[name] ? t(nameMap[name]) : name;
    if (name.startsWith('driver')) {
      items.push({ path: '/driver', title: t('nav.driver'), icon: iconForCode('driver') });
    } else if (name.startsWith('device')) {
      items.push({ path: '/device', title: t('nav.device'), icon: iconForCode('device') });
    } else if (name.startsWith('profile')) {
      items.push({ path: '/profile', title: t('nav.profile'), icon: iconForCode('profile') });
    } else if (name.startsWith('point')) {
      items.push({ path: '/point_value', title: t('nav.pointValue'), icon: iconForCode('pointValue') });
    } else if (name.startsWith('settings')) {
      items.push({ path: '/settings/user', title: t('nav.settings'), icon: iconForCode('settings') });
      const mid = settingsDetailParent[name];
      if (mid) {
        items.push({ path: mid.path, title: t(mid.titleKey), icon: iconForCode(mid.code) });
      }
    }
    if (!['home', 'driver', 'profile', 'device', 'pointValue', 'settings'].includes(name)) {
      const last = items[items.length - 1];
      if (!last || last.path !== route.path) {
        items.push({ path: route.path, title, icon: iconForCode(name) });
      }
    }
    return items;
  });

  // Top-level menus come from the backend (dc3_menu). Home is rendered separately
  // as the leftmost entry with its own fixed icon; Settings is reached from the
  // avatar dropdown, not the header bar.
  const topLevelMenus = computed(() => {
    return (menuStore.tree || [])
      .filter((n) => n.menuCode !== 'home' && n.menuCode !== 'settings')
      .slice()
      .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0));
  });

  const handleMenuEnter = (index: string) => {
    if (index.indexOf('/') === 0) {
      const split = index.split('/');
      if (split.length > 2) {
        return '/' + split[1];
      }
    }
    return index;
  };

  const handleCommand = async (command: string) => {
    if (command === 'settings') {
      await router.push({ name: 'settingsUser' });
    } else if (command === 'logout') {
      await authStore.logout();
      await router.push({ name: 'login' });
    } else if (command === 'help') {
      window.open('https://doc.dc3.site');
    }
  };
</script>

<style lang="scss" scoped>
  .container {
    color: #2c3e50;
    -moz-osx-font-smoothing: grayscale;
    -webkit-font-smoothing: antialiased;
    font-family: 'Avenir', Helvetica, Arial, sans-serif;

    .header {
      width: 100%;
      height: 60px;
      display: flex;
      border-bottom: 1px solid #dcdfe6;

      .header_item {
        height: 100%;
      }

      .header_logo {
        height: 60px;
        margin-left: 10px;
      }

      .header_menu {
        display: flex;
        justify-content: center;
        border-bottom: none !important;
      }

      .header_menu .el-menu-item {
        font-size: 15px;
      }

      .header_user {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        padding-right: 20px;

        .user_avatar {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;

          .el-avatar {
            background: #ffffff;
          }

          .user_name {
            color: #303133;
            font-size: 14px;
            font-weight: 500;
          }
        }
      }
    }

    .body {
      top: 60px;
      right: 0;
      left: 0;
      bottom: 0;
      min-width: 1280px;
      padding: 5px 0 5px 0;
      position: absolute;
      background: #f6f7f9;

      .breadcrumb {
        padding: 12px 20px;
        margin-bottom: 1px;
        background: #fff;
        border-radius: 4px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

        .breadcrumb__item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }

        .breadcrumb__icon {
          font-size: 14px;
        }
      }
    }
  }
</style>

<!--
  Element Plus teleports el-dropdown's popper to <body>, which puts it
  outside this component's scoped-CSS boundary. The language-switch row
  lives inside that popper, so its styles need to be non-scoped to
  actually land on the rendered DOM.
-->
<style lang="scss">
  .user_lang_row {
    list-style: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 6px 16px 8px;

    .user_lang_label {
      font-size: 13px;
      color: #606266;
    }
  }
</style>
