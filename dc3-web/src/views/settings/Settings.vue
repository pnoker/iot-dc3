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
  - mobile: drawer opened from a floating toggle (drawer navigation, A3).
  The menu itself is SettingsSidebarMenu, shared by all three hosts.
-->

<template>
  <el-container class="settings-container">
    <el-aside v-if="!isMobile" :width="asideWidth" class="settings-aside">
      <el-card class="settings-aside-card" shadow="never">
        <div class="settings-aside-toolbar">
          <el-button
            :aria-label="asideCollapseLabel"
            :icon="appStore.settingsCollapsed ? Expand : Fold"
            text
            circle
            @click="appStore.toggleSettingsCollapsed()"
          />
        </div>
        <el-scrollbar>
          <settings-sidebar-menu :collapsed="appStore.settingsCollapsed"/>
        </el-scrollbar>
      </el-card>
    </el-aside>

    <el-drawer
      v-model="asideDrawerVisible"
      :size="280"
      :title="t('layout.settings')"
      :with-header="true"
      class="settings-drawer"
      direction="ltr"
    >
      <settings-sidebar-menu @select="asideDrawerVisible = false"/>
    </el-drawer>

    <el-main class="settings-main">
      <el-button
        v-if="isMobile"
        :aria-label="t('layout.settings')"
        :icon="Setting"
        class="settings-aside-toggle"
        circle
        @click="asideDrawerVisible = true"
      />
      <el-scrollbar>
        <router-view/>
      </el-scrollbar>
    </el-main>
  </el-container>
</template>

<script lang="ts" setup>
import {Expand, Fold, Setting} from '@element-plus/icons-vue';
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {useBreakpoint} from '@/composables/useBreakpoint';
import {useAppStore} from '@/store';
import SettingsSidebarMenu from '@/views/settings/components/SettingsSidebarMenu.vue';

const {t} = useI18n();
const {isMobile} = useBreakpoint();
const appStore = useAppStore();

const asideDrawerVisible = ref(false);

// Desktop aside width follows the L2 layout tokens; collapsed rail is
// icon-only (64px) on tablet.
const asideWidth = computed(() => (appStore.settingsCollapsed ? '64px' : '220px'));

const asideCollapseLabel = computed(() =>
  t(appStore.settingsCollapsed ? 'layout.expandSettings' : 'layout.collapseSettings'),
);
</script>

<style lang="scss" scoped>
.settings-container {
  align-items: stretch;
  gap: 4px;
  height: 100%;
  min-width: 0;
}

.settings-aside {
  .settings-aside-card {
    height: 100%;
    border: 0;
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
    justify-content: flex-end;
    padding: var(--dc3-space-2) var(--dc3-space-2) 0;
  }
}

.settings-main {
  padding: 0;
  min-width: 0;
  overflow: hidden;
  position: relative;

  > .el-scrollbar {
    height: 100%;
  }

  // Floating menu trigger for thumb terminals: keeps the content area
  // free of a permanent bar (A3 thumb-zone ergonomics).
  .settings-aside-toggle {
    position: absolute;
    top: var(--dc3-space-3);
    left: var(--dc3-space-3);
    z-index: 10;
  }
}

.settings-drawer {
  :deep(.el-drawer__body) {
    padding: var(--dc3-space-2);
  }
}
</style>
