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
import {createPinia, setActivePinia} from 'pinia';
import {defineComponent, h} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';
import {useAgenticStore} from '@/store';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

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
vi.mock('vue-element-plus-x/styles/index.css', () => ({}));
vi.mock('vue-element-plus-x', () => ({
  Prompts: {template: '<div class="prompts-stub" />'},
  Welcome: {template: '<div class="welcome-stub" />'},
}));

const PassthroughStub = defineComponent({
  name: 'PassthroughStub',
  setup(_, {slots}) {
    return () => h('span', slots.default?.());
  },
});

// Direct state injection on the agentic store. Going through the public
// `bootstrap()` action would force four mocked API calls per test for
// values that are completely irrelevant to the UI assertions below; the
// tradeoff favours seeded state with a one-line escape hatch over
// scaffolding API mocks. If you find yourself growing this helper with
// production-like derivation logic, that's the moment to switch back to
// public actions.
function seedAssistant(messages: AgenticMessage[]) {
  const store = useAgenticStore();
  store.visible = true;
  store.loading = false;
  store.streaming = messages.some((message) => message.streaming);
  store.activeConversationId = 'conversation-ui-test';
  store.sessions = [{conversationId: 'conversation-ui-test', title: 'Reasoning UI'}];
  store.models = [
    {
      model: 'reasoning-model',
      label: 'Reasoning Model',
      stream: true,
      toolCall: true,
      vision: false,
      reasoning: true,
    },
  ];
  store.selectedModel = 'reasoning-model';
  store.reasoningEnabled = true;
  store.messagesByConversation = {'conversation-ui-test': messages};
  store.attachmentsByConversation = {'conversation-ui-test': []};
  store.pendingAttachmentIdsByConversation = {'conversation-ui-test': []};
  store.pendingActionsByConversation = {'conversation-ui-test': []};
  store.traceEventsByConversation = {'conversation-ui-test': []};
}

function mountAssistant() {
  return mount(AgenticAssistant, {
    global: {
      plugins: [i18n],
      directives: {
        loading: () => undefined,
      },
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElDropdown: {name: 'ElDropdown', template: '<span><slot /><slot name="dropdown" /></span>'},
        ElDropdownItem: {template: '<span><slot /></span>'},
        ElDropdownMenu: {template: '<span><slot /></span>'},
        ElIcon: PassthroughStub,
        ElPopover: {template: '<span><slot name="reference" /><slot /></span>'},
        ElSlider: {template: '<input class="el-slider-stub" />'},
        // Element Plus icons — stub to a passthrough so the component tree mounts.
        ChatDotRound: PassthroughStub,
        ChatLineSquare: PassthroughStub,
        Check: PassthroughStub,
        CircleClose: PassthroughStub,
        Clock: PassthroughStub,
        Close: PassthroughStub,
        Cpu: PassthroughStub,
        Delete: PassthroughStub,
        Document: PassthroughStub,
        DocumentCopy: PassthroughStub,
        EditPen: PassthroughStub,
        Lightning: PassthroughStub,
        Paperclip: PassthroughStub,
        Plus: PassthroughStub,
        Promotion: PassthroughStub,
        Setting: PassthroughStub,
        Tickets: PassthroughStub,
        VideoPause: PassthroughStub,
        Warning: PassthroughStub,
        RenderedAssistantMessage: {
          props: ['content'],
          template: '<div class="rendered-assistant-message">{{ content }}</div>',
        },
      },
    },
  });
}

