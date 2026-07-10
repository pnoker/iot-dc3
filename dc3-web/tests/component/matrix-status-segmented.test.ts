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
