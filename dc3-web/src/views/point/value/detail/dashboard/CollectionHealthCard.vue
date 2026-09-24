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

<!-- Collection health trio under the trend chart: sampling-interval
     histogram, the compact gap timeline and the hourly sample volume —
     three equal cards on one row, stacked on narrow screens. -->

<template>
  <el-row :gutter="8" class="collection-health">
    <el-col :lg="8" :md="24" :sm="24" :xl="8" :xs="24">
      <dashboard-card
        :empty="status === 'success' && intervalRows.length === 0"
        :empty-text="$t('pointValue.dashboard.health.intervalEmpty')"
        :error="status === 'error'"
        :error-text="$t('common.loadFailed')"
        :footer-meta="$t('pointValue.dashboard.health.intervalFooter')"
        :height="300"
        :loading="loading"
        :retry-text="$t('common.retry')"
        :title="$t('pointValue.dashboard.health.intervalTitle')"
        body-mode="chart"
        @refresh="emit('refresh')"
      >
        <div ref="intervalChartRef" class="collection-health__canvas"></div>
      </dashboard-card>
    </el-col>

    <el-col :lg="8" :md="24" :sm="24" :xl="8" :xs="24">
      <dashboard-card
        :badge="gaps.length || null"
        :empty="status === 'success' && gaps.length === 0"
        :empty-text="$t('pointValue.dashboard.health.gapEmpty')"
        :error="status === 'error'"
        :error-text="$t('common.loadFailed')"
        :height="300"
        :loading="loading"
        :retry-text="$t('common.retry')"
        :title="$t('pointValue.dashboard.health.gapTitle')"
        body-mode="scroll"
        @refresh="emit('refresh')"
      >
        <el-timeline class="collection-health__gaps">
          <el-timeline-item
            v-for="gap in visibleGaps"
            :key="`${gap.from}-${gap.to}`"
            :timestamp="gapClock(gap.from)"
            placement="top"
            type="danger"
          >
            <div class="collection-health__gap-row">
              <span class="collection-health__gap-range">{{ gapClock(gap.from) }} ~ {{ gapClock(gap.to) }}</span>
              <el-tag effect="plain" size="small" type="danger">{{ formatMs(gap.durationMs) }}</el-tag>
            </div>
          </el-timeline-item>
        </el-timeline>
        <div v-if="gaps.length > MAX_GAPS" class="collection-health__gap-more">
          {{ $t('pointValue.dashboard.health.gapMore', {count: gaps.length - MAX_GAPS}) }}
        </div>
      </dashboard-card>
    </el-col>

    <el-col :lg="8" :md="24" :sm="24" :xl="8" :xs="24">
      <dashboard-card
        :empty="status === 'success' && hourlyRows.length === 0"
        :empty-text="$t('pointValue.dashboard.health.hourlyEmpty')"
        :error="status === 'error'"
        :error-text="$t('common.loadFailed')"
        :footer-meta="$t('pointValue.dashboard.health.hourlyFooter')"
        :height="300"
        :loading="loading"
        :retry-text="$t('common.retry')"
        :title="$t('pointValue.dashboard.health.hourlyTitle')"
        body-mode="chart"
        @refresh="emit('refresh')"
      >
        <div ref="hourlyChartRef" class="collection-health__canvas"></div>
      </dashboard-card>
    </el-col>
  </el-row>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {Chart} from '@antv/g2';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {PointDashboardGap, PointDashboardHourVolume, PointDashboardIntervalBin} from '@/config/types/dashboard';
import {formatClock, formatMs} from '@/utils/timeUtil';
import {chartPalette, compactNumber, hourLabel, intervalBinLabel} from './util';

