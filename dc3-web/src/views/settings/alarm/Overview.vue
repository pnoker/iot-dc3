<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <div class="event-overview">
    <div class="auto-refresh-bar">
      <span class="auto-refresh-bar__label">{{ $t('common.autoRefresh') }} (30s)</span>
      <span class="auto-refresh-bar__time">{{ $t('common.lastRefreshTime') }}: {{ lastRefreshText }}</span>
    </div>
    <el-tabs v-model="activeTab" class="event-overview__tabs" @tab-change="onTabChange">
      <el-tab-pane :label="t('settings.event.overview.tabSituation')" name="situation">
        <blank-card>
          <el-descriptions :column="3" border class="event-overview__quick">
            <el-descriptions-item>
              <template #label>
                <span class="event-overview__quick-label">
                  <el-icon><Management/></el-icon>
                  {{ t('settings.event.device') }}
                </span>
              </template>
              <div class="event-overview__quick-actions">
                <el-button round size="small" @click="goto('device', {})">
                  {{ t('settings.event.overview.quickAll') }}
                </el-button>
                <el-button plain round size="small" type="warning" @click="goto('device', {confirmFlag: '0'})">
                  {{ t('settings.event.overview.quickUnconfirmed') }}
                </el-button>
                <span class="event-overview__quick-divider"/>
                <el-button plain round size="small" @click="goto('device', {rangeKey: 'today'})">
                  {{ t('settings.event.overview.quickToday') }}
                </el-button>
                <el-button plain round size="small" @click="goto('device', {rangeKey: '7d'})">
                  {{ t('settings.event.overview.quick7d') }}
                </el-button>
                <el-button plain round size="small" @click="goto('device', {rangeKey: '30d'})">
                  {{ t('settings.event.overview.quick30d') }}
                </el-button>
              </div>
            </el-descriptions-item>
            <el-descriptions-item>
              <template #label>
                <span class="event-overview__quick-label">
                  <el-icon><Promotion/></el-icon>
                  {{ t('settings.event.driver') }}
                </span>
              </template>
              <div class="event-overview__quick-actions">
                <el-button round size="small" @click="goto('driver', {})">
                  {{ t('settings.event.overview.quickAll') }}
                </el-button>
                <el-button plain round size="small" type="warning" @click="goto('driver', {confirmFlag: '0'})">
                  {{ t('settings.event.overview.quickUnconfirmed') }}
                </el-button>
                <span class="event-overview__quick-divider"/>
                <el-button plain round size="small" @click="goto('driver', {rangeKey: 'today'})">
                  {{ t('settings.event.overview.quickToday') }}
                </el-button>
                <el-button plain round size="small" @click="goto('driver', {rangeKey: '7d'})">
                  {{ t('settings.event.overview.quick7d') }}
                </el-button>
                <el-button plain round size="small" @click="goto('driver', {rangeKey: '30d'})">
                  {{ t('settings.event.overview.quick30d') }}
                </el-button>
              </div>
            </el-descriptions-item>
            <el-descriptions-item>
              <template #label>
                <span class="event-overview__quick-label">
                  <el-icon><CircleCheck/></el-icon>
                  {{ t('settings.event.sourcePoint') }}
                </span>
              </template>
              <div class="event-overview__quick-actions">
                <el-button round size="small" @click="goto('point', {})">
                  {{ t('settings.event.overview.quickAll') }}
                </el-button>
                <el-button plain round size="small" type="warning" @click="goto('point', {confirmFlag: '0'})">
                  {{ t('settings.event.overview.quickUnconfirmed') }}
                </el-button>
                <span class="event-overview__quick-divider"/>
                <el-button plain round size="small" @click="goto('point', {rangeKey: 'today'})">
                  {{ t('settings.event.overview.quickToday') }}
                </el-button>
                <el-button plain round size="small" @click="goto('point', {rangeKey: '7d'})">
                  {{ t('settings.event.overview.quick7d') }}
                </el-button>
                <el-button plain round size="small" @click="goto('point', {rangeKey: '30d'})">
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
            :icon="c.icon"
            :on-refresh="c.onRefresh"
            :sparkline="c.sparkline"
            :subtitle="c.subtitle"
            :title="c.title"
            :tone="c.tone"
            :trend="c.trend"
            :value="c.value"
            @click="c.onClick"
          />
        </div>

        <div class="event-overview__charts">
          <event-trend-chart class="event-overview__chart-main"/>
          <top-sources-chart class="event-overview__chart-side"/>
        </div>

        <!-- Diagnostic grid: heatmap (when) + type pie (what) + storm list
         (who) + recent unconfirmed (what now). Designed to answer "where
         do I look next" without leaving the page. -->
        <div class="event-overview__diagnostic">
          <alert-activity-heatmap class="event-overview__diag-heatmap"/>
          <alert-type-pie class="event-overview__diag-pie"/>
        </div>
        <div class="event-overview__diagnostic">
          <alert-storm-sources class="event-overview__diag-storm"/>
          <recent-unconfirmed class="event-overview__diag-recent"/>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('settings.event.overview.tabNoise')" lazy name="noise">
        <div class="event-overview__grid-2">
          <flapping-sources/>
          <peer-deviation/>
        </div>
        <div class="event-overview__grid-1">
          <alert-correlation/>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('settings.event.overview.tabAvailability')" lazy name="availability">
        <div class="event-overview__grid-2">
          <silent-sources/>
          <point-coverage-gap/>
        </div>
        <div class="event-overview__grid-1">
          <protocol-health/>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('settings.event.overview.tabSla')" lazy name="sla">
        <div class="event-overview__grid-2">
          <aging-backlog/>
          <mtta-mttr/>
        </div>
        <div class="event-overview__grid-1">
          <change-impact/>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import type {Component} from 'vue';
