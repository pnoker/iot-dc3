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
  <dashboard-card :loading="loading" :title="$t('home.trendTitle')" body-mode="chart" @refresh="load">
    <template #tools>
      <range-segmented v-model="rangeKey" size="small" @update:model-value="load" />
    </template>
    <div ref="chartRef" class="trend-chart__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
  import {Chart} from '@antv/g2';

  import {statsTimeseries} from '@/api/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
  import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

  const rangeKey = ref<RangeKey>('24h');
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  const ensureChart = () => {
    if (!chartRef.value) return;
    chart?.destroy();
    chart = new Chart({container: chartRef.value, autoFit: true});
  };

  const render = (points: {bucket: string; count: number}[]) => {
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
    loading.value = true;
    try {
      const res: any = await statsTimeseries({
        granularity: granularityFor(rangeKey.value),
        rangeKey: rangeKey.value,
      });
      const points = (res?.data ?? []).map((p: any) => ({bucket: p.bucket, count: Number(p.count) || 0}));
      await nextTick();
      render(points);
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
  .trend-chart__canvas {
    width: 100%;
    height: 100%;
  }
</style>
