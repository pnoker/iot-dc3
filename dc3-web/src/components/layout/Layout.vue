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
    <a class="skip-link" href="#main-content">{{ t('layout.skipToContent') }}</a>
    <header class="header">
      <div class="header_brand_glass">
        <brand-lockup :compact="isMobile" />
      </div>
      <div class="header_actions_row">
        <div class="header_actions_glass">
          <div class="header_menu_wrap">
            <nav-menu :compact="isTablet || isMobile" mode="horizontal" />
          </div>
          <span class="header_actions_divider" aria-hidden="true" />
          <div class="header_utilities">
            <app-preferences v-if="!isMobile" compact />
            <!-- Phone folds language/theme switching behind one "…" chip so
                 the primary menu keeps its place in the capsule (A3). -->
            <el-popover v-else :width="196" placement="bottom-end" trigger="click">
              <template #reference>
                <el-button
                  :aria-label="t('layout.preferences')"
                  :icon="MoreFilled"
                  circle
                  class="header_more_button"
                  text
                />
              </template>
              <app-preferences surface />
            </el-popover>
            <!-- Phone: the header settings button doubles as the settings
                 navigation — it bubbles up the fully-expanded menu instead
                 of navigating, so users switch settings pages through a
                 control they can already see (no hidden corner toggle). -->
            <el-popover
              v-if="settingsEntryName && isMobile"
              v-model:visible="settingsMenuVisible"
              :persistent="false"
              :width="280"
              placement="bottom-end"
              popper-class="settings-nav-popover"
              trigger="click"
            >
              <template #reference>
                <el-button
                  :aria-label="t('layout.settings')"
                  :icon="Setting"
                  circle
                  class="header_settings_button"
                  text
                />
              </template>
              <settings-sidebar-menu expand-all @select="settingsMenuVisible = false" />
            </el-popover>
            <el-tooltip v-else-if="settingsEntryName" :content="t('layout.settings')" placement="bottom">
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
                <el-icon class="user_trigger__icon" :size="16"><UserFilled/></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <li class="user_dropdown_identity" role="none">
                    <span class="user_dropdown_avatar" aria-hidden="true">
                      <el-icon :size="18"><UserFilled/></el-icon>
                    </span>
                    <span class="user_dropdown_meta">
                      <span class="user_dropdown_name">{{ currentLogin }}</span>
                      <span class="user_dropdown_tenant">{{ t("layout.tenantLabel") }} · {{ currentTenant }}</span>
                    </span>
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
      </div>
    </header>
    <div class="body">
      <div id="main-content" class="body-main" tabindex="-1">
        <div v-if="breadcrumbItems.length > 1" class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumbItems"
              :key="`${item.path}-${index}`"
              :to="item.nav?.length ? undefined : item.path"
            >
              <!-- Crumbs with a nav payload open the layered picker instead
                   of navigating: the settings root lists every group, and a
                   mid-level crumb lists its group's sibling pages — same
                   bubble pattern as the phone settings menu. -->
              <el-popover
                v-if="item.nav?.length"
                :persistent="false"
                :visible="openBreadcrumbNav === `${item.path}-${index}`"
                :width="252"
                placement="bottom-start"
                popper-class="breadcrumb-nav-popover"
                trigger="click"
                @update:visible="onBreadcrumbNavToggle(`${item.path}-${index}`, $event)"
              >
                <template #reference>
                  <button :aria-label="item.title" class="breadcrumb__item breadcrumb__item--nav" type="button">
                    <el-icon v-if="item.icon" class="breadcrumb__icon">
                      <component :is="item.icon" />
                    </el-icon>
                    <span>{{ item.title }}</span>
                    <el-icon class="breadcrumb__caret">
                      <CaretBottom />
                    </el-icon>
                  </button>
                </template>
                <div class="breadcrumb-nav">
                  <template v-for="group in item.nav" :key="group.name">
                    <div v-if="item.nav && item.nav.length > 1" class="breadcrumb-nav__group-title">
                      {{ group.title }}
                    </div>
                    <button
                      v-for="leaf in group.leaves"
                      :key="leaf.name"
                      :class="{'is-active': leaf.name === activeSettingsName}"
                      class="breadcrumb-nav__leaf"
                      type="button"
                      @click="goBreadcrumbNav(leaf.name)"
                    >
                      {{ leaf.title }}
                    </button>
                  </template>
                </div>
              </el-popover>
              <span v-else class="breadcrumb__item">
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
  CaretBottom,
  MoreFilled,
  QuestionFilled,
  UserFilled,
  Setting,
  SwitchButton,
} from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import {
  getSettingsActiveName,
  getSettingsRouteName,
  getSettingsTitleKey,
  SETTINGS_BREADCRUMB_PARENTS,
  SETTINGS_FALLBACK_ICON,
  SETTINGS_FALLBACK_SIDEBAR,
  SETTINGS_TITLE_KEYS,
} from "@/config/settingsNav";
import { useAgenticStore, useAuthStore, useMenuStore } from "@/store";
import type { MenuNode } from "@/store/modules/menu";
import { resolveMenuTitle } from "@/utils/menuUtil";

