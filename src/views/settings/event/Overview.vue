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
  <div class="event-overview">
    <div class="event-overview__cards">
      <stat-card
        v-for="c in cards"
        :key="c.key"
        :title="c.title"
        :value="c.value"
        :subtitle="c.subtitle"
        :icon="c.icon"
        :tone="c.tone"
        :on-refresh="c.onRefresh"
        @click="c.onClick"
      />
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
  import StatCard from '@/components/card/stat/StatCard.vue';

  type Tone = 'blue' | 'purple' | 'orange' | 'red';

  interface Card {
    key: string;
    title: string;
    value: number;
    subtitle: string;
    icon: Component;
    tone: Tone;
    onClick: () => void;
    onRefresh: () => Promise<void>;
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
      onRefresh: load,
    },
    {
      key: 'device-unconfirmed',
      title: t('settings.event.overview.deviceUnconfirmed'),
      value: state.deviceUnconfirmed,
      subtitle: '',
      icon: Warning,
      tone: 'orange',
      onClick: () => router.push({ name: 'settingsDeviceEvent' }),
      onRefresh: load,
    },
    {
      key: 'driver-total',
      title: t('settings.event.overview.driverTotal'),
      value: state.driverTotal,
      subtitle: t('settings.event.overview.goToDriver'),
      icon: Promotion,
      tone: 'purple',
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
      onRefresh: load,
    },
    {
      key: 'driver-unconfirmed',
      title: t('settings.event.overview.driverUnconfirmed'),
      value: state.driverUnconfirmed,
      subtitle: '',
      icon: WarningFilled,
      tone: 'red',
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
      onRefresh: load,
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
  }
</style>
