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
    :empty-text="$t('home.activity.empty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('home.activity.title')"
    body-mode="chart"
    @refresh="load"
  >
    <template #tools>
      <range-segmented v-model="rangeKey" size="small"/>
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
import type {RangeKey} from '@/config/types/dashboard';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

const {t} = useI18n();
// A weekday/hour heatmap is most informative over a full week. The previous
// 24h default correctly populated only two day rows, which looked incomplete.
const rangeKey = ref<RangeKey>('7d');
const {error, loading, run, status} = useAsyncLoader();
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
const rows = ref<{dow: number; hour: number; count: number}[]>([]);
const hasData = computed(() => rows.value.length > 0);

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

const render = (rows: { dow: number; hour: number; count: number }[]) => {
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
  await run(() => statsActivity({rangeKey: rangeKey.value}), {
    apply: (res) => {
      const payload = Array.isArray(res) ? res : [];
      rows.value = payload.map((row) => ({
        dow: Number(row.dow),
        hour: Number(row.hour),
        count: Number(row.count) || 0,
      }));
    },
  });
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (hasData.value) render(rows.value);
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
.activity-heatmap__canvas {
  width: 100%;
  height: 100%;
}
</style>
