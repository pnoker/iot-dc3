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
    <!-- Quick-actions: device / driver split into a two-column
         el-descriptions so the two source lanes read side-by-side
         instead of stacking. Every chip carries real query params
         that EventTable picks up via route.query. -->
    <el-descriptions class="event-overview__quick" :column="2" border>
      <el-descriptions-item>
        <template #label>
          <span class="event-overview__quick-label">
            <el-icon><Management /></el-icon>
            {{ t('settings.event.device') }}
          </span>
        </template>
        <div class="event-overview__quick-actions">
          <el-button size="small" round @click="goto('device', {})">
            {{ t('settings.event.overview.quickAll') }}
          </el-button>
          <el-button size="small" round type="warning" plain @click="goto('device', { confirmFlag: '0' })">
            {{ t('settings.event.overview.quickUnconfirmed') }}
          </el-button>
          <el-button size="small" round plain @click="goto('device', { rangeKey: '24h' })">
            {{ t('settings.event.overview.quickLast24h') }}
          </el-button>
        </div>
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="event-overview__quick-label">
            <el-icon><Promotion /></el-icon>
            {{ t('settings.event.driver') }}
          </span>
        </template>
        <div class="event-overview__quick-actions">
          <el-button size="small" round @click="goto('driver', {})">
            {{ t('settings.event.overview.quickAll') }}
          </el-button>
          <el-button size="small" round type="warning" plain @click="goto('driver', { confirmFlag: '0' })">
            {{ t('settings.event.overview.quickUnconfirmed') }}
          </el-button>
          <el-button size="small" round plain @click="goto('driver', { rangeKey: '24h' })">
            {{ t('settings.event.overview.quickLast24h') }}
          </el-button>
        </div>
      </el-descriptions-item>
    </el-descriptions>

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
  import { Management, Promotion, Warning, WarningFilled } from '@element-plus/icons-vue';

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

  // Quick-action handler. Pre-applies a filter combination on the target
  // event-list route (settings/event/{device,driver}) so the operator
  // lands with the URL query already narrowed; EventTable reads
  // route.query on mount and re-syncs on subsequent changes.
  const goto = (source: 'device' | 'driver', query: Record<string, string>) => {
    const name = source === 'device' ? 'settingsDeviceEvent' : 'settingsDriverEvent';
    router.push({ name, query }).catch(() => {});
  };

  onMounted(load);
</script>

<style lang="scss" scoped>
  // Uniform 12px gap: vertical rhythm between sections, horizontal gap
  // inside every grid row. Bumping one value here should propagate to
  // the whole overview so the layout stays balanced.
  $overview-gap: 12px;

  .event-overview {
    display: flex;
    flex-direction: column;
    gap: $overview-gap;

    // el-descriptions already paints the border/background; just round
    // the frame so it matches the rest of the page's cards.
    .event-overview__quick {
      :deep(.el-descriptions__body) {
        border-radius: 10px;
        overflow: hidden;
      }
      :deep(.el-descriptions__label) {
        width: 96px;
      }
    }

    .event-overview__quick-label {
      font-size: 13px;
      color: #606266;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .event-overview__quick-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }

    .event-overview__cards {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: $overview-gap;

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
      gap: $overview-gap;

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
      gap: $overview-gap;

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
