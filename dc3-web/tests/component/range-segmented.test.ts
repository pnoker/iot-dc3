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
import {describe, expect, it} from 'vitest';

import i18n from '@/config/i18n';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

const ElSegmentedStub = {
  name: 'ElSegmented',
  props: ['modelValue', 'options', 'size'],
  emits: ['update:modelValue'],
  template: `
    <div class="segmented-stub" :data-size="size">
      <button
        v-for="option in options"
        :key="option.value || 'all'"
        type="button"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `,
};

function mountRange(props: Record<string, unknown> = {}) {
  return mount(RangeSegmented, {
    props: {
      modelValue: '24h',
      ...props,
    },
    global: {
      plugins: [i18n],
      stubs: {ElSegmented: ElSegmentedStub},
    },
  });
}

describe('RangeSegmented', () => {
  it('renders the standard time range options', () => {
    const wrapper = mountRange();

    expect(wrapper.text()).toContain('Today');
    expect(wrapper.text()).toContain('Last 24 hours');
    expect(wrapper.text()).toContain('Last 7 days');
    expect(wrapper.text()).toContain('Last 30 days');
    expect(wrapper.text()).not.toContain('All');
  });

  it('can include the all option and emits the selected range key', async () => {
    const wrapper = mountRange({includeAll: true, size: 'small'});
    const buttons = wrapper.findAll('button');

    expect(wrapper.text()).toContain('All');
    expect(wrapper.find('.segmented-stub').attributes('data-size')).toBe('small');

    await buttons[0].trigger('click');
    await buttons[3].trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[''], ['7d']]);
  });
});
