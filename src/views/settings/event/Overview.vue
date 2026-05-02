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
  <div v-loading="loading" class="event-overview">
    <div class="event-overview__cards">
      <el-card v-for="c in cards" :key="c.key" shadow="never" class="event-overview__card" @click="c.onClick">
        <div class="event-overview__card-row">
          <div :class="['event-overview__icon', `event-overview__icon--${c.tone}`]">
            <el-icon :size="24"><component :is="c.icon" /></el-icon>
          </div>
          <div class="event-overview__body">
            <div class="event-overview__title">{{ c.title }}</div>
            <div class="event-overview__value">{{ c.value.toLocaleString() }}</div>
            <div v-if="c.subtitle" class="event-overview__subtitle">{{ c.subtitle }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Component } from 'vue';
  import { Management, Promotion, Warning, WarningFilled } from '@element-plus/icons-vue';

  import { alertPage } from '@/api/dashboard';

  type Tone = 'blue' | 'purple' | 'orange' | 'red';

  interface Card {
    key: string;
    title: string;
    value: number;
    subtitle: string;
    icon: Component;
    tone: Tone;
    onClick: () => void;
  }

  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const state = reactive({
    deviceTotal: 0,
    deviceUnconfirmed: 0,
    driverTotal: 0,
    driverUnconfirmed: 0,
  });

  const fetchCount = async (source: 'device' | 'driver', confirmFlag: number | null) => {
    try {
      const res: any = await alertPage({ source, confirmFlag, current: 1, size: 1 });
      return Number(res?.data?.total ?? 0);
    } catch {
      return 0;
    }
  };

  const load = async () => {
    loading.value = true;
    try {
      const [dt, du, rt, ru] = await Promise.all([
        fetchCount('device', null),
        fetchCount('device', 0),
        fetchCount('driver', null),
        fetchCount('driver', 0),
      ]);
      state.deviceTotal = dt;
      state.deviceUnconfirmed = du;
      state.driverTotal = rt;
      state.driverUnconfirmed = ru;
    } finally {
      loading.value = false;
    }
  };

  const cards = computed<Card[]>(() => [
    {
      key: 'device-total',
      title: t('settings.event.overview.deviceTotal'),
      value: state.deviceTotal,
      subtitle: t('settings.event.overview.goToDevice'),
      icon: Management,
      tone: 'blue',
      onClick: () => router.push({ name: 'settingsDeviceEvent' }),
    },
    {
      key: 'device-unconfirmed',
      title: t('settings.event.overview.deviceUnconfirmed'),
      value: state.deviceUnconfirmed,
      subtitle: '',
      icon: Warning,
      tone: 'orange',
      onClick: () => router.push({ name: 'settingsDeviceEvent' }),
    },
    {
      key: 'driver-total',
      title: t('settings.event.overview.driverTotal'),
      value: state.driverTotal,
      subtitle: t('settings.event.overview.goToDriver'),
      icon: Promotion,
      tone: 'purple',
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
    },
    {
      key: 'driver-unconfirmed',
      title: t('settings.event.overview.driverUnconfirmed'),
      value: state.driverUnconfirmed,
      subtitle: '',
      icon: WarningFilled,
      tone: 'red',
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
    },
  ]);

  onMounted(load);
</script>

<style lang="scss" scoped>
  .event-overview {
    .event-overview__cards {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;

      @media (max-width: 1280px) {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }
    }

    .event-overview__card {
      border-radius: 10px;
      cursor: pointer;
      transition: transform 0.15s ease;

      &:hover {
        transform: translateY(-2px);
      }
    }

    .event-overview__card-row {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .event-overview__icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &--blue {
        background: rgba(64, 158, 255, 0.1);
        color: #409eff;
      }
      &--purple {
        background: rgba(144, 89, 246, 0.1);
        color: #9059f6;
      }
      &--orange {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }
      &--red {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }
    }

    .event-overview__title {
      font-size: 13px;
      color: #909399;
    }

    .event-overview__value {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      line-height: 1.3;
    }

    .event-overview__subtitle {
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
    }
  }
</style>
