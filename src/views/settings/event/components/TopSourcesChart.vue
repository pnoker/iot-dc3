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
  <el-card class="top-sources" shadow="never">
    <template #header>
      <div class="top-sources__header">
        <span class="top-sources__title">{{ $t('settings.event.overview.topSourcesTitle') }}</span>
      </div>
    </template>
    <div ref="chartRef" v-loading="loading" class="top-sources__chart"></div>
  </el-card>
</template>

<script lang="ts" setup>
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import { Chart } from '@antv/g2';

  import { alertTopSources } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';

  const props = defineProps<{ days?: number; limit?: number }>();

  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  const render = (data: { name: string; count: number }[]) => {
    const el = chartRef.value;
    if (!el) return;
    chart?.destroy();
    chart = new Chart({ container: el, autoFit: true, height: 280 });
    chart
      .interval()
      .data(data)
      .encode('x', 'name')
      .encode('y', 'count')
      .encode('color', 'name')
      .scale('x', { padding: 0.5 })
      .axis({
        x: { title: false, labelAutoRotate: false },
        y: { title: false },
      })
      .legend(false)
      .tooltip({ channel: 'y', valueFormatter: (d: number) => d.toLocaleString() })
      .coordinate({ transform: [{ type: 'transpose' }] });
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertTopSources(props.days ?? 30, props.limit ?? 10);
      const rows: any[] = res?.data ?? [];

      // Resolve names
      const devIds = rows.filter((r) => r.source === 'device').map((r) => String(r.sourceId));
      const drvIds = rows.filter((r) => r.source === 'driver').map((r) => String(r.sourceId));
      const nameMap: Record<string, string> = {};

      const jobs: Promise<void>[] = [];
      if (devIds.length) {
        jobs.push(
          getDeviceByIds(devIds)
            .then((r: any) => {
              const d = r?.data || {};
              for (const id of devIds) {
                nameMap[`device:${id}`] = d[id]?.deviceName || id;
              }
            })
            .catch(() => {})
        );
      }
      if (drvIds.length) {
        jobs.push(
          getDriverByIds(drvIds)
            .then((r: any) => {
              const d = r?.data || {};
              for (const id of drvIds) {
                nameMap[`driver:${id}`] = d[id]?.driverName || id;
              }
            })
            .catch(() => {})
        );
      }
      await Promise.all(jobs);

      const chartData = rows.map((r: any) => ({
        name: nameMap[`${r.source}:${r.sourceId}`] || String(r.sourceId),
        count: r.count ?? 0,
      }));
      await nextTick();
      render(chartData);
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
  .top-sources {
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    .top-sources__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .top-sources__title {
      font-weight: 600;
      color: #303133;
    }

    .top-sources__chart {
      width: 100%;
    }
  }
</style>
