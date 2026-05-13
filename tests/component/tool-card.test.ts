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

import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it, vi } from 'vitest';

import i18n from '@/config/i18n';
import ToolCard from '@/components/card/tool/ToolCard.vue';

const formValidate = vi.fn(() => Promise.resolve());
const formResetFields = vi.fn();

const ElFormStub = defineComponent({
  name: 'ElForm',
  props: ['model', 'rules', 'inline'],
  setup(_, { expose, slots }) {
    expose({
      validate: formValidate,
      resetFields: formResetFields,
    });
    return () => h('form', { class: 'el-form-stub' }, slots.default?.());
  },
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: ['icon', 'plain', 'type', 'circle'],
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          type: 'button',
          class: ['el-button-stub', props.circle ? 'is-circle' : ''],
          'data-icon': typeof props.icon === 'object' && props.icon ? (props.icon as { name?: string }).name : '',
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      );
  },
});

const ElPaginationStub = defineComponent({
  name: 'ElPagination',
  props: ['currentPage', 'pageSize', 'pageSizes', 'total'],
  emits: ['size-change', 'current-change'],
  setup(props, { emit }) {
    return () =>
      h('div', { class: 'el-pagination-stub' }, [
        h('span', `${props.currentPage}/${props.pageSize}/${props.total}`),
        h('button', { type: 'button', onClick: () => emit('size-change', 24) }, 'size'),
        h('button', { type: 'button', onClick: () => emit('current-change', 3) }, 'page'),
      ]);
  },
});

function mountToolCard(props: Record<string, unknown> = {}) {
  formValidate.mockClear();
  formResetFields.mockClear();

  return mount(ToolCard, {
    props: {
      formModel: { keyword: 'device' },
      page: { current: 1, size: 12, total: 36 },
      ...props,
    },
    slots: {
      filters: '<div class="filter-slot">filters</div>',
      actions: '<button type="button" class="add-action">Add</button>',
    },
    global: {
      plugins: [i18n],
      stubs: {
        ElButton: ElButtonStub,
        ElCard: { template: '<section class="el-card-stub"><slot /></section>' },
        ElForm: ElFormStub,
        ElFormItem: { template: '<div class="el-form-item-stub"><slot /></div>' },
        ElPagination: ElPaginationStub,
        ElTooltip: { template: '<span class="el-tooltip-stub"><slot /></span>' },
      },
    },
  });
}

describe('ToolCard', () => {
  it('validates before search and emits the live form model', async () => {
    const formModel = { keyword: 'device' };
    const wrapper = mountToolCard({ formModel });

    await (wrapper.vm as unknown as { search: () => Promise<void> }).search();

    expect(formValidate).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('search')).toEqual([[formModel]]);
  });

  it('resets form fields and forwards pagination/tool events', async () => {
    const wrapper = mountToolCard();

    (wrapper.vm as unknown as { reset: () => void }).reset();
    await wrapper.find('.el-pagination-stub button:nth-of-type(1)').trigger('click');
    await wrapper.find('.el-pagination-stub button:nth-of-type(2)').trigger('click');
    await wrapper.find('[data-icon="Refresh"]').trigger('click');
    await wrapper.find('[data-icon="Sort"]').trigger('click');

    expect(formResetFields).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('reset')).toHaveLength(1);
    expect(wrapper.emitted('size-change')).toEqual([[24]]);
    expect(wrapper.emitted('current-change')).toEqual([[3]]);
    expect(wrapper.emitted('refresh')).toHaveLength(1);
    expect(wrapper.emitted('sort')).toHaveLength(1);
  });

  it('honors the hideSort contract', () => {
    const wrapper = mountToolCard({ hideSort: true });

    expect(wrapper.find('[data-icon="Sort"]').exists()).toBe(false);
    expect(wrapper.find('[data-icon="Refresh"]').exists()).toBe(true);
  });
});
