<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <el-config-provider :locale="elementLocale">
    <div id="app">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script lang="ts" setup>
  import {computed} from 'vue';
  import zhCn from 'element-plus/es/locale/lang/zh-cn';
  import en from 'element-plus/es/locale/lang/en';
  import {useI18n} from 'vue-i18n';

  const {locale} = useI18n();

  const elementLocale = computed(() => {
    return locale.value === 'zh' ? zhCn : en;
  });
</script>

<style lang="scss" scoped>
  #app {
    width: 100%;
    height: 100%;
    // Don't clip horizontally — body has min-width: 1280px in global.scss,
    // so when the viewport narrows we want a page-level scroll bar that
    // carries the header + body together instead of hiding the right edge.
  }

  #nprogress .bar {
    background: var(--el-color-primary) !important;
  }
</style>
