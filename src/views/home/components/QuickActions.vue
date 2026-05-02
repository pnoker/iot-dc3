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
  <el-card class="quick-actions" shadow="never">
    <template #header>
      <span class="quick-actions__title">{{ $t('home.quickActions.title') }}</span>
    </template>
    <div class="quick-actions__grid">
      <div
        v-for="a in actions"
        :key="a.key"
        class="quick-actions__item"
        :class="`quick-actions__item--${a.tone}`"
        @click="a.onClick"
      >
        <div class="quick-actions__icon">
          <el-icon :size="22"><component :is="a.icon" /></el-icon>
        </div>
        <div class="quick-actions__label">{{ a.label }}</div>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Component } from 'vue';
  import { Bell, List, Management, Promotion, Setting, Tickets } from '@element-plus/icons-vue';

  type Tone = 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'teal';

  interface Action {
    key: string;
    label: string;
    icon: Component;
    tone: Tone;
    onClick: () => void;
  }

  const { t } = useI18n();
  const router = useRouter();

  const actions = computed<Action[]>(() => [
    {
      key: 'driver',
      label: t('home.quickActions.driver'),
      icon: Promotion,
      tone: 'blue',
      onClick: () => router.push({ name: 'driver' }),
    },
    {
      key: 'device',
      label: t('home.quickActions.device'),
      icon: Management,
      tone: 'purple',
      onClick: () => router.push({ name: 'device' }),
    },
    {
      key: 'profile',
      label: t('home.quickActions.profile'),
      icon: Tickets,
      tone: 'orange',
      onClick: () => router.push({ name: 'profile' }),
    },
    {
      key: 'pointValue',
      label: t('home.quickActions.pointValue'),
      icon: List,
      tone: 'green',
      onClick: () => router.push({ name: 'pointValue' }),
    },
    {
      key: 'alert',
      label: t('home.quickActions.alert'),
      icon: Bell,
      tone: 'red',
      onClick: () => router.push({ name: 'driver' }),
    },
    {
      key: 'settings',
      label: t('home.quickActions.settings'),
      icon: Setting,
      tone: 'teal',
      onClick: () => router.push({ name: 'settingsUser' }),
    },
  ]);
</script>

<style lang="scss" scoped>
  .quick-actions {
    border-radius: 10px;
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 12px 16px 16px;
    }

    .quick-actions__title {
      font-weight: 600;
      color: #303133;
    }

    .quick-actions__grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
    }

    .quick-actions__item {
      cursor: pointer;
      border-radius: 8px;
      padding: 14px 8px;
      text-align: center;
      border: 1px solid var(--el-border-color-lighter);
      transition:
        transform 120ms ease,
        border-color 120ms ease,
        box-shadow 120ms ease;
      background: #ffffff;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
      }
    }

    .quick-actions__icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 10px;
      margin: 0 auto 6px;
    }

    .quick-actions__label {
      font-size: 13px;
      color: #606266;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    // tone palette
    .quick-actions__item--blue {
      .quick-actions__icon {
        background: rgba(64, 158, 255, 0.1);
        color: #409eff;
      }

      &:hover {
        border-color: #409eff;
      }
    }

    .quick-actions__item--purple {
      .quick-actions__icon {
        background: rgba(144, 89, 246, 0.1);
        color: #9059f6;
      }

      &:hover {
        border-color: #9059f6;
      }
    }

    .quick-actions__item--orange {
      .quick-actions__icon {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }

      &:hover {
        border-color: #e6a23c;
      }
    }

    .quick-actions__item--green {
      .quick-actions__icon {
        background: rgba(103, 194, 58, 0.1);
        color: #67c23a;
      }

      &:hover {
        border-color: #67c23a;
      }
    }

    .quick-actions__item--red {
      .quick-actions__icon {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }

      &:hover {
        border-color: #f56c6c;
      }
    }

    .quick-actions__item--teal {
      .quick-actions__icon {
        background: rgba(19, 194, 194, 0.1);
        color: #13c2c2;
      }

      &:hover {
        border-color: #13c2c2;
      }
    }
  }
</style>
