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
import MatrixStatusSegmented from '@/components/segmented/MatrixStatusSegmented.vue';

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
        :data-value="option.value"
        @click="$emit('update:modelValue', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `,
};

function mountSegmented(props: Record<string, unknown> = {}) {
  return mount(MatrixStatusSegmented, {
    props,
    global: {
      plugins: [i18n],
      stubs: {ElSegmented: ElSegmentedStub},
    },
  });
}

describe('MatrixStatusSegmented', () => {
  it('renders all matrix status options', () => {
    const wrapper = mountSegmented({modelValue: '', size: 'small'});
    const buttons = wrapper.findAll('button');

    expect(wrapper.find('.segmented-stub').attributes('data-size')).toBe('small');
    expect(buttons.map((button) => button.attributes('data-value'))).toEqual([
      '',
      'missing',
      'configured',
      'dirty',
      'error',
    ]);
    expect(wrapper.text()).toContain('All');
    expect(wrapper.text()).toContain('Configured');
  });

  it('emits selected matrix status', async () => {
    const wrapper = mountSegmented({modelValue: ''});

    await wrapper.findAll('button')[3].trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([['dirty']]);
  });
});
