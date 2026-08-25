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
      <div class="header_item header_brand">
        <!-- Thumb terminals get a hamburger instead of the horizontal menu
             strip (A3: drawer navigation, docs/design/frontend-three-terminal-ux.md). -->
        <el-button
          :aria-label="t('layout.navigation')"
          :icon="Menu"
          circle
          class="header_menu_toggle"
          text
          @click="navDrawerVisible = true"
        />
        <div class="header_brand_glass">
          <img :src="assetUrl('images/logo/logo.svg')" class="header_logo"/>
          <span class="header_title">IoT DC3</span>
        </div>
      </div>
      <div v-if="!isMobile" class="header_item header_menu_wrap">
        <nav-menu mode="horizontal"/>
      </div>
      <div class="header_item header_user">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user_avatar">
            <el-avatar>
              <img :src="assetUrl('images/common/avatar.png')"/>
            </el-avatar>
            <span class="user_name">{{ currentLogin }}</span>
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
      </div>
    </div>
    <!-- Mobile navigation drawer: the same NavMenu component, vertical mode. -->
    <el-drawer
      v-model="navDrawerVisible"
      :size="280"
      :title="t('layout.navigation')"
      :with-header="true"
      direction="ltr"
      class="nav-drawer"
    >
      <nav-menu mode="vertical"/>
    </el-drawer>
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
      <!-- Backtop keeps clear of the assistant FAB and screen edges on
           thumb terminals (A3). -->
      <el-backtop :bottom="isMobile ? 88 : 40" :right="isMobile ? 16 : 40" target=".body-main .el-scrollbar__wrap"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import NavMenu from '@/components/layout/NavMenu.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import router from '@/config/router';
import {Menu, QuestionFilled, Setting, SwitchButton} from '@element-plus/icons-vue';
import {computed, onMounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute} from 'vue-router';
import {
  getSettingsTitleKey,
  SETTINGS_BREADCRUMB_PARENTS,
  SETTINGS_FALLBACK_ICON,
} from '@/config/settingsNav';
import {useAgenticStore, useAuthStore, useMenuStore} from '@/store';
import type {MenuNode} from '@/store/modules/menu';
import {assetUrl} from '@/utils/assetUrl';

const {t, locale} = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const agenticStore = useAgenticStore();
const {isMobile} = useBreakpoint();
const navDrawerVisible = ref(false);
const currentLogin = computed(() => String(authStore.getName || authStore.name || 'dc3'));

// Close the mobile navigation drawer once navigation actually happens.
watch(() => route.fullPath, () => {
  navDrawerVisible.value = false;
});

// The AI assistant is shown in every build; in mock builds the fetch
// interceptor (src/mock/fetch.ts) answers its chat completions.

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
  const leafCode = name;
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
  color: var(--dc3-text-primary);
  -moz-osx-font-smoothing: grayscale;
  -webkit-font-smoothing: antialiased;
  font-family: 'Avenir', Helvetica, Arial, sans-serif;

  .header {
    width: 100%;
    height: var(--dc3-header-height);
    display: flex;
    border-bottom: 1px solid var(--dc3-border-base);

    // 1:4:1 flex split mirrors the legacy el-col 4/16/4 grid so the
    // desktop layout is unchanged; the menu wrap disappears on mobile
    // and the two side cells absorb the space.
    .header_item {
      height: 100%;
    }

    .header_brand {
      flex: 1 1 0;
      display: flex;
      align-items: center;
      gap: var(--dc3-space-2);
      padding-left: 10px;
    }

    .header_menu_wrap {
      flex: 4 1 0;
      display: flex;
      justify-content: center;
    }

    .header_menu_toggle {
      display: none;
    }

    .header_brand_glass {
      position: relative;
      isolation: isolate;
      display: inline-flex;
      align-items: center;
      box-sizing: border-box;
      width: fit-content;
      height: 40px;
      padding: 5px 13px 5px 6px;
      overflow: hidden;
      border: 1px solid rgba(148, 216, 246, 0.34);
      border-radius: 20px;
      background: radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.82), transparent 38%),
      linear-gradient(135deg, rgba(255, 255, 255, 0.54), rgba(115, 205, 241, 0.13) 52%, rgba(75, 88, 210, 0.08));
      box-shadow: 0 10px 28px rgba(12, 89, 153, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.86),
      inset 0 -8px 18px rgba(55, 131, 203, 0.05);
      backdrop-filter: blur(18px) saturate(1.45);
      -webkit-backdrop-filter: blur(18px) saturate(1.45);
      transition: transform 260ms ease, border-color 260ms ease, box-shadow 260ms ease;

      &::after {
        position: absolute;
        z-index: -1;
        top: 1px;
        right: 12px;
        left: 12px;
        height: 42%;
        border-radius: 999px;
        background: linear-gradient(180deg, rgba(255, 255, 255, 0.48), transparent);
        opacity: 0.72;
        pointer-events: none;
        content: '';
      }

      &:hover {
        border-color: rgba(61, 172, 224, 0.46);
        box-shadow: 0 14px 34px rgba(12, 89, 153, 0.15),
        inset 0 1px 0 rgba(255, 255, 255, 0.92),
        inset 0 -8px 18px rgba(55, 131, 203, 0.07);
        transform: translateY(-1px);
      }
    }

    .header_logo {
      flex: 0 0 auto;
      width: 30px;
      height: 30px;
      filter: drop-shadow(0 4px 7px rgba(13, 82, 157, 0.2));
    }

    .header_title {
      margin-left: 8px;
      background: linear-gradient(112deg, #07549a, #149ed7 46%, #4f52bf);
      background-clip: text;
      -webkit-background-clip: text;
      color: transparent;
      font-size: 20px;
      font-weight: 740;
      letter-spacing: -0.01em;
      line-height: 1;
      white-space: nowrap;
      -webkit-text-fill-color: transparent;
    }

    .header_user {
      flex: 1 1 0;
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

    // Thumb-terminal header (A3): hamburger replaces the menu strip,
    // brand and user cells shrink to icon-only.
    @media (max-width: $breakpoint-xs-max) {
      .header_brand {
        padding-left: var(--dc3-space-2);
      }

      .header_menu_toggle {
        display: inline-flex;
      }

      .header_user {
        padding-right: var(--dc3-space-3);

        .user_name {
          display: none;
        }
      }
    }
  }

  // Mobile navigation drawer content.
  .nav-drawer {
    :deep(.el-drawer__body) {
      padding: var(--dc3-space-2);
    }

    :deep(.nav-menu) {
      border-right: none;
    }
  }

  .body {
    top: var(--dc3-header-height);
    right: 0;
    left: 0;
    bottom: 0;
    display: flex;
    padding: 1px 0 5px 0;
    overflow: hidden;
    position: absolute;
    background: var(--dc3-bg-body);

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
      padding: var(--dc3-space-3) var(--dc3-space-5);
      margin-bottom: 1px;
      background: var(--dc3-bg-elevated);
      border-radius: var(--dc3-radius-sm);
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
