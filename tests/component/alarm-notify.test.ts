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

import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import i18n from '@/config/i18n';
import AlarmNotify from '@/views/settings/alarm/AlarmNotify.vue';

const alarmMocks = vi.hoisted(() => {
  const listResponse = { data: { records: [{ id: 'alarm-row-1', ruleName: 'Cooling threshold' }], total: 1 } };
  const list = () => vi.fn(() => Promise.resolve(listResponse));
  const mutate = () => vi.fn(() => Promise.resolve({ data: true }));

  return {
    addMessage: mutate(),
    addNotify: mutate(),
    addNotifyChannel: mutate(),
    addNotifyChannelBind: mutate(),
    addRule: mutate(),
    deleteMessage: mutate(),
    deleteNotify: mutate(),
    deleteNotifyChannel: mutate(),
    deleteNotifyChannelBind: mutate(),
    deleteRule: mutate(),
    getMessageList: list(),
    getNotifyChannelBindList: list(),
    getNotifyChannelList: list(),
    getNotifyList: list(),
    getNotifyRecordList: list(),
    getRuleList: list(),
    getRuleStateList: list(),
    updateMessage: mutate(),
    updateNotify: mutate(),
    updateNotifyChannel: mutate(),
    updateNotifyChannelBind: mutate(),
    updateRule: mutate(),
  };
});

vi.mock('@/api/alarm', () => alarmMocks);

vi.mock('@/utils/notificationUtil', () => ({
  failMessage: vi.fn(),
  successMessage: vi.fn(),
}));

const ToolCardStub = defineComponent({
  name: 'ToolCard',
  props: ['formModel', 'page'],
  emits: ['search', 'reset', 'refresh', 'sort', 'size-change', 'current-change'],
  setup(_, { emit, slots }) {
    return () =>
      h('section', { 'data-test': 'tool-card' }, [
        h('div', { 'data-test': 'filters' }, slots.filters?.()),
        h('div', { 'data-test': 'actions' }, slots.actions?.()),
        h(
          'button',
          {
            'data-test': 'search',
            type: 'button',
            onClick: () => emit('search', { keyword: 'Cooling threshold', filterValue: 'ENABLE' }),
          },
          'Search'
        ),
      ]);
  },
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: ['type'],
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          class: [`el-button-stub`, props.type ? `is-${props.type}` : ''],
          type: 'button',
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      );
  },
});

function mountAlarmNotify(entity: 'rule' | 'notify' | 'state' = 'rule') {
  return mount(AlarmNotify, {
    props: { entity },
    global: {
      plugins: [i18n],
      directives: {
        loading: vi.fn(),
      },
      stubs: {
        BlankCard: { template: '<section class="blank-card-stub"><slot /></section>' },
        ElCol: { template: '<div><slot /></div>' },
        ElDescriptions: { template: '<section><slot /></section>' },
        ElDescriptionsItem: { template: '<div><slot /></div>' },
        ElDialog: { template: '<section><slot /><slot name="footer" /></section>' },
        ElDrawer: { template: '<section><slot /></section>' },
        ElEmpty: { template: '<div />' },
        ElForm: { template: '<form><slot /></form>' },
        ElFormItem: { template: '<label><slot /></label>' },
        ElInput: { template: '<input />' },
        ElInputNumber: { template: '<input />' },
        ElOption: { template: '<option />' },
        ElPopconfirm: { template: '<span><slot name="reference" /></span>' },
        ElRow: { template: '<div><slot /></div>' },
        ElSelect: { template: '<select><slot /></select>' },
        ElTable: { template: '<section><slot /></section>' },
        ElTableColumn: { template: '<span />' },
        ElTag: { template: '<span><slot /></span>' },
        ToolCard: ToolCardStub,
        ElButton: ElButtonStub,
      },
    },
  });
}

describe('AlarmNotify', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads only the current routed alarm entity', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();

    expect(alarmMocks.getRuleList).toHaveBeenCalledTimes(1);
    expect(alarmMocks.getNotifyList).not.toHaveBeenCalled();
    expect(alarmMocks.getRuleStateList).not.toHaveBeenCalled();

    await wrapper.setProps({ entity: 'notify' });
    await flushPromises();

    expect(alarmMocks.getRuleList).toHaveBeenCalledTimes(1);
    expect(alarmMocks.getNotifyList).toHaveBeenCalledTimes(1);
    expect(alarmMocks.getRuleStateList).not.toHaveBeenCalled();
  });

  it('maps toolbar search values to the active entity query fields', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();

    await wrapper.find('[data-test="search"]').trigger('click');
    await flushPromises();

    expect(alarmMocks.getRuleList).toHaveBeenLastCalledWith(
      expect.objectContaining({
        ruleName: 'Cooling threshold',
        enableFlag: 'ENABLE',
        page: expect.objectContaining({ current: 1, size: 12 }),
      })
    );
  });

  it('keeps runtime state pages read-only in the toolbar', async () => {
    const wrapper = mountAlarmNotify('state');
    await flushPromises();

    expect(alarmMocks.getRuleStateList).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).not.toContain(i18n.global.t('common.add'));
  });
});
