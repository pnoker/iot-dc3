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

// g2ChartUtil tests — the mounting helper behind every dashboard chart. The
// @antv/g2 Chart is faked: what matters here is *when* a chart is constructed
// (zero-size layout race), that container resizes re-fit it, and that the
// disposer tears both sides down.

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {mountG2Chart, observeChartSize} from '@/utils/g2ChartUtil';

const {FakeChart, constructed} = vi.hoisted(() => {
  const constructed: Array<{options: Record<string, unknown>; forceFit: ReturnType<typeof vi.fn>; destroy: ReturnType<typeof vi.fn>}> = [];
  class FakeChart {
    options: Record<string, unknown>;
    forceFit = vi.fn(() => Promise.resolve());
    destroy = vi.fn();

    constructor(options: Record<string, unknown>) {
      this.options = options;
      constructed.push(this);
    }
  }
  return {FakeChart, constructed};
});

vi.mock('@antv/g2', () => ({Chart: FakeChart}));

const observers: Array<{callback: ResizeObserverCallback; observe: ReturnType<typeof vi.fn>; disconnect: ReturnType<typeof vi.fn>}> = [];

class CapturingResizeObserver {
  observe = vi.fn();
  disconnect = vi.fn();

  constructor(callback: ResizeObserverCallback) {
    observers.push({callback, observe: this.observe, disconnect: this.disconnect});
  }
}

const nextFrame = () => new Promise((resolve) => requestAnimationFrame(() => resolve(undefined)));

/** Build a fake container whose measured box is controlled by the test. */
const makeContainer = (width: number, height: number) => {
  const el = document.createElement('div');
  let w = width;
  let h = height;
  Object.defineProperties(el, {
    clientWidth: {get: () => w},
    clientHeight: {get: () => h},
  });
  return {
    el,
    setSize: (nextWidth: number, nextHeight: number) => {
      w = nextWidth;
      h = nextHeight;
    },
  };
};

beforeEach(() => {
  constructed.length = 0;
  observers.length = 0;
  vi.stubGlobal('ResizeObserver', CapturingResizeObserver);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('g2ChartUtil', () => {
  describe('mountG2Chart', () => {
    it('constructs immediately on a laid-out box and runs the build step', () => {
      const {el} = makeContainer(320, 200);
      const build = vi.fn();

      mountG2Chart(el, build);

      expect(constructed).toHaveLength(1);
      expect(constructed[0]!.options).toMatchObject({container: el, autoFit: true});
      expect(build).toHaveBeenCalledWith(constructed[0]);
      expect(observers[0]!.observe).toHaveBeenCalledWith(el);
    });

    it('merges extra chart options over the defaults', () => {
      const {el} = makeContainer(320, 40);

      mountG2Chart(el, () => undefined, {height: 40});

      expect(constructed[0]!.options).toMatchObject({container: el, autoFit: true, height: 40});
    });

    it('defers construction while the container has no box yet', async () => {
      const {el, setSize} = makeContainer(0, 0);

      mountG2Chart(el, () => undefined);
      expect(constructed).toHaveLength(0);

      setSize(320, 200);
      await nextFrame();

      expect(constructed).toHaveLength(1);
    });

    it('falls back to constructing after the retry budget on a hidden container', async () => {
      const {el} = makeContainer(0, 0);

      mountG2Chart(el, () => undefined);
      for (let i = 0; i < 6; i += 1) await nextFrame();

      expect(constructed).toHaveLength(1);
    });

    it('does not construct when disposed before the box appears', async () => {
      const {el} = makeContainer(0, 0);

      const dispose = mountG2Chart(el, () => undefined);
      dispose();
      await nextFrame();

      expect(constructed).toHaveLength(0);
    });

    it('destroys the chart and stops observing on dispose', () => {
      const {el} = makeContainer(320, 200);

      const dispose = mountG2Chart(el, () => undefined);
      dispose();

      expect(constructed[0]!.destroy).toHaveBeenCalledTimes(1);
      expect(observers[0]!.disconnect).toHaveBeenCalledTimes(1);
    });

    it('re-fits the chart when the container is resized', async () => {
      const {el} = makeContainer(320, 200);

      mountG2Chart(el, () => undefined);
      observers[0]!.callback([] as ResizeObserverEntry[], {} as ResizeObserver);
      await nextFrame();

      expect(constructed[0]!.forceFit).toHaveBeenCalled();
    });
  });

  describe('observeChartSize', () => {
    it('observes the container and force-fits on resize', async () => {
      const {el} = makeContainer(320, 200);
      const chart = new FakeChart({});

      const dispose = observeChartSize(el, chart);
      expect(observers[0]!.observe).toHaveBeenCalledWith(el);

      observers[0]!.callback([] as ResizeObserverEntry[], {} as ResizeObserver);
      await nextFrame();

      expect(chart.forceFit).toHaveBeenCalled();
      dispose();
      expect(observers[0]!.disconnect).toHaveBeenCalledTimes(1);
    });
  });
});
