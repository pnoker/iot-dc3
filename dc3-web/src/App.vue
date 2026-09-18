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
  <el-config-provider :locale="elementLocale" :size="appStore.componentSize">
    <div id="app">
      <router-view/>
    </div>
  </el-config-provider>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';
import {useI18n} from 'vue-i18n';

import {useAppStore} from '@/store';

const {locale} = useI18n();
const appStore = useAppStore();

const elementLocale = computed(() => {
  return locale.value === 'zh' ? zhCn : en;
});
</script>

<style lang="scss" scoped>
#app {
  width: 100%;
  height: 100%;
  // No min-width here: the shell adapts to any viewport
  // (docs/design/frontend-three-terminal-ux.md, A2).
}

#nprogress .bar {
  background: var(--el-color-primary) !important;
}
</style>
