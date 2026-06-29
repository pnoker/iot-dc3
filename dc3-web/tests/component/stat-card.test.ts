/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {flushPromises, mount} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import StatCard from '@/components/card/stat/StatCard.vue';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus'; // MiniAreaChart drags @antv/g2 in. We stub it because the StatCard

// MiniAreaChart drags @antv/g2 in. We stub it because the StatCard
// contract under test is "tone, formatted value, refresh button" — not
// the sparkline rendering.
vi.mock('@/components/chart/MiniAreaChart.vue', () => ({
  default: {
    name: 'MiniAreaChart',
    props: ['data', 'color', 'height'],
    template: '<div class="mini-chart-stub" :data-color="color" :data-points="JSON.stringify(data)" />',
  },
}));

function mountStat(props: Record<string, unknown> = {}) {
  return mount(StatCard, {
    props: {title: 'Devices', icon: 'IconStub', ...props},
    global: {
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
      },
    },
  });
}

describe('StatCard', () => {
  it('formats large numeric values with k / M shorthand and falls back to non-numeric strings', () => {
    expect(mountStat({value: 42}).find('.stat-card__value-text').text()).toBe('42');
    expect(mountStat({value: 1500}).find('.stat-card__value-text').text()).toBe('1.5k');
    expect(mountStat({value: 2_500_000}).find('.stat-card__value-text').text()).toBe('2.5M');
    expect(mountStat({value: 'N/A'}).find('.stat-card__value-text').text()).toBe('N/A');
  });

  it('selects the trend icon and direction class from the trend prop', () => {
    const up = mountStat({value: 10, trend: {direction: 'up', label: '+10%'}});
    expect(up.find('.stat-card__trend--up').exists()).toBe(true);
    expect(up.find('.stat-card__trend').text()).toContain('+10%');

    const down = mountStat({value: 10, trend: {direction: 'down', label: '-2%'}});
    expect(down.find('.stat-card__trend--down').exists()).toBe(true);

    const flat = mountStat({value: 10, trend: {direction: 'flat', label: '0%'}});
    expect(flat.find('.stat-card__trend--flat').exists()).toBe(true);
  });

  it('hides the refresh button when no onRefresh handler is provided', () => {
    const wrapper = mountStat();
    expect(wrapper.find('.stat-card__refresh').exists()).toBe(false);
  });

  it('runs the refresh handler and emits click on the card body', async () => {
    const onRefresh = vi.fn(() => Promise.resolve());
    const wrapper = mountStat({onRefresh});

    // Card click → emits 'click'.
    await wrapper.find('.stat-card').trigger('click');
    expect(wrapper.emitted('click')).toHaveLength(1);

    // Refresh button click → invokes the handler. @click.stop must keep
    // the click event from bubbling and double-firing.
    const refreshBtn = wrapper.find('.stat-card__refresh');
    expect(refreshBtn.exists()).toBe(true);
    await refreshBtn.trigger('click');
    await flushPromises();
    expect(onRefresh).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('click')).toHaveLength(1);
  });

  it('applies the tone-derived accent colour to the underlying sparkline', () => {
    const wrapper = mountStat({tone: 'green', sparkline: [1, 2, 3]});
    const chart = wrapper.find('.mini-chart-stub');
    expect(chart.attributes('data-color')).toBe('var(--el-color-success)');
    expect(chart.attributes('data-points')).toBe(JSON.stringify([1, 2, 3]));
  });
});