import {computed, defineAsyncComponent, onBeforeUnmount, onMounted, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute, useRouter} from 'vue-router';
import {Bell, CircleCheck, Management, Promotion, Warning, WarningFilled} from '@element-plus/icons-vue';

import {alertPage, alertStats, alertTrend} from '@/api/dashboard';
import blankCard from '@/components/card/blank/BlankCard.vue';
import StatCard from '@/components/card/stat/StatCard.vue';
import EventTrendChart from './components/EventTrendChart.vue';
import TopSourcesChart from './components/TopSourcesChart.vue';
import RecentUnconfirmed from './components/RecentUnconfirmed.vue';
import AlertActivityHeatmap from './components/AlertActivityHeatmap.vue';
import AlertTypePie from './components/AlertTypePie.vue';
import AlertStormSources from './components/AlertStormSources.vue';

const FlappingSources = defineAsyncComponent(() => import('./components/FlappingSources.vue'));
const PeerDeviation = defineAsyncComponent(() => import('./components/PeerDeviation.vue'));
const AlertCorrelation = defineAsyncComponent(() => import('./components/AlertCorrelation.vue'));
const SilentSources = defineAsyncComponent(() => import('./components/SilentSources.vue'));
const PointCoverageGap = defineAsyncComponent(() => import('./components/PointCoverageGap.vue'));
const ProtocolHealth = defineAsyncComponent(() => import('./components/ProtocolHealth.vue'));
const AgingBacklog = defineAsyncComponent(() => import('./components/AgingBacklog.vue'));
const MttaMttr = defineAsyncComponent(() => import('./components/MttaMttr.vue'));
const ChangeImpact = defineAsyncComponent(() => import('./components/ChangeImpact.vue'));

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

const {t} = useI18n();
const router = useRouter();
const route = useRoute();

// Tab state — synced to URL query so direct links to "?tab=sla" land on
// the SLA sub-board. Valid names: situation (default) | noise | availability | sla.
const VALID_TABS = ['situation', 'noise', 'availability', 'sla'] as const;
type TabName = (typeof VALID_TABS)[number];
const initialTab = (VALID_TABS as readonly string[]).includes(String(route.query.tab))
  ? (route.query.tab as TabName)
  : 'situation';
const activeTab = ref<TabName>(initialTab);

const onTabChange = (name: string | number) => {
  router.replace({query: {...route.query, tab: String(name)}}).catch(() => {
  });
};

const loading = ref(false);
const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);
const lastRefreshTime = ref<number>(Date.now());
const AUTO_REFRESH_INTERVAL = 30000;

const lastRefreshText = computed(() => {
  const d = new Date(lastRefreshTime.value);
  return d.toLocaleTimeString();
});

const state = reactive({
  deviceTotal: 0,
  deviceUnconfirmed: 0,
  driverTotal: 0,
  driverUnconfirmed: 0,
  // Today counts come from alertStats (backend already exposes them for
  // Home's today-alert cards — no extra round-trip needed here).
  todayDevice: 0,
  todayDriver: 0,
  todayDeviceUnconfirmed: 0,
  todayDriverUnconfirmed: 0,
  // 24h hourly bucket (overall) — used by the Today cards since "today"
  // is inherently about the current day's hourly activity.
  sparkline24h: [] as number[],
  // 7-day daily buckets split by source. Feeds each non-today card's own
  // sparkline + trend, so driver-total and device-total don't end up
  // with identical chart shapes. Mirrors how Home uses driverSparkline /
  // deviceSparkline from dailyGrowth.
  driverDaily: [] as number[],
  deviceDaily: [] as number[],
});

