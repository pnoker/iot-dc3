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

import {flushPromises, mount} from '@vue/test-utils';
import {defineComponent, h, nextTick} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryHistory, createRouter} from 'vue-router';

import i18n from '@/config/i18n';
import AlarmNotify from '@/views/settings/alarm/AlarmNotify.vue';

import {layoutStubs} from '../setup/stubs/element-plus';

const elementMocks = vi.hoisted(() => ({
  confirm: vi.fn(() => Promise.resolve()),
}));

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus');
  return {
    ...actual,
    ElMessageBox: {
      ...actual.ElMessageBox,
      confirm: elementMocks.confirm,
    },
  };
});

const alarmMocks = vi.hoisted(() => {
  const listResponse = {items: [{id: 'alarm-row-1', ruleName: 'Cooling threshold'}], total: 1};
  const list = () => vi.fn(() => Promise.resolve(listResponse));
  const mutate = () => vi.fn(() => Promise.resolve(true));

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

const relationMocks = vi.hoisted(() => ({
  listDevice: vi.fn(() => Promise.resolve({items: [], total: 0})),
  listDriver: vi.fn(() => Promise.resolve({items: [], total: 0})),
  listPoint: vi.fn(() => Promise.resolve({items: [], total: 0})),
}));

vi.mock('@/api/device', () => ({listDevice: relationMocks.listDevice}));
vi.mock('@/api/driver', () => ({listDriver: relationMocks.listDriver}));
vi.mock('@/api/point', () => ({listPoint: relationMocks.listPoint}));

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

const ResponsiveRecordListStub = defineComponent({
  name: 'ResponsiveRecordList',
  props: {
    columns: {type: Array, default: () => []},
    errorText: String,
    loading: Boolean,
    openable: Boolean,
    rows: {type: Array, default: () => []},
    status: String,
  },
  emits: ['open', 'retry'],
  setup(props, {emit, slots}) {
    return () =>
      h(
        'section',
        {
          'data-test': 'responsive-list',
          'data-status': props.status,
          'data-loading': String(props.loading),
        },
        [
          ...(props.status === 'error'
            ? [h('button', {'data-test': 'retry', type: 'button', onClick: () => emit('retry')}, props.errorText)]
            : []),
          ...(props.rows as Record<string, unknown>[]).map((row) =>
            h('article', {'data-test': 'alarm-row'}, [
              h('button', {'data-test': 'open-row', type: 'button', onClick: () => emit('open', row)}, String(row.id)),
              ...(slots.actions?.({mobile: false, row}) || []),
            ])
          ),
        ]
      );
  },
});

const ElDialogStub = defineComponent({
  name: 'ElDialog',
  props: {
    beforeClose: Function,
    closeOnPressEscape: Boolean,
    modelValue: Boolean,
    showClose: Boolean,
  },
  setup(props, {slots}) {
    return () =>
      h(
        'section',
        {
          class: 'el-dialog-stub',
          'data-visible': String(props.modelValue),
          'data-escape': String(props.closeOnPressEscape),
          'data-show-close': String(props.showClose),
        },
        [slots.default?.(), slots.footer?.()]
      );
  },
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: {disabled: Boolean, loading: Boolean},
  emits: ['click'],
  setup(props, {emit, slots}) {
    return () =>
      h(
        'button',
        {
          type: 'button',
          disabled: props.disabled,
          'data-loading': String(props.loading),
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      );
  },
});

const formSpies = {
  clearValidate: vi.fn(),
  validate: vi.fn(() => Promise.resolve(true)),
};

const ElFormStub = defineComponent({
  name: 'ElForm',
  props: ['model', 'rules'],
  setup(_, {expose, slots}) {
    expose(formSpies);
    return () => h('form', {class: 'el-form-stub'}, slots.default?.());
  },
});

const deferred = <T>() => {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return {promise, reject, resolve};
};

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
        ElAlert: {
          props: ['title'],
          template: '<aside class="el-alert-stub" :data-title="title"><slot /></aside>',
        },
        ElButton: ElButtonStub,
        ElDialog: ElDialogStub,
        ElForm: ElFormStub,
        ResponsiveRecordList: ResponsiveRecordListStub,
        ToolCard: ToolCardStub,
      },
    },
  });
}

