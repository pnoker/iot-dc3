/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Shared mounting helper for @antv/g2 charts.
 *
 * <p>G2's `autoFit` measures the container exactly once at construction and
 * silently falls back to a 640x480 canvas when the box is still empty (layout
 * race: route enter, loading→content swap, grid settle). The oversized canvas
 * overflows the card body and raises both scrollbars; a too-early small box
 * leaves the chart tiny and never fills the card. G2's own re-fit listens to
 * `window.resize` alone, so neither failure self-heals.</p>
 *
 * <p>`mountG2Chart` closes both gaps: it defers construction until the box has
 * a real size, then keeps the instance fitted to container resizes via
 * ResizeObserver + `forceFit()` (which re-reads the box without pinning
 * width/height into the chart options, unlike `changeSize`).</p>
 */

import type {Chart} from '@antv/g2';
import {Chart as G2Chart} from '@antv/g2';

/** The slice of the G2 chart API the size observer drives — structurally typed so test doubles stay cast-free. */
interface ForceFitTarget {
  forceFit: () => unknown;
}

/** Frames to wait for a non-empty box before constructing anyway (hidden tabs heal via the observer). */
const MAX_ZERO_SIZE_FRAMES = 5;

/**
 * True while the container has no laid-out box yet (percentage-height chain unresolved or display:none).
 * @param el - the chart container element
 * @returns whether the container currently measures 0 in either axis
 */
const isEmptyBox = (el: HTMLElement): boolean => el.clientWidth === 0 || el.clientHeight === 0;

/**
 * Keep a self-constructed G2 chart fitted to its container box.
 *
 * Use this when a component builds the chart itself (custom options or a
 * multi-step render pipeline); otherwise prefer {@link mountG2Chart}.
 *
 * @param container - the element the chart was constructed on
 * @param chart - the live chart instance
 * @returns disposer that stops observing; call it next to `chart.destroy()`
 */
export const observeChartSize = (container: HTMLElement, chart: ForceFitTarget): (() => void) => {
  let frame = 0;
  const fit = () => {
    cancelAnimationFrame(frame);
    frame = requestAnimationFrame(() => void chart.forceFit());
  };
  const observer = new ResizeObserver(fit);
  observer.observe(container);
  // The observer's initial notification coalesces with the layout settle,
  // so a chart that mounted on a half-finished box still snaps to size.
  fit();
  return () => {
    cancelAnimationFrame(frame);
    observer.disconnect();
  };
};

/**
 * Mount a G2 chart that always matches its container box.
 *
 * @param container - the element the chart renders into; must be the same node passed to G2
 * @param build - configures marks/interactions on the constructed chart and calls `chart.render()`
 * @param options - extra G2 chart options merged over the `{container, autoFit: true}` defaults
 * @returns disposer that cancels the pending mount and destroys the chart; call it before re-drawing
 */
export const mountG2Chart = (
  container: HTMLElement,
  build: (chart: Chart) => void,
  options: Record<string, unknown> = {}
): (() => void) => {
  let chart: Chart | undefined;
  let disposeFit: (() => void) | undefined;
  let frame = 0;
  let disposed = false;

  const mount = (attempt: number) => {
    if (disposed) return;
    if (isEmptyBox(container) && attempt < MAX_ZERO_SIZE_FRAMES) {
      frame = requestAnimationFrame(() => mount(attempt + 1));
      return;
    }
    chart = new G2Chart({container, autoFit: true, ...options});
    build(chart);
    disposeFit = observeChartSize(container, chart);
  };
  mount(0);

  return () => {
    disposed = true;
    cancelAnimationFrame(frame);
    disposeFit?.();
    disposeFit = undefined;
    chart?.destroy();
    chart = undefined;
  };
};
