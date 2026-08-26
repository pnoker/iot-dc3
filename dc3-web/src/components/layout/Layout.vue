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
        <img :src="assetUrl('images/logo/logo.svg')" class="header_logo" />
        <span class="header_title">IoT DC3</span>
      </div>
      <div class="header_actions_glass">
        <div v-if="!isMobile" class="header_menu_wrap">
          <nav-menu :compact="isTablet" mode="horizontal" />
        </div>
        <span v-if="!isMobile" class="header_actions_divider" aria-hidden="true" />
        <div class="header_utilities">
          <div :aria-label="t('layout.language')" class="header_language_switch" role="group">
            <button
              :aria-pressed="langModel === 'en'"
              :class="{active: langModel === 'en'}"
              type="button"
              @click="langModel = 'en'"
            >
              EN
            </button>
            <button
              :aria-pressed="langModel === 'zh'"
              :class="{active: langModel === 'zh'}"
              type="button"
              @click="langModel = 'zh'"
            >
              中
            </button>
          </div>
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
          <el-dropdown trigger="click" @command="handleCommand">
            <button :aria-label="t('layout.account')" class="user_trigger" type="button">
              <span class="user_initial" aria-hidden="true">{{ currentInitial }}</span>
              <span v-if="isDesktop" class="user_name">{{ currentLogin }}</span>
              <el-icon v-if="isDesktop" class="user_chevron"><ArrowDown/></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
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
import NavMenu from "@/components/layout/NavMenu.vue";
import { useBreakpoint } from "@/composables/useBreakpoint";
import router from "@/config/router";
import {
  ArrowDown,
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

const { t, locale } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const agenticStore = useAgenticStore();
const { isDesktop, isMobile, isTablet } = useBreakpoint();
const navDrawerVisible = ref(false);
const currentLogin = computed(() =>
  String(authStore.getName || authStore.name || "dc3"),
);
const currentInitial = computed(() => Array.from(currentLogin.value.trim())[0]?.toUpperCase() || "D");

// Close the mobile navigation drawer once navigation actually happens.
watch(
  () => route.fullPath,
  () => {
    navDrawerVisible.value = false;
  },
);

// The AI assistant is shown in every build; in mock builds the fetch
// interceptor (src/mock/fetch.ts) answers its chat completions.

const langModel = computed({
  get: () => locale.value,
  set: (val: string) => {
    locale.value = val;
    localStorage.setItem("locale", val);
  },
});

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
      "https://doc.dc3.site",
      "_blank",
      "noopener,noreferrer",
    );
    if (helpWindow) helpWindow.opener = null;
  }
};
</script>

