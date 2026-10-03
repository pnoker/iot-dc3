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
import {describe, expect, it, vi} from 'vitest';

import MessageStream from '@/components/agentic/MessageStream.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';

vi.mock('vue-element-plus-x', () => ({
  Prompts: {template: '<div class="prompts-stub" />'},
  Welcome: {template: '<div class="welcome-stub" />'},
}));

const global = {
  plugins: [i18n],
  stubs: {
    ElIcon: true,
    ElScrollbar: {template: '<div><slot /></div>'},
    ElTag: {template: '<span class="el-tag-stub"><slot /></span>'},
    ElTooltip: {template: '<span><slot /></span>'},
    ElSkeleton: true,
    ElButton: true,
    ElDialog: {template: '<div><slot /></div>'},
    ElDropdown: {template: '<span><slot /></span>'},
    ElDropdownItem: {template: '<span><slot /></span>'},
    ElDropdownMenu: {template: '<span><slot /></span>'},
    ElPopover: {template: '<span><slot /></span>'},
    ElInput: true,
    ElSlider: true,
    ElSwitch: true,
    ElSelect: {template: '<div><slot /></div>'},
    ElOption: true,
    ElInputNumber: true,
  },
};

const buildConversation = (rounds: number): AgenticMessage[] =>
  Array.from({length: rounds * 2}, (_, i) => ({
    id: `m${i}`,
    role: i % 2 === 0 ? 'user' : 'assistant',
    content: `message ${i} ` + 'x'.repeat(80),
    createTime: '2026-10-03 10:00:00',
    ...(i % 2 === 1
      ? {contentExt: {tools: [`tool${i}`], tokens: {input: 1, output: 2}}}
      : {}),
  })) as AgenticMessage[];

describe('long conversation capacity', () => {
  it('renders a 200-message conversation within the node and time budgets', () => {
    const messages = buildConversation(100);
    const started = performance.now();
    const wrapper = mount(MessageStream, {
      global,
      props: {messages, liveTraces: [], loading: false, focusedMessageId: '', prompts: []},
    });
    const elapsed = performance.now() - started;

    const nodes = wrapper.element.querySelectorAll('*').length;
    // Budget: a message must not cost more than ~60 DOM nodes, and a full
    // conversation must mount in seconds, not minutes. Measured baseline is
    // well under these bounds — they exist to catch regression, not to grade
    // hardware.
    expect(nodes / messages.length).toBeLessThan(60);
    expect(elapsed).toBeLessThan(5000);
    // and the DOM count stays linear (no per-message blow-up)
    expect(nodes).toBeLessThan(12000);
  });

  it('keeps every message reachable without virtualization gaps', () => {
    const messages = buildConversation(60);
    const wrapper = mount(MessageStream, {
      global,
      props: {messages, liveTraces: [], loading: false, focusedMessageId: '', prompts: []},
    });
    // a scroll-driven list must still expose each message to find()
    for (const id of ['m0', 'm59', 'm119']) {
      expect(wrapper.text()).toContain(id.replace('m', 'message '));
    }
    expect(wrapper.findAllComponents({name: 'MessageItem'})).toHaveLength(120);
  });
});
