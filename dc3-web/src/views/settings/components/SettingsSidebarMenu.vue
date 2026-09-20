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
  Settings sidebar menu — one component, three physical hosts (A2):
  desktop aside (expanded), tablet aside (collapsed icon rail), and the
  mobile nav popover (expand-all: every group is unfolded so the whole
  tree is scannable in the bubble without extra taps). The menu tree
  comes from the backend (dc3_menu via menuStore) with the frontend
  fallback from settingsNav.
-->

<template>
  <el-menu
    :collapse="collapsed"
    :collapse-transition="false"
    :default-active="activeMenu"
    :default-openeds="defaultOpeneds"
    class="settings-sidebar-menu"
    @select="onSelect"
  >
    <template v-for="item in sidebarItems" :key="item.name">
      <el-sub-menu v-if="item.children?.length" :index="item.name">
        <template #title>
          <el-icon v-if="item.icon">
            <component :is="item.icon"/>
          </el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.name" :index="child.name">
          <el-icon v-if="child.icon">
            <component :is="child.icon"/>
          </el-icon>
          <span>{{ child.title }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="item.name">
        <el-icon v-if="item.icon">
          <component :is="item.icon"/>
        </el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script lang="ts" setup>
import {computed, onMounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {
  getSettingsActiveName,
  getSettingsDefaultOpeneds,
  getSettingsRouteName,
} from '@/config/settingsNav';
import {useMenuStore} from '@/store';

import {useSettingsSidebarItems} from '@/views/settings/composables/useSettingsSidebarItems';

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false,
  },
  /** Unfold every group on mount — the mobile popover shows the full tree. */
  expandAll: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits<{
  (e: 'select'): void;
}>();

const route = useRoute();
const router = useRouter();
const menuStore = useMenuStore();

onMounted(() => {
  // Reuse the cached tree — menu edits invalidate the pinia cache from
  // menuConfig, so force-refetching on every settings visit only re-paid
  // a ~1.5s list_tree for data that is already in memory.
  void menuStore.fetchTree();
});

// Shared with the toolbar search so both read one item source.
const {sidebarItems} = useSettingsSidebarItems();

const activeMenu = computed(() => {
  const name = String(route.name || 'settingsUser');
  return getSettingsActiveName(name);
});

const defaultOpeneds = computed(() => {
  // The mobile popover remounts on every open (persistent=false), so the
  // full-tree openeds reapply each time — no group starts folded.
  if (props.expandAll) {
    return sidebarItems.value.filter((item) => item.children?.length).map((item) => item.name);
  }
  return getSettingsDefaultOpeneds(activeMenu.value);
});

const onSelect = (name: string) => {
  void router.push({name: getSettingsRouteName(name)});
  emit('select');
};
</script>

<style lang="scss" scoped>
.settings-sidebar-menu {
  border-right: none;
  padding: var(--dc3-space-2);
  background: transparent;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 42px;
    margin: 2px 0;
    border-radius: var(--dc3-radius-lg);
    color: var(--dc3-text-regular);
    transition:
      color var(--dc3-duration-fast) var(--dc3-ease-standard),
      background-color var(--dc3-duration-fast) var(--dc3-ease-standard),
      box-shadow var(--dc3-duration-fast) var(--dc3-ease-standard);
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background: var(--dc3-bg-interactive);
    color: var(--el-color-primary);
  }

  :deep(.el-menu-item.is-active) {
    position: relative;
    background: var(--dc3-brand-gradient-soft);
    box-shadow: inset 0 0 0 1px var(--dc3-border-strong);
    color: var(--el-color-primary);
    font-weight: 650;

    &::before {
      position: absolute;
      top: 50%;
      left: 5px;
      width: 3px;
      height: 18px;
      border-radius: var(--dc3-radius-full);
      background: var(--dc3-brand-gradient);
      content: '';
      transform: translateY(-50%);
    }
  }

  :deep(.el-menu-item:focus-visible),
  :deep(.el-sub-menu__title:focus-visible) {
    outline: none;
    box-shadow: var(--dc3-focus-ring);
  }

  &.el-menu--collapse {
    box-sizing: border-box;
    width: 100%;
    padding: var(--dc3-space-2) var(--dc3-space-1);

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      justify-content: center;
      padding: 0;
      // The collapsed title span collapses to zero width but the flex gap
      // still takes space, nudging the icon off the rail centerline.
      gap: 0;
    }
  }
}
</style>
