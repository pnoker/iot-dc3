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
  <dashboard-card :loading="loading" :title="$t('home.latency.title')" body-mode="chart" @refresh="load">
    <template #tools>
      <range-segmented v-model="rangeKey" size="small" @update:model-value="load"/>
    </template>
    <div ref="chartRef" class="latency-chart__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {statsLatency} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

const {t} = useI18n();
const rangeKey = ref<RangeKey>('24h');
const loading = ref(false);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;

// Bin labels line up with the 6-bucket layout in the backend SQL.
const binLabels = () => [
  t('home.latency.bin.under100ms'),
  t('home.latency.bin.100to500ms'),
  t('home.latency.bin.500msto1s'),
  t('home.latency.bin.1to5s'),
  t('home.latency.bin.5to30s'),
  t('home.latency.bin.over30s'),
];

const render = (rows: { bin: number; count: number }[]) => {
  if (!chartRef.value) return;
  chart?.destroy();
  chart = new Chart({container: chartRef.value, autoFit: true});
  const labels = binLabels();
  const data = rows.map((r) => ({label: labels[r.bin] || `bin-${r.bin}`, bin: r.bin, count: Number(r.count) || 0}));
  chart
    .interval()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'count')
    .encode('color', 'bin')
    .scale('color', {
      range: ['#67c23a', '#95d475', '#f0c14b', '#e6a23c', '#f56c6c', '#c45656'],
    })
    .legend(false)
    .axis({x: {title: false, labelAutoRotate: false}, y: {title: false}});
  chart.render();
};

const load = async () => {
  loading.value = true;
  try {
    const res: any = await statsLatency({rangeKey: rangeKey.value});
    const rows = (res?.data ?? []) as { bin: number; count: number }[];
    await nextTick();
    render(rows);
  } catch {
    // handled globally
  } finally {
    loading.value = false;
  }
};

onMounted(load);
watch(rangeKey, load);
onUnmounted(() => chart?.destroy());
</script>

<style lang="scss" scoped>
.latency-chart__canvas {
  width: 100%;
  height: 100%;
}
</style>
