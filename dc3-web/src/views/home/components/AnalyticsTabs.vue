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
  <dashboard-card
    :empty="status === 'success' && !hasData"
    :empty-image-size="80"
    :empty-text="$t('home.tabs.empty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    body-mode="chart"
    class="analytics-tabs"
    variant="tabs"
    @refresh="load"
  >
    <template #title>
      <el-tabs v-model="activeTab" class="analytics-tabs__bar">
        <el-tab-pane v-for="t in tabs" :key="t.key" :label="t.label" :name="t.key"/>
      </el-tabs>
    </template>

    <template #tools>
      <range-segmented v-if="isTopTab" v-model="rangeKey" size="small"/>
    </template>

    <div class="analytics-tabs__chart-wrap">
      <div ref="chartRef" class="analytics-tabs__chart"></div>
    </div>

    <!-- Caption line — spells out the ranking rule and (for top-N tabs) the
         active time range, so the chart is never ambiguous about what it's
         measuring or how it's sorted. Lives in the footer's meta side per
         the family-wide "time left, explanation right" contract. -->
    <template #footer-meta>
      <span class="analytics-tabs__caption">{{ caption }}</span>
    </template>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {deviceStats, driverStats, statsTop} from '@/api/dashboard';
import {listDeviceByIds} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {listPointByIds} from '@/api/point';
import {listProfileByIds} from '@/api/profile';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/config/types/dashboard';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {observeChartSize} from '@/utils/g2ChartUtil';

type TabKey = 'deviceStatus' | 'protocol' | 'profile' | 'topDevice' | 'topPoint' | 'topDriver';
type Group = 'structural' | 'top';

const props = withDefaults(
  defineProps<{
    /** Which half of the analytics split this card hosts. */
    group?: Group;
  }>(),
  {group: 'structural'}
);

const {t, locale} = useI18n();

// Each group keeps its own driver → point → device → profile precedence
// so both halves read left-to-right the same way.
const STRUCTURAL_TABS: TabKey[] = ['protocol', 'deviceStatus', 'profile'];
const TOP_TABS: TabKey[] = ['topDriver', 'topPoint', 'topDevice'];

const tabs = computed<{ key: TabKey; label: string }[]>(() => {
  const keys = props.group === 'top' ? TOP_TABS : STRUCTURAL_TABS;
  return keys.map((k) => ({key: k, label: t(`home.tabs.${k}`)}));
});

const activeTab = ref<TabKey>(props.group === 'top' ? 'topDriver' : 'protocol');
const rangeKey = ref<RangeKey>('24h');
const {loading, run, status} = useAsyncLoader();
const chartRef = ref<HTMLElement>();
const chartData = ref<{key: string; count: number}[]>([]);
const hasData = computed(() => chartData.value.length > 0);

const isTopTab = computed(
  () => activeTab.value === 'topDevice' || activeTab.value === 'topPoint' || activeTab.value === 'topDriver'
);

const isTopTabKey = (tab: TabKey) => tab === 'topDevice' || tab === 'topPoint' || tab === 'topDriver';

// Caption text for the current tab — structural tabs get a fixed phrase,
// top-N tabs interpolate the active range so "{range} internal write volume"
// stays accurate when the user flips the segmented control.
const caption = computed(() => {
  switch (activeTab.value) {
    case 'deviceStatus':
      return t('home.tabs.captionDeviceStatus');
    case 'protocol':
      return t('home.tabs.captionProtocol');
    case 'profile':
      return t('home.tabs.captionProfile');
    default: {
      const rangeMap: Record<RangeKey, string> = {
        '': t('common.all'),
        today: t('common.ranges.today'),
        '24h': t('common.ranges.h24'),
        '7d': t('common.ranges.d7'),
        '30d': t('common.ranges.d30'),
      };
      return t('home.tabs.captionTopActive', {range: rangeMap[rangeKey.value]});
    }
  }
});

let chart: Chart | undefined;
let disposeFit: (() => void) | undefined;

const disposeChart = () => {
  disposeFit?.();
  disposeFit = undefined;
  chart?.destroy();
  chart = undefined;
};

const createChart = () => {
  const el = chartRef.value;
  if (!el) return;
  disposeChart();
  chart = new Chart({container: el, autoFit: true});
  disposeFit = observeChartSize(el, chart);
};

// Entity id → display name caches so top-N charts render labels, not ids.
type NameKind = 'device' | 'point' | 'driver' | 'profile';
const nameCache: Record<NameKind, Record<string, string>> = {
  device: {},
  point: {},
  driver: {},
  profile: {},
};
let namesGeneration = 0;

const resolveNames = async (kind: NameKind, ids: string[]) => {
  const cache = nameCache[kind];
  const generation = namesGeneration;
  const missing = ids.filter((id) => id && !cache[id]);
  if (missing.length === 0) return;
  try {
    let res: any;
    if (kind === 'device') res = await listDeviceByIds(missing);
    else if (kind === 'point') res = await listPointByIds(missing);
    else if (kind === 'driver') res = await listDriverByIds(missing);
    else res = await listProfileByIds(missing);
    const data = res || {};
    for (const id of missing) {
      const item = data[id];
      if (item && generation === namesGeneration) {
        cache[id] = item.deviceName || item.pointName || item.driverName || item.profileName || id;
      }
    }
  } catch {
    // handled globally
  }
};

// ---- renderers ----------------------------------------------------------
const renderPie = (data: { key: string; count: number }[]) => {
  createChart();
  if (!chart) return;
  chart.clear();
  chart
    .interval()
    .data(data)
    .transform({type: 'stackY'})
    .coordinate({type: 'theta', innerRadius: 0.6})
    .encode('y', 'count')
    .encode('color', 'key')
    .legend('color', {position: 'right'})
    .tooltip({title: (d: any) => d.key, items: [{field: 'count'}]})
    .label({text: 'key', position: 'outside', style: {fontSize: 11}});
  chart.render();
};