const fetchCount = async (source: 'point' | 'device' | 'driver', confirmFlag: number | null) => {
  try {
    const res: { data?: { total?: number } } = await alertPage({source, confirmFlag, current: 1, size: 1});
    return Number(res?.data?.total ?? 0);
  } catch {
    return 0;
  }
};

const load = async () => {
  loading.value = true;
  try {
    const [dt, du, rt, ru, stats, trend] = await Promise.all([
      fetchCount('device', null),
      fetchCount('device', 0),
      fetchCount('driver', null),
      fetchCount('driver', 0),
      alertStats().catch(() => null),
      alertTrend(7).catch(() => null),
    ]);
    state.deviceTotal = dt;
    state.deviceUnconfirmed = du;
    state.driverTotal = rt;
    state.driverUnconfirmed = ru;
    const statsData = (
      stats as {
        data?: {
          sparkline24h?: number[];
          todayDeviceAlarms?: number;
          todayDriverAlarms?: number;
          todayDeviceUnconfirmed?: number;
          todayDriverUnconfirmed?: number;
        };
      } | null
    )?.data;
    state.sparkline24h = statsData?.sparkline24h ?? [];
    state.todayDevice = Number(statsData?.todayDeviceAlarms ?? 0);
    state.todayDriver = Number(statsData?.todayDriverAlarms ?? 0);
    state.todayDeviceUnconfirmed = Number(statsData?.todayDeviceUnconfirmed ?? 0);
    state.todayDriverUnconfirmed = Number(statsData?.todayDriverUnconfirmed ?? 0);

    const trendRows =
      ((trend as { data?: Array<{ deviceCount?: number; driverCount?: number }> } | null)?.data as Array<{
        deviceCount?: number;
        driverCount?: number;
      }>) || [];
    state.driverDaily = trendRows.map((r) => Number(r.driverCount || 0));
    state.deviceDaily = trendRows.map((r) => Number(r.deviceCount || 0));
  } finally {
    loading.value = false;
    lastRefreshTime.value = Date.now();
  }
};

// Diff-style trend (latest vs previous bucket) matching the Home page's
// stat cards. The earlier percentage version returned null whenever the
// previous bucket was 0 which, combined with quiet event streams, left
// most cards with no trend indicator at all.
const sparkTrend = (data: number[]): Trend | null => {
  if (data.length < 2) return null;
  const prev = data[data.length - 2] ?? 0;
  const curr = data[data.length - 1] ?? 0;
  const diff = curr - prev;
  if (diff > 0) return {direction: 'up', label: `+${diff}`};
  if (diff < 0) return {direction: 'down', label: `${diff}`};
  return {direction: 'flat', label: '0'};
};

// Subtitle builders — every card gets a second data point so the primary
// metric never stands alone. totalSubtitle supplies "how many open" on the
// total cards; unconfirmedSubtitle supplies "how large is the backlog vs
// cumulative total" on the unconfirmed cards; todaySubtitle supplies
// today's unconfirmed count on the today cards.
const totalSubtitle = (unconfirmed: number, total: number) => {
  if (total === 0) return '';
  const pct = Math.round((unconfirmed / total) * 100);
  return t('settings.event.overview.subtitleTotalUnconfirmed', {unconfirmed, total, pct});
};
const unconfirmedSubtitle = (unconfirmed: number, total: number) => {
  if (total === 0) return '';
  const pct = Math.round((unconfirmed / total) * 100);
  return t('settings.event.overview.subtitleUnconfirmedOfTotal', {total, pct});
};
const todaySubtitle = (unconfirmed: number) => t('settings.event.overview.subtitleTodayUnconfirmed', {unconfirmed});

