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
  <el-card class="trend-chart" shadow="never">
    <template #header>
      <div class="trend-chart__header">
        <span class="trend-chart__title">{{ $t('home.trendTitle') }}</span>
        <div class="trend-chart__actions">
          <el-segmented
            v-model="rangeKey"
            :options="[
              { label: $t('home.ranges.h24'), value: 'h24' },
              { label: $t('home.ranges.d7'), value: 'd7' },
              { label: $t('home.ranges.d30'), value: 'd30' },
            ]"
            size="small"
            @change="load"
          />
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
        </div>
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="trend-chart__canvas"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { statsTimeseries } from '@/api/dashboard';

  type RangeKey = 'h24' | 'd7' | 'd30';

  const rangeKey = ref<RangeKey>('h24');
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  const ensureChart = () => {
    if (!chartRef.value) return;
    chart?.destroy();
    chart = new Chart({ container: chartRef.value, autoFit: true, height: 280 });
  };

  const render = (points: { bucket: string; count: number }[]) => {
    ensureChart();
    if (!chart) return;
    chart
      .area()
      .data(points)
      .encode('x', 'bucket')
      .encode('y', 'count')
      .encode('shape', 'smooth')
      .scale('y', { zero: true, nice: true })
      .style('fill', 'linear-gradient(-90deg, rgba(64,158,255,0.02) 0%, rgba(64,158,255,0.45) 100%)')
      .axis({ x: { title: false, labelAutoHide: true }, y: { title: false } })
      .animate('enter', { type: 'fadeIn', duration: 400 });
    chart
      .line()
      .data(points)
      .encode('x', 'bucket')
      .encode('y', 'count')
      .encode('shape', 'smooth')
      .style('stroke', '#409eff')
      .style('lineWidth', 2)
      .axis(false)
      .legend(false);
    chart.render();
  };

  const load = async () => {
    const params: { granularity: 'hour' | 'day'; rangeHours: number } =
      rangeKey.value === 'h24'
        ? { granularity: 'hour', rangeHours: 24 }
        : rangeKey.value === 'd7'
          ? { granularity: 'day', rangeHours: 168 }
          : { granularity: 'day', rangeHours: 720 };
    loading.value = true;
    try {
      const res: any = await statsTimeseries(params);
      const points = (res?.data ?? []).map((p: any) => ({ bucket: p.bucket, count: Number(p.count) || 0 }));
      await nextTick();
      render(points);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(load);
  watch(rangeKey, load);
  onUnmounted(() => chart?.destroy());
</script>

<style lang="scss" scoped>
  .trend-chart {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    .trend-chart__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .trend-chart__title {
      font-weight: 600;
      color: #303133;
    }

    .trend-chart__actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .trend-chart__canvas {
      width: 100%;
      height: 280px;
    }
  }
</style>
