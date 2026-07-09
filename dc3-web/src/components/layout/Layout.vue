<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <div class="container">
    <div class="header">
      <el-col :span="4" class="header_item">
        <img class="header_logo" src="/images/logo/logo.svg"/>
      </el-col>
      <el-col :span="16" class="header_item">
        <el-menu :default-active="handleMenuEnter($route.path)" :router="true" class="header_menu" mode="horizontal">
          <el-menu-item index="/home">
            <el-icon>
              <HomeFilled/>
            </el-icon>
            {{ t('nav.home') }}
          </el-menu-item>
          <template v-for="node in topLevelMenus" :key="`menu-${node.id}`">
            <el-sub-menu v-if="node.children && node.children.length > 0" :index="`node-${node.id}`">
              <template #title>
                <el-icon v-if="node.menuExt?.content?.icon">
                  <component :is="node.menuExt.content.icon"/>
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
                <component :is="node.menuExt.content.icon"/>
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
              <img src="/images/common/avatar.png"/>
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
                <el-segmented v-model="langModel" :options="langOptions" class="user_lang_seg" size="small"/>
              </li>
              <el-dropdown-item v-if="settingsEntryName" :icon="Setting" command="settings" divided>
                {{ t('layout.settings') }}
              </el-dropdown-item>
              <el-dropdown-item :icon="QuestionFilled" command="help">{{ t('layout.about') }}</el-dropdown-item>
              <el-dropdown-item :icon="SwitchButton" command="logout">{{ t('layout.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </div>
    <div class="body">
      <div class="body-main">
        <div v-if="breadcrumbItems.length > 1" class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbItems" :key="`${item.path}-${index}`" :to="item.path">
              <span class="breadcrumb__item">
                <el-icon v-if="item.icon" class="breadcrumb__icon">
                  <component :is="item.icon"/>
                </el-icon>
                <span>{{ item.title }}</span>
              </span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-scrollbar v-if="!isFixedLayout" ref="scrollbarRef">
          <router-view/>
        </el-scrollbar>
        <div v-else class="fixed-viewport">
          <router-view/>
        </div>
      </div>
      <agentic-assistant/>
      <el-backtop :bottom="40" :right="40" target=".body-main .el-scrollbar__wrap"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import router from '@/config/router';
import {HomeFilled, QuestionFilled, Setting, SwitchButton} from '@element-plus/icons-vue';
import {computed, onMounted} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute} from 'vue-router';
import {
  getSettingsLeafIconCode,
  getSettingsTitleKey,
  SETTINGS_BREADCRUMB_PARENTS,
  SETTINGS_FALLBACK_ICON,
} from '@/config/settingsNav';
import {useAgenticStore, useAuthStore, useMenuStore} from '@/store';
import type {MenuNode} from '@/store/modules/menu';
import {resolveMenuTitle} from '@/utils/menuUtil';

const {t, locale} = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const agenticStore = useAgenticStore();

const langOptions = [
  {label: 'EN', value: 'en'},
  {label: '中', value: 'zh'},
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
  settings: 'nav.settings',
  settingsIdentity: 'nav.settingsIdentity',
  settingsAccess: 'nav.settingsAccess',
  settingsEventCommand: 'nav.settingsEventCommand',
  settingsAudit: 'nav.settingsAudit',
  settingsIntegration: 'nav.settingsIntegration',
  settingsSystem: 'nav.settingsSystem',
  settingsUser: 'nav.settingsUser',
  settingsPrincipal: 'nav.settingsPrincipal',
  settingsTenantMembership: 'nav.settingsTenantMembership',
  settingsLocalCredential: 'nav.settingsLocalCredential',
  settingsIdentityAudit: 'nav.settingsIdentityAudit',
  settingsRole: 'nav.settingsRole',
  settingsRolePrincipalBind: 'nav.settingsRolePrincipalBind',
  settingsResource: 'nav.settingsResource',
  settingsApi: 'nav.settingsApi',
  settingsMenu: 'nav.settingsMenu',
  settingsGroup: 'nav.settingsGroup',
  settingsLabel: 'nav.settingsLabel',
  settingsAlarm: 'nav.settingsAlarm',
  settingsAlarmRule: 'nav.settingsAlarmRule',
  settingsAlarmNotify: 'nav.settingsAlarmNotify',
  settingsAlarmMessage: 'nav.settingsAlarmMessage',
  settingsAlarmChannel: 'nav.settingsAlarmChannel',
  settingsAlarmBind: 'nav.settingsAlarmBind',
  settingsAlarmState: 'nav.settingsAlarmState',
  settingsAlarmHistory: 'nav.settingsAlarmHistory',
  settingsModel: 'nav.settingsModel',
  settingsModelConfig: 'nav.settingsModelConfig',
  settingsModelProvider: 'nav.settingsModelProvider',
  settingsEvent: 'nav.settingsEvent',
  settingsAlarmOverview: 'nav.settingsAlarmOverview',
  settingsDeviceAlarm: 'nav.settingsDeviceAlarm',
  settingsDriverAlarm: 'nav.settingsDriverAlarm',
  settingsPointAlarm: 'nav.settingsPointAlarm',
  settingsAbout: 'nav.settingsAbout',
  settingsUserDetail: 'nav.settingsUserDetail',
  settingsRoleDetail: 'nav.settingsRoleDetail',
  settingsResourceDetail: 'nav.settingsResourceDetail',
  settingsApiDetail: 'nav.settingsApiDetail',
  settingsMenuDetail: 'nav.settingsMenuDetail',
  settingsGroupDetail: 'nav.settingsGroupDetail',
  settingsLabelDetail: 'nav.settingsLabelDetail',
  settingsAlarmRuleDetail: 'nav.settingsAlarmRuleDetail',
  settingsAlarmNotifyDetail: 'nav.settingsAlarmNotifyDetail',
  settingsAlarmMessageDetail: 'nav.settingsAlarmMessageDetail',
  settingsAlarmChannelDetail: 'nav.settingsAlarmChannelDetail',
  settingsAlarmBindDetail: 'nav.settingsAlarmBindDetail',
  settingsAlarmStateDetail: 'nav.settingsAlarmStateDetail',
  settingsAlarmHistoryDetail: 'nav.settingsAlarmHistoryDetail',
  settingsModelConfigDetail: 'nav.settingsModelConfigDetail',
  settingsModelProviderDetail: 'nav.settingsModelProviderDetail',
  settingsCommand: 'nav.settingsCommand',
  settingsCommandHistory: 'nav.settingsCommandHistory',
  settingsEventHistory: 'nav.settingsEventHistory',
  settingsServiceAccount: 'nav.settingsServiceAccount',
  settingsMcpServer: 'nav.settingsMcpServer',
  settingsMcpConnection: 'nav.settingsMcpConnection',
  settingsMcpClient: 'nav.settingsMcpClient',
  settingsMcpTool: 'nav.settingsMcpTool',
  settingsMcpAudit: 'nav.settingsMcpAudit',
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
  settingsIdentity: 'User',
  settingsAccess: 'Stamp',
  settingsEventCommand: 'Operation',
  settingsAudit: 'Files',
  settingsIntegration: 'Share',
  settingsSystem: 'Tools',
  settingsUser: 'User',
  settingsPrincipal: 'Avatar',
  settingsTenantMembership: 'OfficeBuilding',
  settingsLocalCredential: 'Lock',
  settingsIdentityAudit: 'DocumentChecked',
  settingsRole: 'UserFilled',
  settingsRolePrincipalBind: 'Link',
  settingsResource: 'Key',
  settingsApi: 'Link',
  settingsMenu: 'Menu',
  settingsGroup: 'Grid',
  settingsLabel: 'CollectionTag',
  settingsAlarm: 'AlarmClock',
  settingsAlarmRule: 'SetUp',
  settingsAlarmNotify: 'Bell',
  settingsAlarmMessage: 'Message',
  settingsAlarmChannel: 'Connection',
  settingsAlarmBind: 'Link',
  settingsAlarmState: 'Monitor',
  settingsAlarmHistory: 'DocumentChecked',
  settingsModel: 'Cpu',
  settingsModelConfig: 'ChatDotRound',
  settingsModelProvider: 'ChatLineSquare',
  settingsEvent: 'Bell',
  settingsAlarmOverview: 'DataLine',
  settingsDeviceAlarm: 'Management',
  settingsDriverAlarm: 'Promotion',
  settingsPointAlarm: 'TrendCharts',
  settingsAbout: 'InfoFilled',
  driverDetail: 'Promotion',
  deviceDetail: 'Management',
  deviceEdit: 'Management',
  profileDetail: 'List',
  profileEdit: 'List',
  settingsUserDetail: 'User',
  settingsRoleDetail: 'UserFilled',
  settingsResourceDetail: 'Key',
  settingsApiDetail: 'Link',
  settingsMenuDetail: 'Menu',
  settingsGroupDetail: 'Grid',
  settingsLabelDetail: 'CollectionTag',
  settingsAlarmRuleDetail: 'SetUp',
  settingsAlarmNotifyDetail: 'Bell',
  settingsAlarmMessageDetail: 'Message',
  settingsAlarmChannelDetail: 'Connection',
  settingsAlarmBindDetail: 'Link',
  settingsAlarmStateDetail: 'Monitor',
  settingsAlarmHistoryDetail: 'DocumentChecked',
  settingsModelConfigDetail: 'ChatDotRound',
  settingsModelProviderDetail: 'ChatLineSquare',
  settingsCommand: 'Operation',
  settingsCommandHistory: 'Document',
  settingsEventHistory: 'Document',
  settingsServiceAccount: 'Key',
  settingsMcpServer: 'Connection',
  settingsMcpConnection: 'Link',
  settingsMcpClient: 'Ticket',
  settingsMcpTool: 'Tools',
  settingsMcpAudit: 'Document',
};

const iconForCode = (code: string): string | undefined => {
  const node = menuStore.findByCode(code);
  return node?.menuExt?.content?.icon || SETTINGS_FALLBACK_ICON[code] || FALLBACK_ICON[code];
};

const isFixedLayout = computed(() => {
  const name = route.name as string;
  return !!name && name.startsWith('settings');
});

const breadcrumbItems = computed(() => {
  const items: { path: string; title: string; icon?: string }[] = [
    {path: '/home', title: t('nav.home'), icon: iconForCode('home')},
  ];
  const name = route.name as string;
  if (!name || name === 'home') return items;

  const titleKey = name.startsWith('settings') ? getSettingsTitleKey(name) : nameMap[name];
  const title = titleKey ? t(titleKey) : name;
  const leafCode = getSettingsLeafIconCode(name);
  if (name.startsWith('driver')) {
    items.push({path: '/driver', title: t('nav.driver'), icon: iconForCode('driver')});
  } else if (name.startsWith('device')) {
    items.push({path: '/device', title: t('nav.device'), icon: iconForCode('device')});
  } else if (name.startsWith('profile')) {
    items.push({path: '/profile', title: t('nav.profile'), icon: iconForCode('profile')});
  } else if (name.startsWith('point')) {
    items.push({path: '/profile', title: t('nav.profile'), icon: iconForCode('profile')});
  } else if (name.startsWith('settings')) {
    items.push({path: '/settings', title: t('nav.settings'), icon: iconForCode('settings')});
    (SETTINGS_BREADCRUMB_PARENTS[name] || []).forEach((mid) => {
      items.push({path: mid.path, title: t(mid.titleKey), icon: iconForCode(mid.code)});
    });
  }
  if (!['home', 'driver', 'profile', 'device', 'pointValue', 'settings'].includes(name)) {
    const last = items[items.length - 1];
    if (!last || last.path !== route.path || last.title !== title) {
      items.push({path: route.path, title, icon: iconForCode(leafCode)});
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

const firstRouteableMenuName = (node?: MenuNode): string | undefined => {
  if (!node) return undefined;
  if (node.menuExt?.content?.url) return node.menuCode;
  for (const child of node.children || []) {
    const hit = firstRouteableMenuName(child);
    if (hit) return hit;
  }
  return undefined;
};

const settingsEntryName = computed(() => firstRouteableMenuName(menuStore.findByCode('settings')));

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
    if (settingsEntryName.value) {
      await router.push({name: settingsEntryName.value});
    }
  } else if (command === 'logout') {
    try {
      await authStore.logout();
    } catch {
      // proceed with redirect even if server cancel fails
    }
    menuStore.reset();
    agenticStore.reset();
    await router.push({name: 'login'});
  } else if (command === 'help') {
    const helpWindow = window.open('https://doc.dc3.site', '_blank', 'noopener,noreferrer');
    if (helpWindow) helpWindow.opener = null;
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
    display: flex;
    min-width: 1280px;
    padding: 1px 0 5px 0;
    overflow: hidden;
    position: absolute;
    background: #f6f7f9;

    .body-main {
      display: flex;
      flex-direction: column;
      flex: 1 1 auto;
      min-width: 0;
      height: 100%;

      > .el-scrollbar {
        flex: 1;
        min-height: 0;
      }

      .fixed-viewport {
        flex: 1;
        min-height: 0;
        overflow: hidden;
      }
    }

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
  justify-content: center;
  padding: 6px 16px 8px;

  .user_lang_seg {
    width: 100%;
  }
}
</style>
