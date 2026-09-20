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
    :empty-text="$t('home.trendEmpty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :footer-meta="$t('home.trendFooter')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('home.trendTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <template #tools>
      <range-segmented v-model="rangeKey" size="small"/>
    </template>
    <div ref="chartRef" class="trend-chart__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {Chart} from '@antv/g2';

import {statsTimeseries} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {RangeKey} from '@/config/types/dashboard';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

const rangeKey = ref<RangeKey>('24h');
const {error, loading, run, status} = useAsyncLoader();
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
const points = ref<{bucket: string; count: number}[]>([]);
const hasData = computed(() => points.value.length > 0);

const ensureChart = () => {
  if (!chartRef.value) return;
  chart?.destroy();
  chart = new Chart({container: chartRef.value, autoFit: true});
};

const render = (points: { bucket: string; count: number }[]) => {
  ensureChart();
  if (!chart) return;
  chart
    .area()
    .data(points)
    .encode('x', 'bucket')
    .encode('y', 'count')
    .encode('shape', 'smooth')
    .scale('y', {zero: true, nice: true})
    .style('fill', 'linear-gradient(-90deg, rgba(64,158,255,0.02) 0%, rgba(64,158,255,0.45) 100%)')
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false}})
    .animate('enter', {type: 'fadeIn', duration: 400});
  chart
    .line()
    .data(points)
    .encode('x', 'bucket')
    .encode('y', 'count')
    .encode('shape', 'smooth')
    .style('stroke', '#409eff')
    .style('lineWidth', 2)
    .axis(false)
    .legend(false);
  chart.render();
};

// Backend resolves rangeKey → from-timestamp itself (TimeRangeUtil);
// we still pass a granularity hint because hourly buckets over a 30-day
// span would be too dense to render. Short ranges (today / 24h) use
// hourly buckets, longer ones (7d / 30d) use daily buckets.
const granularityFor = (key: RangeKey): 'hour' | 'day' => {
  if (key === 'today' || key === '24h' || key === '') return 'hour';
  return 'day';
};

const load = async () => {
  const requestRange = rangeKey.value;
  await run(
    () =>
      statsTimeseries({
        granularity: granularityFor(requestRange),
        rangeKey: requestRange,
      }),
    {
      apply: (res) => {
        const payload = Array.isArray(res) ? res : [];
        points.value = payload.map((point) => ({bucket: String(point.bucket), count: Number(point.count) || 0}));
      },
    }
  );
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (hasData.value) render(points.value);
  else {
    chart?.destroy();
    chart = undefined;
  }
};

onMounted(load);
watch(rangeKey, load);
watch(error, (value) => {
  if (value) {
    chart?.destroy();
    chart = undefined;
  }
});
onUnmounted(() => {
  chart?.destroy();
  chart = undefined;
});
</script>

<style lang="scss" scoped>
.trend-chart__canvas {
  width: 100%;
  height: 100%;
}
</style>
