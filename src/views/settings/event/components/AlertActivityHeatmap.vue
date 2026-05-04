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
  <el-card class="alert-activity" shadow="never">
    <template #header>
      <div class="alert-activity__header">
        <span class="alert-activity__title">{{ t('settings.event.overview.activityTitle') }}</span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="alert-activity__canvas"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { alertActivity } from '@/api/dashboard';

  const { t } = useI18n();

  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  // Sunday-first so the row order matches Postgres EXTRACT(DOW) (0..6 = Sun..Sat).
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
    chart = new Chart({ container: chartRef.value, autoFit: true });
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
      // Red-tinted sequential palette — alarm activity reads hotter than the
      // blue-tinted volume heatmap on Home so the two pages stay visually
      // distinct.
      .scale('color', { type: 'sequential', palette: 'reds' })
      .style({ stroke: '#ffffff', lineWidth: 1, inset: 0.5 })
      .axis({ x: { title: false, labelAutoHide: false }, y: { title: false } })
      .tooltip({
        title: (d: { dow: string; hour: string }) => `${d.dow} ${d.hour}:00`,
        items: [{ field: 'count', name: t('settings.event.overview.alarmCount') }],
      });
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: { dow: number; hour: number; count: number }[] } = await alertActivity(7);
      const rows = res?.data ?? [];
      await nextTick();
      render(rows);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(load);
  onUnmounted(() => chart?.destroy());
  defineExpose({ refresh: load });
</script>

<style lang="scss" scoped>
  .alert-activity {
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

    .alert-activity__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .alert-activity__title {
      font-weight: 600;
      color: #303133;
    }

    .alert-activity__canvas {
      flex: 1;
      width: 100%;
      min-height: 0;
    }
  }
</style>
