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
    :title="$t('settings.event.overview.topSourcesTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="top-sources__chart"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {Chart} from '@antv/g2';

import {alertTopSources} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useEntityNames} from '@/composables/useEntityNames';

const props = defineProps<{ days?: number; limit?: number }>();

const loading = ref(false);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
const {resolveBySource, nameBySource} = useEntityNames();

const render = (data: { name: string; count: number }[]) => {
  const el = chartRef.value;
  if (!el) return;
  chart?.destroy();
  chart = new Chart({container: el, autoFit: true});
  chart
    .interval()
    .data(data)
    .encode('x', 'name')
    .encode('y', 'count')
    .encode('color', 'name')
    .scale('x', {padding: 0.5})
    .axis({
      x: {title: false, labelAutoRotate: false},
      y: {title: false},
    })
    .legend(false)
    .tooltip({channel: 'y', valueFormatter: (d: number) => d.toLocaleString()})
    .coordinate({transform: [{type: 'transpose'}]});
  chart.render();
};

const load = async () => {
  loading.value = true;
  try {
    const res: any = await alertTopSources(props.days ?? 30, props.limit ?? 10);
    const rows: any[] = res?.data ?? [];

    await resolveBySource(rows);

    const chartData = rows.map((r: any) => ({
      name: nameBySource(r.source, r.sourceId),
      count: r.count ?? 0,
    }));
    await nextTick();
    render(chartData);
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
.top-sources__chart {
  width: 100%;
  height: 100%;
}
</style>
