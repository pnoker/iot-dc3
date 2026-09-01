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
    <header class="header">
      <div class="header_brand_glass">
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
        <brand-lockup :compact="isMobile" />
      </div>
      <div class="header_actions_glass">
        <div v-if="!isMobile" class="header_menu_wrap">
          <nav-menu :compact="isTablet" mode="horizontal" />
        </div>
        <span v-if="!isMobile" class="header_actions_divider" aria-hidden="true" />
        <div class="header_utilities">
          <app-preferences compact />
          <el-tooltip v-if="settingsEntryName" :content="t('layout.settings')" placement="bottom">
            <el-button
              :aria-label="t('layout.settings')"
              :icon="Setting"
              circle
              class="header_settings_button"
              text
              @click="handleCommand('settings')"
            />
          </el-tooltip>
        </div>
        <span class="header_actions_divider" aria-hidden="true" />
        <div class="header_user">
          <el-dropdown
            popper-class="user-dropdown-popper"
            trigger="click"
            :popper-options="{
              modifiers: [{ name: 'preventOverflow', options: { padding: 8 } }],
            }"
            @command="handleCommand"
          >
            <button :aria-label="t('layout.account')" class="user_trigger" type="button">
              <img :src="assetUrl('images/common/avatar.png')" alt="" class="user_avatar" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <li class="user_dropdown_identity" role="none">
                  <span class="user_dropdown_name">{{ currentLogin }}</span>
                </li>
                <el-dropdown-item :icon="QuestionFilled" command="help">{{
                  t("layout.about")
                }}</el-dropdown-item>
                <el-dropdown-item :icon="SwitchButton" command="logout" divided>{{
                  t("layout.logout")
                }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    <!-- Mobile navigation drawer: the same NavMenu component, vertical mode. -->
    <el-drawer
      v-model="navDrawerVisible"
      :size="280"
      :title="t('layout.navigation')"
      :with-header="true"
      class="nav-drawer"
      direction="ltr"
    >
      <nav-menu mode="vertical" />
    </el-drawer>
    <div class="body">
      <div class="body-main">
        <div v-if="breadcrumbItems.length > 1" class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbItems"
              :key="`${item.path}-${index}`"
              :to="item.path"
            >
              <span class="breadcrumb__item">
                <el-icon v-if="item.icon" class="breadcrumb__icon">
                  <component :is="item.icon" />
                </el-icon>
                <span>{{ item.title }}</span>
              </span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-scrollbar v-if="!isFixedLayout" ref="scrollbarRef">
          <router-view />
        </el-scrollbar>
        <div v-else class="fixed-viewport">
          <router-view />
        </div>
      </div>
      <agentic-assistant />
      <!-- Backtop keeps clear of the assistant FAB and screen edges on
           thumb terminals (A3). -->
      <el-backtop
        :bottom="isMobile ? 88 : 40"
        :right="isMobile ? 16 : 40"
        target=".body-main .el-scrollbar__wrap"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import AgenticAssistant from "@/components/agentic/AgenticAssistant.vue";
import BrandLockup from "@/components/brand/BrandLockup.vue";
import NavMenu from "@/components/layout/NavMenu.vue";
import { useBreakpoint } from "@/composables/useBreakpoint";
import router from "@/config/router";
import {
  Menu,
  QuestionFilled,
  Setting,
  SwitchButton,
} from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import {
  getSettingsTitleKey,
  SETTINGS_BREADCRUMB_PARENTS,
  SETTINGS_FALLBACK_ICON,
} from "@/config/settingsNav";
import { useAgenticStore, useAuthStore, useMenuStore } from "@/store";
import type { MenuNode } from "@/store/modules/menu";
import { assetUrl } from "@/utils/assetUrl";

import AppPreferences from "@/components/layout/AppPreferences.vue";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const agenticStore = useAgenticStore();
const { isMobile, isTablet } = useBreakpoint();
const navDrawerVisible = ref(false);
const currentLogin = computed(() =>
  String(authStore.getName || authStore.name || "dc3"),
);

// Close the mobile navigation drawer once navigation actually happens.
watch(
  () => route.fullPath,
  () => {
    navDrawerVisible.value = false;
  },
);

// The AI assistant is shown in every build; in mock builds the fetch
// interceptor (src/mock/fetch.ts) answers its chat completions.

onMounted(() => {
  menuStore.fetchTree();
});

const nameMap: Record<string, string> = {
  home: "nav.home",
  driver: "nav.driver",
  profile: "nav.profile",
  device: "nav.device",
  pointValue: "nav.pointValue",
  driverDetail: "nav.driverDetail",
  deviceDetail: "nav.deviceDetail",
  deviceEdit: "nav.deviceEdit",
  profileDetail: "nav.profileDetail",
  profileEdit: "nav.profileEdit",
  settings: "nav.settings",
  settingsIdentity: "nav.settingsIdentity",
  settingsAccess: "nav.settingsAccess",
  settingsEventCommand: "nav.settingsEventCommand",
  settingsAudit: "nav.settingsAudit",
  settingsIntegration: "nav.settingsIntegration",
  settingsSystem: "nav.settingsSystem",
  settingsUser: "nav.settingsUser",
  settingsPrincipal: "nav.settingsPrincipal",
  settingsTenantMembership: "nav.settingsTenantMembership",
  settingsLocalCredential: "nav.settingsLocalCredential",
  settingsIdentityAudit: "nav.settingsIdentityAudit",
  settingsRole: "nav.settingsRole",
  settingsRolePrincipalBind: "nav.settingsRolePrincipalBind",
  settingsResource: "nav.settingsResource",
  settingsApi: "nav.settingsApi",
  settingsMenu: "nav.settingsMenu",
  settingsGroup: "nav.settingsGroup",
  settingsLabel: "nav.settingsLabel",
  settingsAlarm: "nav.settingsAlarm",
  settingsAlarmRule: "nav.settingsAlarmRule",
  settingsAlarmNotify: "nav.settingsAlarmNotify",
  settingsAlarmMessage: "nav.settingsAlarmMessage",
  settingsAlarmChannel: "nav.settingsAlarmChannel",
  settingsAlarmBind: "nav.settingsAlarmBind",
  settingsAlarmState: "nav.settingsAlarmState",
  settingsAlarmHistory: "nav.settingsAlarmHistory",
  settingsModel: "nav.settingsModel",
  settingsModelConfig: "nav.settingsModelConfig",
  settingsModelProvider: "nav.settingsModelProvider",
  settingsEvent: "nav.settingsEvent",
  settingsAlarmOverview: "nav.settingsAlarmOverview",
  settingsDeviceAlarm: "nav.settingsDeviceAlarm",
  settingsDriverAlarm: "nav.settingsDriverAlarm",
  settingsPointAlarm: "nav.settingsPointAlarm",
  settingsAbout: "nav.settingsAbout",
  settingsUserDetail: "nav.settingsUserDetail",
  settingsRoleDetail: "nav.settingsRoleDetail",
  settingsResourceDetail: "nav.settingsResourceDetail",
  settingsApiDetail: "nav.settingsApiDetail",
  settingsMenuDetail: "nav.settingsMenuDetail",
  settingsGroupDetail: "nav.settingsGroupDetail",
  settingsLabelDetail: "nav.settingsLabelDetail",
  settingsAlarmRuleDetail: "nav.settingsAlarmRuleDetail",
  settingsAlarmNotifyDetail: "nav.settingsAlarmNotifyDetail",
  settingsAlarmMessageDetail: "nav.settingsAlarmMessageDetail",
  settingsAlarmChannelDetail: "nav.settingsAlarmChannelDetail",
  settingsAlarmBindDetail: "nav.settingsAlarmBindDetail",
  settingsAlarmStateDetail: "nav.settingsAlarmStateDetail",
  settingsAlarmHistoryDetail: "nav.settingsAlarmHistoryDetail",
  settingsModelConfigDetail: "nav.settingsModelConfigDetail",
  settingsModelProviderDetail: "nav.settingsModelProviderDetail",
  settingsCommand: "nav.settingsCommand",
  settingsCommandHistory: "nav.settingsCommandHistory",
  settingsEventHistory: "nav.settingsEventHistory",
  settingsServiceAccount: "nav.settingsServiceAccount",
  settingsMcpServer: "nav.settingsMcpServer",
  settingsMcpConnection: "nav.settingsMcpConnection",
  settingsMcpClient: "nav.settingsMcpClient",
  settingsMcpTool: "nav.settingsMcpTool",
  settingsMcpAudit: "nav.settingsMcpAudit",
};

// Static icon fallback for route names the backend menu tree does not yet
// describe (detail pages, legacy routes). The top-level nav entries defer
// to `menuStore.findByCode(...).menuExt.content.icon` first; this map only
// kicks in for crumbs the backend has no row for.
const FALLBACK_ICON: Record<string, string> = {
  home: "HomeFilled",
  driver: "Promotion",
  profile: "List",
  device: "Management",
  pointValue: "TrendCharts",
  settings: "Setting",
  settingsIdentity: "User",
  settingsAccess: "Stamp",
  settingsEventCommand: "Operation",
  settingsAudit: "Files",
  settingsIntegration: "Share",
  settingsSystem: "Tools",
  settingsUser: "User",
  settingsPrincipal: "Avatar",
  settingsTenantMembership: "OfficeBuilding",
  settingsLocalCredential: "Lock",
  settingsIdentityAudit: "DocumentChecked",
  settingsRole: "UserFilled",
  settingsRolePrincipalBind: "Link",
  settingsResource: "Key",
  settingsApi: "Link",
  settingsMenu: "Menu",
  settingsGroup: "Grid",
  settingsLabel: "CollectionTag",
  settingsAlarm: "AlarmClock",
  settingsAlarmRule: "SetUp",
  settingsAlarmNotify: "Bell",
  settingsAlarmMessage: "Message",
  settingsAlarmChannel: "Connection",
  settingsAlarmBind: "Link",
  settingsAlarmState: "Monitor",
  settingsAlarmHistory: "DocumentChecked",
  settingsModel: "Cpu",
  settingsModelConfig: "ChatDotRound",
  settingsModelProvider: "ChatLineSquare",
  settingsEvent: "Bell",
  settingsAlarmOverview: "DataLine",
  settingsDeviceAlarm: "Management",
  settingsDriverAlarm: "Promotion",
  settingsPointAlarm: "TrendCharts",
  settingsAbout: "InfoFilled",
  driverDetail: "Promotion",
  deviceDetail: "Management",
  deviceEdit: "Management",
  profileDetail: "List",
  profileEdit: "List",
  settingsUserDetail: "User",
  settingsRoleDetail: "UserFilled",
  settingsResourceDetail: "Key",
  settingsApiDetail: "Link",
  settingsMenuDetail: "Menu",
  settingsGroupDetail: "Grid",
  settingsLabelDetail: "CollectionTag",
  settingsAlarmRuleDetail: "SetUp",
  settingsAlarmNotifyDetail: "Bell",
  settingsAlarmMessageDetail: "Message",
  settingsAlarmChannelDetail: "Connection",
  settingsAlarmBindDetail: "Link",
  settingsAlarmStateDetail: "Monitor",
  settingsAlarmHistoryDetail: "DocumentChecked",
  settingsModelConfigDetail: "ChatDotRound",
  settingsModelProviderDetail: "ChatLineSquare",
  settingsCommand: "Operation",
  settingsCommandHistory: "Document",
  settingsEventHistory: "Document",
  settingsServiceAccount: "Key",
  settingsMcpServer: "Connection",
  settingsMcpConnection: "Link",
  settingsMcpClient: "Ticket",
  settingsMcpTool: "Tools",
  settingsMcpAudit: "Document",
};

const iconForCode = (code: string): string | undefined => {
  const node = menuStore.findByCode(code);
  return (
    node?.menuExt?.content?.icon ||
    SETTINGS_FALLBACK_ICON[code] ||
    FALLBACK_ICON[code]
  );
};

const isFixedLayout = computed(() => {
  const name = route.name as string;
  return !!name && name.startsWith("settings");
});

const breadcrumbItems = computed(() => {
  const items: { path: string; title: string; icon?: string }[] = [
    { path: "/home", title: t("nav.home"), icon: iconForCode("home") },
  ];
  const name = route.name as string;
  if (!name || name === "home") return items;

  const titleKey = name.startsWith("settings")
    ? getSettingsTitleKey(name)
    : nameMap[name];
  const title = titleKey ? t(titleKey) : name;
  const leafCode = name;
  if (name.startsWith("driver")) {
    items.push({
      path: "/driver",
      title: t("nav.driver"),
      icon: iconForCode("driver"),
    });
  } else if (name.startsWith("device")) {
    items.push({
      path: "/device",
      title: t("nav.device"),
      icon: iconForCode("device"),
    });
  } else if (name.startsWith("profile")) {
    items.push({
      path: "/profile",
      title: t("nav.profile"),
      icon: iconForCode("profile"),
    });
  } else if (name.startsWith("point")) {
    items.push({
      path: "/profile",
      title: t("nav.profile"),
      icon: iconForCode("profile"),
    });
  } else if (name.startsWith("settings")) {
    items.push({
      path: "/settings",
      title: t("nav.settings"),
      icon: iconForCode("settings"),
    });
    (SETTINGS_BREADCRUMB_PARENTS[name] || []).forEach((mid) => {
      items.push({
        path: mid.path,
        title: t(mid.titleKey),
        icon: iconForCode(mid.code),
      });
    });
  }
  if (
    !["home", "driver", "profile", "device", "pointValue", "settings"].includes(
      name,
    )
  ) {
    const last = items[items.length - 1];
    if (!last || last.path !== route.path || last.title !== title) {
      items.push({ path: route.path, title, icon: iconForCode(leafCode) });
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

const settingsEntryName = computed(() =>
  firstRouteableMenuName(menuStore.findByCode("settings")),
);

const handleCommand = async (command: string) => {
  if (command === "settings") {
    if (settingsEntryName.value) {
      await router.push({ name: settingsEntryName.value });
    }
  } else if (command === "logout") {
    try {
      await authStore.logout();
    } catch {
      // proceed with redirect even if server cancel fails
    }
    menuStore.reset();
    agenticStore.reset();
    await router.push({ name: "login" });
  } else if (command === "help") {
    const helpWindow = window.open(
      "https://dc3.site",
      "_blank",
      "noopener,noreferrer",
    );
    if (helpWindow) helpWindow.opener = null;
  }
};
</script>

<style lang="scss" scoped>
@use '@/styles/glass-capsule.scss' as *;

.container {
  color: var(--dc3-text-primary);
  -moz-osx-font-smoothing: grayscale;
  -webkit-font-smoothing: antialiased;
  font-family: "Avenir", Helvetica, Arial, sans-serif;

  .header {
    position: relative;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--dc3-space-3);
    box-sizing: border-box;
    width: 100%;
    height: var(--dc3-header-height);
    padding: 9px clamp(10px, 2vw, 24px);
    border-bottom: 1px solid var(--dc3-border-base);
    background:
      radial-gradient(circle at 12% -80%, var(--dc3-ambient-primary), transparent 36%),
      color-mix(in srgb, var(--dc3-bg-body) 88%, transparent);
    backdrop-filter: blur(16px) saturate(1.2);
    -webkit-backdrop-filter: blur(16px) saturate(1.2);

    .header_brand_glass,
    .header_actions_glass {
      @include glass-capsule(
        $extra-bg: color-mix(in srgb, var(--dc3-bg-elevated) 82%, transparent)
      );
      transition:
        border-color 260ms ease,
        box-shadow 260ms ease;

      &:hover {
        border-color: var(--dc3-border-strong);
        box-shadow:
          var(--dc3-shadow-hover),
          inset 0 1px 0 var(--dc3-highlight-sheen);
      }
    }

    .header_brand_glass {
      flex: 0 0 auto;
      padding: 5px 14px 5px 6px;
    }

    .header_actions_glass {
      flex: 0 1 auto;
      min-width: 0;
      max-width: calc(100% - 228px);
      padding: 3px 5px 3px 7px;
    }

    .header_menu_wrap {
      flex: 1 1 auto;
      min-width: 0;
      height: 100%;
      overflow: hidden;
    }

    .header_menu_toggle {
      display: none;
      flex: 0 0 auto;
      margin-right: 2px;
    }

    .header_user {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
    }

    .header_utilities {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
      gap: 3px;
    }

    .header_settings_button {
      width: 32px;
      height: 32px;
      border: 1px solid var(--dc3-border-base);
      background: var(--dc3-bg-interactive);
      color: var(--dc3-text-regular);
      font-size: 16px;
      transition: color 180ms ease, background-color 180ms ease, transform 180ms ease;

      &:hover,
      &:focus-visible {
        background: var(--dc3-bg-interactive-active);
        color: var(--dc3-text-brand);
        transform: rotate(18deg);
      }
    }

    .user_trigger {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 32px;
      min-height: 32px;
      padding: 2px;
      border: 0;
      border-radius: 16px;
      background: transparent;
      cursor: pointer;
      transition: background-color 180ms ease, box-shadow 180ms ease;

      &:hover,
      &:focus-visible {
        outline: none;
        background: var(--dc3-bg-interactive);
        box-shadow: inset 0 0 0 1px var(--dc3-border-base);
      }

      .user_avatar {
        display: block;
        width: 28px;
        height: 28px;
        border-radius: 50%;
        box-shadow:
          0 5px 12px rgba(23, 130, 191, 0.2),
          inset 0 1px 0 rgba(255, 255, 255, 0.34);
        object-fit: cover;
      }
    }

    .header_actions_divider {
      flex: 0 0 1px;
      width: 1px;
      height: 22px;
      margin: 0 3px;
      background: var(--dc3-border-base);
    }

    // Tablet keeps navigation visible as accessible icon buttons. Language,
    // settings, and account remain first-class actions in the same capsule.
    @media (max-width: $breakpoint-sm-max) {
      .header_actions_glass {
        max-width: calc(100% - 218px);
      }
    }

    // Phone keeps exactly two capsules: menu + brand on the left, user on
    // the right. The drawer remains the navigation surface.
    @media (max-width: $breakpoint-xs-max) {
      gap: var(--dc3-space-2);
      padding-right: var(--dc3-space-2);
      padding-left: var(--dc3-space-2);

      .header_brand_glass {
        min-width: 0;
        padding-right: 11px;
      }

      .header_menu_toggle {
        display: inline-flex;
      }

      .header_actions_glass {
        flex: 0 0 auto;
        max-width: none;
        padding: 3px 5px;
      }

      .header_settings_button {
        width: 30px;
        height: 30px;
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
    padding: var(--dc3-space-2);
    overflow: hidden;
    position: absolute;
    background: var(--dc3-bg-canvas);

    .body-main {
      display: flex;
      flex-direction: column;
      flex: 1 1 auto;
      min-width: 0;
      height: 100%;

      > .el-scrollbar {
        flex: 1;
        min-height: 0;

        :deep(.el-scrollbar__view) {
          min-height: 100%;
        }
      }

      .fixed-viewport {
        flex: 1;
        min-height: 0;
        overflow: hidden;
      }
    }

    .breadcrumb {
      padding: 10px var(--dc3-space-4);
      margin-bottom: var(--dc3-space-2);
      border: 1px solid var(--dc3-border-base);
      background: var(--dc3-bg-elevated);
      border-radius: var(--dc3-radius-lg);
      box-shadow: var(--dc3-shadow-sm);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);

      .breadcrumb__item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }

      .breadcrumb__icon {
        font-size: 14px;
      }
    }

    @media (max-width: $breakpoint-xs-max) {
      padding: var(--dc3-space-1);

      .breadcrumb {
        padding: 9px var(--dc3-space-3);
        margin-bottom: var(--dc3-space-1);
      }
    }
  }
}
</style>
