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
  <dashboard-card :loading="loading" :title="$t('home.activity.title')" body-mode="chart" @refresh="load">
    <template #tools>
      <range-segmented v-model="rangeKey" size="small" @update:model-value="load" />
    </template>
    <div ref="chartRef" class="activity-heatmap__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {Chart} from '@antv/g2';

  import {statsActivity} from '@/api/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
  import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

  const {t} = useI18n();
  const rangeKey = ref<RangeKey>('24h');
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  // Sunday-first to match Postgres EXTRACT(DOW) (0..6 = Sun..Sat).
  const dayLabels = computed(() => [
    t('home.activity.dow.sun'),
    t('home.activity.dow.mon'),
    t('home.activity.dow.tue'),
    t('home.activity.dow.wed'),
    t('home.activity.dow.thu'),
    t('home.activity.dow.fri'),
    t('home.activity.dow.sat'),
  ]);

  const render = (rows: {dow: number; hour: number; count: number}[]) => {
    if (!chartRef.value) return;
    chart?.destroy();
    chart = new Chart({container: chartRef.value, autoFit: true});

    const labels = dayLabels.value;
    const data = rows.map((r) => ({
      dow: labels[r.dow] || `d-${r.dow}`,
      hour: String(r.hour).padStart(2, '0'),
      count: Number(r.count) || 0,
    }));

    chart
      .cell()
      .data(data)
      .encode('x', 'hour')
      .encode('y', 'dow')
      .encode('color', 'count')
      .scale('color', {
        type: 'sequential',
        palette: 'blues',
      })
      .style({stroke: '#ffffff', lineWidth: 1, inset: 0.5})
      .axis({
        x: {title: false, labelAutoHide: false},
        y: {title: false},
      })
      .tooltip({
        title: (d: any) => `${d.dow} ${d.hour}:00`,
        items: [{field: 'count', name: t('home.activity.count')}],
      });
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await statsActivity({rangeKey: rangeKey.value});
      const rows = (res?.data ?? []) as {dow: number; hour: number; count: number}[];
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
  .activity-heatmap__canvas {
    width: 100%;
    height: 100%;
  }
</style>
