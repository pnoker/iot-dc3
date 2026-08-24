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
  Top-level navigation tree, rendered from the backend menu tree (menuStore).
  One component, two physical implementations (A2): horizontal in the desktop
  header (with Element Plus ellipsis overflow for tablet widths), vertical
  inside the mobile navigation drawer.
-->

<template>
  <el-menu
    :default-active="activeIndex"
    :ellipsis="mode === 'horizontal'"
    :mode="mode"
    :router="true"
    class="nav-menu"
  >
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
          :index="child.menuExt?.content?.url || '/' + child.menuCode"
        >
          <el-icon v-if="child.menuExt?.content?.icon">
            <component :is="child.menuExt.content.icon"/>
          </el-icon>
          {{ resolveMenuTitle(child) }}
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="node.menuExt?.content?.url || '/' + node.menuCode">
        <el-icon v-if="node.menuExt?.content?.icon">
          <component :is="node.menuExt.content.icon"/>
        </el-icon>
        {{ resolveMenuTitle(node) }}
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script lang="ts" setup>
import {HomeFilled} from '@element-plus/icons-vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute} from 'vue-router';

import {useMenuStore} from '@/store';
import {resolveMenuTitle} from '@/utils/menuUtil';

defineProps({
  mode: {
    type: String as () => 'horizontal' | 'vertical',
    default: 'horizontal',
  },
});

const {t} = useI18n();
const route = useRoute();
const menuStore = useMenuStore();

// Top-level menus come from the backend (dc3_menu). Home is rendered separately
// as the leftmost entry with its own fixed icon; Settings is reached from the
// avatar dropdown, not the header bar.
const topLevelMenus = computed(() => {
  return (menuStore.tree || [])
    .filter((n) => n.menuCode !== 'home' && n.menuCode !== 'settings')
    .slice()
    .sort((a, b) => (a.menuIndex ?? 0) - (b.menuIndex ?? 0));
});

// Highlight the top-level segment of the active route, matching the legacy
// handleMenuEnter behaviour.
const activeIndex = computed(() => {
  const path = route.path;
  if (path.indexOf('/') === 0) {
    const split = path.split('/');
    if (split.length > 2) return '/' + split[1];
  }
  return path;
});
</script>

<style lang="scss" scoped>
.nav-menu {
  &.el-menu--horizontal {
    display: flex;
    justify-content: center;
    border-bottom: none !important;
  }

  .el-menu-item {
    font-size: 15px;
  }
}
</style>
