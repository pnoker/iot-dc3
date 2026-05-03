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
         instead of stacking. Wrapped in BlankCard (border:0) so the
         block sits flush under the breadcrumb like the rest of the
         dashboard, matching About's Platform card treatment. Every
         chip carries real query params that EventTable picks up via
         route.query. -->
    <blank-card>
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
            <span class="event-overview__quick-divider" />
            <el-button size="small" round plain @click="goto('device', { rangeKey: 'today' })">
              {{ t('settings.event.overview.quickToday') }}
            </el-button>
            <el-button size="small" round plain @click="goto('device', { rangeKey: '7d' })">
              {{ t('settings.event.overview.quick7d') }}
            </el-button>
            <el-button size="small" round plain @click="goto('device', { rangeKey: '30d' })">
              {{ t('settings.event.overview.quick30d') }}
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
            <span class="event-overview__quick-divider" />
            <el-button size="small" round plain @click="goto('driver', { rangeKey: 'today' })">
              {{ t('settings.event.overview.quickToday') }}
            </el-button>
            <el-button size="small" round plain @click="goto('driver', { rangeKey: '7d' })">
              {{ t('settings.event.overview.quick7d') }}
            </el-button>
            <el-button size="small" round plain @click="goto('driver', { rangeKey: '30d' })">
              {{ t('settings.event.overview.quick30d') }}
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </blank-card>

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
  import { Bell, Management, Promotion, Warning, WarningFilled } from '@element-plus/icons-vue';

  import { alertPage, alertStats } from '@/api/dashboard';
  import blankCard from '@/components/card/blank/BlankCard.vue';
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
    // Today counts come from alertStats (backend already exposes them for
    // Home's today-alert cards — no extra round-trip needed here).
    todayDevice: 0,
    todayDriver: 0,
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
      const statsData = (
        stats as {
          data?: { sparkline24h?: number[]; todayDeviceAlarms?: number; todayDriverAlarms?: number };
        } | null
      )?.data;
      state.sparkline = statsData?.sparkline24h ?? [];
      state.todayDevice = Number(statsData?.todayDeviceAlarms ?? 0);
      state.todayDriver = Number(statsData?.todayDriverAlarms ?? 0);
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

  // Card order chosen deliberately: driver lane first (the one tenants run
  // diagnostics off most), then device, split into totals → unconfirmed →
  // today across two logical rows rendered by the CSS grid.
  const cards = computed<Card[]>(() => [
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
      key: 'today-driver',
      title: t('settings.event.overview.todayDriver'),
      value: state.todayDriver,
      subtitle: '',
      icon: Bell,
      tone: 'purple',
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
      onClick: () => router.push({ name: 'settingsDriverEvent', query: { rangeKey: 'today' } }),
      onRefresh: load,
    },
    {
      key: 'today-device',
      title: t('settings.event.overview.todayDevice'),
      value: state.todayDevice,
      subtitle: '',
      icon: Bell,
      tone: 'blue',
      sparkline: state.sparkline,
      trend: sparkTrend(state.sparkline),
      onClick: () => router.push({ name: 'settingsDeviceEvent', query: { rangeKey: 'today' } }),
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
  // Uniform 8px gap: vertical rhythm between sections, horizontal gap
  // inside every grid row. Matches the home page's 8px rhythm — with
  // el-card's 4px radius, a 12px gap looked too breathy.
  $overview-gap: 8px;

  .event-overview {
    display: flex;
    flex-direction: column;
    gap: $overview-gap;
    // Dashboard-style page (like Home). Match the 4px left breathing
    // room that .settings-container's gap gives between aside and main
    // on the right, so the content reads symmetric like a board.
    // Other settings sub-pages (User / Role / Api / ...) are form/table
    // views and keep flush-right — only this overview needs the balance.
    padding-right: 4px;

    .event-overview__quick {
      // Pin label column width so the left and right action groups
      // line up. (BlankCard handles the outer border/radius now.)
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

    // Visual divider between the confirmation-state chips (All / Unconfirmed)
    // and the time-range chips (Today / 7d / 30d). Thin vertical line so the
    // two semantic groups read as groups, not one long string of buttons.
    .event-overview__quick-divider {
      width: 1px;
      align-self: stretch;
      background: var(--el-border-color-light);
      margin: 4px 2px;
    }

    .event-overview__cards {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: $overview-gap;

      @media (max-width: 1280px) {
        grid-template-columns: repeat(3, minmax(0, 1fr));
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
