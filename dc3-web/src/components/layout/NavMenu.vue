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
  One component, three physical presentations (A2): labelled horizontal on
  desktop, icon-compact horizontal on tablet, and vertical inside the mobile
  navigation drawer. Horizontal overflow never falls back to a mystery
  ellipsis item.
-->

<template>
  <el-menu
    :default-active="activeIndex"
    :class="{'nav-menu--compact': compact && mode === 'horizontal'}"
    :ellipsis="false"
    :mode="mode"
    :router="true"
    class="nav-menu"
  >
    <el-menu-item index="/home">
      <el-icon>
        <HomeFilled/>
      </el-icon>
      <span class="nav-menu__label">{{ t('nav.home') }}</span>
    </el-menu-item>
    <template v-for="node in topLevelMenus" :key="`menu-${node.id}`">
      <el-sub-menu v-if="node.children && node.children.length > 0" :index="`node-${node.id}`">
        <template #title>
          <el-icon v-if="node.menuExt?.content?.icon">
            <component :is="node.menuExt.content.icon"/>
          </el-icon>
          <span class="nav-menu__label">{{ resolveMenuTitle(node) }}</span>
        </template>
        <el-menu-item
          v-for="child in node.children"
          :key="`menu-child-${child.id}`"
          :index="child.menuExt?.content?.url || '/' + child.menuCode"
        >
          <el-icon v-if="child.menuExt?.content?.icon">
            <component :is="child.menuExt.content.icon"/>
          </el-icon>
          <span>{{ resolveMenuTitle(child) }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="node.menuExt?.content?.url || '/' + node.menuCode">
        <el-icon v-if="node.menuExt?.content?.icon">
          <component :is="node.menuExt.content.icon"/>
        </el-icon>
        <span class="nav-menu__label">{{ resolveMenuTitle(node) }}</span>
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
  compact: {
    type: Boolean,
    default: false,
  },
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
    width: 100%;
    border-bottom: none !important;
    background: transparent;

    :deep(> .el-menu-item),
    :deep(> .el-sub-menu .el-sub-menu__title) {
      position: relative;
      height: 36px;
      padding: 0 var(--dc3-space-3);
      border: 1px solid transparent;
      border-radius: var(--dc3-radius-lg);
      color: var(--dc3-text-regular);
      line-height: 36px;
      transition:
        color var(--dc3-duration-fast) var(--dc3-ease-standard),
        background-color var(--dc3-duration-fast) var(--dc3-ease-standard),
        box-shadow var(--dc3-duration-fast) var(--dc3-ease-standard);
    }

    :deep(> .el-menu-item:hover),
    :deep(> .el-sub-menu .el-sub-menu__title:hover) {
      background: var(--dc3-bg-interactive);
      color: var(--el-color-primary);
    }

    :deep(> .el-menu-item.is-active),
    :deep(> .el-sub-menu.is-active .el-sub-menu__title) {
      border-bottom-color: transparent;
      border-color: var(--dc3-border-strong);
      background: var(--dc3-brand-gradient-soft);
      box-shadow:
        inset 0 1px 0 var(--dc3-highlight-sheen),
        var(--dc3-shadow-active);
      color: var(--dc3-text-brand);
      font-weight: 650;
    }

    :deep(> .el-menu-item.is-active::after),
    :deep(> .el-sub-menu.is-active .el-sub-menu__title::after) {
      position: absolute;
      bottom: 3px;
      left: 50%;
      width: 12px;
      height: 2px;
      border-radius: var(--dc3-radius-full);
      background: var(--dc3-brand-gradient);
      content: '';
      transform: translateX(-50%);
    }

    :deep(> .el-menu-item:focus-visible),
    :deep(> .el-sub-menu .el-sub-menu__title:focus-visible) {
      outline: none;
      box-shadow: var(--dc3-focus-ring);
    }
  }

  &.nav-menu--compact {
    :deep(> .el-menu-item),
    :deep(> .el-sub-menu .el-sub-menu__title) {
      width: 36px;
      padding: 0;
      justify-content: center;
    }

    :deep(> .el-menu-item .el-icon),
    :deep(> .el-sub-menu .el-sub-menu__title .el-icon) {
      margin-right: 0;
    }

    .nav-menu__label {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  }

  &.el-menu--vertical {
    border-right: 0;
    background: transparent;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: var(--dc3-touch-target);
      margin: 2px 0;
      border-radius: var(--dc3-radius-lg);
      color: var(--dc3-text-regular);
    }

    :deep(.el-menu-item:hover),
    :deep(.el-sub-menu__title:hover) {
      background: var(--dc3-bg-interactive);
      color: var(--el-color-primary);
    }

    :deep(.el-menu-item.is-active) {
      background: var(--dc3-brand-gradient-soft);
      box-shadow: inset 0 0 0 1px var(--dc3-border-strong);
      color: var(--dc3-text-brand);
      font-weight: 650;
    }
  }

  .el-menu-item {
    font-size: 15px;
  }
}
</style>
