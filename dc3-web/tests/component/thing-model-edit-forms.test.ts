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

import {flushPromises, mount, type VueWrapper} from '@vue/test-utils';
import {defineComponent, h, nextTick, type Component} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import i18n from '@/config/i18n';
import CommandEditForm from '@/views/settings/command/edit/CommandEditForm.vue';
import EventEditForm from '@/views/settings/event/definition/edit/EventEditForm.vue';

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

const apiMocks = vi.hoisted(() => ({
  listCommandParamByCommandId: vi.fn(() => Promise.resolve([])),
  listEventParamByEventId: vi.fn(() => Promise.resolve([])),
}));

vi.mock('@/api/command', () => ({listCommandParamByCommandId: apiMocks.listCommandParamByCommandId}));
vi.mock('@/api/event', () => ({listEventParamByEventId: apiMocks.listEventParamByEventId}));

const formSpies = {
  clearValidate: vi.fn(),
  validate: vi.fn(() => Promise.resolve(true)),
};

const ElFormStub = defineComponent({
  name: 'ElForm',
  setup(_, {expose, slots}) {
    expose(formSpies);
    return () => h('form', {class: 'el-form-stub'}, slots.default?.());
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
        slots.default?.()
      );
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

interface ExposedForm {
  addParamRow: () => void;
  reactiveData: {
    form: Record<string, unknown>;
    paramError: unknown;
    paramLoading: boolean;
    params: Array<Record<string, unknown>>;
    saveError: unknown;
    submitting: boolean;
    visible: boolean;
  };
  requestClose: (done?: () => void) => Promise<void>;
  retryParams: () => Promise<void>;
  show: (profileId?: string) => void;
  showEdit: (row: Record<string, unknown>) => void;
  submit: () => Promise<void>;
}

interface FormCase {
  component: Component;
  eventName: 'update-thing';
  formNameKey: 'commandName' | 'eventName';
  label: string;
  loader: typeof apiMocks.listCommandParamByCommandId;
  paramCode: string;
  paramName: string;
}

const cases: FormCase[] = [
  {
    component: CommandEditForm,
    eventName: 'update-thing',
    formNameKey: 'commandName',
    label: 'command',
    loader: apiMocks.listCommandParamByCommandId,
    paramCode: 'speed',
    paramName: 'Speed',
  },
  {
    component: EventEditForm,
    eventName: 'update-thing',
    formNameKey: 'eventName',
    label: 'event',
    loader: apiMocks.listEventParamByEventId,
    paramCode: 'temperature',
    paramName: 'Temperature',
  },
];

const mountForm = (component: Component) =>
  mount(component, {
    global: {
      plugins: [i18n],
      directives: {loading: () => undefined},
      stubs: {
        ...layoutStubs,
        ElAlert: {
          props: ['title'],
          template: '<aside class="el-alert-stub" :data-title="title"><slot /></aside>',
        },
        ElButton: ElButtonStub,
        ElDialog: ElDialogStub,
        ElForm: ElFormStub,
      },
    },
  });

const exposed = (wrapper: VueWrapper) => wrapper.vm as ExposedForm;

describe.each(cases)('$label edit form', (formCase) => {
  beforeEach(() => {
    vi.clearAllMocks();
    elementMocks.confirm.mockResolvedValue(undefined);
    formSpies.validate.mockResolvedValue(true);
    apiMocks.listCommandParamByCommandId.mockResolvedValue([]);
    apiMocks.listEventParamByEventId.mockResolvedValue([]);
  });

  it('renders parameter drafts as responsive form rows instead of a horizontal table', async () => {
    const wrapper = mountForm(formCase.component);
    const form = exposed(wrapper);

    form.show();
    form.addParamRow();
    await nextTick();

    expect(wrapper.find('.param-editor__row').exists()).toBe(true);
    expect(wrapper.find('.el-table-stub').exists()).toBe(false);
  });

  it('isolates parameter requests and exposes a retryable loading failure', async () => {
    const obsolete = deferred<Array<Record<string, unknown>>>();
    formCase.loader.mockReturnValueOnce(obsolete.promise);
    formCase.loader.mockResolvedValueOnce([
      {id: 'param-current', paramName: formCase.paramName, paramCode: formCase.paramCode, version: 1},
    ]);
    const wrapper = mountForm(formCase.component);
    const form = exposed(wrapper);

    form.showEdit({id: 'entity-old', version: 1, [formCase.formNameKey]: 'Old'});
    form.showEdit({id: 'entity-current', version: 1, [formCase.formNameKey]: 'Current'});
    await flushPromises();
    obsolete.resolve([{id: 'param-old', paramName: 'Obsolete', paramCode: 'obsolete', version: 1}]);
    await flushPromises();

    expect(form.reactiveData.params[0]?.id).toBe('param-current');

    formCase.loader.mockRejectedValueOnce(new Error('offline'));
    await form.retryParams();
    expect(form.reactiveData.paramError).toBeInstanceOf(Error);
    expect(wrapper.findAll('.el-alert-stub').some((alert) => alert.attributes('data-title') === i18n.global.t('common.loadFailed'))).toBe(true);
  });

  it('preserves a failed draft, prevents duplicate submit, and guards dirty close', async () => {
    const wrapper = mountForm(formCase.component);
    const form = exposed(wrapper);

    form.showEdit({id: 'entity-1', version: 1, [formCase.formNameKey]: 'Original'});
    await flushPromises();
    form.reactiveData.form[formCase.formNameKey] = 'Draft';
    form.addParamRow();
    Object.assign(form.reactiveData.params[0]!, {
      paramName: formCase.paramName,
      paramCode: formCase.paramCode,
    });

    void form.submit();
    void form.submit();
    await flushPromises();

    const events = wrapper.emitted(formCase.eventName) || [];
    expect(events).toHaveLength(1);
    const done = events[0]?.at(-1) as ((close?: boolean) => void) | undefined;
    expect(done).toBeTypeOf('function');
    done?.(false);
    await nextTick();

    expect(form.reactiveData.visible).toBe(true);
    expect(form.reactiveData.form[formCase.formNameKey]).toBe('Draft');
    expect(form.reactiveData.saveError).toBeInstanceOf(Error);

    elementMocks.confirm.mockRejectedValueOnce(new Error('keep editing'));
    await form.requestClose();
    expect(form.reactiveData.visible).toBe(true);
    expect(elementMocks.confirm).toHaveBeenCalledTimes(1);

    elementMocks.confirm.mockResolvedValueOnce(undefined);
    await form.requestClose();
    expect(form.reactiveData.visible).toBe(false);
  });
});
