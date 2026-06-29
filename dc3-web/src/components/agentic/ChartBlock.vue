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
  <figure v-if="normalizedChart" class="agentic-chart">
    <figcaption v-if="normalizedChart.title" class="agentic-chart__title">{{ normalizedChart.title }}</figcaption>
    <p v-if="normalizedChart.description" class="agentic-chart__description">{{ normalizedChart.description }}</p>
    <div v-if="isStatChart" class="agentic-chart__stats">
      <div v-for="item in statItems" :key="item.label" class="agentic-chart__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <div v-else-if="canRenderG2Chart" ref="containerRef" class="agentic-chart__canvas"></div>
    <div v-else class="agentic-chart__empty">Chart data is unavailable.</div>
    <ul v-if="annotationItems.length" class="agentic-chart__annotations">
      <li v-for="item in annotationItems" :key="`${item.type}-${item.label}-${item.value}`">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </li>
    </ul>
  </figure>
  <figure v-else-if="hasChartInput" class="agentic-chart">
    <div class="agentic-chart__empty">Chart data is unavailable.</div>
  </figure>
</template>

<script lang="ts" setup>
  import {Chart} from '@antv/g2';
  import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
  import type {ChartSpec} from './assistantContent';
  import type {AgenticVisualizationSpec, AgenticVisualizationType} from '@/config/types';

  const props = defineProps<{
    kind?: 'line' | 'area' | 'column';
    spec?: ChartSpec;
    chart?: AgenticVisualizationSpec;
  }>();

  const containerRef = ref<HTMLDivElement>();
  let chartInstance: Chart | undefined;

  type NormalizedChart = Required<Pick<AgenticVisualizationSpec, 'type' | 'dataset' | 'encode'>> &
    Omit<AgenticVisualizationSpec, 'type' | 'dataset' | 'encode'>;

  interface ChartMark {
    data(value: unknown): ChartMark;

    encode(channel: string, field?: string): ChartMark;

    style(name: string, value: unknown): ChartMark;

    axis(channel: string, value: Record<string, unknown>): ChartMark;

    legend(channelOrValue: string | boolean | Record<string, unknown>, value?: Record<string, unknown>): ChartMark;

    scale(channel: string, value: Record<string, unknown>): ChartMark;
  }

  const supportedTypes = new Set<AgenticVisualizationType>([
    'line',
    'area',
    'column',
    'bar',
    'pie',
    'donut',
    'heatmap',
    'scatter',
    'stat',
  ]);

  const normalizedChart = computed<NormalizedChart | undefined>(() => {
    if (props.chart) {
      return normalizeStructuredChart(props.chart);
    }
    if (props.kind && props.spec) {
      return normalizeLegacyChart(props.kind, props.spec);
    }
    return undefined;
  });

  const hasChartInput = computed(() => Boolean(props.chart || (props.kind && props.spec)));

  const isStatChart = computed(() => normalizedChart.value?.type === 'stat');

  const canRenderG2Chart = computed(() => {
    const current = normalizedChart.value;
    if (!current || current.type === 'stat' || current.dataset.length === 0) {
      return false;
    }
    if (current.type === 'pie' || current.type === 'donut') {
      return Boolean(current.encode.y && (current.encode.color || current.encode.x));
    }
    return Boolean(current.encode.x && current.encode.y);
  });

  const statItems = computed(() => {
    const current = normalizedChart.value;
    const row = current?.dataset[0] || {};
    return Object.entries(row)
      .filter(([, value]) => value !== null && value !== undefined)
      .slice(0, 6)
      .map(([label, value]) => ({label: formatLabel(label), value: formatValue(value)}));
  });

  const annotationItems = computed(() => {
    return (normalizedChart.value?.annotations || [])
      .filter((item) => item && item.value !== null && item.value !== undefined)
      .map((item) => ({
        type: item.type || 'annotation',
        label: item.label || formatLabel(item.type || 'annotation'),
        value: formatValue(item.value),
      }));
  });

  const normalizeLegacyChart = (kind: 'line' | 'area' | 'column', spec: ChartSpec): NormalizedChart | undefined => {
    const dataset = spec.series.flatMap((series) =>
      series.data.map((point) => ({
        series: series.name || 'value',
        x: point[0],
        y: typeof point[1] === 'number' ? point[1] : Number(point[1]),
      }))
    );
    return {
      id: spec.title,
      type: kind,
      title: spec.title,
      dataset,
      encode: {x: 'x', y: 'y', color: 'series'},
      scale: {x: spec.xType, y: 'linear'},
      meta: {unit: spec.unit, xLabel: spec.xLabel, yLabel: spec.yLabel},
    };
  };

  const normalizeStructuredChart = (spec: AgenticVisualizationSpec): NormalizedChart | undefined => {
    if (!supportedTypes.has(spec.type) || !Array.isArray(spec.dataset)) {
      return undefined;
    }
    return {
      ...spec,
      dataset: spec.dataset.filter((row) => row && typeof row === 'object'),
      encode: spec.encode || {},
    };
  };

  const renderChart = () => {
    const container = containerRef.value;
    const current = normalizedChart.value;
    if (!container || !current || !canRenderG2Chart.value) {
      destroyChart();
      return;
    }
    destroyChart();
    chartInstance = new Chart({
      container,
      autoFit: true,
      height: 260,
      paddingTop: 16,
      paddingRight: 12,
      paddingBottom: 32,
      paddingLeft: 48,
    });

    if (current.type === 'pie' || current.type === 'donut') {
      renderPieLikeChart(chartInstance, current);
    } else {
      renderCartesianChart(chartInstance, current);
    }

    chartInstance.render();
  };

  const renderCartesianChart = (target: Chart, current: NormalizedChart) => {
    const mark = createCartesianMark(target, current.type);
    if (current.type === 'bar') {
      target.coordinate({transform: [{type: 'transpose'}]});
    }
    mark.data(current.dataset).encode('x', current.encode.x).encode('y', current.encode.y);
    if (current.encode.color) {
      mark.encode('color', current.encode.color);
    }
    if (current.type === 'heatmap' && current.encode.color) {
      mark.style('inset', 2);
    }
    mark
      .axis('x', {title: axisTitle(current, 'x')})
      .axis('y', {title: axisTitle(current, 'y')})
      .legend(current.encode.color ? {color: {position: 'top'}} : false);
    applyScale(mark, current, 'x');
    applyScale(mark, current, 'y');
  };

  const renderPieLikeChart = (target: Chart, current: NormalizedChart) => {
    const colorField = current.encode.color || current.encode.x;
    target.coordinate({type: 'theta', outerRadius: 0.82, innerRadius: current.type === 'donut' ? 0.52 : 0});
    target
      .interval()
      .data(current.dataset)
      .transform({type: 'stackY'})
      .encode('y', current.encode.y)
      .encode('color', colorField)
      .legend('color', {position: 'bottom'});
  };

  const createCartesianMark = (target: Chart, type: AgenticVisualizationType): ChartMark => {
    if (type === 'line') return target.line() as unknown as ChartMark;
    if (type === 'area') return target.area() as unknown as ChartMark;
    if (type === 'scatter') return target.point() as unknown as ChartMark;
    if (type === 'heatmap') return (target as unknown as {cell: () => ChartMark}).cell();
    return target.interval() as unknown as ChartMark;
  };

  const axisTitle = (current: NormalizedChart, axis: 'x' | 'y') => {
    const label = current.meta?.[`${axis}Label`];
    if (typeof label === 'string') {
      return label;
    }
    if (axis === 'y' && typeof current.meta?.unit === 'string') {
      return current.meta.unit;
    }
    return null;
  };

  const applyScale = (
    mark: {scale: (channel: string, value: Record<string, unknown>) => void},
    current: NormalizedChart,
    axis: 'x' | 'y'
  ) => {
    const type = current.scale?.[axis];
    if (type === 'time' || type === 'linear') {
      mark.scale(axis, {type});
    }
  };

  const formatLabel = (value: string) => {
    return value
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/[_-]+/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  const formatValue = (value: unknown) => {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return new Intl.NumberFormat(undefined, {maximumFractionDigits: 3}).format(value);
    }
    return String(value);
  };

  const destroyChart = () => {
    chartInstance?.destroy();
    chartInstance = undefined;
  };

  onMounted(() => {
    nextTick(renderChart);
  });

  watch(
    normalizedChart,
    () => {
      nextTick(renderChart);
    },
    {deep: true}
  );

  onBeforeUnmount(destroyChart);
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

  .agentic-chart__description,
  .agentic-chart__empty {
    margin: 0 0 8px;
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
  }

  .agentic-chart__canvas {
    width: 100%;
    height: 260px;
  }

  .agentic-chart__stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
    gap: 6px;
  }

  .agentic-chart__stat {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    padding: 8px;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    background: #f8fafc;

    span,
    strong {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span {
      color: #64748b;
      font-size: 11px;
    }

    strong {
      color: #1f2937;
      font-size: 14px;
    }
  }

  .agentic-chart__annotations {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 8px 0 0;
    padding: 0;
    list-style: none;

    li {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      max-width: 100%;
      padding: 3px 6px;
      border: 1px solid #e2e8f0;
      border-radius: 4px;
      background: #f8fafc;
      color: #64748b;
      font-size: 11px;
      line-height: 1.4;
    }

    span,
    strong {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      color: #334155;
      font-weight: 600;
    }
  }
</style>
