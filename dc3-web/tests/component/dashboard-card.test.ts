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

import {mount} from '@vue/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

const ElSegmentedStub = {
  name: 'ElSegmented',
  props: ['modelValue', 'options', 'size'],
  emits: ['update:modelValue'],
  template: `
    <div class="seg-stub">
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        :data-value="opt.value"
        @click="$emit('update:modelValue', opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>
  `,
};

function mountDashboard(props: Record<string, unknown> = {}, slots: Record<string, string> = {}) {
  return mount(DashboardCard, {
    props,
    slots,
    global: {
      // v-loading is a runtime Element Plus directive — stub it as a no-op
      // so vitest.setup's strict warning handler doesn't promote the
      // "Failed to resolve directive" warning into a thrown error.
      directives: {loading: {}},
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElSegmented: ElSegmentedStub,
      },
    },
  });
}

describe('DashboardCard', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the title and shows an empty placeholder when empty=true', () => {
    const wrapper = mountDashboard({title: 'Device trend', empty: true, emptyText: 'No samples'});

    expect(wrapper.find('.dashboard-card__title-text').text()).toBe('Device trend');
    expect(wrapper.find('.dashboard-card__empty').exists()).toBe(true);
    expect(wrapper.find('.dashboard-card__content').exists()).toBe(false);
  });

  it('emits refresh when the refresh button is clicked', async () => {
    const wrapper = mountDashboard({title: 'X'});

    await wrapper.find('button[data-icon="Refresh"]').trigger('click');
    expect(wrapper.emitted('refresh')).toHaveLength(1);
  });

  it('emits update:interval and starts polling refresh on segmented change', async () => {
    const wrapper = mountDashboard({
      title: 'X',
      autoRefresh: [
        {label: 'Off', value: 0},
        {label: '5s', value: 5000},
      ],
    });

    await wrapper.find('button[data-value="5000"]').trigger('click');
    expect(wrapper.emitted('update:interval')?.[0]).toEqual([5000]);

    // After the interval lands, we expect 'refresh' to fire once per tick.
    vi.advanceTimersByTime(5000);
    expect(wrapper.emitted('refresh')).toHaveLength(1);
    vi.advanceTimersByTime(5000);
    expect(wrapper.emitted('refresh')).toHaveLength(2);
  });

  it('hides the footer wrapper when no footer slot is provided, and shows it when present', () => {
    const noFooter = mountDashboard({title: 'X'});
    expect(noFooter.find('.dashboard-card__footer').exists()).toBe(false);

    const withFooter = mountDashboard({title: 'X'}, {footer: '<span class="ftr">last sync 5s ago</span>'});
    expect(withFooter.find('.dashboard-card__footer .ftr').exists()).toBe(true);
  });
});
