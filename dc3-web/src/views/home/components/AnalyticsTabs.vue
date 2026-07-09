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
    :empty="!loading && empty"
    :empty-image-size="80"
    :empty-text="$t('home.liveFeed.empty')"
    :loading="loading"
    body-mode="chart"
    class="analytics-tabs"
    variant="tabs"
    @refresh="load"
  >
    <template #title>
      <el-tabs v-model="activeTab" class="analytics-tabs__bar" @tab-change="onTabChange">
        <el-tab-pane v-for="t in tabs" :key="t.key" :label="t.label" :name="t.key"/>
      </el-tabs>
    </template>

    <template #tools>
      <range-segmented v-if="isTopTab" v-model="rangeKey" size="small" @update:model-value="load"/>
    </template>

    <!-- Caption line — spells out the ranking rule and (for top-N tabs) the
         active time range, so the chart is never ambiguous about what it's
         measuring or how it's sorted. -->
    <div class="analytics-tabs__caption">{{ caption }}</div>
    <div class="analytics-tabs__chart-wrap">
      <div ref="chartRef" class="analytics-tabs__chart"></div>
    </div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {deviceStats, driverStats, statsTop} from '@/api/dashboard';
import {listDeviceByIds} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {listPointByIds} from '@/api/point';
import {listProfileByIds} from '@/api/profile';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

type TabKey = 'deviceStatus' | 'protocol' | 'profile' | 'topDevice' | 'topPoint' | 'topDriver';
type Group = 'structural' | 'top';

const props = withDefaults(
  defineProps<{
    /** Which half of the analytics split this card hosts. */
    group?: Group;
  }>(),
  {group: 'structural'}
);

const {t} = useI18n();

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
const loading = ref(false);
const chartRef = ref<HTMLElement>();
const empty = ref(false);

const isTopTab = computed(
  () => activeTab.value === 'topDevice' || activeTab.value === 'topPoint' || activeTab.value === 'topDriver'
);

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

const onTabChange = () => load();

const disposeChart = () => {
  chart?.destroy();
  chart = undefined;
};

const ensureChart = () => {
  if (!chartRef.value) return;
  if (chart) return;
  chart = new Chart({container: chartRef.value, autoFit: true});
};

// Entity id → display name caches so top-N charts render labels, not ids.
type NameKind = 'device' | 'point' | 'driver' | 'profile';
const nameCache: Record<NameKind, Record<string, string>> = {
  device: {},
  point: {},
  driver: {},
  profile: {},
};

const resolveNames = async (kind: NameKind, ids: string[]) => {
  const cache = nameCache[kind];
  const missing = ids.filter((id) => id && !cache[id]);
  if (missing.length === 0) return;
  try {
    let res: any;
    if (kind === 'device') res = await listDeviceByIds(missing);
    else if (kind === 'point') res = await listPointByIds(missing);
    else if (kind === 'driver') res = await listDriverByIds(missing);
    else res = await listProfileByIds(missing);
    const data = res?.data || {};
    for (const id of missing) {
      const item = data[id];
      if (item) {
        cache[id] = item.deviceName || item.pointName || item.driverName || item.profileName || id;
      }
    }
  } catch {
    // handled globally
  }
};

// ---- renderers ----------------------------------------------------------
const renderPie = (data: { key: string; count: number }[]) => {
  ensureChart();
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
  ensureChart();
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
  ensureChart();
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
const loadDriverOrDevice = async () => {
  // deviceStatus / protocol / profile share the same two sources.
  const [drv, dev]: any = await Promise.all([driverStats(), deviceStats(10)]);
  const driverPayload = drv?.data || {byEnable: [], byType: [], byService: []};
  const devicePayload = dev?.data || {byEnable: [], byProfile: [], byDriver: []};

  if (activeTab.value === 'deviceStatus') {
    const buckets = (devicePayload.byEnable || []) as { key: string; count: number }[];
    empty.value = buckets.length === 0;
    await nextTick();
    if (!empty.value) renderPie(buckets);
    return;
  }
  if (activeTab.value === 'protocol') {
    // byService keys look like "dc3-driver-modbus-tcp" / "dc3-driver-mqtt";
    // strip the prefix so the chart axis reads "modbus-tcp / mqtt / opc-ua"
    // without the noise.
    const raw = (driverPayload.byService || []) as { key: string; count: number }[];
    const buckets = raw.map((b) => ({
      key: (b.key || '-').replace(/^dc3-driver-/, ''),
      count: b.count,
    }));
    empty.value = buckets.length === 0;
    await nextTick();
    if (!empty.value) renderBar(buckets);
    return;
  }
  if (activeTab.value === 'profile') {
    const raw = (devicePayload.byProfile || []) as { key: string; count: number }[];
    const ids = raw.map((b) => b.key).filter(Boolean);
    await resolveNames('profile', ids);
    const buckets = raw.map((b) => ({key: nameCache.profile[b.key] || b.key, count: b.count}));
    empty.value = buckets.length === 0;
    await nextTick();
    if (!empty.value) renderHorizontalBar(buckets);
  }
};

const loadTop = async () => {
  const dimMap: Record<Exclude<TabKey, 'deviceStatus' | 'protocol' | 'profile'>, 'device' | 'point' | 'driver'> = {
    topDevice: 'device',
    topPoint: 'point',
    topDriver: 'driver',
  };
  const dim = dimMap[activeTab.value as keyof typeof dimMap];
  const res: any = await statsTop({dimension: dim, rangeKey: rangeKey.value, limit: 10});
  const rows: { entityId: number; count: number }[] = res?.data || [];
  const ids = rows.map((r) => String(r.entityId));
  await resolveNames(dim, ids);
  const buckets = rows.map((r) => ({
    key: nameCache[dim][String(r.entityId)] || String(r.entityId),
    count: r.count,
  }));
  empty.value = buckets.length === 0;
  await nextTick();
  if (!empty.value) renderHorizontalBar(buckets);
};

const load = async () => {
  loading.value = true;
  try {
    if (isTopTab.value) {
      await loadTop();
    } else {
      await loadDriverOrDevice();
    }
  } catch {
    // handled globally
    empty.value = true;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  load();
});

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

  // Caption lives between the tab bar and the chart — tight vertical
  // rhythm (8px below tabs, 4px above chart) so it feels attached to the
  // tab rather than floating in the card body.
  .analytics-tabs__caption {
    font-size: 12px;
    color: #909399;
    padding: 10px 16px 4px;
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
