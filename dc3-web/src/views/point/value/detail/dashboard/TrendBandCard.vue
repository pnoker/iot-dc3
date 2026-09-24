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

<!-- Trend overview of the point dashboard: the average line wrapped in the
     min/max envelope band (G2 rangeY). Hosts the shared window selector in
     the card header — switching it re-fetches the whole dashboard. -->

<template>
  <dashboard-card
    :empty="status === 'success' && !hasData"
    :empty-text="$t('pointValue.dashboard.trend.empty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :footer-meta="$t('pointValue.dashboard.trend.footer')"
    :height="340"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('pointValue.dashboard.trend.title')"
    body-mode="chart"
    @refresh="emit('refresh')"
  >
    <template #tools>
      <el-segmented
        :model-value="rangeHours"
        :options="windowOptions"
        size="small"
        @update:model-value="onRangeChange"
      />
    </template>
    <div ref="chartRef" class="trend-band-card__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {PointDashboardTrendBucket} from '@/config/types/dashboard';
import {formatDateTime} from '@/utils/timeUtil';
import {chartPalette, compactNumber, formatValue, hourLabel} from './util';

const props = defineProps({
  /** Trend buckets of the dashboard payload, ascending by time. */
  data: {
    type: Array as PropType<PointDashboardTrendBucket[]>,
    default: () => [],
  },
  /** Unit suffix shown in the tooltip. */
  unit: {
    type: String,
    default: '',
  },
  /** Active lookback window in hours (1/6/24/168). */
  rangeHours: {
    type: Number,
    default: 24,
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

const emit = defineEmits<{
  (e: 'update:rangeHours', value: number): void;
  (e: 'refresh'): void;
}>();

const {t} = useI18n();
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;

const hasData = computed(() => props.data.length > 0);

const windowOptions = computed(() => [
  {label: t('pointValue.dashboard.window.h1'), value: 1},
  {label: t('pointValue.dashboard.window.h6'), value: 6},
  {label: t('pointValue.dashboard.window.h24'), value: 24},
  {label: t('pointValue.dashboard.window.d7'), value: 168},
]);

const onRangeChange = (value: string | number | boolean | undefined) => {
  const hours = Number(value);
  if (Number.isFinite(hours) && hours !== props.rangeHours) emit('update:rangeHours', hours);
};

// Chart rows: a display label per bucket plus the numeric band columns.
// Buckets outside a single day carry the month/day so repeated hours stay
// distinguishable on the 7d window.
const rows = computed(() =>
  props.data.map((bucket) => ({
    label: hourLabel(String(bucket.from ?? ''), props.rangeHours > 24),
    from: String(bucket.from ?? ''),
    min: Number.isFinite(bucket.min) ? Number(bucket.min) : null,
    max: Number.isFinite(bucket.max) ? Number(bucket.max) : null,
    avg: Number.isFinite(bucket.avg) ? Number(bucket.avg) : null,
    count: Number.isFinite(bucket.sampleCount) ? bucket.sampleCount : 0,
  }))
);

const destroyChart = () => {
  chart?.destroy();
  chart = undefined;
};

const draw = () => {
  const el = chartRef.value;
  if (!el) return;
  destroyChart();
  chart = new Chart({container: el, autoFit: true});
  const {primary} = chartPalette();
  const data = rows.value;

  chart
    .rangeY()
    .data(data)
    .encode('x', 'label')
    .encode('y', ['min', 'max'])
    .style('fill', primary)
    .style('fillOpacity', 0.12)
    .scale('y', {nice: true})
    .axis({
      x: {title: false, labelAutoHide: true},
      y: {title: false, labelFormatter: (d: number) => compactNumber(Number(d))},
    })
    .legend(false);

  chart
    .line()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'avg')
    .encode('shape', 'smooth')
    .style('stroke', primary)
    .style('lineWidth', 2)
    .axis(false)
    .legend(false);

  chart
    .point()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'avg')
    .style('fill', primary)
    .style('r', 2.5)
    .axis(false)
    .legend(false);

  chart.interaction('tooltip', {
    render: (
      _event: unknown,
      {items}: {items: Array<{value: unknown; data?: Record<string, unknown>}>}
    ) => {
      const d = items?.[0]?.data as Record<string, any> | undefined;
      if (!d) return '';
      const unit = props.unit ? ` ${props.unit}` : '';
      const rowsHtml = [
        [t('pointValue.dashboard.trend.min'), d.min],
        [t('pointValue.dashboard.trend.max'), d.max],
        [t('pointValue.dashboard.trend.avg'), d.avg],
      ]
        .filter(([, v]) => v != null && Number.isFinite(Number(v)))
        .map(
          ([name, v]) =>
            `<div class="trend-band-tooltip__row"><span>${name}</span><b>${formatValue(Number(v))}${unit}</b></div>`
        )
        .join('');
      return `<div class="trend-band-tooltip">
        <div class="trend-band-tooltip__title">${formatDateTime(String(d.from ?? ''))}</div>
        ${rowsHtml}
        <div class="trend-band-tooltip__count">${Number(d.count) || 0} ${t('pointValue.dashboard.trend.samples')}</div>
      </div>`;
    },
  });

  chart.render();
};

// Redraw whenever the data, unit or window label mode changes. The canvas
// lives inside the dashboard-card content slot, which unmounts while the
// empty/error state shows — nextTick lets the ref re-attach before drawing.
watch(
  () => [props.data, props.unit, props.rangeHours],
  async () => {
    await nextTick();
    if (chartRef.value && hasData.value) draw();
    else destroyChart();
  },
  {deep: true, flush: 'post'}
);

onMounted(async () => {
  await nextTick();
  if (chartRef.value && hasData.value) draw();
});

onUnmounted(destroyChart);
</script>

<style lang="scss" scoped>
.trend-band-card__canvas {
  width: 100%;
  height: 100%;
}

:deep(.trend-band-tooltip) {
  font-size: 12px;
  line-height: 1.6;
}

:deep(.trend-band-tooltip__title) {
  margin-bottom: 4px;
  font-weight: 600;
  color: var(--dc3-text-secondary);
}

:deep(.trend-band-tooltip__row) {
  display: flex;
  justify-content: space-between;
  gap: var(--dc3-space-4);

  b {
    font-weight: 600;
    color: var(--dc3-text-primary);
  }
}

:deep(.trend-band-tooltip__count) {
  margin-top: 4px;
  color: var(--dc3-text-muted);
}
</style>
