<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <div ref="containerRef" :style="{height: `${height}px`}" class="mini-area-chart"></div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {onMounted, onUnmounted, ref, watch} from 'vue';
import {Chart} from '@antv/g2';

/**
 * A small area+line chart used by stat cards and the point-value history
 * card. Backed by @antv/g2 v5. Consolidates the shared setup the two
 * callers used to duplicate:
 *   - CSS-string gradient fill (G2 v5 rejects descriptor-object form,
 *     throws `colorStr.indexOf is not a function`)
 *   - rAF-wrapped chart construction so autoFit picks up a real width
 *     even when the parent grid is still laying out
 *   - flush:'post' + immediate watch so the first paint happens after
 *     Vue has patched the DOM
 */

const props = defineProps({
  /** Y-series. Numeric values; null/NaN entries are treated as 0. */
  data: {
    type: Array as PropType<number[]>,
    default: () => [],
  },
  /** Accent colour for the line stroke and the gradient's inner stop. */
  color: {
    type: String,
    default: 'var(--el-color-primary)',
  },
  /** Chart height in px. Container is stretched to the parent width. */
  height: {
    type: Number,
    default: 40,
  },
  /** When non-empty, enables a tooltip that renders "<value> <unit>". */
  tooltipUnit: {
    type: String,
    default: '',
  },
  /** Toggles the foreground line on top of the area. */
  showLine: {
    type: Boolean,
    default: true,
  },
  /** Toggles the fadeIn animation on first render / data change. */
  animate: {
    type: Boolean,
    default: false,
  },
});

const containerRef = ref<HTMLElement>();
let chart: Chart | undefined;

const resolveCssColor = (color: string) => {
  const match = color.match(/^var\((--[^),]+)(?:,[^)]+)?\)$/);
  const token = match?.[1];
  if (!token) return color;
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim() || color;
};

const draw = (attempt = 0) => {
  const el = containerRef.value;
  if (!el || !props.data || props.data.length === 0) return;
  requestAnimationFrame(() => {
    const node = containerRef.value;
    if (!node) return;
    // autoFit reads the container's offsetWidth once at construction time.
    // When the parent layout hasn't settled yet (e.g. a grid cell that
    // hasn't finished sizing on the first paint), the chart silently
    // renders with zero width. Retry a couple of frames before giving up.
    if (node.offsetWidth === 0 && attempt < 5) {
      draw(attempt + 1);
      return;
    }
    const points = props.data.map((y, i) => ({x: i, y: Number.isFinite(y) ? y : 0}));

    chart?.destroy();
    chart = new Chart({
      container: node,
      autoFit: true,
      height: props.height,
      paddingTop: 2,
      paddingBottom: 2,
      paddingLeft: 2,
      paddingRight: 2,
    });

    const color = resolveCssColor(props.color);
    const fillGradient = `linear-gradient(90deg, rgba(255,255,255,0) 0%, ${color} 100%)`;

    const area = chart
      .area()
      .data(points)
      .encode('x', 'x')
      .encode('y', 'y')
      .encode('shape', 'smooth')
      .scale('y', {zero: true})
      .style('fill', fillGradient)
      .style('fillOpacity', 0.3)
      .axis(false)
      .legend(false);
    if (props.animate) {
      area.animate('enter', {type: 'fadeIn'});
    }
    if (props.tooltipUnit) {
      chart.interaction('tooltip', {
        render: (_e: unknown, {items}: { items: Array<{ value: unknown }> }) => {
          const first = items[0];
          return first ? `${first.value} ${props.tooltipUnit}` : '';
        },
      });
    } else {
      area.tooltip(false);
    }

    if (props.showLine) {
      const line = chart
        .line()
        .data(points)
        .encode('x', 'x')
        .encode('y', 'y')
        .encode('shape', 'smooth')
        .style('stroke', color)
        .style('lineWidth', 2)
        .axis(false)
        .legend(false);
      if (!props.tooltipUnit) line.tooltip(false);
    }

    chart.render();
  });
};

// Initial paint triggers from onMounted (not from watch immediate:true).
// When the caller gates the component with v-if, the previous "immediate
// watch" pattern could fire before the <div> ref was ready in some
// mount orderings; splitting the two makes the lifecycle explicit.
onMounted(() => draw());
watch(
  () => [props.data, props.color, props.height, props.tooltipUnit, props.showLine],
  () => draw(),
  {deep: true, flush: 'post'}
);

onUnmounted(() => chart?.destroy());
</script>

<style lang="scss" scoped>
.mini-area-chart {
  width: 100%;
}
</style>
