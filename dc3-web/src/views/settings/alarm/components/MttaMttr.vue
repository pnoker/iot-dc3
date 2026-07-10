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
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.mttaEmpty')"
    :loading="loading"
    :subtitle="subtitleText"
    :title="t('settings.event.overview.mttaTitle')"
    body-mode="chart"
    class="mtta-mttr"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small"/>
    </template>
    <div ref="chartRef" class="mtta-mttr__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {alertMtta} from '@/api/dashboard';
import type {MttaTrend} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {formatMs} from '@/utils/timeUtil';

const {t} = useI18n();
const {loading, run} = useAsyncLoader();

const daysOptions = [
  {label: '7d', value: '7'},
  {label: '30d', value: '30'},
  {label: '90d', value: '90'},
];
const daysKey = ref<string>('30');

const rows = ref<MttaTrend[]>([]);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;

const subtitleText = computed(() => {
  if (rows.value.length === 0) return '';
  const last = rows.value[rows.value.length - 1]!;
  return t('settings.event.overview.mttaSubtitle', {
    p50: formatMs(last.p50Ms),
    p95: formatMs(last.p95Ms),
  });
});

const render = () => {
  const el = chartRef.value;
  if (!el) return;
  chart?.destroy();
  chart = new Chart({container: el, autoFit: true});

  // Two-series line: p50 (green, operator-steady) and p95 (red, tail
  // latency). Y axis in seconds to keep numbers readable.
  const data: Array<{ date: string; series: string; seconds: number }> = [];
  for (const r of rows.value) {
    data.push({date: r.date, series: 'P50', seconds: r.p50Ms / 1000});
    data.push({date: r.date, series: 'P95', seconds: r.p95Ms / 1000});
  }
  chart
    .line()
    .data(data)
    .encode('x', 'date')
    .encode('y', 'seconds')
    .encode('color', 'series')
    .encode('shape', 'smooth')
    .scale('color', {range: ['#67c23a', '#f56c6c']})
    .style('lineWidth', 2)
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false}})
    .legend('color');
  chart.render();
};

const load = () =>
  run(async () => {
    const res: { data?: MttaTrend[] } = await alertMtta(Number(daysKey.value));
    rows.value = res?.data ?? [];
    await nextTick();
    if (rows.value.length > 0) render();
  });

watch(daysKey, load);
onMounted(load);
onUnmounted(() => chart?.destroy());

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.mtta-mttr__canvas {
  width: 100%;
  height: 100%;
}
</style>
