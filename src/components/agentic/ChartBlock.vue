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
  <figure class="agentic-chart">
    <figcaption v-if="title" class="agentic-chart__title">{{ title }}</figcaption>
    <div ref="containerRef" class="agentic-chart__canvas"></div>
  </figure>
</template>

<script lang="ts" setup>
  import { Chart } from '@antv/g2';
  import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import type { ChartSpec } from './assistantContent';

  const props = defineProps<{
    kind: 'line' | 'area' | 'column';
    spec: ChartSpec;
  }>();

  const containerRef = ref<HTMLDivElement>();
  let chart: Chart | undefined;

  const title = ref(props.spec.title || '');

  const flattenData = (spec: ChartSpec) => {
    return spec.series.flatMap((series) =>
      series.data.map((point) => ({
        series: series.name || 'value',
        x: point[0],
        y: typeof point[1] === 'number' ? point[1] : Number(point[1]),
      }))
    );
  };

  const renderChart = () => {
    const container = containerRef.value;
    if (!container) {
      return;
    }
    if (chart) {
      chart.destroy();
      chart = undefined;
    }
    chart = new Chart({
      container,
      autoFit: true,
      height: 260,
      paddingTop: 16,
      paddingRight: 12,
      paddingBottom: 32,
      paddingLeft: 48,
    });

    const data = flattenData(props.spec);

    let mark;
    if (props.kind === 'line') {
      mark = chart.line();
    } else if (props.kind === 'area') {
      mark = chart.area();
    } else {
      mark = chart.interval();
    }
    mark
      .data(data)
      .encode('x', 'x')
      .encode('y', 'y')
      .encode('color', 'series')
      .axis('x', { title: props.spec.xLabel || null })
      .axis('y', { title: props.spec.yLabel || props.spec.unit || null })
      .legend(props.spec.series.length > 1 ? { color: { position: 'top' } } : false);

    if (props.spec.xType === 'time' || props.spec.xType === 'linear') {
      mark.scale('x', { type: props.spec.xType });
    }

    chart.render();
  };

  onMounted(renderChart);

  watch(
    () => props.spec,
    () => {
      title.value = props.spec.title || '';
      renderChart();
    },
    { deep: true }
  );

  onBeforeUnmount(() => {
    chart?.destroy();
    chart = undefined;
  });
</script>

<style lang="scss" scoped>
  .agentic-chart {
    margin: 8px 0;
    padding: 10px;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    background: #fbfdff;
  }

  .agentic-chart__title {
    margin-bottom: 6px;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }

  .agentic-chart__canvas {
    width: 100%;
    height: 260px;
  }
</style>
