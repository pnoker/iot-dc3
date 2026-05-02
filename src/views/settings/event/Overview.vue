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
        :sparkline="c.sparkline"
        :trend="c.trend"
        :on-refresh="c.onRefresh"
        @click="c.onClick"
      />
    </div>

    <div class="event-overview__charts">
      <event-trend-chart class="event-overview__chart-main" />
      <top-sources-chart class="event-overview__chart-side" />
    </div>

    <recent-unconfirmed />
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Component } from 'vue';
  import { Management, Promotion, Warning, WarningFilled } from '@element-plus/icons-vue';

  import { alertPage, alertStats } from '@/api/dashboard';
  import StatCard from '@/components/card/stat/StatCard.vue';
  import EventTrendChart from './components/EventTrendChart.vue';
  import TopSourcesChart from './components/TopSourcesChart.vue';
  import RecentUnconfirmed from './components/RecentUnconfirmed.vue';

  type Tone = 'blue' | 'purple' | 'orange' | 'red';

  interface Trend {
    direction: 'up' | 'down' | 'flat';
    label: string;
  }

  interface Card {
    key: string;
    title: string;
    value: number;
    subtitle: string;
    icon: Component;
    tone: Tone;
    sparkline: number[];
    trend: Trend | null;
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
    sparkline: [] as number[],
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
      const [dt, du, rt, ru, stats] = await Promise.all([
        fetchCount('device', null),
        fetchCount('device', 0),
        fetchCount('driver', null),
        fetchCount('driver', 0),
        alertStats().catch(() => null),
      ]);
      state.deviceTotal = dt;
      state.deviceUnconfirmed = du;
      state.driverTotal = rt;
      state.driverUnconfirmed = ru;
      state.sparkline = (stats as any)?.data?.sparkline24h ?? [];
    } finally {
      loading.value = false;
    }
  };

  const sparkTrend = (data: number[]): Trend | null => {
    if (data.length < 2) return null;
    const prev = data[data.length - 2] ?? 0;
    const curr = data[data.length - 1] ?? 0;
    if (prev === 0 && curr === 0) return null;
    const pct = prev === 0 ? 100 : Math.round(((curr - prev) / prev) * 100);
    if (pct > 0) return { direction: 'up', label: `+${pct}%` };
    if (pct < 0) return { direction: 'down', label: `${pct}%` };
    return { direction: 'flat', label: '0%' };
  };

  // Share the same 24-hour sparkline across all four cards so the row
  // lines up at the same height and the "floating total" cards don't look
  // visually hollow next to the "unconfirmed" ones.
  const unconfirmedSubtitle = (unconfirmed: number, total: number) => {
    if (total === 0) return '';
    const pct = Math.round((unconfirmed / total) * 100);
    return t('settings.event.overview.unconfirmedRatio', { unconfirmed, total, pct });
  };

  const cards = computed<Card[]>(() => [
    {
      key: 'device-total',
      title: t('settings.event.overview.deviceTotal'),
      value: state.deviceTotal,
      subtitle: t('settings.event.overview.goToDevice'),
      icon: Management,
      tone: 'blue',
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
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
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
      onRefresh: load,
    },
    {
      key: 'device-unconfirmed',
      title: t('settings.event.overview.deviceUnconfirmed'),
      value: state.deviceUnconfirmed,
      subtitle: unconfirmedSubtitle(state.deviceUnconfirmed, state.deviceTotal),
      icon: Warning,
      tone: 'orange',
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
      onClick: () => router.push({ name: 'settingsDeviceEvent' }),
      onRefresh: load,
    },
    {
      key: 'driver-unconfirmed',
      title: t('settings.event.overview.driverUnconfirmed'),
      value: state.driverUnconfirmed,
      subtitle: unconfirmedSubtitle(state.driverUnconfirmed, state.driverTotal),
      icon: WarningFilled,
      tone: 'red',
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
      onClick: () => router.push({ name: 'settingsDriverEvent' }),
      onRefresh: load,
    },
  ]);

  onMounted(load);
</script>

<style lang="scss" scoped>
  .event-overview {
    display: flex;
    flex-direction: column;
    gap: 12px;

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

    .event-overview__charts {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 12px;

      @media (max-width: 1024px) {
        grid-template-columns: 1fr;
      }
    }

    .event-overview__chart-main,
    .event-overview__chart-side {
      min-height: 340px;
    }
  }
</style>
