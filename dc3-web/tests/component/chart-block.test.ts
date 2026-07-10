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

import {mount} from '@vue/test-utils';
import {nextTick} from 'vue';
import {describe, expect, it, vi} from 'vitest';
import ChartBlock from '@/components/agentic/ChartBlock.vue';
import type {AgenticVisualizationSpec} from '@/config/types';

vi.mock('@antv/g2', () => {
  const mark = {
    axis: vi.fn().mockReturnThis(),
    data: vi.fn().mockReturnThis(),
    encode: vi.fn().mockReturnThis(),
    legend: vi.fn().mockReturnThis(),
    scale: vi.fn().mockReturnThis(),
    style: vi.fn().mockReturnThis(),
    transform: vi.fn().mockReturnThis(),
  };
  const Chart = vi.fn(function (this: Record<string, unknown>) {
    this.area = vi.fn(() => mark);
    this.coordinate = vi.fn().mockReturnThis();
    this.destroy = vi.fn();
    this.interval = vi.fn(() => mark);
    this.line = vi.fn(() => mark);
    this.point = vi.fn(() => mark);
    this.render = vi.fn();
  });
  return {Chart};
});

describe('ChartBlock', () => {
  it('renders whitelisted stat charts with formatted labels', () => {
    const chart: AgenticVisualizationSpec = {
      type: 'stat',
      title: 'Point value summary',
      dataset: [{latest: 23.4567, numericCount: 12}],
      encode: {},
    };

    const wrapper = mount(ChartBlock, {props: {chart}});

    expect(wrapper.text()).toContain('Point value summary');
    expect(wrapper.text()).toContain('Latest');
    expect(wrapper.text()).toContain('23.457');
    expect(wrapper.text()).toContain('Numeric Count');
  });

  it('renders structured annotations outside the chart canvas', async () => {
    const chart: AgenticVisualizationSpec = {
      type: 'line',
      title: 'Trend',
      dataset: [{index: 0, value: 20}],
      encode: {x: 'index', y: 'value'},
      annotations: [{type: 'y', label: 'Average', value: 20.1234}],
    };

    const wrapper = mount(ChartBlock, {props: {chart}});
    await nextTick();

    expect(wrapper.find('.agentic-chart__annotations').exists()).toBe(true);
    expect(wrapper.text()).toContain('Average');
    expect(wrapper.text()).toContain('20.123');
  });

  it('rejects unsupported chart types through the safe whitelist', () => {
    // @ts-expect-error — `type: 'custom'` is intentionally outside the
    // AgenticVisualizationSpec union to verify the whitelist guard.
    const chart: AgenticVisualizationSpec = {
      type: 'custom',
      dataset: [{x: 1, y: 2}],
      encode: {x: 'x', y: 'y'},
    };

    const wrapper = mount(ChartBlock, {props: {chart}});

    expect(wrapper.text()).toContain('Chart data is unavailable.');
  });
});
