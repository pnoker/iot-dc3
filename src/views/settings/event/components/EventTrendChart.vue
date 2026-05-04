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
  <el-card class="event-trend" shadow="never">
    <template #header>
      <div class="event-trend__header">
        <span class="event-trend__title">{{ $t('settings.event.overview.trendTitle') }}</span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="event-trend__chart"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { alertTrend } from '@/api/dashboard';

  const props = defineProps<{ days?: number }>();

  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  const render = (data: { date: string; source: string; count: number }[]) => {
    const el = chartRef.value;
    if (!el) return;
    chart?.destroy();
    chart = new Chart({ container: el, autoFit: true });
    chart
      .line()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'count')
      .encode('color', 'source')
      .encode('shape', 'smooth')
      .style('lineWidth', 2)
      .axis({ x: { title: false, labelAutoHide: true }, y: { title: false } })
      .legend('color')
      .tooltip({ channel: 'y', valueFormatter: (d: number) => d.toLocaleString() });
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertTrend(props.days ?? 30);
      const rows: any[] = res?.data ?? [];
      const flat: { date: string; source: string; count: number }[] = [];
      for (const r of rows) {
        flat.push({ date: r.date, source: 'Device', count: r.deviceCount ?? 0 });
        flat.push({ date: r.date, source: 'Driver', count: r.driverCount ?? 0 });
      }
      await nextTick();
      render(flat);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(load);
  watch(() => props.days, load);
  onUnmounted(() => chart?.destroy());

  defineExpose({ refresh: load });
</script>

<style lang="scss" scoped>
  .event-trend {
    min-height: 360px;
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
    }

    .event-trend__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .event-trend__title {
      font-weight: 600;
      color: #303133;
    }

    .event-trend__chart {
      flex: 1;
      width: 100%;
      min-height: 0;
    }
  }
</style>
