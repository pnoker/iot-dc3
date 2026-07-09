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
