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
  <el-card class="latency-chart" shadow="never">
    <template #header>
      <div class="latency-chart__header">
        <span class="latency-chart__title">{{ $t('home.latency.title') }}</span>
        <div class="latency-chart__actions">
          <range-segmented v-model="rangeKey" size="small" @update:model-value="load" />
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
        </div>
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="latency-chart__canvas"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { statsLatency } from '@/api/dashboard';
  import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
  import type { RangeKey } from '@/components/segmented/RangeSegmented.vue';

  const { t } = useI18n();
  const rangeKey = ref<RangeKey>('24h');
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  // Bin labels line up with the 6-bucket layout in the backend SQL.
  const binLabels = () => [
    t('home.latency.bin.under100ms'),
    t('home.latency.bin.100to500ms'),
    t('home.latency.bin.500msto1s'),
    t('home.latency.bin.1to5s'),
    t('home.latency.bin.5to30s'),
    t('home.latency.bin.over30s'),
  ];

  const render = (rows: { bin: number; count: number }[]) => {
    if (!chartRef.value) return;
    chart?.destroy();
    chart = new Chart({ container: chartRef.value, autoFit: true, height: 280 });
    const labels = binLabels();
    const data = rows.map((r) => ({ label: labels[r.bin] || `bin-${r.bin}`, bin: r.bin, count: Number(r.count) || 0 }));
    chart
      .interval()
      .data(data)
      .encode('x', 'label')
      .encode('y', 'count')
      .encode('color', 'bin')
      .scale('color', {
        range: ['#67c23a', '#95d475', '#f0c14b', '#e6a23c', '#f56c6c', '#c45656'],
      })
      .legend(false)
      .axis({ x: { title: false, labelAutoRotate: false }, y: { title: false } });
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await statsLatency({ rangeKey: rangeKey.value });
      const rows = (res?.data ?? []) as { bin: number; count: number }[];
      await nextTick();
      render(rows);
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
  .latency-chart {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    .latency-chart__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .latency-chart__title {
      font-weight: 600;
      color: #303133;
    }

    .latency-chart__actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .latency-chart__canvas {
      width: 100%;
      height: 280px;
    }
  }
</style>
