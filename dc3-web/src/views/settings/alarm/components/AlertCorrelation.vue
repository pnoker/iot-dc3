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
    :empty="!loading && pairs.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.correlationEmpty')"
    :height="520"
    :loading="loading"
    :subtitle="t('settings.event.overview.correlationSubtitle', {hours, windowSec})"
    :title="t('settings.event.overview.correlationTitle')"
    body-mode="chart"
    class="alert-correlation"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="hoursKey" :options="hoursOptions" size="small" />
    </template>

    <div ref="graphRef" class="alert-correlation__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {Graph} from '@antv/g6';

  import {alertCorrelation} from '@/api/dashboard';
  import type {CorrelationPair} from '@/config/types/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
  import {useEntityNames} from '@/composables/useEntityNames';
  import {DASHBOARD_PALETTE} from '@/config/constant/palette';

  const {t} = useI18n();
  const {loading, run} = useAsyncLoader();
  const {resolveBySource, nameBySource} = useEntityNames();

  // Window tool — longer windows reveal relationships that take minutes
  // to propagate (MQTT broker hiccup → cascading device timeouts), shorter
  // windows isolate immediate co-firings.
  const hoursOptions = [
    {label: '6h', value: '6'},
    {label: '24h', value: '24'},
    {label: '7d', value: '168'},
  ];
  const hoursKey = ref<string>('24');
  const hours = computed(() => Number(hoursKey.value));
  const windowSec = 30;

  const pairs = ref<CorrelationPair[]>([]);
  const graphRef = ref<HTMLElement>();
  let graph: Graph | undefined;

  const load = () =>
    run(async () => {
      const res: {data?: CorrelationPair[]} = await alertCorrelation(hours.value, windowSec, 15);
      pairs.value = res?.data ?? [];
      // Feed both endpoints per pair into the shared name cache.
      const flat = pairs.value.flatMap((p) => [
        {source: p.aSource, sourceId: p.aSourceId},
        {source: p.bSource, sourceId: p.bSourceId},
      ]);
      await resolveBySource(flat);
      await nextTick();
      if (pairs.value.length > 0) renderGraph();
    });

  watch(hoursKey, load);
  onMounted(load);
  onUnmounted(() => graph?.destroy());

  const nodeId = (source: string, id: number | string) => `${source}:${id}`;

  const renderGraph = () => {
    const container = graphRef.value;
    if (!container) return;
    graph?.destroy();

    // Convert pairs → unique nodes + weighted edges.
    const nodeMap = new Map<string, {id: string; data: {source: string; sid: number | string; name: string}}>();
    const edges: Array<{source: string; target: string; data: {weight: number}}> = [];
    for (const p of pairs.value) {
      const aId = nodeId(p.aSource, p.aSourceId);
      const bId = nodeId(p.bSource, p.bSourceId);
      if (!nodeMap.has(aId)) {
        nodeMap.set(aId, {
          id: aId,
          data: {source: p.aSource, sid: p.aSourceId, name: nameBySource(p.aSource, p.aSourceId)},
        });
      }
      if (!nodeMap.has(bId)) {
        nodeMap.set(bId, {
          id: bId,
          data: {source: p.bSource, sid: p.bSourceId, name: nameBySource(p.bSource, p.bSourceId)},
        });
      }
      edges.push({source: aId, target: bId, data: {weight: p.coCount}});
    }
    const nodes = Array.from(nodeMap.values());

    graph = new Graph({
      container,
      data: {nodes, edges},
      autoResize: true,
      layout: {type: 'force', preventOverlap: true, nodeSize: 32, linkDistance: 90},
      node: {
        style: {
          size: 28,
          fill: (d: {data?: {source?: string}}) =>
            d.data?.source === 'device' ? DASHBOARD_PALETTE.device : DASHBOARD_PALETTE.driver,
          stroke: 'transparent',
          labelText: (d: {data?: {name?: string}}) => d.data?.name ?? '',
          labelPlacement: 'bottom',
          labelFontSize: 11,
          labelFill: '#303133',
          cursor: 'pointer',
        },
      },
      edge: {
        style: {
          stroke: '#c0c4cc',
          // Thicker edge = more co-occurrences. Clamp between 1-6 so a
          // single hot pair doesn't wash out everything else.
          lineWidth: (d: {data?: {weight?: number}}) => Math.max(1, Math.min(6, Math.log2((d.data?.weight ?? 1) + 1))),
          endArrow: false,
          labelText: (d: {data?: {weight?: number}}) => String(d.data?.weight ?? ''),
          labelFontSize: 10,
          labelFill: '#909399',
        },
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    });
    graph.render();
  };

  defineExpose({refresh: load});
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
