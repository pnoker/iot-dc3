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
import DOMPurify from 'dompurify';
import {marked} from 'marked';
import {describe, expect, it, vi} from 'vitest';

import MessageStream from '@/components/agentic/MessageStream.vue';
import RenderedAssistantMessage from '@/components/agentic/RenderedAssistantMessage.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';

vi.mock('vue-element-plus-x', () => ({
  Prompts: {template: '<div class="prompts-stub" />'},
  Welcome: {template: '<div class="welcome-stub" />'},
}));

const global = {
  plugins: [i18n],
  stubs: {
    // G2 needs a real canvas; the perf budgets here measure the markdown
    // pipeline, not chart rendering.
    ChartBlock: {name: 'ChartBlock', template: '<div class="chart-block-stub" />'},
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

describe('streaming render capacity', () => {
  // M39 regression bound: every streamed delta used to trigger a full
  // markdown parse + DOMPurify pass + DOM swap. With delta coalescing in
  // the store (~one flush per frame) and a raw-text live tail, the number
  // of parses must stay bounded by the frame count — not the token count —
  // and the final frame must re-parse and re-sanitize the whole message.
  const markdownFrame = (frame: number) => {
    return `**第 ${frame} 段**：风机轴承温度持续攀升，超过告警阈值。建议检查润滑与散热。\n\n- 项号 ${frame}：温度 8${frame % 10}.4℃\n- 项号 ${frame + 1}：负载偏高\n\n\`sensor-${frame}\` 读数异常。\n\n`;
  };

  it('bounds markdown re-parses for a long chart-free stream and re-sanitizes at the end', async () => {
    const parseSpy = vi.spyOn(marked, 'parse');
    const sanitizeSpy = vi.spyOn(DOMPurify, 'sanitize');
    const totalFrames = 40;

    const wrapper = mount(RenderedAssistantMessage, {
      global,
      props: {content: '', streaming: true},
    });

    let streamed = '';
    for (let frame = 1; frame <= totalFrames; frame += 1) {
      streamed += markdownFrame(frame);
      // one coalesced store flush lands per frame
      await wrapper.setProps({content: streamed});
    }

    // The live tail renders as raw text and the stream is chart-free, so no
    // markdown parse happens at all while streaming.
    expect(parseSpy.mock.calls.length).toBe(0);
    expect(wrapper.find('.agentic-markdown--raw').text()).toContain(markdownFrame(1).slice(0, 8));

    await wrapper.setProps({content: streamed, streaming: false});

    // The completion pass renders the full formatted body exactly once.
    expect(parseSpy.mock.calls.length).toBe(1);
    expect(sanitizeSpy.mock.calls.length).toBe(1);
    const rendered = wrapper.find('.agentic-markdown');
    expect(rendered.text()).toContain('第 1 段');
    expect(rendered.html()).toContain('<strong>第 1 段</strong>');
    expect(wrapper.find('.agentic-markdown--raw').exists()).toBe(false);
  });

  it('parses each closed chart-adjacent segment at most once while charts stream in', async () => {
    const parseSpy = vi.spyOn(marked, 'parse');
    const chartFence = (index: number) =>
      '```chart:line\n' +
      JSON.stringify({title: `trend ${index}`, series: [{name: 'temp', data: [[index, 80], [index + 1, 82]]}]}) +
      '\n```';

    const wrapper = mount(RenderedAssistantMessage, {global, props: {content: '', streaming: true}});

    // Three chart fences close over the stream; each closure turns the
    // markdown before it into a closed segment that parses once.
    let streamed = '';
    for (let frame = 1; frame <= 30; frame += 1) {
      streamed += markdownFrame(frame);
      if (frame % 10 === 0) streamed += `${chartFence(frame / 10)}\n\n`;
      await wrapper.setProps({content: streamed});
    }

    // 3 closed segments parse once each — not once per frame that re-renders
    // them (30 frames would have re-parsed them 30 times each before).
    expect(parseSpy.mock.calls.length).toBe(3);

    await wrapper.setProps({content: streamed, streaming: false});
    // The completion pass re-renders every markdown segment once more
    // (fresh sanitize).
    expect(parseSpy.mock.calls.length).toBe(6);
    expect(wrapper.findAll('.agentic-markdown')).toHaveLength(3);
    expect(wrapper.findAll('.chart-block-stub')).toHaveLength(3);
  });
});
