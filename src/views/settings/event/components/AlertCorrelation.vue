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
  <dashboard-card
    class="alert-correlation"
    :title="t('settings.event.overview.correlationTitle')"
    :subtitle="t('settings.event.overview.correlationSubtitle', { hours, windowSec })"
    :loading="loading"
    :empty="!loading && pairs.length === 0"
    :empty-text="t('settings.event.overview.correlationEmpty')"
    :empty-image-size="60"
    :height="520"
    body-mode="chart"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="hoursKey" :options="hoursOptions" size="small" />
    </template>

    <div ref="graphRef" class="alert-correlation__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Graph } from '@antv/g6';

  import { alertCorrelation } from '@/api/dashboard';
  import type { CorrelationPair } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const { t } = useI18n();

  // Window tool — longer windows reveal relationships that take minutes
  // to propagate (MQTT broker hiccup → cascading device timeouts), shorter
  // windows isolate immediate co-firings.
  const hoursOptions = [
    { label: '6h', value: '6' },
    { label: '24h', value: '24' },
    { label: '7d', value: '168' },
  ];
  const hoursKey = ref<string>('24');
  const hours = computed(() => Number(hoursKey.value));
  const windowSec = 30;

  const loading = ref(false);
  const pairs = ref<CorrelationPair[]>([]);
  const nameMap = reactive<{ devices: Record<string, string>; drivers: Record<string, string> }>({
    devices: {},
    drivers: {},
  });
  const graphRef = ref<HTMLElement>();
  let graph: Graph | undefined;

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: CorrelationPair[] } = await alertCorrelation(hours.value, windowSec, 15);
      pairs.value = res?.data ?? [];
      await resolveNames(pairs.value);
      await nextTick();
      if (pairs.value.length > 0) renderGraph();
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  watch(hoursKey, load);
  onMounted(load);
  onUnmounted(() => graph?.destroy());

  const resolveNames = async (batch: CorrelationPair[]) => {
    const devIds = new Set<string>();
    const drvIds = new Set<string>();
    for (const p of batch) {
      if (p.aSource === 'device') devIds.add(String(p.aSourceId));
      else drvIds.add(String(p.aSourceId));
      if (p.bSource === 'device') devIds.add(String(p.bSourceId));
      else drvIds.add(String(p.bSourceId));
    }
    const devMissing = [...devIds].filter((id) => !nameMap.devices[id]);
    const drvMissing = [...drvIds].filter((id) => !nameMap.drivers[id]);
    const jobs: Promise<void>[] = [];
    if (devMissing.length) {
      jobs.push(
        getDeviceByIds(devMissing)
          .then((r: { data?: Record<string, { deviceName?: string }> }) => {
            const d = r?.data || {};
            for (const id of devMissing) if (d[id]) nameMap.devices[id] = d[id].deviceName || id;
          })
          .catch(() => {})
      );
    }
    if (drvMissing.length) {
      jobs.push(
        getDriverByIds(drvMissing)
          .then((r: { data?: Record<string, { driverName?: string }> }) => {
            const d = r?.data || {};
            for (const id of drvMissing) if (d[id]) nameMap.drivers[id] = d[id].driverName || id;
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const nodeId = (source: string, id: number | string) => `${source}:${id}`;
  const nodeLabel = (source: string, id: number | string) => {
    const key = String(id);
    if (source === 'device') return nameMap.devices[key] || key;
    return nameMap.drivers[key] || key;
  };

  const renderGraph = () => {
    const container = graphRef.value;
    if (!container) return;
    graph?.destroy();

    // Convert pairs → unique nodes + weighted edges.
    const nodeMap = new Map<string, { id: string; data: { source: string; sid: number | string; name: string } }>();
    const edges: Array<{ source: string; target: string; data: { weight: number } }> = [];
    for (const p of pairs.value) {
      const aId = nodeId(p.aSource, p.aSourceId);
      const bId = nodeId(p.bSource, p.bSourceId);
      if (!nodeMap.has(aId)) {
        nodeMap.set(aId, {
          id: aId,
          data: { source: p.aSource, sid: p.aSourceId, name: nodeLabel(p.aSource, p.aSourceId) },
        });
      }
      if (!nodeMap.has(bId)) {
        nodeMap.set(bId, {
          id: bId,
          data: { source: p.bSource, sid: p.bSourceId, name: nodeLabel(p.bSource, p.bSourceId) },
        });
      }
      edges.push({ source: aId, target: bId, data: { weight: p.coCount } });
    }
    const nodes = Array.from(nodeMap.values());

    // Width (clientWidth) / height (clientHeight) come from the card body.
    graph = new Graph({
      container,
      data: { nodes, edges },
      autoResize: true,
      layout: { type: 'force', preventOverlap: true, nodeSize: 32, linkDistance: 90 },
      node: {
        style: {
          size: 28,
          // Devices blue, drivers purple — same family as LiveDataFeed.
          fill: (d: { data?: { source?: string } }) => (d.data?.source === 'device' ? '#409eff' : '#9059f6'),
          stroke: 'transparent',
          labelText: (d: { data?: { name?: string } }) => d.data?.name ?? '',
          labelPlacement: 'bottom',
          labelFontSize: 11,
          labelFill: '#303133',
          cursor: 'pointer',
        },
      },
      edge: {
        style: {
          stroke: '#c0c4cc',
          // Thicker edge = more co-occurrences. Clamp between 1–6 so a
          // single hot pair doesn't wash out everything else.
          lineWidth: (d: { data?: { weight?: number } }) =>
            Math.max(1, Math.min(6, Math.log2((d.data?.weight ?? 1) + 1))),
          endArrow: false,
          labelText: (d: { data?: { weight?: number } }) => String(d.data?.weight ?? ''),
          labelFontSize: 10,
          labelFill: '#909399',
        },
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    });
    graph.render();
  };
</script>

<style lang="scss" scoped>
  .alert-correlation {
    .alert-correlation__canvas {
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
  }
</style>