import AppPreferences from "@/components/layout/AppPreferences.vue";
import SettingsSidebarMenu from "@/views/settings/components/SettingsSidebarMenu.vue";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const agenticStore = useAgenticStore();
const { isMobile, isTablet } = useBreakpoint();
const currentLogin = computed(() =>
  String(authStore.getName || authStore.name || "dc3"),
);
const currentTenant = computed(() =>
  String(authStore.getTenant || "default"),
);
const settingsMenuVisible = ref(false);

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
  pointValueDetail: "nav.pointValueDetail",
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
  // Non-settings routes only: the settings domain resolves through
  // SETTINGS_FALLBACK_ICON (see iconForCode) — the truth source for every
  // settings icon lives in src/config/settingsNav.ts and the backend seed.
  home: 'HomeFilled',
  driver: 'Promotion',
  profile: 'List',
  device: 'Management',
  pointValue: 'TrendCharts',
  driverDetail: 'Promotion',
  deviceDetail: 'Management',
  deviceEdit: 'Management',
  profileDetail: 'List',
  profileEdit: 'List',
  // Legacy redirect route names (not present in SETTINGS_FALLBACK_ICON):
  // mirror their redirect targets' icons.
  settingsEvent: 'Lightning',
  settingsCommand: 'Position',
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

// ---- layered breadcrumb navigation ---------------------------------------
// Crumbs that own a subtree (the settings root, the mid-level groups) open
// a picker popover instead of hard-navigating — the settings root lists
// every group, a mid-level crumb lists its group's sibling pages. Same
// data source as the settings sidebar: the backend menu tree when loaded,
// the static fallback otherwise.
interface BreadcrumbNavLeaf {
  name: string;
  title: string;
}
interface BreadcrumbNavGroup {
  name: string;
  title: string;
  leaves: BreadcrumbNavLeaf[];
}
interface BreadcrumbItem {
  path: string;
  title: string;
  icon?: string;
  nav?: BreadcrumbNavGroup[];
}

const settingsNavTitle = (code: string, fallback: () => string) => {
  const key = SETTINGS_TITLE_KEYS[code];
  return key ? t(key) : fallback();
};

const settingsNavGroups = computed<BreadcrumbNavGroup[]>(() => {
  const settings = menuStore.findByCode("settings");
  const children = settings?.children || [];
  if (menuStore.loaded && children.length) {
    return children
      .slice()
      .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
      .map((group) => ({
        name: group.menuCode,
        title: settingsNavTitle(group.menuCode, () => resolveMenuTitle(group)),
        leaves: (group.children || [])
          .slice()
          .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0))
          .map((leaf) => ({
            name: leaf.menuCode,
            title: settingsNavTitle(leaf.menuCode, () => resolveMenuTitle(leaf)),
          })),
      }));
  }
  return SETTINGS_FALLBACK_SIDEBAR.map((group) => ({
    name: group.name,
    title: t(group.titleKey),
    leaves: (group.children || []).map((leaf) => ({name: leaf.name, title: t(leaf.titleKey)})),
  }));
});