// Card order chosen deliberately: driver lane first (the one tenants run
// diagnostics off most), then device, split into totals → unconfirmed →
// today across two logical rows rendered by the CSS grid.
const cards = computed<Card[]>(() => [
  // Driver cards share the 7-day driver daily series; device cards share
  // the device daily series. Today cards use the 24h hourly bucket since
  // "today" is an intra-day metric. Each card therefore shows its own
  // shape + its own day-over-day diff, matching Home's per-card trend.
  {
    key: 'driver-total',
    title: t('settings.event.overview.driverTotal'),
    value: state.driverTotal,
    subtitle: totalSubtitle(state.driverUnconfirmed, state.driverTotal),
    icon: Promotion,
    tone: 'purple',
    sparkline: state.driverDaily,
    trend: sparkTrend(state.driverDaily),
    onClick: () => router.push({name: 'settingsDriverAlarm'}),
    onRefresh: load,
  },
  {
    key: 'device-total',
    title: t('settings.event.overview.deviceTotal'),
    value: state.deviceTotal,
    subtitle: totalSubtitle(state.deviceUnconfirmed, state.deviceTotal),
    icon: Management,
    tone: 'blue',
    sparkline: state.deviceDaily,
    trend: sparkTrend(state.deviceDaily),
    onClick: () => router.push({name: 'settingsDeviceAlarm'}),
    onRefresh: load,
  },
  {
    key: 'driver-unconfirmed',
    title: t('settings.event.overview.driverUnconfirmed'),
    value: state.driverUnconfirmed,
    subtitle: unconfirmedSubtitle(state.driverUnconfirmed, state.driverTotal),
    icon: WarningFilled,
    tone: 'red',
    sparkline: state.driverDaily,
    trend: sparkTrend(state.driverDaily),
    onClick: () => router.push({name: 'settingsDriverAlarm', query: {confirmFlag: '0'}}),
    onRefresh: load,
  },
  {
    key: 'device-unconfirmed',
    title: t('settings.event.overview.deviceUnconfirmed'),
    value: state.deviceUnconfirmed,
    subtitle: unconfirmedSubtitle(state.deviceUnconfirmed, state.deviceTotal),
    icon: Warning,
    tone: 'orange',
    sparkline: state.deviceDaily,
    trend: sparkTrend(state.deviceDaily),
    onClick: () => router.push({name: 'settingsDeviceAlarm', query: {confirmFlag: '0'}}),
    onRefresh: load,
  },
  {
    key: 'today-driver',
    title: t('settings.event.overview.todayDriver'),
    value: state.todayDriver,
    subtitle: todaySubtitle(state.todayDriverUnconfirmed),
    icon: Bell,
    tone: 'purple',
    sparkline: state.sparkline24h,
    trend: sparkTrend(state.sparkline24h),
    onClick: () => router.push({name: 'settingsDriverAlarm', query: {rangeKey: 'today'}}),
    onRefresh: load,
  },
  {
    key: 'today-device',
    title: t('settings.event.overview.todayDevice'),
    value: state.todayDevice,
    subtitle: todaySubtitle(state.todayDeviceUnconfirmed),
    icon: Bell,
    tone: 'blue',
    sparkline: state.sparkline24h,
    trend: sparkTrend(state.sparkline24h),
    onClick: () => router.push({name: 'settingsDeviceAlarm', query: {rangeKey: 'today'}}),
    onRefresh: load,
  },
]);

// Quick-action handler. Pre-applies a filter combination on the target
// event-list route (settings/event/{device,driver}) so the operator
// lands with the URL query already narrowed; EventTable reads
// route.query on mount and re-syncs on subsequent changes.
const goto = (source: 'point' | 'device' | 'driver', query: Record<string, string>) => {
  let name: string;
  if (source === 'point') name = 'settingsPointAlarm';
  else if (source === 'driver') name = 'settingsDriverAlarm';
  else name = 'settingsDeviceAlarm';
  router.push({name, query}).catch(() => {
  });
};

onMounted(() => {
  load();
  autoRefreshTimer.value = setInterval(() => {
    if (!loading.value) {
      load();
    }
  }, AUTO_REFRESH_INTERVAL);
});

onBeforeUnmount(() => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value);
    autoRefreshTimer.value = null;
  }
});
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

  // Tabs wrap the whole page — each pane renders its own flex-column
  // stack. el-tabs renders flat by default (expects to live inside a
  // card); since this page is a board with the tabs at the top level,
  // give the header its own surface so it reads as a proper tab bar
  // instead of floating on the page background.
  .event-overview__tabs {
    :deep(.el-tabs__header) {
      margin: 0 0 $overview-gap 0;
      padding: 0 12px;
      background: var(--el-bg-color);
      border-radius: 4px;
    }

    :deep(.el-tabs__nav-wrap::after) {
      // Element draws a 1px full-width bottom line under the nav strip;
      // inside a pilled card it reads as a visual double-border with
      // the card edge. Drop it — the active-item underline is enough.
      display: none;
    }

    :deep(.el-tab-pane) {
      display: flex;
      flex-direction: column;
      gap: $overview-gap;
    }
  }

  // 2-col / 1-col helper grids used inside each non-situation tab. Same
  // 8px gap as the rest of the page.
  .event-overview__grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $overview-gap;
    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }

  .event-overview__grid-1 {
    display: grid;
    grid-template-columns: 1fr;
    gap: $overview-gap;
  }

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

  // Chart cards own their own min-height (440, shared family baseline);
  // the grid item only needs to stretch to row height.

  .event-overview__diagnostic {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $overview-gap;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }

  // Diagnostic cards also set their own 440 min-height internally.

  .auto-refresh-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 4px 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
    border-radius: 4px;

    &__label {
      font-weight: 500;
    }

    &__time {
      color: var(--el-text-color-placeholder);
    }
  }
}
</style>
