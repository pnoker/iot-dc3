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

import {createElButtonStub} from '../setup/stubs/element-plus';
import {createPinia, setActivePinia} from 'pinia';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgenticComposer from '@/components/agentic/AgenticComposer.vue';
import AgenticSessionsRail from '@/components/agentic/AgenticSessionsRail.vue';
import MessageItem from '@/components/agentic/MessageItem.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';

const apiMocks = vi.hoisted(() => ({
  completeAgenticChatCompletion: vi.fn(),
  confirmAgenticAction: vi.fn(),
  deleteAgenticSession: vi.fn(),
  listAgenticAttachments: vi.fn(),
  listAgenticMessages: vi.fn(),
  listAgenticModels: vi.fn(),
  listPendingAgenticActions: vi.fn(),
  listAgenticSessions: vi.fn(),
  rejectAgenticAction: vi.fn(),
  streamAgenticChatCompletion: vi.fn(),
  updateAgenticSession: vi.fn(),
  uploadAgenticAttachment: vi.fn(),
}));
vi.mock('@/api/agentic', () => apiMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), warnMessage: vi.fn()}));

const global = {
  plugins: [i18n, createPinia()],
  stubs: {
    ElButton: createElButtonStub(),
    ElDialog: {name: 'ElDialog', template: '<div class="el-dialog"><slot /><slot name="footer" /></div>'},
    ElDropdown: {name: 'ElDropdown', template: '<span><slot /><slot name="dropdown" /></span>'},
    ElDropdownItem: {template: '<span><slot /></span>'},
    ElDropdownMenu: {template: '<span><slot /></span>'},
    ElIcon: true,
    ElInput: {
      props: ['modelValue'],
      template: '<input :value="modelValue" @input="onInput" />',
      methods: {onInput(e) { this.$emit('update:modelValue', e.target.value); }},
    },
    ElPopover: {template: '<span><slot name="reference" /><slot /></span>'},
    ElScrollbar: {template: '<div><slot /></div>'},
    ElInputNumber: true,
    ElOption: true,
    ElSelect: {template: '<div><slot /></div>'},
    ElSlider: true,
    ElSwitch: true,
    ElTag: {template: '<span class="el-tag-stub"><slot /></span>'},
    ElTooltip: {template: '<span><slot /></span>'},
  },
};

const session = (id: string, title: string, archived = false) => ({
  conversationId: id,
  title,
  sessionExt: archived ? {archived: true} : {},
});

const message = (patch: Partial<AgenticMessage>): AgenticMessage =>
  ({id: 'm', role: 'assistant', content: 'hello', ...patch}) as AgenticMessage;

describe('AgenticSessionsRail behaviors', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const baseProps = {
    sessions: [session('c1', 'Device status'), session('c2', '能耗分析'), session('c3', 'Old work', true)],
    activeId: 'c1',
    sessionActionLoading: {},
    disabled: false,
    collapsed: false,
    truncated: false,
  };

  it('filters sessions by title and summary, archived included', async () => {
    const wrapper = mount(AgenticSessionsRail, {global, props: baseProps});
    expect(wrapper.findAll('.agentic-sessions__item')).toHaveLength(2); // archived folded by default
    await wrapper.find('.agentic-sessions__archived-toggle').trigger('click');
    expect(wrapper.findAll('.agentic-sessions__item')).toHaveLength(3);
    const search = wrapper.find('.agentic-sessions__search');
    expect(search.exists()).toBe(true);
    await search.setValue('能耗');
    await flushPromises();
    expect(wrapper.findAll('.agentic-sessions__item')).toHaveLength(1);
    expect(wrapper.text()).toContain('能耗分析');

    await search.setValue('no-such-conversation');
    await flushPromises();
    expect(wrapper.findAll('.agentic-sessions__item')).toHaveLength(0);
    expect(wrapper.text()).toMatch(/无匹配|No matching/);
  });

  it('emits per-item rename, archive and delete with the right id', async () => {
    const wrapper = mount(AgenticSessionsRail, {global, props: baseProps});
    const cmd = (command: string, idx: number) =>
      wrapper.findAllComponents({name: 'ElDropdown'})[idx].vm.$emit('command', command);

    await wrapper.find('.agentic-sessions__archived-toggle').trigger('click');
    await cmd('rename', 0);
    // rename opens the dialog targeting c1; saving emits (id, title)
    await wrapper.find('.el-dialog input').setValue('Renamed!');
    const saveBtn = wrapper
      .findAll('button')
      .find((b) => b.text().includes('保存') || b.text().toLowerCase().includes('save'));
    expect(saveBtn?.text() ?? '').toMatch(/保存|[Ss]ave/);
    await saveBtn!.trigger('click');
    await flushPromises();
    expect(wrapper.emitted('rename')![0]).toEqual(['c1', 'Renamed!']);

    await cmd('archive', 1);
    expect(wrapper.emitted('archive')![0]).toEqual(['c2', true]);

    await cmd('delete', 2);
    expect(wrapper.emitted('delete')![0]).toEqual(['c3']);
  });

  it('keeps the icon pair and hides list text on the collapsed rail', () => {
    const wrapper = mount(AgenticSessionsRail, {global, props: {...baseProps, collapsed: true}});
    expect(wrapper.find('.agentic-sessions').classes()).toContain('is-collapsed');
    const labels = wrapper.findAll('button').map((b) => b.attributes('aria-label'));
    expect(labels).toContain('Search conversations');
    expect(labels).toContain('New');
  });
});

