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
  <el-container class="settings-container">
    <el-aside width="220px" class="settings-aside">
      <el-card class="settings-aside-card" shadow="never">
        <el-menu :default-active="activeMenu" @select="onSelect">
          <el-menu-item index="settingsUser">
            <el-icon><User /></el-icon>
            <span>{{ t('nav.settingsUser') }}</span>
          </el-menu-item>
          <el-menu-item index="settingsRole">
            <el-icon><Avatar /></el-icon>
            <span>{{ t('nav.settingsRole') }}</span>
          </el-menu-item>
          <el-menu-item index="settingsResource">
            <el-icon><Files /></el-icon>
            <span>{{ t('nav.settingsResource') }}</span>
          </el-menu-item>
          <el-menu-item index="settingsApi">
            <el-icon><Link /></el-icon>
            <span>{{ t('nav.settingsApi') }}</span>
          </el-menu-item>
        </el-menu>
      </el-card>
    </el-aside>
    <el-main class="settings-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';
  import { Avatar, Files, Link, User } from '@element-plus/icons-vue';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const activeMenu = computed(() => String(route.name || 'settingsUser'));

  const onSelect = (name: string) => {
    router.push({ name });
  };
</script>

<style lang="scss" scoped>
  .settings-container {
    align-items: stretch;
    gap: 4px;
    min-height: calc(100vh - 120px);
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
      }
    }

    :deep(.el-menu) {
      height: 100%;
      border-right: none;
    }

    :deep(.el-menu-item) {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .settings-main {
    padding: 0;
  }
</style>
