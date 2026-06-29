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
import ToolCard from '@/components/card/tool/ToolCard.vue';

import {createElButtonStub, createElFormStub, createElPaginationStub, layoutStubs} from '../setup/stubs/element-plus';

function mountToolCard(props: Record<string, unknown> = {}) {
  const {ElForm, validate, resetFields} = createElFormStub();

  const wrapper = mount(ToolCard, {
    props: {
      formModel: {keyword: 'device'},
      page: {current: 1, size: 12, total: 36},
      ...props,
    },
    slots: {
      filters: '<div class="filter-slot">filters</div>',
      actions: '<button type="button" class="add-action">Add</button>',
    },
    global: {
      plugins: [i18n],
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElForm,
        ElPagination: createElPaginationStub(),
      },
    },
  });

  return {wrapper, validate, resetFields};
}

describe('ToolCard', () => {
  it('validates before search and emits the live form model when the search button is clicked', async () => {
    const formModel = {keyword: 'device'};
    const {wrapper, validate} = mountToolCard({formModel});

    // Trigger via the rendered Search button — testing the public contract,
    // not internal vm methods. The Search button is the only `type=primary`
    // button in the footer's button cluster.
    const primaryButtons = wrapper.findAll('button.el-button-stub.is-primary');
    expect(primaryButtons).toHaveLength(1);
    await primaryButtons[0].trigger('click');
    await Promise.resolve();

    expect(validate).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('search')).toEqual([[formModel]]);
  });

  it('resets form fields and forwards pagination/tool events from button clicks', async () => {
    const {wrapper, resetFields} = mountToolCard();

    // Reset button is the second non-circle, non-primary button in the footer
    // (Search comes first). Find by data-icon on its leading icon prop.
    await wrapper.find('[data-icon="RefreshRight"]').trigger('click');
    await wrapper.find('.el-pagination-stub button:nth-of-type(1)').trigger('click');
    await wrapper.find('.el-pagination-stub button:nth-of-type(2)').trigger('click');
    await wrapper.find('[data-icon="Refresh"]').trigger('click');
    await wrapper.find('[data-icon="Sort"]').trigger('click');

    expect(resetFields).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('reset')).toHaveLength(1);
    expect(wrapper.emitted('size-change')).toEqual([[24]]);
    expect(wrapper.emitted('current-change')).toEqual([[3]]);
    expect(wrapper.emitted('refresh')).toHaveLength(1);
    expect(wrapper.emitted('sort')).toHaveLength(1);
  });

  it('honors the hideSort contract', () => {
    const {wrapper} = mountToolCard({hideSort: true});

    expect(wrapper.find('[data-icon="Sort"]').exists()).toBe(false);
    expect(wrapper.find('[data-icon="Refresh"]').exists()).toBe(true);
  });
});