// A group code maps to its own group; a leaf code maps to the group that
// contains it (the crumb's sibling pages).
const navForCode = (code: string): BreadcrumbNavGroup[] => {
  const groups = settingsNavGroups.value;
  const own = groups.find((group) => group.name === code);
  if (own) return [own];
  const parent = groups.find((group) => group.leaves.some((leaf) => leaf.name === code));
  return parent ? [parent] : [];
};

const openBreadcrumbNav = ref("");
const onBreadcrumbNavToggle = (key: string, visible: boolean) => {
  openBreadcrumbNav.value = visible ? key : "";
};
const goBreadcrumbNav = (name: string) => {
  openBreadcrumbNav.value = "";
  void router.push({name: getSettingsRouteName(name)});
};

const activeSettingsName = computed(() => getSettingsActiveName(String(route.name || "")));

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = [
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
  } else if (name.startsWith("pointValue")) {
    // Point value history is a top-level destination in its own right —
    // the startsWith("point") branch below is for the profile-tree detail
    // pages and wrongly stamped "Profile" onto this page's breadcrumb.
    // startsWith also covers pointValueDetail (route detail page).
    items.push({
      path: "/point_value",
      title: t("nav.pointValue"),
      icon: iconForCode("pointValue"),
    });
  } else if (name.startsWith("point")) {
    items.push({
      path: "/profile",
      title: t("nav.profile"),
      icon: iconForCode("profile"),
    });
  } else if (name.startsWith("settings")) {
    // "/settings" itself is not a route — navigating there surfaced a
    // permission error and bounced to home. The settings crumb now opens
    // the layered picker instead of navigating; mid-level crumbs get
    // their group's sibling pages.
    items.push({
      path: "/settings",
      title: t("nav.settings"),
      icon: iconForCode("settings"),
      nav: settingsNavGroups.value,
    });
    (SETTINGS_BREADCRUMB_PARENTS[name] || []).forEach((mid) => {
      items.push({
        path: mid.path,
        title: t(mid.titleKey),
        icon: iconForCode(mid.code),
        nav: navForCode(mid.code),
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

.skip-link {
  position: fixed;
  z-index: 1000;
  top: var(--dc3-space-2);
  left: var(--dc3-space-2);
  padding: var(--dc3-space-2) var(--dc3-space-3);
  border-radius: var(--dc3-radius-md);
  background: var(--el-color-primary);
  color: var(--el-color-white);
  box-shadow: var(--dc3-shadow-md);
  transform: translateY(-200%);
  transition: transform var(--dc3-duration-fast) var(--dc3-ease-standard);

  &:focus {
    transform: translateY(0);
    outline: none;
    box-shadow: var(--dc3-focus-ring), var(--dc3-shadow-md);
  }
}

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
    padding-block: 9px;
    padding-inline: var(--dc3-page-padding);
    border-bottom: 1px solid var(--dc3-border-base);
    background:
      radial-gradient(circle at 12% -80%, var(--dc3-ambient-primary), transparent 36%),
      color-mix(in srgb, var(--dc3-bg-body) 88%, transparent);
    backdrop-filter: blur(16px) saturate(1.2);
    -webkit-backdrop-filter: blur(16px) saturate(1.2);

    .header_brand_glass,
    .header_actions_row {
      flex: 0 1 auto;
      min-width: 0;
      max-width: calc(100% - 228px);
    }

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

    // Brand sits in the same glass-capsule family as the actions capsule
    // and the login brand pill (glass-capsule.scss) — the logo never rides
    // bare on the header background.
    .header_brand_glass {
      @include glass-capsule(
        $extra-bg: color-mix(in srgb, var(--dc3-bg-elevated) 82%, transparent)
      );
      flex: 0 0 auto;
      padding: 4px 14px 4px 6px;
    }

    .header_actions_glass {
      min-width: 0;
      padding: 3px 5px 3px 7px;
    }

    .header_menu_wrap {
      flex: 1 1 auto;
      min-width: 0;
      height: 100%;
      overflow: hidden;
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
      gap: var(--dc3-space-2);
    }

    // Settings and the phone-only preferences ("…") chip share one utility
    // button treatment so the capsule end reads as a single control group.
    .header_settings_button,
    .header_more_button {
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
      }
    }

    .header_settings_button:hover,
    .header_settings_button:focus-visible {
      transform: rotate(18deg);
    }

    // Account tile follows the platform's tone-tile grammar (the same
    // accent-tinted glyph block family as ThingsCardHeader / StatCard):
    // brand-tinted disc, hairline accent ring, glyph in the accent colour.
    .user_trigger {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 32px;
      min-height: 32px;
      padding: 0;
      border: 1px solid color-mix(in srgb, var(--el-color-primary) 26%, transparent);
      border-radius: var(--dc3-radius-full);
      background:
        radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.5), transparent 52%),
        var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      cursor: pointer;
      transition:
        background-color 180ms ease,
        box-shadow 180ms ease,
        transform 180ms ease;

      &:hover,
      &:focus-visible {
        outline: none;
        background-color: var(--el-color-primary-light-8);
        box-shadow:
          0 2px 8px color-mix(in srgb, var(--el-color-primary) 26%, transparent),
          inset 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 34%, transparent);
      }

      &:active {
        transform: scale(0.96);
      }
    }

    // One shoulder width for every divider and control group in the
    // capsule: menu|⌐utilities⌐|user and the AppPreferences internal
    // divider all read from the same 8px rhythm, so no gap looks pinched
    // next to its neighbour.
    .header_actions_divider {
      flex: 0 0 1px;
      width: 1px;
      height: 22px;
      margin: 0 var(--dc3-space-2);
      background: var(--dc3-border-base);
    }

    // Tablet keeps navigation visible as accessible icon buttons. Language,
    // settings, and account remain first-class actions in the same capsule.
    @media (max-width: $breakpoint-sm-max) {
      .header_actions_row {
        max-width: calc(100% - 218px);
      }
    }

    // Phone keeps the same single-row contract as every other tier: brand
    // capsule (logo-only) on the left, one capsule holding the icon-compact
    // menu plus the folded utilities on the right. Language/theme live
    // behind the "…" chip instead of stealing menu space, so primary
    // navigation never leaves the header.
    @media (max-width: $breakpoint-xs-max) {
      gap: var(--dc3-space-2);
      padding-block: 8px;
      padding-inline: var(--dc3-page-padding);

      // Logo-only content in a square box reads as a circle; asymmetric
      // side padding makes the 42px capsule near-square, which the
      // pill radius then renders as a squashed oval.
      .header_brand_glass {
        flex: 0 0 42px;
        width: 42px;
        min-width: 0;
        padding: 0;
        justify-content: center;
      }

      .header_actions_row {
        flex: 1 1 auto;
        max-width: none;
      }

      .header_actions_glass {
        width: 100%;
      }

      .header_settings_button,
      .header_more_button {
        width: 30px;
        height: 30px;
      }
    }
  }

  .body {
    top: var(--dc3-header-height);
    right: 0;
    left: 0;
    bottom: 0;
    display: flex;
    padding: var(--dc3-page-padding);
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
      display: flex;
      align-items: center;
      // Optical centering for CJK ink: glyph ink sits above the em-box
      // middle (descender space lives below), so a geometrically centred
      // row still reads high — and the bias grows under fractional
      // display scaling. Calibrated per user perception across terminals:
      // 0px reads high, 0.5px still high, 1px lands on the axis. Keep the
      // total padding height constant (9 + 7 = 2 × 8).
      padding: calc(var(--dc3-space-2) + 1px) var(--dc3-space-4) calc(var(--dc3-space-2) - 1px);
      margin-bottom: var(--dc3-space-2);
      border: 1px solid var(--dc3-border-base);
      background: var(--dc3-bg-elevated);
      border-radius: var(--dc3-radius-lg);
      box-shadow: var(--dc3-shadow-sm);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);

      // Element Plus ships .el-breadcrumb with line-height:1 and leaves
      // .el-breadcrumb__inner as a plain inline box, so every level of the
      // breadcrumb aligns on text baselines — and CJK glyphs sit high in
      // their em-box, which reads as "content above the middle". Give each
      // level a flex center so the icons and labels are positioned
      // geometrically on the strip's axis instead of by baseline.
      :deep(.el-breadcrumb) {
        line-height: 20px;

        .el-breadcrumb__item,
        .el-breadcrumb__inner {
          display: inline-flex;
          align-items: center;
        }
      }

      .breadcrumb__item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }

      // Crumbs that open the layered picker render as buttons — strip the
      // native chrome so they read exactly like their span siblings.
      .breadcrumb__item--nav {
        padding: 0;
        border: 0;
        background: transparent;
        color: inherit;
        font: inherit;
        cursor: pointer;

        &:hover,
        &:focus-visible {
          color: var(--el-color-primary);
          outline: none;
        }
      }

      .breadcrumb__caret {
        font-size: 10px;
        color: var(--dc3-text-muted);
      }

      .breadcrumb__icon {
        font-size: 14px;
      }
    }

    @media (max-width: $breakpoint-xs-max) {
      .breadcrumb {
        // Same CJK optical offset as the base rule (see above).
        padding: calc(var(--dc3-space-2) + 1px) var(--dc3-space-3) calc(var(--dc3-space-2) - 1px);
        margin-bottom: var(--dc3-space-1);
      }
    }
  }
}
</style>

