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
  <div class="settings-wrapper">
    <el-container class="settings-container">
      <el-aside width="220px" class="settings-aside">
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
        </el-menu>
      </el-aside>
      <el-main class="settings-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute, useRouter } from 'vue-router';
  import { Avatar, Files, User } from '@element-plus/icons-vue';

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const activeMenu = computed(() => String(route.name || 'settingsUser'));

  const onSelect = (name: string) => {
    router.push({ name });
  };
</script>

<style lang="scss" scoped>
  .settings-wrapper {
    padding: 16px;
  }

  .settings-container {
    min-height: calc(100vh - 160px);
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    overflow: hidden;
  }

  .settings-aside {
    background: #fafbfc;
    border-right: 1px solid var(--el-border-color-lighter);

    :deep(.el-menu) {
      border-right: none;
      background: transparent;
    }

    :deep(.el-menu-item) {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .settings-main {
    padding: 20px;
  }
</style>
