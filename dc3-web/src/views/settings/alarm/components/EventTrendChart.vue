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
    :empty="status === 'success' && rows.length === 0"
    :empty-text="$t('settings.event.overview.trendEmpty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('settings.event.overview.trendTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="event-trend__chart"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {alertTrend} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {observeChartSize} from '@/utils/g2ChartUtil';

const props = defineProps<{ days?: number }>();
const {t, locale} = useI18n();

const {loading, run, status} = useAsyncLoader();
const rows = ref<{date: string; source: string; count: number}[]>([]);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
let disposeFit: (() => void) | undefined;

const destroyChart = () => {
  disposeFit?.();
  disposeFit = undefined;
  chart?.destroy();
  chart = undefined;
};

const render = (data: { date: string; source: string; count: number }[]) => {
  const el = chartRef.value;
  if (!el) return;
  destroyChart();
  chart = new Chart({container: el, autoFit: true});
  disposeFit = observeChartSize(el, chart);
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
  const days = props.days ?? 30;
  await run(() => alertTrend(days), {
    apply: (res) => {
      const flat: {date: string; source: string; count: number}[] = [];
      type TrendPayload = {
        date: string;
        source?: string;
        count?: number;
        deviceCount?: number;
        driverCount?: number;
        pointCount?: number;
      };
      for (const row of (Array.isArray(res) ? res : []) as TrendPayload[]) {
        if (row.source) {
          flat.push({date: row.date, source: row.source, count: Number(row.count) || 0});
          continue;
        }
        flat.push({date: row.date, source: t('settings.event.sourceDevice'), count: Number(row.deviceCount) || 0});
        flat.push({date: row.date, source: t('settings.event.sourceDriver'), count: Number(row.driverCount) || 0});
        flat.push({date: row.date, source: t('settings.event.sourcePoint'), count: Number(row.pointCount) || 0});
      }
      rows.value = flat;
    },
  });
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (rows.value.length > 0) render(rows.value);
  else destroyChart();
};

onMounted(load);
watch(() => props.days, load);
watch(locale, load);
onUnmounted(destroyChart);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.event-trend__chart {
  width: 100%;
  height: 100%;
}
</style>
