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
import {createPinia, setActivePinia} from 'pinia';
import {defineComponent, h} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';
import {useAgenticStore} from '@/store';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

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
        ElDropdown: {template: '<span><slot /><slot name="dropdown" /></span>'},
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
});