describe('MessageItem behaviors', () => {
  it('shows the outcome badge only for failed, cancelled or recovered messages', () => {
    const failed = mount(MessageItem, {
      global,
      props: {message: message({status: 'FAILED', createTime: '2026-10-01 10:00:00'}), continued: false, focused: false, liveTraces: []},
    });
    expect(failed.find('.agentic-message__badge').text()).toMatch(/失败|Failed/);

    const recovered = mount(MessageItem, {
      global,
      props: {message: message({contentExt: {recovered: true}}), continued: false, focused: false, liveTraces: []},
    });
    expect(recovered.find('.agentic-message__badge').text()).toMatch(/恢复|Recovered/);

    const plain = mount(MessageItem, {
      global,
      props: {message: message({status: 'COMPLETED'}), continued: false, focused: false, liveTraces: []},
    });
    expect(plain.find('.agentic-message__badge').exists()).toBe(false);
  });

  it('renders the timestamp and disables copy and quote while streaming', () => {
    const done = mount(MessageItem, {
      global,
      props: {message: message({createTime: '2026-10-01 10:00:00'}), continued: true, focused: true, liveTraces: []},
    });
    expect(done.find('.agentic-message__time').text()).toContain('10:00');
    expect(done.find('.agentic-message').classes()).toContain('is-continued');
    expect(done.find('.agentic-message').classes()).toContain('is-focused');
    const buttons = done.findAll('button');
    for (const b of buttons) expect(b.attributes('disabled')).toBeUndefined();

    const streaming = mount(MessageItem, {
      global,
      props: {message: message({streaming: true, content: ''}), continued: false, focused: false, liveTraces: []},
    });
    const sButtons = streaming.findAll('button');
    for (const b of sButtons) expect(b.attributes('disabled')).not.toBeUndefined();
    streaming.findAll('button')[0].trigger('click');
    expect(streaming.emitted('copy')).toBeUndefined(); // disabled buttons do not emit
  });
});

describe('AgenticComposer behaviors', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('never submits blank or whitespace-only drafts', async () => {
    const wrapper = mount(AgenticComposer, {
      global,
      props: {draft: '', quotedMessage: undefined},
    });
    await wrapper.find('.agentic-input').trigger('keydown', {key: 'Enter'});
    expect(wrapper.emitted('submit')).toBeUndefined();

    await wrapper.setProps({draft: '   '});
    await wrapper.find('.agentic-input').trigger('keydown', {key: 'Enter'});
    expect(wrapper.emitted('submit')).toBeUndefined();
  });

  it('emits submit when the draft carries content', async () => {
    const wrapper = mount(AgenticComposer, {
      global,
      props: {draft: '列出设备'},
    });
    await wrapper.find('.agentic-input').trigger('keydown', {key: 'Enter'});
    expect(wrapper.emitted('submit')).toHaveLength(1);
  });

  it('emits update:quotedMessage with undefined when the quote is removed', async () => {
    const wrapper = mount(AgenticComposer, {
      global,
      props: {
        draft: '',
        quotedMessage: message({id: 'q1', role: 'assistant', content: 'quoted **body**'}),
      },
    });
    const quote = wrapper.find('.chat-quote');
    expect(quote.exists()).toBe(true);
    wrapper.findComponent({name: 'QuoteCard'}).vm.$emit('remove');
    expect(wrapper.emitted('update:quotedMessage')![0]).toEqual([undefined]);
  });
});
