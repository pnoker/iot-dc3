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

<!-- Value profile of the point (值域画像): numeric value histogram beside
     the 24h typical-day curve. Two equal cards side by side; stacked on
     narrow screens. -->

<template>
  <el-row :gutter="8" class="value-profile">
    <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24">
      <dashboard-card
        :empty="status === 'success' && histogramRows.length === 0"
        :empty-text="$t('pointValue.dashboard.value.histogramEmpty')"
        :error="status === 'error'"
        :error-text="$t('common.loadFailed')"
        :footer-meta="$t('pointValue.dashboard.value.histogramFooter')"
        :height="300"
        :loading="loading"
        :retry-text="$t('common.retry')"
        :title="$t('pointValue.dashboard.value.histogramTitle')"
        body-mode="chart"
        @refresh="emit('refresh')"
      >
        <div ref="histogramChartRef" class="value-profile__canvas"></div>
      </dashboard-card>
    </el-col>

    <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24">
      <dashboard-card
        :empty="status === 'success' && typicalRows.length === 0"
        :empty-text="$t('pointValue.dashboard.value.typicalEmpty')"
        :error="status === 'error'"
        :error-text="$t('common.loadFailed')"
        :footer-meta="$t('pointValue.dashboard.value.typicalFooter')"
        :height="300"
        :loading="loading"
        :retry-text="$t('common.retry')"
        :title="$t('pointValue.dashboard.value.typicalTitle')"
        body-mode="chart"
        @refresh="emit('refresh')"
      >
        <div ref="typicalChartRef" class="value-profile__canvas"></div>
      </dashboard-card>
    </el-col>
  </el-row>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {Chart} from '@antv/g2';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {PointDashboardHourAverage, PointDashboardValueBin} from '@/config/types/dashboard';
import {chartPalette, compactNumber, formatValue} from './util';

const props = defineProps({
  /** Numeric value histogram bins of the dashboard payload. */
  histogram: {
    type: Array as PropType<PointDashboardValueBin[]>,
    default: () => [],
  },
  /** Typical-day hourly averages of the dashboard payload. */
  typicalDay: {
    type: Array as PropType<PointDashboardHourAverage[]>,
    default: () => [],
  },
  /** Unit suffix shown in the typical-day tooltip. */
  unit: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  status: {
    type: String as PropType<'idle' | 'loading' | 'success' | 'error'>,
    default: 'idle',
  },
});

const emit = defineEmits<{ (e: 'refresh'): void }>();

const histogramChartRef = ref<HTMLElement>();
const typicalChartRef = ref<HTMLElement>();
let histogramChart: Chart | undefined;
let typicalChart: Chart | undefined;

// "41.5 ~ 44" style bin labels; the open-ended top bin reads ">41".
const histogramRows = computed(() =>
  props.histogram.map((bin) => {
    const from = Number.isFinite(bin.from) ? Number(bin.from) : null;
    const to = Number.isFinite(bin.to) ? Number(bin.to) : null;
    return {
      label: from == null ? '--' : to == null ? `>${formatValue(from)}` : `${formatValue(from)} ~ ${formatValue(to)}`,
      count: Number.isFinite(bin.count) ? bin.count : 0,
    };
  })
);

// Sorted by hour of day; the x axis renders the 0..23 hour labels.
const typicalRows = computed(() =>
  [...props.typicalDay]
    .sort((a, b) => Number(a.hourOfDay ?? 0) - Number(b.hourOfDay ?? 0))
    .map((row) => ({
      label: `${Number(row.hourOfDay) || 0}h`,
      hour: Number(row.hourOfDay) || 0,
      avg: Number.isFinite(row.avg) ? Number(row.avg) : null,
    }))
);

const destroyCharts = () => {
  histogramChart?.destroy();
  histogramChart = undefined;
  typicalChart?.destroy();
  typicalChart = undefined;
};

// Value histogram: square-root y scale so a skewed distribution keeps its
// sparse bins readable; the long range labels auto-rotate when crowded.
const drawHistogram = () => {
  const el = histogramChartRef.value;
  if (!el) return;
  histogramChart?.destroy();
  histogramChart = new Chart({container: el, autoFit: true});
  const {primary} = chartPalette();
  histogramChart
    .interval()
    .data(histogramRows.value)
    .encode('x', 'label')
    .encode('y', 'count')
    .style('fill', primary)
    .scale('y', {type: 'sqrt', zero: true, nice: true})
    .axis({x: {title: false, labelAutoRotate: true}, y: {title: false, labelFormatter: (d: number) => compactNumber(Number(d))}})
    .legend(false);
  histogramChart.render();
};

// Typical day: line + point over the 0..23 hour axis.
const drawTypical = () => {
  const el = typicalChartRef.value;
  if (!el) return;
  typicalChart?.destroy();
  typicalChart = new Chart({container: el, autoFit: true});
  const {primary} = chartPalette();
  const data = typicalRows.value;
  typicalChart
    .line()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'avg')
    .encode('shape', 'smooth')
    .style('stroke', primary)
    .style('lineWidth', 2)
    .scale('y', {nice: true})
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false, labelFormatter: (d: number) => compactNumber(Number(d))}})
    .legend(false);
  typicalChart
    .point()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'avg')
    .style('fill', primary)
    .style('r', 2.5)
    .axis(false)
    .legend(false);
  typicalChart.interaction('tooltip', {
    render: (
      _event: unknown,
      {items}: {items: Array<{value: unknown; data?: Record<string, unknown>}>}
    ) => {
      const d = items?.[0]?.data as Record<string, any> | undefined;
      if (!d) return '';
      const unit = props.unit ? ` ${props.unit}` : '';
      return `${String(d.label ?? '')} · <b>${formatValue(Number(d.avg))}${unit}</b>`;
    },
  });
  typicalChart.render();
};

watch(
  () => [props.histogram, props.typicalDay, props.unit],
  async () => {
    await nextTick();
    if (histogramChartRef.value && histogramRows.value.length) drawHistogram();
    else {
      histogramChart?.destroy();
      histogramChart = undefined;
    }
    if (typicalChartRef.value && typicalRows.value.length) drawTypical();
    else {
      typicalChart?.destroy();
      typicalChart = undefined;
    }
  },
  {deep: true, flush: 'post'}
);

onMounted(async () => {
  await nextTick();
  if (histogramChartRef.value && histogramRows.value.length) drawHistogram();
  if (typicalChartRef.value && typicalRows.value.length) drawTypical();
});

onUnmounted(destroyCharts);
</script>

<style lang="scss" scoped>
.value-profile {
  row-gap: var(--dc3-gutter);
}

.value-profile__canvas {
  width: 100%;
  height: 100%;
}
</style>
