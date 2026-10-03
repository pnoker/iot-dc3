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

import {createElButtonStub} from '../setup/stubs/element-plus';
import {describe, expect, it, vi} from 'vitest';

import AgenticContextPanel from '@/components/agentic/AgenticContextPanel.vue';
import MessageDetails from '@/components/agentic/MessageDetails.vue';
import MessageStream from '@/components/agentic/MessageStream.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage, AgenticSession} from '@/config/types';

vi.mock('vue-element-plus-x', () => ({
  Prompts: {props: ['items'], emits: ['item-click'], template: '<div class="prompts-stub" @click="$emit(\'item-click\', items[0])"><slot /></div>'},
  Welcome: {template: '<div class="welcome-stub" />'},
}));

const global = {
  plugins: [i18n],
  stubs: {
    ElIcon: true,
    ElScrollbar: {template: '<div><slot /></div>'},
    ElTag: {template: '<span class="el-tag-stub"><slot /></span>'},
    ElButton: createElButtonStub(),
    ElSkeleton: true,
    ElTooltip: {template: '<span><slot /></span>'},
  },
};

const message = (patch: Partial<AgenticMessage>): AgenticMessage =>
  ({id: 'm', role: 'assistant', content: 'ok', ...patch}) as AgenticMessage;

describe('MessageDetails', () => {
  it('renders nothing for plain messages and folds rich footnotes for full ones', () => {
    const plain = mount(MessageDetails, {
      global,
      props: {message: message({status: 'COMPLETED'}), liveTraces: []},
    });
    expect(plain.find('.agentic-details').exists()).toBe(false);

    const rich = mount(MessageDetails, {
      global,
      props: {
        message: message({
          contentExt: {
            tools: ['searchDevices'],
            tokens: {input: 3, output: 4},
            contexts: [{type: 'memory', content: 'ctx-body'}],
          },
        }),
        liveTraces: [],
      },
    });
    expect(rich.find('.agentic-details').exists()).toBe(true);
    expect(rich.find('summary').text()).toMatch(/1 (tools|个工具)/); // summary folds counts
    expect(rich.findAll('.chat-card').length).toBeGreaterThanOrEqual(2); // tools + contexts cards
    expect(rich.find('.agentic-tool-list').exists()).toBe(true);
    expect(rich.find('.agentic-context pre').text()).toBe('ctx-body');
    expect(rich.find('.chat-stat').exists()).toBe(true); // token grid
  });
});

describe('MessageStream', () => {
  const prompts = [{key: 'k', label: 'prompt', description: 'desc'}];

  it('shows the welcome state with prompts when empty and emits prompt-click', async () => {
    const wrapper = mount(MessageStream, {
      global,
      props: {messages: [], liveTraces: [], loading: false, focusedMessageId: '', prompts},
    });
    expect(wrapper.find('.welcome-stub').exists()).toBe(true);
    await wrapper.find('.prompts-stub').trigger('click');
    expect(wrapper.emitted('prompt-click')![0]).toEqual([prompts[0]]);
    expect(wrapper.find('.agentic-messages').exists()).toBe(false);
  });

  it('shows the skeleton only while the first load is in flight', () => {
    const loading = mount(MessageStream, {
      global,
      props: {messages: [], liveTraces: [], loading: true, focusedMessageId: '', prompts},
    });
    expect(loading.find('.agentic-loading-state').exists()).toBe(true);
    expect(loading.find('.welcome-stub').exists()).toBe(false);
  });

  it('marks the focused message and forwards item events upward', async () => {
    const messages = [
      message({id: 'a', role: 'user', content: 'q'}),
      message({id: 'b', role: 'assistant', content: 'ans', createTime: '2026-10-01 10:00:00'}),
    ];
    const wrapper = mount(MessageStream, {
      global,
      props: {messages, liveTraces: [], loading: false, focusedMessageId: 'a', prompts},
    });
    const items = wrapper.findAllComponents({name: 'MessageItem'});
    expect(items).toHaveLength(2);
    expect(items[0].props('focused')).toBe(true);
    expect(items[1].props('focused')).toBe(false);
    // continuation only when the previous message shares the role
    expect(items[1].props('continued')).toBe(false);

    await items[1].vm.$emit('focus');
    expect(wrapper.emitted('focus-message')![0]).toEqual(['b']);
    await items[0].vm.$emit('copy', messages[0]);
    expect(wrapper.emitted('copy')![0]).toEqual([messages[0]]);
    await items[0].vm.$emit('quote', messages[0]);
    expect(wrapper.emitted('quote')![0]).toEqual([messages[0]]);
  });
});

describe('AgenticContextPanel', () => {
  const session = {
    conversationId: 'c1',
    title: 'Device status',
    summary: 'weekly scan',
    sessionExt: {model: 'm1', reasoningEnabled: true, temperature: 0.7, maxTokens: 2048},
  } as AgenticSession & {title: string};

  it('renders the session card chips and the empty hint without a focused message', () => {
    const wrapper = mount(AgenticContextPanel, {
      global,
      props: {session, modelLabel: 'MiMo Flash', liveTraces: []},
    });
    const text = wrapper.text();
    expect(text).toContain('Device status');
    expect(text).toContain('weekly scan');
    expect(text).toContain('MiMo Flash');
    expect(text).toContain('0.7');
    expect(wrapper.find('.agentic-context__empty').exists()).toBe(true);
  });

  it('shows the focused message run and emits toggle from the close button', async () => {
    const wrapper = mount(AgenticContextPanel, {
      global,
      props: {
        session,
        modelLabel: 'MiMo Flash',
        liveTraces: [],
        message: message({contentExt: {tools: ['searchDevices'], tokens: {input: 1, output: 2}}}),
      },
    });
    expect(wrapper.find('.agentic-context__empty').exists()).toBe(false);
    expect(wrapper.find('.agentic-tool-list').exists()).toBe(true);
    expect(wrapper.find('.chat-stat').exists()).toBe(true);
    await wrapper.find('.agentic-context__header button').trigger('click');
    expect(wrapper.emitted('toggle')).toHaveLength(1);
  });
});
