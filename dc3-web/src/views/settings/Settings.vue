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

<!--
  Settings shell — three physical forms (A2):
  - desktop: fixed 220px aside (user-collapsible, persisted);
  - tablet: 64px icon rail aside;
  - mobile: no aside at all — the header's settings button bubbles up the
    fully-expanded nav popover (users never think to hunt for a floating
    corner toggle, so the shell reuses the button they can already see).
  The menu itself is SettingsSidebarMenu, shared by all three hosts.
-->

<template>
  <el-container class="settings-container">
    <el-aside v-if="!isMobile" :width="asideWidth" class="settings-aside">
      <el-card class="settings-aside-card" shadow="never">
        <!-- Toolbar: quick search fills the free width beside the collapse
             toggle. Collapsed, the search degrades to an equal-sized icon
             whose popover never expands the rail. -->
        <div class="settings-aside-toolbar" :class="{'is-collapsed': sidebarCollapsed}">
          <settings-menu-search :collapsed="sidebarCollapsed" />
          <el-button
            :aria-label="asideCollapseLabel"
            circle
            text
            @click="toggleAside()"
          >
            <!-- 18px matches the el-menu icon size so the toggle and the menu
                 glyphs sit on one visual scale (buttons otherwise inherit the
                 14px control font size). -->
            <el-icon :size="18"><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
          </el-button>
        </div>
        <el-scrollbar>
          <settings-sidebar-menu :collapsed="sidebarCollapsed" />
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
import { Expand, Fold } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useI18n } from "vue-i18n";

import { useBreakpoint } from "@/composables/useBreakpoint";
import { useAppStore } from "@/store";
import SettingsMenuSearch from "@/views/settings/components/SettingsMenuSearch.vue";
import SettingsSidebarMenu from "@/views/settings/components/SettingsSidebarMenu.vue";

const { t } = useI18n();
const { isMobile, isTablet } = useBreakpoint();
const appStore = useAppStore();

// Tablet defaults to the icon rail so the content canvas stays usable at
// 768–1199px; the rail stays expandable via a persisted override, mirroring
// the desktop collapse preference (both survive reloads like theme/density).
const sidebarCollapsed = computed(() => (isTablet.value ? !appStore.settingsRailExpanded : appStore.settingsCollapsed));
const asideWidth = computed(() => (sidebarCollapsed.value ? "64px" : "220px"));

const toggleAside = () => {
  if (isTablet.value) {
    appStore.toggleSettingsRailExpanded();
    return;
  }
  appStore.toggleSettingsCollapsed();
};

const asideCollapseLabel = computed(() =>
  t(
    sidebarCollapsed.value ? "layout.expandSettings" : "layout.collapseSettings",
  ),
);
</script>

<style lang="scss" scoped>
.settings-container {
  align-items: stretch;
  gap: var(--dc3-space-2);
  height: 100%;
  min-width: 0;
}

.settings-aside {
  // el-aside defaults to overflow: auto; the inner el-scrollbar owns scrolling,
  // so a fractional-zoom 1px card overflow must not surface a second scrollbar.
  overflow: hidden;

  .settings-aside-card {
    height: 100%;
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-xl);
    background: var(--dc3-bg-elevated);
    box-shadow: var(--dc3-shadow-sm);
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      padding: 0;
      min-height: 0;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    :deep(.el-scrollbar) {
      height: 100%;
      flex: 1;
      min-height: 0;
    }
  }

  .settings-aside-toolbar {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-1);
    padding: var(--dc3-space-2) var(--dc3-space-2);
    // A hairline separates the toolbar (search + collapse) from the menu
    // tree so the strip reads as its own row rather than the menu's first.
    border-bottom: 1px solid var(--dc3-border-base);

    // In the icon rail (64px) two side-by-side 32px buttons would overflow
    // the rail edges. Stack them instead — collapse on top, search below —
    // each centered on the rail centerline like the menu icons. Kill the
    // Element Plus sibling margin (.el-button + .el-button): it exists for
    // horizontal rows and would push the stacked button off-center.
    &.is-collapsed {
      flex-direction: column;
      justify-content: center;
      gap: var(--dc3-space-2);
      padding: var(--dc3-space-2) 0;

      :deep(.el-button + .el-button) {
        margin-left: 0;
      }
    }
  }
}

.settings-main {
  padding: 0;
  min-width: 0;
  overflow: hidden;
  position: relative;

  > .el-scrollbar {
    height: 100%;

    :deep(.el-scrollbar__view) {
      min-height: 100%;
    }
  }
}
</style>
