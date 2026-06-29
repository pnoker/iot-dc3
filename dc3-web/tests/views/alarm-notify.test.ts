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

import {flushPromises, mount} from '@vue/test-utils';
import {defineComponent, h} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryHistory, createRouter} from 'vue-router';

import i18n from '@/config/i18n';
import AlarmNotify from '@/views/settings/alarm/AlarmNotify.vue';

import {createElButtonStub, createElFormStub, layoutStubs} from '../setup/stubs/element-plus';

const alarmMocks = vi.hoisted(() => {
  const listResponse = {data: {records: [{id: 'alarm-row-1', ruleName: 'Cooling threshold'}], total: 1}};
  const list = () => vi.fn(() => Promise.resolve(listResponse));
  const mutate = () => vi.fn(() => Promise.resolve({data: true}));

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
    listMessage: list(),
    listNotifyChannelBind: list(),
    listNotifyChannel: list(),
    listNotify: list(),
    listNotifyHistory: list(),
    listRule: list(),
    listRuleState: list(),
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
  setup(_, {emit, slots}) {
    return () =>
      h('section', {'data-test': 'tool-card'}, [
        h('div', {'data-test': 'filters'}, slots.filters?.()),
        h('div', {'data-test': 'actions'}, slots.actions?.()),
        h(
          'button',
          {
            'data-test': 'search',
            type: 'button',
            onClick: () => emit('search', {keyword: 'Cooling threshold', filterValue: 'ENABLE'}),
          },
          'Search'
        ),
      ]);
  },
});

function makeTestRouter() {
  const noop = defineComponent({render: () => null});
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {name: 'home', path: '/', component: noop},
      {name: 'alarm-rule', path: '/alarm/rule', component: noop},
      {name: 'alarm-notify', path: '/alarm/notify', component: noop},
      {name: 'alarm-state', path: '/alarm/state', component: noop},
    ],
  });
}

function mountAlarmNotify(entity: 'rule' | 'notify' | 'state' = 'rule') {
  const {ElForm} = createElFormStub();
  return mount(AlarmNotify, {
    props: {entity},
    global: {
      plugins: [i18n, makeTestRouter()],
      directives: {
        loading: () => undefined,
      },
      stubs: {
        ...layoutStubs,
        BlankCard: {template: '<section class="blank-card-stub"><slot /></section>'},
        ElForm,
        ToolCard: ToolCardStub,
        ElButton: createElButtonStub(),
      },
    },
  });
}

describe('AlarmNotify view', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads only the current routed alarm entity', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();

    expect(alarmMocks.listRule).toHaveBeenCalledTimes(1);
    expect(alarmMocks.listNotify).not.toHaveBeenCalled();
    expect(alarmMocks.listRuleState).not.toHaveBeenCalled();

    await wrapper.setProps({entity: 'notify'});
    await flushPromises();

    expect(alarmMocks.listRule).toHaveBeenCalledTimes(1);
    expect(alarmMocks.listNotify).toHaveBeenCalledTimes(1);
    expect(alarmMocks.listRuleState).not.toHaveBeenCalled();
  });

  it('maps toolbar search values to the active entity query fields', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();

    await wrapper.find('[data-test="search"]').trigger('click');
    await flushPromises();

    expect(alarmMocks.listRule).toHaveBeenLastCalledWith(
      expect.objectContaining({
        ruleName: 'Cooling threshold',
        enableFlag: 'ENABLE',
        page: expect.objectContaining({current: 1, size: 12}),
      })
    );
  });

  it('keeps runtime state pages read-only in the toolbar', async () => {
    const wrapper = mountAlarmNotify('state');
    await flushPromises();

    expect(alarmMocks.listRuleState).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).not.toContain(i18n.global.t('common.add'));
  });
});
