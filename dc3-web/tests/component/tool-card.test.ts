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
import {afterEach, describe, expect, it, vi} from 'vitest';

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

// Wall-managed pages (page.sizeAuto boolean) replace the built-in sizes
// entry with the explicit adaptive/manual select. Table pages omit the
// flag and must render the classic pager untouched.
describe('ToolCard wall-managed size mode', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const ElSelectStub = {
    name: 'ElSelect',
    props: ['modelValue'],
    emits: ['change'],
    template: '<select class="el-select-stub" :data-model="modelValue"><slot /></select>',
  };

  const ElOptionStub = {
    name: 'ElOption',
    props: ['label', 'value'],
    template: '<option class="el-option-stub" :data-value="value">{{ label }}</option>',
  };

  const mountManaged = (
    page: Record<string, unknown>,
    desktop = true,
    extraProps: Record<string, unknown> = {},
  ) => {
    // useBreakpoint derives from matchMedia: desktop matches the lg/xl
    // min-width queries, thumb terminals match nothing (xs fallback).
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: desktop && (query.includes('min-width: 1200px') || query.includes('min-width: 1920px')),
      media: query,
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));
    const {ElForm} = createElFormStub();
    return mount(ToolCard, {
      props: {
        formModel: {},
        page,
        ...extraProps,
      },
      global: {
        plugins: [i18n],
        stubs: {
          ...layoutStubs,
          ElButton: createElButtonStub(),
          ElForm,
          ElPagination: createElPaginationStub(),
          ElSelect: ElSelectStub,
          ElOption: ElOptionStub,
        },
      },
    });
  };

  it('renders the adaptive select with the computed size label and emits page-size-mode', async () => {
    const wrapper = mountManaged({current: 1, size: 10, total: 12, sizeAuto: true});

    // Built-in sizes entry is gone; the adaptive select owns size choice.
    expect(wrapper.find('.el-pagination-stub').attributes('data-layout')).not.toContain('sizes');
    const select = wrapper.find('.el-select-stub');
    expect(select.attributes('data-model')).toBe('auto');
    const options = wrapper.findAll('.el-option-stub');
    expect(options[0].attributes('data-value')).toBe('auto');
    expect(options[0].text()).toContain('10');
    expect(options.map((o) => o.attributes('data-value'))).toContain('24');
    // The auto-derived density is pinnable without hunting for a preset.
    expect(options.map((o) => o.attributes('data-value'))).toContain('10');

    const selectVm = wrapper.findComponent({name: 'ElSelect'});
    selectVm.vm.$emit('change', 'auto');
    await Promise.resolve();
    expect(wrapper.emitted('page-size-mode')).toEqual([['auto']]);

    selectVm.vm.$emit('change', 24);
    await Promise.resolve();
    expect(wrapper.emitted('page-size-mode')).toEqual([['auto'], [24]]);
  });

  it('shows the picked numeric size in manual mode', () => {
    const wrapper = mountManaged({current: 1, size: 24, total: 48, sizeAuto: false});

    const select = wrapper.find('.el-select-stub');
    expect(select.attributes('data-model')).toBe('24');
    const autoOption = wrapper.findAll('.el-option-stub')[0];
    expect(autoOption.text()).toBe('Auto');
  });

  it('keeps the classic pager byte-identical for unmanaged table pages', () => {
    const wrapper = mountManaged({current: 1, size: 12, total: 36});

    expect(wrapper.find('.el-select-stub').exists()).toBe(false);
    expect(wrapper.find('.el-pagination-stub').attributes('data-layout')).toContain('sizes');
  });

  it('hides the size control on thumb terminals even when managed', () => {
    const wrapper = mountManaged({current: 1, size: 10, total: 12, sizeAuto: true}, false);

    expect(wrapper.find('.el-select-stub').exists()).toBe(false);
    expect(wrapper.find('.el-pagination-stub').attributes('data-layout')).toBe('prev, pager, next');
  });

  it('keeps the size control beside the cursor prev/next buttons', () => {
    const wrapper = mountManaged(
      {current: 1, size: 10, total: 12, sizeAuto: true},
      true,
      {cursorMode: true, cursorNext: true},
    );

    // Size choice is orthogonal to cursor paging: the wall-managed select
    // sits beside prev/next so the cursor wall is not the only one without
    // an adaptive/manual control.
    expect(wrapper.find('.el-select-stub').exists()).toBe(true);
    expect(wrapper.find('.el-select-stub').attributes('data-model')).toBe('auto');
    expect(wrapper.find('.el-pagination-stub').exists()).toBe(false);
    // The cursor buttons must actually render too — an if/else chain in the
    // footer drops history paging on desktop while this test stays green.
    const labels = wrapper.findAll('button').map((button) => button.text());
    expect(labels).toEqual(expect.arrayContaining(['Previous page', 'Next page']));
  });

  it('still pages with the cursor buttons when the select is hidden on thumb terminals', () => {
    const wrapper = mountManaged(
      {current: 1, size: 10, total: 12, sizeAuto: true},
      false,
      {cursorMode: true, cursorNext: true},
    );

    expect(wrapper.find('.el-select-stub').exists()).toBe(false);
    const labels = wrapper.findAll('button').map((button) => button.text());
    expect(labels).toEqual(expect.arrayContaining(['Previous page', 'Next page']));
  });
});
