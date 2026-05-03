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
    <!-- Alert banner: pops up when the total unconfirmed count crosses the
         operator-attention threshold. Clicking it drills into the unified
         event list pre-filtered to "only unconfirmed". -->
    <el-alert
      v-if="unconfirmedTotal >= UNCONFIRMED_BANNER_THRESHOLD"
      class="event-overview__banner"
      type="error"
      effect="dark"
      show-icon
      :closable="false"
      @click="goToUnconfirmed"
    >
      <template #title>
        {{ t('settings.event.overview.bannerUnhandled', { n: unconfirmedTotal }) }}
      </template>
      <template #default>
        <span class="event-overview__banner-cta">{{ t('settings.event.overview.bannerCta') }}</span>
      </template>
    </el-alert>

    <!-- Quick-action chips: jump straight to common filter combinations so
         the operator doesn't have to compose them by hand from the event
         page's filter bar. -->
    <div class="event-overview__quick">
      <span class="event-overview__quick-label">{{ t('settings.event.overview.quickActions') }}</span>
      <el-button size="small" round :icon="Bell" @click="goToUnconfirmed">
        {{ t('settings.event.overview.quickUnconfirmed') }}
      </el-button>
      <el-button size="small" round :icon="Management" @click="goToSource('device')">
        {{ t('settings.event.overview.quickDevice') }}
      </el-button>
      <el-button size="small" round :icon="Promotion" @click="goToSource('driver')">
        {{ t('settings.event.overview.quickDriver') }}
      </el-button>
      <el-button size="small" round :icon="Clock" @click="goToLastHour">
        {{ t('settings.event.overview.quickLastHour') }}
      </el-button>
    </div>

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

    <!-- Diagnostic grid: heatmap (when) + type pie (what) + storm list
         (who) + recent unconfirmed (what now). Designed to answer "where
         do I look next" without leaving the page. -->
    <div class="event-overview__diagnostic">
      <alert-activity-heatmap class="event-overview__diag-heatmap" />
      <alert-type-pie class="event-overview__diag-pie" />
    </div>
    <div class="event-overview__diagnostic">
      <alert-storm-sources class="event-overview__diag-storm" />
      <recent-unconfirmed class="event-overview__diag-recent" />
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import type { Component } from 'vue';
  import { Bell, Clock, Management, Promotion, Warning, WarningFilled } from '@element-plus/icons-vue';

  import { alertPage, alertStats } from '@/api/dashboard';
  import StatCard from '@/components/card/stat/StatCard.vue';
  import EventTrendChart from './components/EventTrendChart.vue';
  import TopSourcesChart from './components/TopSourcesChart.vue';
  import RecentUnconfirmed from './components/RecentUnconfirmed.vue';
  import AlertActivityHeatmap from './components/AlertActivityHeatmap.vue';
  import AlertTypePie from './components/AlertTypePie.vue';
  import AlertStormSources from './components/AlertStormSources.vue';

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

  // When unconfirmed alarms exceed this count, a prominent red banner
  // appears at the top of the overview nudging the operator toward the
  // unconfirmed-only event list. Chosen empirically — under 100 the
  // operator typically keeps pace, over 100 they usually need a nudge.
  const UNCONFIRMED_BANNER_THRESHOLD = 100;

  const loading = ref(false);
  const state = reactive({
    deviceTotal: 0,
    deviceUnconfirmed: 0,
    driverTotal: 0,
    driverUnconfirmed: 0,
    sparkline: [] as number[],
  });

  const unconfirmedTotal = computed(() => state.deviceUnconfirmed + state.driverUnconfirmed);

  const fetchCount = async (source: 'device' | 'driver', confirmFlag: number | null) => {
    try {
      const res: { data?: { total?: number } } = await alertPage({ source, confirmFlag, current: 1, size: 1 });
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
      const statsData = (stats as { data?: { sparkline24h?: number[] } } | null)?.data;
      state.sparkline = statsData?.sparkline24h ?? [];
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
      onClick: () => router.push({ name: 'settingsDeviceEvent', query: { confirmFlag: '0' } }),
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
      onClick: () => router.push({ name: 'settingsDriverEvent', query: { confirmFlag: '0' } }),
      onRefresh: load,
    },
  ]);

  // Quick-action handlers. Each pre-applies a filter combination on the
  // target event-list route so the operator lands with the query already
  // narrowed instead of having to compose it from the filter bar.
  const goToUnconfirmed = () =>
    router.push({ name: 'settingsDeviceEvent', query: { confirmFlag: '0' } }).catch(() => {});
  const goToSource = (source: 'device' | 'driver') => {
    const name = source === 'device' ? 'settingsDeviceEvent' : 'settingsDriverEvent';
    router.push({ name }).catch(() => {});
  };
  const goToLastHour = () => router.push({ name: 'settingsDeviceEvent', query: { rangeKey: '24h' } }).catch(() => {});

  onMounted(load);
</script>

<style lang="scss" scoped>
  .event-overview {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .event-overview__banner {
      cursor: pointer;
    }

    .event-overview__banner-cta {
      text-decoration: underline;
    }

    .event-overview__quick {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: #fff;
      border-radius: 10px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    }

    .event-overview__quick-label {
      font-size: 13px;
      color: #909399;
      margin-right: 4px;
    }

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

    .event-overview__diagnostic {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;

      @media (max-width: 1024px) {
        grid-template-columns: 1fr;
      }
    }

    .event-overview__diag-heatmap,
    .event-overview__diag-pie,
    .event-overview__diag-storm,
    .event-overview__diag-recent {
      min-height: 320px;
    }
  }
</style>
