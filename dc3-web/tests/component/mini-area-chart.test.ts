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

import {flushPromises, mount} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';
import MiniAreaChart from '@/components/chart/MiniAreaChart.vue';

const chartInstanceMocks = vi.hoisted(() => ({
  area: vi.fn(),
  line: vi.fn(),
  interaction: vi.fn(),
  render: vi.fn(),
  destroy: vi.fn(),
  ChartCtor: vi.fn(),
}));

vi.mock('@antv/g2', () => {
  // Builder-style chainable returned by chart.area() / chart.line().
  // Every method returns the same builder so `.encode().scale().style()`
  // chains work without recording a separate fixture per call.
  const builder = {
    data: vi.fn().mockReturnThis(),
    encode: vi.fn().mockReturnThis(),
    scale: vi.fn().mockReturnThis(),
    style: vi.fn().mockReturnThis(),
    axis: vi.fn().mockReturnThis(),
    legend: vi.fn().mockReturnThis(),
    animate: vi.fn().mockReturnThis(),
    tooltip: vi.fn().mockReturnThis(),
  };
  chartInstanceMocks.area.mockReturnValue(builder);
  chartInstanceMocks.line.mockReturnValue(builder);

  class Chart {
    area = chartInstanceMocks.area;
    line = chartInstanceMocks.line;
    interaction = chartInstanceMocks.interaction;
    render = chartInstanceMocks.render;
    destroy = chartInstanceMocks.destroy;

    constructor() {
      chartInstanceMocks.ChartCtor();
    }
  }

  return {Chart};
});

describe('MiniAreaChart', () => {
  it('does not construct a chart when data is empty', async () => {
    chartInstanceMocks.ChartCtor.mockClear();
    chartInstanceMocks.render.mockClear();

    mount(MiniAreaChart, {props: {data: []}});
    await flushPromises();
    // Wait one rAF tick — the component schedules draw via requestAnimationFrame.
    await new Promise((resolve) => requestAnimationFrame(() => resolve(null)));

    expect(chartInstanceMocks.ChartCtor).not.toHaveBeenCalled();
  });

  it('exposes the passed height as inline style on the container', () => {
    const wrapper = mount(MiniAreaChart, {props: {data: [], height: 80}});
    expect(wrapper.find('.mini-area-chart').attributes('style')).toContain('height: 80px');
  });
});