describe('AgenticAssistant', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('opens the reasoning panel while an assistant answer is streaming', () => {
    seedAssistant([
      {
        id: 'assistant-1',
        role: 'assistant',
        content: '',
        reasoning: '正在确认当前租户上下文，并准备查询设备列表。',
        streaming: true,
      },
    ]);

    const wrapper = mountAssistant();
    const panel = wrapper.find('.agentic-reasoning-panel');

    expect(panel.exists()).toBe(true);
    // <details open> renders with `open=""` — the attribute exists with an
    // empty string value. Be explicit so an accidental `open="false"`
    // wouldn't pass.
    expect(Object.keys(panel.attributes())).toContain('open');
    expect(wrapper.find('.agentic-reasoning-panel__text').text()).toContain('正在确认当前租户上下文');
    expect(wrapper.find('.agentic-live-thinking').exists()).toBe(false);
  });

  it('collapses completed reasoning by default and keeps technical details separate', () => {
    seedAssistant([
      {
        id: 'assistant-1',
        role: 'assistant',
        content: '当前共有 3 台设备在线。',
        reasoning: '先读取租户上下文，再调用设备查询工具。',
        streaming: false,
        contentExt: {
          reasoning: true,
          tools: ['searchDevices'],
          tokens: {input: 40, output: 20},
        },
      },
    ]);

    const wrapper = mountAssistant();
    const panel = wrapper.find('.agentic-reasoning-panel');

    expect(panel.exists()).toBe(true);
    expect(panel.attributes('open')).toBeUndefined();
    expect(wrapper.find('.agentic-reasoning-panel__text').text()).toContain('先读取租户上下文');
    expect(wrapper.find('.agentic-details').text()).not.toContain('先读取租户上下文');
    expect(wrapper.find('.agentic-details').text()).toContain('searchDevices');
  });

  it('shows a compact removable preview instead of inserting raw markdown into the composer', async () => {
    seedAssistant([
      {
        id: 'assistant-quote',
        role: 'assistant',
        content: '### 分析结果\n这是一段需要引用的 **AI 分析结果**。\n它包含多行详细信息。',
        streaming: false,
      },
    ]);

    const wrapper = mountAssistant();
    const actions = wrapper.findAll('.agentic-message__action');
    await actions[1].trigger('click');

    expect(wrapper.find('.chat-quote--surface').exists()).toBe(true);
    expect(wrapper.find('.chat-quote--surface').text()).toContain('AI response');
    expect(wrapper.find('.chat-quote--surface').text()).toContain('需要引用的 AI 分析结果');
    expect(wrapper.find('.chat-quote--surface').text()).not.toContain('> ');
    expect(wrapper.find('.chat-quote--surface').text()).not.toContain('###');

    await wrapper.find('.chat-quote__close').trigger('click');
    expect(wrapper.find('.chat-quote--surface').exists()).toBe(false);
  });

  it('renders quoted user content as a styled card instead of raw markdown markers', () => {
    seedAssistant([
      {
        id: 'user-quote',
        role: 'user',
        content: '> **AI response**\n> FAN-03 温度趋势存在异常。\n\n请继续分析原因。',
        streaming: false,
      },
    ]);

    const wrapper = mountAssistant();

    expect(wrapper.find('.chat-quote--on-primary').text()).toContain('FAN-03 温度趋势存在异常');
    expect(wrapper.find('.agentic-text').text()).toBe('请继续分析原因。');
    expect(wrapper.find('.agentic-message__content').text()).not.toContain('> **');
  });

  // M41: the docked history dropdown must apply the SAME session-list
  // contract as the workbench rail — archived sessions folded out, one
  // shared search rule, pagination reachable.
  it('docked history dropdown shares the rail data rules and pagination', async () => {
    seedAssistant([
      {
        id: 'assistant-1',
        role: 'assistant',
        content: '当前共有 3 台设备在线。',
        streaming: false,
      },
    ]);
    const store = useAgenticStore();
    apiMocks.listAgenticSessions
      .mockResolvedValueOnce({
        items: [
          {conversationId: 'conversation-ui-test', title: 'Reasoning UI'},
          {conversationId: 'archived-1', title: 'Archived work', sessionExt: {archived: true}},
          {conversationId: 'live-2', title: 'Live session'},
        ],
        hasNext: true,
      })
      .mockResolvedValueOnce({
        items: [
          {conversationId: 'page-2-a', title: 'Older session A'},
          {conversationId: 'page-2-b', title: 'Older session B'},
        ],
        hasNext: false,
      });
    await store.loadSessions();
    expect(store.sessionsHasMore).toBe(true);

    const wrapper = mountAssistant();

    // Session titles as the dropdown lists them (the load-more entry is
    // filtered out — it is an action, not a conversation).
    const historyTitles = () =>
      wrapper
        .findAll('.agentic-history-item')
        .filter((item) => item.find('.agentic-history-more').exists() === false)
        .map((item) => item.find('strong').text());

    // Archived sessions never mix into the docked switcher.
    expect(historyTitles()).toEqual(['Reasoning UI', 'Live session']);
    expect(wrapper.find('.agentic-history-search').exists()).toBe(true);

    // The dropdown search narrows by the same title+summary rule as the rail.
    const search = wrapper.find('.agentic-history-search input');
    await search.setValue('Live');
    expect(historyTitles()).toEqual(['Live session']);
    await search.setValue('no-such-session');
    expect(historyTitles()).toEqual([]);
    expect(wrapper.text()).toMatch(/无匹配|No matching/);

    // Clearing the search restores the list; the load-more command appends
    // the next page through the store (offset pagination, no flicker).
    await search.setValue('');
    expect(historyTitles()).toEqual(['Reasoning UI', 'Live session']);

    wrapper.findComponent({name: 'ElDropdown'}).vm.$emit('command', 'load-more');
    await flushPromises();

    expect(apiMocks.listAgenticSessions).toHaveBeenLastCalledWith({offset: 3, limit: 50});
    expect(historyTitles()).toEqual(['Reasoning UI', 'Live session', 'Older session A', 'Older session B']);
    expect(store.sessionsHasMore).toBe(false);
  });
});
