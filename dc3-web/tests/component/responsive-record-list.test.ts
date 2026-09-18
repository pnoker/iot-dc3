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
import {computed, defineComponent, h, inject, nextTick, provide, type Ref} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import i18n from '@/config/i18n';
import type {ResponsiveListColumn} from '@/config/types';

interface Row {
  id: string;
  name: string;
  status: string;
  internal: string;
}

const rows: Row[] = [{id: 'row-1', name: 'Gateway', status: 'healthy', internal: 'private'}];
const columns: ResponsiveListColumn<Row>[] = [
  {key: 'name', label: 'Name', mobile: 'primary'},
  {key: 'status', label: 'Status', kind: 'custom', mobile: 'detail'},
  {key: 'internal', label: 'Internal', mobile: 'hidden'},
];

const tableRowsKey = Symbol('tableRows');

const ElTable = defineComponent({
  props: {data: {type: Array, default: () => []}},
  setup(props, {slots}) {
    provide(tableRowsKey, computed(() => props.data as Row[]));
    return () => h('section', {class: 'el-table-stub'}, slots.default?.());
  },
});

const ElTableColumn = defineComponent({
  props: {label: String},
  setup(props, {slots}) {
    const tableRows = inject<Ref<Row[]>>(tableRowsKey);
    return () =>
      h(
        'section',
        {class: 'el-table-column-stub', 'data-label': props.label},
        tableRows?.value.flatMap((row) => slots.default?.({row}) || []) || [],
      );
  },
});

const ElButton = defineComponent({
  props: {disabled: Boolean, loading: Boolean},
  emits: ['click'],
  setup(props, {attrs, emit, slots}) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: 'button',
          disabled: props.disabled,
          'data-loading': String(props.loading),
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.(),
      );
  },
});

const installViewport = (width: number) => {
  const matchMedia = vi.fn((query: string) => {
    const values = query.match(/\d+(?:\.\d+)?px/g)?.map((value) => Number.parseFloat(value)) || [];
    const matches = values.length === 2
      ? width >= values[0]! && width <= values[1]!
      : query.startsWith('(max-width')
        ? width <= values[0]!
        : query.startsWith('(min-width')
          ? width >= values[0]!
          : false;
    return {
      matches,
      media: query,
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    };
  });
  Object.defineProperty(window, 'matchMedia', {configurable: true, writable: true, value: matchMedia});
};

const mountList = (props: Record<string, unknown> = {}) =>
  mount(ResponsiveRecordList<Row>, {
    props: {rows, columns, ...props},
    slots: {
      'cell-status': ({row, mobile}: {row: Row; mobile: boolean}) =>
        h('span', {class: 'status-slot', 'data-mobile': String(mobile)}, row.status),
      actions: ({row, mobile}: {row: Row; mobile: boolean}) =>
        h('button', {class: 'action-slot', 'data-mobile': String(mobile)}, `Edit ${row.name}`),
    },
    global: {
      plugins: [i18n],
      directives: {loading: () => undefined},
      stubs: {
        BlankCard: {template: '<section class="blank-card-stub"><slot /></section>'},
        ElAlert: {
          props: ['title'],
          template: '<aside class="el-alert-stub" :data-title="title"><slot /></aside>',
        },
        ElButton,
        ElEmpty: {props: ['description'], template: '<div class="el-empty-stub" :data-description="description" />'},
        ElTable,
        ElTableColumn,
        ElTag: {template: '<span class="el-tag-stub"><slot /></span>'},
      },
    },
  });

describe('ResponsiveRecordList', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'en';
  });

  it('renders the shared column schema as a desktop table', async () => {
    installViewport(1440);
    const wrapper = mountList();
    await nextTick();

    expect(wrapper.find('.el-table-stub').exists()).toBe(true);
    expect(wrapper.find('.responsive-record-list__cards').exists()).toBe(false);
    expect(wrapper.findAll('.el-table-column-stub').map((column) => column.attributes('data-label'))).toEqual([
      'Name',
      'Status',
      'Internal',
      'Operation',
    ]);
    expect(wrapper.get('.status-slot').attributes('data-mobile')).toBe('false');
    expect(wrapper.get('.action-slot').attributes('data-mobile')).toBe('false');
  });

  it('rebuilds the same schema as an accessible mobile summary card', async () => {
    installViewport(393);
    const wrapper = mountList({openable: true});
    await nextTick();

    expect(wrapper.find('.el-table-stub').exists()).toBe(false);
    expect(wrapper.findAll('.responsive-record-list__card')).toHaveLength(1);
    expect(wrapper.text()).toContain('Gateway');
    expect(wrapper.text()).toContain('Status');
    expect(wrapper.text()).not.toContain('Internal');
    expect(wrapper.text()).not.toContain('private');
    expect(wrapper.get('.status-slot').attributes('data-mobile')).toBe('true');
    expect(wrapper.get('.action-slot').attributes('data-mobile')).toBe('true');

    const openButton = wrapper.get('button[aria-label="Detail: Gateway"]');
    await openButton.trigger('click');
    expect(wrapper.emitted('open')).toEqual([[rows[0]]]);
  });

  it('uses summary cards on tablet so fixed operation columns cannot cover data', async () => {
    installViewport(834);
    const wrapper = mountList({openable: true});
    await nextTick();

    expect(wrapper.find('.el-table-stub').exists()).toBe(false);
    expect(wrapper.findAll('.responsive-record-list__card')).toHaveLength(1);
    expect(wrapper.get('.status-slot').attributes('data-mobile')).toBe('true');
  });

  it('exposes loading and recoverable error states on mobile', async () => {
    installViewport(393);
    const loadingWrapper = mountList({rows: [], loading: true, status: 'loading'});
    await nextTick();
    expect(loadingWrapper.get('[role="status"]').attributes('aria-label')).toBe('Loading...');
    expect(loadingWrapper.findAll('.responsive-record-list__skeleton')).toHaveLength(3);

    const errorWrapper = mountList({rows: [], status: 'error', errorText: 'Network unavailable'});
    await nextTick();
    expect(errorWrapper.get('.el-alert-stub').attributes('data-title')).toBe('Network unavailable');
    expect(errorWrapper.get('.el-empty-stub').attributes('data-description')).toBe('Network unavailable');
    await errorWrapper.get('.el-alert-stub button').trigger('click');
    expect(errorWrapper.emitted('retry')).toHaveLength(1);
  });
});
