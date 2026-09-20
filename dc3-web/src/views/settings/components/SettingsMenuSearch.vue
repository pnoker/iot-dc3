<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<!-- Settings-menu quick search, two host forms over one item source:
     expanded rail → inline input filling the toolbar next to the collapse
     toggle, results in an anchored panel; collapsed rail → an equal-sized
     icon button whose popover carries the same input + results. Picking a
     result navigates WITHOUT expanding the rail — search never mutates the
     collapse state. -->

<template>
  <!-- Expanded rail: inline input + self-drawn results panel. -->
  <div v-if="!collapsed" class="settings-menu-search">
    <el-input
      v-model="query"
      :aria-label="t('settings.search.placeholder')"
      :placeholder="t('settings.search.placeholder')"
      clearable
      size="default"
      @focus="focused = true"
      @blur="onBlur"
      @keydown.enter.prevent="goFirst"
      @keydown.esc="focused = false"
    >
      <template #prefix>
        <el-icon :size="16"><Search/></el-icon>
      </template>
    </el-input>
    <transition name="el-fade-in-linear">
      <ul v-if="focused && query.trim()" class="settings-menu-search__results" role="listbox">
        <li v-for="item in results" :key="item.name" role="option">
          <button class="settings-menu-search__option" type="button" @mousedown.prevent="go(item.name)">
            <span class="settings-menu-search__path">{{ item.path }}</span>
          </button>
        </li>
        <li v-if="results.length === 0" class="settings-menu-search__empty">
          {{ t('settings.search.empty') }}
        </li>
      </ul>
    </transition>
  </div>

  <!-- Collapsed rail: icon trigger + click popover (Element-managed open
       state — no manual visibility wiring needed). -->
  <el-popover v-else :width="248" placement="bottom-start" popper-class="settings-search-popover" trigger="click">
    <template #reference>
      <el-button :aria-label="t('settings.search.placeholder')" circle text type="default">
        <!-- 18px matches the collapse toggle and the menu icon scale. -->
        <el-icon :size="18"><Search/></el-icon>
      </el-button>
    </template>
    <div class="settings-menu-search__pop">
      <el-input
        v-model="query"
        :placeholder="t('settings.search.placeholder')"
        clearable
        size="default"
        @keydown.enter.prevent="goFirst"
      >
        <template #prefix>
          <el-icon :size="16"><Search/></el-icon>
        </template>
      </el-input>
      <ul class="settings-menu-search__results settings-menu-search__results--pop" role="listbox">
        <li v-for="item in results" :key="item.name" role="option">
          <button class="settings-menu-search__option" type="button" @click="go(item.name)">
            <span class="settings-menu-search__path">{{ item.path }}</span>
          </button>
        </li>
        <li v-if="query.trim() && results.length === 0" class="settings-menu-search__empty">
          {{ t('settings.search.empty') }}
        </li>
      </ul>
    </div>
  </el-popover>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';
import {Search} from '@element-plus/icons-vue';

import {getSettingsRouteName} from '@/config/settingsNav';

import {useSettingsSidebarItems} from '@/views/settings/composables/useSettingsSidebarItems';

defineProps({
  collapsed: {
    type: Boolean,
    default: false,
  },
});

const {t} = useI18n();
const router = useRouter();
const {sidebarItems} = useSettingsSidebarItems();

const query = ref('');
const focused = ref(false);

interface SearchRow {
  name: string;
  path: string;
}

// Flattened "Group / Leaf" rows; group containers jump to their first child
// via SETTINGS_ACTIVE_ALIAS. Case-insensitive substring on the whole path.
const results = computed<SearchRow[]>(() => {
  const q = query.value.trim().toLowerCase();
  if (!q) return [];
  const rows: SearchRow[] = [];
  for (const group of sidebarItems.value) {
    if (!group.children?.length) continue;
    for (const leaf of group.children) {
      const path = `${group.title} / ${leaf.title}`;
      if (path.toLowerCase().includes(q)) rows.push({name: leaf.name, path});
    }
  }
  return rows.slice(0, 12);
});

const go = (name: string) => {
  void router.push({name: getSettingsRouteName(name)});
  // Search never mutates the collapse state — the rail stays as it is.
  query.value = '';
  focused.value = false;
};

const goFirst = () => {
  if (results.value.length > 0) go(results.value[0]!.name);
};

const onBlur = () => {
  // Delay so a mousedown on an option can win over the blur.
  setTimeout(() => (focused.value = false), 120);
};
</script>

<style lang="scss" scoped>
.settings-menu-search {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
}

.settings-menu-search__results {
  position: absolute;
  top: calc(100% + var(--dc3-space-1));
  left: 0;
  z-index: 30;
  width: 240px;
  max-height: 300px;
  margin: 0;
  padding: var(--dc3-space-1);
  overflow-y: auto;
  list-style: none;
  background: var(--dc3-bg-elevated-strong);
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  box-shadow: var(--dc3-shadow-md);
}

.settings-menu-search__results--pop {
  position: static;
  width: auto;
  border: none;
  box-shadow: none;
  background: transparent;
}

.settings-menu-search__option {
  display: block;
  width: 100%;
  padding: var(--dc3-space-1) var(--dc3-space-2);
  border: none;
  border-radius: var(--dc3-radius-md);
  background: transparent;
  color: var(--dc3-text-regular);
  text-align: left;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: var(--dc3-bg-interactive);
    color: var(--el-color-primary);
    outline: none;
  }
}

.settings-menu-search__path {
  font-size: 13px;
  line-height: 20px;
}

.settings-menu-search__empty {
  padding: var(--dc3-space-2);
  color: var(--dc3-text-muted);
  font-size: 13px;
  text-align: center;
}

.settings-menu-search__pop {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-2);
}
</style>
