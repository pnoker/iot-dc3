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
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.typeDistributionEmpty')"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :loading="loading"
    :retry-text="t('common.retry')"
    :title="t('settings.event.overview.typeDistributionTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="alert-type-pie__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {alertTypeDistribution} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {AlertTypeRow} from '@/config/types/dashboard';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {observeChartSize} from '@/utils/g2ChartUtil';

const {t, locale} = useI18n();

const {loading, run, status} = useAsyncLoader();
const rows = ref<AlertTypeRow[]>([]);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
let disposeFit: (() => void) | undefined;

const destroyChart = () => {
  disposeFit?.();
  disposeFit = undefined;
  chart?.destroy();
  chart = undefined;
};

// Human-readable label per event-ext.type tag produced by the backend.
// Falls back to the raw type when the backend introduces a new tag we
// haven't localised yet — better "driver-xyz" than blank.
const labelFor = (type: string) => {
  const key = `settings.event.overview.types.${type.replace(/-/g, '_')}`;
  const translated = t(key);
  return translated && translated !== key ? translated : type;
};

const render = (data: AlertTypeRow[]) => {
  const el = chartRef.value;
  if (!el) return;
  destroyChart();
  chart = new Chart({container: el, autoFit: true});
  disposeFit = observeChartSize(el, chart);
  const mapped = data.map((r) => ({type: labelFor(r.type), count: Number(r.count) || 0}));
  chart
    .interval()
    .data(mapped)
    .transform({type: 'stackY'})
    .coordinate({type: 'theta', innerRadius: 0.6})
    .encode('y', 'count')
    .encode('color', 'type')
    .legend('color', {position: 'right'})
    .tooltip({title: (d: { type: string }) => d.type, items: [{field: 'count'}]});
  chart.render();
};

const load = async () => {
  await run(() => alertTypeDistribution(30), {
    apply: (res) => {
      rows.value = (Array.isArray(res) ? res : []).map((row) => ({
        type: String(row.type || '-'),
        count: Number(row.count) || 0,
      }));
    },
  });
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (rows.value.length > 0) render(rows.value);
  else destroyChart();
};

onMounted(load);
watch(locale, load);
onUnmounted(destroyChart);
defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.alert-type-pie__canvas {
  width: 100%;
  height: 100%;
}
</style>
