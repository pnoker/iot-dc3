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

const props = defineProps<{ days?: number }>();

const loading = ref(false);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;

const render = (data: { date: string; source: string; count: number }[]) => {
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
    const flat: { date: string; source: string; count: number }[] = [];
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
