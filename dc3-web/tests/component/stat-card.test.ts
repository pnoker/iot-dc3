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