<style lang="scss" scoped>
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
    border-bottom: 1px solid rgba(148, 203, 229, 0.22);
    background:
      radial-gradient(circle at 12% -80%, rgba(92, 215, 244, 0.2), transparent 36%),
      color-mix(in srgb, var(--dc3-bg-body) 88%, transparent);
    backdrop-filter: blur(16px) saturate(1.2);
    -webkit-backdrop-filter: blur(16px) saturate(1.2);

    .header_brand_glass,
    .header_actions_glass {
      position: relative;
      isolation: isolate;
      display: flex;
      align-items: center;
      box-sizing: border-box;
      height: 42px;
      overflow: hidden;
      border: 1px solid rgba(148, 216, 246, 0.34);
      border-radius: 21px;
      background:
        radial-gradient(
          circle at 18% 0%,
          rgba(255, 255, 255, 0.82),
          transparent 38%
        ),
        linear-gradient(
          135deg,
          rgba(255, 255, 255, 0.54),
          rgba(115, 205, 241, 0.13) 52%,
          rgba(75, 88, 210, 0.08)
        );
      box-shadow:
        0 10px 28px rgba(12, 89, 153, 0.1),
        inset 0 1px 0 rgba(255, 255, 255, 0.86),
        inset 0 -8px 18px rgba(55, 131, 203, 0.05);
      backdrop-filter: blur(18px) saturate(1.45);
      -webkit-backdrop-filter: blur(18px) saturate(1.45);
      transition:
        border-color 260ms ease,
        box-shadow 260ms ease;

      &::after {
        position: absolute;
        z-index: -1;
        top: 1px;
        right: 12px;
        left: 12px;
        height: 42%;
        border-radius: 999px;
        background: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0.48),
          transparent
        );
        opacity: 0.72;
        pointer-events: none;
        content: "";
      }

      &:hover {
        border-color: rgba(61, 172, 224, 0.46);
        box-shadow:
          0 14px 34px rgba(12, 89, 153, 0.15),
          inset 0 1px 0 rgba(255, 255, 255, 0.92),
          inset 0 -8px 18px rgba(55, 131, 203, 0.07);
      }
    }

    .header_brand_glass {
      flex: 0 0 auto;
      padding: 5px 13px 5px 6px;
    }

    .header_actions_glass {
      flex: 0 1 auto;
      min-width: 0;
      max-width: calc(100% - 178px);
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

    .header_language_switch {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 2px;
      box-sizing: border-box;
      width: 68px;
      height: 30px;
      padding: 2px;
      border: 1px solid rgba(88, 161, 199, 0.16);
      border-radius: 15px;
      background: rgba(255, 255, 255, 0.34);

      button {
        display: grid;
        place-items: center;
        min-width: 0;
        padding: 0;
        border: 0;
        border-radius: 12px;
        background: transparent;
        color: #637b8b;
        cursor: pointer;
        font-family: inherit;
        font-size: 11px;
        font-weight: 680;
        line-height: 24px;
        transition: color 180ms ease, background-color 180ms ease, box-shadow 180ms ease;

        &:hover,
        &:focus-visible {
          outline: none;
          color: #0878b8;
        }

        &.active {
          background: rgba(255, 255, 255, 0.92);
          box-shadow:
            0 3px 9px rgba(12, 89, 153, 0.13),
            inset 0 0 0 1px rgba(18, 150, 219, 0.08);
          color: #0878b8;
        }
      }
    }

    .header_settings_button {
      width: 32px;
      height: 32px;
      border: 1px solid rgba(88, 161, 199, 0.12);
      background: rgba(255, 255, 255, 0.28);
      color: #456578;
      font-size: 16px;
      transition: color 180ms ease, background-color 180ms ease, transform 180ms ease;

      &:hover,
      &:focus-visible {
        background: rgba(18, 150, 219, 0.1);
        color: #0878b8;
        transform: rotate(18deg);
      }
    }

    .user_trigger {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      min-height: 32px;
      padding: 2px 7px 2px 2px;
      border: 0;
      border-radius: 16px;
      background: transparent;
      color: #2d4658;
      cursor: pointer;
      font: inherit;
      transition: background-color 180ms ease, box-shadow 180ms ease;

      &:hover,
      &:focus-visible {
        outline: none;
        background: rgba(18, 150, 219, 0.09);
        box-shadow: inset 0 0 0 1px rgba(18, 150, 219, 0.1);
      }

      .user_initial {
        display: grid;
        place-items: center;
        flex: 0 0 auto;
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: linear-gradient(135deg, #119bd6, #5558c9);
        box-shadow:
          0 5px 12px rgba(23, 130, 191, 0.2),
          inset 0 1px 0 rgba(255, 255, 255, 0.34);
        color: #ffffff;
        font-size: 12px;
        font-weight: 760;
        line-height: 1;
      }

      .user_name {
        max-width: 100px;
        overflow: hidden;
        color: #304b5d;
        font-size: 13px;
        font-weight: 620;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .user_chevron {
        color: #78909f;
        font-size: 11px;
      }
    }

    .header_actions_divider {
      flex: 0 0 1px;
      width: 1px;
      height: 22px;
      margin: 0 3px;
      background: rgba(94, 155, 188, 0.22);
    }

    // Tablet keeps navigation visible as accessible icon buttons. Language,
    // settings, and account remain first-class actions in the same capsule.
    @media (max-width: $breakpoint-sm-max) {
      .header_actions_glass {
        max-width: calc(100% - 164px);
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

      .header_title {
        margin-left: 6px;
        font-size: 18px;
      }

      .header_actions_glass {
        flex: 0 0 auto;
        max-width: none;
        padding: 3px 5px;
      }

      .header_language_switch {
        width: 66px;
      }

      .header_settings_button {
        width: 30px;
        height: 30px;
      }

      .user_trigger {
        padding-right: 2px;
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