const props = defineProps({
  /** Sampling-interval histogram bins of the dashboard payload. */
  intervalHistogram: {
    type: Array as PropType<PointDashboardIntervalBin[]>,
    default: () => [],
  },
  /** Collection gaps of the dashboard payload, newest first. */
  gaps: {
    type: Array as PropType<PointDashboardGap[]>,
    default: () => [],
  },
  /** Hourly sample volumes of the dashboard payload, hours ascending. */
  hourlyVolume: {
    type: Array as PropType<PointDashboardHourVolume[]>,
    default: () => [],
  },
  /** Active lookback window in hours — drives the hour label mode. */
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

const emit = defineEmits<{ (e: 'refresh'): void }>();

/** Gap rows rendered on the timeline before the "… more" digest. */
const MAX_GAPS = 10;

const intervalChartRef = ref<HTMLElement>();
const hourlyChartRef = ref<HTMLElement>();
let intervalChart: Chart | undefined;
let hourlyChart: Chart | undefined;

const visibleGaps = computed(() => props.gaps.slice(0, MAX_GAPS));

const gapClock = (iso: string) => formatClock(iso) || iso;

const intervalRows = computed(() =>
  props.intervalHistogram.map((bin) => ({
    label: intervalBinLabel(bin.fromMs, bin.toMs),
    count: Number.isFinite(bin.count) ? bin.count : 0,
    open: bin.toMs == null || !Number.isFinite(bin.toMs),
  }))
);

const hourlyRows = computed(() =>
  props.hourlyVolume.map((row) => ({
    label: hourLabel(String(row.hourStart ?? ''), props.rangeHours > 24),
    hourStart: String(row.hourStart ?? ''),
    count: Number.isFinite(row.count) ? row.count : 0,
  }))
);

const destroyCharts = () => {
  intervalChart?.destroy();
  intervalChart = undefined;
  hourlyChart?.destroy();
  hourlyChart = undefined;
};

// Interval histogram: one bar per fixed bin, the open-ended top bin
// highlighted so slow collections read as a tail risk, not noise. The
// square-root y scale keeps rare slow bins visible against the mass.
const drawInterval = () => {
  const el = intervalChartRef.value;
  if (!el) return;
  intervalChart?.destroy();
  intervalChart = new Chart({container: el, autoFit: true});
  const {primary, danger} = chartPalette();
  const data = intervalRows.value;
  intervalChart
    .interval()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'count')
    .encode('color', 'open')
    .scale('y', {type: 'sqrt', zero: true, nice: true})
    .scale('color', {range: [primary, danger]})
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false, labelFormatter: (d: number) => compactNumber(Number(d))}})
    .legend(false);
  intervalChart.render();
};

// Hourly volume: plain bars, one per hour of the window.
const drawHourly = () => {
  const el = hourlyChartRef.value;
  if (!el) return;
  hourlyChart?.destroy();
  hourlyChart = new Chart({container: el, autoFit: true});
  const {primary} = chartPalette();
  const data = hourlyRows.value;
  hourlyChart
    .interval()
    .data(data)
    .encode('x', 'label')
    .encode('y', 'count')
    .style('fill', primary)
    .scale('y', {zero: true, nice: true})
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false, labelFormatter: (d: number) => compactNumber(Number(d))}})
    .legend(false);
  hourlyChart.render();
};

watch(
  () => [props.intervalHistogram, props.hourlyVolume, props.rangeHours],
  async () => {
    await nextTick();
    if (intervalChartRef.value && intervalRows.value.length) drawInterval();
    else {
      intervalChart?.destroy();
      intervalChart = undefined;
    }
    if (hourlyChartRef.value && hourlyRows.value.length) drawHourly();
    else {
      hourlyChart?.destroy();
      hourlyChart = undefined;
    }
  },
  {deep: true, flush: 'post'}
);

onMounted(async () => {
  await nextTick();
  if (intervalChartRef.value && intervalRows.value.length) drawInterval();
  if (hourlyChartRef.value && hourlyRows.value.length) drawHourly();
});

onUnmounted(destroyCharts);
</script>

<style lang="scss" scoped>
.collection-health {
  row-gap: var(--dc3-gutter);
}

.collection-health__canvas {
  width: 100%;
  height: 100%;
}

.collection-health__gaps {
  padding: var(--dc3-space-2) var(--dc3-space-4) 0;

  :deep(.el-timeline-item__timestamp) {
    font-size: 12px;
    color: var(--dc3-text-muted);
    margin-bottom: 2px;
  }
}

.collection-health__gap-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-3);
}

.collection-health__gap-range {
  font-size: 12px;
  color: var(--dc3-text-secondary);
}

.collection-health__gap-more {
  padding: var(--dc3-space-2) var(--dc3-space-4) var(--dc3-space-3);
  font-size: 12px;
  color: var(--dc3-text-muted);
}
</style>
