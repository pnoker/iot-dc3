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
  <el-card class="activity-heatmap" shadow="never">
    <template #header>
      <div class="activity-heatmap__header">
        <span class="activity-heatmap__title">{{ $t('home.activity.title') }}</span>
        <div class="activity-heatmap__actions">
          <el-segmented
            v-model="rangeKey"
            :options="[
              { label: $t('common.ranges.h24'), value: 'h24' },
              { label: $t('common.ranges.d7'), value: 'd7' },
              { label: $t('common.ranges.d30'), value: 'd30' },
            ]"
            size="small"
            @change="load"
          />
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
        </div>
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="activity-heatmap__canvas"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { statsActivity } from '@/api/dashboard';

  type RangeKey = 'h24' | 'd7' | 'd30';

  const { t } = useI18n();
  const rangeKey = ref<RangeKey>('h24');
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

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
    chart = new Chart({ container: chartRef.value, autoFit: true, height: 280 });

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
      .style({ stroke: '#ffffff', lineWidth: 1, inset: 0.5 })
      .axis({
        x: { title: false, labelAutoHide: false },
        y: { title: false },
      })
      .tooltip({
        title: (d: any) => `${d.dow} ${d.hour}:00`,
        items: [{ field: 'count', name: t('home.activity.count') }],
      });
    chart.render();
  };

  const load = async () => {
    const hours = rangeKey.value === 'h24' ? 24 : rangeKey.value === 'd7' ? 168 : 720;
    loading.value = true;
    try {
      const res: any = await statsActivity(hours);
      const rows = (res?.data ?? []) as { dow: number; hour: number; count: number }[];
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
  .activity-heatmap {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    .activity-heatmap__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .activity-heatmap__title {
      font-weight: 600;
      color: #303133;
    }

    .activity-heatmap__actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .activity-heatmap__canvas {
      width: 100%;
      height: 280px;
    }
  }
</style>