const renderBar = (data: { key: string; count: number }[]) => {
  createChart();
  if (!chart) return;
  chart.clear();
  chart
    .interval()
    .data(data)
    .encode('x', 'key')
    .encode('y', 'count')
    .encode('color', 'key')
    .legend(false)
    .axis('x', {title: false, labelAutoRotate: false})
    .axis('y', {title: false});
  chart.render();
};

const renderHorizontalBar = (data: { key: string; count: number }[]) => {
  createChart();
  if (!chart) return;
  chart.clear();
  chart
    .interval()
    .data(data)
    .encode('x', 'key')
    .encode('y', 'count')
    .encode('color', 'count')
    .coordinate({transform: [{type: 'transpose'}]})
    .legend(false)
    .axis('x', {title: false})
    .axis('y', {title: false});
  chart.render();
};

// ---- loaders ------------------------------------------------------------
type Bucket = {key: string; count: number};
type StructuralPayload = {
  driver?: {byService?: Array<{key: string; count: number}>};
  device?: {
    byEnable?: Array<{key: string; count: number}>;
    byProfile?: Array<{key: string; count: number}>;
  };
};

const loadStructural = async (tab: Extract<TabKey, 'deviceStatus' | 'protocol' | 'profile'>): Promise<Bucket[]> => {
  // Fetch both summaries once for parity with the existing endpoint contract,
  // but derive the result from the tab captured at request start. This keeps a
  // late response from being interpreted using a newer tab selection.
  const [driverResult, deviceResult] = (await Promise.all([driverStats(), deviceStats(10)])) as [StructuralPayload, any];
  const driverPayload = driverResult?.driver ?? (driverResult as any)?.data ?? driverResult ?? {};
  const devicePayload = deviceResult?.device ?? (deviceResult as any)?.data ?? deviceResult ?? {};

  if (tab === 'deviceStatus') {
    return (devicePayload.byEnable ?? []).map((bucket: {key: string; count: number}) => ({
      key: bucket.key === 'ENABLED' ? t('common.enable') : bucket.key === 'DISABLED' ? t('common.disable') : bucket.key,
      count: Number(bucket.count) || 0,
    }));
  }
  if (tab === 'protocol') {
    return (driverPayload.byService ?? []).map((bucket: {key: string; count: number}) => ({
      key: (bucket.key || '-').replace(/^dc3-driver-/, ''),
      count: Number(bucket.count) || 0,
    }));
  }

  const raw = (devicePayload.byProfile ?? []) as Array<{key: string; count: number}>;
  await resolveNames('profile', raw.map((bucket) => bucket.key).filter(Boolean));
  return raw.map((bucket) => ({
    key: nameCache.profile[bucket.key] || bucket.key,
    count: Number(bucket.count) || 0,
  }));
};

const loadTop = async (
  tab: Extract<TabKey, 'topDevice' | 'topPoint' | 'topDriver'>,
  range: RangeKey
): Promise<Bucket[]> => {
  const dimMap: Record<typeof tab, 'device' | 'point' | 'driver'> = {
    topDevice: 'device',
    topPoint: 'point',
    topDriver: 'driver',
  };
  const dim = dimMap[tab];
  const result = await statsTop({dimension: dim, rangeKey: range, limit: 10});
  const rows = (Array.isArray(result) ? result : []) as Array<{entityId: string | number; count: number}>;
  await resolveNames(dim, rows.map((row) => String(row.entityId)));
  return rows.map((row) => ({
    key: nameCache[dim][String(row.entityId)] || String(row.entityId),
    count: Number(row.count) || 0,
  }));
};

const renderForTab = (tab: TabKey, buckets: Bucket[]) => {
  if (tab === 'deviceStatus') renderPie(buckets);
  else if (tab === 'protocol') renderBar(buckets);
  else renderHorizontalBar(buckets);
};

const load = async () => {
  const tab = activeTab.value;
  const range = rangeKey.value;
  await run(
    () =>
      isTopTabKey(tab)
        ? loadTop(tab as Extract<TabKey, 'topDevice' | 'topPoint' | 'topDriver'>, range)
        : loadStructural(tab as Extract<TabKey, 'deviceStatus' | 'protocol' | 'profile'>),
    {apply: (buckets) => (chartData.value = buckets)}
  );
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (chartData.value.length > 0) renderForTab(tab, chartData.value);
  else disposeChart();
};

// Tab/range changes only need a reload; cached entity names stay valid
// and are invalidated solely when the locale (the display language) flips.
watch([activeTab, rangeKey], () => {
  load();
});

watch(locale, () => {
  namesGeneration += 1;
  for (const names of Object.values(nameCache)) {
    for (const id of Object.keys(names)) delete names[id];
  }
  load();
});

onMounted(load);

onUnmounted(() => disposeChart());
</script>

<style lang="scss" scoped>
.analytics-tabs {
  // AnalyticsTabs has its own body stack (caption + chart-wrap), each with
  // its own padding, so reset the chart-mode default padding to keep the
  // caption flush with the card edges like the original layout.
  :deep(.dashboard-card__content) {
    padding: 0;
  }

  .analytics-tabs__bar {
    flex: 1;
    min-width: 0;
  }

  // Caption lives in the footer's meta slot; the footer is a one-line
  // strip, so a long caption must ellipsize instead of wrapping.
  .analytics-tabs__caption {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .analytics-tabs__chart-wrap {
    flex: 1;
    min-height: 0;
    padding: 4px 16px 16px;
  }

  .analytics-tabs__chart {
    width: 100%;
    height: 100%;
  }
}
</style>
