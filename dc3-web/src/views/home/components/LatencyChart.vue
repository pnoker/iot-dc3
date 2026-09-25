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
    :empty-text="$t('home.latency.empty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :footer-meta="$t('home.latency.footer')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('home.latency.title')"
    body-mode="chart"
    @refresh="load"
  >
    <template #tools>
      <range-segmented v-model="rangeKey" size="small"/>
    </template>
    <div ref="chartRef" class="latency-chart__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';

import {statsLatency} from '@/api/dashboard';
import {deviceLatency} from '@/api/dashboard/device';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/config/types/dashboard';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {mountG2Chart} from '@/utils/g2ChartUtil';

const {t} = useI18n();
const props = defineProps<{deviceId?: string}>();

const rangeKey = ref<RangeKey>('24h');
const {error, loading, run, status} = useAsyncLoader();
const chartRef = ref<HTMLElement>();
let disposeChart: (() => void) | undefined;
const rows = ref<{bin: number; count: number}[]>([]);
const hasData = computed(() => rows.value.length > 0);

// Bin labels line up with the 6-bucket layout in the backend SQL.
const binLabels = () => [
  t('home.latency.bin.under100ms'),
  t('home.latency.bin.100to500ms'),
  t('home.latency.bin.500msto1s'),
  t('home.latency.bin.1to5s'),
  t('home.latency.bin.5to30s'),
  t('home.latency.bin.over30s'),
];

const destroyChart = () => {
  disposeChart?.();
  disposeChart = undefined;
};

const render = (rows: { bin: number; count: number }[]) => {
  const el = chartRef.value;
  if (!el) return;
  destroyChart();
  const labels = binLabels();
  const data = rows.map((r) => ({label: labels[r.bin] || `bin-${r.bin}`, bin: r.bin, count: Number(r.count) || 0}));
  disposeChart = mountG2Chart(el, (chart) => {
    chart
      .interval()
      .data(data)
      .encode('x', 'label')
      .encode('y', 'count')
      .encode('color', 'bin')
      // Latency distributions are strongly right-skewed. A square-root scale
      // preserves a zero baseline while keeping rare slow buckets visible.
      .scale('y', {type: 'sqrt', zero: true, nice: true})
      .scale('color', {
        range: ['#67c23a', '#95d475', '#f0c14b', '#e6a23c', '#f56c6c', '#c45656'],
      })
      .legend(false)
      .axis({x: {title: false, labelAutoRotate: false}, y: {title: false}});
    chart.render();
  });
};

const load = async () => {
  await run(
    () =>
      props.deviceId
        ? deviceLatency(props.deviceId, {rangeKey: rangeKey.value})
        : statsLatency({rangeKey: rangeKey.value}),
    {
    apply: (res) => {
      const payload = Array.isArray(res) ? res : [];
      rows.value = payload.map((row) => ({bin: Number(row.bin), count: Number(row.count) || 0}));
    },
  });
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (hasData.value) render(rows.value);
  else destroyChart();
};

onMounted(load);
watch(rangeKey, load);
watch(() => props.deviceId, load);
watch(error, (value) => {
  if (value) destroyChart();
});
onUnmounted(destroyChart);
</script>

<style lang="scss" scoped>
.latency-chart__canvas {
  width: 100%;
  height: 100%;
}
</style>