describe('AlarmNotify view', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    elementMocks.confirm.mockResolvedValue(undefined);
    formSpies.validate.mockResolvedValue(true);
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
        offset: 0,
        limit: 12,
        sort: [{field: 'create_time', direction: 'DESC'}],
      })
    );
  });

  it('keeps runtime state pages read-only in the toolbar', async () => {
    const wrapper = mountAlarmNotify('state');
    await flushPromises();

    expect(alarmMocks.listRuleState).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).not.toContain(i18n.global.t('common.add'));
  });

  it('shows a recoverable list error and retries without losing the page contract', async () => {
    alarmMocks.listRule.mockRejectedValueOnce(new Error('offline'));
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();

    expect(wrapper.get('[data-test="responsive-list"]').attributes('data-status')).toBe('error');
    expect(wrapper.get('[data-test="retry"]').text()).toContain(i18n.global.t('common.loadFailed'));

    await wrapper.get('[data-test="retry"]').trigger('click');
    await flushPromises();

    expect(alarmMocks.listRule).toHaveBeenCalledTimes(2);
    expect(wrapper.get('[data-test="responsive-list"]').attributes('data-status')).toBe('success');
  });

  it('ignores an obsolete list response after switching alarm entities', async () => {
    const oldRequest = deferred<{items: Array<{id: string; ruleName: string}>; total: number}>();
    alarmMocks.listRule.mockReturnValueOnce(oldRequest.promise);
    alarmMocks.listNotify.mockResolvedValueOnce({items: [{id: 'notify-new', notifyName: 'Current'}], total: 1});

    const wrapper = mountAlarmNotify('rule');
    await wrapper.setProps({entity: 'notify'});
    await flushPromises();

    oldRequest.resolve({items: [{id: 'rule-old', ruleName: 'Obsolete'}], total: 1});
    await flushPromises();

    expect(wrapper.get('[data-test="alarm-row"]').text()).toContain('notify-new');
    expect(wrapper.text()).not.toContain('rule-old');
  });

  it('restores the original edit snapshot when resetting the form', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      formModel: Record<string, unknown>;
      openEdit: (row: Record<string, unknown>) => void;
      resetForm: () => void;
    };

    page.openEdit({id: 'alarm-row-1', ruleName: 'Original', ruleCode: 'rule.original'});
    await nextTick();
    page.formModel.ruleName = 'Changed';
    page.resetForm();

    expect(page.formModel.ruleName).toBe('Original');
    expect(page.formModel.ruleCode).toBe('rule.original');
    expect(formSpies.clearValidate).toHaveBeenCalled();
  });

  it('resetting the form keeps entityId and skips redundant remote loads', async () => {
    relationMocks.listPoint.mockResolvedValue({items: [{id: 'point-1', pointName: 'Temperature'}], total: 1});
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      formModel: Record<string, unknown>;
      openEdit: (row: Record<string, unknown>) => void;
      resetForm: () => void;
    };

    page.openEdit({id: 'alarm-row-1', ruleName: 'Original', alarmTargetTypeFlag: 'POINT', entityId: 'point-1'});
    await flushPromises();
    // One load comes from opening the form; a same-value reset must not fire
    // the target-type watcher again (it would clear entityId mid-assign and
    // depend on field ordering to write it back).
    const loadsOnOpen = relationMocks.listPoint.mock.calls.length;
    expect(loadsOnOpen).toBeGreaterThan(0);

    page.formModel.ruleName = 'Changed';
    page.resetForm();
    await nextTick();
    await flushPromises();

    expect(page.formModel.entityId).toBe('point-1');
    expect(relationMocks.listPoint.mock.calls.length).toBe(loadsOnOpen);
  });

  it('keeps a failed draft visible and prevents duplicate submissions', async () => {
    const saveRequest = deferred<boolean>();
    alarmMocks.updateRule.mockReturnValueOnce(saveRequest.promise);
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      formModel: Record<string, unknown>;
      formVisible: boolean;
      openEdit: (row: Record<string, unknown>) => void;
      state: {saveError: unknown; saving: boolean};
      submit: () => Promise<void>;
    };

    page.openEdit({id: 'alarm-row-1', ruleName: 'Original'});
    await nextTick();
    page.formModel.ruleName = 'Draft';
    void page.submit();
    void page.submit();
    await flushPromises();

    expect(alarmMocks.updateRule).toHaveBeenCalledTimes(1);
    expect(page.state.saving).toBe(true);

    saveRequest.reject(new Error('save failed'));
    await flushPromises();

    expect(page.formVisible).toBe(true);
    expect(page.formModel.ruleName).toBe('Draft');
    expect(page.state.saveError).toBeInstanceOf(Error);
    expect(wrapper.get('.el-alert-stub').attributes('data-title')).toBe(i18n.global.t('common.saveFailed'));
  });

  it('confirms before discarding a dirty dialog but closes a clean dialog immediately', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      formModel: Record<string, unknown>;
      formVisible: boolean;
      openAdd: () => void;
      requestCloseForm: (done?: () => void) => Promise<void>;
    };

    page.openAdd();
    await nextTick();
    await page.requestCloseForm();
    expect(elementMocks.confirm).not.toHaveBeenCalled();
    expect(page.formVisible).toBe(false);

    page.openAdd();
    page.formModel.ruleName = 'Unsaved';
    elementMocks.confirm.mockRejectedValueOnce(new Error('cancel'));
    await page.requestCloseForm();
    expect(page.formVisible).toBe(true);

    elementMocks.confirm.mockResolvedValueOnce(undefined);
    await page.requestCloseForm();
    expect(page.formVisible).toBe(false);
  });

  it('marks a delete in progress and suppresses duplicate delete requests', async () => {
    const deleteRequest = deferred<boolean>();
    alarmMocks.deleteRule.mockReturnValueOnce(deleteRequest.promise);
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      isDeleting: (row: Record<string, unknown>) => boolean;
      remove: (row: Record<string, unknown>) => void;
    };
    const row = {id: 'alarm-row-1'};

    page.remove(row);
    page.remove(row);

    expect(alarmMocks.deleteRule).toHaveBeenCalledTimes(1);
    expect(page.isDeleting(row)).toBe(true);

    deleteRequest.resolve(true);
    await flushPromises();
    expect(page.isDeleting(row)).toBe(false);
  });

  it('surfaces remote option failures and ignores obsolete dependent options', async () => {
    const obsoleteRequest = deferred<{items: Array<{id: string; pointName: string}>; total: number}>();
    relationMocks.listPoint.mockReturnValueOnce(obsoleteRequest.promise);
    relationMocks.listDevice.mockResolvedValueOnce({items: [{id: 'device-1', deviceName: 'Pump'}], total: 1});
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const page = wrapper.vm as {
      activeConfig: {fields: Array<{prop: string}>};
      formModel: Record<string, unknown>;
      loadRemote: (field: {prop: string}, force?: boolean) => Promise<void>;
      openAdd: () => void;
      remoteOptions: Record<string, Array<{label: string; value: string}>>;
      remoteState: Record<string, {error: boolean}>;
    };

    page.openAdd();
    await nextTick();
    const entityField = page.activeConfig.fields.find((field) => field.prop === 'entityId');
    expect(entityField).toBeDefined();

    page.formModel.alarmTargetTypeFlag = 'DEVICE';
    await flushPromises();
    obsoleteRequest.resolve({items: [{id: 'point-old', pointName: 'Obsolete point'}], total: 1});
    await flushPromises();

    expect(page.remoteOptions.entityId).toEqual([{label: 'Pump', value: 'device-1'}]);

    relationMocks.listDevice.mockRejectedValueOnce(new Error('offline'));
    await page.loadRemote(entityField!, true);
    expect(page.remoteState.entityId?.error).toBe(true);
  });

  it('derives a semantic mobile summary from the same alarm column schema', async () => {
    const wrapper = mountAlarmNotify('rule');
    await flushPromises();
    const list = wrapper.getComponent(ResponsiveRecordListStub);
    const columns = list.props('columns') as Array<{key: string; mobile?: string}>;

    expect(columns.find((column) => column.key === 'ruleName')?.mobile).toBe('primary');
    expect(columns.find((column) => column.key === 'alarmTargetTypeFlag')?.mobile).toBe('detail');
    expect(columns.find((column) => column.key === 'remark')?.mobile).toBe('hidden');
  });
});
