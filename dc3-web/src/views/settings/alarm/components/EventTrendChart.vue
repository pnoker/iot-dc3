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
  <dashboard-card
    :loading="loading"
    :title="$t('settings.event.overview.trendTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="event-trend__chart"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
  import {Chart} from '@antv/g2';

  import {alertTrend} from '@/api/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const props = defineProps<{days?: number}>();

  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  const render = (data: {date: string; source: string; count: number}[]) => {
    const el = chartRef.value;
    if (!el) return;
    chart?.destroy();
    chart = new Chart({container: el, autoFit: true});
    chart
      .line()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'count')
      .encode('color', 'source')
      .encode('shape', 'smooth')
      .style('lineWidth', 2)
      .axis({x: {title: false, labelAutoHide: true}, y: {title: false}})
      .legend('color')
      .tooltip({channel: 'y', valueFormatter: (d: number) => d.toLocaleString()});
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertTrend(props.days ?? 30);
      const rows: any[] = res?.data ?? [];
      const flat: {date: string; source: string; count: number}[] = [];
      for (const r of rows) {
        flat.push({date: r.date, source: 'Device', count: r.deviceCount ?? 0});
        flat.push({date: r.date, source: 'Driver', count: r.driverCount ?? 0});
        flat.push({date: r.date, source: 'Point', count: r.pointCount ?? 0});
      }
      await nextTick();
      render(flat);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(load);
  watch(() => props.days, load);
  onUnmounted(() => chart?.destroy());

  defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
  .event-trend__chart {
    width: 100%;
    height: 100%;
  }
</style>
