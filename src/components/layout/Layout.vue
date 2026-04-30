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
          <template v-for="menusItem in menus">
            <el-sub-menu v-if="menusItem.children" :key="`${menusItem.path}-submenu`" :index="menusItem.path">
              <template #title>{{ menusItem.meta?.title }}</template>
              <el-menu-item v-for="item in menusItem.children" :key="item.path" :index="item.path">
                {{ item.meta?.title }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :key="`${menusItem.path}-menuitem`" :index="menusItem.path">
              <el-icon>
                <component :is="menusItem.meta?.icon"></component>
              </el-icon>
              {{ menusItem.meta?.title }}
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
              <el-dropdown-item :class="{ 'is-active': locale === 'en' }" command="lang-en">
                <span class="lang-flag">🇺🇸</span>
                <span class="lang-text">English</span>
                <el-icon v-if="locale === 'en'" class="lang-check"><Check /></el-icon>
              </el-dropdown-item>
              <el-dropdown-item :class="{ 'is-active': locale === 'zh' }" command="lang-zh">
                <span class="lang-flag">🇨🇳</span>
                <span class="lang-text">中文</span>
                <el-icon v-if="locale === 'zh'" class="lang-check"><Check /></el-icon>
              </el-dropdown-item>
              <el-dropdown-item divided command="settings">{{ t('layout.settings') }}</el-dropdown-item>
              <el-dropdown-item command="help">{{ t('layout.about') }}</el-dropdown-item>
              <el-dropdown-item command="logout">{{ t('layout.logout') }}</el-dropdown-item>
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
              {{ item.title }}
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
  import menu from '@/config/router/views';
  import { Check, HomeFilled } from '@element-plus/icons-vue';
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { useAuthStore } from '@/store';
  import type { RouteRecordRaw } from 'vue-router';

  const { t, locale } = useI18n();
  const route = useRoute();
  const authStore = useAuthStore();

  const nameMap: Record<string, string> = {
    home: 'nav.home',
    driver: 'nav.driver',
    profile: 'nav.profile',
    device: 'nav.device',
    pointValue: 'nav.data',
    driverDetail: 'nav.driverDetail',
    deviceDetail: 'nav.deviceDetail',
    deviceEdit: 'nav.deviceEdit',
    profileDetail: 'nav.profileDetail',
    profileEdit: 'nav.profileEdit',
    pointDetail: 'nav.pointDetail',
    pointEdit: 'nav.pointEdit',
    dashboard: 'nav.dashboard',
    application: 'nav.application',
    settings: 'nav.settings',
    settingsUser: 'nav.settingsUser',
    settingsRole: 'nav.settingsRole',
    settingsResource: 'nav.settingsResource',
  };

  const breadcrumbItems = computed(() => {
    const items: { path: string; title: string }[] = [{ path: '/home', title: t('nav.home') }];
    const name = route.name as string;
    if (!name || name === 'home') return items;

    const title = nameMap[name] ? t(nameMap[name]) : name;
    if (name.startsWith('driver')) {
      items.push({ path: '/driver', title: t('nav.driver') });
    } else if (name.startsWith('device')) {
      items.push({ path: '/device', title: t('nav.device') });
    } else if (name.startsWith('profile')) {
      items.push({ path: '/profile', title: t('nav.profile') });
    } else if (name.startsWith('point')) {
      items.push({ path: '/point_value', title: t('nav.data') });
    } else if (name.startsWith('settings')) {
      items.push({ path: '/settings/user', title: t('nav.settings') });
    }
    if (!['home', 'driver', 'profile', 'device', 'pointValue', 'settings'].includes(name)) {
      items.push({ path: route.path, title });
    }
    return items;
  });

  const menus = computed(() => {
    const children: RouteRecordRaw[] = menu?.children || [];
    return children.filter((view) => view.path !== '/home');
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
    if (command.startsWith('lang-')) {
      const lang = command.slice(5);
      locale.value = lang;
      localStorage.setItem('locale', lang);
    } else if (command === 'settings') {
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
  @use '@/components/layout/style.scss';
</style>

<style lang="scss">
  .el-dropdown-menu {
    .lang-flag {
      margin-right: 8px;
      font-size: 16px;
    }

    .lang-text {
      margin-right: 16px;
    }

    .lang-check {
      margin-left: auto;
      color: var(--el-color-primary);
    }
  }
</style>
