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
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

// The segmented stub mirrors el-segmented's emit shape so we can drive
// the option click via a real DOM event instead of poking wrapper.vm.
const ElSegmentedStub = {
  name: 'ElSegmented',
  props: ['modelValue', 'options', 'size'],
  emits: ['update:modelValue'],
  template: `
    <div class="el-segmented-stub" :data-size="size">
      <button
        v-for="option in options"
        :key="String(option.value)"
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
  return mount(EnableFlagSegmented, {
    props,
    global: {
      plugins: [i18n],
      stubs: {ElSegmented: ElSegmentedStub},
    },
  });
}

describe('EnableFlagSegmented', () => {
  it('uses string-coded ENABLE/DISABLE options by default', () => {
    const wrapper = mountSegmented({modelValue: 'ENABLE'});
    const buttons = wrapper.findAll('button');
    expect(buttons).toHaveLength(2);
    expect(buttons[0].attributes('data-value')).toBe('ENABLE');
    expect(buttons[1].attributes('data-value')).toBe('DISABLE');
  });

  it('switches to numeric 0/1 options when valueType=number', () => {
    const wrapper = mountSegmented({modelValue: 0, valueType: 'number'});
    const buttons = wrapper.findAll('button');
    expect(buttons[0].attributes('data-value')).toBe('0');
    expect(buttons[1].attributes('data-value')).toBe('1');
  });

  it('prepends an all option when includeAll=true', () => {
    const wrapper = mountSegmented({modelValue: '', includeAll: true});
    const buttons = wrapper.findAll('button');
    expect(buttons).toHaveLength(3);
    expect(buttons[0].attributes('data-value')).toBe('');
    expect(buttons[1].attributes('data-value')).toBe('ENABLE');
    expect(buttons[2].attributes('data-value')).toBe('DISABLE');
  });

  it('emits the chosen flag string via update:modelValue', async () => {
    const wrapper = mountSegmented({modelValue: 'ENABLE'});
    await wrapper.findAll('button')[1].trigger('click');
    expect(wrapper.emitted('update:modelValue')).toEqual([['DISABLE']]);
  });

  it('emits 1 (numeric) when DISABLE is chosen with valueType=number', async () => {
    const wrapper = mountSegmented({modelValue: 0, valueType: 'number'});
    await wrapper.findAll('button')[1].trigger('click');
    expect(wrapper.emitted('update:modelValue')).toEqual([[1]]);
  });

  it('keeps the empty all value when includeAll is used with numeric mode', async () => {
    const wrapper = mountSegmented({modelValue: '', includeAll: true, valueType: 'number'});
    await wrapper.findAll('button')[0].trigger('click');
    expect(wrapper.emitted('update:modelValue')).toEqual([['']]);
  });
});