<style lang="scss">
// The phone settings-nav popover teleports to body, so its skin lives in
// a global block: capped height with internal scrolling keeps every group
// reachable while the arrowed bubble stays anchored to the header button.
.settings-nav-popover.el-popover {
  max-width: calc(100vw - var(--dc3-space-4));
  box-sizing: border-box;
  padding: var(--dc3-space-1) 0;

  .settings-sidebar-menu {
    max-height: min(60vh, 480px);
    overflow-y: auto;
    overscroll-behavior: contain;
  }
}

// Layered breadcrumb picker — same bubble family as the settings nav
// popover, sized for a single group (or the settings root listing all
// groups with small headers between them).
.breadcrumb-nav-popover.el-popover {
  max-width: calc(100vw - var(--dc3-space-4));
  box-sizing: border-box;
  padding: var(--dc3-space-2);

  .breadcrumb-nav {
    display: flex;
    flex-direction: column;
    max-height: min(60vh, 440px);
    overflow-y: auto;
    overscroll-behavior: contain;
  }

  .breadcrumb-nav__group-title {
    padding: var(--dc3-space-1) var(--dc3-space-2) 2px;
    color: var(--dc3-text-muted);
    font-size: 12px;
    font-weight: 600;

    & + .breadcrumb-nav__leaf {
      margin-top: 2px;
    }
  }

  .breadcrumb-nav__leaf {
    display: block;
    width: 100%;
    padding: 0 var(--dc3-space-2);
    border: 0;
    border-radius: var(--dc3-radius-md);
    background: transparent;
    color: var(--dc3-text-regular);
    font-size: 13px;
    line-height: 32px;
    text-align: left;
    cursor: pointer;
    transition:
      color var(--dc3-duration-fast) var(--dc3-ease-standard),
      background-color var(--dc3-duration-fast) var(--dc3-ease-standard);

    &:hover,
    &:focus-visible {
      background: var(--dc3-bg-interactive);
      color: var(--el-color-primary);
      outline: none;
    }

    &.is-active {
      background: var(--dc3-brand-gradient-soft);
      color: var(--el-color-primary);
      font-weight: 650;
    }
  }
}
</style>
